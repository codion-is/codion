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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.prefs.Preferences;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

/**
 * In-memory hierarchical JSON storage for preferences — the storage half of {@link JsonPreferencesStore}, without any
 * file handling. Path-addressed ({@code "a/b/c"}) get/put/remove of string values (a value that is itself valid JSON is
 * stored nested), plus child-node listing. Thread-safe.
 * <p>
 * {@link #jsonPreferences()} wraps a fresh instance as a free-floating, file-less {@link Preferences} node — useful for
 * capturing or transferring preference state in memory (dependency panels, migration) and for hermetic tests that must
 * not touch the real user preferences store.
 * @see #jsonPreferences()
 * @see JsonPreferencesStore
 * @see FilePreferences
 */
public final class JsonPreferences {

	private static final Logger LOG = LoggerFactory.getLogger(JsonPreferences.class);

	private static final String PATH_SEPARATOR = "/";
	private static final String NODE = ".node";

	private final Object lock = new Object();
	private final JSONObject data = new JSONObject();

	JsonPreferences() {}

	/**
	 * @return a file-less, in-memory {@link Preferences} node backed by a fresh {@link JsonPreferences}
	 */
	public static Preferences jsonPreferences() {
		return new FilePreferences(new JsonPreferences());
	}

	void put(String path, String key, String value) {
		requireNonNull(path);
		requireNonNull(key);
		requireNonNull(value);

		synchronized (lock) {
			LOG.trace("Putting value at path '{}', key '{}'", path, key);
			JSONObject node = getOrCreateNode(path);

			// Try to parse the value as JSON
			Object valueToStore = tryParseJson(value);
			if (valueToStore != null) {
				// Valid JSON - store as nested object/array
				node.put(key, valueToStore);
				LOG.trace("Stored value as JSON object/array for key '{}'", key);
			}
			else {
				// Not valid JSON - store as string
				node.put(key, value);
			}
		}
	}

	@Nullable String get(String path, String key) {
		requireNonNull(path);
		requireNonNull(key);

		synchronized (lock) {
			JSONObject node = getNode(path);
			if (node == null || !node.has(key)) {
				return null;
			}

			Object value = node.get(key);
			if (value instanceof String) {
				return (String) value;
			}
			else if (value instanceof JSONObject || value instanceof JSONArray) {
				// Convert JSON object/array back to string
				return value.toString();
			}
			else {
				// Handle other types (numbers, booleans)
				return String.valueOf(value);
			}
		}
	}

	void remove(String path, String key) {
		requireNonNull(path);
		requireNonNull(key);

		synchronized (lock) {
			LOG.trace("Removing key '{}' at path '{}'", key, path);
			JSONObject node = getNode(path);
			if (node != null) {
				node.remove(key);
			}
		}
	}

	Set<String> keys(String path) {
		requireNonNull(path);

		synchronized (lock) {
			JSONObject node = getNode(path);
			if (node == null) {
				return emptySet();
			}

			// Return all keys except those that are child nodes (JSONObjects used for hierarchy)
			// Note: JSONObjects/JSONArrays that represent stored JSON values should be included
			return node.keySet().stream()
							.filter(k -> !isChildNode(node, k) && !NODE.equals(k))
							.collect(toSet());
		}
	}

	Set<String> childrenNames(String path) {
		requireNonNull(path);

		synchronized (lock) {
			JSONObject node = getNode(path);
			if (node == null) {
				return emptySet();
			}

			// Return only child nodes (JSONObjects used for hierarchy)
			return node.keySet().stream()
							.filter(k -> isChildNode(node, k))
							.collect(toSet());
		}
	}

	void removeNode(String path) {
		requireNonNull(path);

		synchronized (lock) {
			LOG.debug("Removing node at path '{}'", path);
			if (path.isEmpty()) {
				// Clear root node
				data.keySet().clear();
			}
			else {
				int lastSeparator = path.lastIndexOf(PATH_SEPARATOR);
				if (lastSeparator >= 0) {
					String parentPath = path.substring(0, lastSeparator);
					String nodeName = path.substring(lastSeparator + 1);
					JSONObject parent = getNode(parentPath);
					if (parent != null) {
						parent.remove(nodeName);
					}
				}
				else {
					// Direct child of root
					data.remove(path);
				}
			}
		}
	}

	/**
	 * @param prettyPrint true if the JSON should be pretty-printed
	 * @return the whole tree serialized as JSON, or null if it holds no actual values (only node markers / empty nodes)
	 */
	@Nullable String snapshot(boolean prettyPrint) {
		synchronized (lock) {
			if (isEmpty(data)) {
				return null;
			}

			return data.toString(prettyPrint ? 2 : 0);
		}
	}

	/**
	 * Replaces the entire contents with the given JSON.
	 * @param json the JSON to load
	 * @throws JSONException in case the JSON is invalid
	 */
	void load(String json) {
		synchronized (lock) {
			JSONObject reloaded = new JSONObject(json); // may throw, leaving the current contents untouched
			data.keySet().clear();
			for (String key : reloaded.keySet()) {
				data.put(key, reloaded.get(key));
			}
		}
	}

	/**
	 * Clears the entire contents.
	 */
	void clear() {
		synchronized (lock) {
			data.keySet().clear();
		}
	}

	private @Nullable JSONObject getNode(String path) {
		if (path.isEmpty()) {
			return data;
		}
		String[] parts = path.split(PATH_SEPARATOR);
		JSONObject current = data;
		for (String part : parts) {
			if (!current.has(part) || !(current.get(part) instanceof JSONObject)) {
				return null;
			}
			current = current.getJSONObject(part);
		}

		return current;
	}

	private JSONObject getOrCreateNode(String path) {
		if (path.isEmpty()) {
			return data;
		}
		String[] parts = path.split(PATH_SEPARATOR);
		JSONObject current = data;
		for (String part : parts) {
			if (!current.has(part)) {
				JSONObject newNode = new JSONObject();
				// Mark this as a preferences node, not a stored JSON value
				newNode.put(NODE, true);
				current.put(part, newNode);
			}
			else if (!(current.get(part) instanceof JSONObject)) {
				// A value exists at this path, cannot create a node
				LOG.error("Cannot create node '{}' at path '{}' because a value already exists", part, path);
				throw new IllegalStateException("Cannot create node '" + part +
								"' because a value already exists at this path");
			}
			current = current.getJSONObject(part);
		}

		return current;
	}

	/**
	 * Attempts to parse a string value as JSON.
	 * @param value the string to parse
	 * @return a JSONObject or JSONArray if the value is valid JSON, null otherwise
	 */
	private static @Nullable Object tryParseJson(String value) {
		// Quick check to avoid parsing non-JSON strings
		if (value == null || value.isEmpty()) {
			return null;
		}

		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}

		// Only try to parse if it looks like JSON (starts with { or [)
		char firstChar = trimmed.charAt(0);
		if (firstChar != '{' && firstChar != '[') {
			return null;
		}

		try {
			if (firstChar == '{') {
				return new JSONObject(value);
			}
			else {
				return new JSONArray(value);
			}
		}
		catch (JSONException e) {
			// Not valid JSON, return null to store as string
			return null;
		}
	}

	/**
	 * Checks if a key represents a child node (used for preferences hierarchy)
	 * rather than a stored value.
	 */
	private static boolean isChildNode(JSONObject parent, String key) {
		Object value = parent.get(key);
		if (!(value instanceof JSONObject)) {
			return false;
		}

		JSONObject obj = (JSONObject) value;

		// A node is marked with the special ".node" property
		return obj.optBoolean(NODE, false);
	}

	/**
	 * Recursively checks if a node is empty (contains no actual preference values,
	 * only node markers or empty child nodes).
	 */
	private static boolean isEmpty(JSONObject node) {
		for (String key : node.keySet()) {
			if (NODE.equals(key)) {
				continue;
			}
			Object value = node.get(key);
			if (value instanceof JSONObject) {
				if (!isEmpty((JSONObject) value)) {
					return false; // Found a non-empty child
				}
			}
			else {
				return false; // Found an actual value
			}
		}

		return true;
	}
}
