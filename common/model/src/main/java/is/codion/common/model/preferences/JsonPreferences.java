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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

/**
 * In-memory hierarchical JSON storage for preferences — the storage half of {@link JsonPreferencesStore}, without any
 * file handling. Path-addressed ({@code "a/b/c"}) get/put/remove of string values, plus child-node listing. Thread-safe.
 * <p>
 * A value is returned as it was put. One that is JSON in the form this class writes it, {@code {"w":100,"i":0}},
 * is stored nested, for a readable file, and may come back with its members in another order once the file has
 * been read again; any other value is stored as the string it is, text that merely starts with a bracket included.
 * <p>
 * {@link #jsonPreferences()} wraps a fresh instance as a free-floating, file-less {@link Preferences} node — useful for
 * capturing or transferring preference state in memory (dependency panels) and for hermetic tests that must
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
	//the changes made since the last load or save, re-applied on top of the file when it has been modified externally
	private final @Nullable List<Consumer<JsonPreferences>> journal;

	JsonPreferences() {
		this(false);
	}

	/**
	 * @param journal true if the changes made should be journaled, for re-applying them on top of an externally modified file
	 */
	JsonPreferences(boolean journal) {
		this.journal = journal ? new ArrayList<>() : null;
	}

	/**
	 * A key and a child node can not share a name, as with {@link FilePreferences}.
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
			doPut(path, key, value);
			record(preferences -> preferences.doPut(path, key, value));
		}
	}

	private void doPut(String path, String key, String value) {
		synchronized (lock) {
			LOG.trace("Putting value at path '{}', key '{}'", path, key);
			// a key and a child node share the namespace of a node, a value would replace the node, its subtree included
			JSONObject existing = getNode(path);
			if (existing != null && existing.has(key) && isChildNode(existing, key)) {
				throw new IllegalStateException("Cannot put a value for key '" + key + "' at path '" + path +
								"' because a child node with the same name exists");
			}
			JSONObject node = getOrCreateNode(path);

			// Try to parse the value as JSON
			Object valueToStore = tryParseJson(value);
			// nested only when it reads back as the very text given, the parser being lenient and get() returning
			// the parsed value written out again, and never when it carries the member marking a node
			if (valueToStore != null && valueToStore.toString().equals(value) && !marksNode(valueToStore)) {
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
			// a child node is not a value
			if (node == null || !node.has(key) || isChildNode(node, key)) {
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
			doRemove(path, key);
			record(preferences -> preferences.doRemove(path, key));
		}
	}

	private void doRemove(String path, String key) {
		synchronized (lock) {
			LOG.trace("Removing key '{}' at path '{}'", key, path);
			JSONObject node = getNode(path);
			// a child node is not a value, removing it would remove its subtree
			if (node != null && node.has(key) && !isChildNode(node, key)) {
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
			return keys(node).stream()
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
			return keys(node).stream()
							.filter(k -> isChildNode(node, k))
							.collect(toSet());
		}
	}

	void removeNode(String path) {
		requireNonNull(path);

		synchronized (lock) {
			doRemoveNode(path);
			record(preferences -> preferences.doRemoveNode(path));
		}
	}

	private void doRemoveNode(String path) {
		synchronized (lock) {
			LOG.debug("Removing node at path '{}'", path);
			if (path.isEmpty()) {
				// Clear root node
				clearObject(data);
			}
			else {
				int lastSeparator = path.lastIndexOf(PATH_SEPARATOR);
				if (lastSeparator >= 0) {
					String parentPath = path.substring(0, lastSeparator);
					String nodeName = path.substring(lastSeparator + 1);
					JSONObject parent = getNode(parentPath);
					// a value is not a node, one of that name is left alone
					if (parent != null && parent.has(nodeName) && isChildNode(parent, nodeName)) {
						parent.remove(nodeName);
					}
				}
				else if (data.has(path) && isChildNode(data, path)) {
					// Direct child of root
					data.remove(path);
				}
			}
		}
	}

	/**
	 * <p>Nodes holding no actual values are left out, a node exists in the file only for the values it holds.
	 * A node created but never written to therefore does not survive a restart, which nothing here relies on.
	 * A value that is an empty JSON object, {@code {}}, is left out as well, read back as missing.
	 * @param prettyPrint true if the JSON should be pretty-printed
	 * @return the whole tree serialized as JSON, null if it holds no actual values (only node markers / empty nodes),
	 * along with the number of journaled changes it contains, to hand back to {@link #saved(int)} once it has been saved
	 */
	Snapshot snapshot(boolean prettyPrint) {
		synchronized (lock) {
			int changes = journal == null ? 0 : journal.size();
			if (isEmpty(data)) {
				return new Snapshot(null, changes);
			}
			//prune a copy, the live tree keeps the nodes the application has created
			JSONObject snapshot = new JSONObject(data.toString());
			prune(snapshot);

			return new Snapshot(snapshot.toString(prettyPrint ? 2 : 0), changes);
		}
	}

	/**
	 * Replaces the entire contents with the given JSON, discarding the journaled changes.
	 * @param json the JSON to load
	 * @throws JSONException in case the JSON is invalid
	 */
	void load(String json) {
		synchronized (lock) {
			replace(json);
			if (journal != null) {
				journal.clear();
			}
		}
	}

	/**
	 * Replaces the entire contents with the given JSON and re-applies the changes journaled since the last
	 * load or save on top of it, so that an external modification does not discard them.
	 * <p>A change that no longer fits, the JSON holding a node where it puts a key or a key where it needs
	 * a node, is left out and logged, the JSON, what has been saved, taking precedence over it.
	 * @param json the JSON to load
	 * @throws JSONException in case the JSON is invalid
	 */
	void merge(String json) {
		synchronized (lock) {
			replace(json);
			if (journal != null) {
				for (Consumer<JsonPreferences> change : journal) {
					try {
						change.accept(this);
					}
					catch (IllegalStateException e) {
						LOG.warn("A change made since the preferences were last read no longer fits them, left out: {}", e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * @return true if changes have been made since the last load or save
	 */
	boolean modified() {
		synchronized (lock) {
			return journal != null && !journal.isEmpty();
		}
	}

	/**
	 * Drops the given number of journaled changes, the oldest, once the snapshot containing them has been saved.
	 * @param changes the number of changes saved, as reported by {@link Snapshot#changes}
	 */
	void saved(int changes) {
		synchronized (lock) {
			if (journal != null) {
				journal.subList(0, changes).clear();
			}
		}
	}

	private void replace(String json) {
		JSONObject reloaded = new JSONObject(json); // may throw, leaving the current contents untouched
		clearObject(data);
		for (String key : keys(reloaded)) {
			data.put(key, reloaded.get(key));
		}
	}

	private void record(Consumer<JsonPreferences> change) {
		if (journal != null) {
			journal.add(change);
		}
	}

	/**
	 * Clears the entire contents, discarding the journaled changes.
	 */
	void clear() {
		synchronized (lock) {
			clearObject(data);
			if (journal != null) {
				journal.clear();
			}
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

	private static boolean marksNode(Object value) {
		return value instanceof JSONObject && ((JSONObject) value).has(NODE);
	}

	private static boolean emptyObject(Object value) {
		return value instanceof JSONObject && ((JSONObject) value).length() == 0;
	}

	// Android's built-in org.json.JSONObject has neither keySet() nor clear(); iterate keys() (an Iterator) instead.
	private static Set<String> keys(JSONObject object) {
		Set<String> keys = new LinkedHashSet<>();
		Iterator<String> iterator = object.keys();
		while (iterator.hasNext()) {
			keys.add(iterator.next());
		}

		return keys;
	}

	private static void clearObject(JSONObject object) {
		keys(object).forEach(object::remove);
	}

	/**
	 * Removes every child node holding no actual preference values, depth first, so that a node
	 * emptied by the removal of its last value-bearing child is removed along with it, and every value
	 * that is an empty object. A value is otherwise left as it is, what it contains being its own.
	 */
	private static void prune(JSONObject node) {
		for (String key : keys(node)) {
			if (isChildNode(node, key)) {
				JSONObject child = node.getJSONObject(key);
				prune(child);
				if (isEmpty(child)) {
					node.remove(key);
				}
			}
			else if (emptyObject(node.get(key))) {
				node.remove(key);
			}
		}
	}

	/**
	 * Recursively checks if a node is empty (contains no actual preference values,
	 * only node markers, empty child nodes or values that are empty objects).
	 */
	private static boolean isEmpty(JSONObject node) {
		for (String key : keys(node)) {
			if (NODE.equals(key)) {
				continue;
			}
			if (isChildNode(node, key)) {
				if (!isEmpty(node.getJSONObject(key))) {
					return false; // Found a non-empty child
				}
			}
			else if (!emptyObject(node.get(key))) {
				return false; // Found an actual value
			}
		}

		return true;
	}

	/**
	 * The tree serialized as JSON along with the number of journaled changes it contains.
	 */
	static final class Snapshot {

		final @Nullable String content;
		final int changes;

		private Snapshot(@Nullable String content, int changes) {
			this.content = content;
			this.changes = changes;
		}
	}
}
