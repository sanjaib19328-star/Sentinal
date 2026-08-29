import urllib.request
import urllib.error
import json
import time
import io
from PIL import Image

BASE = "https://pixelvault-clean-api.onrender.com"

def generate_valid_png():
    img = Image.new('RGB', (200, 200), color=(73, 109, 137))
    buf = io.BytesIO()
    img.save(buf, format='PNG')
    return buf.getvalue()

def main():
    print("=" * 70)
    print("      TESTING REAL HOSTED PIXELVAULT API WORKFLOW END-TO-END")
    print("=" * 70)

    # 1. Health Check
    req = urllib.request.Request(f"{BASE}/api/v1/health")
    with urllib.request.urlopen(req, timeout=15) as resp:
        print(f"\n[1/6] GET /api/v1/health -> HTTP {resp.status}")
        print(f"      Body: {resp.read().decode('utf-8')}")

    # 2. Upload Image
    print(f"\n[2/6] POST /api/v1/images/upload (Uploading valid PNG)...")
    png_data = generate_valid_png()
    boundary = f"----WebKitBoundary{int(time.time()*1000)}"
    header_part = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="valid_forensic_sample.png"\r\n'
        f"Content-Type: image/png\r\n\r\n"
    ).encode("utf-8")
    footer_part = f"\r\n--{boundary}--\r\n".encode("utf-8")
    multipart_body = header_part + png_data + footer_part

    upload_req = urllib.request.Request(f"{BASE}/api/v1/images/upload", data=multipart_body, method="POST")
    upload_req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")

    with urllib.request.urlopen(upload_req, timeout=20) as resp:
        upload_res = json.loads(resp.read().decode("utf-8"))
        image_id = upload_res.get("image_id")
        print(f"      HTTP {resp.status} - Upload Successful!")
        print(f"      - Image ID:  {image_id}")
        print(f"      - Filename:  {upload_res.get('filename')}")
        print(f"      - SHA-256:   {upload_res.get('sha256_hash')}")
        print(f"      - File Size: {upload_res.get('file_size_bytes')} bytes")

    if not image_id:
        print("ERROR: No image_id returned.")
        return

    # 3. Analyze Image
    print(f"\n[3/6] POST /api/v1/images/{image_id}/analyze ...")
    analyze_req = urllib.request.Request(f"{BASE}/api/v1/images/{image_id}/analyze", method="POST", data=b"")
    with urllib.request.urlopen(analyze_req, timeout=20) as resp:
        analyze_res = json.loads(resp.read().decode("utf-8"))
        print(f"      HTTP {resp.status} - Analysis Completed!")
        print(f"      - Security Score: {analyze_res.get('security_score')}/100")
        print(f"      - Risk Level:     {analyze_res.get('risk_level')}")
        print(f"      - Dimensions:     {analyze_res.get('dimensions')}")
        print(f"      - Has Sensitive:  {analyze_res.get('has_sensitive_metadata')}")

    # 4. Clean Image
    print(f"\n[4/6] POST /api/v1/images/{image_id}/clean ...")
    clean_req = urllib.request.Request(f"{BASE}/api/v1/images/{image_id}/clean", method="POST", data=b"")
    with urllib.request.urlopen(clean_req, timeout=20) as resp:
        clean_res = json.loads(resp.read().decode("utf-8"))
        print(f"      HTTP {resp.status} - Cleaning Completed!")
        print(f"      - Cleaned File:   {clean_res.get('cleaned_filename')}")
        print(f"      - New Sec Score:  {clean_res.get('new_security_score')}/100")
        print(f"      - Removed EXIF:   {clean_res.get('removed_exif_count')}")
        print(f"      - C2PA Stripped:  {clean_res.get('c2pa_stripped')}")

    # 5. Get Forensic Report
    print(f"\n[5/6] GET /api/v1/images/{image_id}/report ...")
    report_req = urllib.request.Request(f"{BASE}/api/v1/images/{image_id}/report", method="GET")
    with urllib.request.urlopen(report_req, timeout=20) as resp:
        report_res = json.loads(resp.read().decode("utf-8"))
        print(f"      HTTP {resp.status} - Forensic Report Retrieved!")
        print(f"      - Platform:     {report_res.get('platform')} v{report_res.get('version')}")
        print(f"      - Generated At: {report_res.get('generated_at')}")
        print(f"      - Filename:     {report_res.get('filename')}")

    # 6. Download Image
    print(f"\n[6/6] GET /api/v1/images/{image_id}/download ...")
    download_req = urllib.request.Request(f"{BASE}/api/v1/images/{image_id}/download", method="GET")
    with urllib.request.urlopen(download_req, timeout=20) as resp:
        image_bytes = resp.read()
        print(f"      HTTP {resp.status} - Cleaned Image Downloaded!")
        print(f"      - Content-Type: {resp.headers.get('Content-Type')}")
        print(f"      - Payload Size: {len(image_bytes)} bytes")

    print("\n" + "=" * 70)
    print("      ALL 6 PIXELVAULT ENDPOINTS PASSED PERFECTLY (HTTP 200 OK)")
    print("=" * 70)

if __name__ == "__main__":
    main()
