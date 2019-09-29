/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

/**
 * A utility class for configuration values.
 * Parses a property file on class load, specified by the {@link #CONFIGURATION_FILE} system property.
 */
public final class Configuration {

  /**
   * Specifies the main configuration file.<br>
   * Value type: String<br>
   * Default value: null
   */
  public static final String CONFIGURATION_FILE = "jminor.configurationFile";

  private static final String DEFAULT_CONFIGURATION_FILE = System.getProperty("user.home") + "/jminor.config";

  private static final PropertyStore STORE;

  static {
    final String configurationFile = System.getProperty(CONFIGURATION_FILE, DEFAULT_CONFIGURATION_FILE);
    try {
      STORE = new PropertyStore(configurationFile);
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
  public static Value<Boolean> booleanValue(final String key, final Boolean defaultValue) {
    return value(key, defaultValue, value -> value.equalsIgnoreCase(Boolean.TRUE.toString()));
  }

  /**
   * Creates a integer configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static Value<Integer> integerValue(final String key, final Integer defaultValue) {
    return value(key, defaultValue, Integer::parseInt);
  }

  /**
   * Creates a long configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static Value<Long> longValue(final String key, final Long defaultValue) {
    return value(key, defaultValue, Long::parseLong);
  }

  /**
   * Creates a double configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static Value<Double> doubleValue(final String key, final Double defaultValue) {
    return value(key, defaultValue, Double::parseDouble);
  }

  /**
   * Creates a string configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static Value<String> stringValue(final String key, final String defaultValue) {
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
  public static <T> Value<T> value(final String key, final T defaultValue, final Function<String, T> parser) {
    return STORE.propertyValue(key, defaultValue, parser, Objects::toString);
  }
}
