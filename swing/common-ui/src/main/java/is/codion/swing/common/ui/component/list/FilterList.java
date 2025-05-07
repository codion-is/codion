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

import javax.swing.JList;
import javax.swing.JViewport;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
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

	private final class ScrollToSelected implements Consumer<List<Integer>> {

		@Override
		public void accept(List<Integer> selectedIndexes) {
			JViewport viewport = parentOfType(JViewport.class, FilterList.this);
			if (viewport != null && !selectedIndexes.isEmpty()) {
				ensureIndexIsVisible(selectedIndexes.get(0));
			}
		}
	}
}
