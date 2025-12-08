package com.myorg.apiurlcomparison;

import com.myorg.apiurlcomparison.dto.ComparisonResult;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class HtmlReportGenerator {

    public static void generateReport(List<ComparisonResult> results, long totalDuration, String testType, String api1Url, String api2Url, File reportDir) {
        try {
            //InputStream is = HtmlReportGenerator.class.getClassLoader().getResourceAsStream("templates/report-template.mustache");
            InputStream is = HtmlReportGenerator.class.getResourceAsStream("/templates/report-template.mustache");
            if (is == null) {
                System.err.println("ERROR: Template 'templates/report-template.mustache' not found in classpath. HTML report cannot be generated.");
                return;
            }
            Template template = Mustache.compiler().compile(new InputStreamReader(is, StandardCharsets.UTF_8));

            Map<String, Object> data = new HashMap<>();
            long passedCount = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.MATCH).count();
            long failedCount = results.size() - passedCount;

            data.put("hasResults", !results.isEmpty());
            data.put("totalIterations", results.size());
            data.put("passedCount", passedCount);
            data.put("failedCount", failedCount);
            data.put("overallStatus", failedCount > 0 ? "FAIL" : "PASS");
            data.put("overallStatusCssClass", failedCount > 0 ? "status-fail" : "status-pass");
            data.put("totalTimeTaken", String.format("%,d ms", totalDuration));
            data.put("api1Url", api1Url);
            data.put("api2Url", api2Url); // The timestamp is added to the template data below
            data.put("generatedTimestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date()));


            List<Map<String, Object>> iterationData = new ArrayList<>();
            for (ComparisonResult result : results) {
                Map<String, Object> iterMap = new HashMap<>();
                iterMap.put("iteration", result.getIteration());
                iterMap.put("isSuccess", result.getStatus() == ComparisonResult.Status.MATCH);
                iterMap.put("status", result.getStatus());
                iterMap.put("error", String.join("\n", result.getDifferences()));

                String tokensUsed = result.getTokens().entrySet().stream()
                        .map(entry -> "<span class=\"token\"><strong>" + entry.getKey() + ":</strong> " + entry.getValue() + "</span>")
                        .collect(Collectors.joining(" "));
                iterMap.put("tokensUsed", tokensUsed);

                iterMap.put("api1Payload", result.getApi1Payload());
                iterMap.put("api1Response", result.getApi1Response());
                iterMap.put("api2Payload", result.getApi2Payload());
                iterMap.put("api2Response", result.getApi2Response());

                iterMap.put("api1Details", Map.of(
                        "url", result.getEndpoint(),
                        "headerEntries", result.getApi1Headers() != null ? result.getApi1Headers().entrySet() : Collections.emptySet()
                ));
                iterMap.put("api2Details", Map.of(
                        "url", result.getApi2Endpoint(),
                        "headerEntries", result.getApi2Headers() != null ? result.getApi2Headers().entrySet() : Collections.emptySet()
                ));

                iterationData.add(iterMap);
            }

            // Sort results: MISMATCH and ERROR statuses first, then MATCH
            iterationData.sort((o1, o2) -> {
                boolean isSuccess1 = (boolean) o1.get("isSuccess");
                boolean isSuccess2 = (boolean) o2.get("isSuccess");
                return Boolean.compare(isSuccess1, isSuccess2); // false (MISMATCH/ERROR) comes before true (MATCH)
            });

            // Add a sequential display index after sorting for the summary table
            for (int i = 0; i < iterationData.size(); i++) {
                iterationData.get(i).put("displayIndex", i + 1);
            }


            data.put("results", iterationData);

            File reportFile = new File(reportDir, "comparison-report.html");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(reportFile), StandardCharsets.UTF_8)) {
                template.execute(data, writer);
            }

            System.out.println("HTML report generated at " + reportFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
