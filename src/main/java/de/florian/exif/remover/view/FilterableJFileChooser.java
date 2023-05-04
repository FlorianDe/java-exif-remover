package de.florian.exif.remover.view;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;

public class FilterableJFileChooser extends JFileChooser {

    public FilterableJFileChooser(File currentDirectory, boolean multipleSelection) {
        this(currentDirectory, multipleSelection, null, (FileFilter) null);
    }

    public FilterableJFileChooser(File currentDirectory, boolean multipleSelection, JComponent accessory) {
        this(currentDirectory, multipleSelection, accessory, (FileFilter) null);
    }

    public FilterableJFileChooser(File currentDirectory, boolean multipleSelection, JComponent accessory, FileFilter... filerfilters) {
        super();

        FilterableJFileChooser.disableNewFolderButton(this);
        if (filerfilters.length > 0) {
            this.setAcceptAllFileFilterUsed(false);
            for (FileFilter fileFilter : filerfilters) {
                this.setFileFilter(fileFilter);
            }
        }
        this.setMultiSelectionEnabled(multipleSelection);
        if (multipleSelection) {
            this.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        }
        this.setCurrentDirectory(currentDirectory);

        if (accessory != null) {
            this.setAccessory(accessory);
        }
    }


    static void disableNewFolderButton(Container c) {
        int len = c.getComponentCount();
        for (int i = 0; i < len; i++) {
            Component comp = c.getComponent(i);
            if (comp instanceof JButton) {
                JButton b = (JButton) comp;
                Icon icon = b.getIcon();
                if (icon != null && icon == UIManager.getIcon("FileChooser.newFolderIcon")) {
                    b.setEnabled(false);
                }
            } else if (comp instanceof Container) {
                disableNewFolderButton((Container) comp);
            }
        }
    }
}
