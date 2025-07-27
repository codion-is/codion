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
package is.codion.swing.common.ui.component.list;

import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

abstract class AbstractListBuilder<T, V, B extends ListBuilder<T, V, B>> extends AbstractComponentBuilder<V, FilterList<T>, B> implements ListBuilder<T, V, B> {

	private final FilterListModel<T> listModel;
	private final List<ListSelectionListener> listSelectionListeners = new ArrayList<>();

	private @Nullable ListCellRenderer<T> cellRenderer;

	private @Nullable Integer visibleRowCount;
	private int layoutOrientation = JList.VERTICAL;
	private int fixedCellHeight = -1;
	private int fixedCellWidth = -1;

	AbstractListBuilder(FilterListModel<T> listModel) {
		this.listModel = requireNonNull(listModel);
	}

	@Override
	public final B visibleRowCount(int visibleRowCount) {
		this.visibleRowCount = visibleRowCount;
		return self();
	}

	@Override
	public final B layoutOrientation(int layoutOrientation) {
		this.layoutOrientation = layoutOrientation;
		return self();
	}

	@Override
	public final B fixedCellHeight(int fixedCellHeight) {
		this.fixedCellHeight = fixedCellHeight;
		return self();
	}

	@Override
	public final B fixedCellWidth(int fixedCellWidth) {
		this.fixedCellWidth = fixedCellWidth;
		return self();
	}

	@Override
	public final B cellRenderer(@Nullable ListCellRenderer<T> cellRenderer) {
		this.cellRenderer = cellRenderer;
		return self();
	}

	@Override
	public final B listSelectionListener(ListSelectionListener listSelectionListener) {
		listSelectionListeners.add(requireNonNull(listSelectionListener));
		return self();
	}

	protected final FilterList<T> createList() {
		FilterList<T> list = new FilterList<>(listModel);
		if (cellRenderer != null) {
			list.setCellRenderer(cellRenderer);
		}
		listSelectionListeners.forEach(new AddListSelectionListener(list));
		if (visibleRowCount != null) {
			list.setVisibleRowCount(visibleRowCount);
		}
		list.setLayoutOrientation(layoutOrientation);
		list.setFixedCellHeight(fixedCellHeight);
		list.setFixedCellWidth(fixedCellWidth);

		return list;
	}
}
