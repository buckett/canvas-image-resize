package uk.ac.ox.it;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageResizer {

    private final int desiredWidth;
    private final int desiredHeight;

    public ImageResizer(int width, int height) {
        this.desiredWidth = width;
        this.desiredHeight = height;
    }

    public BufferedImage resize(BufferedImage source, String format) throws IOException {
        if (desiredHeight < source.getHeight() && desiredWidth < source.getWidth()) {
            Thumbnails.Builder<BufferedImage> thumbnailBuilder = Thumbnails.of(source).outputFormat(format);
            thumbnailBuilder.crop(Positions.CENTER);
            thumbnailBuilder.size(desiredWidth, desiredHeight);
            BufferedImage destination = thumbnailBuilder.asBufferedImage();
            return destination;
        }
        return null;
    }
}
