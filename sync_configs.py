import os
import shutil
import hashlib

old_base = r"c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1\ygh-applications"
new_base = r"E:\yuegang-zhihui-ai\yuegang-zhihui-ai\ygh-applications"

services_to_sync = [
    "ygh-admin", "ygh-ai", "ygh-inventory", "ygh-knowledge",
    "ygh-notification", "ygh-order", "ygh-product", "ygh-search",
    "ygh-training", "ygh-wallet"
]

def md5_file(path):
    with open(path, "rb") as f:
        return hashlib.md5(f.read()).hexdigest()

# 同步 pom.xml 和 resources 下的文件
copied = 0
overwritten = 0

for svc in services_to_sync:
    old_svc = os.path.join(old_base, svc)
    new_svc = os.path.join(new_base, svc)
    
    for sub in [f"{svc}-service", f"{svc}-api"]:
        old_sub = os.path.join(old_svc, sub)
        new_sub = os.path.join(new_svc, sub)
        
        if not os.path.isdir(new_sub):
            continue
        
        # 1. 同步 pom.xml
        new_pom = os.path.join(new_sub, "pom.xml")
        old_pom = os.path.join(old_sub, "pom.xml")
        if os.path.exists(new_pom):
            if not os.path.exists(old_pom) or md5_file(new_pom) != md5_file(old_pom):
                os.makedirs(old_sub, exist_ok=True)
                shutil.copy2(new_pom, old_pom)
                if not os.path.exists(old_pom):
                    copied += 1
                    print(f"  新增 pom: {svc}/{sub}/pom.xml")
                else:
                    overwritten += 1
                    print(f"  覆盖 pom: {svc}/{sub}/pom.xml")
        
        # 2. 同步 resources 目录
        new_res = os.path.join(new_sub, "src", "main", "resources")
        old_res = os.path.join(old_sub, "src", "main", "resources")
        
        if not os.path.isdir(new_res):
            continue
        
        os.makedirs(old_res, exist_ok=True)
        
        for root, dirs, files in os.walk(new_res):
            for f in files:
                new_fp = os.path.join(root, f)
                rel = os.path.relpath(new_fp, new_res)
                old_fp = os.path.join(old_res, rel)
                
                if not os.path.exists(old_fp) or md5_file(new_fp) != md5_file(old_fp):
                    os.makedirs(os.path.dirname(old_fp), exist_ok=True)
                    shutil.copy2(new_fp, old_fp)
                    if not os.path.exists(old_fp):
                        copied += 1
                        print(f"  新增 res: {svc}/{sub}/src/main/resources/{rel}")
                    else:
                        overwritten += 1
                        print(f"  覆盖 res: {svc}/{sub}/src/main/resources/{rel}")

# 3. 同步 ygh-applications/pom.xml
old_app_pom = os.path.join(old_base, "pom.xml")
new_app_pom = os.path.join(new_base, "pom.xml")
if os.path.exists(new_app_pom) and md5_file(new_app_pom) != md5_file(old_app_pom):
    shutil.copy2(new_app_pom, old_app_pom)
    overwritten += 1
    print(f"  覆盖: ygh-applications/pom.xml")

print(f"\n=== 同步完成 ===")
print(f"新增: {copied}")
print(f"覆盖: {overwritten}")
