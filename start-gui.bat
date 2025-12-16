@echo off
echo Starting API Comparison Tool Web GUI...
echo.

call mvn clean compile exec:java -Dexec.mainClass="com.raks.apiurlcomparison.ApiUrlComparisonWeb"

pause
