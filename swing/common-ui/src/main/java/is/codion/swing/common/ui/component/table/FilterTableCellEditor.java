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

import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor.Builder.ComponentStep;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;
import java.util.EventObject;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.integerValue;

/**
 * A {@link TableCellEditor} implementation for {@link FilterTable}.
 * @param <C> the component type
 * @param <T> the value type
 * @see #builder()
 */
public interface FilterTableCellEditor<C extends JComponent, T> extends TableCellEditor {

	/**
	 * The default click count to start editing in tables.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 2
	 * </ul>
	 * @see javax.swing.DefaultCellEditor#setClickCountToStart(int)
	 */
	PropertyValue<Integer> CLICK_COUNT_TO_START =
					integerValue(FilterTableCellEditor.class.getName() + ".clickCountToStart", 2);

	/**
	 * @return the underlying component value
	 */
	ComponentValue<C, T> componentValue();

	/**
	 * @return a {@link Builder.ComponentStep} instance.
	 */
	static ComponentStep builder() {
		return DefaultFilterTableCellEditor.DefaultBuilder.COMPONENT_STEP;
	}

	/**
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 * @return a new default {@link FilterTableCellEditor.Factory} instance
	 */
	static <R, C> FilterTableCellEditor.Factory<R, C> factory() {
		return (Factory<R, C>) DefaultFilterTableCellEditor.DEFAULT_FACTORY;
	}

	/**
	 * A factory for {@link FilterTableCellEditor} instances.
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 */
	interface Factory<R, C> {

		/**
		 * @param identifier the column identifier
		 * @param table the table
		 * @return a {@link FilterTableCellEditor} instance for the given column or an empty optional if the column should not be editable
		 */
		Optional<FilterTableCellEditor<?, ?>> create(C identifier, FilterTable<R, C> table);
	}

	/**
	 * @param <C> the component type
	 * @param <T> the cell type
	 */
	interface Builder<C extends JComponent, T> {

		/**
		 * Provides a {@link FilterTableCellEditor.Builder}
		 */
		interface ComponentStep {

			/**
			 * Creates a new default {@link FilterTableCellEditor.Builder} instance.
			 * @param component supplies the input component
			 * @param <C> the component type
			 * @param <T> the cell value type
			 * @return a new {@link FilterTableCellEditor.Builder} instance
			 */
			<C extends JComponent, T> Builder<C, T> component(Supplier<ComponentValue<C, T>> component);
		}

		/**
		 * Overrides {@link #clickCountToStart(int)}
		 * @param cellEditable a function specifying if a cell is editable
		 * @return this builder
		 * @see TableCellEditor#isCellEditable(EventObject)
		 */
		Builder<C, T> cellEditable(Function<EventObject, Boolean> cellEditable);

		/**
		 * @param shouldSelectCell a function specifying if the editing cell should be selected
		 * @return this builder
		 * @see TableCellEditor#shouldSelectCell(EventObject)
		 */
		Builder<C, T> shouldSelectCell(Function<EventObject, Boolean> shouldSelectCell);

		/**
		 * Default specified by {@link #CLICK_COUNT_TO_START}.
		 * <p>Mote that when using {@link javax.swing.JCheckBox} based editors setting this to 1 is usually preferred.
		 * @param clickCountToStart specifies the number of clicks needed to start editing
		 * @return this builder
		 * @see #CLICK_COUNT_TO_START
		 */
		Builder<C, T> clickCountToStart(int clickCountToStart);

		/**
		 * <p>Configures whether this editor should request the row to be resized to accommodate the editor compoent size.
		 * <p>Note that this setting is for overriding the default one, set via the global
		 * {@link FilterTable#RESIZE_ROW_TO_FIT_EDITOR} configuration setting or
		 * {@link FilterTable.Builder#resizeRowToFitEditor(boolean)} for a specific table instance.
		 * @return this builder
		 */
		Builder<C, T> resizeRow(boolean resizeRow);

		/**
		 * For custom or composite components, this method can be used to configure the component,
		 * for example to stop editing, which may not work properly if the component isn't
		 * a direct descendant of the typical Swing components ({@link javax.swing.JTextField}, {@link javax.swing.JComboBox}, etc.).
		 * @param cellEditor receives the cell editor for configuring
		 * @return this builder
		 */
		Builder<C, T> configure(Consumer<FilterTableCellEditor<C, T>> cellEditor);

		/**
		 * @return a new {@link FilterTableCellEditor} based on this builder
		 */
		FilterTableCellEditor<C, T> build();
	}
}
