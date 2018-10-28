package de.florian.exif.remover;

import de.florian.exif.remover.view.FilterableJFileChooser;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ExifRemover {

    enum Ext {
        PNG,
        JPG,
        JPEG;

        String regex() {
            return "\\." + this.name();
        }

        static String getOrRegex() {
            return "(" + String.join("|", Arrays.stream(values()).map(Ext::regex).collect(Collectors.toList())) + ")";
        }

        static String[] getNames() {
            return Arrays.stream(values()).map(Ext::name).toArray(String[]::new);
        }

        static String getExtensionDescription() {
            return "(" + String.join(", ", Arrays.stream(values()).map(Ext::name).collect(Collectors.toList())) + ")";
        }
    }

    private final Pattern imageEndingPattern = Pattern.compile(Ext.getOrRegex(), Pattern.CASE_INSENSITIVE);

    public void run() {

        FileNameExtensionFilter imagefilesExtensionFilter = new FileNameExtensionFilter("Supported files " + Ext.getExtensionDescription(), Ext.getNames());

        FilterableJFileChooser cfc = new FilterableJFileChooser(new File("."), true, null, imagefilesExtensionFilter);
        cfc.setDialogTitle("Select the source images/folders.");

        int selectFilesResult = cfc.showOpenDialog(null);
        switch (selectFilesResult) {
            case JFileChooser.APPROVE_OPTION:
                String userDir = System.getProperty("user.home");
                JFileChooser dfFileChooser = new JFileChooser(userDir + "/Desktop");
                dfFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                dfFileChooser.setDialogTitle("Select the destination folder.");
                int selectDestinationResult = dfFileChooser.showOpenDialog(null);
                switch (selectDestinationResult) {
                    case JFileChooser.APPROVE_OPTION:
                        for (File fileTier0 : cfc.getSelectedFiles()) {
                            System.out.println("Processing " + fileTier0 + " element.");
                            try {
                                List<Path> filePaths = Files.walk(Paths.get(fileTier0.getPath()))
                                        .filter(Files::isRegularFile)
                                        .filter(p -> imagefilesExtensionFilter.accept(p.toFile()))
                                        .collect(Collectors.toList());

                                for (Path filePath : filePaths) {
                                    String relative = fileTier0.getParentFile().toURI().relativize(filePath.toUri()).getPath();
                                    removeMetaData(new File(filePath.toUri()), new File(dfFileChooser.getSelectedFile(), relative));
                                }
                            } catch (IOException err) {
                                System.out.println("Cannot load: " + fileTier0.getPath());
                            }
                        }
                        break;
                    default:
                        System.out.println("Process canceled by the user while selecting destination folder.");
                        break;
                }
                break;
            case JFileChooser.CANCEL_OPTION:
                System.out.println("Process canceled by the user while selecting the source folders/items.");
                break;
            case JFileChooser.ERROR_OPTION:
                System.out.println("Error while processing.");
                break;
        }
    }


    private void removeMetaData(File file, File outputFile) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image != null) {
                Matcher imageEndingMatcher = imageEndingPattern.matcher(file.getPath());
                if (imageEndingMatcher.find()) {
                    String foundFileEnding = imageEndingMatcher.group(1).replace(".", "").toUpperCase();

                    File parent = outputFile.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw new IllegalStateException("Couldn't create dir: " + parent);
                    }

                    Ext extAsEnum = Ext.valueOf(foundFileEnding);
                    switch (extAsEnum) {
                        case PNG:
                            ImageIO.write(image, Ext.PNG.name(), outputFile);
                            System.out.printf("Removed metadata from png file: %s and saved to %s\n", file, outputFile);
                            break;

                        case JPG:
                        case JPEG:
                            FileImageOutputStream output = new FileImageOutputStream(outputFile);

                            Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(foundFileEnding);
                            ImageWriter writer = iter.next();
                            ImageWriteParam iwp = writer.getDefaultWriteParam();
                            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            iwp.setCompressionQuality(1.0f);
                            writer.setOutput(output);
                            writer.write(null, new IIOImage(image, null, null), iwp);
                            writer.dispose();

                            System.out.printf("Removed metadata from jpg/jpeg file: %s and saved to %s\n", file, outputFile);
                            break;

                        default:
                            System.out.println("Not a supported image type: " + file);
                            break;
                    }
                }
            } else {
                System.err.println("Error while reading image, image read was null: " + file);
            }
        } catch (IOException e) {
            System.err.println("For file: " + file);
            e.printStackTrace();
        }
    }

}

