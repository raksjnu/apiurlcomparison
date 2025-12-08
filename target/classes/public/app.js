document.addEventListener('DOMContentLoaded', () => {
    const compareBtn = document.getElementById('compareBtn');
    const resultsContainer = document.getElementById('resultsContainer');
    const statusIndicator = document.getElementById('statusIndicator');

    const addHeaderBtn = document.getElementById('addHeaderBtn');
    const headersTable = document.getElementById('headersTable').querySelector('tbody');
    const addTokenBtn = document.getElementById('addTokenBtn');
    const tokensTable = document.getElementById('tokensTable').querySelector('tbody');

    // -- Dynamic Rows Logic --
    if (addHeaderBtn && headersTable) {
        addHeaderBtn.addEventListener('click', () => addRow(headersTable, ['Header Name', 'Value']));
    }
    if (addTokenBtn && tokensTable) {
        addTokenBtn.addEventListener('click', () => addRow(tokensTable, ['Token Name', 'Values (semicolon separated)']));
    }

    // Load defaults on startup
    loadDefaults();

    async function loadDefaults() {
        try {
            const response = await fetch('/api/config');
            if (!response.ok) return; // Silent fail if no config api or file
            const config = await response.json();
            if (!config) return;

            // 1. Basic Fields
            if (config.testType) document.getElementById('testType').value = config.testType;
            if (config.iterationController) document.getElementById('iterationController').value = config.iterationController;
            if (config.maxIterations) document.getElementById('maxIterations').value = config.maxIterations;

            // 2. Determine which API config to use (REST or SOAP)
            const apis = config.testType === 'SOAP' ? config.soapApis : config.restApis;
            const activeApis = (config.testType === 'SOAP') ? config.soap : config.rest;

            if (activeApis && activeApis.api1 && activeApis.api2) {
                document.getElementById('url1').value = activeApis.api1.baseUrl || '';
                document.getElementById('url2').value = activeApis.api2.baseUrl || '';

                // Extract Auth from api1 (assume symmetric)
                if (activeApis.api1.authentication) {
                    document.getElementById('clientId').value = activeApis.api1.authentication.clientId || '';
                    document.getElementById('clientSecret').value = activeApis.api1.authentication.clientSecret || '';
                }

                // Extract Operation Details from api1 (assume 1st operation is main)
                if (activeApis.api1.operations && activeApis.api1.operations.length > 0) {
                    const op = activeApis.api1.operations[0];
                    if (op.name) document.getElementById('operationName').value = op.name;
                    if (op.payloadTemplatePath) document.getElementById('payload').value = op.payloadTemplatePath;
                    // Populate Method
                    if (op.methods && op.methods.length > 0) {
                        const methodVal = op.methods[0];
                        const methodSelect = document.getElementById('method');
                        // Ensure value exists in select options
                        if ([...methodSelect.options].some(o => o.value === methodVal)) {
                            methodSelect.value = methodVal;
                        }
                    }

                    // Populate Headers
                    if (op.headers && headersTable) {
                        headersTable.innerHTML = ''; // Clear existing
                        Object.entries(op.headers).forEach(([k, v]) => {
                            addRow(headersTable, ['Header Name', 'Value']);
                            const lastRow = headersTable.lastElementChild;
                            if (lastRow) {
                                lastRow.querySelector('.key-input').value = k;
                                lastRow.querySelector('.value-input').value = v;
                            }
                        });
                    }
                }
            }

            // 3. Populate Tokens
            if (config.tokens && tokensTable) {
                tokensTable.innerHTML = ''; // Clear existing
                Object.entries(config.tokens).forEach(([k, list]) => {
                    if (Array.isArray(list)) {
                        const valStr = list.join('; ');
                        addRow(tokensTable, ['Token Name', 'Values (semicolon separated)']);
                        const lastRow = tokensTable.lastElementChild;
                        if (lastRow) {
                            lastRow.querySelector('.key-input').value = k;
                            lastRow.querySelector('.value-input').value = valStr;
                        }
                    }
                });
            }

        } catch (e) {
            console.error("Failed to load defaults", e);
        }
    }

    function addRow(tbody, placeholders) {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><input type="text" placeholder="${placeholders[0]}" class="key-input"></td>
            <td><input type="text" placeholder="${placeholders[1]}" class="value-input"></td>
            <td><button type="button" class="btn-remove" onclick="this.closest('tr').remove()">×</button></td>
        `;
        tbody.appendChild(tr);
    }

    // -- Main Comparison Logic --
    compareBtn.addEventListener('click', async (e) => {
        e.preventDefault();

        // 1. Gather Data
        const config = buildConfig();
        if (!validateConfig(config)) return;

        // 2. Update UI State
        setLoading(true);

        try {
            // 3. Send Request
            const response = await fetch('/api/compare', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(config)
            });

            if (!response.ok) throw new Error('Network response was not ok');

            const results = await response.json();

            // 4. Render Results
            renderResults(results);
        } catch (error) {
            console.error('Error:', error);
            resultsContainer.innerHTML = `<div class="error-msg">Error executing comparison: ${error.message}</div>`;
        } finally {
            setLoading(false);
        }
    });

    function buildConfig() {
        const testType = document.getElementById('testType').value;
        const opName = document.getElementById('operationName').value || 'web-operation';
        const method = document.getElementById('method').value || 'POST';
        const url1 = document.getElementById('url1').value;
        const url2 = document.getElementById('url2').value;
        const payloadTemplate = document.getElementById('payload').value;
        const iterationType = document.getElementById('iterationController').value;
        const maxIterations = parseInt(document.getElementById('maxIterations').value) || 100;

        const clientId = document.getElementById('clientId').value;
        const clientSecret = document.getElementById('clientSecret').value;

        // Parse Headers
        const headers = {};
        if (headersTable) {
            headersTable.querySelectorAll('tr').forEach(tr => {
                const keyInput = tr.querySelector('.key-input');
                const valInput = tr.querySelector('.value-input');
                if (keyInput && valInput) {
                    const key = keyInput.value.trim();
                    const val = valInput.value.trim();
                    if (key) headers[key] = val;
                }
            });
        }

        // Parse Tokens
        const tokens = {};
        if (tokensTable) {
            tokensTable.querySelectorAll('tr').forEach(tr => {
                const keyInput = tr.querySelector('.key-input');
                const valInput = tr.querySelector('.value-input');
                if (keyInput && valInput) {
                    const key = keyInput.value.trim();
                    const valStr = valInput.value;
                    if (key) {
                        // Split by semicolon, trimming whitespace
                        let items = valStr.split(';').map(v => v.trim());

                        // Remove last item if it is empty (trailing semicolon effect)
                        if (items.length > 0 && items[items.length - 1] === '') {
                            items.pop();
                        }

                        // Convert to numbers where appropriate (DISABLED: Keep as strings to match CLI/YAML behavior)
                        const finalItems = items.map(v => v);

                        if (finalItems.length > 0) tokens[key] = finalItems;
                    }
                }
            });
        }

        const auth = {
            tokenUrl: null, // Basic auth doesn't strictly need this for simple username/password
            clientId: clientId || null,
            clientSecret: clientSecret || null
        };

        const opConfig = {
            name: opName,
            methods: [method], // Use selected method
            headers: headers,
            payloadTemplatePath: payloadTemplate || null
        };

        const apiConfig1 = {
            baseUrl: url1, // Full URL now
            authentication: auth,
            operations: [{ ...opConfig }]
        };

        const apiConfig2 = {
            baseUrl: url2,
            authentication: auth,
            operations: [{ ...opConfig }]
        };

        const config = {
            testType: testType,
            maxIterations: maxIterations,
            iterationController: iterationType,
            tokens: tokens,
            rest: { api1: apiConfig1, api2: apiConfig2 },
            soap: { api1: apiConfig1, api2: apiConfig2 }
        };

        return config;
    }

    function validateConfig(config) {
        if (!document.getElementById('url1').value) {
            alert("URL 1 is required");
            return false;
        }
        return true;
    }

    function setLoading(isLoading) {
        if (isLoading) {
            compareBtn.disabled = true;
            compareBtn.innerText = "Running...";
            resultsContainer.innerHTML = '<div class="empty-state"><p>Processing...</p></div>';
        } else {
            compareBtn.disabled = false;
            compareBtn.innerText = "Run Comparison";
        }
    }

    function renderResults(results) {
        resultsContainer.innerHTML = '';

        if (results.length === 0) {
            resultsContainer.innerHTML = '<div class="empty-state">No results returned.</div>';
            return;
        }

        // --- Summary Section ---
        const total = results.length;
        const matches = results.filter(r => r.status === 'MATCH').length;
        const mismatches = results.filter(r => r.status === 'MISMATCH').length;
        const errors = results.filter(r => r.status === 'ERROR').length;
        const totalDuration = results.reduce((acc, r) => acc + (r.api1?.duration || 0) + (r.api2?.duration || 0), 0);

        // Generate timestamp with timezone
        const now = new Date();
        const timestamp = now.toLocaleString('en-US', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false,
            timeZoneName: 'short'
        });

        const summaryContainer = document.createElement('div');
        summaryContainer.style.marginBottom = '20px';
        summaryContainer.innerHTML = `
            <div class="comparison-grid" style="gap: 10px;">
                <div class="card" style="padding: 15px; background: #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                    <h3 style="margin-bottom: 10px; font-size: 1rem; color: #27173e;">Execution Summary</h3>
                    <div style="font-size: 0.9rem; line-height: 1.6;">
                        <div><strong>Total Iterations:</strong> ${total}</div>
                        <div><strong>Total Duration:</strong> ${totalDuration} ms</div>
                        <div><strong>Report Generated:</strong> ${timestamp}</div>
                    </div>
                </div>
                <div class="card" style="padding: 15px; background: #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                    <h3 style="margin-bottom: 10px; font-size: 1rem; color: #27173e;">Comparison Summary</h3>
                    <div style="font-size: 0.9rem; line-height: 1.6;">
                        <div><span class="status-MATCH">Matches: ${matches}</span></div>
                        <div style="margin-top:5px;"><span class="status-MISMATCH">Mismatches: ${mismatches}</span></div>
                        <div style="margin-top:5px;"><span class="status-ERROR">Errors: ${errors}</span></div>
                    </div>
                </div>
            </div>
        `;
        resultsContainer.appendChild(summaryContainer);
        // -----------------------

        results.forEach((res, index) => {
            const isMatch = res.status === 'MATCH';
            const statusClass = isMatch ? 'status-MATCH' : (res.status === 'MISMATCH' ? 'status-MISMATCH' : 'status-ERROR');

            // Format tokens string: "account=123; id=456"
            let tokenStr = '';
            if (res.iterationTokens) {
                tokenStr = Object.entries(res.iterationTokens)
                    .map(([k, v]) => `${k}=${v}`)
                    .join('; ');
            }
            const tokenDisplay = tokenStr ? `<br><small style="color:#666; font-weight:normal;">Tokens: ${tokenStr}</small>` : '';

            // Timestamp
            const timeDisplay = res.timestamp ? `<span style="font-size:0.75rem; color:#999; margin-left: 10px;">${res.timestamp}</span>` : '';

            const card = document.createElement('div');
            card.className = `result-item`;

            const header = document.createElement('div');
            header.className = 'result-header';
            header.innerHTML = `
                <div>
                    <span>Iteration #${index + 1} - ${res.operationName}</span>
                    ${tokenDisplay}
                </div>
                <div>
                   ${timeDisplay}
                   <span class="${statusClass}" style="margin-left:10px;">${res.status}</span>
                </div>
            `;
            header.onclick = () => card.classList.toggle('expanded');

            const body = document.createElement('div');
            body.className = 'result-body';

            if (res.errorMessage) {
                body.innerHTML = `<p class="error-text">${res.errorMessage}</p>`;
            } else {
                let diffHtml = '';
                if (res.status === 'MISMATCH' && res.differences && res.differences.length > 0) {
                    diffHtml = `
                        <div class="diff-list">
                            <h5>Differences Found</h5>
                            <ul>
                                ${res.differences.map(d => `<li>${d}</li>`).join('')}
                            </ul>
                        </div>
                     `;
                }

                // Request Payload (Common)
                const reqPayload = res.api1 && res.api1.requestPayload ? res.api1.requestPayload : '';
                const reqDisplay = reqPayload ? `
                    <div class="request-box" style="margin-bottom: 20px;">
                        <h4 style="margin-bottom: 10px; font-size: 0.9rem; color: #27173e; font-weight: 600;">Request Payload</h4>
                        <pre>${formatJson(reqPayload)}</pre>
                    </div>` : '';

                if (isMatch) {
                    body.innerHTML = `
                        ${diffHtml}
                        ${reqDisplay}
                        <div class="single-view">
                            <h4>Response (Identical)</h4>
                            <pre>${formatJson(res.api1.responsePayload)}</pre>
                            <p><small>Duration: ${res.api1.duration}ms</small></p>
                        </div>
                    `;
                } else {
                    body.innerHTML = `
                        ${diffHtml}
                        ${reqDisplay}
                        <div class="comparison-grid">
                            <div class="payload-box">
                                <h4>API 1 Response (${res.api1.duration}ms)</h4>
                                <pre>${formatJson(res.api1.responsePayload)}</pre>
                            </div>
                            <div class="payload-box">
                                <h4>API 2 Response (${res.api2.duration}ms)</h4>
                                <pre>${formatJson(res.api2.responsePayload)}</pre>
                            </div>
                        </div>
                    `;
                }
            }

            card.appendChild(header);
            card.appendChild(body);
            resultsContainer.appendChild(card);
        });
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatXml(xml) {
        const PADDING = '  '; // 2 spaces for indentation
        const reg = /(>)(<)(\/*)/g;
        let formatted = '';
        let pad = 0;

        xml = xml.replace(reg, '$1\n$2$3');
        const lines = xml.split('\n');

        lines.forEach((line) => {
            let indent = 0;
            if (line.match(/.+<\/\w[^>]*>$/)) {
                indent = 0;
            } else if (line.match(/^<\/\w/)) {
                if (pad !== 0) {
                    pad -= 1;
                }
            } else if (line.match(/^<\w([^>]*[^\/])?>.*$/)) {
                indent = 1;
            } else {
                indent = 0;
            }

            formatted += PADDING.repeat(pad) + line + '\n';
            pad += indent;
        });

        return formatted.trim();
    }

    function formatJson(str) {
        if (!str) return '';

        // If it's a string, check if it's JSON or XML
        if (typeof str === 'string') {
            // Try JSON first
            try {
                const parsed = JSON.parse(str);
                return escapeHtml(JSON.stringify(parsed, null, 2));
            } catch (e) {
                // Not JSON, check if it's XML and format it
                if (str.trim().startsWith('<')) {
                    try {
                        return escapeHtml(formatXml(str));
                    } catch (xmlError) {
                        // If XML formatting fails, return escaped as-is
                        return escapeHtml(str);
                    }
                }
                // Not JSON or XML, return HTML-escaped
                return escapeHtml(str);
            }
        }

        // If it's already an object, stringify it
        try {
            return escapeHtml(JSON.stringify(str, null, 2));
        } catch (e) {
            return escapeHtml(String(str));
        }
    }
});
