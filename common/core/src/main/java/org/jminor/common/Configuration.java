/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.jminor.common.value.PropertyValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
   * Value type: String<br>
   * Default value: null
   */
  public static final String CONFIGURATION_FILE = "jminor.configurationFile";

  /**
   * Specifies whether or not the application requires configuration file to run.<br>
   * If this is set to true and the file referenced by {@link #CONFIGURATION_FILE}<br>
   * is not found an exception is thrown.<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String CONFIGURATION_FILE_REQUIRED = "jminor.configurationFileRequired";

  private static final PropertyStore STORE;

  static {
    final String configurationFile = System.getProperty(CONFIGURATION_FILE, System.getProperty("user.home") + "/jminor.config");
    try {
      final File file = new File(configurationFile);
      final boolean configurationFileRequired =
              System.getProperty(CONFIGURATION_FILE_REQUIRED, "false").equalsIgnoreCase(Boolean.TRUE.toString());
      if (configurationFileRequired && !file.exists()) {
        throw new FileNotFoundException(configurationFile);
      }
      STORE = new PropertyStore(file);
    }
    catch (final IOException e) {
      throw new RuntimeException("Unable to read configuration file: " + configurationFile, e);
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
   * Creates a integer configuration value
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
   * Creates a configuration value
   * @param key the configuration key
   * @param defaultValue the default value
   * @param parser the parser used to parse a string representation of the value
   * @param <T> the value type
   * @return the configuration value
   */
  public static <T> PropertyValue<T> value(final String key, final T defaultValue, final Function<String, T> parser) {
    return STORE.propertyValue(key, defaultValue, parser, Objects::toString);
  }
}
