import os
import hashlib
import sys

old_base = r"c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1"
new_base = r"E:\yuegang-zhihui-ai\yuegang-zhihui-ai"

modules = [
    r"ygh-platform\ygh-auth-service\src\main\java",
    r"ygh-platform\ygh-auth-service\src\main\resources",
    r"ygh-platform\ygh-gateway\src\main\java",
    r"ygh-platform\ygh-gateway\src\main\resources",
    r"ygh-common\ygh-common-core\src\main\java",
    r"ygh-common\ygh-common-web\src\main\java",
    r"ygh-common\ygh-common-redis\src\main\java",
    r"ygh-common\ygh-common-security\src\main\java",
    r"ygh-common\ygh-common-mybatis\src\main\java",
    r"ygh-common\ygh-common-mq\src\main\java",
    r"ygh-applications\ygh-user\ygh-user-service\src\main\java",
    r"ygh-applications\ygh-user\ygh-user-api\src\main\java",
    r"ygh-applications\ygh-system\ygh-system-service\src\main\java",
    r"ygh-applications\ygh-system\ygh-system-api\src\main\java",
]

def md5_file(path):
    with open(path, "rb") as f:
        return hashlib.md5(f.read()).hexdigest()

def collect_files(base, sub):
    d = os.path.join(base, sub)
    if not os.path.isdir(d):
        return {}
    result = {}
    for root, dirs, files in os.walk(d):
        for fn in files:
            fp = os.path.join(root, fn)
            rel = os.path.relpath(fp, d)
            result[rel] = fp
    return result

total_same = 0
total_diff = 0
total_new_only = 0
total_old_only = 0
diff_files = []
new_only_files = []
old_only_files = []

for mod in modules:
    old_files = collect_files(old_base, mod)
    new_files = collect_files(new_base, mod)
    
    for rel, new_fp in sorted(new_files.items()):
        old_fp = os.path.join(old_base, mod, rel)
        if not os.path.exists(old_fp):
            new_only_files.append((mod, rel))
            total_new_only += 1
        elif md5_file(new_fp) != md5_file(old_fp):
            diff_files.append((mod, rel))
            total_diff += 1
        else:
            total_same += 1
    
    for rel, old_fp in sorted(old_files.items()):
        new_fp = os.path.join(new_base, mod, rel)
        if not os.path.exists(new_fp):
            old_only_files.append((mod, rel))
            total_old_only += 1

print(f"=== Summary ===")
print(f"Same: {total_same}")
print(f"Different: {total_diff}")
print(f"New only: {total_new_only}")
print(f"Old only: {total_old_only}")

if diff_files:
    print(f"\n=== Different files ({len(diff_files)}) ===")
    for mod, rel in diff_files:
        print(f"  DIFF: {mod}\\{rel}")

if new_only_files:
    print(f"\n=== New only files ({len(new_only_files)}) ===")
    for mod, rel in new_only_files:
        print(f"  NEW: {mod}\\{rel}")

if old_only_files:
    print(f"\n=== Old only files ({len(old_only_files)}) ===")
    for mod, rel in old_only_files:
        print(f"  OLD: {mod}\\{rel}")
