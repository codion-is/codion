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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import java.util.List;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SelectedValuesTest {

	@Test
	void selectedItemValueLink() {
		Value<String> value = Value.nullable();
		ComponentValue<String, JComboBox<String>> componentValue = Components.comboBox(new DefaultComboBoxModel<>(new String[] {"b", "d", "s"}),
										value)
						.buildValue();
		JComboBox<String> box = componentValue.component();

		assertNull(value.get());
		value.set("s");
		assertEquals("s", box.getSelectedItem());
		box.setSelectedItem("d");
		assertEquals("d", value.get());
	}

	@Test
	void selectedItemValue() {
		List<Item<String>> items = asList(item(null), item("one"),
						item("two"), item("three"), item("four"));
		ComponentValue<String, JComboBox<Item<String>>> componentValue = Components.itemComboBox(items)
						.value("two")
						.buildValue();
		assertEquals(5, componentValue.component().getModel().getSize());
		assertEquals("two", componentValue.get());

		componentValue = Components.itemComboBox(items)
						.buildValue();
		assertEquals(5, componentValue.component().getModel().getSize());
		assertNull(componentValue.get());
	}

	@Test
	void selectedValue() {
		ComponentValue<String, JComboBox<String>> value = Components.comboBox(new DefaultComboBoxModel<>(new String[] {null, "one", "two", "three"}))
						.buildValue();
		JComboBox<String> box = value.component();

		assertNull(value.get());
		box.setSelectedIndex(1);
		assertEquals("one", box.getSelectedItem());
		box.setSelectedIndex(2);
		assertEquals("two", box.getSelectedItem());
		box.setSelectedIndex(3);
		assertEquals("three", box.getSelectedItem());
		box.setSelectedIndex(0);
		assertNull(value.get());

		value.set("two");
		assertEquals("two", box.getSelectedItem());
	}
}
