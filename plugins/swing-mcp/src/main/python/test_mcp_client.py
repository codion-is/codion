#!/usr/bin/env python3
"""
Simple MCP HTTP client to test Codion MCP server
No external dependencies required (uses only standard library)
"""

import json
import urllib.request
import urllib.parse
import sys

class MCPClient:
    def __init__(self, base_url="http://localhost:8080/mcp"):
        self.base_url = base_url
        
    def _request(self, method, endpoint, data=None):
        url = f"{self.base_url}{endpoint}"
        headers = {"Content-Type": "application/json"}
        
        if data:
            data = json.dumps(data).encode('utf-8')
            
        try:
            req = urllib.request.Request(url, data=data, headers=headers, method=method)
            with urllib.request.urlopen(req, timeout=10) as response:
                return json.loads(response.read().decode('utf-8'))
        except Exception as e:
            print(f"Request failed: {e}")
            return None
    
    def status(self):
        """Check server status"""
        return self._request("GET", "/status")
    
    def initialize(self):
        """Initialize MCP connection"""
        data = {
            "protocolVersion": "1.0.0",
            "clientInfo": {"name": "python-test-client", "version": "1.0.0"}
        }
        return self._request("POST", "/initialize", data)
    
    def list_tools(self):
        """List available tools"""
        return self._request("GET", "/tools/list")
    
    def call_tool(self, name, arguments=None):
        """Call a specific tool"""
        data = {"name": name, "arguments": arguments or {}}
        return self._request("POST", "/tools/call", data)

def main():
    client = MCPClient()
    
    print("=== Testing Codion MCP HTTP Server ===")
    
    # Test 1: Server status
    print("\n1. Checking server status...")
    status = client.status()
    if status:
        print(f"✓ Server running: {status.get('serverName', 'Unknown')} v{status.get('serverVersion', 'Unknown')}")
        print(f"  Tools available: {status.get('toolCount', 0)}")
        print(f"  Auth required: {status.get('authRequired', 'Unknown')}")
    else:
        print("✗ Server not responding. Make sure Chinook is running!")
        return
    
    # Test 2: List tools
    print("\n2. Available tools:")
    tools = client.list_tools()
    if tools and 'tools' in tools:
        for tool in tools['tools']:
            print(f"  - {tool['name']}: {tool['description']}")
    else:
        print("  Failed to get tools list")
        return
    
    # Test 3: Application window bounds
    print("\n3. Testing app_window_bounds...")
    bounds = client.call_tool("app_window_bounds")
    if bounds and 'content' in bounds:
        content = bounds['content']
        if isinstance(content, str):
            content = json.loads(content)
        print(f"  Window bounds: x={content.get('x')}, y={content.get('y')}, "
              f"width={content.get('width')}, height={content.get('height')}")
    else:
        print("  Failed to get window bounds")
    
    # Test 4: Take application screenshot
    print("\n4. Testing app_screenshot...")
    screenshot = client.call_tool("app_screenshot", {"format": "png"})
    if screenshot and 'content' in screenshot:
        content = screenshot['content']
        if isinstance(content, str):
            content = json.loads(content)
        image_size = len(content.get('image', ''))
        print(f"  Screenshot taken: {content.get('width')}x{content.get('height')} pixels")
        print(f"  Base64 data size: {image_size} characters")
        print(f"  Estimated image size: ~{image_size * 3 // 4 // 1024}KB")
    else:
        print("  Failed to take screenshot")
    
    # Get available tools for dynamic command handling
    available_tools = {}
    tools_result = client.list_tools()
    if tools_result and 'tools' in tools_result:
        for tool in tools_result['tools']:
            available_tools[tool['name']] = tool
    
    print("\n5. Interactive mode (type 'help' for commands, 'quit' to exit)")
    print("You can call any MCP tool directly by name (e.g., 'tab', 'app_screenshot', 'key_combo Alt+Tab')")
    
    while True:
        try:
            cmd = input("\nMCP> ").strip()
            if cmd in ['quit', 'exit', 'q']:
                break
            elif cmd == 'help':
                print("Commands:")
                print("  help - Show this help")
                print("  status - Check server status") 
                print("  tools - List available tools")
                print("  quit - Exit")
                print("\nAvailable MCP tools (call directly by name):")
                for tool_name, tool_info in available_tools.items():
                    print(f"  {tool_name} - {tool_info['description']}")
                print("\nExamples:")
                print("  tab - Press Tab key")
                print("  app_screenshot - Take application screenshot")
                print("  type_text Hello - Type 'Hello'")
                print("  key_combo Alt+Tab - Press Alt+Tab")
            elif cmd == 'status':
                result = client.status()
                print(json.dumps(result, indent=2) if result else "Failed")
            elif cmd == 'tools':
                result = client.list_tools()
                if result and 'tools' in result:
                    for tool in result['tools']:
                        print(f"  {tool['name']}: {tool['description']}")
                else:
                    print("Failed to get tools")
            else:
                # Parse command as potential MCP tool call
                parts = cmd.split(' ', 1)
                tool_name = parts[0]
                args_text = parts[1] if len(parts) > 1 else ""
                
                if tool_name in available_tools:
                    # Build arguments based on the tool
                    arguments = {}
                    
                    # Handle specific tools that need arguments
                    if tool_name == 'type_text' and args_text:
                        arguments['text'] = args_text
                    elif tool_name == 'key_combo' and args_text:
                        arguments['combo'] = args_text
                    elif tool_name == 'tab' and args_text:
                        # Parse tab arguments like "tab 3" or "tab 2 shift"
                        tab_parts = args_text.split()
                        if tab_parts:
                            try:
                                arguments['count'] = int(tab_parts[0])
                            except ValueError:
                                pass
                        if len(tab_parts) > 1 and 'shift' in tab_parts[1].lower():
                            arguments['shift'] = True
                    elif tool_name == 'arrow' and args_text:
                        # Parse arrow arguments like "arrow up" or "arrow down 3"
                        arrow_parts = args_text.split()
                        if arrow_parts:
                            arguments['direction'] = arrow_parts[0]
                        if len(arrow_parts) > 1:
                            try:
                                arguments['count'] = int(arrow_parts[1])
                            except ValueError:
                                pass
                    elif tool_name in ['screenshot', 'app_screenshot'] and args_text:
                        arguments['format'] = args_text
                    
                    # Call the tool
                    print(f"Calling {tool_name}...")
                    result = client.call_tool(tool_name, arguments)
                    
                    if result:
                        if 'content' in result:
                            content = result['content']
                            # Handle different response types
                            if tool_name in ['screenshot', 'app_screenshot']:
                                if isinstance(content, str):
                                    content = json.loads(content)
                                print(f"Screenshot taken: {content.get('width')}x{content.get('height')} pixels")
                                if 'format' in content:
                                    print(f"Format: {content['format']}")
                            elif tool_name == 'app_window_bounds':
                                if isinstance(content, str):
                                    content = json.loads(content)
                                print(f"Window bounds: x={content.get('x')}, y={content.get('y')}, width={content.get('width')}, height={content.get('height')}")
                            else:
                                # For simple string responses
                                if isinstance(content, str) and len(content) < 200:
                                    print(f"Result: {content}")
                                else:
                                    print("Success!")
                        else:
                            print("Success!")
                    else:
                        print("Failed")
                else:
                    print(f"Unknown command: {cmd}")
                    print("Type 'help' to see available commands and tools.")
        except KeyboardInterrupt:
            print("\nExiting...")
            break
        except EOFError:
            break

if __name__ == "__main__":
    main()