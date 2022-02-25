/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.properties;

import is.codion.common.value.AbstractValue;
import is.codion.common.value.PropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  public PropertyValue<Boolean> propertyValue(String propertyName, Boolean defaultValue) {
    return propertyValue(propertyName, defaultValue, false, Boolean::parseBoolean, Objects::toString);
  }

  @Override
  public PropertyValue<String> propertyValue(String propertyName, String defaultValue) {
    return propertyValue(propertyName, defaultValue, null, Objects::toString, Objects::toString);
  }

  @Override
  public PropertyValue<Integer> propertyValue(String propertyName, Integer defaultValue) {
    return propertyValue(propertyName, defaultValue, null, Integer::parseInt, Objects::toString);
  }

  @Override
  public PropertyValue<Double> propertyValue(String propertyName, Double defaultValue) {
    return propertyValue(propertyName, defaultValue, null, Double::parseDouble, Objects::toString);
  }

  @Override
  public <T> PropertyValue<T> propertyValue(String propertyName, T defaultValue, T nullValue,
                                            Function<String, T> decoder, Function<T, String> encoder) {
    if (propertyValues.containsKey(requireNonNull(propertyName, "propertyName"))) {
      throw new IllegalArgumentException("Configuration value for property '" + propertyName + "' has already been created");
    }
    DefaultPropertyValue<T> value = new DefaultPropertyValue<>(propertyName, defaultValue, nullValue, decoder, encoder);
    propertyValues.put(propertyName, value);

    return value;
  }

  @Override
  public <T> PropertyValue<List<T>> propertyListValue(String propertyName, List<T> defaultValue,
                                                      Function<String, T> decoder, Function<T, String> encoder) {
    if (propertyValues.containsKey(requireNonNull(propertyName, "propertyName"))) {
      throw new IllegalArgumentException("Configuration value for property '" + propertyName + "' has already been created");
    }

    DefaultPropertyValue<List<T>> value = new DefaultPropertyValue<>(propertyName, defaultValue, null,
            stringValue -> stringValue == null ? emptyList() :
                    Arrays.stream(stringValue.split(VALUE_SEPARATOR))
                            .map(decoder)
                            .collect(toList()),
            valueList -> valueList.stream()
                    .map(encoder)
                    .collect(joining(VALUE_SEPARATOR)));
    propertyValues.put(propertyName, value);

    return value;
  }

  @Override
  public <T> PropertyValue<T> getPropertyValue(String propertyName) {
    return (PropertyValue<T>) propertyValues.get(propertyName);
  }

  @Override
  public void setProperty(String propertyName, String value) {
    if (propertyValues.containsKey(propertyName)) {
      throw new IllegalArgumentException("Value bound properties can only be modified through their Value instances");
    }
    properties.setProperty(propertyName, value);
  }

  @Override
  public String getProperty(String propertyName) {
    return properties.getProperty(propertyName);
  }

  @Override
  public List<String> getProperties(String prefix) {
    return properties.stringPropertyNames().stream()
            .filter(propertyName -> propertyName.startsWith(prefix))
            .map(properties::getProperty)
            .collect(toList());
  }

  @Override
  public List<String> getPropertyNames(String prefix) {
    return properties.stringPropertyNames().stream()
            .filter(propertyName -> propertyName.startsWith(prefix))
            .collect(toList());
  }

  @Override
  public boolean containsProperty(String propertyName) {
    return properties.containsKey(propertyName);
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
    try (OutputStream output = new FileOutputStream(propertiesFile)) {
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
    try (InputStream input = new FileInputStream(propertiesFile)) {
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

    private DefaultPropertyValue(String propertyName, T defaultValue, T nullValue,
                                 Function<String, T> decoder, Function<T, String> encoder) {
      super(nullValue, NotifyOnSet.YES);
      this.propertyName = propertyName;
      requireNonNull(decoder, "decoder");
      this.encoder = requireNonNull(encoder, "encoder");
      String initialValue = getInitialValue(propertyName);
      set(initialValue == null ? defaultValue : decoder.apply(initialValue));
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

    private String getInitialValue(String property) {
      String initialValue = System.getProperty(property);
      if (initialValue == null) {
        initialValue = properties.getProperty(property);
      }

      return initialValue;
    }
  }
}
