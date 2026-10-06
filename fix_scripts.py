"""
修复所有启动脚本中的中文路径问题
移除 Set-Location，改用 mvn -f 完整路径
"""
import os
import glob

BASE_DIR = r"c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1"

# 找到所有 start-*.ps1 脚本
scripts = glob.glob(os.path.join(BASE_DIR, "start-*.ps1"))

for script_path in scripts:
    with open(script_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 移除 Set-Location 行
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        if 'Set-Location' in line:
            continue
        # 将 mvn -f 的相对路径改为完整路径
        # 例如: mvn -f ygh-applications/... 改为 mvn -f "c:\Users\...\ygh-applications/..."
        new_lines.append(line)

    content = '\n'.join(new_lines)

    # 将 mvn -f 的相对路径替换为完整路径
    # 匹配 mvn -f ygh-applications/... 或 mvn -f ygh-platform/...
    import re
    content = re.sub(
        r'& mvn -f (ygh-)',
        lambda m: f'& mvn -f "{BASE_DIR}\\{m.group(1)}',
        content
    )
    # 修复路径分隔符 - 将 "c:\...\ygh-applications/ 改为 "c:\...\ygh-applications\
    content = content.replace(f'"{BASE_DIR}\\ygh-applications/', f'"{BASE_DIR}\\ygh-applications\\')
    content = content.replace(f'"{BASE_DIR}\\ygh-platform/', f'"{BASE_DIR}\\ygh-platform\\')

    # pom.xml 后面可能还有 / 分隔符需要替换
    # 实际上 mvn -f 接受 / 和 \ 混合路径，但为了安全统一为 \
    # 找到 mvn -f "..." 中的路径，将 / 替换为 \
    def fix_path(match):
        path = match.group(1)
        # 只替换路径部分中的 / 为 \，但不影响后面的参数
        return f'& mvn -f "{path}"'

    with open(script_path, 'w', encoding='utf-8') as f:
        f.write(content)

    script_name = os.path.basename(script_path)
    print(f"  修复: {script_name}")

print(f"\n共修复 {len(scripts)} 个脚本")
