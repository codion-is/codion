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
import java.util.stream.Collectors;

/**
 * Provides configuration values which sync with a central configuration store, which can be written to file.
 */
public final class ConfigurationStore {

  public static final String VALUE_SEPARATOR = ";";

  private final String propertiesFile;
  private final Map<String, Value> configurationValues = new HashMap<>();

  //trying to make the configuration file human readable by sorting the keys
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
   * @param propertiesFile the file to read from initially and to use when persisting the configuration properties
   * @throws IOException in case the given properties file exists but reading it failed
   */
  public ConfigurationStore(final String propertiesFile) throws IOException {
    this.propertiesFile = Objects.requireNonNull(propertiesFile);
    readFromFile();
  }

  /**
   * Returns the configuration value associated with the given property.
   * @param property the property
   * @param <V> the value type
   * @return the configuration value or null if none is found
   */
  public <V> Value<V> getConfigurationValue(final String property) {
    return configurationValues.get(property);
  }

  /**
   * Instantiates a Value representing the given property.
   * If a Value has been created previously for the given property it is returned.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and
   * when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   */
  public Value<Boolean> value(final String property, final Boolean defaultValue) {
    return value(property, defaultValue, Boolean::parseBoolean);
  }

  /**
   * Instantiates a Value representing the given property.
   * If a Value has been created previously for the given property it is returned.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and
   * when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   */
  public Value<String> value(final String property, final String defaultValue) {
    return value(property, defaultValue, Objects::toString);
  }

  /**
   * Instantiates a Value representing the given property.
   * If a Value has been created previously for the given property it is returned.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and
   * when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   */
  public Value<Integer> value(final String property, final Integer defaultValue) {
    return value(property, defaultValue, Integer::parseInt);
  }

  /**
   * Instantiates a Value representing the given property.
   * If a Value has been created previously for the given property it is returned.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and
   * when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   */
  public Value<Double> value(final String property, final Double defaultValue) {
    return value(property, defaultValue, Double::parseDouble);
  }

  /**
   * Instantiates a Value representing the given property.
   * If a Value has been created previously for the given property it is returned.
   * @param <V> the value type
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and
   * when the value is set to null
   * @param parser a parser for parsing a value from a string
   * @return the configuration value
   * @throws NullPointerException if {@code property}, {@code defaultValue} or {@code parser} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public <V> Value<V> value(final String property, final V defaultValue, final Configuration.ValueParser<V> parser) {
    if (configurationValues.containsKey(Objects.requireNonNull(property, "property"))) {
      throw new IllegalArgumentException("Configuration value for property '" + property + "' has already been created");
    }
    Objects.requireNonNull(defaultValue, "defaultValue");
    Objects.requireNonNull(parser, "parser");
    if (properties.getProperty(property) == null) {
      properties.setProperty(property, defaultValue.toString());
    }
    final Value<V> value = Values.value(parser.parse(properties.getProperty(property)), defaultValue);
    value.getChangeObserver().addDataListener(doubleValue -> properties.setProperty(property, doubleValue.toString()));
    configurationValues.put(property, value);

    return value;
  }

  /**
   * Instantiates a Value representing the given property.
   * If a Value has been created previously for the given property it is returned.
   * @param <V> the value type
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and
   * when the value is set to null
   * @param parser a parser for parsing a value from a string
   * @return the configuration value
   * @throws NullPointerException if {@code property}, {@code defaultValue} or {@code parser} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public <V> Value<List<V>> listValue(final String property, final List<V> defaultValue,
                                      final Configuration.ValueParser<V> parser) {
    if (configurationValues.containsKey(Objects.requireNonNull(property, "property"))) {
      throw new IllegalArgumentException("Configuration value for property '" + property + "' has already been created");
    }
    Objects.requireNonNull(defaultValue, "defaultValue");
    Objects.requireNonNull(parser, "parser");
    if (properties.getProperty(property) == null) {
      properties.setProperty(property, defaultValue.stream().map(Object::toString).collect(Collectors.joining(VALUE_SEPARATOR)));
    }
    final Value<List<V>> value = Values.value(Arrays.stream(properties.getProperty(property).split(VALUE_SEPARATOR))
            .map(parser::parse).collect(Collectors.toList()), defaultValue);
    value.getChangeObserver().addDataListener(values ->
            properties.setProperty(property, values.stream().map(Object::toString).collect(Collectors.joining(VALUE_SEPARATOR))));
    configurationValues.put(property, value);

    return value;
  }

  /**
   * Sets the value of the given property
   * @param property the property
   * @param value the value
   * @throws IllegalArgumentException in the property is value bound
   */
  public void set(final String property, final String value) {
    if (configurationValues.containsKey(property)) {
      throw new IllegalArgumentException("Value bound properties can not be directly manipulated");
    }
    properties.setProperty(property, value);
  }

  /**
   * Retreives the value for the given property
   * @param property the property
   * @return the value
   */
  public String get(final String property) {
    return properties.getProperty(property);
  }

  /**
   * @return all properties with the given prefix
   * @param prefix the property prefix
   */
  public List<String> getValues(final String prefix) {
    return properties.stringPropertyNames().stream().filter(propertyName ->
            propertyName.startsWith(prefix)).map(properties::getProperty).collect(Collectors.toList());
  }

  /**
   * @return all properties with the given prefix
   * @param prefix the property prefix
   */
  public List<String> getProperties(final String prefix) {
    return properties.stringPropertyNames().stream().filter(propertyName ->
            propertyName.startsWith(prefix)).collect(Collectors.toList());
  }

  /**
   * Removes all properties with the given prefix
   * @param prefix the prefix
   * @throws IllegalArgumentException in case any of the properties with the given prefix are value bound
   */
  public void removeAll(final String prefix) {
    final List<String> propertyKeys = getProperties(prefix);
    if (propertyKeys.stream().anyMatch(configurationValues::containsKey)) {
      throw new IllegalArgumentException("Value bound properties can not be directly manipulated");
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

  private void readFromFile() throws IOException {
    final File conigurationFile = new File(propertiesFile);
    if (conigurationFile.exists()) {
      try (final InputStream input = new FileInputStream(conigurationFile)) {
        properties.load(input);
      }
    }
  }
}
