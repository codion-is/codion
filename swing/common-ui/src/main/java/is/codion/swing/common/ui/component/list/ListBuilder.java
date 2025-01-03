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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import java.util.List;

/**
 * Builds a JList instance.
 * @param <T> the value type
 * @param <V> the component value type
 * @param <B> the builder type
 * @see #factory(ListModel)
 */
public interface ListBuilder<T, V, B extends ListBuilder<T, V, B>> extends ComponentBuilder<V, JList<T>, B> {

	/**
	 * @param visibleRowCount the visible row count
	 * @return this builder instance
	 * @see JList#setVisibleRowCount(int)
	 */
	B visibleRowCount(int visibleRowCount);

	/**
	 * @param layoutOrientation the list layout orientation
	 * @return thi builder instance
	 * @see JList#setLayoutOrientation(int)
	 */
	B layoutOrientation(int layoutOrientation);

	/**
	 * @param fixedCellHeight the fixed cell height
	 * @return this builder instance
	 * @see JList#setFixedCellHeight(int)
	 */
	B fixedCellHeight(int fixedCellHeight);

	/**
	 * @param fixedCellWidth the fixed cell width
	 * @return this builder instance
	 * @see JList#setFixedCellWidth(int)
	 */
	B fixedCellWidth(int fixedCellWidth);

	/**
	 * @param cellRenderer the cell renderer
	 * @return this builder instance
	 * @see JList#setCellRenderer(ListCellRenderer)
	 */
	B cellRenderer(ListCellRenderer<T> cellRenderer);

	/**
	 * @param selectionModel the list selection model
	 * @return this builder instance
	 * @see JList#setSelectionModel(ListSelectionModel)
	 */
	B selectionModel(ListSelectionModel selectionModel);

	/**
	 * @param listSelectionListener the list selection listener
	 * @return this builder instance
	 * @see JList#addListSelectionListener(ListSelectionListener)
	 */
	B listSelectionListener(ListSelectionListener listSelectionListener);

	/**
	 * Builds a JList, where the value is represented by the list items.
	 * @param <T> the value type
	 */
	interface Items<T> extends ListBuilder<T, List<T>, Items<T>> {

		/**
		 * @param selectionMode the list selection model
		 * @return this builder instance
		 * @see JList#setSelectionMode(int)
		 */
		Items<T> selectionMode(int selectionMode);
	}

	/**
	 * Builds a multi-selection JList, where the value is represented by the selected items.
	 * @param <T> the value type
	 */
	interface SelectedItems<T> extends ListBuilder<T, List<T>, SelectedItems<T>> {}

	/**
	 * Builds a single-selection JList, where the value is represented by the selected item.
	 * @param <T> the value type
	 */
	interface SelectedItem<T> extends ListBuilder<T, T, SelectedItem<T>> {}

	/**
	 * A factory for list builders, depending on what the component value should represent.
	 */
	interface Factory<T> {

		/**
		 * A JList builder, where the value is represented by the list items.
		 * @return a JList builder
		 */
		Items<T> items();

		/**
		 * A JList builder, where the value is represented by the list items.
		 * @param linkedValue the value to link to the list items
		 * @return a JList builder
		 */
		Items<T> items(Value<List<T>> linkedValue);

		/**
		 * A multi selection JList builder, where the value is represented by the selected items.
		 * @return a JList builder
		 */
		SelectedItems<T> selectedItems();

		/**
		 * A multi selection JList builder, where the value is represented by the selected items.
		 * @param linkedValue the value to link to the selected items
		 * @return a JList builder
		 */
		SelectedItems<T> selectedItems(Value<List<T>> linkedValue);

		/**
		 * A single-selection JList builder, where the value is represented by the selected item.
		 * @return a JList builder
		 */
		SelectedItem<T> selectedItem();

		/**
		 * A single-selection JList builder, where the value is represented by the selected item.
		 * @param linkedValue the value to link to the selected item
		 * @return a JList builder
		 */
		SelectedItem<T> selectedItem(Value<T> linkedValue);
	}

	/**
	 * @param listModel the list model to base the list on
	 * @param <T> the list value type
	 * @return a new list builder factory
	 */
	static <T> Factory<T> factory(ListModel<T> listModel) {
		return new DefaultListBuilderFactory<>(listModel);
	}
}
