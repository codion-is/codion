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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

import static is.codion.common.model.preferences.PreferencesPath.userPreferencesPath;

/**
 * A file-based preferences factory that removes the length restrictions
 * of the default Java Preferences implementation.
 * <p>
 * The default Java Preferences API imposes the following restrictions:
 * <ul>
 *   <li>Maximum key length: 80 characters</li>
 *   <li>Maximum value length: 8,192 characters (8 KB)</li>
 *   <li>Maximum node name length: 80 characters</li>
 * </ul>
 * <p>
 * This implementation removes these restrictions by storing preferences in a JSON file,
 * allowing unlimited key and value lengths. This is particularly useful for storing
 * configuration data such as serialized table column preferences or other structured data
 * that may exceed the default limits.
 * <p>
 * <strong>File Location:</strong>
 * <ul>
 *   <li>Windows: {@code %LOCALAPPDATA%\Codion\preferences.json}</li>
 *   <li>macOS: {@code ~/Library/Preferences/Codion/preferences.json}</li>
 *   <li>Linux: {@code ~/.config/codion/preferences.json}</li>
 *   <li>Other: {@code ~/.codion/preferences.json}</li>
 * </ul>
 * <p>
 * <strong>File Format:</strong>
 * <p>
 * Preferences are stored as a flat JSON object:
 * <pre>
 * {
 *   "normal.key": "normal value",
 *   "very.long.key.that.exceeds.eighty.characters": "value",
 *   "key.with.large.value": "... 100KB of text ...",
 *   "key.with.newlines": "Line 1\nLine 2\nLine 3"
 * }
 * </pre>
 * <p>
 * <strong>Usage:</strong>
 * <p>
 * To use this factory, set the system property before accessing preferences:
 * <pre>
 * // At application startup
 * System.setProperty("java.util.prefs.PreferencesFactory",
 *     "is.codion.common.model.preferences.FilePreferencesFactory");
 *
 * // Then use preferences normally
 * Preferences prefs = Preferences.userRoot();
 * prefs.put("my.very.long.key.name.that.exceeds.80.chars", "my huge value...");
 * prefs.flush(); // Writes to ~/.codion/preferences.json
 * </pre>
 * <p>
 * <strong>Limitations:</strong>
 * <ul>
 *   <li>Currently only supports user preferences (not system preferences)</li>
 * </ul>
 * <p>
 * <strong>Thread Safety and Concurrency:</strong>
 * <p>
 * This implementation is thread-safe both within a single JVM and across multiple JVMs:
 * <ul>
 *   <li>Internal synchronization ensures thread safety within a JVM</li>
 *   <li>File locking prevents concurrent writes from multiple JVMs</li>
 *   <li>Atomic writes prevent file corruption</li>
 *   <li>External changes are detected and reloaded via {@code sync()}</li>
 * </ul>
 * <p>
 * <strong>Migration:</strong>
 * <p>
 * On first use, if the preferences file doesn't exist, this factory will automatically
 * attempt to migrate existing preferences from the default Java implementation.
 * To disable automatic migration:
 * <pre>
 * System.setProperty("codion.preferences.migrate", "false");
 * </pre>
 * @see Preferences
 * @see PreferencesFactory
 */
public final class FilePreferencesFactory implements PreferencesFactory {

	private static final Logger LOG = LoggerFactory.getLogger(FilePreferencesFactory.class);

	private static final String MIGRATE_PROPERTY = "codion.preferences.migrate";
	private static final ThreadLocal<Boolean> MIGRATING = ThreadLocal.withInitial(() -> false);

	private static volatile Preferences userRootInstance;

	@Override
	public synchronized Preferences userRoot() {
		if (userRootInstance == null) {
			LOG.debug("Initializing file-based preferences");
			try {
				if (shouldMigrate() && !MIGRATING.get()) {
					LOG.info("Performing automatic migration from default preferences");
					MIGRATING.set(true);
					try {
						performMigration();
					}
					finally {
						MIGRATING.set(false);
					}
				}
				userRootInstance = new SimpleFilePreferences();
				LOG.info("File-based preferences initialized successfully at {}", userPreferencesPath());
			}
			catch (IOException | BackingStoreException e) {
				LOG.error("Failed to initialize file preferences", e);
				throw new RuntimeException("Failed to initialize file preferences", e);
			}
		}

		return userRootInstance;
	}

	private static boolean shouldMigrate() {
		String migrate = System.getProperty(MIGRATE_PROPERTY, "true");
		boolean shouldMigrate = "true".equalsIgnoreCase(migrate) && !Files.exists(userPreferencesPath());

		if (!shouldMigrate && "true".equalsIgnoreCase(migrate)) {
			LOG.debug("Migration not needed, preferences file already exists");
		}
		else if (!"true".equalsIgnoreCase(migrate)) {
			LOG.debug("Migration disabled via system property");
		}

		return shouldMigrate;
	}

	private static void performMigration() throws IOException, BackingStoreException {
		PreferencesMigrator migrator = PreferencesMigrator.builder()
						.targetPath(userPreferencesPath())
						.build();

		migrator.migrate();
	}

	@Override
	public Preferences systemRoot() {
		LOG.warn("System preferences requested but not supported by file-based implementation");
		throw new UnsupportedOperationException("System preferences not supported");
	}
}