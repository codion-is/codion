# Codion Swing MCP Module

This module provides Model Context Protocol (MCP) integration for Codion Swing applications, enabling external tools (like Claude Desktop) to interact with and control Swing UIs programmatically.

## Overview

The swing-mcp module allows:
- Remote control of Swing applications via keyboard/mouse automation
- Screenshot capture of windows and dialogs
- Window enumeration and management
- HTTP-based MCP integration for any MCP-compatible client

## Architecture

### Core Components

1. **SwingMcpPlugin** (`SwingMcpPlugin.java`)
   - Main plugin class that integrates with Codion applications
   - Starts HTTP server on port 8080 (configurable via `codion.swing.mcp.http.port`)
   - Simplified HTTP-only architecture
   - Uses SLF4J for logging

2. **SwingMcpServer** (`SwingMcpServer.java`)
   - Core UI automation server implementation
   - Manages Robot instance for UI automation
   - Provides all MCP tool implementations  
   - HTTP-only, no STDIO complexity
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
- **key** - Press any key combination using AWT KeyStroke format (replaces tab, arrow, enter, escape)
  - Parameters:
    - `combo` (required) - Key combination in AWT keystroke format
    - `repeat` (optional) - Number of times to repeat the keystroke (default: 1)
    - `description` (optional) - Description of the action associated with this keystroke
- **clear_field** - Select all and delete (convenience function)

### Screenshot Tools
- **app_screenshot** - Application window screenshot (works even when obscured!)
- **active_window_screenshot** - Currently active window screenshot (dialog, popup, etc.)
- **app_window_bounds** - Get application window bounds

### Window Management Tools
- **focus_window** - Bring application window to front
- **click_at** - Click at specific coordinates
- **list_windows** - List all application windows with hierarchy

### Utility Tools
- **wait** - Wait for specified milliseconds

## Recent Improvements (January 2025)

1. **Simplified to HTTP-only architecture** - Removed STDIO complexity for cleaner codebase
2. **Consolidated error handling patterns** - Centralized error handling utilities across tool operations  
3. **Replaced System.err.println with SLF4J logging** - Better logging integration
4. **Implemented direct window painting for screenshots** - Works even when windows are obscured
5. **Added window hierarchy tracking** - `parentWindow` field for dialogs
6. **Refactored to use Jackson ObjectMapper** - Consistent JSON handling, no manual escaping
7. **Added screenshot compression and scaling** - Optimized for AI processing with 1024×768 max size
8. **Implemented JPG format support** - Better compression ratios for faster AI image processing
9. **Added headless environment detection in tests** - Prevents dangerous automation in development environments

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
claude mcp add codion python3 /path/to/codion/tools/swing/mcp/src/main/python/mcp_bridge.py

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

# Press a key combination
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name": "key", "arguments": {"combo": "ENTER"}}'

# Press a key combination with repeat
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name": "key", "arguments": {"combo": "DOWN", "repeat": 3}}'

# Press a key combination with description
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name": "key", "arguments": {"combo": "control S", "description": "Save document"}}'

# Take application screenshot
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name": "app_screenshot", "arguments": {"format": "png"}}'

# Take active window screenshot
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name": "active_window_screenshot", "arguments": {"format": "png"}}'
```

## Building and Testing

```bash
# Build the module
./gradlew :codion-tools-swing-mcp:build

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
Uses AWT KeyStroke format directly - supports any key combination:

**Single Keys:**
- "ENTER", "TAB", "ESCAPE", "DELETE", "INSERT"
- "F1", "F2", ..., "F12"
- "UP", "DOWN", "LEFT", "RIGHT"

**With Modifiers:**
- "control S" - Ctrl+S
- "alt shift F10" - Alt+Shift+F10
- "shift TAB" - Shift+Tab for backward navigation
- "control alt DELETE" - Ctrl+Alt+Delete

**Typed Characters:**
- "typed a" - Type lowercase 'a'
- "typed A" - Type uppercase 'A'
- "typed !" - Type exclamation mark

**Navigation Examples:**
- "UP", "DOWN", "LEFT", "RIGHT" - Arrow keys
- "shift UP" - Shift+Up for selection
- "control HOME" - Ctrl+Home

### JSON Serialization
All JSON responses use Jackson ObjectMapper with record classes:
- WindowInfo, WindowBounds, WindowListResponse
- ScreenshotResponse, ScreenSizeResponse, SaveScreenshotResponse

## Configuration Properties

- `codion.swing.mcp.http.port` - HTTP server port (default: 8080)

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
- Use `app_screenshot` for the main application window (reliable, works even when obscured)
- Use `active_window_screenshot` for the currently active window (dialog, popup, etc.)
- Both screenshot tools work even when windows are partially obscured
- **Format options**: `"png"` (lossless) or `"jpg"` (smaller file size, good for AI processing)
- JPG format is recommended for better compression and faster AI processing
- Check that the target window is available and visible

### Key Combinations Not Working
- Use lowercase for modifier keys: "control", not "CONTROL"
- Separate with spaces: "control alt S", not "control+alt+S"
- Use uppercase for key names: "ENTER", "TAB", "UP", "DOWN"
- For typed characters use: "typed a", not just "a"
- Check KeyEvent VK_ constants for valid key names (without VK_ prefix)

**Examples:**
- ✅ "control S" (Save)
- ✅ "shift TAB" (Backward navigation)
- ✅ "ENTER" (Confirm)
- ✅ "typed @" (Type @ symbol)
- ❌ "ctrl+s" (wrong format)
- ❌ "CONTROL S" (wrong case)
- ❌ "enter" (wrong case for key name)

## Code Style Notes

- Uses Codion's builder pattern throughout
- Static factory methods for consistency
- Record classes for data transfer objects
- Package-private classes (no public API)
- Comprehensive Javadoc on public methods