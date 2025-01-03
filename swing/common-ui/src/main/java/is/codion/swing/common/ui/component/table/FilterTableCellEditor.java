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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A {@link TableCellEditor} implementation for {@link FilterTable}.
 * @param <T> the value type
 */
public interface FilterTableCellEditor<T> extends TableCellEditor {

	/**
	 * @return the underlying component value
	 */
	ComponentValue<T, ? extends JComponent> componentValue();

	/**
	 * Creates a new default {@link FilterTableCellEditor} instance.
	 * @param inputComponent supplies the input component
	 * @return a new default {@link FilterTableCellEditor} instance
	 * @param <T> the cell value type
	 */
	static <T> FilterTableCellEditor<T> filterTableCellEditor(Supplier<ComponentValue<T, ? extends JComponent>> inputComponent) {
		return new DefaultFilterTableCellEditor<>(inputComponent);
	}

	/**
	 * A factory for {@link TableCellEditor} instances.
	 */
	interface Factory<C> {

		/**
		 * @param identifier the column identifier
		 * @return a {@link TableCellEditor} instance for the given column or an empty optional if the column should not be editable
		 */
		Optional<TableCellEditor> create(C identifier);
	}
}
