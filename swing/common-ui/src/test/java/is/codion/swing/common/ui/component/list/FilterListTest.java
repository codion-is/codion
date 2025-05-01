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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.common.Text;
import is.codion.swing.common.model.component.list.FilterListModel;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class FilterListTest {

	private static final String ONE = "One";
	private static final String TWO = "Two";
	private static final String THREE = "Three";

	@Test
	void test() {
		List<String> items = asList(ONE, TWO, THREE);
		FilterListModel<String> model = FilterListModel.builder(items)
						.comparator(Text.collator())
						.build();
		model.selection().item().set(TWO);
		FilterList<String> list = ListBuilder.factory(model).items().build();
		assertEquals(TWO, list.getSelectedValue());
		assertEquals(1, list.model().items().visible().indexOf(THREE));
		assertThrows(IllegalStateException.class, () -> list.setSelectionModel(new DefaultListSelectionModel()));
		assertThrows(IllegalStateException.class, () -> list.setModel(new DefaultListModel<>()));
	}
}
