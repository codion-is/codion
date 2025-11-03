#!/usr/bin/env python3
"""
MCP STDIO to HTTP bridge for Codion MCP server

Bridges the Model Context Protocol (MCP) between Claude Desktop's STDIO 
transport and Codion's HTTP MCP server.
"""

import json
import sys
import urllib.request
import urllib.error
import os
from typing import Optional, Dict, Any


class MCPBridge:
    """Bridge between MCP STDIO and HTTP transports"""
    
    def __init__(self, base_url: str = "http://localhost:8080/mcp"):
        self.base_url = base_url
        self.connected = False
    
    def log(self, message: str, level: str = "INFO") -> None:
        """Log message to stderr"""
        sys.stderr.write(f"[{level}] {message}\n")
        sys.stderr.flush()
    
    def make_request(self, method: str, endpoint: str, data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Make HTTP request to Codion MCP server"""
        url = f"{self.base_url}{endpoint}"
        req_data = json.dumps(data).encode('utf-8') if data else None
        
        req = urllib.request.Request(
            url,
            data=req_data,
            headers={'Content-Type': 'application/json'},
            method=method
        )
        
        try:
            with urllib.request.urlopen(req, timeout=10) as response:
                return json.loads(response.read().decode('utf-8'))
        except urllib.error.HTTPError as e:
            error_message = f"HTTP {e.code}: {e.reason}"
            try:
                error_body = json.loads(e.read().decode('utf-8'))
                if 'error' in error_body:
                    error_message = error_body['error']
            except:
                pass
            return {"error": error_message}
        except urllib.error.URLError as e:
            return {"error": f"Connection failed: {e.reason}"}
        except Exception as e:
            return {"error": f"Request failed: {str(e)}"}
    
    def check_server_connection(self) -> bool:
        """Verify connection to Codion MCP server"""
        self.log("Checking connection to Codion MCP server...")
        status = self.make_request("GET", "/status")
        
        if "error" in status:
            self.log(f"Cannot connect to Codion MCP server: {status['error']}", "ERROR")
            self.log("Make sure Codion is running with the MCP plugin enabled", "ERROR")
            self.log(f"Expected server URL: {self.base_url}", "ERROR")
            return False
        
        server_name = status.get('serverName', 'Unknown')
        server_version = status.get('serverVersion', 'Unknown')
        tool_count = status.get('toolCount', 0)
        
        self.log(f"Connected to {server_name} v{server_version} ({tool_count} tools available)")
        self.connected = True
        return True
    
    def handle_initialize(self, msg_id: str, params: Dict[str, Any]) -> Dict[str, Any]:
        """Handle MCP initialize request"""
        self.log("Initializing MCP connection...")
        
        init_result = self.make_request("POST", "/initialize", params)
        
        if "error" in init_result:
            return {
                "jsonrpc": "2.0",
                "id": msg_id,
                "error": {"code": -32603, "message": f"Initialization failed: {init_result['error']}"}
            }
        
        # Ensure MCP protocol compliance
        result = {
            "protocolVersion": "2024-11-05",  # Latest MCP protocol version
            "serverInfo": init_result.get("serverInfo", {
                "name": "codion-swing-mcp",
                "version": "unknown"
            }),
            "capabilities": {
                "tools": {},  # MCP requires empty object, not boolean
                "logging": {}
            }
        }
        
        self.log("MCP initialization successful")
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "result": result
        }
    
    def handle_tools_list(self, msg_id: str) -> Dict[str, Any]:
        """Handle tools/list request"""
        self.log("Fetching tool list...")
        
        tools_result = self.make_request("GET", "/tools/list")
        
        if "error" in tools_result:
            return {
                "jsonrpc": "2.0",
                "id": msg_id,
                "error": {"code": -32603, "message": f"Failed to fetch tools: {tools_result['error']}"}
            }
        
        tool_count = len(tools_result.get('tools', []))
        self.log(f"Retrieved {tool_count} tools")
        
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "result": tools_result
        }
    
    def handle_tool_call(self, msg_id: str, params: Dict[str, Any]) -> Dict[str, Any]:
        """Handle tools/call request"""
        tool_name = params.get("name", "")
        arguments = params.get("arguments", {})
        
        self.log(f"Calling tool: {tool_name}")
        
        call_result = self.make_request("POST", "/tools/call", {
            "name": tool_name,
            "arguments": arguments
        })
        
        if "error" in call_result:
            return {
                "jsonrpc": "2.0",
                "id": msg_id,
                "error": {"code": -32603, "message": f"Tool execution failed: {call_result['error']}"}
            }
        
        # Handle different response types
        content = call_result.get("content", "")
        
        # Screenshot tools return structured image data
        if self._is_screenshot_tool(tool_name) and isinstance(content, dict) and "image" in content:
            return self._create_image_response(msg_id, content)
        
        # JSON responses (like window bounds, window list)
        elif isinstance(content, dict):
            return self._create_json_response(msg_id, content)
        
        # Simple text responses
        else:
            return self._create_text_response(msg_id, str(content))
    
    def _is_screenshot_tool(self, tool_name: str) -> bool:
        """Check if tool returns screenshot data"""
        return tool_name in ["app_screenshot", "active_window_screenshot"]
    
    def _create_image_response(self, msg_id: str, content: Dict[str, Any]) -> Dict[str, Any]:
        """Create MCP response for image content"""
        image_format = content.get("format", "png")
        image_data = content.get("image", "")
        width = content.get("width", 0)
        height = content.get("height", 0)
        
        self.log(f"Screenshot captured: {width}x{height} {image_format}")
        
        return {
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
                        "text": f"Screenshot captured: {width}x{height} pixels ({image_format})"
                    }
                ]
            }
        }
    
    def _create_json_response(self, msg_id: str, content: Dict[str, Any]) -> Dict[str, Any]:
        """Create MCP response for JSON content"""
        formatted_json = json.dumps(content, indent=2)
        
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "result": {
                "content": [
                    {
                        "type": "text", 
                        "text": formatted_json
                    }
                ]
            }
        }
    
    def _create_text_response(self, msg_id: str, content: str) -> Dict[str, Any]:
        """Create MCP response for text content"""
        return {
            "jsonrpc": "2.0",
            "id": msg_id,
            "result": {
                "content": [
                    {
                        "type": "text",
                        "text": content
                    }
                ]
            }
        }
    
    def handle_notification(self, method: str, params: Optional[Dict[str, Any]] = None) -> None:
        """Handle MCP notification (no response required)"""
        self.log(f"Received notification: {method}")
        
        if method == "notifications/initialized":
            self.log("MCP client initialized successfully")
    
    def run(self) -> None:
        """Main STDIO bridge loop"""
        if not self.check_server_connection():
            sys.exit(1)
        
        self.log("Starting MCP STDIO bridge...")
        
        while True:
            try:
                line = sys.stdin.readline()
                if not line:  # EOF
                    self.log("STDIO connection closed")
                    break
                
                line = line.strip()
                if not line:  # Empty line
                    continue
                
                try:
                    msg = json.loads(line)
                except json.JSONDecodeError as e:
                    self.log(f"Invalid JSON received: {e}", "ERROR")
                    continue
                
                method = msg.get("method")
                msg_id = msg.get("id")
                params = msg.get("params", {})
                
                # Handle notifications (no response needed)
                if msg_id is None:
                    self.handle_notification(method, params)
                    continue
                
                # Handle requests (response required)
                response = None
                
                if method == "initialize":
                    response = self.handle_initialize(msg_id, params)
                elif method == "tools/list":
                    response = self.handle_tools_list(msg_id)
                elif method == "tools/call":
                    response = self.handle_tool_call(msg_id, params)
                else:
                    self.log(f"Unknown method: {method}", "WARN")
                    response = {
                        "jsonrpc": "2.0",
                        "id": msg_id,
                        "error": {"code": -32601, "message": f"Method not found: {method}"}
                    }
                
                if response:
                    print(json.dumps(response), flush=True)
            
            except KeyboardInterrupt:
                self.log("Bridge interrupted by user")
                break
            except Exception as e:
                self.log(f"Unexpected error: {e}", "ERROR")
                continue


def main():
    """Entry point"""
    # Allow port configuration via environment variable
    port = int(os.environ.get("CODION_MCP_PORT", "8080"))
    base_url = f"http://localhost:{port}/mcp"
    
    bridge = MCPBridge(base_url)
    bridge.run()


if __name__ == "__main__":
    main()