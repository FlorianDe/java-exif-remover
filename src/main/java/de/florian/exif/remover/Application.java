package de.florian.exif.remover;

import de.florian.exif.remover.view.MainFrame;

import javax.swing.*;

public class Application {
    public static void main(String[] args) {
        //TODO Add args parsing to allow usage via CLI
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
