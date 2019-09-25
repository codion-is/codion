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

  //trying to make to configuration file human readable by sorting the keys
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
    readConfigurationFile();
  }

  /**
   * Instantiates a Value representing the given property.
   * This value is not nullable, missing or null values are interpreted as 'false'.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   */
  public Value<Boolean> value(final String property, final Boolean defaultValue) {
    Objects.requireNonNull(property, "property");
    Objects.requireNonNull(defaultValue, "defaultValue");
    if (get(property) == null) {
      set(property, Boolean.toString(defaultValue));
    }
    final Value<Boolean> value = Values.value(parseValue(property, stringValue ->
            stringValue == null ? false : Boolean.parseBoolean(stringValue)), false);
    value.getChangeObserver().addDataListener(booleanValue -> set(property, booleanValue == null ? "false" : Boolean.toString(booleanValue)));


    return value;
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   */
  public Value<String> value(final String property, final String defaultValue) {
    Objects.requireNonNull(property, "property");
    Objects.requireNonNull(defaultValue, "defaultValue");
    if (get(property) == null) {
      set(property, defaultValue);
    }
    final Value<String> value = Values.value(get(property));
    value.getChangeObserver().addDataListener(stringValue -> set(property, stringValue));
    configurationValues.put(property, value);

    return value;
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   */
  public Value<Integer> value(final String property, final Integer defaultValue) {
    Objects.requireNonNull(property, "property");
    Objects.requireNonNull(defaultValue, "defaultValue");
    if (get(property) == null) {
      set(property, Integer.toString(defaultValue));
    }
    final Value<Integer> value = Values.value(parseValue(property, Integer::parseInt));
    value.getChangeObserver().addDataListener(integerValue -> set(property, Integer.toString(integerValue)));
    configurationValues.put(property, value);

    return value;
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   */
  public Value<Double> value(final String property, final Double defaultValue) {
    Objects.requireNonNull(property, "property");
    Objects.requireNonNull(defaultValue, "defaultValue");
    if (get(property) == null) {
      set(property, Double.toString(defaultValue));
    }
    final Value<Double> value = Values.value(parseValue(property, Double::parseDouble));
    value.getChangeObserver().addDataListener(doubleValue -> set(property, Double.toString(doubleValue)));
    configurationValues.put(property, value);

    return value;
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present
   * @return the configuration value
   * @throws NullPointerException if {@code property} or {@code defaultValue} is null
   */
  public Value<List<String>> value(final String property, final List<String> defaultValue) {
    Objects.requireNonNull(property, "property");
    Objects.requireNonNull(defaultValue, "defaultValue");
    if (get(property) == null) {
      setStringList(property, defaultValue);
    }
    final Value<List<String>> value = Values.value(getStringList(property));
    value.getChangeObserver().addDataListener(values -> setStringList(property, values));
    configurationValues.put(property, value);

    return value;
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
   * Sets the value of the given property
   * @param property the property
   * @param value the value
   */
  public void set(final String property, final String value) {
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
            propertyName.startsWith(prefix)).map(this::get).collect(Collectors.toList());
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
   */
  public void removeAll(final String prefix) {
    getProperties(prefix).forEach(properties::remove);
  }

  private <V> V parseValue(final String property, final org.jminor.common.Configuration.ValueParser<V> parser) {
    return parser.parse(get(property));
  }

  private void setStringList(final String property, final List<String> strings) {
    set(property, String.join(VALUE_SEPARATOR, strings));
  }

  private List<String> getStringList(final String property) {
    return new ArrayList<>(Arrays.asList(get(property).split(VALUE_SEPARATOR)));
  }

  private void readConfigurationFile() throws IOException {
    final File settingsFile = new File(propertiesFile);
    if (settingsFile.exists()) {
      try (final InputStream input = new FileInputStream(settingsFile)) {
        properties.load(input);
      }
    }
  }
}
