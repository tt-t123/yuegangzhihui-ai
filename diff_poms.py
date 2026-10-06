import os
import difflib

old_root = r"c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1"
new_root = r"E:\yuegang-zhihui-ai\yuegang-zhihui-ai"

# 检查关键pom.xml差异
poms_to_check = [
    "ygh-platform/pom.xml",
    "ygh-platform/ygh-auth-service/pom.xml",
    "ygh-platform/ygh-gateway/pom.xml",
    "ygh-common/pom.xml",
    "ygh-common/ygh-common-core/pom.xml",
    "ygh-common/ygh-common-mq/pom.xml",
    "ygh-common/ygh-common-mybatis/pom.xml",
    "ygh-common/ygh-common-redis/pom.xml",
    "ygh-common/ygh-common-security/pom.xml",
    "ygh-common/ygh-common-test/pom.xml",
    "ygh-common/ygh-common-web/pom.xml",
]

for rel in poms_to_check:
    old_fp = os.path.join(old_root, rel)
    new_fp = os.path.join(new_root, rel)
    
    if not os.path.exists(old_fp) or not os.path.exists(new_fp):
        print(f"\n=== {rel} === (文件缺失)")
        continue
    
    with open(old_fp, 'r', encoding='utf-8') as f:
        old_lines = f.readlines()
    with open(new_fp, 'r', encoding='utf-8') as f:
        new_lines = f.readlines()
    
    diff = list(difflib.unified_diff(old_lines, new_lines, 
                                      fromfile=f"旧/{rel}", tofile=f"新/{rel}", n=1))
    if diff:
        print(f"\n=== {rel} ===")
        for line in diff:
            print(line.rstrip())
