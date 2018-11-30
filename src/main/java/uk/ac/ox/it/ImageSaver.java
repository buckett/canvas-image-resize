package uk.ac.ox.it;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Just saves a copy of the image to the supplied directory.
 */
public class ImageSaver {

    private Path folder;

    public ImageSaver(File directory) throws IOException {
        if (directory != null) {
            Path path = directory.toPath();
            folder = Files.createDirectories(path);
        }
    }

    public void save(BufferedImage image, String format, String file) throws IOException {
        if (folder != null) {
            String filename = (file.contains(".")) ? file : file + "." + Utils.toExtension(format);
            Path path = folder.resolve(filename);
            if (!ImageIO.write(image, format, path.toFile())) {
                throw new IllegalArgumentException("Failed to find writer for: " + format);
            }
        }
    }

}


