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
 * Copyright (c) 2016 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.condition.ConditionModel;

import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

public class ColumnConditionPanelTest {

	@Test
	void test() {
		ConditionModel<String> model = ConditionModel.builder()
						.valueClass(String.class)
						.build();
		ColumnConditionPanel<String> panel = ColumnConditionPanel.builder()
						.model(model)
						.build();
		assertSame(model, panel.model());
		assertNotNull(panel.operands().equal());
		assertNotNull(panel.operands().upper());
		assertNotNull(panel.operands().lower());
		assertThrows(NullPointerException.class, () -> ColumnConditionPanel.<String>builder().model(null));
	}

	@Test
	void componentNames() {
		ConditionModel<String> model = ConditionModel.builder()
						.valueClass(String.class)
						.build();
		ColumnConditionPanel<String> panel = ColumnConditionPanel.builder()
						.model(model)
						.name("test.column")
						.build();
		panel.operands().equal();//triggers lazy component creation

		//each input component is named by its role, so a tool driving the UI, such as the Swing MCP server,
		//identifies the focused condition field and its column via Component.getName()
		Set<String> names = panel.components().stream()
						.map(Component::getName)
						.filter(Objects::nonNull)
						.collect(toSet());
		assertTrue(names.contains("test.column:operator"), names::toString);
		assertTrue(names.contains("test.column:equal"), names::toString);
		assertTrue(names.stream().allMatch(name -> name.startsWith("test.column:")), names::toString);
	}

	@Test
	void unnamedByDefault() {
		ConditionModel<String> model = ConditionModel.builder()
						.valueClass(String.class)
						.build();
		ColumnConditionPanel<String> panel = ColumnConditionPanel.builder()
						.model(model)
						.build();
		//no name provided, the components keep their default null name
		assertTrue(panel.operands().equal().isPresent());
		assertNull(panel.operands().equal().get().getName());
	}

	@Test
	void lockedModel() {
		ConditionModel<String> model = ConditionModel.builder()
						.valueClass(String.class)
						.build();
		model.locked().set(true);
		ColumnConditionPanel.builder().model(model).build();
	}
}
