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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.Text;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.ui.component.table.FilteredTableCellRendererFactory;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableCellRenderer;

import javax.swing.table.TableCellRenderer;
import java.util.function.Function;

final class RatingCellRendererFactory
				implements FilteredTableCellRendererFactory<Attribute<?>> {

	private final SwingEntityTableModel tableModel;
	private final Column<Integer> ratingColumn;

	RatingCellRendererFactory(SwingEntityTableModel tableModel, Column<Integer> ratingColumn) {
		this.tableModel = tableModel;
		this.ratingColumn = ratingColumn;
	}

	@Override
	public TableCellRenderer tableCellRenderer(FilteredTableColumn<Attribute<?>> column) {
		if (column.getIdentifier().equals(ratingColumn)) {
			return EntityTableCellRenderer.builder(tableModel, column.getIdentifier())
							.displayValueProvider(new RatingDisplayValueProvider())
							.toolTipData(true)
							.build();
		}

		return EntityTableCellRenderer.builder(tableModel, column.getIdentifier()).build();
	}

	private static class RatingDisplayValueProvider implements Function<Object, Object> {

		@Override
		public Object apply(Object columnValue) {
			int ranking = (Integer) columnValue;

			return Text.leftPad("", ranking, '*');
		}
	}
}
