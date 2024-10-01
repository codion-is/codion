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

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableCellRendererFactory;

import java.util.Map;

import static is.codion.common.Text.rightPad;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.rangeClosed;

final class RatingCellRendererFactory extends EntityTableCellRendererFactory {

	private static final Map<Integer, String> RATINGS = rangeClosed(1, 10)
					.mapToObj(ranking -> rightPad("", ranking, '*'))
					.collect(toMap(String::length, identity()));

	private final Column<Integer> ratingColumn;

	RatingCellRendererFactory(SwingEntityTableModel tableModel, Column<Integer> ratingColumn) {
		super(tableModel);
		this.ratingColumn = ratingColumn;
	}

	@Override
	public FilterTableCellRenderer tableCellRenderer(FilterTableColumn<Attribute<?>> column) {
		if (column.identifier().equals(ratingColumn)) {
			builder(column).string(rating -> RATINGS.get((Integer) rating))
							.toolTipData(true);
		}

		return builder(column).build();
	}
}
