import os
import shutil

old_base = r"c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1\ygh-applications"
new_base = r"E:\yuegang-zhihui-ai\yuegang-zhihui-ai\ygh-applications"

# 只同步这些服务（auth/user/system/gateway已修复，不动）
services_to_sync = [
    "ygh-admin", "ygh-ai", "ygh-inventory", "ygh-knowledge",
    "ygh-notification", "ygh-order", "ygh-product", "ygh-search",
    "ygh-training", "ygh-wallet"
]

copied_count = 0
overwritten_count = 0

for svc in services_to_sync:
    old_svc = os.path.join(old_base, svc)
    new_svc = os.path.join(new_base, svc)
    
    # 同步 service 和 api 子模块的 src
    for sub in [f"{svc}-service", f"{svc}-api"]:
        old_sub = os.path.join(old_svc, sub)
        new_sub = os.path.join(new_svc, sub)
        
        if not os.path.isdir(new_sub):
            continue
        
        old_src = os.path.join(old_sub, "src")
        new_src = os.path.join(new_sub, "src")
        
        if not os.path.isdir(new_src):
            continue
        
        # 确保旧项目src目录存在
        os.makedirs(old_src, exist_ok=True)
        
        # 收集新项目所有文件
        new_files = {}
        for root, dirs, files in os.walk(new_src):
            for f in files:
                fp = os.path.join(root, f)
                rel = os.path.relpath(fp, new_src)
                new_files[rel] = fp
        
        # 复制所有文件（缺失的直接复制，已存在的覆盖）
        for rel, new_fp in sorted(new_files.items()):
            old_fp = os.path.join(old_src, rel)
            
            if not os.path.exists(old_fp):
                # 缺失文件，直接复制
                os.makedirs(os.path.dirname(old_fp), exist_ok=True)
                shutil.copy2(new_fp, old_fp)
                copied_count += 1
                print(f"  新增: {svc}/{sub}/src/{rel}")
            else:
                # 已存在文件，用新项目覆盖（这些服务未修复过）
                shutil.copy2(new_fp, old_fp)
                overwritten_count += 1

print(f"\n=== 同步完成 ===")
print(f"新增文件: {copied_count}")
print(f"覆盖文件: {overwritten_count}")
