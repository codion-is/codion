# Codion MCP Test Tools

This directory contains test tools for the Codion MCP server.

## Files

### test_web_mcp.html
Web-based interface for testing MCP tools when using HTTP mode.

**Usage:**
1. Start your Codion application with MCP server enabled
2. Open test_web_mcp.html in a web browser
3. Use the buttons to test various MCP operations

### test_*.json
Test configuration files for the MCP inspector tools. These reference the Java bridge distribution.

### run_inspector.sh
Helper script for running MCP inspector tools.

## Building the Java Bridge

Before using the test tools, build the Java bridge distribution:

```bash
./gradlew :codion-tools-swing-mcp:installDist
```

This creates a runnable distribution at:
```
tools/swing/mcp/build/install/codion-tools-swing-mcp/
├── bin/
│   ├── codion-tools-swing-mcp      (Unix launcher)
│   └── codion-tools-swing-mcp.bat  (Windows launcher)
└── lib/
    └── (all JAR dependencies)
```

## How It Works

### Architecture
1. **Codion Application** runs with MCP HTTP server enabled (port 8080 by default)
2. **Java Bridge** (`SwingMcpBridge`) translates STDIO to HTTP for Claude Desktop
3. **Claude Desktop** communicates with the bridge via STDIO

### Claude Desktop Configuration

```json
{
  "mcpServers": {
    "codion": {
      "command": "/path/to/codion-tools-swing-mcp/bin/codion-tools-swing-mcp",
      "args": ["8080"]
    }
  }
}
```

Or using the Claude CLI:
```bash
claude mcp add codion /path/to/codion-tools-swing-mcp/bin/codion-tools-swing-mcp 8080
```

### HTTP Mode (Direct Testing)
For testing without Claude Desktop:
1. Start Codion application with MCP server enabled
2. Use test_web_mcp.html or curl to test tools directly

```bash
# Check status
curl http://localhost:8080/mcp/status

# List tools
curl -X POST http://localhost:8080/mcp/tools/list -H "Content-Type: application/json" -d '{}'
```

## Available Tools

When connected, the following tools are available:
- `type_text` - Type text into fields
- `key` - Press keyboard combinations (supports repeat and description)
- `clear_field` - Clear current field
- `app_screenshot` - Take application screenshot
- `active_window_screenshot` - Take active window screenshot
- `app_window_bounds` - Get window position/size
- `focus_window` - Bring application to front
- `list_windows` - List all application windows
- `wait` - Wait for specified milliseconds

## Creating a Distributable ZIP

To create a standalone distribution for deployment:

```bash
./gradlew :codion-tools-swing-mcp:distZip
```

Output: `tools/swing/mcp/build/distributions/codion-tools-swing-mcp-<version>.zip`
