package ru.itq.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.itq.api.dto.CreateDocumentRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.logging.Logger;

public class DocumentGeneratorApp {

    private static final Logger LOG = Logger.getLogger(DocumentGeneratorApp.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        Properties props = loadConfig(args);
        int count = Integer.parseInt(props.getProperty("generator.count", "100"));
        String baseUrl = props.getProperty("generator.base-url", "http://localhost:8080/api/documents");

        LOG.info("Requested " + count + " documents, target URL: " + baseUrl);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        long totalStart = System.currentTimeMillis();
        int successCount = 0;
        int errorCount = 0;

        for (int i = 1; i <= count; i++) {
            try {
                CreateDocumentRequest request = new CreateDocumentRequest(
                        "Generator",
                        "Document #" + i,
                        "generator-util"
                );

                String body = MAPPER.writeValueAsString(request);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201) {
                    successCount++;
                } else {
                    errorCount++;
                    LOG.warning("Document #" + i + " creation failed: HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                errorCount++;
                LOG.severe("Document #" + i + " error: " + e.getMessage());
            }

            if (i % 100 == 0 || i == count) {
                LOG.info("Progress: created " + i + " of " + count
                        + " (success=" + successCount + ", errors=" + errorCount + ")");
            }
        }

        long totalElapsed = System.currentTimeMillis() - totalStart;
        LOG.info("Completed: created " + successCount + " of " + count
                + " documents in " + totalElapsed + " ms"
                + " (errors=" + errorCount + ")");
    }

    private static Properties loadConfig(String[] args) throws IOException {
        Properties props = new Properties();

        if (args.length > 0) {
            try (InputStream is = java.nio.file.Files.newInputStream(java.nio.file.Path.of(args[0]))) {
                props.load(is);
                LOG.info("Loaded config from: " + args[0]);
                return props;
            }
        }

        try (InputStream is = DocumentGeneratorApp.class.getClassLoader()
                .getResourceAsStream("generator-config.properties")) {
            if (is != null) {
                props.load(is);
                LOG.info("Loaded default config from classpath");
            } else {
                LOG.warning("No config found, using defaults: count=100, url=http://localhost:8080/api/documents");
                props.setProperty("generator.count", "100");
                props.setProperty("generator.base-url", "http://localhost:8080/api/documents");
            }
        }
        return props;
    }
}
