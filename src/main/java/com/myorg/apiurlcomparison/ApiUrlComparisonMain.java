package com.myorg.apiurlcomparison;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.myorg.apiurlcomparison.http.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "apiurlcomparison", mixinStandardHelpOptions = true, version = "1.0")
public class ApiUrlComparisonMain implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ApiUrlComparisonMain.class);

    @CommandLine.Option(names = { "-c",
            "--config" }, description = "Path to the configuration YAML file", required = true)
    private File configFile;

    @CommandLine.Option(names = { "-o",
            "--output" }, description = "Path for the output JSON report", defaultValue = "results.json")
    private String outputReportPath;

    @Override
    public Integer call() throws Exception {
        logger.info("Starting API URL Comparison Tool...");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        // 1. Load Configuration
        Config config = mapper.readValue(configFile, Config.class);
        logger.info("Configuration loaded successfully from: {}", configFile.getAbsolutePath());

        // 2. Prepare for iterations (Cartesian product of tokens)
        logger.info("Generating iterations with strategy: {}", config.getIterationController());
        List<Map<String, Object>> iterations = TestDataGenerator.generate(
                config.getTokens(),
                config.getMaxIterations(),
                config.getIterationController());
        logger.info("Generated {} iterations based on tokens.", iterations.size());

        List<ComparisonResult> allResults = new ArrayList<>();
        int iterationCount = 0;

        for (Map<String, Object> currentTokens : iterations) {
            iterationCount++;
            logger.info("Running iteration {}: {}", iterationCount, currentTokens);

            try {
                // Determine API type and process
                if ("REST".equalsIgnoreCase(config.getTestType())) {
                    processApis(config.getRestApis(), currentTokens, allResults, config.getTestType());
                } else if ("SOAP".equalsIgnoreCase(config.getTestType())) {
                    processApis(config.getSoapApis(), currentTokens, allResults, config.getTestType());
                } else {
                    logger.error("Invalid testType specified in config: {}", config.getTestType());
                    return 1;
                }
            } catch (Exception e) {
                logger.error("Error during iteration {}: {}", iterationCount, e.getMessage(), e);
            }
        }

        // 3. Generate Report
        try {
            logger.info("Generating JSON report...");
            File jsonReportFile = HtmlReportGenerator.generateJsonReport(allResults, outputReportPath);
            logger.info("JSON report generated successfully: {}", jsonReportFile.getAbsolutePath());

            logger.info("Generating HTML report...");
            File htmlReportFile = HtmlReportGenerator.generateHtmlReport(allResults, outputReportPath);
            logger.info("HTML report generated successfully: {}", htmlReportFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to generate report(s): {}", e.getMessage(), e);
        }

        return 0; // Indicate success
    }

    private void processApis(Map<String, ApiConfig> apis, Map<String, Object> currentTokens,
            List<ComparisonResult> allResults, String apiType) {
        if (apis == null || apis.isEmpty()) {
            logger.warn("No {} APIs configured.", apiType);
            return;
        }

        // This tool is designed for comparison, so we expect api1 and api2.
        ApiConfig api1Config = apis.get("api1");
        ApiConfig api2Config = apis.get("api2");

        if (api1Config == null || api2Config == null) {
            logger.error("Comparison requires both 'api1' and 'api2' to be configured for the test type '{}'.",
                    apiType);
            return;
        }

        // Assuming operations should be matched by name for comparison
        for (Operation op1 : api1Config.getOperations()) {
            Operation op2 = api2Config.getOperations().stream()
                    .filter(o -> op1.getName().equals(o.getName()))
                    .findFirst()
                    .orElse(null);

            if (op2 == null) {
                logger.warn("No matching operation named '{}' found in api2. Skipping comparison for this operation.",
                        op1.getName());
                continue;
            }

            // Assuming we compare the first method found in each matched operation
            String method1 = op1.getMethods().get(0);
            String method2 = op2.getMethods().get(0);

            ComparisonResult result = new ComparisonResult();
            result.setOperationName(op1.getName());
            result.setIterationTokens(new HashMap<>(currentTokens));

            ApiClient client1 = new ApiClient(api1Config.getAuthentication());
            ApiClient client2 = new ApiClient(api2Config.getAuthentication());

            ApiCallResult api1CallResult = new ApiCallResult();
            ApiCallResult api2CallResult = new ApiCallResult();
            result.setApi1(api1CallResult);
            result.setApi2(api2CallResult);

            try {
                String url1 = "SOAP".equalsIgnoreCase(apiType) ? api1Config.getBaseUrl()
                        : api1Config.getBaseUrl() + op1.getPath();
                String url2 = "SOAP".equalsIgnoreCase(apiType) ? api2Config.getBaseUrl()
                        : api2Config.getBaseUrl() + op2.getPath();

                // Populate API 1 details before the call
                api1CallResult.setUrl(url1);
                api1CallResult.setMethod(method1);
                api1CallResult.setRequestHeaders(op1.getHeaders());
                String payload1 = null;
                if (op1.getPayloadTemplatePath() != null && !op1.getPayloadTemplatePath().isEmpty()) {
                    PayloadProcessor processor1 = new PayloadProcessor(op1.getPayloadTemplatePath(), apiType);
                    payload1 = processor1.process(currentTokens);
                }
                api1CallResult.setRequestPayload(payload1);

                // Populate API 2 details before the call
                api2CallResult.setUrl(url2);
                api2CallResult.setMethod(method2);
                api2CallResult.setRequestHeaders(op2.getHeaders());
                String payload2 = null;
                if (op2.getPayloadTemplatePath() != null && !op2.getPayloadTemplatePath().isEmpty()) {
                    PayloadProcessor processor2 = new PayloadProcessor(op2.getPayloadTemplatePath(), apiType);
                    payload2 = processor2.process(currentTokens);
                }
                api2CallResult.setRequestPayload(payload2);

                // Execute API 1 Call
                long start1 = System.currentTimeMillis();
                String response1 = client1.sendRequest(url1, method1, op1.getHeaders(), payload1);
                api1CallResult.setDuration(System.currentTimeMillis() - start1);
                api1CallResult.setResponsePayload(response1);

                // Execute API 2 Call
                long start2 = System.currentTimeMillis();
                String response2 = client2.sendRequest(url2, method2, op2.getHeaders(), payload2);
                api2CallResult.setDuration(System.currentTimeMillis() - start2);
                api2CallResult.setResponsePayload(response2);

                // Perform comparison
                ComparisonEngine.compare(result, apiType);

            } catch (Exception e) {
                logger.error("Error during operation '{}' comparison: {}", op1.getName(), e.getMessage());
                result.setErrorMessage("Operation failed: " + e.getMessage());
                result.setStatus(ComparisonResult.Status.ERROR);
            }
            allResults.add(result);
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ApiUrlComparisonMain()).execute(args);
        System.exit(exitCode);
    }
}
