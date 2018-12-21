package uk.ac.ox.it;

import com.instructure.canvas.api.AccountsApi;
import com.instructure.canvas.api.CoursesApi;
import com.instructure.canvas.api.FilesApi;
import com.instructure.canvas.invoker.ApiClient;
import com.instructure.canvas.invoker.PagedList;
import com.instructure.canvas.model.Course;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static picocli.CommandLine.*;
import static picocli.CommandLine.Option;


/**
 * This attempts to resize course images to a sensible width/height.
 */
@Command(name = "resize", description = "Scans all course images and attempts to resize those that are too large")
public class ImageCheck implements Callable<Void> {

    private final Logger log = LoggerFactory.getLogger(ImageCheck.class);

    private ImageSaver originalSaver;
    private ImageSaver resizedSaver;
    private ImageResizer resizer;
    private CourseUpdater courseUpdater;
    private ImageUploader imageUploader;
    private ImageRetriever imageRetriever;
    private AccountsApi accountsApi;

    @Option(names = {"-c", "--config"}, description = "Filename to load config from.", required = true)
    private File configFile;

    @Option(names = {"-a", "--account"}, description = "Canvas account to process. (default: ${DEFAULT-VALUE})")
    private String accountId = "1";
    // These are the desired Canvas width and height for a course image
    @Option(names = {"-w", "--width"}, description = "Desired width of images. (default: ${DEFAULT-VALUE})")
    private int desiredWidth = 262;
    @Option(names = {"-h", "--height"}, description = "Desired height of images. (default: ${DEFAULT-VALUE})")
    private int desiredHeight = 146;
    @Option(names = {"-d", "--dry-run"}, description = "If set then don't upload/update course image. (default: ${DEFAULT-VALUE})")
    private boolean dryRun = false;
    @Option(names = {"-o", "--originals"}, description = "Directory to put original images in.")
    private File originalDirectory;
    @Option(names = {"-r", "--resized"}, description = "Directory to put resized images in.")
    private File resizedDirectory;
    @Option(names = {"-s", "--summary"}, description = "File containing summary CSV report on images and resizing. (use - for stdout)")
    private File summaryFile;
    @ParentCommand
    private Entrypoint entrypoint;


    public void init() {
        if (!configFile.exists()) {
            throw new MessageException("Cannot find config file: " + configFile.getPath());
        }
        if (!configFile.canRead()) {
            throw new MessageException("Cannot read config file: " + configFile.getPath());
        }
        if (!configFile.isFile()) {
            throw new MessageException("Configuration is not a file: " + configFile.getPath());
        }
        CanvasApiClientFactory factory = new CanvasApiClientFactory(configFile);
        factory.setDebug(entrypoint.debug);
        ApiClient client;
        try {
            client = factory.getClient();
        } catch (IllegalArgumentException e) {
            throw new MessageException("Configuration not valid: " + e.getMessage());
        }

        accountsApi = client.buildClient(AccountsApi.class);

        imageRetriever = new ImageRetriever();
        try {
            originalSaver = new ImageSaver(originalDirectory);
        } catch (IOException e) {
            throw new MessageException("Cannot store originals in: "+ originalDirectory+ " "+ e.getLocalizedMessage());
        }
        try {
            resizedSaver = new ImageSaver(resizedDirectory);
        } catch (IOException e) {
            throw new MessageException("Cannot store resized in: "+ resizedDirectory+ " "+ e.getLocalizedMessage());
        }
        resizer = new ImageResizer(desiredWidth, desiredHeight);

        imageUploader = new ImageUploader(client.buildClient(FilesApi.class), factory.getDepositApi());
        courseUpdater = new CourseUpdater(client.buildClient(CoursesApi.class));

        if (entrypoint.debug) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        }
    }

    public Void call() throws IOException {
        init();

        try {
            AccountsApi.ListActiveCoursesInAccountQueryParams params = new AccountsApi.ListActiveCoursesInAccountQueryParams();
            params.include(Collections.singletonList("course_image"));
            params.put("per_page", "100");
            List<Course> courses = accountsApi.listActiveCoursesInAccount(accountId, params);
            try (SummaryCsv summaryCsv = new SummaryCsv(summaryFile)) {
                do {
                    for (Course course : courses) {
                        try {
                            Object[] results = resizeCourseImage(course);
                            summaryCsv.record(results);
                        } catch (IOException ioe) {
                            log.info("Failed to process " + course.getId().toString() + " " + ioe.getMessage());
                        }
                    }
                    courses = nextPage(courses);
                } while (courses != null);

                imageRetriever.cleanup();
                resizer.cleanup();
                imageUploader.cleanup();
            }
        } catch (FeignException e) {
            throw new MessageException("Problem with request: " + e.getMessage(), e);
        }
        return null;
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
                    if (!dryRun) {
                        Integer uploadedFileId = imageUploader.uploadResized(course.getId(), imageBytes, loadedImage.format);
                        courseUpdater.updateImage(course.getId(), uploadedFileId);
                    }
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



    /**
     * Class to indicate it's an expected exception and we should just show the message to the user without the
     * whole stack trace.
     */
    static class MessageException extends RuntimeException {
        MessageException(String message) {
            super(message);
        }

        public MessageException(String message, Throwable e) {
            super(message,e);
        }
    }

}
