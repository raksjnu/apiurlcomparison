# API Response Comparison Tool - APITestingGuard

A powerful Java-based tool for comparing API responses between two endpoints with support for both REST and SOAP APIs. Features include automated iteration testing, detailed comparison reports, and both CLI and GUI interfaces.

## Features

- **Dual Interface**: Command-line (CLI) and Web-based GUI
- **Multiple API Types**: Support for REST (JSON) and SOAP (XML) APIs
- **Flexible Iteration Strategies**:
  - `ONE_BY_ONE`: Tests baseline + each token value individually (efficient)
  - `ALL_COMBINATIONS`: Tests all possible token combinations (exhaustive)
- **Original Payload Testing**: Always executes the original payload as-is before token replacements
- **Detailed Comparison Reports**: 
  - JSON format for programmatic processing
  - HTML format for visual review
  - GUI dashboard with real-time results
- **Token-Based Testing**: Dynamic payload generation with variable substitution
- **Authentication Support**: Basic authentication for secured endpoints
- **Mock API Server**: Built-in mock server for testing and development

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Installation

1. Clone or download the project
2. Navigate to the project directory
3. Build the project:
   ```bash
   mvn clean package
   ```

## Usage

### Option 1: Command-Line Interface (CLI)

Run comparisons from the command line and generate reports:

```bash
java -jar target/apiurlcomparison-1.0.0.jar --config config.yaml --output ./reports
```

**CLI Arguments:**
- `--config`: Path to the YAML configuration file (required)
- `--output`: Directory where reports will be generated (required)

**Generated Reports:**
- `results.json`: Machine-readable JSON report
- `results.html`: Human-readable HTML report with detailed comparison

### Option 2: Web-Based GUI

Launch the interactive web interface:

```bash
java -jar target/apiurlcomparison-1.0.0.jar
```

The GUI will automatically open in your default browser at `http://localhost:4567`

**Quick Start Scripts:**
- **Windows**: Double-click `start-gui.bat` or run `start-gui.bat` from command prompt
- **macOS/Linux**: Run `./start-gui.sh` from terminal (make executable first: `chmod +x start-gui.sh`)

**GUI Features:**
- Interactive configuration form with default SOAP type
- Real-time execution dashboard
- Visual comparison results with professional purple theme
- Expandable iteration details with horizontal scrolling
- XML/SOAP pretty-printing for better readability
- Timestamp with timezone in execution summary
- Full-width responsive layout

### Option 3: Mock API Server (For Testing)

Run the built-in mock server to simulate API responses:

```bash
mvn compile exec:java -Dexec.mainClass="com.myorg.apiurlcomparison.MockApiServer"
```

**Quick Start Scripts:**
- **Windows**: Double-click `start-mock-server.bat` or run from command prompt
- **macOS/Linux**: Run `./start-mock-server.sh` from terminal (make executable first: `chmod +x start-mock-server.sh`)

The mock server starts on ports `8081-8084` (REST API 1, REST API 2, SOAP API 1, SOAP API 2) and provides test endpoints for development and validation.

## Configuration

Create a `config.yaml` file to define your API comparison settings:

```yaml
testType: REST  # or SOAP

# Iteration strategy
iterationController: ONE_BY_ONE  # or ALL_COMBINATIONS
maxIterations: 100

# Configuration block for REST APIs
rest:
  api1:
    baseUrl: "http://localhost:8080"
    authentication:
      clientId: "user1"
      clientSecret: "pass1"
    operations:
      - name: "createResource"
        methods: ["POST"]
        path: "/api/resource"
        payloadTemplatePath: "C:/path/to/payload.json"
        headers:
          Content-Type: "application/json"
  
  api2:
    baseUrl: "http://localhost:8080"
    authentication:
      clientId: "user2"
      clientSecret: "pass2"
    operations:
      - name: "createResource"
        methods: ["POST"]
        path: "/api/resource"
        payloadTemplatePath: "C:/path/to/payload.json"
        headers:
          Content-Type: "application/json"

# Configuration block for SOAP APIs
soap:
  api1:
    baseUrl: "http://localhost:8083/ws/AccountService"
    authentication:
      clientId: "user1"
      clientSecret: "pass1"
    operations:
      - name: "getAccountDetails"
        methods: ["POST"]
        headers:
          Content-Type: "text/xml;charset=UTF-8"
          SOAPAction: "getAccountDetails"
        payloadTemplatePath: "C:/path/to/payload.xml"

  api2:
    baseUrl: "http://localhost:8084/ws/AccountService"
    authentication:
      clientId: "user2"
      clientSecret: "pass2"
    operations:
      - name: "getAccountDetails"
        methods: ["POST"]
        headers:
          Content-Type: "text/xml;charset=UTF-8"
          SOAPAction: "getAccountDetails"
        payloadTemplatePath: "C:/path/to/payload.xml"

# Token definitions for iteration testing
tokens:
  account:
    - "999"
    - "1000"
    - "1001"
  uuid:
    - "id1"
    - "id2"
```

### Payload Templates

Create JSON or XML payload templates with token placeholders:

**JSON Example:**
```json
{
  "account": "1479",
  "myaccountvalue": 987,
  "name": "Rakesh",
  "uuid": "testid"
}
```

The tool uses **field name matching** for token replacement. If a field name contains a token name (case-insensitive), it will be replaced during iterations.

## Iteration Logic

### Original Input Payload (Iteration #1)
The tool **always** executes the original payload first without any token replacements. This ensures you have a baseline execution with your exact input data.

### ONE_BY_ONE Strategy
After the baseline, tests each token value individually:
- **Iteration 1**: Original payload (no replacements)
- **Iteration 2**: First token, first value
- **Iteration 3**: First token, second value
- **Iteration 4**: Second token, first value
- **Iteration 5**: Second token, second value

**Total Iterations**: 1 (baseline) + sum of all token values

### ALL_COMBINATIONS Strategy
After the baseline, tests all possible combinations:
- **Iteration 1**: Original payload (no replacements)
- **Iterations 2+**: All combinations of token values

**Total Iterations**: 1 (baseline) + (product of all token value counts)

## Reports

### CLI HTML Report
- **Header**: "API Response Comparison Tool - APITestingGuard"
- **Execution Summary**: Total iterations, duration, timestamp
- **Comparison Summary**: Matches, mismatches, errors
- **Iteration Details**: Expandable sections with full request/response data
- **Differences**: Highlighted mismatches with field-level details

### GUI Dashboard
- **Real-time Results**: Live updates as comparisons execute
- **Visual Status Indicators**: Color-coded match/mismatch/error badges
- **Expandable Iterations**: Click to view detailed request/response data
- **Professional Purple Theme**: Subtle borders with hover effects for modern look
- **XML Pretty-Printing**: Formatted XML/SOAP payloads with proper indentation
- **Horizontal Scrolling**: View complete long payloads without truncation
- **Timestamp**: Report generation time with timezone
- **Full-Width Layout**: Utilizes 95% of screen width for better visibility

### JSON Report
Machine-readable format for automation and integration:
```json
{
  "status": "MATCH",
  "operationName": "createResource (Original Input Payload)",
  "iterationTokens": {},
  "timestamp": "2025-12-08 13:00:00",
  "api1": { ... },
  "api2": { ... },
  "differences": []
}
```

## Examples

### Basic REST API Comparison
```bash
# 1. Start mock server (optional, for testing)
mvn compile exec:java -Dexec.mainClass="com.myorg.apiurlcomparison.MockApiServer"

# 2. Run comparison
java -jar target/apiurlcomparison-1.0.0.jar --config config.yaml --output ./reports

# 3. View results
open reports/results.html
```

### Using the GUI
```bash
# 1. Launch GUI
java -jar target/apiurlcomparison-1.0.0.jar

# 2. Configure in browser:
#    - Enter endpoint URLs
#    - Add payload template
#    - Define tokens
#    - Click "Run Comparison"

# 3. View results in the Execution Dashboard
```

## Troubleshooting

### Port Already in Use (GUI)
The GUI automatically finds an available port if 4567 is taken.

### 404 Errors
- Verify `baseUrl` and `path` in config.yaml
- Check that endpoints are accessible
- Ensure mock server is running if testing locally

### Token Replacement Not Working
- Verify field names in payload contain token names (case-insensitive)
- Check that tokens are defined in config.yaml
- Review the "Original Input Payload" iteration to see baseline behavior

### Build Errors
```bash
# Clean and rebuild
mvn clean package

# Skip tests if needed
mvn clean package -DskipTests
```

## Project Structure

```
apiurlcomparison/
├── src/main/java/com/myorg/apiurlcomparison/
│   ├── ApiUrlComparisonMain.java      # CLI entry point
│   ├── ApiUrlComparisonWeb.java       # GUI entry point
│   ├── ComparisonService.java         # Core comparison logic
│   ├── TestDataGenerator.java         # Iteration generation
│   ├── PayloadProcessor.java          # Token replacement
│   ├── ComparisonEngine.java          # Response comparison
│   ├── HtmlReportGenerator.java       # CLI report generation
│   ├── MockApiServer.java             # Mock server for testing
│   └── http/ApiClient.java            # HTTP client wrapper
├── src/main/resources/
│   └── public/                        # GUI assets
│       ├── index.html
│       ├── app.js
│       └── style.css
├── config.yaml                        # Configuration file
├── pom.xml                           # Maven configuration
└── README.md                         # This file
```

## Contributing

This tool is designed for API testing and validation. Feel free to extend it with additional features such as:
- OAuth 2.0 authentication support
- GraphQL API support
- Custom comparison rules
- Performance metrics
- Parallel execution

## License

[Specify your license here]

## Support

For issues, questions, or feature requests, please contact the development team.
