import socket

def test_port(port):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(2)
    res = s.connect_ex(('127.0.0.1', port))
    s.close()
    return res == 0

print("Port 3306 open:", test_port(3306))
print("Port 3307 open:", test_port(3307))
