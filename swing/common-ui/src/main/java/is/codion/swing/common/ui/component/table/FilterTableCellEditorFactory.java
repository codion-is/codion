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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import javax.swing.table.TableCellEditor;
import java.util.Optional;

/**
 * A factory for {@link TableCellEditor} instances.
 */
public interface FilterTableCellEditorFactory<C> {

	/**
	 * @param column the column
	 * @return a {@link TableCellEditor} instance for the given column or an empty optional if the column should not be editable
	 */
	Optional<TableCellEditor> tableCellEditor(FilterTableColumn<C> column);
}
