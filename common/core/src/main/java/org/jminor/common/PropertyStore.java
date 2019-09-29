/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Provides configuration values which sync with a central configuration store as well as system properties,
 * which can be written to file.
 * Initial values parsed from a configuration file are overridden by system properties.
 * If values are not found in the configuration file or in system properties the default value is used.
 * <pre>
 * String configurationFile = System.getProperty("user.home") + "/app.properties";
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

  private static final Logger LOG = LoggerFactory.getLogger(PropertyStore.class);

  /**
   * The separator used to separate multiple values.
   */
  public static final String VALUE_SEPARATOR = ";";

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
   * Instantiates a new PropertyStore backed by the given file.
   * If the file exists this PropertyStore is initialized with the properties and values found in it.
   * @param propertiesFile the path to the file to read from initially
   * @throws IOException in case the given properties file exists but reading it failed
   */
  public PropertyStore(final String propertiesFile) throws IOException {
    this(readFromFile(Objects.requireNonNull(propertiesFile)));
  }

  /**
   * Instantiates a new PropertyStore initialized with the given properties.
   * @param properties the initial properties
   */
  public PropertyStore(final Properties properties) {
    this.properties.putAll(Objects.requireNonNull(properties, "properties"));
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public Value<Boolean> propertyValue(final String property, final Boolean defaultValue) {
    return propertyValue(property, defaultValue, Boolean::parseBoolean, Objects::toString);
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public Value<String> propertyValue(final String property, final String defaultValue) {
    return propertyValue(property, defaultValue, Objects::toString, Objects::toString);
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public Value<Integer> propertyValue(final String property, final Integer defaultValue) {
    return propertyValue(property, defaultValue, Integer::parseInt, Objects::toString);
  }

  /**
   * Instantiates a Value representing the given property.
   * @param property the configuration property identifying this value
   * @param defaultValue the default value to use if no value is present and when the value is set to null
   * @return the configuration value
   * @throws NullPointerException if {@code property} is null
   * @throws IllegalArgumentException in case a Value for the given property has already been created
   */
  public Value<Double> propertyValue(final String property, final Double defaultValue) {
    return propertyValue(property, defaultValue, Double::parseDouble, Objects::toString);
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
  public <V> Value<V> propertyValue(final String property, final V defaultValue,
                                    final Function<String, V> decoder, final Function<V, String> encoder) {
    if (propertyValues.containsKey(Objects.requireNonNull(property, "property"))) {
      throw new IllegalArgumentException("Configuration value for property '" + property + "' has already been created");
    }
    final PropertyValue<V> value = new PropertyValue<>(property, defaultValue, decoder, encoder);
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
  public <V> Value<List<V>> propertyListValue(final String property, final List<V> defaultValue,
                                              final Function<String, V> decoder, final Function<V, String> encoder) {
    if (propertyValues.containsKey(Objects.requireNonNull(property, "property"))) {
      throw new IllegalArgumentException("Configuration value for property '" + property + "' has already been created");
    }

    final PropertyValue<List<V>> value = new PropertyValue<>(property, defaultValue,
            stringValue -> stringValue == null ? Collections.emptyList() :
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
  public void writeToFile(final String propertiesFile) throws IOException {
    final File configurationFile = new File(Objects.requireNonNull(propertiesFile, "propertiesFile"));
    if (!configurationFile.exists() && !configurationFile.createNewFile()) {
      throw new IOException("Unable to create configuration file");
    }
    LOG.debug("Writing configuration to file: {}", configurationFile);
    try (final OutputStream output = new FileOutputStream(configurationFile)) {
      properties.store(output, null);
    }
  }

  /**
   * Reads all properties from the givcen properties file if it exists
   * @param propertiesFile the properties file to read from
   * @return the properties read from the given file
   * @throws IOException in case the file exists but can not be read
   */
  public static Properties readFromFile(final String propertiesFile) throws IOException {
    final Properties propertiesFromFile = new Properties();
    final File file = new File(Objects.requireNonNull(propertiesFile, "propertiesFile"));
    if (file.exists()) {
      LOG.debug("Reading configuration from file: {}", propertiesFile);
      try (final InputStream input = new FileInputStream(file)) {
        propertiesFromFile.load(input);
      }
    }

    return propertiesFromFile;
  }

  private final class PropertyValue<T> implements Value<T> {

    private final Event<T> changeEvent = Events.event();
    private final String property;
    private final Function<T, String> encoder;

    private T value;

    private PropertyValue(final String property, final T defaultValue,
                          final Function<String, T> decoder, final Function<T, String> encoder) {
      this.property = property;
      Objects.requireNonNull(decoder, "decoder");
      this.encoder = Objects.requireNonNull(encoder, "encoder");
      final String initialValue = getInitialValue(property);
      set(initialValue == null ? defaultValue : decoder.apply(initialValue));
    }

    @Override
    public void set(final T value) {
      if (!Objects.equals(this.value, value)) {
        this.value = value;
        if (value == null) {
          LOG.debug("Property value removed {}", property);
          properties.remove(property);
          System.clearProperty(property);
        }
        else {
          LOG.debug("Property value set {} -> {}", property, value);
          properties.setProperty(property, encoder.apply(value));
          System.setProperty(property, properties.getProperty(property));
        }
        changeEvent.fire(this.value);
      }
    }

    @Override
    public ValueObserver<T> getValueObserver() {
      return Values.valueObserver(this);
    }

    @Override
    public T get() {
      return value;
    }

    @Override
    public boolean isNullable() {
      return true;
    }

    @Override
    public EventObserver<T> getChangeObserver() {
      return changeEvent.getObserver();
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
