import urllib.request
import urllib.error
import json
import time
import concurrent.futures
import statistics

BASE_URL = "http://127.0.0.1:8080"

def post_json(path, data, token=None):
    url = f"{BASE_URL}{path}"
    body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))

def send_request(api_key):
    url = f"{BASE_URL}/api/v1/gateway/users"
    req = urllib.request.Request(url, method="GET")
    req.add_header("X-Sentinel-API-Key", api_key)
    
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            status = resp.status
    except urllib.error.HTTPError as e:
        status = e.code
    except Exception:
        status = 500
    lat_ms = (time.perf_counter() - t0) * 1000.0
    return status, lat_ms

def run_concurrency_batch(api_key, concurrency, total_requests):
    print(f"\n--- Running Load Test: {concurrency} Concurrent Workers ({total_requests} Total Requests) ---")
    latencies = []
    statuses = []
    
    t_start = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(send_request, api_key) for _ in range(total_requests)]
        for f in concurrent.futures.as_completed(futures):
            status, lat = f.result()
            statuses.append(status)
            latencies.append(lat)
    t_total = time.perf_counter() - t_start

    rps = total_requests / t_total if t_total > 0 else 0
    latencies.sort()
    avg_lat = statistics.mean(latencies) if latencies else 0
    p50_lat = statistics.median(latencies) if latencies else 0
    p95_lat = latencies[int(len(latencies) * 0.95)] if latencies else 0
    p99_lat = latencies[int(len(latencies) * 0.99)] if latencies else 0
    
    success_count = sum(1 for s in statuses if s == 200)
    error_count = total_requests - success_count
    error_rate = (error_count / total_requests) * 100.0

    print(f"  Throughput: {rps:.2f} reqs/sec")
    print(f"  Avg Latency: {avg_lat:.2f} ms")
    print(f"  P50 Latency: {p50_lat:.2f} ms")
    print(f"  P95 Latency: {p95_lat:.2f} ms")
    print(f"  P99 Latency: {p99_lat:.2f} ms")
    print(f"  Error Rate: {error_rate:.1f}% ({success_count}/{total_requests} succeeded in {t_total:.2f}s)")
    return {
        "concurrency": concurrency,
        "total": total_requests,
        "rps": rps,
        "avg": avg_lat,
        "p50": p50_lat,
        "p95": p95_lat,
        "p99": p99_lat,
        "error_rate": error_rate
    }

def test_performance_benchmarks():
    print("================================================================================")
    print("                 SENTINEL GATEWAY PERFORMANCE & LOAD BENCHMARK                  ")
    print("================================================================================")

    unique = int(time.time())
    email = f"perf-{unique}@sentinel.io"
    post_json("/api/v1/auth/register", {"name": "Perf Admin", "email": email, "password": "Password123!"})
    _, login = post_json("/api/v1/auth/login", {"email": email, "password": "Password123!"})
    token = login["token"]

    _, app = post_json("/api/v1/applications", {"name": f"PerfApp-{unique}", "baseUrl": "http://127.0.0.1:9090"}, token=token)
    app_id = app["id"]

    # Generate key with high rate limit to test raw throughput
    _, key = post_json(f"/api/v1/applications/{app_id}/keys", {"name": "PerfKey", "rateLimitPerMinute": 10000}, token=token)
    api_key = key["apiKey"]

    results = []
    # Test concurrency: 10, 50, 100
    results.append(run_concurrency_batch(api_key, concurrency=10, total_requests=50))
    results.append(run_concurrency_batch(api_key, concurrency=50, total_requests=100))
    results.append(run_concurrency_batch(api_key, concurrency=100, total_requests=200))

    print("\n[PASS] Performance Benchmarks Completed.")
    return results

if __name__ == "__main__":
    test_performance_benchmarks()
