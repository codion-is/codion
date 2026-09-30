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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.swing.common.model.component.table.SwingFilterTableModel;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.time.LocalDate;
import java.util.List;

import static is.codion.swing.common.ui.component.table.FilterTableCellRenderer.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DefaultFilterTableCellRendererTest {

	@Test
	void horizontalAlignment() {
		assertEquals(BOOLEAN_HORIZONTAL_ALIGNMENT.get(),
						FilterTableCellRenderer.builder()
										.type(Boolean.class)
										.build()
										.horizontalAlignment());
		assertEquals(TEMPORAL_HORIZONTAL_ALIGNMENT.get(),
						FilterTableCellRenderer.builder()
										.type(LocalDate.class)
										.build()
										.horizontalAlignment());
		assertEquals(NUMERICAL_HORIZONTAL_ALIGNMENT.get(),
						FilterTableCellRenderer.builder()
										.type(Double.class)
										.build()
										.horizontalAlignment());
	}

	@Test
	void selectedForeground() {
		SwingFilterTableModel<String, Integer> model = SwingFilterTableModel.<String, Integer>builder()
						.columns(new TableColumns<String, Integer>() {
							@Override
							public List<Integer> identifiers() {
								return singletonList(0);
							}

							@Override
							public Class<?> type(Integer identifier) {
								return String.class;
							}

							@Override
							public Object value(String row, Integer identifier) {
								return row;
							}
						})
						.items(() -> asList("a", "b"))
						.build();
		model.items().refresh();
		FilterTable<String, Integer> table = FilterTable.builder()
						.model(model)
						.cellRenderer(0, String.class, renderer -> renderer
										.foreground((filterTable, row, identifier, value) -> Color.RED))
						.build();
		table.setRowSelectionInterval(0, 0);
		// the selection foreground wins over the cell foreground
		assertEquals(table.getSelectionForeground(), foreground(table, 0));
		assertEquals(Color.RED, foreground(table, 1));
	}

	private static Color foreground(FilterTable<?, ?> table, int row) {
		return table.prepareRenderer(table.getCellRenderer(row, 0), row, 0).getForeground();
	}
}
