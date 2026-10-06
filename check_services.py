import os
import shutil

old_base = r"c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1\ygh-applications"
new_base = r"E:\yuegang-zhihui-ai\yuegang-zhihui-ai\ygh-applications"

# 检查所有服务模块
services = [
    "ygh-admin", "ygh-ai", "ygh-inventory", "ygh-knowledge",
    "ygh-notification", "ygh-order", "ygh-product", "ygh-search",
    "ygh-training", "ygh-wallet"
]

print("=== 检查旧项目各服务源码情况 ===")
for svc in services:
    old_svc = os.path.join(old_base, svc)
    new_svc = os.path.join(new_base, svc)
    
    # 检查service子模块
    old_service = os.path.join(old_svc, f"{svc}-service")
    new_service = os.path.join(new_svc, f"{svc}-service")
    
    old_src = os.path.join(old_service, "src")
    new_src = os.path.join(new_service, "src")
    
    old_has_src = os.path.isdir(old_src)
    new_has_src = os.path.isdir(new_src)
    
    print(f"\n--- {svc} ---")
    print(f"  旧项目 service模块: {'存在' if os.path.isdir(old_service) else '缺失'}")
    print(f"  旧项目 src目录: {'存在' if old_has_src else '缺失'}")
    print(f"  新项目 src目录: {'存在' if new_has_src else '缺失'}")
    
    if old_has_src:
        # 统计旧项目src下文件数
        old_files = []
        for root, dirs, files in os.walk(old_src):
            for f in files:
                old_files.append(os.path.relpath(os.path.join(root, f), old_src))
        print(f"  旧项目 src文件数: {len(old_files)}")
    
    if new_has_src:
        new_files = []
        for root, dirs, files in os.walk(new_src):
            for f in files:
                new_files.append(os.path.relpath(os.path.join(root, f), new_src))
        print(f"  新项目 src文件数: {len(new_files)}")
