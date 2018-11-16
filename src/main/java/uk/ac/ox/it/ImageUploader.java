package uk.ac.ox.it;

import com.instructure.canvas.api.DepositApi;
import com.instructure.canvas.api.FilesApi;
import com.instructure.canvas.model.Deposit;
import com.instructure.canvas.model.Upload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImageUploader {

    public static final String COURSE_IMAGE = "/course_image";
    public static final String RESIZED = "resized";
    private FilesApi filesApi;
    private DepositApi depositApi;

    public ImageUploader(FilesApi filesApi, DepositApi depositApi) {
        this.filesApi = filesApi;
        this.depositApi = depositApi;
    }

    public Integer uploadResized(Integer courseId, byte[] image, String format) throws IOException {
        String filename = RESIZED + "." + Utils.toExtension(format);
        Upload upload = filesApi.uploadFile(courseId.toString(), filename , null, null, null, COURSE_IMAGE, null, Collections.emptyList());
        Map<String, Object> params = new LinkedHashMap<>();
        // Have to sort the params.
        params.put("key", upload.getUploadParams().remove("key"));
        params.putAll(upload.getUploadParams());
        params.put("file", image);

        try {
            Deposit deposit = depositApi.upload(new URI(upload.getUploadUrl()), params);
            // Unpublish our upload so it doesn't show up.
            filesApi.updateFile(deposit.getId().toString(), null, null, null, null, null, true, null);
            return deposit.getId();

        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to create URI.", e);
        }
    }
}
