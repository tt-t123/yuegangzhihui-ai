import os
import yaml

new_base = r"E:\yuegang-zhihui-ai\yuegang-zhihui-ai\ygh-applications"

services = [
    "ygh-admin", "ygh-ai", "ygh-inventory", "ygh-knowledge",
    "ygh-notification", "ygh-order", "ygh-product", "ygh-search",
    "ygh-training", "ygh-wallet"
]

print("=== 各服务端口配置（从新项目application.yml）===")
for svc in services:
    yml_path = os.path.join(new_base, svc, f"{svc}-service", "src", "main", "resources", "application.yml")
    if not os.path.exists(yml_path):
        print(f"  {svc}: application.yml 不存在")
        continue
    
    with open(yml_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 查找端口配置
    port = "未知"
    for line in content.split('\n'):
        if 'port:' in line and 'YGH_' in line:
            # 提取 ${YGH_XXX_PORT:NNNN} 格式
            if ':' in line:
                parts = line.split(':')
                if len(parts) >= 3:
                    port_part = parts[-1].strip()
                    if '}' in port_part:
                        port = port_part.split('}')[0].split(':')[-1]
                    else:
                        port = port_part
            break
        elif 'port:' in line and 'server' not in line.lower():
            continue
        elif 'port:' in line:
            parts = line.split('port:')
            if len(parts) > 1:
                port = parts[1].strip().strip('"').strip("'")
                if '${' in port:
                    # 提取默认端口
                    if ':' in port and '}' in port:
                        port = port.split(':')[-1].split('}')[0]
                break
    
    # 简单方式：搜索PORT
    import re
    m = re.search(r'YGH_(\w+)_PORT:(\d+)', content)
    if m:
        port = m.group(2)
        env_name = m.group(1)
        print(f"  {svc:20s} 端口={port}  (YGH_{env_name}_PORT)")
    else:
        # 搜索 server: port: NNNN
        m2 = re.search(r'port:\s*(\d+)', content)
        if m2:
            port = m2.group(1)
            print(f"  {svc:20s} 端口={port}  (硬编码)")
        else:
            print(f"  {svc:20s} 端口=未找到")
