import os
import re

old_base = r"c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1\ygh-applications"

services = [
    "ygh-admin", "ygh-ai", "ygh-inventory", "ygh-knowledge",
    "ygh-notification", "ygh-order", "ygh-product", "ygh-search",
    "ygh-training", "ygh-wallet"
]

fixed = 0
for svc in services:
    for sub in [f"{svc}-service", f"{svc}-api"]:
        pom_path = os.path.join(old_base, svc, sub, "pom.xml")
        if not os.path.exists(pom_path):
            continue
        
        with open(pom_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original = content
        
        # 修复 parent artifactId
        content = content.replace(
            '<artifactId>yuegang-zhihui-ai</artifactId>',
            '<artifactId>yuegang-zhihui-ai1</artifactId>'
        )
        
        # 修复 parent version
        content = content.replace(
            '<version>${revision}</version>',
            '<version>1.0.0-SNAPSHOT</version>'
        )
        
        # 修复 ygh-dependencies version
        content = content.replace(
            '<version>${revision}</version>',
            '<version>1.0.0-SNAPSHOT</version>'
        )
        
        # 修复依赖中的 ${revision}
        content = re.sub(
            r'<version>\$\{revision\}</version>',
            '<version>1.0.0-SNAPSHOT</version>',
            content
        )
        
        if content != original:
            with open(pom_path, 'w', encoding='utf-8') as f:
                f.write(content)
            fixed += 1
            print(f"  修复: {svc}/{sub}/pom.xml")

# 也修复各服务自己根目录的 pom.xml
for svc in services:
    pom_path = os.path.join(old_base, svc, "pom.xml")
    if not os.path.exists(pom_path):
        continue
    
    with open(pom_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    content = content.replace(
        '<artifactId>yuegang-zhihui-ai</artifactId>',
        '<artifactId>yuegang-zhihui-ai1</artifactId>'
    )
    content = re.sub(
        r'<version>\$\{revision\}</version>',
        '<version>1.0.0-SNAPSHOT</version>',
        content
    )
    
    if content != original:
        with open(pom_path, 'w', encoding='utf-8') as f:
            f.write(content)
        fixed += 1
        print(f"  修复: {svc}/pom.xml")

print(f"\n共修复 {fixed} 个pom.xml")
