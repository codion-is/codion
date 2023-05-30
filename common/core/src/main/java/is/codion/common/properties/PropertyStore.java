/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Provides configuration values which sync with system properties when set. Note that setting the value via {@link System#setProperty(String, String)} does not affect the property store value, so the value should only be modified via the property store value instance.
 * If no value is found in a configuration file or in a system property, the default property value is used as the inital value.
 * When the value is set to null via {@link is.codion.common.value.Value#set(Object)} the default value is used, if one has been specified.
 * <pre>
 * File configurationFile = new File(System.getProperty("user.home") + "/app.properties");
 *
 * PropertyStore store = PropertyStore.propertyStore(configurationFile);
 *
 * Value&lt;Boolean&gt; featureEnabled = store.booleanValue("feature.enabled", false);
 * Value&lt;String&gt; defaultUsername = store.stringValue("default.username", System.getProperty("user.name"));
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
 * </pre>
 */
public interface PropertyStore {

  /**
   * Creates a value for the given boolean property
   * @param propertyName the property name
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<Boolean> booleanValue(String propertyName);

  /**
   * Creates a value for the given boolean property
   * @param propertyName the property name
   * @param defaultValue the default value
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<Boolean> booleanValue(String propertyName, boolean defaultValue);

  /**
   * Creates a value for the given double property
   * @param propertyName the property name
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<Double> doubleValue(String propertyName);

  /**
   * Creates a value for the given double property
   * @param propertyName the property name
   * @param defaultValue the default value
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<Double> doubleValue(String propertyName, double defaultValue);

  /**
   * Creates a value for the given integer property
   * @param propertyName the property name
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<Integer> integerValue(String propertyName);

  /**
   * Creates a value for the given integer property
   * @param propertyName the property name
   * @param defaultValue the default value
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<Integer> integerValue(String propertyName, int defaultValue);

  /**
   * Creates a value for the given long property
   * @param propertyName the property name
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<Long> longValue(String propertyName);

  /**
   * Creates a value for the given long property
   * @param propertyName the property name
   * @param defaultValue the default value
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<Long> longValue(String propertyName, long defaultValue);

  /**
   * Creates a value for the given long property
   * @param propertyName the property name
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<Character> characterValue(String propertyName);

  /**
   * Creates a value for the given long property
   * @param propertyName the property name
   * @param defaultValue the default value
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<Character> characterValue(String propertyName, char defaultValue);

  /**
   * Creates a value for the given string property
   * @param propertyName the property name
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<String> stringValue(String propertyName);

  /**
   * Creates a value for the given string property
   * @param propertyName the property name
   * @param defaultValue the default value
   * @return a new {@link PropertyValue} instance
   * @throws NullPointerException if {@code propertyName} is null
   */
  PropertyValue<String> stringValue(String propertyName, String defaultValue);

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
  <T extends Enum<T>> PropertyValue<T> enumValue(String propertyName, Class<T> enumClass, T defaultValue);

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
  <T> PropertyValue<T> value(String propertyName, Function<String, T> decoder, Function<T, String> encoder, T defaultValue);

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
  void setProperty(String propertyName, String value);

  /**
   * Retrieves the value for the given property, null if no value is present
   * @param propertyName the property name
   * @return the value or null if no value is present
   */
  String getProperty(String propertyName);

  /**
   * Returns the values associated with the properties with the given prefix
   * @param prefix the property prefix
   * @return all values associated with the properties with the given prefix
   */
  Collection<String> properties(String prefix);

  /**
   * Returns all property names with the given prefix
   * @param prefix the property name prefix
   * @return all property names with the given prefix
   */
  Collection<String> propertyNames(String prefix);

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
  static PropertyStore propertyStore(File propertiesFile) throws IOException {
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
   * @return a String containing all system properties, one per line
   */
  static String systemProperties() {
    return systemProperties((property, value) -> value);
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
