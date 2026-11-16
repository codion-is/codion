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

import is.codion.common.utilities.property.PropertyValue;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.PreferencesPath.userPreferencesPath;
import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.Configuration.stringValue;
import static java.util.Objects.requireNonNull;

/**
 * <p>A file-based preferences implementation without length restrictions, on keys, values or node names.
 * <p>Supports hierarchical preferences through nested JSON structure.
 * <p>Clearing the preferences instance and flushing deletes the underlying file.
 * <p>A value is returned as it was put, with one exception: a value that is an empty JSON object, {@code {}},
 * is not written to the file, nor is a node holding no values, so that neither is there once the file is read again.
 * A JSON value is stored nested in the file and may come back with its members in another order.
 * <p>A key and a child node can not share a name, both being stored in the same JSON object. Putting a value
 * for a key naming a child node throws {@link IllegalStateException}, as does putting a value into a child node
 * named after a key, while {@link #get(String, String)} and {@link #remove(String)} ignore child nodes.
 * <p>Note that, unlike the {@link AbstractPreferences} contract, {@link java.util.prefs.PreferenceChangeListener}s
 * are not notified of {@link #put(String, String)} calls.
 * @see #filePreferences(String)
 * @see #PREFERENCES_LOCATION
 * @see #PRETTY_PRINT
 * @see UserPreferences
 */
public final class FilePreferences extends AbstractPreferences {

	private static final Logger LOG = LoggerFactory.getLogger(FilePreferences.class);

	/**
	 * Provides a way to override the default (OS dependent) directory used to store the user preferences file.
	 * <p>Note that changing this value after a file based preferences instance has been created has no effect on subsequent instances.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 * @see #filePreferences(String)
	 */
	public static final PropertyValue<String> PREFERENCES_LOCATION = stringValue("codion.preferences.location");

	/**
	 * Specifies whether JSON preference files are pretty-printed, causes significant size increase.
	 * <p>Note that this value is captured when a file based preferences instance is created; changing it
	 * afterwards has no effect on that instance.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	public static final PropertyValue<Boolean> PRETTY_PRINT = booleanValue("codion.preferences.prettyPrint", false);

	private static final Map<Path, FilePreferences> FILE_PREFERENCES = new ConcurrentHashMap<>();

	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final String PATH_SEPARATOR = "/";
	private static final String NODE_HAS_BEEN_REMOVED = "Node has been removed.";

	private final JsonPreferences preferences;
	private final @Nullable JsonPreferencesStore store;
	private final @Nullable FilePreferences parentNode;
	private final String path;
	//the children named by more than MAX_NAME_LENGTH characters, which AbstractPreferences refuses
	//to create and does not know of, cached and removed here instead, guarded by the lock
	private final Map<String, FilePreferences> longNamedChildren = new HashMap<>();

	private FilePreferences(Path path) {
		this(new JsonPreferencesStore(path, PRETTY_PRINT.getOrThrow()));
		LOG.info("Created file preferences: {}", path);
	}

	FilePreferences(JsonPreferencesStore store) {
		this(null, "", store.preferences(), store);
		LOG.debug("Created file preferences with custom store");
	}

	FilePreferences(JsonPreferences preferences) {
		this(null, "", preferences, null);
		LOG.debug("Created in-memory preferences");
	}

	private FilePreferences(@Nullable FilePreferences parent, String name, JsonPreferences preferences,
													@Nullable JsonPreferencesStore store) {
		super(parent, name);
		this.preferences = preferences;
		this.store = store;
		this.parentNode = parent;
		this.path = parent == null ? "" : (parent.path.isEmpty() ? name : parent.path + PATH_SEPARATOR + name);
	}

	@Override
	public void put(String key, String value) {
		requireNonNull(key, "key cannot be null");
		requireNonNull(value, "value cannot be null");
		synchronized (lock) {
			if (isRemoved()) {
				throw new IllegalStateException(NODE_HAS_BEEN_REMOVED);
			}
			putSpi(key, value);
		}
	}

	/**
	 * @param filename the preferences filename
	 * @return a file based Preferences instance based on the given filename
	 */
	public static Preferences filePreferences(String filename) {
		return FILE_PREFERENCES.computeIfAbsent(userPreferencesPath(filename), FilePreferences::new);
	}

	/**
	 * As {@link AbstractPreferences#node(String)}, without the limit on the length of a node name.
	 */
	@Override
	public Preferences node(String path) {
		requireNonNull(path);
		if (!containsLongName(path)) {
			return super.node(path);
		}
		boolean absolute = path.startsWith(PATH_SEPARATOR);
		String relative = absolute ? path.substring(1) : path;
		if (relative.endsWith(PATH_SEPARATOR)) {
			throw new IllegalArgumentException("Path ends with slash");
		}
		FilePreferences node = absolute ? root() : this;
		for (String name : relative.split(PATH_SEPARATOR, -1)) {
			if (name.isEmpty()) {
				throw new IllegalArgumentException("Consecutive slashes in path");
			}
			node = node.child(name);
		}

		return node;
	}

	@Override
	public String[] childrenNames() throws BackingStoreException {
		synchronized (lock) {
			String[] names = super.childrenNames();
			if (longNamedChildren.isEmpty()) {
				return names;
			}
			Set<String> childrenNames = new TreeSet<>(Arrays.asList(names));
			childrenNames.addAll(longNamedChildren.keySet());

			return childrenNames.toArray(EMPTY_STRING_ARRAY);
		}
	}

	/**
	 * As {@link AbstractPreferences#removeNode()}, removing the long named nodes below this one as well.
	 */
	@Override
	public void removeNode() throws BackingStoreException {
		if (parentNode == null) {
			throw new UnsupportedOperationException("Can't remove the root!");
		}
		removeLongNamedDescendants();
		super.removeNode();
		synchronized (parentNode.lock) {
			parentNode.longNamedChildren.remove(name(), this);
		}
	}

	@Override
	protected @Nullable AbstractPreferences getChild(String nodeName) throws BackingStoreException {
		synchronized (lock) {
			FilePreferences longNamedChild = longNamedChildren.get(nodeName);

			return longNamedChild == null ? super.getChild(nodeName) : longNamedChild;
		}
	}

	@Override
	protected void putSpi(String key, String value) {
		preferences.put(path, key, value);
	}

	@Override
	protected @Nullable String getSpi(String key) {
		return preferences.get(path, key);
	}

	@Override
	protected void removeSpi(String key) {
		LOG.trace("Removing preference key '{}' at path '{}'", key, path);
		preferences.remove(path, key);
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		return preferences.keys(path).toArray(EMPTY_STRING_ARRAY);
	}

	/**
	 * Saves the changes made since the last flush or sync, once for the whole tree, re-applied on top of the file
	 * if it has been modified externally in the meantime, as {@link #sync()} does. Leaves the file untouched when
	 * there are no changes to save. {@link AbstractPreferences#flush()} would call {@link #flushSpi()} on this node
	 * and on each cached descendant, saving the file once per node.
	 * @throws BackingStoreException in case of an I/O error
	 */
	@Override
	public void flush() throws BackingStoreException {
		synchronized (lock) {
			flushSpi();
		}
	}

	/**
	 * Reloads the file if it has been modified externally since it was last read or written, re-applying the
	 * changes made since the last flush or sync on top of it, and saves those changes, once for the whole tree.
	 * {@link AbstractPreferences#sync()} would call {@link #syncSpi()} on this node and on each cached descendant.
	 * @throws BackingStoreException in case of an I/O error
	 * @throws IllegalStateException in case this node has been removed
	 */
	@Override
	public void sync() throws BackingStoreException {
		synchronized (lock) {
			if (isRemoved()) {
				throw new IllegalStateException(NODE_HAS_BEEN_REMOVED);
			}
			syncSpi();
		}
	}

	@Override
	protected void flushSpi() throws BackingStoreException {
		if (store == null) {
			return; // in-memory, nothing to flush
		}
		LOG.debug("Flushing preferences to disk");
		try {
			store.sync();
		}
		catch (IOException e) {
			LOG.error("Failed to flush preferences", e);
			throw new BackingStoreException(e);
		}
	}

	@Override
	protected void syncSpi() throws BackingStoreException {
		if (store == null) {
			return; // in-memory, nothing to sync
		}
		LOG.debug("Syncing preferences with disk");
		try {
			store.sync();
		}
		catch (IOException e) {
			LOG.error("Failed to sync preferences", e);
			throw new BackingStoreException(e);
		}
	}

	@Override
	protected String[] childrenNamesSpi() {
		return preferences.childrenNames(path).toArray(EMPTY_STRING_ARRAY);
	}

	@Override
	protected AbstractPreferences childSpi(String name) {
		LOG.trace("Creating child preference node '{}' under path '{}'", name, path);
		return new FilePreferences(this, name, preferences, store);
	}

	private FilePreferences child(String name) {
		if (name.length() <= MAX_NAME_LENGTH) {
			return (FilePreferences) super.node(name);
		}
		synchronized (lock) {
			if (isRemoved()) {
				throw new IllegalStateException(NODE_HAS_BEEN_REMOVED);
			}

			return longNamedChildren.computeIfAbsent(name, childName -> new FilePreferences(this, childName, preferences, store));
		}
	}

	private FilePreferences root() {
		FilePreferences node = this;
		while (node.parentNode != null) {
			node = node.parentNode;
		}

		return node;
	}

	/**
	 * AbstractPreferences removes the children it has cached along with a node, the long named ones it does not know of
	 */
	private void removeLongNamedDescendants() throws BackingStoreException {
		List<FilePreferences> longNamed;
		AbstractPreferences[] cached;
		synchronized (lock) {
			longNamed = new ArrayList<>(longNamedChildren.values());
			cached = cachedChildren();
		}
		for (FilePreferences child : longNamed) {
			child.removeNode();
		}
		for (AbstractPreferences child : cached) {
			((FilePreferences) child).removeLongNamedDescendants();
		}
	}

	private static boolean containsLongName(String path) {
		if (path.length() <= MAX_NAME_LENGTH) {
			return false;
		}
		int start = 0;
		while (start <= path.length()) {
			int end = path.indexOf(PATH_SEPARATOR, start);
			if (end < 0) {
				end = path.length();
			}
			if (end - start > MAX_NAME_LENGTH) {
				return true;
			}
			start = end + 1;
		}

		return false;
	}

	@Override
	protected void removeNodeSpi() {
		LOG.debug("Removing preference node at path '{}'", path);
		//persisted by the next flush(), as the Preferences contract states
		preferences.removeNode(path);
	}
}