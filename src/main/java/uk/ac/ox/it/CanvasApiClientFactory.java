package uk.ac.ox.it;

import com.instructure.canvas.api.DepositApi;
import com.instructure.canvas.invoker.ApiClient;
import com.instructure.canvas.invoker.auth.ApiKeyAuth;
import feign.Feign;
import feign.Logger;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CanvasApiClientFactory {

    private Properties properties;
    private boolean debug;

    public CanvasApiClientFactory(String resource) {
        InputStream resourceAsStream = getClass().getResourceAsStream(resource);
        if (resourceAsStream == null) {
            throw new IllegalArgumentException("Failed to find configuration resource: "+ resource);
        }
        load(resourceAsStream);
    }

    public CanvasApiClientFactory(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            load(fileInputStream);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Failed to find configuration file: "+file.getAbsolutePath());
        }
    }

    public CanvasApiClientFactory(InputStream in) {
        load(in);
    }

    private void load(InputStream in) {
        ApiClient apiClient = new ApiClient();
        try {
            properties = new Properties();
            properties.load(in);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration");
        }
    }

    public ApiClient getClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.addAuthorization("OAUTH", new ApiKeyAuth("header", "Authorization"));
        apiClient.setApiKey("Bearer "+ getOrException(properties, "canvas.token"));
        apiClient.setBasePath(getOrException(properties, "canvas.url"));
        if (debug) {
            apiClient.getFeignBuilder().logLevel(Logger.Level.FULL);
        }
        return apiClient;
    }

    public DepositApi getDepositApi() {
        Feign.Builder builder = Feign.builder().encoder(new FormEncoder())
                // Have to use the OkHttpClient as the standard one can't follow the redirect.
                .client(new OkHttpClient())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger());
        if (debug) {
            builder.logLevel(Logger.Level.FULL);
        }
        // When we upload files we have a complete URL so we don't need a URL.
        return builder.target(DepositApi.class, "http://not-used/");
    }


    private String getOrException(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("The key "+ key+ " must be set in the configuration");
        }
        return value;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
