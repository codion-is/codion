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
 * Copyright (c) 2017 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common;

import is.codion.common.property.PropertyStore;
import is.codion.common.property.PropertyValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Collections.emptyList;

/**
 * A utility class for central configuration values.
 * <p>
 * Parses a property file on class load, specified by the {@link #CONFIGURATION_FILE} system property.
 * <p>
 * Note that if {@link #CONFIGURATION_FILE_REQUIRED} is true and the file referenced by {@link #CONFIGURATION_FILE}
 * is not found a {@link ConfigurationFileNotFoundException} is thrown when this class is loaded.
 * @see #CONFIGURATION_FILE_REQUIRED
 */
public final class Configuration {

	/**
	 * Specifies the main configuration file.<br>
	 * Prefix with 'classpath:' to indicate that the configuration file is on the classpath.
	 * <p>
	 * Note: When using classpath prefix, the configuration file must be located in the classpath root
	 * (not in subdirectories) to ensure accessibility across module boundaries.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 */
	public static final String CONFIGURATION_FILE = "codion.configurationFile";

	/**
	 * Specifies whether the application requires a configuration file to run.<br>
	 * If this is set to true and the file referenced by {@link #CONFIGURATION_FILE}<br>
	 * is not found a {@link ConfigurationFileNotFoundException} is thrown when this class is loaded.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	public static final String CONFIGURATION_FILE_REQUIRED = "codion.configurationFileRequired";

	private static final String CLASSPATH_PREFIX = "classpath:";

	private static final PropertyStore STORE;

	static {
		STORE = loadConfiguration();
	}

	private Configuration() {}

	/**
	 * Creates a boolean configuration value
	 * @param key the configuration value key
	 * @return a configuration value builder
	 */
	public static PropertyValue<Boolean> booleanValue(String key) {
		return STORE.booleanValue(key);
	}

	/**
	 * Creates a boolean configuration value
	 * @param key the configuration value key
	 * @param defaultValue the default value
	 * @return a configuration value builder
	 */
	public static PropertyValue<Boolean> booleanValue(String key, boolean defaultValue) {
		return STORE.booleanValue(key, defaultValue);
	}

	/**
	 * Creates an integer configuration value
	 * @param key the configuration value key
	 * @return a configuration value builder
	 */
	public static PropertyValue<Integer> integerValue(String key) {
		return STORE.integerValue(key);
	}

	/**
	 * Creates an integer configuration value
	 * @param key the configuration value key
	 * @param defaultValue the default value
	 * @return a configuration value builder
	 */
	public static PropertyValue<Integer> integerValue(String key, int defaultValue) {
		return STORE.integerValue(key, defaultValue);
	}

	/**
	 * Creates a long configuration value
	 * @param key the configuration value key
	 * @return a configuration value builder
	 */
	public static PropertyValue<Long> longValue(String key) {
		return STORE.longValue(key);
	}

	/**
	 * Creates a long configuration value
	 * @param key the configuration value key
	 * @param defaultValue the default value
	 * @return a configuration value builder
	 */
	public static PropertyValue<Long> longValue(String key, long defaultValue) {
		return STORE.longValue(key, defaultValue);
	}

	/**
	 * Creates a double configuration value
	 * @param key the configuration value key
	 * @return a configuration value builder
	 */
	public static PropertyValue<Double> doubleValue(String key) {
		return STORE.doubleValue(key);
	}

	/**
	 * Creates a double configuration value
	 * @param key the configuration value key
	 * @param defaultValue the default value
	 * @return a configuration value builder
	 */
	public static PropertyValue<Double> doubleValue(String key, double defaultValue) {
		return STORE.doubleValue(key, defaultValue);
	}

	/**
	 * Creates a character configuration value
	 * @param key the configuration value key
	 * @return a configuration value builder
	 */
	public static PropertyValue<Character> characterValue(String key) {
		return STORE.characterValue(key);
	}

	/**
	 * Creates a character configuration value
	 * @param key the configuration value key
	 * @param defaultValue the default value
	 * @return a configuration value builder
	 */
	public static PropertyValue<Character> characterValue(String key, char defaultValue) {
		return STORE.characterValue(key, defaultValue);
	}

	/**
	 * Creates a string configuration value
	 * @param key the configuration value key
	 * @return a configuration value builder
	 */
	public static PropertyValue<String> stringValue(String key) {
		return STORE.stringValue(key);
	}

	/**
	 * Creates a string configuration value
	 * @param key the configuration value key
	 * @param defaultValue the default value
	 * @return a configuration value builder
	 */
	public static PropertyValue<String> stringValue(String key, String defaultValue) {
		return STORE.stringValue(key, defaultValue);
	}

	/**
	 * Creates an enum configuration value
	 * @param key the configuration value key
	 * @param enumClass the enum class
	 * @param <T> the enum type
	 * @return a configuration value builder
	 */
	public static <T extends Enum<T>> PropertyValue<T> enumValue(String key, Class<T> enumClass) {
		return STORE.enumValue(key, enumClass);
	}

	/**
	 * Creates an enum configuration value
	 * @param key the configuration value key
	 * @param enumClass the enum class
	 * @param defaultValue the default value
	 * @param <T> the enum type
	 * @return a configuration value builder
	 */
	public static <T extends Enum<T>> PropertyValue<T> enumValue(String key, Class<T> enumClass, T defaultValue) {
		return STORE.enumValue(key, enumClass, defaultValue);
	}

	/**
	 * Creates a list configuration value
	 * @param key the configuration value key
	 * @param parser the parser used to parse a string representation of the value
	 * @param <T> the value type
	 * @return a configuration value builder
	 */
	public static <T> PropertyValue<List<T>> listValue(String key, Function<String, T> parser) {
		return listValue(key, parser, emptyList());
	}

	/**
	 * Creates a list configuration value
	 * @param key the configuration value key
	 * @param parser the parser used to parse a string representation of the value
	 * @param defaultValue the default value
	 * @param <T> the value type
	 * @return a configuration value builder
	 */
	public static <T> PropertyValue<List<T>> listValue(String key, Function<String, T> parser, List<T> defaultValue) {
		return STORE.listValue(key, parser, Objects::toString, defaultValue);
	}

	/**
	 * Creates a configuration value
	 * @param key the configuration value key
	 * @param parser the parser used to parse a string representation of the value
	 * @param <T> the value type
	 * @return a configuration value builder
	 */
	public static <T> PropertyValue<T> value(String key, Function<String, T> parser) {
		return STORE.value(key, parser, Objects::toString);
	}

	/**
	 * Creates a configuration value
	 * @param key the configuration value key
	 * @param parser the parser used to parse a string representation of the value
	 * @param defaultValue the default value
	 * @param <T> the value type
	 * @return a configuration value builder
	 */
	public static <T> PropertyValue<T> value(String key, Function<String, T> parser, T defaultValue) {
		return STORE.value(key, parser, Objects::toString, defaultValue);
	}

	private static PropertyStore loadConfiguration() {
		boolean configurationFileRequired = System.getProperty(CONFIGURATION_FILE_REQUIRED, "false").equalsIgnoreCase(Boolean.TRUE.toString());
		String configurationFilePath = System.getProperty(CONFIGURATION_FILE,
						System.getProperty("user.home") + FileSystems.getDefault().getSeparator() + "codion.config");

		// Validate configuration file path for security
		validateConfigurationPath(configurationFilePath);

		if (configurationFilePath.toLowerCase().startsWith(CLASSPATH_PREFIX)) {
			return loadFromClasspath(configurationFilePath, configurationFileRequired);
		}

		return loadFromFile(configurationFilePath, configurationFileRequired);
	}

	/**
	 * Loads configuration from classpath. The file must be in the classpath root
	 * to ensure module system compatibility.
	 * @param filePath the classpath file path (with classpath: prefix)
	 * @param configurationRequired whether the configuration file is required
	 * @return the property store
	 * @throws ConfigurationFileNotFoundException if required file not found
	 * @throws IllegalArgumentException if file path contains subdirectories
	 */
	static PropertyStore loadFromClasspath(String filePath, boolean configurationRequired) {
		String filepath = classpathFilepath(filePath);
		try (InputStream configurationFileStream = Configuration.class.getClassLoader().getResourceAsStream(filepath)) {
			if (configurationFileStream == null) {
				if (configurationRequired) {
					throw new ConfigurationFileNotFoundException(filePath);
				}

				return PropertyStore.propertyStore();
			}

			return PropertyStore.propertyStore(configurationFileStream);
		}
		catch (IOException e) {
			throw new RuntimeException("Unable to load configuration from classpath: " + filePath, e);
		}
	}

	static PropertyStore loadFromFile(String filePath, boolean configurationRequired) {
		try {
			Path file = Path.of(filePath);
			if (!Files.exists(file)) {
				if (configurationRequired) {
					throw new ConfigurationFileNotFoundException(filePath);
				}

				return PropertyStore.propertyStore();
			}

			return PropertyStore.propertyStore(file);
		}
		catch (IOException e) {
			throw new RuntimeException("Unable to load configuration from file: " + filePath, e);
		}
	}

	/**
	 * Validates configuration file paths to prevent security issues.
	 * @param filePath the configuration file path to validate
	 * @throws SecurityException if the path is potentially dangerous
	 */
	private static void validateConfigurationPath(String filePath) {
		if (filePath == null || filePath.trim().isEmpty()) {
			throw new SecurityException("Configuration file path cannot be null or empty");
		}

		// Normalize path for consistent validation
		String normalizedPath = filePath.trim().toLowerCase();
		// Check for path traversal attempts
		if (normalizedPath.contains("../") || normalizedPath.contains("..\\") ||
						normalizedPath.contains("%2e%2e") || normalizedPath.contains("..%2f") ||
						normalizedPath.contains("..%5c")) {
			throw new SecurityException("Configuration file path contains path traversal sequences: " + filePath);
		}

		// For classpath files, additional validation is handled in classpathFilepath()
		if (!normalizedPath.startsWith(CLASSPATH_PREFIX.toLowerCase()) &&
						(normalizedPath.startsWith("/etc/") || normalizedPath.startsWith("c:\\windows\\") ||
										normalizedPath.contains("/proc/") || normalizedPath.contains("/sys/"))) {
			throw new SecurityException("Configuration file path accesses restricted system directories: " + filePath);
		}

		// Check path length to prevent potential buffer overflow-style attacks
		if (filePath.length() > 4096) {
			throw new SecurityException("Configuration file path exceeds maximum allowed length: " + filePath.length());
		}
	}

	private static String classpathFilepath(String filePath) {
		String path = filePath.substring(CLASSPATH_PREFIX.length());
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (path.contains("/")) {
			throw new IllegalArgumentException(
							"Configuration files must be in the classpath root to ensure " +
											"accessibility across module boundaries. Use a filename without " +
											"path separators (e.g. 'app.config' instead of 'config/app.config').");
		}

		return path;
	}

	/**
	 * Indicates that a required configuration file was not found.
	 */
	public static final class ConfigurationFileNotFoundException extends RuntimeException {

		private ConfigurationFileNotFoundException(String filePath) {
			super(filePath.toLowerCase().startsWith(CLASSPATH_PREFIX)
							? "Required configuration file not found on classpath: " + filePath
							: "Required configuration file not found: " + filePath);
		}
	}
}
