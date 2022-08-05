/*
 * Copyright (c) 2017 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import is.codion.common.properties.PropertyStore;
import is.codion.common.properties.PropertyValue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Collections.emptyList;

/**
 * A utility class for central configuration values.
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

  private static final String CLASSPATH_PREFIX = "classpath:";

  private static final PropertyStore STORE;

  static {
    STORE = loadConfiguration();
  }

  private Configuration() {}

  /**
   * Creates a boolean configuration value
   * @param key the configuration value key
   * @return a configuration value builder
   */
  public static PropertyValue<Boolean> booleanValue(String key) {
    return STORE.booleanValue(key);
  }

  /**
   * Creates a boolean configuration value
   * @param key the configuration value key
   * @param defaultValue the default value
   * @return a configuration value builder
   */
  public static PropertyValue<Boolean> booleanValue(String key, boolean defaultValue) {
    return STORE.booleanValue(key, defaultValue);
  }

  /**
   * Creates an integer configuration value
   * @param key the configuration value key
   * @return a configuration value builder
   */
  public static PropertyValue<Integer> integerValue(String key) {
    return STORE.integerValue(key);
  }

  /**
   * Creates an integer configuration value
   * @param key the configuration value key
   * @param defaultValue the default value
   * @return a configuration value builder
   */
  public static PropertyValue<Integer> integerValue(String key, int defaultValue) {
    return STORE.integerValue(key, defaultValue);
  }

  /**
   * Creates a long configuration value
   * @param key the configuration value key
   * @return a configuration value builder
   */
  public static PropertyValue<Long> longValue(String key) {
    return STORE.longValue(key);
  }

  /**
   * Creates a long configuration value
   * @param key the configuration value key
   * @param defaultValue the default value
   * @return a configuration value builder
   */
  public static PropertyValue<Long> longValue(String key, long defaultValue) {
    return STORE.longValue(key, defaultValue);
  }

  /**
   * Creates a double configuration value
   * @param key the configuration value key
   * @return a configuration value builder
   */
  public static PropertyValue<Double> doubleValue(String key) {
    return STORE.doubleValue(key);
  }

  /**
   * Creates a double configuration value
   * @param key the configuration value key
   * @param defaultValue the default value
   * @return a configuration value builder
   */
  public static PropertyValue<Double> doubleValue(String key, double defaultValue) {
    return STORE.doubleValue(key, defaultValue);
  }

  /**
   * Creates a character configuration value
   * @param key the configuration value key
   * @return a configuration value builder
   */
  public static PropertyValue<Character> characterValue(String key) {
    return STORE.characterValue(key);
  }

  /**
   * Creates a character configuration value
   * @param key the configuration value key
   * @param defaultValue the default value
   * @return a configuration value builder
   */
  public static PropertyValue<Character> characterValue(String key, char defaultValue) {
    return STORE.characterValue(key, defaultValue);
  }

  /**
   * Creates a string configuration value
   * @param key the configuration value key
   * @return a configuration value builder
   */
  public static PropertyValue<String> stringValue(String key) {
    return stringValue(key, null);
  }

  /**
   * Creates a string configuration value
   * @param key the configuration value key
   * @param defaultValue the default value
   * @return a configuration value builder
   */
  public static PropertyValue<String> stringValue(String key, String defaultValue) {
    return STORE.stringValue(key, defaultValue);
  }

  /**
   * Creates an enum configuration value
   * @param key the configuration value key
   * @param enumClass the enum class
   * @param <T> the enum type
   * @return a configuration value builder
   */
  public static <T extends Enum<T>> PropertyValue<T> enumValue(String key, Class<T> enumClass) {
    return enumValue(key, enumClass, null);
  }

  /**
   * Creates an enum configuration value
   * @param key the configuration value key
   * @param enumClass the enum class
   * @param defaultValue the default value
   * @param <T> the enum type
   * @return a configuration value builder
   */
  public static <T extends Enum<T>> PropertyValue<T> enumValue(String key, Class<T> enumClass, T defaultValue) {
    return STORE.enumValue(key, enumClass, defaultValue);
  }

  /**
   * Creates a list configuration value
   * @param key the configuration value key
   * @param parser the parser used to parse a string representation of the value
   * @param <T> the value type
   * @return a configuration value builder
   */
  public static <T> PropertyValue<List<T>> listValue(String key, Function<String, T> parser) {
    return listValue(key, parser, emptyList());
  }

  /**
   * Creates a list configuration value
   * @param key the configuration value key
   * @param parser the parser used to parse a string representation of the value
   * @param defaultValue the default value
   * @param <T> the value type
   * @return a configuration value builder
   */
  public static <T> PropertyValue<List<T>> listValue(String key, Function<String, T> parser, List<T> defaultValue) {
    return STORE.listValue(key, parser, Objects::toString, defaultValue);
  }

  /**
   * Creates a configuration value
   * @param key the configuration value key
   * @param parser the parser used to parse a string representation of the value
   * @param <T> the value type
   * @return a configuration value builder
   */
  public static <T> PropertyValue<T> value(String key, Function<String, T> parser) {
    return value(key, parser, null);
  }

  /**
   * Creates a configuration value
   * @param key the configuration value key
   * @param parser the parser used to parse a string representation of the value
   * @param defaultValue the default value
   * @param <T> the value type
   * @return a configuration value builder
   */
  public static <T> PropertyValue<T> value(String key, Function<String, T> parser, T defaultValue) {
    return STORE.value(key, parser, Objects::toString, defaultValue);
  }

  private static PropertyStore loadConfiguration() {
    boolean configurationFileRequired = System.getProperty(CONFIGURATION_FILE_REQUIRED, "false").equalsIgnoreCase(Boolean.TRUE.toString());
    String configurationFilePath = System.getProperty(CONFIGURATION_FILE,
            System.getProperty("user.home") + Separators.FILE_SEPARATOR + "codion.config");
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
