# Codion Swing MCP Module

This module provides Model Context Protocol (MCP) integration for Codion Swing applications, enabling external tools (like Claude Desktop) to interact with and control Swing UIs programmatically.

## Overview

The swing-mcp module allows:
- Remote control of Swing applications via keyboard/mouse automation
- Screenshot capture of windows and dialogs
- Window enumeration and management
- Integration with any MCP-compatible client

## Architecture

### Core Components

1. **SwingMcpPlugin** (`SwingMcpPlugin.java`)
   - Main plugin class that integrates with Codion applications
   - Starts HTTP server on port 8080 (configurable via `codion.swing.mcp.port`)
   - Registers with ApplicationStartedEvent
   - Uses SLF4J for logging

2. **SwingMcpServer** (`SwingMcpServer.java`)
   - Core MCP server implementation
   - Manages Robot instance for UI automation
   - Provides all MCP tool implementations
   - Uses Jackson ObjectMapper for JSON serialization

3. **SwingMcpHttpServer** (`SwingMcpHttpServer.java`)
   - HTTP server wrapper using JDK's built-in HttpServer
   - Handles MCP protocol over HTTP
   - Maps HTTP endpoints to MCP operations

4. **Python Bridge** (`src/main/python/mcp_bridge.py`)
   - Bridges Claude Desktop MCP to HTTP server
   - Handles stdio ↔ HTTP translation
   - Required because Claude Desktop expects stdio communication

## Available MCP Tools

### Keyboard Tools
- **type_text** - Type text into focused field
- **key_combo** - Press key combinations (e.g., "control alt S", "shift F10")
- **tab** - Tab navigation (with shift for reverse)
- **arrow** - Arrow key navigation
- **enter** - Press Enter key
- **escape** - Press Escape key
- **clear_field** - Select all and delete

### Screenshot Tools
- **screenshot** - Desktop screenshot as base64
- **app_screenshot** - Application window screenshot (works even when obscured!)
- **window_screenshot** - Screenshot by window title
- **focused_window_screenshot** - Currently focused window
- **save_screenshot** - Save screenshot to file
- **screen_size** - Get screen dimensions
- **app_window_bounds** - Get application window bounds

### Window Management Tools
- **focus_window** - Bring application window to front
- **click_at** - Click at specific coordinates
- **list_windows** - List all application windows with hierarchy

### Utility Tools
- **wait** - Wait for specified milliseconds

## Recent Improvements (January 2025)

1. **Replaced System.err.println with SLF4J logging** - Better logging integration
2. **Implemented direct window painting for screenshots** - Works even when windows are obscured
3. **Added window hierarchy tracking** - `parentWindow` field for dialogs
4. **Refactored to use Jackson ObjectMapper** - Consistent JSON handling, no manual escaping
5. **Added three new window tools** - list_windows, window_screenshot, focused_window_screenshot

## JSON Response Formats

### Window List
```json
{
  "windows": [
    {
      "title": "Chinook - Administration",
      "type": "frame",
      "mainWindow": true,
      "focused": false,
      "active": true,
      "modal": false,
      "visible": true,
      "bounds": {"x": 100, "y": 100, "width": 1200, "height": 800}
    },
    {
      "title": "Confirm Delete",
      "type": "dialog",
      "mainWindow": false,
      "focused": true,
      "active": true,
      "modal": true,
      "visible": true,
      "bounds": {"x": 500, "y": 400, "width": 300, "height": 150},
      "parentWindow": "Chinook - Administration"
    }
  ]
}
```

### Screenshot Response
```json
{
  "image": "<base64-encoded-image>",
  "width": 1920,
  "height": 1080,
  "format": "png"
}
```

## Usage

### Starting the MCP Server
See how the server is started in ChinookAppPanel:
```java
State mcpServerController = SwingMcpPlugin.mcpServer(this);

mcpServerController.set(true);
```

### Configuring Claude Desktop
```bash
# Add the MCP server
claude mcp add codion python3 /path/to/codion/plugins/swing-mcp/src/main/python/mcp_bridge.py

# Verify configuration
claude mcp list
```

### Direct HTTP Testing
```bash
# Check status
curl http://localhost:8080/mcp/status

# List tools
curl -X POST http://localhost:8080/mcp/tools/list \
  -H "Content-Type: application/json" \
  -d '{}'

# Take screenshot
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name": "app_screenshot", "arguments": {"format": "png"}}'
```

## Building and Testing

```bash
# Build the module
./gradlew :codion-plugin-swing-mcp:build

# Run with a demo application
./gradlew :codion-demo-chinook:runClientLocal
```

## Key Implementation Details

### Screenshot Implementation
The `takeApplicationScreenshot()` method uses direct window painting instead of Robot screen capture:
```java
BufferedImage image = new BufferedImage(window.getWidth(), window.getHeight(), TYPE_INT_RGB);
Graphics2D graphics = image.createGraphics();
window.paint(graphics);
```
This approach works regardless of window visibility or z-order.

### Key Combination Format
Uses Swing KeyStroke format directly:
- "control S" - Ctrl+S
- "alt shift F10" - Alt+Shift+F10
- "control alt DELETE" - Ctrl+Alt+Delete

### JSON Serialization
All JSON responses use Jackson ObjectMapper with record classes:
- WindowInfo, WindowBounds, WindowListResponse
- ScreenshotResponse, ScreenSizeResponse, SaveScreenshotResponse

## Configuration Properties

- `codion.swing.mcp.port` - HTTP server port (default: 8080)
- `codion.swing.mcp.hostname` - Hostname to bind (default: localhost)

## Known Limitations

1. **Single Application** - Currently supports controlling one application instance
2. **Port Conflicts** - Fixed port 8080 might conflict with other services
3. **Security** - No authentication (intended for local development use)

## Future Improvements

1. **Dynamic Port Selection** - Avoid port conflicts
2. **Multi-Application Support** - Control multiple Codion apps
3. **Mouse Click Tools** - Click on specific components
4. **Component Inspection** - Get component properties and values
5. **Record/Playback** - Record UI interactions for test automation

## Module Dependencies

- Java 17+
- Jackson for JSON (provided by MCP SDK)
- JDK HttpServer (built-in)
- Python 3 for bridge script
- SLF4J for logging

## Troubleshooting

### Tools Not Available in Claude Desktop
1. Check if HTTP server is running: `curl http://localhost:8080/mcp/status`
2. Verify MCP configuration: `claude mcp list`
3. Check logs: `~/.config/Claude/logs/mcp-server-codion.log`
4. Restart Claude Desktop after configuration changes

### Screenshot Issues
- Use `app_screenshot` for reliable window capture
- `screenshot` captures entire desktop (requires window to be visible)
- Check window title exactly matches for `window_screenshot`

### Key Combinations Not Working
- Use lowercase for modifier keys: "control", not "CONTROL"
- Separate with spaces: "control alt S", not "control+alt+S"
- Check KeyEvent constants for valid key names

## Code Style Notes

- Uses Codion's builder pattern throughout
- Static factory methods for consistency
- Record classes for data transfer objects
- Package-private classes (no public API)
- Comprehensive Javadoc on public methods