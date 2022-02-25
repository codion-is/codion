/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import is.codion.common.properties.PropertyStore;
import is.codion.common.value.PropertyValue;

import java.io.File;
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
    STORE = loadConfiguration();
  }

  private Configuration() {}

  /**
   * Creates a boolean configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static PropertyValue<Boolean> booleanValue(String key, Boolean defaultValue) {
    return value(key, defaultValue, value -> value.equalsIgnoreCase(Boolean.TRUE.toString()));
  }

  /**
   * Creates an integer configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static PropertyValue<Integer> integerValue(String key, Integer defaultValue) {
    return value(key, defaultValue, Integer::parseInt);
  }

  /**
   * Creates a long configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static PropertyValue<Long> longValue(String key, Long defaultValue) {
    return value(key, defaultValue, Long::parseLong);
  }

  /**
   * Creates a double configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static PropertyValue<Double> doubleValue(String key, Double defaultValue) {
    return value(key, defaultValue, Double::parseDouble);
  }

  /**
   * Creates a string configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static PropertyValue<String> stringValue(String key, String defaultValue) {
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
  public static <T extends Enum<T>> PropertyValue<T> enumValue(String key, Class<T> enumClass, T defaultValue) {
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
  public static <T> PropertyValue<T> value(String key, T defaultValue, Function<String, T> parser) {
    return STORE.propertyValue(key, defaultValue, null, parser, Objects::toString);
  }

  private static PropertyStore loadConfiguration() {
    boolean configurationFileRequired = System.getProperty(CONFIGURATION_FILE_REQUIRED, "false").equalsIgnoreCase(Boolean.TRUE.toString());
    String configurationFilePath = System.getProperty(CONFIGURATION_FILE, System.getProperty("user.home") + Util.FILE_SEPARATOR + "codion.config");
    if (configurationFilePath.toLowerCase().startsWith(CLASSPATH_PREFIX)) {
      return loadFromClasspath(configurationFilePath, configurationFileRequired);
    }

    return loadFromFile(configurationFilePath, configurationFileRequired);
  }

  static PropertyStore loadFromClasspath(String filePath, boolean configurationRequired) {
    String filepath = getClasspathFilepath(filePath);
    try (InputStream configurationFileStream = Configuration.class.getClassLoader().getResourceAsStream(filepath)) {
      if (configurationFileStream == null) {
        if (configurationRequired) {
          throw new RuntimeException("Required configuration file not found on classpath: " + filePath);
        }

        return PropertyStore.propertyStore();
      }

      return PropertyStore.propertyStore(configurationFileStream);
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to load configuration from classpath: " + filePath, e);
    }
  }

  static PropertyStore loadFromFile(String filePath, boolean configurationRequired) {
    try {
      File file = new File(filePath);
      if (!file.exists()) {
        if (configurationRequired) {
          throw new RuntimeException("Required configuration file not found: " + filePath);
        }

        return PropertyStore.propertyStore();
      }

      return PropertyStore.propertyStore(file);
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to load configuration from file: " + filePath);
    }
  }

  private static String getClasspathFilepath(String filePath) {
    String path = filePath.substring(CLASSPATH_PREFIX.length());
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    if (path.contains("/")) {
      throw new IllegalArgumentException("Configuration files must be in the classpath root");
    }

    return path;
  }
}
