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

import is.codion.swing.common.ui.component.table.FilterTableCellEditor.Builder.ComponentStep;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;
import java.util.EventObject;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link TableCellEditor} implementation for {@link FilterTable}.
 * @param <T> the value type
 * @see #builder()
 */
public interface FilterTableCellEditor<T> extends TableCellEditor {

	/**
	 * @return the underlying component value
	 */
	ComponentValue<T, ? extends JComponent> componentValue();

	/**
	 * @return a {@link Builder.ComponentStep} instance.
	 */
	static ComponentStep builder() {
		return DefaultFilterTableCellEditor.DefaultBuilder.COMPONENT;
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

	/**
	 * @param <T> the cell type
	 */
	interface Builder<T> {

		/**
		 * Provides a {@link FilterTableCellEditor.Builder}
		 */
		interface ComponentStep {

			/**
			 * Creates a new default {@link FilterTableCellEditor.Builder} instance.
			 * @param component supplies the input component
			 * @param <T> the cell value type
			 * @return a new {@link FilterTableCellEditor.Builder} instance
			 */
			<T> Builder<T> component(Supplier<ComponentValue<T, ? extends JComponent>> component);
		}

		/**
		 * @param cellEditable a function specifying if a cell is editable
		 * @return this builder
		 * @see TableCellEditor#isCellEditable(EventObject)
		 */
		Builder<T> cellEditable(Function<EventObject, Boolean> cellEditable);

		/**
		 * @return a new {@link FilterTableCellEditor} based on this builder
		 */
		FilterTableCellEditor<T> build();
	}
}
