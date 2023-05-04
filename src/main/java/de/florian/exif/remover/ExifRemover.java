package de.florian.exif.remover;


import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExifRemover {

    public enum Ext {
        PNG,
        JPG,
        JPEG;

        String regex() {
            return "\\." + this.name();
        }

        public static String getOrRegex() {
            return "(" + Arrays.stream(values()).map(Ext::regex).collect(Collectors.joining("|")) + ")";
        }

        public static String[] getNames() {
            return Arrays.stream(values()).map(Ext::name).toArray(String[]::new);
        }

        public static String getExtensionDescription() {
            return "(" + Arrays.stream(values()).map(Ext::name).collect(Collectors.joining(", ")) + ")";
        }
    }

    private final Pattern imageEndingPattern = Pattern.compile(Ext.getOrRegex(), Pattern.CASE_INSENSITIVE);


    public Stream<File> traverseFile(File file, Predicate<? super File> filter) throws IOException {
        Predicate<? super File> fileFilter = Optional.of(filter).orElse((f) -> true);
        return Files.walk(Paths.get(file.getPath()))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(fileFilter);
    }

    public long count(File[] files, Predicate<? super File> filter) throws IOException {
        return Arrays.stream(files).reduce(0L, (acc, file) -> {
            try {
                System.out.println("Counting files in " + file.getPath());
                return acc + this.count(file, filter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, Long::sum);
    }

    public long count(File file, Predicate<? super File> filter) throws IOException {
        return this.traverseFile(file, filter).count();
    }

    public void copy(File[] files, File destination, Predicate<? super File> filter, boolean flattenFiles, BiConsumer<File, File> fileCopiedCallback) throws IOException {
        for (File fileTier0 : files) {
            this.traverseFile(fileTier0, filter).forEach((file) -> {
                String relative = fileTier0.getParentFile().toURI().relativize(file.toURI()).getPath();
                File src = new File(file.toURI());
                File target;
                if (flattenFiles) {
                    target = new File(destination, file.getName());
                } else {
                    target = new File(destination, relative);
                }
                copyImageWithoutMetaData(src, target);
                fileCopiedCallback.accept(src, target);
            });
        }
    }

    private void copyImageWithoutMetaData(File file, File outputFile) {
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
                            try (FileImageOutputStream output = new FileImageOutputStream(outputFile)) {
                                Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(foundFileEnding);
                                ImageWriter writer = iter.next();
                                ImageWriteParam iwp = writer.getDefaultWriteParam();
                                iwp.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
//                            iwp.setTilingMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
//                            iwp.setProgressiveMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
//                            iwp.setCompressionQuality(1.0f);
                                writer.setOutput(output);
                                writer.prepareWriteSequence(null);
                                writer.write(null, new IIOImage(image, null, null), iwp);
                                writer.endWriteSequence();
                                writer.dispose();
                            }

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

