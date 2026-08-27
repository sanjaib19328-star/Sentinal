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
        with urllib.request.urlopen(req, timeout=30) as resp:
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
    print("SENTINEL VERIFICATION: AI BULK API CHECKING & BATCH DIAGNOSTICS")
    print("=" * 70)

    # 1. Register & Login Test Tenant
    ts = int(time.time())
    email = f"bulk_tester_{ts}@sentinel.io"
    password = "Password123!"

    s_reg, r_reg = http_req("POST", f"{BASE_URL}/api/v1/auth/register", {"email": email, "password": password, "name": "Bulk Tester"})
    assert s_reg == 201, f"Reg failed: {r_reg}"

    s_log, r_log = http_req("POST", f"{BASE_URL}/api/v1/auth/login", {"email": email, "password": password})
    assert s_log == 200, f"Login failed: {r_log}"
    token = r_log["token"]
    print("  Tenant authenticated [PASS]")

    # 2. Connect Application with multiple endpoints
    print("\n[STEP 1: CONNECT APPLICATION & DISCOVER APIS]")
    s_conn, r_conn = http_req("POST", f"{BASE_URL}/api/v1/applications/connect-and-discover", {
        "applicationName": "Bulk-Check-Target",
        "sentinelUrl": HOSTED_URL,
        "apiKey": ""
    }, headers={"Authorization": f"Bearer {token}"})

    assert s_conn == 200, f"Connect failed: {r_conn}"
    app_id = r_conn["applicationId"]
    total_apis = r_conn["apisDiscoveredCount"]
    print(f"  Connected App ID: {app_id}, Discovered APIs: {total_apis}")

    # 3. Test Bulk API Check with Controlled Batching (batchSize = 3)
    print("\n[STEP 2: EXECUTE BATCHED AI BULK CHECK (Batch size 3)]")
    batch_index = 0
    all_results = []
    total_batches = 1

    while True:
        s_bulk, r_bulk = http_req("POST", f"{BASE_URL}/api/v1/applications/{app_id}/bulk-api-check", {
            "applicationId": app_id,
            "batchIndex": batch_index,
            "batchSize": 3
        }, headers={"Authorization": f"Bearer {token}"})

        assert s_bulk == 200, f"Bulk check failed on batch {batch_index}: {r_bulk}"
        total_batches = r_bulk["totalBatches"]
        batch_results = r_bulk["results"]
        all_results.extend(batch_results)

        print(f"  Batch {batch_index + 1}/{total_batches}: Analyzed {len(batch_results)} endpoints (Completed {r_bulk['completedCount']}/{r_bulk['totalEndpoints']})")
        for res in batch_results:
            print(f"    * [{res['status']}] {res['method']} {res['path']} -> {res['responseValidity']} | TIP: {res['recommendation']}")

        if r_bulk["lastBatch"] or batch_index >= total_batches - 1:
            break
        batch_index += 1

    print(f"\n[STEP 3: SUMMARY & VERIFICATION]")
    print(f"  Total APIs Analyzed across {batch_index + 1} batches: {len(all_results)}")
    assert len(all_results) == total_apis, f"Expected {total_apis} results, got {len(all_results)}"
    
    valid_count = sum(1 for r in all_results if r["status"] == "VALID")
    warn_count = sum(1 for r in all_results if r["status"] in ("WARNING", "REQUIRES_INPUT"))
    err_count = sum(1 for r in all_results if r["status"] == "ERROR")

    print(f"  Valid Endpoints:   {valid_count}")
    print(f"  Warning / Inputs:  {warn_count}")
    print(f"  Errors:            {err_count}")

    print("\n" + "=" * 70)
    print("AI BULK API CHECKING VERIFICATION: 100% PASS")
    print("=" * 70)

if __name__ == "__main__":
    main()
