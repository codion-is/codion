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

import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionListener;
import java.util.List;

/**
 * Builds a JList instance.
 * @param <V> the component value type
 * @param <T> the value type
 * @param <B> the builder type
 * @see #builder()
 */
public interface ListBuilder<V, T, B extends ListBuilder<V, T, B>> extends ComponentValueBuilder<FilterList<T>, V, B> {

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
	B cellRenderer(@Nullable ListCellRenderer<T> cellRenderer);

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
	interface Items<T> extends ListBuilder<List<T>, T, Items<T>> {

		/**
		 * @param selectionMode the list selection model
		 * @return this builder instance
		 * @see JList#setSelectionMode(int)
		 */
		Items<T> selectionMode(int selectionMode);

		/**
		 * Default false.
		 * @param nullable if true then null is used instead of an empty list
		 * @return this builder instance
		 */
		Items<T> nullable(boolean nullable);
	}

	/**
	 * Builds a multi-selection JList, where the value is represented by the selected items.
	 * @param <T> the value type
	 */
	interface SelectedItems<T> extends ListBuilder<List<T>, T, SelectedItems<T>> {

		/**
		 * Default false.
		 * @param nullable if true then null is used instead of an empty list
		 * @return this builder instance
		 */
		SelectedItems<T> nullable(boolean nullable);
	}

	/**
	 * Builds a single-selection JList, where the value is represented by the selected item.
	 * @param <T> the value type
	 */
	interface SelectedItem<T> extends ListBuilder<T, T, SelectedItem<T>> {}

	/**
	 * Provides a {@link Factory}
	 */
	interface ModelStep {

		/**
		 * @param listModel the list model
		 * @return a {@link Factory}
		 * @param <T> the list item type
		 */
		<T> Factory<T> model(FilterListModel<T> listModel);
	}

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
		 * A multi selection JList builder, where the value is represented by the selected items.
		 * @return a JList builder
		 */
		SelectedItems<T> selectedItems();

		/**
		 * A single-selection JList builder, where the value is represented by the selected item.
		 * @return a JList builder
		 */
		SelectedItem<T> selectedItem();
	}

	/**
	 * @return a new list builder factory
	 */
	static ModelStep builder() {
		return DefaultListBuilderFactory.MODEL;
	}
}
