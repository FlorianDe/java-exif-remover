package de.florian.exif.remover.view;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.settings.ThemeSettings;
import com.github.weisj.darklaf.theme.info.DefaultThemeProvider;
import de.florian.exif.remover.ExifRemover;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;


class DirectoryFileFilter extends FileFilter {
    @Override
    public boolean accept(File file) {
        return file.isDirectory();
    }

    @Override
    public String getDescription() {
        return "Folders only";
    }
}

class LabelAccessory extends JLabel implements PropertyChangeListener {
    private static final int PREFERRED_WIDTH = 125;
    private static final int PREFERRED_HEIGHT = 125;

    public LabelAccessory(JFileChooser chooser) {
        setVerticalAlignment(JLabel.CENTER);
        setHorizontalAlignment(JLabel.CENTER);
        chooser.addPropertyChangeListener(this);
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
    }

    public void propertyChange(PropertyChangeEvent changeEvent) {
        String changeName = changeEvent.getPropertyName();
        if (changeName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            File file = (File) changeEvent.getNewValue();
            if (file != null) {
                ImageIcon icon = new ImageIcon(file.getPath());
                if (icon.getIconWidth() > PREFERRED_WIDTH) {
                    icon = new ImageIcon(icon.getImage().getScaledInstance(PREFERRED_WIDTH, -1, Image.SCALE_DEFAULT));
                    if (icon.getIconHeight() > PREFERRED_HEIGHT) {
                        icon = new ImageIcon(icon.getImage().getScaledInstance(-1, PREFERRED_HEIGHT, Image.SCALE_DEFAULT));
                    }
                }
                setIcon(icon);
            }
        }
    }
}

class FileChooserExtrasRow extends JPanel {
    private final JCheckBox checkBox;

    private void setFileHidingEnabled(JFileChooser fileChooser, JCheckBox checkBox) {
        fileChooser.setFileHidingEnabled(!checkBox.isSelected());
        fileChooser.rescanCurrentDirectory();
    }

    public FileChooserExtrasRow(JFileChooser fileChooser) {
        super();
        checkBox = new JCheckBox("Show hidden files");
        checkBox.addActionListener(e -> this.setFileHidingEnabled(fileChooser, checkBox));

        this.setFileHidingEnabled(fileChooser, checkBox);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(Box.createVerticalGlue());
        this.add(checkBox);
    }
}

public class MainFrame extends JFrame {

    private final JFileChooser fileChooserSource;
    private final JFileChooser fileChooserDestination;
    private final JButton copyButton;
    private final JProgressBar progressBar;
    private final JTextArea textArea;
    private final JCheckBox flattenFilesCheckBox;

    private final ExifRemover exifRemover = new ExifRemover();
    private final FileNameExtensionFilter imagefilesExtensionFilter = new FileNameExtensionFilter("Supported files " + ExifRemover.Ext.getExtensionDescription(), ExifRemover.Ext.getNames());

    public MainFrame() {
        LafManager.setThemeProvider(new DefaultThemeProvider());
        LafManager.installTheme(LafManager.getPreferredThemeStyle());
        LafManager.setDecorationsEnabled(true);
        LafManager.enabledPreferenceChangeReporting(true);
        ThemeSettings.getInstance().setThemeFollowsSystem(true);

        setTitle("Java Exit Remover");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create the file choosers
        fileChooserSource = new FilterableJFileChooser(null, true, null, imagefilesExtensionFilter);
        fileChooserSource.setControlButtonsAreShown(false);
        fileChooserSource.setAccessory(new LabelAccessory(fileChooserSource));
        FileChooserExtrasRow fileChooserSourceExtras = new FileChooserExtrasRow(fileChooserSource);

        fileChooserDestination = new JFileChooser();
        fileChooserDestination.setControlButtonsAreShown(false);
//        fileChooserDestination.setAcceptAllFileFilterUsed(false);
        fileChooserDestination.setFileFilter(new DirectoryFileFilter());
        fileChooserDestination.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        FileChooserExtrasRow fileChooserDestinationExtras = new FileChooserExtrasRow(fileChooserDestination);


        // Create the labels
        JLabel sourceLabel = new JLabel("Source files/folders");
        sourceLabel.setHorizontalAlignment(JLabel.CENTER);
        sourceLabel.setVerticalAlignment(JLabel.CENTER);
        sourceLabel.setFont(sourceLabel.getFont().deriveFont(Font.BOLD));
        sourceLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel destLabel = new JLabel("Destination folder");
        destLabel.setHorizontalAlignment(JLabel.CENTER);
        destLabel.setVerticalAlignment(JLabel.CENTER);
        destLabel.setFont(destLabel.getFont().deriveFont(Font.BOLD));
        destLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Create the progress bar
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setStringPainted(true);
        progressBar.setVisible(true);

        // Create TextArea
        Box textAreaBox = Box.createVerticalBox();
        textArea = new JTextArea();
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);
        textAreaBox.add(scrollPane);
        textArea.setVisible(false);

        // Create the panels for each file chooser and label
        JPanel sourcePanel = new JPanel(new BorderLayout());
        sourcePanel.add(sourceLabel, BorderLayout.NORTH);
        sourcePanel.add(fileChooserSource, BorderLayout.CENTER);
        sourcePanel.add(fileChooserSourceExtras, BorderLayout.SOUTH);
        JPanel destPanel = new JPanel(new BorderLayout());
        destPanel.add(destLabel, BorderLayout.NORTH);
        destPanel.add(fileChooserDestination, BorderLayout.CENTER);
        destPanel.add(fileChooserDestinationExtras, BorderLayout.SOUTH);

        // Create the action and info panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        copyButton = new JButton("Copy");
        copyButton.setEnabled(false);
        flattenFilesCheckBox = new JCheckBox("Flatten files");
        flattenFilesCheckBox.setToolTipText("Copy all files right into the destination folder");
        actionPanel.add(copyButton);
        actionPanel.add(progressBar);
        actionPanel.add(flattenFilesCheckBox);

        JPanel actionInfoPanel = new JPanel();
        actionInfoPanel.setLayout(new BoxLayout(actionInfoPanel, BoxLayout.Y_AXIS));
        actionInfoPanel.add(Box.createVerticalStrut(10));
        actionInfoPanel.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.LINE_START);
        actionInfoPanel.add(Box.createVerticalStrut(5));
        actionInfoPanel.add(actionPanel);
        actionInfoPanel.add(textAreaBox);

        // Create the split pane for the file choosers
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sourcePanel, destPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);

        // Add the split pane and button panel to the JFrame
        add(splitPane, BorderLayout.CENTER);
        add(actionInfoPanel, BorderLayout.SOUTH);

        Supplier<Boolean> filesSelected = () -> {
            File[] files = fileChooserSource.getSelectedFiles();
            File file = fileChooserDestination.getSelectedFile();
            return files.length > 0 && file != null;
        };

        Consumer<JFileChooser> addChangeListeners = (fileChooser) -> {
            fileChooser.addPropertyChangeListener(JFileChooser.DIRECTORY_CHANGED_PROPERTY, (e) -> {
                fileChooser.setSelectedFile(null);
                copyButton.setEnabled(filesSelected.get());
            });
            fileChooser.addPropertyChangeListener((e) -> {
                if (e.getPropertyName().equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY) || e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    copyButton.setEnabled(filesSelected.get());
                }
            });
        };
        addChangeListeners.accept(fileChooserSource);
        addChangeListeners.accept(fileChooserDestination);

        copyButton.addActionListener(e -> {
            File[] files = fileChooserSource.getSelectedFiles();
            File dest = fileChooserDestination.getSelectedFile();
            try {
                if (dest == null) {
                    JOptionPane.showMessageDialog(
                            this,
                            "No destination folder selected",
                            "No destination folder selected",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                if (files == null || files.length == 0) {
                    JOptionPane.showMessageDialog(
                            this,
                            "No files selected somehow",
                            "No files selected",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                long filesCount = exifRemover.count(files, imagefilesExtensionFilter::accept);
                String rootFoldersJoinedByNewline = Arrays.stream(files)
                        .map(File::getAbsolutePath)
                        .map((path) -> "\t\t - " + path)
                        .collect(Collectors.joining("\n"));

                if (filesCount == 0) {
                    JOptionPane.showMessageDialog(
                            this,
                            "No matching files found in selected root folders:\n\t\t - " + rootFoldersJoinedByNewline,
                            "No matching files found",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }
                String msg = "Are you sure you want to copy " + filesCount + " files without meta data?   \n\n" +
                        "From selected root folders:\n" +
                        rootFoldersJoinedByNewline + "\n\n" +
                        "To destination folder:\n" +
                        "\t\t - " + dest.getAbsolutePath() + "\n\n" +
                        "This will overwrite existing files.\n\n";

                int copyConfirmResponse = JOptionPane.showConfirmDialog(
                        this,
                        msg,
                        "Copy files to destination?",
                        JOptionPane.YES_NO_OPTION
                );
                if (copyConfirmResponse == JOptionPane.YES_OPTION) {
                    progressBar.setMaximum((int) filesCount);
                    textAreaBox.setPreferredSize(new Dimension(400, 200));
                    textArea.setVisible(true);
                    textArea.setText("");
                    copyButton.setEnabled(false);
                    progressBar.setEnabled(true);
                    progressBar.setVisible(true);

                    pack();
                    new Thread(new CopyFilesTask(files, dest)).start();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        pack();
        setVisible(true);
    }

    private class CopyFilesTask implements Runnable {
        private final File[] files;
        private final File dest;
        AtomicInteger counter = new AtomicInteger(0);

        public CopyFilesTask(File[] files, File dest) {
            this.files = files;
            this.dest = dest;
        }

        private void onFileCopy(File src, File target) {
            try {
                SwingUtilities.invokeAndWait(() -> progressBar.setValue(counter.incrementAndGet()));
                textArea.append(String.format("Copied %s to %s\n", src, target));
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            } finally {
                copyButton.setEnabled(true);
            }
        }

        @Override
        public void run() {
            try {
                exifRemover.copy(files, this.dest, imagefilesExtensionFilter::accept, flattenFilesCheckBox.isSelected(), this::onFileCopy);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
