# Codion Swing MCP Module

Model Context Protocol (MCP) integration for Codion Swing applications, letting an MCP client (Claude Code,
Claude Desktop) **drive a running Swing app and verify each step cheaply** — over HTTP, in-process.

The point isn't just "send keystrokes and screenshot." Every keystroke comes back with a **verdict** (did it go
through, which component received it, what it was bound to), input is delivered **deterministically on the event
dispatch thread**, and the **model state** behind the focused component is readable as compact text. So an agent
can act and self-verify without a screenshot after every step — reserving screenshots for genuinely visual checks.

## Architecture

1. **SwingMcpPlugin** — integrates with a Codion application; starts the HTTP server on port 8080
   (`codion.tools.mcp.port`). Toggle at runtime via `SwingMcpPlugin.mcpServer(panel)` (a `State`).
2. **SwingMcpServer** — the tool implementations. Drives the UI through a `Controller`
   (`is.codion.tools.swing.robot`) using the **EDT transport** (`Controller.Transport.EDT`), and reads model
   state through the `UiInspector` SPI (`is.codion.swing.common.ui.inspect`, located via `ServiceLoader`). Uses
   Jackson for JSON.
3. **SwingMcpHttpServer** — HTTP wrapper over the JDK `HttpServer`, maps HTTP to MCP.
4. **SwingMcpBridge** — STDIO↔HTTP bridge for Claude Desktop (built as a runnable distribution).

The module stays **framework-agnostic**: it depends on `swing-common-ui` + `robot`, not on framework-ui. The
entity-aware introspection arrives at runtime through the `UiInspector` ServiceLoader boundary (framework-ui
`provides` `EntityEditorInspector` and `EntityTableModelInspector`).

## Available MCP tools

### Input (each returns a verdict)
- **type_text** `{text}` — type into the focused field. Returns an `Interaction` verdict (below).
- **key** `{combo, repeat?, description?}` — press an AWT keystroke (e.g. `ENTER`, `ctrl S`, `shift TAB`,
  `alt A`, `typed a`). Returns a verdict incl. the bound `action`.
- **interactions** `{interactions: [...]}` — run an ordered **batch** (fill a whole form) in one call. Each step
  has `key` (optional `repeat`), `text`, or `wait` (ms). Stops at the first that does not go through and
  **localizes** it. Returns `{ok:true, executed:n}` (plus `fellThrough` if any step did nothing), or
  `{ok:false, failedAt:i, step, delivery, component}`. Use `wait` steps to let async work settle (see below).
- **clear_field** — select-all + delete (convenience).

### Introspection (cheap text, no pixels)
- **model_state** — the state of the model behind the focused component. In an **edit panel**: per-attribute
  `value/valid/modified/message` + entity `exists/modified/valid`. In a **table**: `rowCount`, `selectionCount`,
  `selectedIndex`, `selected`. In a **condition/filter field**: `type` (`condition` = sent to the DB, `filter` =
  client-side), `attribute`, `operator`, `operands`, `enabled`. Which one you get depends on where focus is
  (exactly one inspector applies; a condition field reports the condition, not the enclosing table).

### Screenshots
- **app_screenshot** `{format}` — the main window, via direct painting (works even when obscured). `png` or `jpg`.
- **active_window_screenshot** `{format}` — the currently active window (dialog/popup). Use this to see dialogs.
- **app_window_bounds** — the application window bounds.

### Window / narrator
- **focus_window** — bring the app window to front.
- **narrate**, **clear_narration**, **clear_keystrokes** — on-screen narration overlay (only present when a
  narrator is attached; used for demo recording).

## Driving an app effectively (read this before controlling a UI)

The whole design exists so you *don't* screenshot after every keystroke. The loop:

1. **Act.** For a known flow, prefer **`interactions`** — one call fills a form and is one agent turn (this matters
   at high reasoning effort, where the cost is the number of turns). For single/adaptive steps use `key`/`type`.
2. **Read the verdict.** `component` tells you *where the keystroke landed*, named by its attribute
   (`NumberField[employees.department.department_no]`) — so you know focus is right without looking. `action` is
   the Codion binding it resolved to. Note: for a focus-transfer key (`ENTER`, `TAB`) the `component` is the field
   being *left*; the destination shows in the *next* input's verdict.
   - `CONSUMED` = a component handled it. `FELL_THROUGH` = observed but nothing consumed it (often wrong focus).
     `MISSED` = it did not go through.
   - A **surprising `component`**, a `FELL_THROUGH`, or a `MISSED` is your cue to look — that's when a screenshot
     earns its cost.
3. **Assert state with `model_state`**, not pixels — confirm values/validity/messages after an edit, or the
   selection after navigating a table. The post-insert "form reset to empty" is itself the success signal.
4. **Screenshot only** for genuinely visual checks, or to confirm a surprising verdict/state. Use
   `active_window_screenshot` for dialogs.

### `wait` and async application behaviour
Input *delivery* is deterministic, but the app's *response* can be async (a table refresh or master-detail
selection runs a background query; a combo filters; a calendar popup returns focus). A rapid batch can outrun it —
the next keystroke lands before the app settled. Insert **`{wait: ms}`** steps after the async points:
- after an **insert** (`alt A`) — table refresh + master-detail selection
- after **panel navigation** (`ctrl alt DOWN`/`UP`) — master-detail propagation
- after a **combo selection** — the filter settling
- after a **calendar close** — the async focus-return

If a batch fails, `failedAt` points at the exact step where a settle is missing (e.g. a `FocusLostException` — no
focus owner — means focus hadn't returned yet). Add a `wait` before it and re-run. This is the same pacing the
demo scripts use (`DemoScript`), expressed declaratively.

## Response formats

Interaction verdict (`key`/`type`):
```json
{ "keyStroke": "ctrl S", "delivery": "CONSUMED",
  "component": "JTextField[employees.department.name]", "action": "EntityEditPanel save ctrl S WHEN_IN_FOCUSED_WINDOW" }
```
`model_state` (edit panel):
```json
{ "entityType": "employees.department", "exists": false, "modified": false, "valid": true,
  "attributes": [ { "attribute": "name", "value": "Demo", "valid": true, "modified": true, "original": "null" } ] }
```
`model_state` (table): `{ "entityType": "...", "rowCount": 5, "selectionCount": 1, "selectedIndex": 0, "selected": ["Demo"] }`
`model_state` (condition/filter field): `{ "entityType": "...", "type": "condition", "attribute": "employees.department.name", "enabled": true, "operator": "EQUAL", "operands": { "equal": "SALES" } }`

`interactions`: `{ "ok": true, "executed": 12 }` or `{ "ok": false, "failedAt": 6, "step": {...}, "delivery": "MISSED", "component": "..." }`

Screenshot: `{ "image": "<base64>", "width": 1920, "height": 1080, "format": "png" }`

## Usage

Start the server from an application panel:
```java
State mcpServer = SwingMcpPlugin.mcpServer(this);
mcpServer.set(true);
```
Build the bridge distribution: `./gradlew :codion-tools-swing-mcp:installDist` (or `distZip`).

Configure Claude Desktop:
```json
{ "mcpServers": { "codion": { "command": "/path/to/codion-tools-swing-mcp/bin/codion-tools-swing-mcp", "args": ["8080"] } } }
```

Direct HTTP testing:
```bash
curl http://localhost:8080/mcp/status
curl -X POST http://localhost:8080/mcp/tools/call -H "Content-Type: application/json" \
  -d '{"name": "key", "arguments": {"combo": "ENTER"}}'                     # → interaction verdict
curl -X POST http://localhost:8080/mcp/tools/call -H "Content-Type: application/json" \
  -d '{"name": "interactions", "arguments": {"interactions": [{"text":"Demo"},{"key":"ENTER"},{"wait":300}]}}'
curl -X POST http://localhost:8080/mcp/tools/call -H "Content-Type: application/json" \
  -d '{"name": "model_state", "arguments": {}}'
```

## Key implementation details

- **EDT transport.** `SwingMcpServer` builds `Controller.controller(Controller.Transport.EDT)`. Keystrokes are
  synthesized and **posted to the EventQueue** targeting the focus owner, with an `invokeAndWait` barrier — no
  OS focus race, no dropped events, and app exceptions during dispatch surface through the app's own handler (so
  a validation dialog appears rather than the tool erroring). Demo recording still uses `Transport.ROBOT`.
- **Verification.** The `Controller`'s `KeyboardFocusManager` listeners observe every key event; a requested
  `KeyStroke` is matched to the observed event via `KeyStroke.getKeyStrokeForEvent`, and the post-processor's
  `isConsumed()` gives the `Delivery`. `Interaction` is a plain class (not a record — the module merges to a
  JDK 8 branch).
- **Introspection.** `UiInspector` (common-ui) is a `ServiceLoader` SPI; framework-ui provides
  `EntityEditorInspector` / `EntityTableModelInspector`, which project the `EntityEditor` / `EntityTableModel`
  observables to a map. `model_state` runs the inspectors on the EDT and returns the first that applies.
- **Screenshots** paint the window directly (`window.paint(graphics)`), so they work regardless of z-order; JPG
  is preferred for AI processing; images are scaled to a 1024-wide max.

## Configuration / limitations

- `codion.tools.mcp.port` (default 8080).
- One application at a time; fixed port; no authentication (local-development use).

## Future ideas
- Dynamic port selection; multi-application support; a mouse/`invoke`-by-`Control`-name tool; richer table
  introspection (filtered/total counts, selected entities as attribute maps); a self-test harness that scripts a
  CRUD flow with assertions (`interactions` + `model_state`).

## Notes for editing this module
- Package-private classes, no public API surface added lightly; builders + static factories; comprehensive
  javadoc. Tests detect headless environments (`GraphicsEnvironment.isHeadless()`) to avoid driving a UI in CI.
- The verification/transport lives in `is.codion.tools.swing.robot` (`Controller`, `Interaction`, `Verifier`);
  the introspection SPI in `is.codion.swing.common.ui.inspect`. Design notes: `.claude/swing-agent-control.md`.
