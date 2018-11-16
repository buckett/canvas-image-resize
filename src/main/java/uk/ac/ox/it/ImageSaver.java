package uk.ac.ox.it;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageSaver {

    private final Path folder;

    public ImageSaver(String directory) throws IOException {
        Path path = Paths.get(directory);
        folder = Files.createDirectories(path);
    }

    public void save(BufferedImage image, String format, String file) throws IOException {
        String filename = (file.contains(".")) ? file : file + "."+ Utils.toExtension(format);
        Path path = folder.resolve(filename);
        if(!ImageIO.write(image, format, path.toFile())) {
            throw new IllegalArgumentException("Failed to find writer for: "+ format);
        }
    }

}


