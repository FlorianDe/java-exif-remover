package de.florian.exif.remover;

public class Application {
    public static void main(String[] args) {
        //TODO Add args parsing to allow usage via CLI
        ExifRemover exifRemover = new ExifRemover();
        exifRemover.run();
    }
}
