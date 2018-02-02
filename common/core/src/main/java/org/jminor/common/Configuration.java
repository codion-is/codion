/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * A utility class for configuration values.
 * Parses a property file on class load, specified by the {@link #CONFIGURATION_FILE} system property,
 * starting by looking for the file on the classpath then in the file system.
 */
public final class Configuration {

  private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

  /**
   * Specifies the main configuration file.<br>
   * Value type: String<br>
   * Default value: null
   * @see #parseConfigurationFile()
   */
  public static final String CONFIGURATION_FILE = "jminor.configurationFile";

  /**
   * Add a property with this name in the main configuration file and specify a comma separated list
   * of additional configuration files that should be parsed along with the main configuration file.<br>
   * Value type: String<br>
   * Default value: null
   * @see #parseConfigurationFile()
   */
  public static final String ADDITIONAL_CONFIGURATION_FILES = "jminor.additionalConfigurationFiles";

  static {
    parseConfigurationFile();
  }

  private Configuration() {}

  /**
   * Creates a boolean configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static Value<Boolean> booleanValue(final String key, final Boolean defaultValue) {
    return new BooleanValue(key, defaultValue);
  }

  /**
   * Creates a integer configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static Value<Integer> integerValue(final String key, final Integer defaultValue) {
    return new IntegerValue(key, defaultValue);
  }

  /**
   * Creates a long configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static Value<Long> longValue(final String key, final Long defaultValue) {
    return new LongValue(key, defaultValue);
  }

  /**
   * Creates a double configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static Value<Double> doubleValue(final String key, final Double defaultValue) {
    return new DoubleValue(key, defaultValue);
  }

  /**
   * Creates a string configuration value
   * @param key the configuration key
   * @param defaultValue the default value, if any
   * @return the configuration value
   */
  public static Value<String> stringValue(final String key, final String defaultValue) {
    return new StringValue(key, defaultValue);
  }

  /**
   * Parses the configuration file specified by the {@link #CONFIGURATION_FILE} property,
   * adding the resulting properties via System.setProperty(key, value).
   * Also parses any configuration files specified by {@link #ADDITIONAL_CONFIGURATION_FILES}.
   * @see #CONFIGURATION_FILE
   */
  public static void parseConfigurationFile() {
    parseConfigurationFile(System.getProperty(CONFIGURATION_FILE));
  }

  /**
   * Parses the given configuration file adding the resulting properties via System.setProperty(key, value).
   * If a file with the given name is not found on the classpath we try to locate it on the filesystem,
   * relative to user.dir, if the file is not found a RuntimeException is thrown.
   * If the {@link #ADDITIONAL_CONFIGURATION_FILES} property is found, the files specified are parsed as well,
   * note that the actual property value is not added to the system properties.
   * @param filename the configuration filename
   * @throws IllegalArgumentException in case the configuration file is not found
   * @see #CONFIGURATION_FILE
   */
  public static void parseConfigurationFile(final String filename) {
    if (filename == null) {
      return;
    }
    final Properties properties = new Properties();
    InputStream inputStream = null;
    String additionalConfigurationFiles = null;
    try {
      inputStream = ClassLoader.getSystemResourceAsStream(filename);
      if (inputStream == null) {//not on classpath
        final File configurationFile = new File(System.getProperty("user.dir") + File.separator + filename);
        if (!configurationFile.exists()) {
          throw new IllegalArgumentException("Configuration file not found on classpath (" + filename + ") or as a file (" + configurationFile.getPath() + ")");
        }
        inputStream = new FileInputStream(configurationFile);
        LOG.debug("Reading configuration file from filesystem: {}", filename);
      }
      else {
        LOG.debug("Reading configuration file from classpath: {}", filename);
      }

      properties.load(inputStream);
      for (final Map.Entry entry : properties.entrySet()) {
        final Object key = entry.getKey();
        final String value = (String) properties.get(key);
        LOG.debug("{} -> {}", key, value);
        if (ADDITIONAL_CONFIGURATION_FILES.equals(key)) {
          additionalConfigurationFiles = value;
        }
        else {
          System.setProperty((String) key, value);
        }
      }
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      Util.closeSilently(inputStream);
    }
    if (additionalConfigurationFiles != null) {
      final String[] configurationFiles = additionalConfigurationFiles.split(",");
      for (final String configurationFile : configurationFiles) {
        parseConfigurationFile(configurationFile.trim());
      }
    }
  }

  /**
   * A Value for configuration, setting the value also sets the System property
   * @param <T> the value type
   */
  public static abstract class ConfigurationValue<T> implements Value<T> {

    private final Event<T> changeEvent = Events.event();
    private final String key;
    private T value;

    /**
     * Creates a new configuration value. The initial value is parsed from system properties,
     * if no value is found in system properties the default value is used.
     * @param key the configuration key
     * @param defaultValue the default value
     */
    private ConfigurationValue(final String key, final T defaultValue) {
      this.key = Objects.requireNonNull(key, "key");
      final String stringValue = System.getProperty(key);
      this.value = stringValue == null ? defaultValue : parse(stringValue);
      LOG.debug(key + ": " + stringValue + " [default: " + defaultValue +"]");
    }

    @Override
    public final void set(final T value) {
      this.value = value;
      if (value == null) {
        System.clearProperty(key);
      }
      else {
        System.setProperty(key, value.toString());
      }
      changeEvent.fire(value);
    }

    @Override
    public final T get() {
      return value;
    }

    @Override
    public final EventObserver<T> getObserver() {
      return changeEvent.getObserver();
    }

    @Override
    public final String toString() {
      return key;
    }

    /**
     * Parses the configuration value from the given non-null string
     * @param value the string value, not null
     * @return the parsed value
     */
    protected abstract T parse(final String value);
  }

  private static final class StringValue extends ConfigurationValue<String> {

    private StringValue(final String key, final String defaultValue) {
      super(key, defaultValue);
    }

    @Override
    protected String parse(final String value) {
      return value;
    }
  }

  private static final class BooleanValue extends ConfigurationValue<Boolean> {

    private BooleanValue(final String key, final Boolean defaultValue) {
      super(key, defaultValue);
    }

    @Override
    protected Boolean parse(final String value) {
      return value.equalsIgnoreCase(Boolean.TRUE.toString());
    }
  }

  private static final class IntegerValue extends ConfigurationValue<Integer> {

    private IntegerValue(final String key, final Integer defaultValue) {
      super(key, defaultValue);
    }

    @Override
    protected Integer parse(final String value) {
      return Integer.parseInt(value);
    }
  }

  private static final class DoubleValue extends ConfigurationValue<Double> {

    private DoubleValue(final String key, final Double defaultValue) {
      super(key, defaultValue);
    }

    @Override
    protected Double parse(final String value) {
      return Double.parseDouble(value);
    }
  }

  private static final class LongValue extends ConfigurationValue<Long> {

    private LongValue(final String key, final Long defaultValue) {
      super(key, defaultValue);
    }

    @Override
    protected Long parse(final String value) {
      return Long.parseLong(value);
    }
  }
}
