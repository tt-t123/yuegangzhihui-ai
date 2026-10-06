"""测试 AI 客服功能（DeepSeek）"""
import base64
import hashlib
import hmac
import json
import time
import urllib.request

# === 配置 ===
HMAC_KEY_B64 = "UufWBki+DGYekhzIKVjw4pbZLD1mN2kYo3G1iYgr/x0="
AI_BASE_URL = "http://127.0.0.1:8090"

def generate_signature(user_id, roles, permissions, trace_id, request_id, method, path, timestamp_ms):
    """生成内部用户上下文签名（与 Java InternalUserContextSignature 兼容）"""
    key = base64.b64decode(HMAC_KEY_B64)
    # roles 和 permissions 需要排序去重
    roles_sorted = sorted(set(roles))
    perms_sorted = sorted(set(permissions))
    # 构建规范化字符串（与 Java canonical 方法一致）
    canonical = "\n".join([
        user_id,
        ",".join(roles_sorted),
        ",".join(perms_sorted),
        trace_id,
        request_id,
        method,
        path,
        str(timestamp_ms)
    ])
    # HMAC-SHA256
    digest = hmac.new(key, canonical.encode("utf-8"), hashlib.sha256).digest()
    return digest.hex()

def test_ai_chat():
    """测试 AI 聊天接口"""
    user_id = "1"
    roles = ["ADMIN"]
    permissions = []
    trace_id = "test-trace-001"
    request_id = "test-req-001"
    method = "POST"
    path = "/api/v1/ai/chat"
    timestamp_ms = int(time.time() * 1000)

    signature = generate_signature(user_id, roles, permissions, trace_id, request_id, method, path, timestamp_ms)

    headers = {
        "Content-Type": "application/json",
        "X-YGH-User-Id": user_id,
        "X-YGH-Roles": ",".join(roles),
        "X-YGH-Permissions": ",".join(permissions),
        "X-Trace-Id": trace_id,
        "X-Request-Id": request_id,
        "X-YGH-User-Context-Timestamp": str(timestamp_ms),
        "X-YGH-User-Context-Signature": signature,
    }

    body = json.dumps({"message": "你好，请介绍一下你自己", "includeOwnOrders": False}).encode("utf-8")

    req = urllib.request.Request(f"{AI_BASE_URL}{path}", data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            result = json.loads(resp.read().decode("utf-8"))
            print(f"Status: {resp.status}")
            print(f"Response: {json.dumps(result, ensure_ascii=False, indent=2)}")
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        print(f"HTTP Error: {e.code}")
        print(f"Response: {body}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    test_ai_chat()
