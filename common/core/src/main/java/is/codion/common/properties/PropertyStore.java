/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.properties;

import is.codion.common.value.PropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Provides configuration values which sync with a central configuration store as well as system properties,
 * which can be written to file.
 * Initial values parsed from a configuration file are overridden by system properties.
 * If values are not found in the configuration file or in system properties the default value is used.
 * <pre>
 * File configurationFile = new File(System.getProperty("user.home") + "/app.properties");
 *
 * PropertyStore store = PropertyStore.propertyStore(configurationFile);
 *
 * Value&lt;Boolean&gt; featureEnabled = store.propertyValue("feature.enabled", false);
 * Value&lt;String&gt; defaultUsername = store.propertyValue("default.username", System.getProperty("user.name"));
 *
 * featureEnabled.set(true);
 * defaultUsername.set("scott");
 *
 * store.writeToFile(configurationFile);
 * </pre>
 */
public interface PropertyStore {

  /**
   * Instantiates a Value representing the given property.
   * @param propertyName the configuration property name identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code propertyName} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  PropertyValue<Boolean> propertyValue(String propertyName, Boolean defaultValue);

  /**
   * Instantiates a Value representing the given property.
   * @param propertyName the configuration property name identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code propertyName} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  PropertyValue<String> propertyValue(String propertyName, String defaultValue);

  /**
   * Instantiates a Value representing the given property.
   * @param propertyName the configuration property name identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code propertyName} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  PropertyValue<Integer> propertyValue(String propertyName, Integer defaultValue);

  /**
   * Instantiates a Value representing the given property.
   * @param propertyName the configuration property name identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code propertyName} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  PropertyValue<Double> propertyValue(String propertyName, Double defaultValue);

  /**
   * Instantiates a Value representing the given property.
   * @param <V> the value type
   * @param propertyName the configuration property name identifying this value
   * @param defaultValue the default value to use if no initial value is present
   * @param nullValue the value to use instead of null, if any
   * @param decoder a decoder for decoding the value from a string
   * @param encoder an encoder for encoding the value to a string
   * @return the configuration value
   * @throws NullPointerException if {@code propertyName}, {@code decoder} or {@code encoder} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  <V> PropertyValue<V> propertyValue(String propertyName, V defaultValue, V nullValue,
                                     Function<String, V> decoder, Function<V, String> encoder);

  /**
   * Instantiates a Value representing the given property.
   * @param <V> the value type
   * @param propertyName the configuration property name identifying this value
   * @param defaultValue the default value to use if no initial value is present
   * @param decoder a decoder for decoding the value from a string
   * @param encoder an encoder for encoding the value to a string
   * @return the configuration value
   * @throws NullPointerException if {@code propertyName}, {@code decoder} or {@code encoder} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  <V> PropertyValue<List<V>> propertyListValue(String propertyName, List<V> defaultValue,
                                               Function<String, V> decoder, Function<V, String> encoder);

  /**
   * Returns the Value associated with the given property, null if none has been created.
   * @param propertyName the property name
   * @param <V> the value type
   * @return the configuration value or null if none is found
   */
  <V> PropertyValue<V> getPropertyValue(String propertyName);

  /**
   * Sets the value of the given property
   * @param propertyName the property name
   * @param value the value
   * @throws IllegalArgumentException if the property is value bound
   */
  void setProperty(String propertyName, String value);

  /**
   * Retrieves the value for the given property, null if no value is present
   * @param propertyName the property name
   * @return the value or null if no value is present
   */
  String getProperty(String propertyName);

  /**
   * Returns the values associated with the properties with the given prefix
   * @return all values associated with the properties with the given prefix
   * @param prefix the property prefix
   */
  List<String> getProperties(String prefix);

  /**
   * Returns all property names with the given prefix
   * @return all property names with the given prefix
   * @param prefix the property name prefix
   */
  List<String> getPropertyNames(String prefix);

  /**
   * Returns true if this PropertyStore contains a value for the given property
   * @param propertyName the property
   * @return true if a value for the given property exists
   */
  boolean containsProperty(String propertyName);

  /**
   * Removes all properties with the given prefix
   * @param prefix the prefix
   * @throws IllegalArgumentException in case any of the properties with the given prefix are value bound
   */
  void removeAll(String prefix);

  /**
   * Writes the stored properties to a file
   * @param propertiesFile the properties file to write to
   * @throws IOException in case writing the file was not successful
   */
  void writeToFile(File propertiesFile) throws IOException;

  /**
   * Instantiates a new PropertyStore initialized with the properties found in the given file.
   * @param propertiesFile the file to read from initially
   * @return a new PropertyStore
   * @throws IOException in case the given properties file exists but reading it failed
   */
  static PropertyStore propertyStore(final File propertiesFile) throws IOException {
    return propertyStore(readFromFile(requireNonNull(propertiesFile)));
  }

  /**
   * Instantiates a new PropertyStore initialized with the given properties.
   * @param properties the initial properties
   * @return a new PropertyStore
   */
  static PropertyStore propertyStore(final Properties properties) {
    return new DefaultPropertyStore(properties);
  }

  /**
   * Reads all properties from the given properties file if it exists,
   * if it does not an empty {@link Properties} instance is returned.
   * @param propertiesFile the properties file to read from
   * @return the properties read from the given file
   * @throws IOException in case the file exists but can not be read
   */
  static Properties readFromFile(final File propertiesFile) throws IOException {
    final Properties propertiesFromFile = new Properties();
    if (propertiesFile.exists()) {
      try (final InputStream input = new FileInputStream(propertiesFile)) {
        propertiesFromFile.load(input);
      }
    }

    return propertiesFromFile;
  }

  /**
   * @return a String containing all system properties, one per line
   */
  static String getSystemProperties() {
    return getSystemProperties((property, value) -> value);
  }

  /**
   * Returns a String containing all system properties, sorted by name, written by the given {@link PropertyFormatter}.
   * @param propertyFormatter for specific property formatting or exclusions
   * @return a String containing all system properties, one per line
   */
  static String getSystemProperties(final PropertyFormatter propertyFormatter) {
    requireNonNull(propertyFormatter, "propertyWriter");
    try {
      final SecurityManager manager = System.getSecurityManager();
      if (manager != null) {
        manager.checkPropertiesAccess();
      }
    }
    catch (final SecurityException e) {
      System.err.println(e.getMessage());
      return "";
    }
    final Properties props = System.getProperties();
    final Enumeration<?> propNames = props.propertyNames();
    final List<String> propertyNames = new ArrayList<>(props.size());
    while (propNames.hasMoreElements()) {
      propertyNames.add((String) propNames.nextElement());
    }

    Collections.sort(propertyNames);

    return propertyNames.stream().map(key -> key + ": " +
            propertyFormatter.formatValue(key, props.getProperty(key))).collect(Collectors.joining("\n"));
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
