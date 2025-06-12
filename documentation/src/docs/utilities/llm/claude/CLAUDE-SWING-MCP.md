# Claude Swing MCP Integration Guide

This guide provides comprehensive instructions for using the Codion Swing MCP (Model Context Protocol) server to create professional demo videos and interact with running Codion applications.

## Overview

The Codion Swing MCP server enables AI assistants to control Swing applications programmatically, making it ideal for:
- Creating polished demo videos without human hesitation or typos
- Automated testing and validation
- Interactive documentation and tutorials
- Application exploration and learning

## Prerequisites

1. **Running Codion Application**: Any Codion demo application with MCP server enabled
2. **MCP Server Active**: Verify the server is running on port 8080 (configurable via `codion.swing.mcp.http.port`)
3. **Client Help Reference**: Familiarize yourself with the [Codion Client Help](../../asciidoc/help/client.adoc) for application-specific shortcuts and patterns

## Available MCP Tools

### Screenshot Tools (Primary for Demos)

#### `mcp__codion__active_window_screenshot` (Default Choice)
- **Purpose**: Capture currently active window (dialogs, popups, main window)
- **Format**: `{"format": "jpg"}` (recommended for compression) or `{"format": "png"}` (lossless)
- **Best Practice**: Use JPG format for faster processing and smaller files
- **When to Use**: **Default choice for all screenshots** - captures whatever is currently active
- **Reliability**: Always captures the topmost active window, whether it's a dialog or main application

#### `mcp__codion__app_screenshot` (Specific Use)
- **Purpose**: Capture the main application window specifically
- **Format**: Same as active_window_screenshot
- **When to Use**: Only when you specifically need the main application window (e.g., when a dialog is open but you want to show the background)
- **Reliability**: Works even when window is obscured, always shows main application

### Navigation Tools

#### `mcp__codion__tab`
- **Purpose**: Navigate between form fields
- **Parameters**: 
  - `count`: Number of tab presses (default: 1)
  - `shift`: Boolean for reverse navigation (default: false)
- **Demo Usage**: Moving through form fields systematically

#### `mcp__codion__arrow`
- **Purpose**: Navigate within tables, lists, and menus
- **Parameters**:
  - `direction`: "up", "down", "left", "right"
  - `count`: Number of presses (default: 1)
- **Demo Usage**: Table row selection, menu navigation

### Input Tools

#### `mcp__codion__type_text`
- **Purpose**: Type text into focused field
- **Parameters**: `text`: String to type
- **Best Practice**: Ensure field is focused first (use tab or click)
- **Demo Usage**: Filling forms, entering search criteria

#### `mcp__codion__key_combo`
- **Purpose**: Execute keyboard shortcuts
- **Parameters**: `combo`: KeyStroke format (e.g., "control S", "alt F4")
- **Demo Usage**: Save operations, menu shortcuts, application commands
- **Format Examples**:
  - `"control S"` - Save
  - `"control alt S"` - Advanced save
  - `"shift F10"` - Context menu
  - `"alt F4"` - Close window

#### `mcp__codion__enter`
- **Purpose**: Press Enter key (advances focus in Codion apps)
- **Demo Usage**: Confirming input, moving to next field

#### `mcp__codion__escape`
- **Purpose**: Press Escape key
- **Demo Usage**: Canceling operations, closing dialogs

#### `mcp__codion__clear_field`
- **Purpose**: Select all and delete current field content
- **Demo Usage**: Clearing existing data before new input

### Window Management Tools

#### `mcp__codion__focus_window`
- **Purpose**: Bring application window to front
- **Demo Usage**: Ensuring application is visible and active

#### `mcp__codion__app_window_bounds`
- **Purpose**: Get window position and size information
- **Returns**: `{x, y, width, height}`
- **Demo Usage**: Verifying window state, troubleshooting layout issues

### Menu and Popup Tools (Future Enhancement)

#### Known Limitation: Menu Visibility
Currently, menus and popups may not be captured by standard screenshot tools:
- **Table popup menus** (triggered by Ctrl+G)
- **Combo box dropdowns** 
- **Context menus** (right-click menus)
- **Application menus** (Alt+F for File menu, etc.)

#### Potential Future Tools
- **`menu_visible`**: Check if any menu is currently visible
- **`active_menu_screenshot`**: Capture visible menus and popups
- **`menu_items`**: List available menu items in current menu

## Demo Script Patterns

### Basic Demo Flow

1. **Start with Screenshot**: Always begin by taking a screenshot to show current state
2. **Focus Application**: Ensure the application window is active
3. **Systematic Navigation**: Use tab/arrow keys for predictable movement
4. **Capture Key Moments**: Screenshot after significant actions
5. **Clean Transitions**: Use escape/enter for smooth dialog handling

### Form Filling Pattern

```markdown
1. Take initial screenshot
2. Focus on first field (tab if needed)
3. Clear existing content (if any)
4. Type new content
5. Move to next field (tab)
6. Repeat until form complete
7. Take final screenshot
8. Submit (enter or specific save command)
```

### Table Navigation Pattern

```markdown
1. Screenshot current table state
2. Use arrow keys for row selection
3. Screenshot selected row
4. Use enter or double-click to edit/view
5. Handle detail view/edit form
6. Return to table (escape or save)
7. Screenshot final state
```

### Dialog Handling Pattern

```markdown
1. Trigger dialog (via menu, button, or shortcut like Alt+S, Alt+I)
2. Screenshot dialog appearance (active_window_screenshot - default choice)
3. Navigate dialog fields (tab navigation or arrow keys for lists)
4. Fill required information or make selection
5. Confirm or cancel (enter/escape)
6. Screenshot result (active_window_screenshot - will show main window once dialog closes)
```

## Demo Best Practices

### Timing and Flow

- **Pause Between Actions**: Allow 1-2 seconds between significant actions for video clarity
- **Systematic Movement**: Always move in predictable patterns (left-to-right, top-to-bottom)
- **Clear Intentions**: Take screenshots before and after important operations
- **Smooth Transitions**: Use standard Codion navigation patterns

### Screenshot Strategy

- **Use JPG Format**: Better compression for faster processing
- **Default Tool**: Use `active_window_screenshot` by default - it captures whatever is currently active
- **Document State Changes**: Screenshot before/after data modifications
- **Capture Everything**: Active window tool automatically captures dialogs, main windows, popups, etc.
- **Specific Cases Only**: Only use `app_screenshot` when you specifically need the main application window
- **Show Results**: Always show the outcome of operations

### Error Prevention

- **Verify Focus**: Ensure correct field is focused before typing
- **Clear Before Type**: Use clear_field to avoid mixing old/new content
- **Check State**: Take screenshots to verify expected state
- **Escape on Problems**: Use escape to back out of problematic dialogs

## Common Workflows

### New Record Creation

1. Navigate to entity panel
2. Press Ctrl+N (or equivalent new record shortcut)
3. Fill mandatory fields systematically
4. Show validation messages (if any)
5. Save record (Ctrl+S)
6. Show success confirmation

### Record Editing

1. Navigate to record in table
2. Select target record (arrow keys)
3. Enter edit mode (Enter or F2)
4. Modify specific fields
5. Show unsaved changes indicators
6. Save changes
7. Verify updates in table view

### Search and Filter

1. Navigate to search field or use Alt+S for search field dialog
2. If Alt+S used: Screenshot the search field selection dialog (active_window_screenshot)
3. Select desired search field (arrow keys + enter)
4. Enter search criteria in the selected field
5. Show filtered results (app_screenshot)
6. Clear search to show all records
7. Demonstrate different search patterns

### Master-Detail Navigation

1. Show master records
2. Select master record
3. Show related detail records appear
4. Navigate within details
5. Show relationship consistency

## Keyboard Shortcuts Reference

### Standard Codion Shortcuts (see client.adoc for complete list)

- **Ctrl+N**: New record
- **Ctrl+S**: Save
- **Ctrl+D**: Delete
- **Ctrl+R**: Refresh
- **F2**: Edit mode
- **F5**: Refresh data
- **Alt+S**: Search field selection dialog (use active_window_screenshot)
- **Alt+I**: Input field selection dialog (use active_window_screenshot)
- **Escape**: Cancel/Close
- **Enter**: Confirm/Next field

### Navigation Shortcuts

- **Tab/Shift+Tab**: Field navigation
- **Arrow Keys**: Table/list navigation
- **Page Up/Down**: Scroll large tables
- **Home/End**: First/last record
- **Ctrl+Home**: First field
- **Ctrl+End**: Last field

## Troubleshooting

### Screenshot Issues

- **Window Not Visible**: Use `focus_window` before screenshot
- **Missing Dialogs**: Always use `active_window_screenshot` (default) to catch dialogs automatically
- **Need Background View**: Only use `app_screenshot` when you specifically need the main window
- **Poor Image Quality**: Prefer JPG format for better compression
- **Large File Sizes**: JPG format significantly reduces file size

### Menu and Popup Issues

- **Invisible Menus**: Current limitation - menus may not appear in screenshots
- **Table Popup Menu**: Ctrl+G triggers popup, but may not be captured
- **Combo Dropdowns**: Dropdown lists may not be visible in screenshots
- **Context Menus**: Right-click menus may not be captured
- **Workaround**: Describe menu interactions verbally until menu tools are implemented

### Navigation Issues

- **Field Not Focused**: Use tab navigation to reach intended field
- **Unexpected Behavior**: Take screenshot to diagnose current state
- **Dialog Problems**: Use escape to cancel and restart operation
- **Table Selection**: Use arrow keys rather than trying to click

### Input Issues

- **Text Not Appearing**: Verify field focus with screenshot
- **Mixed Content**: Use clear_field before typing new content
- **Special Characters**: Some characters may need key combinations
- **Validation Errors**: Screenshot error messages for debugging

## MCP Server Configuration

### Port Configuration

Default port is 8080, configurable via:
```
-Dcodion.swing.mcp.http.port=8080
```

### Logging

The MCP server uses SLF4J logging. Enable debug logging for detailed operation traces:
```
-Dorg.slf4j.simpleLogger.log.is.codion.plugin.swing.mcp=debug
```

### Testing Server Status

Verify server is running:
```bash
curl http://localhost:8080/mcp/status
```

## Integration with Claude Desktop

The MCP server integrates seamlessly with Claude Desktop through the Python bridge. When properly configured, all tools appear in your Claude Desktop interface with the `mcp__codion__` prefix.

### Tool Availability

Tools should appear as:
- `mcp__codion__app_screenshot`
- `mcp__codion__type_text`
- `mcp__codion__tab`
- etc.

If tools are not available, check the MCP configuration and server status.

## Demo Script Templates

### Basic CRUD Demo

```markdown
## Customer Management Demo

1. **Initial State**: Take screenshot of customer panel
2. **New Customer**: 
   - Press Alt+C to clear the form (if necessary)
   - Screenshot new form
   - Fill customer details (Enter or Tab between fields)
   - Add with Alt+A
   - Screenshot saved customer

3. **Edit Customer**:
   - Move focus to the table (Ctrl-T)
   - Select customer in table (arrow keys), down once to select the topmost one (the new one) if the selection is empty
   - Focus the initial focus component on the edit panel (Ctrl-E) or choose a specific input field via Ctrl-I
   - Modify value(s)
   - Update with Alt-U
   - Screenshot updated record

4. **Search Customer**:
   - Select a search field to focus (Ctrl-S)
   - Type search criteria
   - Refresh with Enter
   - Clear search
```

### Master-Detail Demo

```markdown
## Invoice with Line Items Demo

1. **Master Record**: Show invoice list
2. **Selection**: Select specific invoice
3. **Detail Display**: Show invoice lines appear automatically
4. **Detail Navigation**: Navigate within line items
5. **Add Line Item**: Create new line item
6. **Relationship**: Show how master-detail relationship works
```

This guide provides the foundation for creating professional, consistent demos that showcase Codion's capabilities effectively while avoiding the hesitations and errors that come with manual demonstration.

## Known Limitations

1. **Single Application** - Currently supports controlling one application instance
2. **Port Conflicts** - Fixed port 8080 might conflict with other services  
3. **Security** - No authentication (intended for local development use)
4. **Menu Visibility** - Menus, popups, and combo dropdowns may not be captured in screenshots

## Future Improvements

1. **Menu and Popup Support** - Critical for comprehensive demos
   - `menu_visible` - Check if any menu is currently visible
   - `active_menu_screenshot` - Capture menus, popups, combo dropdowns
   - `menu_items` - List available items in current menu
2. **Dynamic Port Selection** - Avoid port conflicts
3. **Multi-Application Support** - Control multiple Codion apps
4. **Mouse Click Tools** - Click on specific components
5. **Component Inspection** - Get component properties and values
6. **Record/Playback** - Record UI interactions for test automation