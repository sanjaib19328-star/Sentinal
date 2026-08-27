# Sentinel Developer Guide

## Getting Started

### 1. Register an Application
1. Log into the Sentinel Web Console.
2. Navigate to **Applications** $\to$ **Register Application**.
3. Provide the service name and downstream `Base URL` (e.g. `http://localhost:9090`).

### 2. Onboard APIs with OpenAPI
1. Open the application details $\to$ **API Catalog** tab.
2. Click **Import OpenAPI**.
3. Paste an OpenAPI 3.0 / Swagger JSON or YAML specification, or supply a public URL.
4. Endpoints are registered as `DOCUMENTED`.

### 3. Generate Scoped API Key
1. Go to the **API Keys** tab $\to$ **Generate New Key**.
2. Copy the generated secret key (e.g., `sk_sentinel_...`). *Note: The raw secret is shown once and never stored in plaintext.*

### 4. Test with Developer API Console
1. Click **Try** on any cataloged endpoint or click **API Console**.
2. Select your active scoped API key.
3. Configure parameters, headers, or body $\to$ click **Send Request**.
4. Inspect real-time status code, latency (ms), rate-limit remaining, and response payload.

### 5. Send Real Traffic
```bash
curl -X GET "http://localhost:8080/api/v1/gateway/users" \
  -H "X-Sentinel-API-Key: sk_sentinel_your_key_here" \
  -H "Accept: application/json"
```
The endpoint in the catalog automatically transitions to `DOCUMENTED_AND_DISCOVERED`!
