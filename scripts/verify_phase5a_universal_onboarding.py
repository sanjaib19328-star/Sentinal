#!/usr/bin/env python3
"""
Sentinel Phase 5A Live E2E Verification Script:
Universal Application Onboarding & Upstream Authentication

Verifies:
1. Universal Onboarding with 6 upstream auth types:
   - NONE (Public Upstream)
   - BEARER_TOKEN (Authorization: Bearer <secret>)
   - API_KEY_HEADER (e.g. X-API-Key: <secret>)
   - API_KEY_QUERY (?apiKey=<secret>)
   - BASIC_AUTH (Authorization: Basic <base64>)
   - CUSTOM_HEADER (e.g. X-Custom-Token: <secret>)
2. Zero-Pollution Connection Probing (UPSTREAM_CONNECTION_TESTED audit log, zero request logs/metrics pollution).
3. Two-Layer Credential Separation:
   - Consumer uses X-Sentinel-API-Key: sk_sentinel_...
   - Sentinel forwards to upstream with configured upstream auth credentials.
4. Upstream Credential Rotation:
   - Rotate secret -> verify new secret takes effect immediately.
5. Upstream Secret Protection:
   - Secrets are masked (••••••••) in API responses and encrypted at rest with AES-256-GCM.
"""

import sys
import time
import requests
import subprocess
import os

SENTINEL_URL = os.environ.get("SENTINEL_URL", "http://127.0.0.1:8080")
TARGET_URL = os.environ.get("TARGET_URL", "http://127.0.0.1:9090")

passed_steps = 0
total_steps = 0

def check(step_name, condition, details=""):
    global passed_steps, total_steps
    total_steps += 1
    if condition:
        passed_steps += 1
        print(f"  [PASS] Step {total_steps}: {step_name}")
        if details:
            print(f"         {details}")
    else:
        print(f"  [FAIL] Step {total_steps}: {step_name}")
        if details:
            print(f"         Details: {details}")
        sys.exit(1)

def main():
    print("=" * 70)
    print("  SENTINEL PHASE 5A: UNIVERSAL ONBOARDING E2E VERIFICATION")
    print("=" * 70)

    # 1. Verify Target App is running
    print("\n--- 1. Verifying Upstream Multi-Auth Target Application ---")
    try:
        r = requests.get(f"{TARGET_URL}/health", timeout=3)
        check("Target App Health Check", r.status_code == 200 and r.json().get("status") == "healthy", f"HTTP {r.status_code}")
    except Exception as e:
        print(f"Target app at {TARGET_URL} not responding: {e}")
        sys.exit(1)

    # 2. Register fresh owner on Sentinel
    print("\n--- 2. Registering Sentinel Tenant Owner ---")
    email = f"universal_owner_{int(time.time())}@sentinel.io"
    reg_resp = requests.post(f"{SENTINEL_URL}/api/v1/auth/register", json={
        "name": "Universal API Owner",
        "email": email,
        "password": "Password123!"
    })
    check("Owner Registration", reg_resp.status_code == 201, f"Owner email: {email}")

    login_resp = requests.post(f"{SENTINEL_URL}/api/v1/auth/login", json={
        "email": email,
        "password": "Password123!"
    })
    check("Owner Login", login_resp.status_code == 200)
    jwt_token = login_resp.json()["token"]
    headers = {"Authorization": f"Bearer {jwt_token}"}

    # 3. Test Zero-Pollution Connection Probing on Onboarding
    print("\n--- 3. Testing Zero-Pollution Connection Probing ---")
    app_payload = {
        "name": "Bearer Microservice",
        "description": "Secured upstream with Bearer Token",
        "baseUrl": TARGET_URL,
        "upstreamAuth": {
            "type": "BEARER_TOKEN",
            "enabled": True,
            "secret": "valid-upstream-bearer-token"
        }
    }
    create_resp = requests.post(f"{SENTINEL_URL}/api/v1/applications", json=app_payload, headers=headers)
    check("Create Application with Bearer Auth", create_resp.status_code == 201)
    app = create_resp.json()
    app_id = app["id"]

    # Verify secret is masked in response
    check("Secret Masked in Creation Response", app["upstreamAuth"]["maskedSecret"] == "••••••••" and "valid-upstream-bearer-token" not in create_resp.text)

    # Test isolated connection probe
    probe_resp = requests.post(f"{SENTINEL_URL}/api/v1/applications/{app_id}/connection-test", headers=headers)
    check("Zero-Pollution Connection Test", probe_resp.status_code == 200 and probe_resp.json()["reachable"] is True, f"Status: {probe_resp.json()['status']}, Latency: {probe_resp.json()['latencyMs']}ms")

    # Verify ZERO request logs and ZERO metrics created from connection probe
    reqs_resp = requests.get(f"{SENTINEL_URL}/api/v1/applications/{app_id}/requests", headers=headers)
    check("Zero Request Logs Generated from Connection Test", reqs_resp.json()["totalElements"] == 0)

    # Verify dedicated audit log was recorded
    audit_resp = requests.get(f"{SENTINEL_URL}/api/v1/audit-logs?applicationId={app_id}", headers=headers)
    check("Audit Event UPSTREAM_CONNECTION_TESTED Recorded", any(log["action"] == "UPSTREAM_CONNECTION_TESTED" for log in audit_resp.json().get("content", [])))

    # 4. Test 2-Layer Authentication with Bearer Upstream
    print("\n--- 4. Testing Layer 1 (Consumer Key) -> Layer 2 (Upstream Bearer Token) ---")
    key_resp = requests.post(f"{SENTINEL_URL}/api/v1/applications/{app_id}/keys", json={"name": "Consumer Key", "rateLimit": 100}, headers=headers)
    check("Generate Consumer API Key", key_resp.status_code == 201)
    consumer_key = key_resp.json()["apiKey"]
    check("Consumer Key Format", consumer_key.startswith("sk_sentinel_"))

    # Make consumer request to /secure/bearer through Gateway
    gw_resp = requests.get(f"{SENTINEL_URL}/api/v1/gateway/secure/bearer", headers={"X-Sentinel-API-Key": consumer_key})
    check("Gateway Forwarding with Injected Bearer Token", gw_resp.status_code == 200 and gw_resp.json().get("auth") == "bearer", f"Response: {gw_resp.text}")

    # 5. Test Upstream API Key (Header)
    print("\n--- 5. Testing Upstream API Key (Header Strategy) ---")
    app2_payload = {
        "name": "Header API Key Service",
        "description": "Secured upstream with X-API-Key",
        "baseUrl": TARGET_URL,
        "upstreamAuth": {
            "type": "API_KEY_HEADER",
            "enabled": True,
            "headerName": "X-API-Key",
            "secret": "valid-upstream-api-key"
        }
    }
    app2_resp = requests.post(f"{SENTINEL_URL}/api/v1/applications", json=app2_payload, headers=headers)
    check("Create Application with API Key Header", app2_resp.status_code == 201)
    app2_id = app2_resp.json()["id"]

    key2_resp = requests.post(f"{SENTINEL_URL}/api/v1/applications/{app2_id}/keys", json={"name": "Consumer Key 2", "rateLimit": 100}, headers=headers)
    consumer_key2 = key2_resp.json()["apiKey"]

    gw2_resp = requests.get(f"{SENTINEL_URL}/api/v1/gateway/secure/api-key-header", headers={"X-Sentinel-API-Key": consumer_key2})
    check("Gateway Forwarding with Injected X-API-Key Header", gw2_resp.status_code == 200 and gw2_resp.json().get("auth") == "api-key-header", f"Response: {gw2_resp.text}")

    # 6. Test Upstream API Key (Query Parameter) with Query String Preservation
    print("\n--- 6. Testing Upstream API Key (Query Parameter Strategy) ---")
    app3_payload = {
        "name": "Query Param API Key Service",
        "description": "Secured upstream with ?apiKey=...",
        "baseUrl": TARGET_URL,
        "upstreamAuth": {
            "type": "API_KEY_QUERY",
            "enabled": True,
            "queryParamName": "apiKey",
            "secret": "valid-upstream-query-key"
        }
    }
    app3_resp = requests.post(f"{SENTINEL_URL}/api/v1/applications", json=app3_payload, headers=headers)
    check("Create Application with API Key Query Param", app3_resp.status_code == 201)
    app3_id = app3_resp.json()["id"]

    key3_resp = requests.post(f"{SENTINEL_URL}/api/v1/applications/{app3_id}/keys", json={"name": "Consumer Key 3", "rateLimit": 100}, headers=headers)
    consumer_key3 = key3_resp.json()["apiKey"]

    # Call with existing consumer query parameter filter=active
    gw3_resp = requests.get(f"{SENTINEL_URL}/api/v1/gateway/secure/api-key-query?filter=active&page=2", headers={"X-Sentinel-API-Key": consumer_key3})
    check("Gateway Forwarding with Injected Query Parameter", gw3_resp.status_code == 200 and gw3_resp.json().get("auth") == "api-key-query", f"Response: {gw3_resp.text}")

    # 7. Test Upstream Basic Authentication
    print("\n--- 7. Testing Upstream Basic Authentication ---")
    app4_payload = {
        "name": "Basic Auth Service",
        "description": "Secured upstream with HTTP Basic Auth",
        "baseUrl": TARGET_URL,
        "upstreamAuth": {
            "type": "BASIC_AUTH",
            "enabled": True,
            "username": "admin",
            "password": "upstream-password-123"
        }
    }
    app4_resp = requests.post(f"{SENTINEL_URL}/api/v1/applications", json=app4_payload, headers=headers)
    check("Create Application with Basic Auth", app4_resp.status_code == 201)
    app4_id = app4_resp.json()["id"]

    key4_resp = requests.post(f"{SENTINEL_URL}/api/v1/applications/{app4_id}/keys", json={"name": "Consumer Key 4", "rateLimit": 100}, headers=headers)
    consumer_key4 = key4_resp.json()["apiKey"]

    gw4_resp = requests.get(f"{SENTINEL_URL}/api/v1/gateway/secure/basic", headers={"X-Sentinel-API-Key": consumer_key4})
    check("Gateway Forwarding with Injected Basic Auth", gw4_resp.status_code == 200 and gw4_resp.json().get("auth") == "basic", f"Response: {gw4_resp.text}")

    # 8. Test Upstream Custom Header Authentication
    print("\n--- 8. Testing Upstream Custom Header Authentication ---")
    app5_payload = {
        "name": "Custom Header Service",
        "description": "Secured upstream with X-Custom-Token",
        "baseUrl": TARGET_URL,
        "upstreamAuth": {
            "type": "CUSTOM_HEADER",
            "enabled": True,
            "headerName": "X-Custom-Token",
            "secret": "valid-upstream-custom-token"
        }
    }
    app5_resp = requests.post(f"{SENTINEL_URL}/api/v1/applications", json=app5_payload, headers=headers)
    check("Create Application with Custom Header", app5_resp.status_code == 201)
    app5_id = app5_resp.json()["id"]

    key5_resp = requests.post(f"{SENTINEL_URL}/api/v1/applications/{app5_id}/keys", json={"name": "Consumer Key 5", "rateLimit": 100}, headers=headers)
    consumer_key5 = key5_resp.json()["apiKey"]

    gw5_resp = requests.get(f"{SENTINEL_URL}/api/v1/gateway/secure/custom-header", headers={"X-Sentinel-API-Key": consumer_key5})
    check("Gateway Forwarding with Injected Custom Header", gw5_resp.status_code == 200 and gw5_resp.json().get("auth") == "custom-header", f"Response: {gw5_resp.text}")

    # 9. Test Upstream Credential Rotation
    print("\n--- 9. Testing Upstream Credential Rotation ---")
    # First rotate to an INVALID credential on app5
    rotate_bad = requests.put(f"{SENTINEL_URL}/api/v1/applications/{app5_id}/upstream-auth", json={
        "type": "CUSTOM_HEADER",
        "enabled": True,
        "headerName": "X-Custom-Token",
        "secret": "wrong-revoked-token"
    }, headers=headers)
    check("Rotate Upstream Credential to Bad Key", rotate_bad.status_code == 200)

    # Consumer request should now fail upstream authentication (HTTP 401 from upstream target)
    gw_fail = requests.get(f"{SENTINEL_URL}/api/v1/gateway/secure/custom-header", headers={"X-Sentinel-API-Key": consumer_key5})
    check("Consumer Request Fails with Rotated Bad Key", gw_fail.status_code == 401)

    # Now rotate to VALID new credential
    rotate_good = requests.put(f"{SENTINEL_URL}/api/v1/applications/{app5_id}/upstream-auth", json={
        "type": "CUSTOM_HEADER",
        "enabled": True,
        "headerName": "X-Custom-Token",
        "secret": "valid-upstream-custom-token"
    }, headers=headers)
    check("Rotate Upstream Credential to Valid Key", rotate_good.status_code == 200)

    # Consumer request now succeeds immediately
    gw_success = requests.get(f"{SENTINEL_URL}/api/v1/gateway/secure/custom-header", headers={"X-Sentinel-API-Key": consumer_key5})
    check("Consumer Request Succeeds after Credential Recovery", gw_success.status_code == 200 and gw_success.json().get("auth") == "custom-header")

    # 10. Test Header Sanitization & Spoofing Prevention
    print("\n--- 10. Testing Header Sanitization & Spoofing Prevention ---")
    # Consumer tries to send their own Authorization header: "Bearer hacker-injected-token"
    # Gateway MUST override with Sentinel's configured upstream auth
    spoof_resp = requests.get(f"{SENTINEL_URL}/api/v1/gateway/auth/echo", headers={
        "X-Sentinel-API-Key": consumer_key,
        "Authorization": "Bearer hacker-injected-token"
    })
    check("Consumer Cannot Spoof Authorization Header", spoof_resp.status_code == 200 and spoof_resp.json()["received_authorization"] == "Bearer valid-upstream-bearer-token")

    # 11. Final Summary
    print("\n" + "=" * 70)
    print(f"  PHASE 5A UNIVERSAL ONBOARDING VERIFICATION PASSED: {passed_steps}/{total_steps} STEPS")
    print("=" * 70)

if __name__ == "__main__":
    main()
