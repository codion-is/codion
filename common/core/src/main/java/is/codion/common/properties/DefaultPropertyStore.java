/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.properties;

import is.codion.common.value.AbstractValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
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

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

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

  DefaultPropertyStore(File propertiesFile) throws IOException {
    this(loadProperties(propertiesFile));
  }

  DefaultPropertyStore(InputStream inputStream) throws IOException {
    this(loadProperties(inputStream));
  }

  DefaultPropertyStore(Properties properties) {
    this.properties.putAll(requireNonNull(properties, "properties"));
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
    return stringValue(propertyName, null);
  }

  @Override
  public PropertyValue<String> stringValue(String propertyName, String defaultValue) {
    return value(propertyName, Objects::toString, Objects::toString, defaultValue);
  }

  @Override
  public <T extends Enum<T>> PropertyValue<T> enumValue(String propertyName, Class<T> enumClass) {
    return enumValue(propertyName, enumClass, null);
  }

  @Override
  public <T extends Enum<T>> PropertyValue<T> enumValue(String propertyName, Class<T> enumClass, T defaultValue) {
    requireNonNull(enumClass);

    return value(propertyName, value -> Enum.valueOf(enumClass, value.toUpperCase()), Objects::toString, defaultValue);
  }

  @Override
  public <T> PropertyValue<List<T>> listValue(String propertyName, Function<String, T> decoder, Function<T, String> encoder) {
    return listValue(propertyName, decoder, encoder, emptyList());
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
  public <T> PropertyValue<T> value(String propertyName, Function<String, T> decoder, Function<T, String> encoder, T defaultValue) {
    if (propertyValues.containsKey(requireNonNull(propertyName))) {
      throw new IllegalStateException("A value has already been created for the property '" + propertyName + "'");
    }
    DefaultPropertyValue<T> value = new DefaultPropertyValue<>(propertyName, decoder, encoder, defaultValue);
    propertyValues.put(propertyName, value);

    return value;
  }

  @Override
  public <T> Optional<PropertyValue<T>> getPropertyValue(String propertyName) {
    return Optional.ofNullable((PropertyValue<T>) propertyValues.get(requireNonNull(propertyName)));
  }

  @Override
  public void setProperty(String propertyName, String value) {
    if (propertyValues.containsKey(requireNonNull(propertyName))) {
      throw new IllegalArgumentException("Value bound properties can only be modified through their Value instances");
    }
    properties.setProperty(propertyName, value);
  }

  @Override
  public String getProperty(String propertyName) {
    return properties.getProperty(requireNonNull(propertyName));
  }

  @Override
  public List<String> getProperties(String prefix) {
    requireNonNull(prefix);
    return properties.stringPropertyNames().stream()
            .filter(propertyName -> propertyName.startsWith(prefix))
            .map(properties::getProperty)
            .collect(toList());
  }

  @Override
  public List<String> getPropertyNames(String prefix) {
    requireNonNull(prefix);
    return properties.stringPropertyNames().stream()
            .filter(propertyName -> propertyName.startsWith(prefix))
            .collect(toList());
  }

  @Override
  public boolean containsProperty(String propertyName) {
    return properties.containsKey(requireNonNull(propertyName));
  }

  @Override
  public void removeAll(String prefix) {
    List<String> propertyKeys = getPropertyNames(prefix);
    if (propertyKeys.stream().anyMatch(propertyValues::containsKey)) {
      throw new IllegalArgumentException("Value bound properties can only be modified through their Value instances");
    }
    propertyKeys.forEach(properties::remove);
  }

  @Override
  public void writeToFile(File propertiesFile) throws IOException {
    requireNonNull(propertiesFile, "propertiesFile");
    if (!propertiesFile.exists() && !propertiesFile.createNewFile()) {
      throw new IOException("Unable to create properties file: " + propertiesFile);
    }
    try (OutputStream output = Files.newOutputStream(propertiesFile.toPath())) {
      properties.store(output, null);
    }
  }

  /**
   * Reads all properties from the given properties file.
   * @param propertiesFile the properties file to read from
   * @return the properties read from the given file
   * @throws IOException in case the file exists but can not be read
   * @throws FileNotFoundException in case the file does not exist
   */
  private static Properties loadProperties(File propertiesFile) throws IOException {
    if (!requireNonNull(propertiesFile).exists()) {
      throw new FileNotFoundException(propertiesFile.toString());
    }
    try (InputStream input = Files.newInputStream(propertiesFile.toPath())) {
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

    private final String propertyName;
    private final Function<T, String> encoder;

    private T value;

    private DefaultPropertyValue(String propertyName, Function<String, T> decoder, Function<T, String> encoder, T defaultValue) {
      super(defaultValue, true);
      this.propertyName = requireNonNull(propertyName);
      this.encoder = requireNonNull(encoder);
      set(getInitialValue(propertyName, requireNonNull(decoder)));
    }

    @Override
    public String getPropertyName() {
      return propertyName;
    }

    @Override
    public T getOrThrow() throws IllegalStateException {
      return getOrThrow("Required configuration value is missing: " + propertyName);
    }

    @Override
    public T getOrThrow(String message) throws IllegalStateException {
      requireNonNull(message, "message");
      if (value == null) {
        throw new IllegalStateException(message);
      }

      return value;
    }

    @Override
    public T get() {
      return value;
    }

    @Override
    public void clear() {
      boolean wasNotNull = value != null;
      setValue(null);
      if (wasNotNull) {
        notifyValueChange();
      }
    }

    @Override
    public String toString() {
      return propertyName;
    }

    @Override
    protected void setValue(T value) {
      this.value = value;
      if (value == null) {
        properties.remove(propertyName);
        System.clearProperty(propertyName);
      }
      else {
        properties.setProperty(propertyName, encoder.apply(value));
        System.setProperty(propertyName, properties.getProperty(propertyName));
      }
    }

    private T getInitialValue(String property, Function<String, T> decoder) {
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
    public List<T> apply(String stringValue) {
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
}
