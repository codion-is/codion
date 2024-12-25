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
package is.codion.swing.common.model.component.table;

import is.codion.common.event.Event;
import is.codion.common.observable.Observer;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.table.FilterTableModel.Columns;

import javax.swing.SortOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableSortModel<R, C> implements FilterTableSortModel<R, C> {

	private final Columns<R, C> columns;
	private final Map<C, Comparator<?>> columnComparators = new HashMap<>();
	private final Event<Boolean> sortingChanged = Event.event();
	private final List<ColumnSortOrder<C>> columnSortOrders = new ArrayList<>(0);
	private final Map<C, State> locked = new HashMap<>();
	private final RowComparator comparator = new RowComparator();

	DefaultFilterTableSortModel(Columns<R, C> columns) {
		this.columns = columns;
	}

	@Override
	public Comparator<R> comparator() {
		return comparator;
	}

	@Override
	public ColumnSortOrder<C> columnSortOrder(C identifier) {
		validateIdentifier(identifier);

		return columnSortOrders.stream()
						.filter(columnSortOrder -> columnSortOrder.identifier().equals(identifier))
						.findFirst()
						.orElse(new DefaultColumnSortOrder<>(identifier, SortOrder.UNSORTED, -1));
	}

	@Override
	public Sort sort(C identifier) {
		return new DefaultSort(identifier);
	}

	@Override
	public List<ColumnSortOrder<C>> columnSortOrder() {
		return unmodifiableList(columnSortOrders);
	}

	@Override
	public void clear() {
		if (!columnSortOrders.isEmpty()) {
			columnSortOrders.clear();
			sortingChanged.accept(false);
		}
	}

	@Override
	public State locked(C identifier) {
		validateIdentifier(identifier);

		return locked.computeIfAbsent(identifier, k -> State.state());
	}

	@Override
	public Observer<Boolean> observer() {
		return sortingChanged.observer();
	}

	private void setSortOrder(C identifier, SortOrder sortOrder, boolean addColumnToSort) {
		validateIdentifier(identifier);
		requireNonNull(sortOrder);
		if (locked.containsKey(identifier) && locked.get(identifier).get()) {
			throw new IllegalStateException("Sorting is locked for column: " + identifier);
		}
		if (!addColumnToSort) {
			columnSortOrders.clear();
		}
		else {
			removeSortOrder(identifier);
		}
		if (sortOrder != SortOrder.UNSORTED) {
			columnSortOrders.add(new DefaultColumnSortOrder<>(identifier, sortOrder, columnSortOrders.size()));
		}
		sortingChanged.accept(!columnSortOrders.isEmpty());
	}

	private boolean removeSortOrder(C identifier) {
		return columnSortOrders.removeIf(columnSortOrder -> columnSortOrder.identifier().equals(identifier));
	}

	private void validateIdentifier(C identifier) {
		if (!columns.identifiers().contains(requireNonNull(identifier))) {
			throw new IllegalArgumentException("Column not found: " + identifier);
		}
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

	private final class DefaultSort implements Sort {

		private final C identifier;

		private DefaultSort(C identifier) {
			validateIdentifier(identifier);
			this.identifier = identifier;
		}

		@Override
		public void set(SortOrder sortOrder) {
			setSortOrder(identifier, sortOrder, false);
		}

		@Override
		public void add(SortOrder sortOrder) {
			setSortOrder(identifier, sortOrder, true);
		}
	}

	private static final class DefaultColumnSortOrder<C> implements ColumnSortOrder<C> {

		private final C identifier;
		private final SortOrder sortOrder;
		private final int priority;

		private DefaultColumnSortOrder(C identifier, SortOrder sortOrder, int priority) {
			this.identifier = identifier;
			this.sortOrder = sortOrder;
			this.priority = priority;
		}

		@Override
		public C identifier() {
			return identifier;
		}

		@Override
		public SortOrder sortOrder() {
			return sortOrder;
		}

		@Override
		public int priority() {
			return priority;
		}
	}
}
