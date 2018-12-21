package uk.ac.ox.it;

import com.instructure.canvas.api.DepositApi;
import com.instructure.canvas.api.FilesApi;
import com.instructure.canvas.model.Deposit;
import com.instructure.canvas.model.Upload;
import feign.FeignException;
import feign.form.FormData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Attempts to upload an image to the course_image folder in the files area.
 */
public class ImageUploader {

    public static final String COURSE_IMAGE = "/course_image";
    public static final String RESIZED = "resized";
    private Logger log = LoggerFactory.getLogger(ImageUploader.class);
    private int uploaded;
    private int bytesUploaded;

    private FilesApi filesApi;
    private DepositApi depositApi;

    public ImageUploader(FilesApi filesApi, DepositApi depositApi) {
        this.filesApi = filesApi;
        this.depositApi = depositApi;
    }

    public Integer uploadResized(Integer courseId, byte[] image, String format) throws IOException {
        if (image.length == 0) {
            throw new IllegalArgumentException("Image can't be empty");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID can't be empty");
        }
        String filename = RESIZED + "." + Utils.toExtension(format);
        try {
            Upload upload = filesApi.uploadFile(courseId.toString(), filename, null, null, null, COURSE_IMAGE, "rename", Collections.emptyList());
            Map<String, Object> params = new TreeMap<>(new KeyFirstComparator());
            params.putAll(upload.getUploadParams());
            // If we don't specify the filename then "null" is put in the MIME boundary and then Canvas uses this
            // to overwrite the filename we supplied in the original upload.
            params.put("file", new FormData("image/" + Utils.toExtension(format), filename, image));

            Deposit deposit = depositApi.upload(new URI(upload.getUploadUrl()), params);
            // Unpublish our upload so it doesn't show up.
            // We also set the display name of our file.
            filesApi.updateFile(deposit.getId().toString(), null, null, null, null, null, true, null);
            uploaded++;
            bytesUploaded += image.length;
            return deposit.getId();

        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to create URI.", e);
        } catch (FeignException e) {
            throw new IOException("Failed upload image for: "+ courseId, e);
        }
    }

    public void cleanup() {
        log.info("Images uploaded: {}, Bytes uploaded: {}", uploaded, bytesUploaded);
    }


    /**
     * Needed to put the key as the first parameter in the map which is needed for AWS S3 uploads
     */
    public static class KeyFirstComparator implements Comparator<String> {

        public static final String KEY = "key";

        @Override
        public int compare(String o1, String o2) {
            if (KEY.equals(o1)) {
                return -1;
            }
            if (KEY.equals(o2)) {
                return 1;
            }
            return o1.compareTo(o2);
        }
    }

    public static class UploadFailedException extends RuntimeException {
        public UploadFailedException(String s, Exception e) {
            super(s,e);
        }
    }
}
