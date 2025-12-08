package com.myorg.apiurlcomparison;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class Config {

    @JsonProperty("testType")
    private String testType;

    @JsonProperty("rest")
    private Map<String, ApiConfig> restApis;

    @JsonProperty("soap")
    private Map<String, ApiConfig> soapApis;

    @JsonProperty("maxIterations")
    private int maxIterations;

    @JsonProperty("tokens")
    private Map<String, List<Object>> tokens;

    @JsonProperty("iterationController")
    private String iterationController;

    // Getters
    public String getTestType() {
        return testType;
    }

    public String getIterationController() {
        return iterationController;
    }

    public Map<String, ApiConfig> getRestApis() {
        return restApis;
    }

    public Map<String, ApiConfig> getSoapApis() {
        return soapApis;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public Map<String, List<Object>> getTokens() {
        return tokens;
    }

    // Setters (optional, but good for Jackson deserialization)
    public void setTestType(String testType) {
        this.testType = testType;
    }

    public void setIterationController(String iterationController) {
        this.iterationController = iterationController;
    }

    public void setRestApis(Map<String, ApiConfig> restApis) {
        this.restApis = restApis;
    }

    public void setSoapApis(Map<String, ApiConfig> soapApis) {
        this.soapApis = soapApis;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public void setTokens(Map<String, List<Object>> tokens) {
        this.tokens = tokens;
    }
}