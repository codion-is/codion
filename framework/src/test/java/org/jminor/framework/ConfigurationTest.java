/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public final class ConfigurationTest {

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
