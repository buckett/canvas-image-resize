package uk.ac.ox.it;

import org.apache.commons.io.input.CountingInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

/**
 * This downloads an image and reads in the image.
 */
public class ImageRetriever {

    private Logger log = LoggerFactory.getLogger(ImageRetriever.class);

    private int imageCount;
    private int unprocessed;
    private int badUrl;

    public LoadedImage loadImage(String imageUrl) {
        try {
            imageCount++;
            URL url = new URL(imageUrl);
            LoadedImage loadedImage = new LoadedImage();
            try (CountingInputStream in = new CountingInputStream(url.openStream())) {

                ImageInputStream imageStream = ImageIO.createImageInputStream(in);
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
                if (!readers.hasNext()) {
                    throw new IOException("Unknown format.");
                }
                ImageReader reader = readers.next();
                loadedImage.format = reader.getFormatName();

                reader.setInput(imageStream, true);
                BufferedImage image = reader.read(0);
                reader.dispose();
                loadedImage.image = image;
                loadedImage.size = in.getCount();
                return loadedImage;

            } catch (IIOException e) {
                log.debug("Problem reading image");
                unprocessed++;
            }
        } catch (MalformedURLException e) {
            log.debug("Bad URL: " + imageUrl);
            badUrl++;
        } catch (IOException e) {
            log.debug("Failed to download: " + imageUrl);
        }
        return null;
    }

    public void cleanup() {
        log.info("Images: {}, Bad URLs: {}, Unprocessed: {}", imageCount, badUrl, unprocessed);
    }

    public static class LoadedImage {
        BufferedImage image;
        String format;
        Integer size;
    }

}
