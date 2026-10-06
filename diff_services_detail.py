import os
import hashlib

old_base = r"c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1\ygh-applications"
new_base = r"E:\yuegang-zhihui-ai\yuegang-zhihui-ai\ygh-applications"

services = [
    "ygh-admin", "ygh-ai", "ygh-inventory", "ygh-knowledge",
    "ygh-notification", "ygh-order", "ygh-product", "ygh-search",
    "ygh-training", "ygh-wallet"
]

def md5_file(path):
    with open(path, "rb") as f:
        return hashlib.md5(f.read()).hexdigest()

total_missing = 0
total_diff = 0
total_same = 0
missing_files = []
diff_files = []

for svc in services:
    old_svc = os.path.join(old_base, svc)
    new_svc = os.path.join(new_base, svc)
    
    # 检查 service 模块和 api 模块的 src
    for sub in [f"{svc}-service", f"{svc}-api"]:
        old_sub = os.path.join(old_svc, sub)
        new_sub = os.path.join(new_svc, sub)
        
        if not os.path.isdir(new_sub):
            continue
        
        old_src = os.path.join(old_sub, "src")
        new_src = os.path.join(new_sub, "src")
        
        if not os.path.isdir(new_src):
            continue
        
        # 收集新项目所有文件
        new_files = {}
        for root, dirs, files in os.walk(new_src):
            for f in files:
                fp = os.path.join(root, f)
                rel = os.path.relpath(fp, new_src)
                new_files[rel] = fp
        
        old_files = {}
        if os.path.isdir(old_src):
            for root, dirs, files in os.walk(old_src):
                for f in files:
                    fp = os.path.join(root, f)
                    rel = os.path.relpath(fp, old_src)
                    old_files[rel] = fp
        
        svc_missing = 0
        svc_diff = 0
        svc_same = 0
        for rel, new_fp in sorted(new_files.items()):
            old_fp = os.path.join(old_src, rel) if os.path.isdir(old_src) else None
            if not old_fp or not os.path.exists(old_fp):
                missing_files.append((svc, sub, rel))
                svc_missing += 1
                total_missing += 1
            elif md5_file(new_fp) != md5_file(old_fp):
                diff_files.append((svc, sub, rel))
                svc_diff += 1
                total_diff += 1
            else:
                svc_same += 1
                total_same += 1
        
        if svc_missing > 0 or svc_diff > 0:
            print(f"{svc}/{sub}: 缺失={svc_missing} 差异={svc_diff} 相同={svc_same}")

print(f"\n=== 汇总 ===")
print(f"相同文件: {total_same}")
print(f"缺失文件: {total_missing}")
print(f"差异文件: {total_diff}")

if missing_files:
    print(f"\n=== 缺失文件清单 ({total_missing}) ===")
    for svc, sub, rel in missing_files:
        print(f"  缺失: {svc}/{sub}/src/{rel}")

if diff_files:
    print(f"\n=== 差异文件清单 ({total_diff}) ===")
    for svc, sub, rel in diff_files:
        print(f"  差异: {svc}/{sub}/src/{rel}")
