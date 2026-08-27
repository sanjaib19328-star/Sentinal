import json
import time
import urllib.request
import urllib.error

BASE_URL = "http://localhost:8080"
HOSTED_URL = "https://pixelvault-clean-api.onrender.com"

def http_req(method, url, data=None, headers=None):
    if headers is None:
        headers = {}
    if data is not None:
        payload = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    else:
        payload = None

    req = urllib.request.Request(url, data=payload, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            body = resp.read().decode("utf-8")
            try:
                return resp.status, json.loads(body)
            except:
                return resp.status, body
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(body)
        except:
            return e.code, body
    except Exception as e:
        return 500, {"error": str(e)}

def main():
    print("=" * 70)
    print("SENTINEL: TESTING REAL HOSTED BACKEND CONNECTION (PixelVault-Clean)")
    print("=" * 70)

    # 1. Register & Login Test Tenant
    ts = int(time.time())
    email = f"hosted_tester_{ts}@sentinel.io"
    password = "Password123!"

    s_reg, r_reg = http_req("POST", f"{BASE_URL}/api/v1/auth/register", {"email": email, "password": password, "name": "Hosted Tester"})
    assert s_reg == 201, f"Reg failed: {r_reg}"

    s_log, r_log = http_req("POST", f"{BASE_URL}/api/v1/auth/login", {"email": email, "password": password})
    assert s_log == 200, f"Login failed: {r_log}"
    token = r_log["token"]
    print("  Tenant authenticated [PASS]")

    # 2. Connect Hosted Application
    print(f"\n[CONNECTING] Connecting to {HOSTED_URL}...")
    s_conn, r_conn = http_req("POST", f"{BASE_URL}/api/v1/applications/connect-and-discover", {
        "applicationName": "PixelVault-Clean",
        "sentinelUrl": HOSTED_URL,
        "apiKey": ""
    }, headers={"Authorization": f"Bearer {token}"})

    print(f"  HTTP Response: {s_conn}")
    if s_conn == 200:
        print(f"  Application Name:     {r_conn['applicationName']}")
        print(f"  Health Status:        {r_conn['healthStatus']} (Healthy: {r_conn['backendHealthy']})")
        print(f"  Sentinel Gateway URL: {r_conn['sentinelGatewayUrl']}")
        print(f"  Sentinel API Key:     {r_conn['apiKey'][:16]}...")
        print(f"  APIs Discovered:      {r_conn['apisDiscoveredCount']} endpoints")
        for ep in r_conn.get("discoveredApis", []):
            print(f"    - {ep['method']} {ep['normalizedPath']} ({ep.get('summary', 'No summary')})")
        print("\n  REAL HOSTED APPLICATION CONNECTION: PASS")
    else:
        print(f"  Hosted connect response: {r_conn}")

if __name__ == "__main__":
    main()
