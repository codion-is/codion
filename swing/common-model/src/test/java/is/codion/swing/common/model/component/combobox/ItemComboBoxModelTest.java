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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.item.Item;

import org.junit.jupiter.api.Test;

import java.util.List;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ItemComboBoxModelTest {

	@Test
	void test() {
		ItemComboBoxModel.itemComboBoxModel();
		Item<Integer> nullItem = item(null, "");
		Item<Integer> aOne = item(1, "AOne");
		Item<Integer> bTwo = item(2, "BTwo");
		Item<Integer> cThree = item(3, "CThree");
		Item<Integer> dFour = item(4, "DFour");

		List<Item<Integer>> items = asList(nullItem, cThree, bTwo, aOne, dFour);
		ItemComboBoxModel<Integer> model = ItemComboBoxModel.sortedItemComboBoxModel(items);

		assertEquals(0, model.indexOf(null));
		assertEquals(1, model.indexOf(1));
		assertEquals(2, model.indexOf(2));
		assertEquals(3, model.indexOf(3));
		assertEquals(4, model.indexOf(4));

		model.setSelectedItem(1);
		assertEquals(model.getSelectedItem(), aOne);
		assertEquals(1, (int) model.selection().value().value());
		assertEquals("AOne", model.getSelectedItem().toString());
		model.setSelectedItem(2);
		assertEquals(2, (int) model.selection().value().value());
		assertEquals(model.getSelectedItem(), bTwo);
		model.setSelectedItem(4);
		assertEquals(4, (int) model.selection().value().value());
		assertEquals(model.getSelectedItem(), dFour);
		model.setSelectedItem(null);
		assertNull(model.selection().value().value());
		assertEquals(model.getSelectedItem(), nullItem);

		model.refresh();

		assertEquals(0, model.indexOf(null));
		assertEquals(1, model.indexOf(1));
		assertEquals(2, model.indexOf(2));
		assertEquals(3, model.indexOf(3));
		assertEquals(4, model.indexOf(4));

		//test unsorted final List<Item<Integer>> items = asList(nullItem, cThree, bTwo, aOne, dFour);
		ItemComboBoxModel<Integer> unsortedModel = ItemComboBoxModel.itemComboBoxModel(items);

		assertEquals(0, unsortedModel.indexOf(null));
		assertEquals(1, unsortedModel.indexOf(3));
		assertEquals(2, unsortedModel.indexOf(2));
		assertEquals(3, unsortedModel.indexOf(1));
		assertEquals(4, unsortedModel.indexOf(4));
	}

	@Test
	void booleanComboBoxModel() {
		ItemComboBoxModel<Boolean> model = ItemComboBoxModel.booleanItemComboBoxModel();

		model.setSelectedItem(false);
		assertEquals(false, model.getSelectedItem().value());
		model.setSelectedItem(true);
		assertEquals(true, model.getSelectedItem().value());
		model.setSelectedItem(null);
		assertNull(model.getSelectedItem().value());
	}
}
