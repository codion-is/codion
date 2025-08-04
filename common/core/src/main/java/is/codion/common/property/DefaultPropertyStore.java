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

import is.codion.common.value.AbstractValue;

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
 * Thread-safe implementation of PropertyStore.
 * All operations are synchronized on the internal propertyValues map.
 */
final class DefaultPropertyStore implements PropertyStore {

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

	DefaultPropertyStore(Path propertiesFile) throws IOException {
		this(loadProperties(propertiesFile));
	}

	DefaultPropertyStore(InputStream inputStream) throws IOException {
		this(loadProperties(inputStream));
	}

	DefaultPropertyStore(Properties properties) {
		this.properties.putAll(requireNonNull(properties));
		this.properties.stringPropertyNames().forEach(property ->
						System.setProperty(property, this.properties.getProperty(property)));
	}

	@Override
	public PropertyValue<Boolean> booleanValue(String propertyName) {
		return value(propertyName, value -> value.equalsIgnoreCase(Boolean.TRUE.toString()), Objects::toString);
	}

	@Override
	public PropertyValue<Boolean> booleanValue(String propertyName, boolean defaultValue) {
		return value(propertyName, value -> value.equalsIgnoreCase(Boolean.TRUE.toString()), Objects::toString, defaultValue);
	}

	@Override
	public PropertyValue<Double> doubleValue(String propertyName) {
		return value(propertyName, Double::parseDouble, Objects::toString);
	}

	@Override
	public PropertyValue<Double> doubleValue(String propertyName, double defaultValue) {
		return value(propertyName, Double::parseDouble, Objects::toString, defaultValue);
	}

	@Override
	public PropertyValue<Integer> integerValue(String propertyName) {
		return value(propertyName, Integer::parseInt, Objects::toString);
	}

	@Override
	public PropertyValue<Integer> integerValue(String propertyName, int defaultValue) {
		return value(propertyName, Integer::parseInt, Objects::toString, defaultValue);
	}

	@Override
	public PropertyValue<Long> longValue(String propertyName) {
		return value(propertyName, Long::parseLong, Objects::toString);
	}

	@Override
	public PropertyValue<Long> longValue(String propertyName, long defaultValue) {
		return value(propertyName, Long::parseLong, Objects::toString, defaultValue);
	}

	@Override
	public PropertyValue<Character> characterValue(String propertyName) {
		return value(propertyName, string -> string.charAt(0), Object::toString);
	}

	@Override
	public PropertyValue<Character> characterValue(String propertyName, char defaultValue) {
		return value(propertyName, string -> string.charAt(0), Object::toString, defaultValue);
	}

	@Override
	public PropertyValue<String> stringValue(String propertyName) {
		return value(propertyName, Objects::toString, Objects::toString);
	}

	@Override
	public PropertyValue<String> stringValue(String propertyName, @Nullable String defaultValue) {
		return value(propertyName, Objects::toString, Objects::toString, defaultValue);
	}

	@Override
	public <T extends Enum<T>> PropertyValue<T> enumValue(String propertyName, Class<T> enumClass) {
		requireNonNull(enumClass);

		return value(propertyName, value -> Enum.valueOf(enumClass, value.toUpperCase()), Objects::toString);
	}

	@Override
	public <T extends Enum<T>> PropertyValue<T> enumValue(String propertyName, Class<T> enumClass, @Nullable T defaultValue) {
		requireNonNull(enumClass);

		return value(propertyName, value -> Enum.valueOf(enumClass, value.toUpperCase()), Objects::toString, defaultValue);
	}

	@Override
	public <T> PropertyValue<List<T>> listValue(String propertyName, Function<String, T> decoder, Function<T, String> encoder) {
		return value(propertyName, new ListValueDecoder<>(decoder), new ListValueEncoder<>(encoder), emptyList());
	}

	@Override
	public <T> PropertyValue<List<T>> listValue(String propertyName, Function<String, T> decoder, Function<T, String> encoder, List<T> defaultValue) {
		return value(propertyName, new ListValueDecoder<>(decoder), new ListValueEncoder<>(encoder), defaultValue);
	}

	@Override
	public <T> PropertyValue<T> value(String propertyName, Function<String, T> decoder, Function<T, String> encoder) {
		return value(propertyName, decoder, encoder, null);
	}

	@Override
	public <T> PropertyValue<T> value(String propertyName, Function<String, T> decoder, Function<T, String> encoder, @Nullable T defaultValue) {
		synchronized (propertyValues) {
			if (propertyValues.containsKey(requireNonNull(propertyName))) {
				throw new IllegalStateException("A value has already been created for the property '" + propertyName + "'");
			}
			DefaultPropertyValue<T> value = new DefaultPropertyValue<>(propertyName, decoder, encoder, defaultValue);
			propertyValues.put(propertyName, value);

			return value;
		}
	}

	@Override
	public <T> Optional<PropertyValue<T>> propertyValue(String propertyName) {
		synchronized (propertyValues) {
			return Optional.ofNullable((PropertyValue<T>) propertyValues.get(requireNonNull(propertyName)));
		}
	}

	@Override
	public void setProperty(String propertyName, @Nullable String value) {
		synchronized (propertyValues) {
			if (propertyValues.containsKey(requireNonNull(propertyName))) {
				throw new IllegalArgumentException("Value bound properties can only be modified through their Value instances");
			}
			properties.setProperty(propertyName, value);
		}
	}

	@Override
	public @Nullable String getProperty(String propertyName) {
		return properties.getProperty(requireNonNull(propertyName));
	}

	@Override
	public Collection<String> properties(Predicate<String> predicate) {
		requireNonNull(predicate);
		return properties.stringPropertyNames().stream()
						.filter(predicate)
						.map(properties::getProperty)
						.collect(toList());
	}

	@Override
	public Collection<String> propertyNames(Predicate<String> predicate) {
		requireNonNull(predicate);
		return properties.stringPropertyNames().stream()
						.filter(predicate)
						.collect(toList());
	}

	@Override
	public boolean containsProperty(String propertyName) {
		return properties.containsKey(requireNonNull(propertyName));
	}

	@Override
	public void removeAll(Predicate<String> predicate) {
		synchronized (propertyValues) {
			Collection<String> propertyKeys = propertyNames(predicate);
			if (propertyKeys.stream().anyMatch(propertyValues::containsKey)) {
				throw new IllegalArgumentException("Value bound properties can only be modified through their Value instances");
			}
			propertyKeys.forEach(properties::remove);
		}
	}

	@Override
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

	private final class DefaultPropertyValue<T> extends AbstractValue<T> implements PropertyValue<T> {

		private final String name;
		private final Function<T, String> encoder;

		private @Nullable T value;

		private DefaultPropertyValue(String name, Function<String, T> decoder, Function<T, String> encoder, @Nullable T defaultValue) {
			super(defaultValue, Notify.CHANGED);
			this.name = requireNonNull(name);
			this.encoder = requireNonNull(encoder);
			set(getInitialValue(name, requireNonNull(decoder)));
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public T getOrThrow() {
			return getOrThrow("Required configuration value is missing: " + name);
		}

		@Override
		public void remove() {
			boolean wasNotNull = value != null;
			setValue(null);
			if (wasNotNull) {
				notifyListeners();
			}
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		protected @Nullable T getValue() {
			return value;
		}

		@Override
		protected void setValue(@Nullable T value) {
			this.value = value;
			if (value == null) {
				properties.remove(name);
				System.clearProperty(name);
			}
			else {
				properties.setProperty(name, encoder.apply(value));
				System.setProperty(name, properties.getProperty(name));
			}
		}

		private @Nullable T getInitialValue(String property, Function<String, T> decoder) {
			String initialValue = System.getProperty(property);
			if (initialValue == null) {
				initialValue = properties.getProperty(property);
			}

			return initialValue == null ? null : decoder.apply(initialValue);
		}
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
