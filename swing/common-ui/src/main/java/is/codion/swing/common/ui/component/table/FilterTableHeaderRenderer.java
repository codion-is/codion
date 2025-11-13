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
package is.codion.swing.common.ui.component.table;

import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.model.component.table.FilterTableModel;

import javax.swing.table.TableCellRenderer;

import static is.codion.common.utilities.Configuration.booleanValue;

/**
 * A renderer for a {@link FilterTable} header.
 */
public interface FilterTableHeaderRenderer extends TableCellRenderer {

	/**
	 * Specifies whether the focused column should be indicated with a darkened header.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> FOCUSED_COLUMN_INDICATOR =
					booleanValue(FilterTableHeaderRenderer.class.getName() + ".focusedColumnIndicator", true);

	/**
	 * A factory for {@link FilterTableHeaderRenderer} instances.
	 * @param <C> the column identifier type
	 */
	interface Factory<C> {

		/**
		 * @param column the column
		 * @param tableModel the table model
		 * @return a {@link FilterTableHeaderRenderer} instance for the given column
		 */
		FilterTableHeaderRenderer create(FilterTableColumn<C> column, FilterTableModel<?, C> tableModel);
	}

	/**
	 * @param <C> the column identifier type
	 * @return a {@link FilterTableHeaderRenderer.Factory} instance
	 */
	static <C> FilterTableHeaderRenderer.Factory<C> factory() {
		return (Factory<C>) DefaultFilterTableHeaderRenderer.FACTORY;
	}
}
