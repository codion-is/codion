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
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.swing.common.model.component.table.FilterTableModel.Columns;

import javax.swing.SortOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

final class DefaultFilterTableSortModel<R, C> implements FilterTableSortModel<R, C> {

	private final Columns<R, C> columns;
	private final Map<C, Comparator<?>> columnComparators = new HashMap<>();
	private final Event<C> sortingChangedEvent = Event.event();
	private final List<ColumnSortOrder<C>> columnSortOrders = new ArrayList<>(0);
	private final Set<C> columnSortingDisabled = new HashSet<>();
	private final RowComparator comparator = new RowComparator();

	DefaultFilterTableSortModel(Columns<R, C> columns) {
		this.columns = columns;
	}

	@Override
	public Comparator<R> comparator() {
		return comparator;
	}

	@Override
	public SortOrder sortOrder(C identifier) {
		requireNonNull(identifier);

		return columnSortOrders.stream()
						.filter(columnSortOrder -> columnSortOrder.identifier().equals(identifier))
						.findFirst()
						.map(ColumnSortOrder::sortOrder)
						.orElse(SortOrder.UNSORTED);
	}

	@Override
	public int sortPriority(C identifier) {
		requireNonNull(identifier);
		for (int i = 0; i < columnSortOrders.size(); i++) {
			if (columnSortOrders.get(i).identifier().equals(identifier)) {
				return i;
			}
		}

		return -1;
	}

	@Override
	public void setSortOrder(C identifier, SortOrder sortOrder) {
		setSortOrder(identifier, sortOrder, false);
	}

	@Override
	public void addSortOrder(C identifier, SortOrder sortOrder) {
		setSortOrder(identifier, sortOrder, true);
	}

	@Override
	public boolean sorted() {
		return !columnSortOrders.isEmpty();
	}

	@Override
	public List<ColumnSortOrder<C>> columnSortOrder() {
		return Collections.unmodifiableList(columnSortOrders);
	}

	@Override
	public void clear() {
		if (!columnSortOrders.isEmpty()) {
			C firstSortColumn = columnSortOrders.get(0).identifier();
			columnSortOrders.clear();
			sortingChangedEvent.accept(firstSortColumn);
		}
	}

	@Override
	public void setSortingEnabled(C identifier, boolean sortingEnabled) {
		requireNonNull(identifier);
		if (sortingEnabled) {
			columnSortingDisabled.remove(identifier);
		}
		else {
			columnSortingDisabled.add(identifier);
			if (removeSortOrder(identifier)) {
				sortingChangedEvent.accept(identifier);
			}
		}
	}

	@Override
	public boolean isSortingEnabled(C identifier) {
		return !columnSortingDisabled.contains(requireNonNull(identifier));
	}

	@Override
	public EventObserver<C> sortingChangedEvent() {
		return sortingChangedEvent.observer();
	}

	private void setSortOrder(C identifier, SortOrder sortOrder, boolean addColumnToSort) {
		requireNonNull(identifier);
		requireNonNull(sortOrder);
		if (!isSortingEnabled(identifier)) {
			throw new IllegalStateException("Sorting is disabled for column: " + identifier);
		}
		if (!addColumnToSort) {
			columnSortOrders.clear();
		}
		else {
			removeSortOrder(identifier);
		}
		if (sortOrder != SortOrder.UNSORTED) {
			columnSortOrders.add(new DefaultColumnSortOrder<>(identifier, sortOrder));
		}
		sortingChangedEvent.accept(identifier);
	}

	private boolean removeSortOrder(C identifier) {
		return columnSortOrders.removeIf(columnSortOrder -> columnSortOrder.identifier().equals(identifier));
	}

	private final class RowComparator implements Comparator<R> {

		@Override
		public int compare(R rowOne, R rowTwo) {
			for (ColumnSortOrder<C> columnSortOrder : columnSortOrders) {
				int comparison = compareRows(rowOne, rowTwo, columnSortOrder.identifier(), columnSortOrder.sortOrder());
				if (comparison != 0) {
					return comparison;
				}
			}

			return 0;
		}

		private int compareRows(R rowOne, R rowTwo, C identifier, SortOrder sortOrder) {
			Object valueOne = columns.value(rowOne, identifier);
			Object valueTwo = columns.value(rowTwo, identifier);
			int comparison;
			// Define null less than everything, except null.
			if (valueOne == null && valueTwo == null) {
				comparison = 0;
			}
			else if (valueOne == null) {
				comparison = -1;
			}
			else if (valueTwo == null) {
				comparison = 1;
			}
			else {
				comparison = ((Comparator<Object>) columnComparators.computeIfAbsent(identifier,
								k -> columns.comparator(identifier))).compare(valueOne, valueTwo);
			}
			if (comparison != 0) {
				return sortOrder == SortOrder.DESCENDING ? -comparison : comparison;
			}

			return 0;
		}
	}

	private static final class DefaultColumnSortOrder<C> implements ColumnSortOrder<C> {

		private final C identifier;
		private final SortOrder sortOrder;

		private DefaultColumnSortOrder(C identifier, SortOrder sortOrder) {
			this.identifier = identifier;
			this.sortOrder = sortOrder;
		}

		@Override
		public C identifier() {
			return identifier;
		}

		@Override
		public SortOrder sortOrder() {
			return sortOrder;
		}
	}
}
