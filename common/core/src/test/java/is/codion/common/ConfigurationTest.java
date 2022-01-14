/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import is.codion.common.properties.PropertyStore;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ConfigurationTest {

  @Test
  void classpath() throws IOException {
    PropertyStore store = Configuration.loadPropertiesFromClasspath("classpath:/config_test.config", false);
    assertTrue(store.containsProperty("test.property"));
    assertTrue(store.containsProperty("test.property2"));

    store = Configuration.loadPropertiesFromClasspath("classpath:/is/codion/common/item/item_config_test.config", false);
    assertTrue(store.containsProperty("item.property"));
    assertTrue(store.containsProperty("item.property2"));

    assertThrows(FileNotFoundException.class, () ->
            Configuration.loadPropertiesFromClasspath("classpath:/is/codion/common/item/item_config_test_non_existing.config", true));
    assertThrows(FileNotFoundException.class, () ->
            Configuration.loadPropertiesFromClasspath("classpath:config_test_non_existing.config", true));
  }

  @Test
  void filePath() throws IOException {
    PropertyStore store = Configuration.loadPropertiesFromFile("src/test/resources/config_test.config", false);
    assertTrue(store.containsProperty("test.property"));
    assertTrue(store.containsProperty("test.property2"));

    store = Configuration.loadPropertiesFromFile("src/test/resources/is/codion/common/item/item_config_test.config", false);
    assertTrue(store.containsProperty("item.property"));
    assertTrue(store.containsProperty("item.property2"));

    assertThrows(FileNotFoundException.class, () ->
            Configuration.loadPropertiesFromFile("src/test/resources/is/codion/common/item/item_config_test_non_existing.config", true));
    assertThrows(FileNotFoundException.class, () ->
            Configuration.loadPropertiesFromFile("src/test/resources/config_test_non_existing.config", true));
  }
}
