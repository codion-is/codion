/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ConfigurationTest {

  @Test
  public void stringValue() {
    final String key = "configuration.test.string";
    final String value = "value";
    final String defaultValue = "default";
    final String setValue = "setValue";
    assertEquals(defaultValue, Configuration.stringValue(key, defaultValue).get());
    assertNull(Configuration.stringValue(key, null).get());
    System.setProperty(key, value);
    assertEquals(value, Configuration.stringValue(key, defaultValue).get());
    Configuration.stringValue(key, null).set(setValue);
    assertEquals(setValue, System.getProperty(key));
    Configuration.stringValue(key, null).set(null);
    assertNull(System.getProperty(key));
    assertEquals(key, Configuration.stringValue(key, null).toString());
  }

  @Test
  public void booleanValue() {
    final String key = "configuration.test.boolean";
    final Boolean value = true;
    final Boolean defaultValue = false;
    assertEquals(defaultValue, Configuration.booleanValue(key, defaultValue).get());
    assertNull(Configuration.booleanValue(key, null).get());
    System.setProperty(key, value.toString());
    assertEquals(value, Configuration.booleanValue(key, defaultValue).get());
  }

  @Test
  public void integerValue() {
    final String key = "configuration.test.integer";
    final Integer value = 1;
    final Integer defaultValue = 42;
    assertEquals(defaultValue, Configuration.integerValue(key, defaultValue).get());
    assertNull(Configuration.integerValue(key, null).get());
    System.setProperty(key, value.toString());
    assertEquals(value, Configuration.integerValue(key, defaultValue).get());
  }

  @Test
  public void longValue() {
    final String key = "configuration.test.long";
    final Long value = System.currentTimeMillis();
    final Long defaultValue = 42424242424242L;
    assertEquals(defaultValue, Configuration.longValue(key, defaultValue).get());
    assertNull(Configuration.longValue(key, null).get());
    System.setProperty(key, value.toString());
    assertEquals(value, Configuration.longValue(key, defaultValue).get());
  }

  @Test
  public void doubleValue() {
    final String key = "configuration.test.double";
    final Double value = 1.1;
    final Double defaultValue = 42.2;
    assertEquals(defaultValue, Configuration.doubleValue(key, defaultValue).get());
    assertNull(Configuration.doubleValue(key, null).get());
    System.setProperty(key, value.toString());
    assertEquals(value, Configuration.doubleValue(key, defaultValue).get());
  }

  @Test
  public void parseConfigurationFile() throws IOException {
    File mainConfigurationFile = null;
    File secondConfigurationFile = null;
    File thirdConfigurationFile = null;
    try {
      //Create three config files, the first references the second which references the third
      final File userDir = new File(System.getProperty("user.dir"));
      mainConfigurationFile = new File(userDir, "UtilTestMain.config");
      secondConfigurationFile = new File(userDir, "UtilTestSecond.config");
      thirdConfigurationFile = new File(userDir, "UtilTestThird.config");

      Properties properties = new Properties();
      properties.put("main.property", "value");
      properties.put(Configuration.ADDITIONAL_CONFIGURATION_FILES, "UtilTestSecond.config");
      try (final FileOutputStream outputStream = new FileOutputStream(mainConfigurationFile)) {
        properties.store(outputStream, "");
      }

      properties = new Properties();
      properties.put("second.property", "value");
      properties.put(Configuration.ADDITIONAL_CONFIGURATION_FILES, "UtilTestThird.config");
      try (final FileOutputStream outputStream = new FileOutputStream(secondConfigurationFile)) {
        properties.store(outputStream, "");
      }

      properties = new Properties();
      properties.put("third.property", "value");
      try (final FileOutputStream outputStream = new FileOutputStream(thirdConfigurationFile)) {
        properties.store(outputStream, "");
      }

      //done prep, now the test
      Configuration.parseConfigurationFile("UtilTestMain.config");
      assertEquals("value", System.getProperty("main.property"));
      assertEquals("value", System.getProperty("second.property"));
      assertEquals("value", System.getProperty("third.property"));
    }
    finally {
      try {
        if (mainConfigurationFile != null) {
          mainConfigurationFile.delete();
        }
      }
      catch (final Exception ignored) {/*ignored*/}
      try {
        if (secondConfigurationFile != null) {
          secondConfigurationFile.delete();
        }
      }
      catch (final Exception ignored) {/*ignored*/}
      try {
        if (thirdConfigurationFile != null) {
          thirdConfigurationFile.delete();
        }
      }
      catch (final Exception ignored) {/*ignored*/}
    }
  }
}
