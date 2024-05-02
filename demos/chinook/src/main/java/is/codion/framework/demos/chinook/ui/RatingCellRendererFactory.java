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
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.Builder;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableCellRendererFactory;

import java.util.function.Function;

final class RatingCellRendererFactory extends EntityTableCellRendererFactory {

	private final Column<Integer> ratingColumn;

	RatingCellRendererFactory(SwingEntityTableModel tableModel, Column<Integer> ratingColumn) {
		super(tableModel);
		this.ratingColumn = ratingColumn;
	}

	@Override
	protected Builder<Entity, Attribute<?>> builder(FilteredTableColumn<Attribute<?>> column) {
		Builder<Entity, Attribute<?>> builder = super.builder(column);
		if (column.getIdentifier().equals(ratingColumn)) {
			builder.values(new RatingValues())
							.toolTipData(true);
		}

		return builder;
	}

	private static class RatingValues implements Function<Object, Object> {

		@Override
		public Object apply(Object columnValue) {
			int ranking = (Integer) columnValue;

			return Text.leftPad("", ranking, '*');
		}
	}
}
