import os
import hashlib

old_root = r"c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1"
new_root = r"E:\yuegang-zhihui-ai\yuegang-zhihui-ai"

def md5_file(path):
    with open(path, "rb") as f:
        return hashlib.md5(f.read()).hexdigest()

# 检查 ygh-platform 和 ygh-common 的差异
for module in ["ygh-platform", "ygh-common"]:
    old_mod = os.path.join(old_root, module)
    new_mod = os.path.join(new_root, module)
    
    if not os.path.isdir(new_mod):
        continue
    
    print(f"\n=== {module} 差异检查 ===")
    
    # 收集新项目所有文件
    new_files = {}
    for root, dirs, files in os.walk(new_mod):
        # 跳过 target 和 .git
        dirs[:] = [d for d in dirs if d not in ('target', '.git', 'node_modules')]
        for f in files:
            fp = os.path.join(root, f)
            rel = os.path.relpath(fp, new_mod)
            new_files[rel] = fp
    
    # 收集旧项目所有文件
    old_files = {}
    if os.path.isdir(old_mod):
        for root, dirs, files in os.walk(old_mod):
            dirs[:] = [d for d in dirs if d not in ('target', '.git', 'node_modules')]
            for f in files:
                fp = os.path.join(root, f)
                rel = os.path.relpath(fp, old_mod)
                old_files[rel] = fp
    
    missing = 0
    diff = 0
    same = 0
    missing_list = []
    diff_list = []
    
    for rel, new_fp in sorted(new_files.items()):
        old_fp = os.path.join(old_mod, rel) if os.path.isdir(old_mod) else None
        if not old_fp or not os.path.exists(old_fp):
            missing += 1
            missing_list.append(rel)
        elif md5_file(new_fp) != md5_file(old_fp):
            diff += 1
            diff_list.append(rel)
        else:
            same += 1
    
    print(f"  相同: {same}  缺失: {missing}  差异: {diff}")
    if missing_list:
        print(f"  --- 缺失文件 ---")
        for f in missing_list:
            print(f"    {f}")
    if diff_list:
        print(f"  --- 差异文件 ---")
        for f in diff_list:
            print(f"    {f}")
