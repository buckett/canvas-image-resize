package uk.ac.ox.it;

import com.instructure.canvas.api.AccountsApi;
import com.instructure.canvas.api.CoursesApi;
import com.instructure.canvas.api.FilesApi;
import com.instructure.canvas.invoker.ApiClient;
import com.instructure.canvas.invoker.PagedList;
import com.instructure.canvas.model.Course;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;


/**
 * This attempts to resize course images to a sensible width/height.
 */
public class ImageCheck {

    private final Logger log = LoggerFactory.getLogger(ImageCheck.class);

    private final ImageSaver originalSaver;
    private final ImageSaver resizedSaver;
    private final ImageResizer resizer;
    private final CourseUpdater courseUpdater;
    private final ImageUploader imageUploader;
    private final ImageRetriever imageRetriever;


    // These are the desired Canvas width and height for a course image
    private int desiredWidth = 262;
    private int desiredHeight = 146;

    private AccountsApi accountsApi;

    public ImageCheck() throws IOException {
        CanvasApiClientFactory factory = new CanvasApiClientFactory(new File("test.properties"));
        ApiClient client = factory.getClient();

        accountsApi = client.buildClient(AccountsApi.class);
        imageRetriever = new ImageRetriever();
        originalSaver = new ImageSaver("images/originals");
        resizedSaver = new ImageSaver("images/resized");
        resizer = new ImageResizer(desiredWidth, desiredHeight);
        imageUploader = new ImageUploader(client.buildClient(FilesApi.class), factory.getDepositApi());
        courseUpdater = new CourseUpdater(client.buildClient(CoursesApi.class));
    }

    public static void main(String... args) throws IOException, URISyntaxException {
        new ImageCheck().run();
    }

    public void run() throws IOException {

        AccountsApi.ListActiveCoursesInAccountQueryParams params = new AccountsApi.ListActiveCoursesInAccountQueryParams();
        params.include(Collections.singletonList("course_image"));
        params.put("per_page", "100");
        List<Course> courses = accountsApi.listActiveCoursesInAccount("1", params);
        try (CSVPrinter csvPrinter = new CSVPrinter(new PrintStream(System.out), CSVFormat.EXCEL)) {
            csvPrinter.printRecord("Course Name", "ID", "Original Size", "New Size");
            do {
                for (Course course : courses) {
                    try {
                        Object[] results = resizeCourseImage(course);
                        if (results != null) {
                            csvPrinter.printRecord(results);
                        }
                    } catch (IOException ioe) {
                        log.info("Failed to process " + course.getId().toString() + " " + ioe.getMessage());
                    }
                }
                courses = nextPage(courses);
            } while (courses != null);
        }
    }

    private Object[] resizeCourseImage(Course course) throws IOException {
        if (course.getImageDownloadUrl() != null) {
            ImageRetriever.LoadedImage loadedImage = imageRetriever.loadImage(course.getImageDownloadUrl());
            if (loadedImage != null) {

                BufferedImage resizedImage = resizer.resize(loadedImage.image, loadedImage.format);
                if (resizedImage != null) {
                    String courseId = course.getId().toString();
                    originalSaver.save(loadedImage.image, loadedImage.format, courseId);
                    resizedSaver.save(resizedImage, loadedImage.format, courseId);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ImageIO.write(resizedImage, loadedImage.format, bos);
                    byte[] imageBytes = bos.toByteArray();
                    Integer uploadedFileId = imageUploader.uploadResized(course.getId(), imageBytes, loadedImage.format);
                    courseUpdater.updateImage(course.getId(), uploadedFileId);
                    return new Object[]{course.getName(), courseId, loadedImage.size, imageBytes.length};
                }
            }
        }
        return null;
    }

    public List<Course> nextPage(List<Course> courses) {
        if (courses instanceof PagedList) {
            PagedList<Course> pagedCourses = (PagedList<Course>) courses;
            if (pagedCourses.hasNext()) {
                try {
                    return accountsApi.listActiveCoursesInAccount(new URI(pagedCourses.next()));
                } catch (URISyntaxException e) {
                    // Ignore
                }
            }
        }
        return null;
    }

}
