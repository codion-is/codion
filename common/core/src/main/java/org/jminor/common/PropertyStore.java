/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import java.util.stream.Collectors;

/**
 * Provides configuration values which sync with a central configuration store, which can be written to file.
 * <pre>
 * PropertyStore store = new PropertyStore(System.getProperty("user.home") + "/app.properties");
 *
 * Value&lt;Boolean&gt; featureEnabled = store.propertyValue("feature.enabled", false);
 * Value&lt;String&gt; defaultUsername = store.propertyValue("default.username", System.getProperty("user.name"));
 *
 * featureEnabled.set(true);
 * defaultUsername.set("scott");
 *
 * store.writeToFile();
 * </pre>
 */
public final class PropertyStore {

  public static final String VALUE_SEPARATOR = ";";

  private final String propertiesFile;
  private final Map<String, Value> propertyValues = new HashMap<>();

  //trying to make the configuration file a bit easier to read by sorting the keys
  private final Properties properties = new Properties() {
    //sort to make properties file easier for human consumption
    @Override
    public Enumeration<Object> keys() {
      final ArrayList<Object> keys = Collections.list(super.keys());
      keys.sort(Comparator.comparing(Object::toString));

      return Collections.enumeration(keys);
    }
  };

  /**
   * Instantiates a new ConfigurationStore backed by the given file.
   * If the file exists this ConfigurationStore is initialized with the properties and values found in it.
   * @param propertiesFile the path to the file to read from initially and to use when persisting the configuration properties
   * @throws IOException in case the given properties file exists but reading it failed
   */
  public PropertyStore(final String propertiesFile) throws IOException {
    this.propertiesFile = Objects.requireNonNull(propertiesFile);
    readFromFile();
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public Value<Boolean> propertyValue(final String property, final Boolean defaultValue) {
    return propertyValue(property, defaultValue, Boolean::parseBoolean);
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public Value<String> propertyValue(final String property, final String defaultValue) {
    return propertyValue(property, defaultValue, Objects::toString);
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public Value<Integer> propertyValue(final String property, final Integer defaultValue) {
    return propertyValue(property, defaultValue, Integer::parseInt);
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public Value<Double> propertyValue(final String property, final Double defaultValue) {
    return propertyValue(property, defaultValue, Double::parseDouble);
  }

  /**
   * Instantiates a Value representing the given property.
   * @param <V> the value type
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @param parser a parser for parsing the value from a string
   * @return the configuration value
   * @throws NullPointerException if {@code property}, {@code defaultValue} or {@code parser} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public <V> Value<V> propertyValue(final String property, final V defaultValue, final Function<String, V> parser) {
    if (propertyValues.containsKey(Objects.requireNonNull(property, "property"))) {
      throw new IllegalArgumentException("Configuration value for property '" + property + "' has already been created");
    }
    Objects.requireNonNull(defaultValue, "defaultValue");
    Objects.requireNonNull(parser, "parser");
    if (properties.getProperty(property) == null) {
      properties.setProperty(property, defaultValue.toString());
    }
    final Value<V> value = Values.value(parser.apply(properties.getProperty(property)), defaultValue);
    value.getChangeObserver().addDataListener(doubleValue -> properties.setProperty(property, doubleValue.toString()));
    propertyValues.put(property, value);

    return value;
  }

  /**
   * Instantiates a Value representing the given property.
   * @param <V> the value type
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @param parser a parser for parsing a value from a string
   * @return the configuration value
   * @throws NullPointerException if {@code property}, {@code defaultValue} or {@code parser} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public <V> Value<List<V>> propertyListValue(final String property, final List<V> defaultValue, final Function<String, V> parser) {
    if (propertyValues.containsKey(Objects.requireNonNull(property, "property"))) {
      throw new IllegalArgumentException("Configuration value for property '" + property + "' has already been created");
    }
    Objects.requireNonNull(defaultValue, "defaultValue");
    Objects.requireNonNull(parser, "parser");
    if (properties.getProperty(property) == null) {
      properties.setProperty(property, defaultValue.stream().map(Object::toString).collect(Collectors.joining(VALUE_SEPARATOR)));
    }
    final Value<List<V>> value = Values.value(Arrays.stream(properties.getProperty(property).split(VALUE_SEPARATOR))
            .map(parser).collect(Collectors.toList()), defaultValue);
    value.getChangeObserver().addDataListener(values ->
            properties.setProperty(property, values.stream().map(Object::toString).collect(Collectors.joining(VALUE_SEPARATOR))));
    propertyValues.put(property, value);

    return value;
  }

  /**
   * Returns the Value associated with the given property, null if none is present.
   * @param property the property
   * @param <V> the value type
   * @return the configuration value or null if none is found
   */
  public <V> Value<V> getPropertyValue(final String property) {
    return propertyValues.get(property);
  }

  /**
   * Sets the value of the given property
   * @param property the property
   * @param value the value
   * @throws IllegalArgumentException in the property is value bound
   */
  public void setProperty(final String property, final String value) {
    if (propertyValues.containsKey(property)) {
      throw new IllegalArgumentException("Value bound properties can only be modified through their Value instances");
    }
    properties.setProperty(property, value);
  }

  /**
   * Retrieves the value for the given property, null if no value is present
   * @param property the property
   * @return the value or null if no value is present
   */
  public String getProperty(final String property) {
    return properties.getProperty(property);
  }

  /**
   * Returns the values associated with the properties with the given prefix
   * @return all values associated with the properties with the given prefix
   * @param prefix the property prefix
   */
  public List<String> getProperties(final String prefix) {
    return properties.stringPropertyNames().stream().filter(propertyName ->
            propertyName.startsWith(prefix)).map(properties::getProperty).collect(Collectors.toList());
  }

  /**
   * Returns all property names with the given prefix
   * @return all property names with the given prefix
   * @param prefix the property name prefix
   */
  public List<String> getPropertyNames(final String prefix) {
    return properties.stringPropertyNames().stream().filter(propertyName ->
            propertyName.startsWith(prefix)).collect(Collectors.toList());
  }

  /**
   * Removes all properties with the given prefix
   * @param prefix the prefix
   * @throws IllegalArgumentException in case any of the properties with the given prefix are value bound
   */
  public void removeAll(final String prefix) {
    final List<String> propertyKeys = getPropertyNames(prefix);
    if (propertyKeys.stream().anyMatch(propertyValues::containsKey)) {
      throw new IllegalArgumentException("Value bound properties can only be modified through their Value instances");
    }
    propertyKeys.forEach(properties::remove);
  }

  /**
   * Writes the stored properties to a file
   * @throws IOException in case writing the file was not successful
   */
  public void writeToFile() throws IOException {
    final File configurationFile = new File(propertiesFile);
    if (!configurationFile.exists() && !configurationFile.createNewFile()) {
      throw new IOException("Unable to create configuration file");
    }
    try (final OutputStream output = new FileOutputStream(configurationFile)) {
      properties.store(output, null);
    }
  }

  /**
   * Reads all properties from the underlying properties file
   * @throws IOException in case the file exists but can not be read
   */
  private void readFromFile() throws IOException {
    final File file = new File(propertiesFile);
    if (file.exists()) {
      try (final InputStream input = new FileInputStream(file)) {
        properties.load(input);
      }
    }
  }
}
