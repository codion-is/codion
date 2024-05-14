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
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.table.ColumnConditionModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FilterColumnConditionPanelTest {

	@Test
	void test() {
		final String key = "key";
		ColumnConditionModel<String, String> model = ColumnConditionModel.builder(key, String.class).build();
		FilterColumnConditionPanel<?, String> panel = FilterColumnConditionPanel.filterColumnConditionPanel(model, "test");
		assertEquals(model, panel.conditionModel());
		assertNotNull(panel.equalField());
		assertNotNull(panel.upperBoundField());
		assertNotNull(panel.lowerBoundField());
		assertThrows(NullPointerException.class, () -> FilterColumnConditionPanel.<String, String>filterColumnConditionPanel(null, null));
	}

	@Test
	void lockedModel() {
		ColumnConditionModel<String, String> model = ColumnConditionModel.builder("key", String.class).build();
		model.locked().set(true);
		FilterColumnConditionPanel.filterColumnConditionPanel(model, "Test");
	}
}
