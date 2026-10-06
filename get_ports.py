import os
import re

new_base = r"E:\yuegang-zhihui-ai\yuegang-zhihui-ai\ygh-applications"

services = [
    "ygh-admin", "ygh-ai", "ygh-inventory", "ygh-knowledge",
    "ygh-notification", "ygh-order", "ygh-product", "ygh-search",
    "ygh-training", "ygh-wallet"
]

print("=== 各服务端口和Application类 ===")
for svc in services:
    yml_path = os.path.join(new_base, svc, f"{svc}-service", "src", "main", "resources", "application.yml")
    
    # 端口
    port = "未知"
    if os.path.exists(yml_path):
        with open(yml_path, 'r', encoding='utf-8') as f:
            content = f.read()
        m = re.search(r'YGH_(\w+)_PORT:(\d+)', content)
        if m:
            port = m.group(2)
        else:
            m2 = re.search(r'port:\s*(\d+)', content)
            if m2:
                port = m2.group(1)
    
    # Application类
    app_class = "未知"
    java_base = os.path.join(new_base, svc, f"{svc}-service", "src", "main", "java")
    if os.path.isdir(java_base):
        for root, dirs, files in os.walk(java_base):
            for f in files:
                if f.endswith("Application.java"):
                    # 获取全限定类名
                    rel = os.path.relpath(os.path.join(root, f), java_base)
                    app_class = rel.replace(os.sep, ".").replace(".java", "")
                    break
    
    # pom路径
    pom_path = f"ygh-applications/{svc}/{svc}-service/pom.xml"
    
    print(f"  {svc:20s} 端口={port:5s}  类={app_class}")
    print(f"    pom: {pom_path}")
