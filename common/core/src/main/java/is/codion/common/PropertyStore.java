/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import is.codion.common.value.AbstractValue;
import is.codion.common.value.PropertyValue;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Provides configuration values which sync with a central configuration store as well as system properties,
 * which can be written to file.
 * Initial values parsed from a configuration file are overridden by system properties.
 * If values are not found in the configuration file or in system properties the default value is used.
 * <pre>
 * File configurationFile = new File(System.getProperty("user.home") + "/app.properties");
 *
 * PropertyStore store = new PropertyStore(configurationFile);
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
public final class PropertyStore {

  /**
   * The separator used to separate multiple values.
   */
  public static final String VALUE_SEPARATOR = ";";

  private final Map<String, PropertyValue<?>> propertyValues = new HashMap<>();

  private final Properties properties = new Properties() {
    @Override
    public Enumeration<Object> keys() {
      //sort to make properties file easier for human consumption
      final List<Object> keys = Collections.list(super.keys());
      keys.sort(Comparator.comparing(Object::toString));

      return Collections.enumeration(keys);
    }
  };

  /**
   * Instantiates a new PropertyStore backed by the given file.
   * If the file exists this PropertyStore is initialized with the properties and values found in it.
   * @param propertiesFile the file to read from initially
   * @throws IOException in case the given properties file exists but reading it failed
   */
  public PropertyStore(final File propertiesFile) throws IOException {
    this(readFromFile(requireNonNull(propertiesFile)));
  }

  /**
   * Instantiates a new PropertyStore initialized with the given properties.
   * @param properties the initial properties
   */
  public PropertyStore(final Properties properties) {
    this.properties.putAll(requireNonNull(properties, "properties"));
    this.properties.stringPropertyNames().forEach(property ->
            System.setProperty(property, this.properties.getProperty(property)));
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public PropertyValue<Boolean> propertyValue(final String property, final Boolean defaultValue) {
    return propertyValue(property, defaultValue, false, Boolean::parseBoolean, Objects::toString);
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public PropertyValue<String> propertyValue(final String property, final String defaultValue) {
    return propertyValue(property, defaultValue, null, Objects::toString, Objects::toString);
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public PropertyValue<Integer> propertyValue(final String property, final Integer defaultValue) {
    return propertyValue(property, defaultValue, null, Integer::parseInt, Objects::toString);
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public PropertyValue<Double> propertyValue(final String property, final Double defaultValue) {
    return propertyValue(property, defaultValue, null, Double::parseDouble, Objects::toString);
  }

  /**
   * Instantiates a Value representing the given property.
   * @param <V> the value type
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no initial value is present
   * @param nullValue the value to use instead of null, if any
   * @param decoder a decoder for decoding the value from a string
   * @param encoder an encoder for encoding the value to a string
   * @return the configuration value
   * @throws NullPointerException if {@code property}, {@code decoder} or {@code encoder} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public <V> PropertyValue<V> propertyValue(final String property, final V defaultValue, final V nullValue,
                                            final Function<String, V> decoder, final Function<V, String> encoder) {
    if (propertyValues.containsKey(requireNonNull(property, "property"))) {
      throw new IllegalArgumentException("Configuration value for property '" + property + "' has already been created");
    }
    final DefaultPropertyValue<V> value = new DefaultPropertyValue<>(property, defaultValue, nullValue, decoder, encoder);
    propertyValues.put(property, value);

    return value;
  }

  /**
   * Instantiates a Value representing the given property.
   * @param <V> the value type
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no initial value is present
   * @param decoder a decoder for decoding the value from a string
   * @param encoder an encoder for encoding the value to a string
   * @return the configuration value
   * @throws NullPointerException if {@code property}, {@code decoder} or {@code encoder} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public <V> PropertyValue<List<V>> propertyListValue(final String property, final List<V> defaultValue,
                                                      final Function<String, V> decoder, final Function<V, String> encoder) {
    if (propertyValues.containsKey(requireNonNull(property, "property"))) {
      throw new IllegalArgumentException("Configuration value for property '" + property + "' has already been created");
    }

    final DefaultPropertyValue<List<V>> value = new DefaultPropertyValue<>(property, defaultValue, null,
            stringValue -> stringValue == null ? emptyList() :
                    Arrays.stream(stringValue.split(VALUE_SEPARATOR)).map(decoder).collect(Collectors.toList()),
            valueList -> valueList.stream().map(encoder).collect(Collectors.joining(VALUE_SEPARATOR)));
    propertyValues.put(property, value);

    return value;
  }

  /**
   * Returns the Value associated with the given property, null if none has been created.
   * @param property the property
   * @param <V> the value type
   * @return the configuration value or null if none is found
   */
  public <V> PropertyValue<V> getPropertyValue(final String property) {
    return (PropertyValue<V>) propertyValues.get(property);
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
   * Returns true if this PropertyStore contains a value for the given property
   * @param property the property
   * @return true if a value for the given property exists
   */
  public boolean containsProperty(final String property) {
    return properties.containsKey(property);
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
   * @param propertiesFile the properties file to write to
   * @throws IOException in case writing the file was not successful
   */
  public void writeToFile(final File propertiesFile) throws IOException {
    requireNonNull(propertiesFile, "propertiesFile");
    if (!propertiesFile.exists() && !propertiesFile.createNewFile()) {
      throw new IOException("Unable to create properties file: " + propertiesFile);
    }
    try (final OutputStream output = new FileOutputStream(propertiesFile)) {
      properties.store(output, null);
    }
  }

  /**
   * Reads all properties from the given properties file if it exists
   * @param propertiesFile the properties file to read from
   * @return the properties read from the given file
   * @throws IOException in case the file exists but can not be read
   */
  public static Properties readFromFile(final File propertiesFile) throws IOException {
    final Properties propertiesFromFile = new Properties();
    if (propertiesFile.exists()) {
      try (final InputStream input = new FileInputStream(propertiesFile)) {
        propertiesFromFile.load(input);
      }
    }

    return propertiesFromFile;
  }

  private final class DefaultPropertyValue<T> extends AbstractValue<T> implements PropertyValue<T> {

    private final String property;
    private final Function<T, String> encoder;
    private final T nullValue;

    private T value;

    private DefaultPropertyValue(final String property, final T defaultValue, final T nullValue,
                                 final Function<String, T> decoder, final Function<T, String> encoder) {
      this.property = property;
      this.nullValue = nullValue;
      requireNonNull(decoder, "decoder");
      this.encoder = requireNonNull(encoder, "encoder");
      final String initialValue = getInitialValue(property);
      set(initialValue == null ? defaultValue : decoder.apply(initialValue));
    }

    @Override
    public String getProperty() {
      return property;
    }

    @Override
    public void set(final T value) {
      final T newValue = value == null ? nullValue : value;
      if (!Objects.equals(this.value, newValue)) {
        this.value = newValue;
        if (newValue == null) {
          properties.remove(property);
          System.clearProperty(property);
        }
        else {
          properties.setProperty(property, encoder.apply(newValue));
          System.setProperty(property, properties.getProperty(property));
        }
        notifyValueChange();
      }
    }

    @Override
    public T get() {
      return value;
    }

    @Override
    public boolean isNullable() {
      return nullValue == null;
    }

    @Override
    public String toString() {
      return property;
    }

    private String getInitialValue(final String property) {
      String initialValue = System.getProperty(property);
      if (initialValue == null) {
        initialValue = properties.getProperty(property);
      }

      return initialValue;
    }
  }
}
