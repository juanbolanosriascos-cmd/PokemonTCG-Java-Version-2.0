package gui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;

public class MediaUtils {

    private MediaUtils() {
    }

    public static ImageIcon loadIcon(String path, int maxWidth, int maxHeight) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        URL resourceUrl = MediaUtils.class.getResource(path);
        if (resourceUrl == null) {
            System.err.println("Icon resource not found: " + path);
            return null;
        }
        ImageIcon originalIcon = new ImageIcon(resourceUrl);
        int originalWidth = originalIcon.getIconWidth();
        int originalHeight = originalIcon.getIconHeight();
        if (originalWidth <= 0 || originalHeight <= 0) {
            return null;
        }
        double scale = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);
        Image scaledImage = originalIcon.getImage().getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    public static ImageIcon loadSprite(String path, int maxWidth, int maxHeight) {
        if (path == null || path.isEmpty() || maxWidth <= 0 || maxHeight <= 0) {
            return null;
        }
        URL resourceUrl = MediaUtils.class.getResource(path);
        if (resourceUrl == null) {
            System.err.println("Sprite resource not found: " + path);
            return null;
        }

        BufferedImage originalImage;
        try {
            originalImage = ImageIO.read(resourceUrl);
        } catch (IOException exception) {
            System.err.println("Unable to read sprite " + path + ": " + exception.getMessage());
            return null;
        }
        if (originalImage == null) {
            return null;
        }

        int minX = originalImage.getWidth();
        int minY = originalImage.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < originalImage.getHeight(); y++) {
            for (int x = 0; x < originalImage.getWidth(); x++) {
                int alpha = (originalImage.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 0) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return null;
        }

        BufferedImage cropped = originalImage.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
        double scale = Math.min((double) maxWidth / cropped.getWidth(), (double) maxHeight / cropped.getHeight());
        int scaledWidth = Math.max(1, (int) Math.round(cropped.getWidth() * scale));
        int scaledHeight = Math.max(1, (int) Math.round(cropped.getHeight() * scale));

        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaledImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(cropped, 0, 0, scaledWidth, scaledHeight, null);
        } finally {
            graphics.dispose();
        }
        return new ImageIcon(scaledImage);
    }

    public static void playAudio(String audioPath) {
        if (audioPath == null || audioPath.isEmpty()) {
            return;
        }
        URL audioResource = MediaUtils.class.getResource(audioPath);
        if (audioResource == null) {
            System.err.println("Audio resource not found: " + audioPath);
            return;
        }
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioResource)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException exception) {
            System.err.println("Unable to play audio " + audioPath + ": " + exception.getMessage());
        }
    }
}