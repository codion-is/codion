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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.FilterTableModel;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel.filterTableColumnComponentPanel;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class FilterTableColumnComponentPanelTest {

	private final FilterTableColumn<Integer> column0 = FilterTableColumn.filterTableColumn(0);
	private final FilterTableColumn<Integer> column1 = FilterTableColumn.filterTableColumn(1);
	private final FilterTableColumn<Integer> column2 = FilterTableColumn.filterTableColumn(2);

	private final FilterTableModel.TableColumns<Object, Integer> columns = new FilterTableModel.TableColumns<>() {
		@Override
		public List<Integer> identifiers() {
			return asList(0, 1, 2);
		}

		@Override
		public Class<?> columnClass(Integer integer) {
			return Object.class;
		}

		@Override
		public Object value(Object row, Integer integer) {
			return null;
		}
	};

	@Test
	void wrongColumn() {
		FilterTableModel<Object, Integer> tableModel =
						FilterTableModel.builder(columns).build();
		FilterTable<Object, Integer> table =
						FilterTable.builder(tableModel)
										.columns(asList(column0, column1, column2))
										.build();
		FilterTableColumnModel<Integer> columnModel = table.columnModel();
		Map<Integer, JPanel> columnComponents = createColumnComponents(columnModel);
		columnComponents.put(3, new JPanel());
		assertThrows(IllegalArgumentException.class, () -> filterTableColumnComponentPanel(columnModel, columnComponents));
	}

	@Test
	void setColumnVisible() {
		FilterTableModel<Object, Integer> tableModel =
						FilterTableModel.builder(columns).build();
		FilterTable<Object, Integer> table =
						FilterTable.builder(tableModel)
										.columns(asList(column0, column1, column2))
										.build();
		FilterTableColumnModel<Integer> columnModel = table.columnModel();

		columnModel.visible(1).set(false);

		FilterTableColumnComponentPanel<Integer> panel = filterTableColumnComponentPanel(columnModel, createColumnComponents(columnModel));
		assertTrue(panel.components().containsKey(1));

		assertNull(panel.components().get(1).getParent());
		columnModel.visible(1).set(true);
		assertNotNull(panel.components().get(1).getParent());
		columnModel.visible(2).set(false);
		assertNull(panel.components().get(2).getParent());
		columnModel.visible(2).set(true);
		assertNotNull(panel.components().get(2).getParent());
	}

	@Test
	void width() {
		FilterTableModel<Object, Integer> tableModel =
						FilterTableModel.builder(columns).build();
		FilterTable<Object, Integer> table =
						FilterTable.builder(tableModel)
										.columns(asList(column0, column1, column2))
										.build();
		FilterTableColumnModel<Integer> columnModel = table.columnModel();

		FilterTableColumnComponentPanel<Integer> panel = filterTableColumnComponentPanel(columnModel, createColumnComponents(columnModel));
		column0.setWidth(100);
		assertEquals(100, panel.components().get(0).getPreferredSize().width);
		column1.setWidth(90);
		assertEquals(90, panel.components().get(1).getPreferredSize().width);
		column2.setWidth(80);
		assertEquals(80, panel.components().get(2).getPreferredSize().width);
	}

	private static Map<Integer, JPanel> createColumnComponents(FilterTableColumnModel<Integer> columnModel) {
		return columnModel.identifiers().stream()
						.collect(Collectors.toMap(Function.identity(), identifier -> new JPanel()));
	}
}
