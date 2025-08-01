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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.property;

import is.codion.common.property.DefaultPropertyStore.DefaultSystemPropertyFormatter;

import org.jspecify.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Provides configuration values which sync with system properties when set. Note that setting the value via {@link System#setProperty(String, String)}
 * does not affect the property store value, so the value should only be modified via the property store value instance.
 * If no value is found in a configuration file or in a system property, the default property value is used as the inital value.
 * When the value is set to null via {@link is.codion.common.value.Value#set(Object)} the default value is used, if one has been specified.
 * {@snippet :
 * Path configurationFile = Path.of(System.getProperty("user.home") + "/app.properties");
 *
 * PropertyStore store = PropertyStore.propertyStore(configurationFile);
 *
 * Value<Boolean> featureEnabled = store.booleanValue("feature.enabled", false);
 * Value<String> defaultUsername = store.stringValue("default.username", System.getProperty("user.name"));
 *
 * featureEnabled.set(true);
 * defaultUsername.set("scott");
 *
 * store.writeToFile(configurationFile);
 *
 * //reverts to the default value
 * featureEnabled.set(null);
 * defaultUsername.set(null);
 *
 * String isFeatureEnabled = System.getProperty("feature.enabled"); // "false"
 *}
 * <p>All implementations are thread-safe and support concurrent access.</p>
 */
public interface PropertyStore {

	/**
	 * Creates a value for the given boolean property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<Boolean> booleanValue(String propertyName);

	/**
	 * Creates a value for the given boolean property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<Boolean> booleanValue(String propertyName, boolean defaultValue);

	/**
	 * Creates a value for the given double property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<Double> doubleValue(String propertyName);

	/**
	 * Creates a value for the given double property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<Double> doubleValue(String propertyName, double defaultValue);

	/**
	 * Creates a value for the given integer property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<Integer> integerValue(String propertyName);

	/**
	 * Creates a value for the given integer property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<Integer> integerValue(String propertyName, int defaultValue);

	/**
	 * Creates a value for the given long property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<Long> longValue(String propertyName);

	/**
	 * Creates a value for the given long property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<Long> longValue(String propertyName, long defaultValue);

	/**
	 * Creates a value for the given character property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<Character> characterValue(String propertyName);

	/**
	 * Creates a value for the given character property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<Character> characterValue(String propertyName, char defaultValue);

	/**
	 * Creates a value for the given string property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<String> stringValue(String propertyName);

	/**
	 * Creates a value for the given string property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	PropertyValue<String> stringValue(String propertyName, @Nullable String defaultValue);

	/**
	 * Creates a value for the given enum property
	 * @param <T> the enum type
	 * @param propertyName the property name
	 * @param enumClass the enum class
	 * @return a new {@link PropertyValue} instance
	 * @throws NullPointerException if {@code propertyName} or {@code enumClass} is null
	 */
	<T extends Enum<T>> PropertyValue<T> enumValue(String propertyName, Class<T> enumClass);

	/**
	 * Creates a value for the given enum property
	 * @param <T> the enum type
	 * @param propertyName the property name
	 * @param enumClass the enum class
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws NullPointerException if {@code propertyName} or {@code enumClass} is null
	 */
	<T extends Enum<T>> PropertyValue<T> enumValue(String propertyName, Class<T> enumClass, @Nullable T defaultValue);

	/**
	 * Creates a value for the given list property.
	 * Note that a list property automatically gets an {@link Collections#emptyList()} as its default value.
	 * @param <T> the value type
	 * @param propertyName the property name
	 * @param decoder a decoder for decoding the value from a string
	 * @param encoder an encoder for encoding the value to a string
	 * @return a new {@link PropertyValue} instance
	 * @throws NullPointerException if {@code propertyName}, {@code decoder} or {@code encoder} is null
	 */
	<T> PropertyValue<List<T>> listValue(String propertyName, Function<String, T> decoder, Function<T, String> encoder);

	/**
	 * Creates a value for the given list property.
	 * Note that a list property automatically gets an {@link Collections#emptyList()} as its default value.
	 * @param <T> the value type
	 * @param propertyName the property name
	 * @param decoder a decoder for decoding the value from a string
	 * @param encoder an encoder for encoding the value to a string
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws NullPointerException if {@code propertyName}, {@code decoder} or {@code encoder} is null
	 */
	<T> PropertyValue<List<T>> listValue(String propertyName, Function<String, T> decoder, Function<T, String> encoder, List<T> defaultValue);

	/**
	 * Creates a value representing the given property name.
	 * @param <T> the value type
	 * @param propertyName the configuration property name identifying this value
	 * @param decoder a decoder for decoding the value from a string
	 * @param encoder an encoder for encoding the value to a string
	 * @return the configuration value
	 * @throws NullPointerException if {@code propertyName}, {@code decoder} or {@code encoder} is null
	 */
	<T> PropertyValue<T> value(String propertyName, Function<String, T> decoder, Function<T, String> encoder);

	/**
	 * Creates a value representing the given property name.
	 * @param <T> the value type
	 * @param propertyName the configuration property name identifying this value
	 * @param decoder a decoder for decoding the value from a string
	 * @param encoder an encoder for encoding the value to a string
	 * @param defaultValue the default value
	 * @return the configuration value
	 * @throws NullPointerException if {@code propertyName}, {@code decoder} or {@code encoder} is null
	 */
	<T> PropertyValue<T> value(String propertyName, Function<String, T> decoder, Function<T, String> encoder, @Nullable T defaultValue);

	/**
	 * Returns the Value associated with the given property, an empty Optional if no such Value has been created.
	 * @param propertyName the property name
	 * @param <T> the value type
	 * @return the configuration value for the given name or an empty Optional if none exists
	 */
	<T> Optional<PropertyValue<T>> propertyValue(String propertyName);

	/**
	 * Sets the value of the given property
	 * @param propertyName the property name
	 * @param value the value
	 * @throws IllegalArgumentException if the property is value bound
	 */
	void setProperty(String propertyName, @Nullable String value);

	/**
	 * Retrieves the value for the given property, null if no value is present
	 * @param propertyName the property name
	 * @return the value or null if no value is present
	 */
	@Nullable String getProperty(String propertyName);

	/**
	 * Returns the values associated with property names fulfilling the given predicate
	 * @param predicate the predicate for the properties which values to return
	 * @return all values associated with the properties with the given prefix
	 */
	Collection<String> properties(Predicate<String> predicate);

	/**
	 * Returns all property names fulfilling the given predicate
	 * @param predicate the predicate used to filter the property names to return
	 * @return all property names with the given prefix
	 */
	Collection<String> propertyNames(Predicate<String> predicate);

	/**
	 * Returns true if this PropertyStore contains a value for the given property
	 * @param propertyName the property
	 * @return true if a value for the given property exists
	 */
	boolean containsProperty(String propertyName);

	/**
	 * Removes all properties which names fulfill the given predicate.
	 * Note that properties which are value bound cannot be removed.
	 * @param predicate the predicate used to filter the properties to be removed
	 * @throws IllegalArgumentException in case any of the properties to remove are value bound
	 */
	void removeAll(Predicate<String> predicate);

	/**
	 * Writes the stored properties to a file
	 * @param propertiesFile the properties file to write to
	 * @throws IOException in case writing the file was not successful
	 */
	void writeToFile(Path propertiesFile) throws IOException;

	/**
	 * Creates a new empy PropertyStore.
	 * @return a new empty PropertyStore instance
	 */
	static PropertyStore propertyStore() {
		return propertyStore(new Properties());
	}

	/**
	 * Creates a new PropertyStore initialized with the properties found in the given file.
	 * @param inputStream the input stream to read from
	 * @return a new PropertyStore
	 * @throws IOException in case the given input stream could not be read
	 */
	static PropertyStore propertyStore(InputStream inputStream) throws IOException {
		return new DefaultPropertyStore(inputStream);
	}

	/**
	 * Creates a new PropertyStore initialized with the properties found in the given file.
	 * @param propertiesFile the file to read from initially
	 * @return a new PropertyStore
	 * @throws IOException in case the given properties file exists but reading it failed
	 * @throws FileNotFoundException in case the file does not exist
	 */
	static PropertyStore propertyStore(Path propertiesFile) throws IOException {
		return new DefaultPropertyStore(propertiesFile);
	}

	/**
	 * Creates a new PropertyStore initialized with the given properties.
	 * @param properties the initial properties
	 * @return a new PropertyStore
	 */
	static PropertyStore propertyStore(Properties properties) {
		return new DefaultPropertyStore(properties);
	}

	/**
	 * Note that class and module paths are displayed as one item per line.
	 * @return a String containing all system properties, one per line
	 */
	static String systemProperties() {
		return systemProperties(new DefaultSystemPropertyFormatter());
	}

	/**
	 * Returns a String containing all system properties, sorted by name, written by the given {@link PropertyFormatter}.
	 * @param propertyFormatter for specific property formatting or exclusions
	 * @return a String containing all system properties, one per line
	 */
	static String systemProperties(PropertyFormatter propertyFormatter) {
		requireNonNull(propertyFormatter);
		Properties properties = System.getProperties();

		return Collections.list(properties.propertyNames()).stream()
						.filter(String.class::isInstance)
						.map(String.class::cast)
						.sorted()
						.map(property -> property + ": " + propertyFormatter.formatValue(property, properties.getProperty(property)))
						.collect(joining("\n"));
	}

	/**
	 * Formats a property value, can f.ex. be used to hide passwords and other sensitive data.
	 */
	interface PropertyFormatter {

		/**
		 * Formats the given value.
		 * @param property the property
		 * @param value the value
		 * @return the value
		 */
		String formatValue(String property, String value);
	}
}
