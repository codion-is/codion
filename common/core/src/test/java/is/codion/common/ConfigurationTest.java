/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
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
