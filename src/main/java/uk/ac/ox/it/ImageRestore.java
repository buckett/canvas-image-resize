package uk.ac.ox.it;

import com.instructure.canvas.api.CoursesApi;
import com.instructure.canvas.api.FilesApi;
import com.instructure.canvas.invoker.ApiClient;
import feign.FeignException;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.ParentCommand;

@Command(name = "restore", description = "Uploads course images to courses.")
public class ImageRestore implements Callable<Void> {

    private ImageUploader imageUploader;
    private CourseUpdater courseUpdater;

    @Option(names = {"-c", "--config"}, description = "Filename to load config from.", required = true)
    private File configFile;

    @Option(names = {"-d", "--directory"}, description = "Directory containing images to upload", required = true)
    private Path imageDirectory;

    @ParentCommand
    private Entrypoint entrypoint;

    public void init() {
        if (!configFile.exists()) {
            throw new ImageCheck.MessageException("Cannot find config file: " + configFile.getPath());
        }
        if (!configFile.canRead()) {
            throw new ImageCheck.MessageException("Cannot read config file: " + configFile.getPath());
        }
        if (!configFile.isFile()) {
            throw new ImageCheck.MessageException("Configuration is not a file: " + configFile.getPath());
        }
        CanvasApiClientFactory factory = new CanvasApiClientFactory(configFile);
        factory.setDebug(entrypoint.debug);
        ApiClient client;
        try {
            client = factory.getClient();
        } catch (IllegalArgumentException e) {
            throw new ImageCheck.MessageException("Configuration not valid: " + e.getMessage());
        }

        imageUploader = new ImageUploader(client.buildClient(FilesApi.class), factory.getDepositApi());
        courseUpdater = new CourseUpdater(client.buildClient(CoursesApi.class));
    }


    public Void call() throws IOException {
        init();
        Pattern pattern = Pattern.compile("(?<id>\\d+)\\.(?<extension>gif|jpg|png)");
        if (Files.isDirectory(imageDirectory)) {

            try (Stream<Path> paths = Files.list(imageDirectory)) {
                paths.forEach(p -> {
                    Matcher matcher = pattern.matcher(p.getFileName().toString());
                    if (matcher.matches()) {
                        Integer courseId = Integer.parseInt(matcher.group("id"));
                        String extension = matcher.group("extension");
                        try {
                            byte[] bytes = Files.readAllBytes(p);
                            try {
                                Integer fileId = imageUploader.uploadResized(courseId, bytes, extension);
                                try {
                                    courseUpdater.updateImage(courseId, fileId);
                                } catch(FeignException e) {
                                    System.out.println("Failed to reset course image on: "+courseId+ ", "+ e.getMessage());
                                }
                            } catch (IOException e) {
                                System.out.println("Failed to upload image: "+ e.getMessage());
                            }
                        } catch (IOException e) {
                            System.out.println("Failed to read: "+ p);
                        }
                    } else {
                        System.out.println("Ignored file: "+ p);
                    }
                });
            }
        } else {
            throw new ImageCheck.MessageException("Couldn't find directory: "+ imageDirectory);
        }
        return null;
    }
}
