/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.tools.swing.mcp;

import java.util.List;

/**
 * The tools an application provides, the name, description and input schema of each.
 * {@link SwingMcpServer} adds the handler of each one, while {@link SwingMcpBridge} serves
 * them as they are, so that a client can connect before an application is running.
 */
final class Tools {

	static final String TEXT = "text";
	static final String FORMAT = "format";
	static final String STRING = "string";
	static final String INPUT_SCHEMA = "{\"type\": \"object\", \"properties\": {}}";

	private static final String IMAGE_FORMAT = "Image format: 'png' or 'jpg' (default: 'png')";
	private static final String KEY_SCHEMA = """
					{
						"type": "object",
						"properties": {
							"combo": {
								"type": "string",
								"description": "Key combination in AWT keystroke format. Examples: 'ENTER', 'shift ENTER', 'TAB', 'ctrl S', 'ctrl alt LEFT', 'shift TAB', 'alt F4', 'UP', 'DOWN', 'typed a', 'F5'"
							},
							"repeat": {
								"type": "integer",
								"description": "Number of times to repeat the keystroke (default: 1)"
							},
							"description": {
								"type": "string",
								"description": "Optional description of the action associated with this keystroke"
							}
						},
						"required": ["combo"]
					}
					""";
	private static final String INTERACTIONS_SCHEMA = """
					{
						"type": "object",
						"properties": {
							"interactions": {
								"type": "array",
								"description": "Ordered interactions to run in one batch, e.g. to fill a whole form. Each item has either 'key' (an AWT keystroke, optionally repeated) or 'text' (to type).",
								"items": {
									"type": "object",
									"properties": {
										"key": {
											"type": "string",
											"description": "Key combination in AWT keystroke format, e.g. 'ENTER', 'ctrl S', 'TAB', 'alt A'"
										},
										"text": {
											"type": "string",
											"description": "Text to type into the currently focused field"
										},
										"repeat": {
											"type": "integer",
											"description": "Number of times to repeat 'key' (default: 1)"
										},
										"wait": {
											"type": "integer",
											"description": "Milliseconds to wait before the next step, to let asynchronous application work (a table refresh, master-detail selection) settle, e.g. after an insert or navigation"
										},
										"description": {
											"type": "string",
											"description": "Optional description of the step"
										}
									}
								}
							}
						},
						"required": ["interactions"]
					}
					""";

	static final Tool TYPE_TEXT = new Tool("type_text",
					"Type text into the currently focused field. Returns the observed delivery " +
									"(CONSUMED = a component received it, FELL_THROUGH = it did nothing, MISSED = it did not go through) " +
									"and the receiving component.",
					createSchema(TEXT, STRING, "The text to type"));
	static final Tool KEY = new Tool("key",
					"Press a key combination using AWT KeyStroke format. Returns the observed delivery " +
									"(CONSUMED = a component handled it, FELL_THROUGH = it did nothing, MISSED = it did not go through), " +
									"the receiving component and the bound action, if any.",
					KEY_SCHEMA);
	static final Tool INTERACTIONS = new Tool("interactions",
					"Run an ordered batch of interactions (e.g. fill a whole form) in a single call, " +
									"stopping at the first that does not go through. Returns {ok:true, executed:n} on success, or " +
									"{ok:false, failedAt:i, step, delivery, component} at the first MISSED step, localizing the failure. " +
									"Use model_state afterwards to assert the resulting state.",
					INTERACTIONS_SCHEMA);
	static final Tool CLEAR_FIELD = new Tool("clear_field",
					"Clear the current field by selecting all and deleting", INPUT_SCHEMA);
	static final Tool MODEL_STATE = new Tool("model_state",
					"Read the state of the edit model behind the focused component: per-attribute " +
									"value, valid, modified and validation message, plus entity-level exists/modified/valid. " +
									"Cheap structured feedback for verifying state after edits, instead of a screenshot.",
					INPUT_SCHEMA);
	static final Tool FOCUS_STATE = new Tool("focus_state",
					"Read the focus state without stealing the focus: the focus owner and focused window, " +
									"plus for each showing window the focus owner it remembers and the component its focus traversal " +
									"policy falls back to, with whether that one is showing, a fallback which is not showing leaves the " +
									"window without a focus owner. Note that the focus owner is null while another application is active.",
					INPUT_SCHEMA);
	static final Tool APP_SCREENSHOT = new Tool("app_screenshot",
					"Take a screenshot of just the application window and return as base64",
					createSchema(FORMAT, STRING, IMAGE_FORMAT + " (tip: use 'jpg' for better compression)"));
	static final Tool ACTIVE_WINDOW_SCREENSHOT = new Tool("active_window_screenshot",
					"Take a screenshot of the currently active window (dialog, popup, etc.) and return as base64",
					createSchema(FORMAT, STRING, IMAGE_FORMAT + " (tip: use 'jpg' for better compression)"));
	static final Tool APP_WINDOW_BOUNDS = new Tool("app_window_bounds",
					"Get the application window bounds (x, y, width, height)", INPUT_SCHEMA);
	static final Tool FOCUS_WINDOW = new Tool("focus_window",
					"Bring the application window to front and focus it", INPUT_SCHEMA);

	/**
	 * The tools every application provides, the narrator ones depending on a narrator being attached
	 */
	static final List<Tool> TOOLS = List.of(TYPE_TEXT, KEY, INTERACTIONS, CLEAR_FIELD, MODEL_STATE,
					FOCUS_STATE, APP_SCREENSHOT, ACTIVE_WINDOW_SCREENSHOT, APP_WINDOW_BOUNDS, FOCUS_WINDOW);

	private Tools() {}

	/**
	 * @param name the name of the tool
	 * @param description the description of the tool
	 * @param inputSchema the input schema of the tool, as JSON
	 */
	record Tool(String name, String description, String inputSchema) {}

	/**
	 * @param propertyName the property name
	 * @param propertyType the property type
	 * @param propertyDescription the property description
	 * @return an input schema for a single required property
	 */
	static String createSchema(String propertyName, String propertyType, String propertyDescription) {
		return String.format("""
						{
							"type": "object",
							"properties": {
								"%s": {
									"type": "%s",
									"description": "%s"
								}
							},
							"required": ["%s"]
						}
						""", propertyName, propertyType, propertyDescription, propertyName);
	}
}
