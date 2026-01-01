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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.property;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Provides configuration values which sync with system properties when set. Note that setting the value via {@link System#setProperty(String, String)}
 * does not affect the property store value, so the value should only be modified via the property store value instance.
 * If no value is found in a configuration file or in a system property, the default property value is used as the inital value.
 * When the value is set to null via {@link is.codion.common.reactive.value.Value#set(Object)} the default value is used, if one has been specified.
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
 */
public final class PropertyStore {

	/**
	 * The separator used to separate multiple values.
	 */
	private static final String VALUE_SEPARATOR = ";";

	private final Map<String, PropertyValue<?>> propertyValues = new HashMap<>();

	private final Properties properties = new Properties() {
		@Override
		public Enumeration<Object> keys() {
			//sort to make properties file easier for human consumption
			List<Object> keys = Collections.list(super.keys());
			keys.sort(Comparator.comparing(Object::toString));

			return Collections.enumeration(keys);
		}
	};

	private PropertyStore(Path propertiesFile) throws IOException {
		this(loadProperties(propertiesFile));
	}

	private PropertyStore(InputStream inputStream) throws IOException {
		this(loadProperties(inputStream));
	}

	private PropertyStore(Properties properties) {
		this.properties.putAll(requireNonNull(properties));
		this.properties.stringPropertyNames().forEach(property ->
						System.setProperty(property, this.properties.getProperty(property)));
	}

	/**
	 * Creates a value for the given boolean property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<Boolean> booleanValue(String propertyName) {
		return value(propertyName, value -> value.equalsIgnoreCase(Boolean.TRUE.toString()), Objects::toString);
	}

	/**
	 * Creates a value for the given boolean property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<Boolean> booleanValue(String propertyName, boolean defaultValue) {
		return value(propertyName, value -> value.equalsIgnoreCase(Boolean.TRUE.toString()), Objects::toString, defaultValue);
	}

	/**
	 * Creates a value for the given double property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<Double> doubleValue(String propertyName) {
		return value(propertyName, Double::parseDouble, Objects::toString);
	}

	/**
	 * Creates a value for the given double property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<Double> doubleValue(String propertyName, double defaultValue) {
		return value(propertyName, Double::parseDouble, Objects::toString, defaultValue);
	}

	/**
	 * Creates a value for the given integer property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<Integer> integerValue(String propertyName) {
		return value(propertyName, Integer::parseInt, Objects::toString);
	}

	/**
	 * Creates a value for the given integer property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<Integer> integerValue(String propertyName, int defaultValue) {
		return value(propertyName, Integer::parseInt, Objects::toString, defaultValue);
	}

	/**
	 * Creates a value for the given long property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<Long> longValue(String propertyName) {
		return value(propertyName, Long::parseLong, Objects::toString);
	}

	/**
	 * Creates a value for the given long property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<Long> longValue(String propertyName, long defaultValue) {
		return value(propertyName, Long::parseLong, Objects::toString, defaultValue);
	}

	/**
	 * Creates a value for the given character property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<Character> characterValue(String propertyName) {
		return value(propertyName, string -> string.charAt(0), Object::toString);
	}

	/**
	 * Creates a value for the given character property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<Character> characterValue(String propertyName, char defaultValue) {
		return value(propertyName, string -> string.charAt(0), Object::toString, defaultValue);
	}

	/**
	 * Creates a value for the given string property
	 * @param propertyName the property name
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<String> stringValue(String propertyName) {
		return value(propertyName, Objects::toString, Objects::toString);
	}

	/**
	 * Creates a value for the given string property
	 * @param propertyName the property name
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws IllegalStateException in case a value has already been created for the given property
	 */
	public PropertyValue<String> stringValue(String propertyName, @Nullable String defaultValue) {
		return value(propertyName, Objects::toString, Objects::toString, defaultValue);
	}

	/**
	 * Creates a value for the given enum property
	 * @param <T> the enum type
	 * @param propertyName the property name
	 * @param enumClass the enum class
	 * @return a new {@link PropertyValue} instance
	 * @throws NullPointerException if {@code propertyName} or {@code enumClass} is null
	 */
	public <T extends Enum<T>> PropertyValue<T> enumValue(String propertyName, Class<T> enumClass) {
		requireNonNull(enumClass);

		return value(propertyName, value -> Enum.valueOf(enumClass, value.toUpperCase()), Objects::toString);
	}

	/**
	 * Creates a value for the given enum property
	 * @param <T> the enum type
	 * @param propertyName the property name
	 * @param enumClass the enum class
	 * @param defaultValue the default value
	 * @return a new {@link PropertyValue} instance
	 * @throws NullPointerException if {@code propertyName} or {@code enumClass} is null
	 */
	public <T extends Enum<T>> PropertyValue<T> enumValue(String propertyName, Class<T> enumClass, @Nullable T defaultValue) {
		requireNonNull(enumClass);

		return value(propertyName, value -> Enum.valueOf(enumClass, value.toUpperCase()), Objects::toString, defaultValue);
	}

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
	public <T> PropertyValue<List<T>> listValue(String propertyName, Function<String, T> decoder, Function<T, String> encoder) {
		return value(propertyName, new ListValueDecoder<>(decoder), new ListValueEncoder<>(encoder), emptyList());
	}

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
	public <T> PropertyValue<List<T>> listValue(String propertyName, Function<String, T> decoder, Function<T, String> encoder, List<T> defaultValue) {
		return value(propertyName, new ListValueDecoder<>(decoder), new ListValueEncoder<>(encoder), defaultValue);
	}

	/**
	 * Creates a value representing the given property name.
	 * @param <T> the value type
	 * @param propertyName the configuration property name identifying this value
	 * @param decoder a decoder for decoding the value from a string
	 * @param encoder an encoder for encoding the value to a string
	 * @return the configuration value
	 * @throws NullPointerException if {@code propertyName}, {@code decoder} or {@code encoder} is null
	 */
	public <T> PropertyValue<T> value(String propertyName, Function<String, T> decoder, Function<T, String> encoder) {
		return value(propertyName, decoder, encoder, null);
	}

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
	public <T> PropertyValue<T> value(String propertyName, Function<String, T> decoder, Function<T, String> encoder, @Nullable T defaultValue) {
		synchronized (propertyValues) {
			if (propertyValues.containsKey(requireNonNull(propertyName))) {
				throw new IllegalStateException("A value has already been created for the property '" + propertyName + "'");
			}
			PropertyValue<T> value = new PropertyValue<>(properties, propertyName, decoder, encoder, defaultValue);
			propertyValues.put(propertyName, value);

			return value;
		}
	}

	/**
	 * Returns the Value associated with the given property, an empty Optional if no such Value has been created.
	 * @param propertyName the property name
	 * @param <T> the value type
	 * @return the configuration value for the given name or an empty Optional if none exists
	 */
	public <T> Optional<PropertyValue<T>> propertyValue(String propertyName) {
		synchronized (propertyValues) {
			return Optional.ofNullable((PropertyValue<T>) propertyValues.get(requireNonNull(propertyName)));
		}
	}

	/**
	 * Sets the value of the given property
	 * @param propertyName the property name
	 * @param value the value
	 * @throws IllegalArgumentException if the property is value bound
	 */
	public void setProperty(String propertyName, @Nullable String value) {
		synchronized (propertyValues) {
			if (propertyValues.containsKey(requireNonNull(propertyName))) {
				throw new IllegalArgumentException("Value bound properties can only be modified through their Value instances");
			}
			properties.setProperty(propertyName, value);
		}
	}

	/**
	 * Retrieves the value for the given property, null if no value is present
	 * @param propertyName the property name
	 * @return the value or null if no value is present
	 */
	public @Nullable String getProperty(String propertyName) {
		return properties.getProperty(requireNonNull(propertyName));
	}

	/**
	 * Returns the values associated with property names fulfilling the given predicate
	 * @param predicate the predicate for the properties which values to return
	 * @return all values associated with the properties with the given prefix
	 */
	public Collection<String> properties(Predicate<String> predicate) {
		requireNonNull(predicate);
		return properties.stringPropertyNames().stream()
						.filter(predicate)
						.map(properties::getProperty)
						.collect(toList());
	}

	/**
	 * Returns all property names fulfilling the given predicate
	 * @param predicate the predicate used to filter the property names to return
	 * @return all property names with the given prefix
	 */
	public Collection<String> propertyNames(Predicate<String> predicate) {
		requireNonNull(predicate);
		return properties.stringPropertyNames().stream()
						.filter(predicate)
						.collect(toList());
	}

	/**
	 * Returns true if this PropertyStore contains a value for the given property
	 * @param propertyName the property
	 * @return true if a value for the given property exists
	 */
	public boolean containsProperty(String propertyName) {
		return properties.containsKey(requireNonNull(propertyName));
	}

	/**
	 * Removes all properties which names fulfill the given predicate.
	 * Note that properties which are value bound cannot be removed.
	 * @param predicate the predicate used to filter the properties to be removed
	 * @throws IllegalArgumentException in case any of the properties to remove are value bound
	 */
	public void removeAll(Predicate<String> predicate) {
		synchronized (propertyValues) {
			Collection<String> propertyKeys = propertyNames(predicate);
			if (propertyKeys.stream().anyMatch(propertyValues::containsKey)) {
				throw new IllegalArgumentException("Value bound properties can only be modified through their Value instances");
			}
			propertyKeys.forEach(properties::remove);
		}
	}

	/**
	 * Writes the stored properties to a file
	 * @param propertiesFile the properties file to write to
	 * @throws IOException in case writing the file was not successful
	 */
	public void writeToFile(Path propertiesFile) throws IOException {
		requireNonNull(propertiesFile);
		synchronized (propertyValues) {
			if (!Files.exists(propertiesFile) && !propertiesFile.toFile().createNewFile()) {
				throw new IOException("Unable to create properties file: " + propertiesFile);
			}
			try (OutputStream output = Files.newOutputStream(propertiesFile)) {
				properties.store(output, null);
			}
		}
	}

	/**
	 * Creates a new empy PropertyStore.
	 * @return a new empty PropertyStore instance
	 */
	public static PropertyStore propertyStore() {
		return propertyStore(new Properties());
	}

	/**
	 * Creates a new PropertyStore initialized with the properties found in the given file.
	 * @param inputStream the input stream to read from
	 * @return a new PropertyStore
	 * @throws IOException in case the given input stream could not be read
	 */
	public static PropertyStore propertyStore(InputStream inputStream) throws IOException {
		return new PropertyStore(inputStream);
	}

	/**
	 * Creates a new PropertyStore initialized with the properties found in the given file.
	 * @param propertiesFile the file to read from initially
	 * @return a new PropertyStore
	 * @throws IOException in case the given properties file exists but reading it failed
	 * @throws FileNotFoundException in case the file does not exist
	 */
	public static PropertyStore propertyStore(Path propertiesFile) throws IOException {
		return new PropertyStore(propertiesFile);
	}

	/**
	 * Creates a new PropertyStore initialized with the given properties.
	 * @param properties the initial properties
	 * @return a new PropertyStore
	 */
	public static PropertyStore propertyStore(Properties properties) {
		return new PropertyStore(properties);
	}

	/**
	 * Note that class and module paths are displayed as one item per line.
	 * @return a String containing all system properties, one per line
	 */
	public static String systemProperties() {
		return systemProperties(new DefaultSystemPropertyFormatter());
	}

	/**
	 * Returns a String containing all system properties, sorted by name, written by the given {@link PropertyFormatter}.
	 * @param formatter for specific property formatting or exclusions
	 * @return a String containing all system properties, one per line
	 */
	static String systemProperties(PropertyFormatter formatter) {
		requireNonNull(formatter);
		Properties properties = System.getProperties();

		return Collections.list(properties.propertyNames()).stream()
						.filter(String.class::isInstance)
						.map(String.class::cast)
						.sorted()
						.map(property -> property + ": " + formatter.formatValue(property, properties.getProperty(property)))
						.collect(joining("\n"));
	}

	/**
	 * Formats a property value, can f.ex. be used to hide passwords and other sensitive data.
	 */
	public interface PropertyFormatter {

		/**
		 * Formats the given value.
		 * @param property the property
		 * @param value the value
		 * @return the value
		 */
		String formatValue(String property, String value);
	}

	/**
	 * Reads all properties from the given properties file.
	 * @param propertiesFile the properties file to read from
	 * @return the properties read from the given file
	 * @throws IOException in case the file exists but can not be read
	 * @throws FileNotFoundException in case the file does not exist
	 */
	private static Properties loadProperties(Path propertiesFile) throws IOException {
		if (!Files.exists(requireNonNull(propertiesFile))) {
			throw new FileNotFoundException(propertiesFile.toString());
		}
		try (InputStream input = Files.newInputStream(propertiesFile)) {
			return loadProperties(input);
		}
	}

	/**
	 * Reads all properties from the given input stream.
	 * @param inputStream the input stream to read from
	 * @return the properties read from the given input stream
	 * @throws IOException in case the file exists but can not be read
	 */
	private static Properties loadProperties(InputStream inputStream) throws IOException {
		requireNonNull(inputStream);
		Properties propertiesFromFile = new Properties();
		propertiesFromFile.load(inputStream);

		return propertiesFromFile;
	}

	private static final class ListValueDecoder<T> implements Function<String, List<T>> {

		private final Function<String, T> decoder;

		private ListValueDecoder(Function<String, T> decoder) {
			this.decoder = requireNonNull(decoder);
		}

		@Override
		public List<T> apply(@Nullable String stringValue) {
			return stringValue == null ? emptyList() : Arrays.stream(stringValue.split(VALUE_SEPARATOR))
							.map(decoder)
							.collect(toList());
		}
	}

	private static final class ListValueEncoder<T> implements Function<List<T>, String> {

		private final Function<T, String> encoder;

		private ListValueEncoder(Function<T, String> encoder) {
			this.encoder = requireNonNull(encoder);
		}

		@Override
		public String apply(List<T> valueList) {
			return valueList.stream()
							.map(encoder)
							.collect(joining(VALUE_SEPARATOR));
		}
	}

	static final class DefaultSystemPropertyFormatter implements PropertyFormatter {

		@Override
		public String formatValue(String property, String value) {
			if (classOrModulePath(property) && !value.isEmpty()) {
				return Stream.of(value.split(File.pathSeparator))
								.collect(joining("\n", "\n", ""));
			}

			return value;
		}

		private static boolean classOrModulePath(String property) {
			return property.endsWith("class.path") || property.endsWith("module.path");
		}
	}
}
