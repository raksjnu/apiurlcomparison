package com.myorg.apiurlcomparison;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

import static spark.Spark.*;

public class ApiUrlComparisonWeb {
    private static final Logger logger = LoggerFactory.getLogger(ApiUrlComparisonWeb.class);
    private static final int DEFAULT_PORT = 4567;

    public static void main(String[] args) {
        int port = findAvailablePort(DEFAULT_PORT);
        port(port);
        staticFiles.location("/public"); // Serve static files from resources/public

        logger.info("Starting Web GUI on port {}", port);

        // API Endpoint to running comparison
        post("/api/compare", (req, res) -> {
            res.type("application/json");
            try {
                ObjectMapper mapper = new ObjectMapper();
                Config config = mapper.readValue(req.body(), Config.class);

                logger.info("Received comparison request via Web GUI");

                ComparisonService service = new ComparisonService();
                List<ComparisonResult> results = service.execute(config);

                return mapper.writeValueAsString(results);
            } catch (Exception e) {
                logger.error("Error processing web request", e);
                res.status(500);
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        });

        // API Endpoint to get current config
        get("/api/config", (req, res) -> {
            res.type("application/json");
            try {
                // Try to load config.yaml from current directory
                java.io.File configFile = new java.io.File("config.yaml");
                if (configFile.exists()) {
                    ObjectMapper yamlMapper = new ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
                    ObjectMapper jsonMapper = new ObjectMapper();

                    Config config = yamlMapper.readValue(configFile, Config.class);
                    return jsonMapper.writeValueAsString(config);
                } else {
                    return "{}"; // Return empty if not found
                }
            } catch (Exception e) {
                logger.error("Error reading config", e);
                res.status(500);
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        });

        // Ensure server is started
        awaitInitialization();
        logger.info("Server started. Access at http://localhost:{}", port);

        // Open browser automatically
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("http://localhost:" + port));
            }
        } catch (Exception e) {
            logger.warn("Could not open browser automatically: {}", e.getMessage());
        }
    }

    private static int findAvailablePort(int startPort) {
        int port = startPort;
        while (port < 65535) {
            try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
                return port;
            } catch (java.io.IOException e) {
                logger.warn("Port {} is already in use, trying next...", port);
                port++;
            }
        }
        throw new RuntimeException("No available ports found starting from " + startPort);
    }
}
