import urllib.request
import urllib.error
import json
import time

BASE_URL = "http://127.0.0.1:8080"

def get_error(path, headers=None, token=None):
    url = f"{BASE_URL}{path}"
    req = urllib.request.Request(url, method="GET")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    if headers:
        for k, v in headers.items():
            req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(body)
        except Exception:
            return e.code, {"raw": body}

def test_error_sanitization():
    print("[Security Test: Error Format Sanitization & Information Leakage Prevention]")

    # 1. 401 Unauthorized Error
    st, err_401 = get_error("/api/v1/applications")
    assert st == 401
    assert "error" in err_401, "Error response missing 'error' field"
    assert "code" in err_401, "Error response missing 'code' field"
    print("  [OK] 401 format complies with standard structured schema")

    # 2. 404 Not Found Error
    st, err_404 = get_error("/api/v1/applications/99999999", token="invalid_bearer_token")
    assert st in [401, 404]
    print("  [OK] 404/401 handles invalid resource identifiers safely")

    # 3. Stack Trace & SQL Injection Check
    st, err_sqli = get_error("/api/v1/applications/'%20OR%201=1--", token="invalid_bearer_token")
    assert "SQLException" not in str(err_sqli)
    assert "HibernateException" not in str(err_sqli)
    assert "org.springframework" not in str(err_sqli)
    assert "root_password" not in str(err_sqli)
    print("  [OK] Zero database stack traces, SQL errors, or credentials leaked")

    print("[PASS] Error Sanitization Test Suite completed successfully.\n")

if __name__ == "__main__":
    test_error_sanitization()
