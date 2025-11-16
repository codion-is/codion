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
package is.codion.swing.common.ui.component.list;

import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.JList;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import java.util.List;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.Utilities.parentOfType;

/**
 * A {@link JList} based on a {@link FilterListModel}
 * @param <T> the item type
 */
public final class FilterList<T> extends JList<T> {

	/**
	 * Instantiates a new {@link FilterList}
	 * @param listModel the list model
	 */
	FilterList(FilterListModel<T> listModel) {
		super(listModel);
		super.setSelectionModel((listModel).selection());
		listModel.selection().indexes().addConsumer(new ScrollToSelected());
	}

	/**
	 * @return the list model
	 */
	public FilterListModel<T> model() {
		return getModel();
	}

	@Override
	public FilterListModel<T> getModel() {
		return (FilterListModel<T>) super.getModel();
	}

	@Override
	public void setModel(ListModel<T> model) {
		throw new IllegalStateException("ListModel has already been set");
	}

	@Override
	public void setSelectionModel(ListSelectionModel selectionModel) {
		throw new IllegalStateException("Selection model has already been set");
	}

	public static Builder.ModelStep builder() {
		return DefaultFilterListBuilderFactory.MODEL;
	}

	private final class ScrollToSelected implements Consumer<List<Integer>> {

		@Override
		public void accept(List<Integer> selectedIndexes) {
			JViewport viewport = parentOfType(JViewport.class, FilterList.this);
			if (viewport != null && !selectedIndexes.isEmpty()) {
				ensureIndexIsVisible(selectedIndexes.get(0));
			}
		}
	}

	/**
	 * Builds a {@link FilterList} instance.
	 * @param <V> the component value type
	 * @param <T> the value type
	 * @param <B> the builder type
	 * @see #builder()
	 */
	public interface Builder<V, T, B extends Builder<V, T, B>> extends ComponentValueBuilder<FilterList<T>, V, B> {

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
		interface Items<T> extends Builder<List<T>, T, Items<T>> {

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
		interface SelectedItems<T> extends Builder<List<T>, T, SelectedItems<T>> {

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
		interface SelectedItem<T> extends Builder<T, T, SelectedItem<T>> {}

		/**
		 * Provides a {@link Factory}
		 */
		interface ModelStep {

			/**
			 * @param listModel the list model
			 * @param <T> the list item type
			 * @return a {@link Factory}
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
	}
}
