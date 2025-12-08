package com.myorg.apiurlcomparison;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.builder.DiffBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import java.util.Iterator;
import java.util.stream.Collectors;

public class ComparisonEngine {

    private static final Logger logger = LoggerFactory.getLogger(ComparisonEngine.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void compare(ComparisonResult result, String apiType) {
        ApiCallResult api1Result = result.getApi1();
        ApiCallResult api2Result = result.getApi2();

        if (api1Result == null || api2Result == null) {
            result.setStatus(ComparisonResult.Status.ERROR);
            result.setErrorMessage("One or both API calls failed, cannot compare.");
            return;
        }

        String response1 = api1Result.getResponsePayload();
        String response2 = api2Result.getResponsePayload();

        if (response1 == null || response2 == null) {
            result.setStatus(ComparisonResult.Status.ERROR);
            result.setErrorMessage("One or both API responses are null.");
            return;
        }

        boolean isMatch;
        List<String> differences = new ArrayList<>();

        try {
            if ("SOAP".equalsIgnoreCase(apiType)) {
                Diff xmlDiff = DiffBuilder.compare(response1).withTest(response2).ignoreWhitespace().build();
                isMatch = !xmlDiff.hasDifferences();
                if (!isMatch) {
                    differences = StreamSupport.stream(xmlDiff.getDifferences().spliterator(), false)
                            .limit(5) // Limit to 5 differences for readability
                            .map(Difference::toString).collect(Collectors.toList());
                }
            } else { // Default to JSON comparison for REST
                JsonNode json1 = objectMapper.readTree(response1);
                JsonNode json2 = objectMapper.readTree(response2);
                isMatch = json1.equals(json2);
                if (!isMatch) {
                    differences = detailedJsonDiff(json1, json2, "$");
                }
            }

            result.setStatus(isMatch ? ComparisonResult.Status.MATCH : ComparisonResult.Status.MISMATCH);
            if (!isMatch) {
                result.setDifferences(differences);
            }
        } catch (IOException e) {
            logger.error("Failed to parse or compare responses", e);
            result.setStatus(ComparisonResult.Status.ERROR);
            result.setErrorMessage("Error during response comparison: " + e.getMessage());
        }
    }

    private static List<String> detailedJsonDiff(JsonNode node1, JsonNode node2, String path) {
        List<String> differences = new ArrayList<>();

        if (node1.isObject() && node2.isObject()) {
            ObjectNode obj1 = (ObjectNode) node1;
            ObjectNode obj2 = (ObjectNode) node2;
            Iterator<String> fieldNames = obj1.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                String currentPath = path + "." + fieldName;
                if (obj2.has(fieldName)) {
                    differences.addAll(detailedJsonDiff(obj1.get(fieldName), obj2.get(fieldName), currentPath));
                } else {
                    differences.add("Missing field in API 2: " + currentPath);
                }
            }

            obj2.fieldNames().forEachRemaining(fieldName -> {
                if (!obj1.has(fieldName)) {
                    String currentPath = path + "." + fieldName;
                    differences.add("Missing field in API 1: " + currentPath);
                }
            });

        } else if (node1.isArray() && node2.isArray()) {
            ArrayNode array1 = (ArrayNode) node1;
            ArrayNode array2 = (ArrayNode) node2;
            int len1 = array1.size();
            int len2 = array2.size();
            int maxLength = Math.max(len1, len2);

            for (int i = 0; i < maxLength; i++) {
                String currentPath = path + "[" + i + "]";
                if (i < len1 && i < len2) {
                    differences.addAll(detailedJsonDiff(array1.get(i), array2.get(i), currentPath));
                } else if (i < len1) {
                    differences.add("Missing element in API 2: " + currentPath);
                } else {
                    differences.add("Missing element in API 1: " + currentPath);
                }
            }
        } else if (!node1.equals(node2)) {
            differences.add("Values differ at " + path + ". API 1: " + node1.asText() + ", API 2: " + node2.asText());
        }

        return differences;
    }





}