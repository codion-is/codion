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
import static is.codion.swing.common.model.component.combobox.ItemComboBoxModel.booleanItems;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ItemComboBoxModelTest {

	@Test
	void test() {
		Item<Integer> nullItem = item(null, "");
		Item<Integer> aOne = item(1, "AOne");
		Item<Integer> bTwo = item(2, "BTwo");
		Item<Integer> cThree = item(3, "CThree");
		Item<Integer> dFour = item(4, "DFour");

		List<Item<Integer>> items = asList(nullItem, cThree, bTwo, aOne, dFour);
		FilterComboBoxModel<Item<Integer>> model = ItemComboBoxModel.builder(items)
						.sorted(true)
						.build();

		assertEquals(0, model.items().visible().indexOf(nullItem));
		assertEquals(1, model.items().visible().indexOf(aOne));
		assertEquals(2, model.items().visible().indexOf(bTwo));
		assertEquals(3, model.items().visible().indexOf(cThree));
		assertEquals(4, model.items().visible().indexOf(dFour));

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

		assertEquals(0, model.items().visible().indexOf(nullItem));
		assertEquals(1, model.items().visible().indexOf(aOne));
		assertEquals(2, model.items().visible().indexOf(bTwo));
		assertEquals(3, model.items().visible().indexOf(cThree));
		assertEquals(4, model.items().visible().indexOf(dFour));

		//test unsorted final List<Item<Integer>> items = asList(nullItem, cThree, bTwo, aOne, dFour);
		FilterComboBoxModel<Item<Integer>> unsortedModel = ItemComboBoxModel.builder(items).build();

		assertEquals(0, unsortedModel.items().visible().indexOf(nullItem));
		assertEquals(1, unsortedModel.items().visible().indexOf(cThree));
		assertEquals(2, unsortedModel.items().visible().indexOf(bTwo));
		assertEquals(3, unsortedModel.items().visible().indexOf(aOne));
		assertEquals(4, unsortedModel.items().visible().indexOf(dFour));
	}

	@Test
	void booleanComboBoxModel() {
		FilterComboBoxModel<Item<Boolean>> model = ItemComboBoxModel.builder(booleanItems()).build();

		model.setSelectedItem(false);
		assertEquals(false, model.getSelectedItem().value());
		model.setSelectedItem(true);
		assertEquals(true, model.getSelectedItem().value());
		model.setSelectedItem(null);
		assertNull(model.getSelectedItem().value());
	}
}
