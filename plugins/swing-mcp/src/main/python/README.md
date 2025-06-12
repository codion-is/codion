# Codion MCP Bridge Scripts

This directory contains Python scripts for bridging between Claude Desktop's STDIO MCP protocol and Codion's HTTP MCP server.

## Files

### mcp_bridge.py
The main bridge script that connects Claude Desktop to Codion applications via MCP.

**Features:**
- Object-oriented design with proper error handling
- Type hints for better code clarity
- Configurable port via `CODION_MCP_PORT` environment variable
- Structured logging with different log levels
- Robust HTTP error handling with detailed error messages
- MCP protocol compliance with latest specification
- Smart content type detection (images, JSON, text)

**Usage:**
```bash
python3 mcp_bridge.py
```

**With custom port:**
```bash
CODION_MCP_PORT=8081 python3 mcp_bridge.py
```

**Claude Desktop Configuration:**
Add to `~/.config/Claude/claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "codion": {
      "command": "python3",
      "args": ["/path/to/mcp_bridge.py"],
      "env": {
        "CODION_MCP_PORT": "8080"
      }
    }
  }
}
```

### test_mcp_client.py
Interactive command-line client for testing the MCP HTTP server directly.

**Usage:**
```bash
python3 test_mcp_client.py
```

This allows you to:
- List available tools
- Call tools with arguments
- Test the MCP server without Claude Desktop

### test_web_mcp.html
Web-based interface for testing MCP tools visually.

**Usage:**
1. Open in a web browser
2. Ensure Codion app is running with `-Dcodion.plugin.mcp.http.enabled=true`
3. Use the buttons to test various MCP operations

## Requirements

- Python 3.6+
- Codion application running with MCP HTTP server enabled
- For Claude Desktop integration: Claude Desktop installed and configured

## How It Works

1. Claude Desktop starts the bridge script via STDIO
2. Bridge connects to Codion's HTTP MCP server at `http://localhost:8080/mcp`
3. Bridge translates between STDIO JSON-RPC and HTTP REST protocols
4. Tools exposed by Codion become available in Claude Desktop

## Available Tools

When connected, the following tools are available:
- `type_text` - Type text into fields
- `key_combo` - Press keyboard combinations
- `tab` - Navigate with Tab key
- `arrow` - Arrow key navigation
- `enter` - Press Enter key
- `escape` - Press Escape key
- `clear_field` - Clear current field
- `screenshot` - Take desktop screenshot
- `app_screenshot` - Take application screenshot
- `app_window_bounds` - Get window position/size
- `focus_window` - Bring application to front