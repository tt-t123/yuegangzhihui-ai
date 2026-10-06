import urllib.request
import json

services = [
    ("AUTH", 8081),
    ("USER", 8082),
    ("SYSTEM", 8083),
    ("GATEWAY", 8080),
]

print("=== 服务健康状态 ===")
for name, port in services:
    for probe in ["liveness", "readiness"]:
        url = f"http://127.0.0.1:{port}/actuator/health/{probe}"
        try:
            req = urllib.request.Request(url)
            with urllib.request.urlopen(req, timeout=5) as r:
                data = json.loads(r.read().decode())
                print(f"  {name:8s} :{port} {probe:12s} -> {data.get('status', 'UNKNOWN')}")
        except Exception as e:
            print(f"  {name:8s} :{port} {probe:12s} -> FAIL: {e}")

print("\n=== 网关验证码路由 ===")
try:
    req = urllib.request.Request("http://127.0.0.1:8080/api/v1/auth/captcha")
    with urllib.request.urlopen(req, timeout=10) as r:
        data = json.loads(r.read().decode())
        print(f"  Captcha: {data['code']}, challengeId: {'present' if data.get('data',{}).get('challengeId') else 'MISSING'}")
except Exception as e:
    print(f"  FAIL: {e}")
