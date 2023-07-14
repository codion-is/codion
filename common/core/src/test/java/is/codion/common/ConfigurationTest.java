/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import is.codion.common.property.PropertyStore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class ConfigurationTest {

  @Test
  void classpath() {
    PropertyStore store = Configuration.loadFromClasspath("classpath:config_test.config", false);
    assertTrue(store.containsProperty("test.property"));
    assertTrue(store.containsProperty("test.property2"));

    store = Configuration.loadFromClasspath("classpath:/config_test.config", false);
    assertTrue(store.containsProperty("test.property"));
    assertTrue(store.containsProperty("test.property2"));

    assertThrows(IllegalArgumentException.class, () ->
            Configuration.loadFromClasspath("classpath:is/codion/common/item/item_config_test_non_existing.config", true));
    assertThrows(IllegalArgumentException.class, () ->
            Configuration.loadFromClasspath("classpath:/is/codion/common/item/item_config_test_non_existing.config", true));
    assertThrows(RuntimeException.class, () ->
            Configuration.loadFromClasspath("classpath:config_test_non_existing.config", true));
  }

  @Test
  void filePath() {
    PropertyStore store = Configuration.loadFromFile("src/test/resources/config_test.config", false);
    assertTrue(store.containsProperty("test.property"));
    assertTrue(store.containsProperty("test.property2"));

    store = Configuration.loadFromFile("src/test/resources/is/codion/common/item/item_config_test.config", false);
    assertTrue(store.containsProperty("item.property"));
    assertTrue(store.containsProperty("item.property2"));

    store = Configuration.loadFromFile("src/test/resources/config_test_non_existing.config", false);
    assertFalse(store.containsProperty("test.property"));
    assertFalse(store.containsProperty("test.property2"));

    store = Configuration.loadFromFile("src/test/resources/is/codion/common/item/item_config_test_non_existing.config", false);
    assertFalse(store.containsProperty("item.property"));
    assertFalse(store.containsProperty("item.property2"));

    assertThrows(RuntimeException.class, () ->
            Configuration.loadFromFile("src/test/resources/is/codion/common/item/item_config_test_non_existing.config", true));
    assertThrows(RuntimeException.class, () ->
            Configuration.loadFromFile("src/test/resources/config_test_non_existing.config", true));
  }
}
