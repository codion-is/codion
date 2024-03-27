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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.item;

import is.codion.common.Serializer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;

import static org.junit.jupiter.api.Assertions.*;

public class ItemsTest {

	@Test
	void item() throws IOException, ClassNotFoundException {
		Item<String> item = Item.item("hello", "world");
		assertEquals("hello", item.get());
		assertEquals("world", item.caption());
		assertEquals("world", item.toString());

		Item<String> newItem = Item.item("hello", "bla");
		assertEquals(item, newItem);
		assertEquals("hello".hashCode(), item.hashCode());
		assertEquals(1, item.compareTo(newItem));

		Item<String> thirdItem = Item.item("hello");
		assertEquals("hello".hashCode(), thirdItem.hashCode());
		assertEquals("hello", thirdItem.caption());

		assertEquals(newItem, Item.item("hello"));
		assertNotEquals(newItem, "hello");

		assertEquals(0, Item.item(null).hashCode());

		//just make sure it's ok post serialization
		Item<String> deser = Serializer.deserialize(Serializer.serialize(item));
		deser.compareTo(item);
	}

	@Test
	void itemI18n() {
		assertThrows(NullPointerException.class, () -> Item.itemI18n("value", null, "item"));
		assertThrows(NullPointerException.class, () -> Item.itemI18n("value", ItemsTest.class.getName(), null));

		Item<String> item = Item.itemI18n("value", ItemsTest.class.getName(), "item");
		Locale.setDefault(new Locale("is", "IS"));
		assertEquals("Gildi", item.caption());

		item = Item.itemI18n("value", ItemsTest.class.getName(), "item");
		Locale.setDefault(new Locale("en", "EN"));
		assertEquals("Item", item.caption());
		//repeat for some coverage
		assertEquals("Item", item.caption());

		assertThrows(MissingResourceException.class, () -> Item.itemI18n("value", ItemsTest.class.getName(), "nonexisting"));
		assertThrows(MissingResourceException.class, () -> Item.itemI18n("value", String.class.getName(), "item"));
	}
}
