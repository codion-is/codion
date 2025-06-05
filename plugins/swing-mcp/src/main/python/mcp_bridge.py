#!/usr/bin/env python3
"""
MCP STDIO to HTTP bridge for Codion MCP server
"""

import json
import sys
import urllib.request

def make_request(method, url, data=None):
    """Make HTTP request"""
    req_data = json.dumps(data).encode('utf-8') if data else None
    req = urllib.request.Request(
        url,
        data=req_data,
        headers={'Content-Type': 'application/json'},
        method=method
    )

    try:
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        return {"error": str(e)}

def main():
    """Main STDIO loop"""
    base_url = "http://localhost:8080/mcp"

    # Check server
    status = make_request("GET", f"{base_url}/status")
    if "error" in status:
        sys.stderr.write(f"ERROR: Cannot connect to Codion MCP server: {status['error']}\n")
        sys.stderr.write("Make sure Codion is running with -Dcodion.mcp.http.enabled=true\n")
        sys.exit(1)

    sys.stderr.write(f"Connected to Codion MCP server\n")

    while True:
        try:
            line = sys.stdin.readline()
            if not line:
                break

            msg = json.loads(line.strip())
            method = msg.get("method")
            msg_id = msg.get("id")

            sys.stderr.write(f"[BRIDGE] Received: {method} (id: {msg_id})\n")

            # Handle notifications (no id, no response needed)
            if msg_id is None:
                sys.stderr.write(f"[BRIDGE] Received notification: {method}\n")
                if method == "notifications/initialized":
                    sys.stderr.write(f"[BRIDGE] Client initialized\n")
                continue

            response = None

            if method == "initialize":
                init_result = make_request("POST", f"{base_url}/initialize", msg.get("params", {}))
                sys.stderr.write(f"[BRIDGE] Server returned: {init_result}\n")

                # Fix the response to match MCP specification
                if isinstance(init_result, dict):
                    # Override protocol version to match Claude Desktop's expectation
                    init_result["protocolVersion"] = "2024-11-05"

                    # Fix capabilities format to match MCP spec
                    if "capabilities" in init_result:
                        caps = init_result["capabilities"]
                        if isinstance(caps, dict):
                            # Convert boolean capabilities to proper MCP format
                            fixed_caps = {}
                            if caps.get("tools"):
                                fixed_caps["tools"] = {}
                            if caps.get("logging"):
                                fixed_caps["logging"] = {}
                            init_result["capabilities"] = fixed_caps

                    sys.stderr.write(f"[BRIDGE] Fixed response: {init_result}\n")

                response = {
                    "jsonrpc": "2.0",
                    "id": msg_id,
                    "result": init_result
                }

            elif method == "tools/list":
                tools_result = make_request("GET", f"{base_url}/tools/list")
                if "error" in tools_result:
                    sys.stderr.write(f"[BRIDGE] Error fetching tools: {tools_result['error']}\n")
                    response = {
                        "jsonrpc": "2.0",
                        "id": msg_id,
                        "error": {"code": -32603, "message": f"Internal error: {tools_result['error']}"}
                    }
                else:
                    sys.stderr.write(f"[BRIDGE] Fetched {len(tools_result.get('tools', []))} tools\n")
                    response = {
                        "jsonrpc": "2.0",
                        "id": msg_id,
                        "result": tools_result
                    }

            elif method == "tools/call":
                params = msg.get("params", {})
                tool_name = params.get("name", "")
                call_result = make_request("POST", f"{base_url}/tools/call", {
                    "name": tool_name,
                    "arguments": params.get("arguments", {})
                })

                content = call_result.get("content", "")
                
                # Handle screenshot tools specially - return as image instead of text
                if tool_name in ["screenshot", "app_screenshot"] and isinstance(content, dict) and "image" in content:
                    image_format = content.get("format", "png")
                    image_data = content.get("image", "")
                    width = content.get("width", 0)
                    height = content.get("height", 0)
                    
                    sys.stderr.write(f"[BRIDGE] Screenshot: {width}x{height} {image_format}\n")
                    
                    response = {
                        "jsonrpc": "2.0",
                        "id": msg_id,
                        "result": {
                            "content": [
                                {
                                    "type": "image", 
                                    "data": image_data,
                                    "mimeType": f"image/{image_format}"
                                },
                                {
                                    "type": "text",
                                    "text": f"Screenshot captured: {width}x{height} pixels"
                                }
                            ]
                        }
                    }
                else:
                    # Regular text response for non-screenshot tools
                    response = {
                        "jsonrpc": "2.0",
                        "id": msg_id,
                        "result": {
                            "content": [{"type": "text", "text": str(content)}]
                        }
                    }

            else:
                response = {
                    "jsonrpc": "2.0",
                    "id": msg_id,
                    "error": {"code": -32601, "message": f"Method not found: {method}"}
                }

            if response:
                sys.stderr.write(f"[BRIDGE] Sending response for {method}\n")
                print(json.dumps(response), flush=True)

        except Exception as e:
            sys.stderr.write(f"[BRIDGE] Error: {e}\n")
            continue

if __name__ == "__main__":
    main()