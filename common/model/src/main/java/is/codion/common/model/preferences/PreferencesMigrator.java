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
package is.codion.common.model.preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.PreferencesPath.userPreferencesPath;
import static java.util.Objects.requireNonNull;

/**
 * Migrates preferences from the default Java Preferences implementation
 * to the file-based implementation.
 */
final class PreferencesMigrator {

	private static final Logger LOG = LoggerFactory.getLogger(PreferencesMigrator.class);

	private static final int MAX_KEY_LENGTH = 80;
	private static final int MAX_VALUE_LENGTH = 8192;

	private final Path targetPath;
	private final boolean migrateTruncated;

	private PreferencesMigrator(Builder builder) {
		this.targetPath = builder.targetPath;
		this.migrateTruncated = builder.migrateTruncated;
	}

	/**
	 * Creates a new migrator with default settings.
	 * @return a new migrator instance
	 */
	static PreferencesMigrator create() {
		return builder().build();
	}

	/**
	 * @return a new builder instance
	 */
	static Builder builder() {
		return new Builder();
	}

	/**
	 * Performs the migration from default preferences to file-based preferences.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Checks if migration has already been performed
	 * <li>Recursively copies all preferences from the default implementation
	 * <li>Detects and optionally migrates truncated values
	 * <li>Creates a migration marker to prevent re-migration
	 * </ul>
	 * @throws IOException if an I/O error occurs
	 * @throws BackingStoreException if the preferences cannot be accessed
	 */
	void migrate() throws IOException, BackingStoreException {
		if (Files.exists(targetPath)) {
			LOG.info("Migration already performed, preferences file exists at {}", targetPath);
			return;
		}

		LOG.info("Starting preferences migration to {}", targetPath);
		long startTime = System.currentTimeMillis();

		// Create the file preferences store
		JsonPreferencesStore store = new JsonPreferencesStore(targetPath);

		// Get the default preferences root
		// The FilePreferencesFactory uses a ThreadLocal flag to prevent recursion
		Preferences defaultRoot = Preferences.userRoot();

		// Migrate recursively
		int nodeCount = migrateNode(defaultRoot, "", store);

		// Save to file
		store.save();

		// Create marker to indicate migration has been performed
		store.put("", ".migrated", String.valueOf(System.currentTimeMillis()));
		store.save();

		long duration = System.currentTimeMillis() - startTime;
		LOG.info("Migration completed successfully: {} nodes migrated in {} ms", nodeCount, duration);
	}

	/**
	 * Checks if a value appears to be truncated by the default preferences implementation.
	 * @param key the preference key
	 * @param value the preference value
	 * @return true if the value appears to be truncated
	 */
	static boolean isTruncated(String key, String value) {
		return (key != null && key.length() == MAX_KEY_LENGTH) ||
						(value != null && value.length() == MAX_VALUE_LENGTH);
	}

	private int migrateNode(Preferences node, String path, JsonPreferencesStore store) throws BackingStoreException {
		int nodeCount = 1;

		// Migrate all key-value pairs in this node
		String[] keys = node.keys();
		LOG.debug("Migrating {} keys from node at path '{}'", keys.length, path);

		for (String key : keys) {
			String value = node.get(key, null);
			if (value != null) {
				if (migrateTruncated || !isTruncated(key, value)) {
					store.put(path, key, value);

					if (isTruncated(key, value)) {
						// Add a marker indicating this value was potentially truncated
						LOG.warn("Detected potentially truncated value for key '{}' at path '{}', length: {}",
										key, path, value.length());
						store.put(path, key + ".truncated", "true");
					}
				}
				else {
					LOG.debug("Skipping truncated value for key '{}' at path '{}'", key, path);
				}
			}
		}

		// Recursively migrate child nodes
		String[] childrenNames = node.childrenNames();
		for (String childName : childrenNames) {
			Preferences child = node.node(childName);
			String childPath = path.isEmpty() ? childName : path + "/" + childName;
			nodeCount += migrateNode(child, childPath, store);
		}

		return nodeCount;
	}

	/**
	 * Builder for {@link PreferencesMigrator}.
	 */
	static final class Builder {

		private Path targetPath = userPreferencesPath();
		private boolean migrateTruncated = true;

		private Builder() {}

		/**
		 * @param targetPath the target file path for the migrated preferences
		 * @return this builder instance
		 */
		Builder targetPath(Path targetPath) {
			this.targetPath = requireNonNull(targetPath);
			return this;
		}

		/**
		 * @param migrateTruncated whether to migrate values that appear to be truncated
		 * @return this builder instance
		 */
		Builder migrateTruncated(boolean migrateTruncated) {
			this.migrateTruncated = migrateTruncated;
			return this;
		}

		/**
		 * @return a new {@link PreferencesMigrator} instance
		 */
		PreferencesMigrator build() {
			return new PreferencesMigrator(this);
		}
	}
}