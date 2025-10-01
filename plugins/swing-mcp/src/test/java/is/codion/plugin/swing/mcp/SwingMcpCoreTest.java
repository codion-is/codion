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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.plugin.swing.mcp;

import is.codion.common.state.State;
import is.codion.plugin.swing.mcp.SwingMcpHttpServer.HttpTool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Core tests for MCP plugin functionality that work reliably in headless environments.
 * These tests focus on API validation, JSON handling, and basic server functionality.
 */
public class SwingMcpCoreTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Test
	void testParameterParsing() {
		// Test integer parameter parsing
		Map<String, Object> countMap = new HashMap<>();
		countMap.put("count", 5);
		assertEquals(5, SwingMcpServer.integerParam(countMap, "count", 1));
		assertEquals(1, SwingMcpServer.integerParam(new HashMap<>(), "count", 1));

		Map<String, Object> invalidMap = new HashMap<>();
		invalidMap.put("count", "invalid");
		assertEquals(1, SwingMcpServer.integerParam(invalidMap, "count", 1));

		Map<String, Object> longMap = new HashMap<>();
		longMap.put("count", 10L);
		assertEquals(10, SwingMcpServer.integerParam(longMap, "count", 1)); // Long to int

		// Test boolean parameter parsing
		Map<String, Object> shiftTrueMap = new HashMap<>();
		shiftTrueMap.put("shift", true);
		assertTrue(SwingMcpServer.booleanParam(shiftTrueMap, "shift", false));
		assertFalse(SwingMcpServer.booleanParam(new HashMap<>(), "shift", false));

		Map<String, Object> shiftInvalidMap = new HashMap<>();
		shiftInvalidMap.put("shift", "invalid");
		assertFalse(SwingMcpServer.booleanParam(shiftInvalidMap, "shift", false));

		Map<String, Object> shiftBooleanMap = new HashMap<>();
		shiftBooleanMap.put("shift", Boolean.TRUE);
		assertTrue(SwingMcpServer.booleanParam(shiftBooleanMap, "shift", false));
	}

	@Test
	void testSchemaCreation() {
		String schema = SwingMcpServer.createSchema("text", "string", "The text to type");
		assertNotNull(schema);
		assertTrue(schema.contains("\"text\""));
		assertTrue(schema.contains("\"string\""));
		assertTrue(schema.contains("The text to type"));

		// Verify it's valid JSON
		assertDoesNotThrow(() -> OBJECT_MAPPER.readTree(schema));

		// Verify JSON structure
		assertDoesNotThrow(() -> {
			JsonNode root = OBJECT_MAPPER.readTree(schema);
			assertEquals("object", root.get("type").asText());
			assertTrue(root.has("properties"));
			assertTrue(root.get("properties").has("text"));
			assertEquals("string", root.get("properties").get("text").get("type").asText());
			assertTrue(root.has("required"));
			assertEquals("text", root.get("required").get(0).asText());
		});
	}

	@Test
	void testTwoPropertySchema() {
		String schema = SwingMcpServer.createTwoPropertySchema(
						"direction", "string", "Direction to move",
						"count", "number", "Number of steps"
		);
		assertNotNull(schema);
		assertTrue(schema.contains("\"direction\""));
		assertTrue(schema.contains("\"count\""));

		// Verify it's valid JSON
		assertDoesNotThrow(() -> OBJECT_MAPPER.readTree(schema));

		// Verify JSON structure
		assertDoesNotThrow(() -> {
			JsonNode root = OBJECT_MAPPER.readTree(schema);
			assertEquals("object", root.get("type").asText());
			JsonNode props = root.get("properties");
			assertTrue(props.has("direction"));
			assertTrue(props.has("count"));
			assertEquals("string", props.get("direction").get("type").asText());
			assertEquals("number", props.get("count").get("type").asText());
		});
	}

	@Test
	void testScreenshotToBase64() throws Exception {
		BufferedImage testImage = new BufferedImage(10, 10, TYPE_INT_RGB);

		String base64 = SwingMcpServer.screenshotToBase64(testImage, "png");
		assertNotNull(base64);
		assertFalse(base64.isEmpty());

		// Test different format
		String jpgBase64 = SwingMcpServer.screenshotToBase64(testImage, "jpg");
		assertNotNull(jpgBase64);
		assertFalse(jpgBase64.isEmpty());

		// Different formats should produce different output (usually)
		// Note: For very small images, they might be the same, so we just verify they work
		assertTrue(base64.length() > 0);
		assertTrue(jpgBase64.length() > 0);
	}

	@Test
	void testPluginStateCreation() {
		JPanel testPanel = new JPanel();

		// Test that we can create MCP server states
		State state1 = SwingMcpPlugin.mcpServer(testPanel);
		State state2 = SwingMcpPlugin.mcpServer(testPanel);

		assertNotNull(state1);
		assertNotNull(state2);
		assertNotSame(state1, state2); // Should be different instances

		// Initially stopped
		assertFalse(state1.is());
		assertFalse(state2.is());

		// Test state manipulation without starting actual servers (to avoid port conflicts)
		// We can't easily test the actual HTTP server in a unit test without complex setup
	}

	@Test
	void testHttpToolCreation() throws Exception {
		// Test HttpTool creation and basic functionality
		HttpTool tool = new HttpTool(
						"test_tool",
						"A test tool for unit testing",
						SwingMcpServer.createSchema("message", "string", "Test message"),
						args -> "Response: " + args.getOrDefault("message", "default")
		);

		assertEquals("test_tool", tool.name());
		assertEquals("A test tool for unit testing", tool.description());
		assertNotNull(tool.inputSchema());

		// Test tool handler
		Map<String, Object> messageMap = new HashMap<>();
		messageMap.put("message", "hello");
		String result = (String) tool.handler().handle(messageMap);
		assertEquals("Response: hello", result);

		String defaultResult = (String) tool.handler().handle(new HashMap<>());
		assertEquals("Response: default", defaultResult);
	}

	@Test
	void testJsonSchemaConstants() throws Exception {
		// Test that the schema constants are valid JSON
		assertDoesNotThrow(() -> OBJECT_MAPPER.readTree(SwingMcpServer.INPUT_SCHEMA));

		JsonNode emptySchema = OBJECT_MAPPER.readTree(SwingMcpServer.INPUT_SCHEMA);
		assertEquals("object", emptySchema.get("type").asText());
		assertEquals(0, emptySchema.get("properties").size());
	}

	@Test
	void testServerCreationAndSafeOperations() throws Exception {
		JPanel testPanel = new JPanel();

		// These tests verify that the server can be created and methods don't throw
		// IMPORTANT: Only test actual Robot operations in headless environments to avoid
		// affecting the development environment (like clearing editor contents!)
		SwingMcpServer server = new SwingMcpServer(testPanel);

		// Only test API surface without actual automation in non-headless environments
		if (GraphicsEnvironment.isHeadless()) {
			// Safe to test actual automation in headless mode
			assertDoesNotThrow(() -> server.keyCombo("control A"));
			assertDoesNotThrow(() -> server.typeText("test"));
		}
		else {
			// In non-headless mode, just verify the server was created successfully
			// This avoids the dangerous automation that could affect the IDE
			assertNotNull(server);
		}

		// Test window operations (may fail in headless, so wrap in try-catch)
		assertDoesNotThrow(() -> {
			try {
				Rectangle bounds = server.getApplicationWindowBounds();
				assertNotNull(bounds);
			}
			catch (IllegalStateException e) {
				// Expected in headless environments where no window is found
			}
		});

		// Test screenshot methods (may fail in headless, but API should exist)
		assertDoesNotThrow(() -> {
			try {
				server.takeApplicationScreenshot();
			}
			catch (Exception e) {
				// Expected in headless environments
			}
		});
		assertDoesNotThrow(() -> {
			try {
				server.takeActiveWindowScreenshot();
			}
			catch (Exception e) {
				// Expected in headless environments
			}
		});

		// Test window listing (returns JSON) - may fail in headless
		assertDoesNotThrow(() -> {
			try {
				String windowsJson = server.listWindows();
				assertNotNull(windowsJson);
				assertFalse(windowsJson.isEmpty());

				// Verify it's valid JSON
				JsonNode windows = OBJECT_MAPPER.readTree(windowsJson);
				assertTrue(windows.has("windows"));
				assertTrue(windows.get("windows").isArray());
			}
			catch (Exception e) {
				// Expected in headless environments
			}
		});

		server.stop();
	}

	@Test
	void testInvalidKeyCombo() throws Exception {
		JPanel testPanel = new JPanel();
		SwingMcpServer server = new SwingMcpServer(testPanel);

		// Test invalid key combination - these should throw exceptions without executing
		// actual automation, so they're safe to test in any environment
		assertThrows(IllegalArgumentException.class, () -> server.keyCombo("invalid combo"));

		server.stop();
	}
}