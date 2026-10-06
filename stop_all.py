import subprocess
import time

# 用tasklist查找java进程
result = subprocess.run(['tasklist', '/FI', 'IMAGENAME eq java.exe', '/FO', 'CSV'], 
                       capture_output=True, text=True, encoding='gbk')
lines = result.stdout.strip().split('\n')
print("=== 当前Java进程 ===")
pids = []
for line in lines[1:]:
    if line.strip():
        parts = line.split(',')
        pid = parts[1].strip('"')
        pids.append(pid)
        print(f"  PID {pid}")

if pids:
    print(f"\n=== 停止 {len(pids)} 个Java进程 ===")
    for pid in pids:
        subprocess.run(['taskkill', '/F', '/PID', pid], capture_output=True)
        print(f"  已停止 PID {pid}")
else:
    print("没有运行中的Java进程")

time.sleep(2)

# 检查端口
print("\n=== 端口检查 ===")
import socket
for port in [8080, 8081, 8082, 8083]:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(0.5)
    result = s.connect_ex(('127.0.0.1', port))
    s.close()
    if result == 0:
        print(f"  Port {port} 仍被占用")
    else:
        print(f"  Port {port} 已释放")
