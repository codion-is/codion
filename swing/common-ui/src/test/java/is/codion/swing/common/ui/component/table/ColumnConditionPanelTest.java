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
 * Copyright (c) 2016 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.condition.ConditionModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnConditionPanelTest {

	@Test
	void test() {
		ConditionModel<String> model = ConditionModel.builder()
						.valueClass(String.class)
						.build();
		ColumnConditionPanel<String> panel = ColumnConditionPanel.builder(model).build();
		assertSame(model, panel.model());
		assertNotNull(panel.operands().equal());
		assertNotNull(panel.operands().upper());
		assertNotNull(panel.operands().lower());
		assertThrows(NullPointerException.class, () -> ColumnConditionPanel.<String>builder(null));
	}

	@Test
	void lockedModel() {
		ConditionModel<String> model = ConditionModel.builder()
						.valueClass(String.class)
						.build();
		model.locked().set(true);
		ColumnConditionPanel.builder(model).build();
	}
}
