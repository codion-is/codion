/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import is.codion.common.properties.PropertyStore;
import is.codion.common.value.PropertyValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

/**
 * A utility class for configuration values.
 * Parses a property file on class load, specified by the {@link #CONFIGURATION_FILE} system property.
 * @see #CONFIGURATION_FILE_REQUIRED
 */
public final class Configuration {

  /**
   * Specifies the main configuration file.<br>
   * Prefix with 'classpath:' to indicate that the configuration file is on the classpath.
   * Value type: String<br>
   * Default value: null
   */
  public static final String CONFIGURATION_FILE = "codion.configurationFile";

  /**
   * Specifies whether the application requires a configuration file to run.<br>
   * If this is set to true and the file referenced by {@link #CONFIGURATION_FILE}<br>
   * is not found a FileNotFoundException is thrown when this class is loaded.<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String CONFIGURATION_FILE_REQUIRED = "codion.configurationFileRequired";

  private static final PropertyStore STORE;

  private static final String CLASSPATH_PREFIX = "classpath:";

  static {
    final String configurationFilePath = System.getProperty(CONFIGURATION_FILE, System.getProperty("user.home") + Util.FILE_SEPARATOR + "codion.config");
    final boolean configurationFileRequired = System.getProperty(CONFIGURATION_FILE_REQUIRED, "false").equalsIgnoreCase(Boolean.TRUE.toString());
    try {
      if (configurationFilePath.toLowerCase().startsWith(CLASSPATH_PREFIX)) {
        STORE = loadPropertiesFromClasspath(configurationFilePath, configurationFileRequired);
      }
      else {
        STORE = loadPropertiesFromFile(configurationFilePath, configurationFileRequired);
      }
    }
    catch (final IOException e) {
      throw new RuntimeException("Unable to read configuration file: " + configurationFilePath, e);
    }
  }

  private Configuration() {}

  /**
   * Creates a boolean configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static PropertyValue<Boolean> booleanValue(final String key, final Boolean defaultValue) {
    return value(key, defaultValue, value -> value.equalsIgnoreCase(Boolean.TRUE.toString()));
  }

  /**
   * Creates an integer configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static PropertyValue<Integer> integerValue(final String key, final Integer defaultValue) {
    return value(key, defaultValue, Integer::parseInt);
  }

  /**
   * Creates a long configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static PropertyValue<Long> longValue(final String key, final Long defaultValue) {
    return value(key, defaultValue, Long::parseLong);
  }

  /**
   * Creates a double configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static PropertyValue<Double> doubleValue(final String key, final Double defaultValue) {
    return value(key, defaultValue, Double::parseDouble);
  }

  /**
   * Creates a string configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static PropertyValue<String> stringValue(final String key, final String defaultValue) {
    return value(key, defaultValue, value -> value);
  }

  /**
   * Creates an enum configuration value
   * @param key the configuration key
   * @param enumClass the enum class
   * @param defaultValue the default value, if any
   * @param <T> the enum type
   * @return the configuration value
   */
  public static <T extends Enum<T>> PropertyValue<T> enumValue(final String key, final Class<T> enumClass, final T defaultValue) {
    return value(key, defaultValue, value -> Enum.valueOf(enumClass, value.toUpperCase()));
  }

  /**
   * Creates a configuration value
   * @param key the configuration key
   * @param defaultValue the default value
   * @param parser the parser used to parse a string representation of the value
   * @param <T> the value type
   * @return the configuration value
   */
  public static <T> PropertyValue<T> value(final String key, final T defaultValue, final Function<String, T> parser) {
    return STORE.propertyValue(key, defaultValue, null, parser, Objects::toString);
  }

  static PropertyStore loadPropertiesFromClasspath(final String configurationFilePath, final boolean configurationFileRequired) throws IOException {
    final String filepath = configurationFilePath.substring(CLASSPATH_PREFIX.length());
    try (final InputStream configurationFileStream = Configuration.class.getResourceAsStream(filepath)) {
      if (configurationFileStream == null && configurationFileRequired) {
        throw new FileNotFoundException(configurationFilePath);
      }

      return PropertyStore.propertyStore(configurationFileStream);
    }
  }

  static PropertyStore loadPropertiesFromFile(final String configurationFilePath, final boolean configurationFileRequired) throws IOException {
    final File file = new File(configurationFilePath);
    if (configurationFileRequired && !file.exists()) {
      throw new FileNotFoundException(configurationFilePath);
    }

    return PropertyStore.propertyStore(file);
  }
}
