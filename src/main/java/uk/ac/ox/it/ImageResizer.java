package uk.ac.ox.it;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * This resizes the image to the correct size if both dimensions are larger than the desired size.
 */
public class ImageResizer {

    private Logger log = LoggerFactory.getLogger(ImageResizer.class);

    private final int desiredWidth;
    private final int desiredHeight;

    private int images;
    private int resized;

    public ImageResizer(int width, int height) {
        this.desiredWidth = width;
        this.desiredHeight = height;
    }

    public BufferedImage resize(BufferedImage source, String format) throws IOException {
        images++;
        if (desiredHeight < source.getHeight() && desiredWidth < source.getWidth()) {
            Thumbnails.Builder<BufferedImage> thumbnailBuilder = Thumbnails.of(source).outputFormat(format);
            thumbnailBuilder.crop(Positions.CENTER);
            thumbnailBuilder.size(desiredWidth, desiredHeight);
            BufferedImage destination = thumbnailBuilder.asBufferedImage();
            resized++;
            return destination;
        }
        return null;
    }

    public void cleanup() {
        log.info("Images handled: {} Images resized: {}", images, resized);

    }
}
