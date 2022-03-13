/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.properties;

import is.codion.common.properties.PropertyValue.Builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Provides configuration values which sync with a central configuration store as well as system properties,
 * which can be written to file.
 * Initial values parsed from a configuration file are overridden by system properties.
 * If values are not found in the configuration file or in system properties the default value is used as the inital value.
 * When the value is set to null via {@link is.codion.common.value.Value#set(Object)} the default value is used, if one has been specified.
 * <pre>
 * File configurationFile = new File(System.getProperty("user.home") + "/app.properties");
 *
 * PropertyStore store = PropertyStore.propertyStore(configurationFile);
 *
 * Value&lt;Boolean&gt; featureEnabled = store.stringValue("feature.enabled")
 *    .defaultValue(false)
 *    .build();
 * Value&lt;String&gt; defaultUsername = store.stringValue("default.username")
 *    .defaultValue(System.getProperty("user.name"))
 *    .build();
 *
 * featureEnabled.set(true);
 * defaultUsername.set("scott");
 *
 * store.writeToFile(configurationFile);
 *
 * //reverts to the default value
 * featureEnabled.set(null);
 * defaultUsername.set(null);
 * </pre>
 */
public interface PropertyStore {

  /**
   * Instantiates a builder for the given boolean property
   * @param propertyName the property name
   * @return a new {@link Builder} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  Builder<Boolean> booleanValue(String propertyName);

  /**
   * Instantiates a builder for the given double property
   * @param propertyName the property name
   * @return a new {@link Builder} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  Builder<Double> doubleValue(String propertyName);

  /**
   * Instantiates a builder for the given integer property
   * @param propertyName the property name
   * @return a new {@link Builder} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  Builder<Integer> integerValue(String propertyName);

  /**
   * Instantiates a builder for the given long property
   * @param propertyName the property name
   * @return a new {@link Builder} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  Builder<Long> longValue(String propertyName);

  /**
   * Instantiates a builder for the given string property
   * @param propertyName the property name
   * @return a new {@link Builder} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  Builder<String> stringValue(String propertyName);

  /**
   * Instantiates a builder for the given enum property
   * @param <T> the enum type
   * @param propertyName the property name
   * @param enumClass the enum class
   * @return a new {@link Builder} instance
   * @throws NullPointerException if {@code propertyName} or {@code enumClass} is null
   */
  <T extends Enum<T>> Builder<T> enumValue(String propertyName, Class<T> enumClass);

  /**
   * Instantiates a builder for the given list property.
   * Note that a list property automatically gets an {@link Collections#emptyList()} as its default value.
   * @param <T> the value type
   * @param propertyName the property name
   * @param decoder a decoder for decoding the value from a string
   * @param encoder an encoder for encoding the value to a string
   * @return a new {@link Builder} instance
   * @throws NullPointerException if {@code propertyName}, {@code decoder} or {@code encoder} is null
   */
  <T> Builder<List<T>> listValue(String propertyName, Function<String, T> decoder, Function<T, String> encoder);

  /**
   * Instantiates a new builder representing the given property name.
   * @param <T> the value type
   * @param propertyName the configuration property name identifying this value
   * @param decoder a decoder for decoding the value from a string
   * @param encoder an encoder for encoding the value to a string
   * @return the configuration value
   * @throws NullPointerException if {@code propertyName}, {@code decoder} or {@code encoder} is null
   */
  <T> Builder<T> value(String propertyName, Function<String, T> decoder, Function<T, String> encoder);

  /**
   * Returns the Value associated with the given property, an empty Optional if none has been created.
   * @param propertyName the property name
   * @param <T> the value type
   * @return the configuration value or an empty Optional if none exists
   */
  <T> Optional<PropertyValue<T>> getPropertyValue(String propertyName);

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
   * Instantiates a new empy PropertyStore.
   * @return a new empty PropertyStore instance
   */
  static PropertyStore propertyStore() {
    return propertyStore(new Properties());
  }

  /**
   * Instantiates a new PropertyStore initialized with the properties found in the given file.
   * @param inputStream the input stream to read from
   * @return a new PropertyStore
   * @throws IOException in case the given input stream could not be read
   */
  static PropertyStore propertyStore(InputStream inputStream) throws IOException {
    return new DefaultPropertyStore(inputStream);
  }

  /**
   * Instantiates a new PropertyStore initialized with the properties found in the given file.
   * @param propertiesFile the file to read from initially
   * @return a new PropertyStore
   * @throws IOException in case the given properties file exists but reading it failed
   * @throws FileNotFoundException in case the file does not exist
   */
  static PropertyStore propertyStore(File propertiesFile) throws IOException {
    return new DefaultPropertyStore(propertiesFile);
  }

  /**
   * Instantiates a new PropertyStore initialized with the given properties.
   * @param properties the initial properties
   * @return a new PropertyStore
   */
  static PropertyStore propertyStore(Properties properties) {
    return new DefaultPropertyStore(properties);
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
  static String getSystemProperties(PropertyFormatter propertyFormatter) {
    requireNonNull(propertyFormatter, "propertyWriter");
    try {
      SecurityManager manager = System.getSecurityManager();
      if (manager != null) {
        manager.checkPropertiesAccess();
      }
    }
    catch (SecurityException e) {
      System.err.println(e.getMessage());
      return "";
    }
    Properties props = System.getProperties();
    Enumeration<?> propNames = props.propertyNames();
    List<String> propertyNames = new ArrayList<>(props.size());
    while (propNames.hasMoreElements()) {
      propertyNames.add((String) propNames.nextElement());
    }

    Collections.sort(propertyNames);

    return propertyNames.stream()
            .map(key -> key + ": " + propertyFormatter.formatValue(key, props.getProperty(key)))
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
