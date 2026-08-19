#!/usr/bin/env python3
"""
NovaNet UI 本地代理绕过器
功能: 拦截 HTTP 请求，修改 User-Agent 头为 MSIE 兼容性模式
用法: python novanet_proxy.py --port 8888 --target localhost:80
"""

import http.server
import socketserver
import urllib.request
import urllib.error
import sys
import argparse

# MSIE 兼容性 User-Agent
MSIE_UA = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/6.0; SLCC2; .NET CLR 2.0.50727; .NET4.0C; .NET4.0E; .NET CLR 3.5.30729; .NET CLR 3.0.30729)"

class NovaNetProxy(http.server.SimpleHTTPRequestHandler):
    """NovaNet UI 代理服务器"""
    
    target_host = "localhost"
    target_port = 80
    
    def do_GET(self):
        self.proxy_request("GET")
    
    def do_POST(self):
        self.proxy_request("POST")
    
    # 忽略这些无关请求 (Chrome DevTools 探测、favicon 等)
    IGNORED_PATHS = {
        '/.well-known/appspecific/com.chrome.devtools.json',
        '/favicon.ico',
    }

    def proxy_request(self, method):
        # 静默忽略无关请求
        if self.path in self.IGNORED_PATHS:
            self.send_response(404)
            self.end_headers()
            return

        try:
            # 构建目标 URL
            url = f"http://{self.target_host}:{self.target_port}{self.path}"

            # 转发请求头
            headers = {}
            for key in self.headers:
                if key.lower() not in ['host', 'proxy-connection']:
                    headers[key] = self.headers[key]

            # 强制修改 User-Agent 为 MSIE 兼容性模式
            headers['User-Agent'] = MSIE_UA
            headers['Accept'] = 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'

            # 读取 POST 数据
            data = None
            if method == "POST":
                content_length = int(self.headers.get('Content-Length', 0))
                if content_length > 0:
                    data = self.rfile.read(content_length)

            # 发送请求
            req = urllib.request.Request(url, data=data, headers=headers, method=method)

            # 添加 Cookie
            if 'Cookie' in self.headers:
                req.add_header('Cookie', self.headers['Cookie'])

            with urllib.request.urlopen(req) as response:
                # 转发响应
                self.send_response(response.status)
                for key, value in response.getheaders():
                    if key.lower() not in ['transfer-encoding']:
                        self.send_header(key, value)
                self.end_headers()

                # 发送响应体
                self.wfile.write(response.read())

        except urllib.error.HTTPError as e:
            # 404 等错误静默返回，不打印噪音
            self.send_response(e.code)
            for key, value in e.headers.items():
                if key.lower() not in ['transfer-encoding', 'connection']:
                    self.send_header(key, value)
            self.end_headers()
            body = e.read()
            if body:
                self.wfile.write(body)
        except Exception as e:
            self.send_error(502, f"Proxy Error: {e}")
            print(f"[ERROR] {e}")

def main():
    parser = argparse.ArgumentParser(description="NovaNet UI Proxy Bypass")
    parser.add_argument("--port", type=int, default=8888, help="本地代理端口")
    parser.add_argument("--target", default="localhost:80", help="目标服务器")
    args = parser.parse_args()
    
    target_parts = args.target.split(':')
    NovaNetProxy.target_host = target_parts[0]
    NovaNetProxy.target_port = int(target_parts[1]) if len(target_parts) > 1 else 80
    
    # Windows 下允许端口重用
    socketserver.TCPServer.allow_reuse_address = True
    
    with socketserver.TCPServer(("", args.port), NovaNetProxy) as httpd:
        print("=== NovaNet UI Bypass Proxy 已启动 ===")
        print(f"本地端口: http://localhost:{args.port}")
        print(f"目标服务器: http://{args.target}")
        print("MSIE UA 注入: 已启用")
        print("\n使用方法:")
        print(f"  1. 浏览器代理设置为 localhost:{args.port}")
        print(f"  2. 访问 http://{args.target}/frame2_login.php")
        print("  3. 或使用 Chrome 扩展 'SwitchyOmega' 配置")
        print("\n按 Ctrl+C 停止代理")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n代理已停止")

if __name__ == "__main__":
    main()
