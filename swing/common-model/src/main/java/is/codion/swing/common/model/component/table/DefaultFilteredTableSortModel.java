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
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.Event;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;

import javax.swing.SortOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultFilteredTableSortModel<R, C> implements FilteredTableSortModel<R, C> {

  private final FilteredTableColumnModel<C> columnModel;
  private final ColumnValueProvider<R, C> columnValueProvider;
  private final Map<C, Comparator<?>> columnComparators = new HashMap<>();
  private final Event<C> sortingChangedEvent = Event.event();
  private final List<ColumnSortOrder<C>> columnSortOrders = new ArrayList<>(0);
  private final Set<C> columnSortingDisabled = new HashSet<>();

  DefaultFilteredTableSortModel(FilteredTableColumnModel<C> columnModel, ColumnValueProvider<R, C> columnValueProvider) {
    this.columnModel = requireNonNull(columnModel);
    this.columnValueProvider = requireNonNull(columnValueProvider);
  }

  @Override
  public void sort(List<R> items) {
    requireNonNull(items, "items").sort(new RowComparator());
  }

  @Override
  public SortOrder sortOrder(C columnIdentifier) {
    requireNonNull(columnIdentifier);

    return columnSortOrders.stream()
            .filter(columnSortOrder -> columnSortOrder.columnIdentifier().equals(columnIdentifier))
            .findFirst()
            .map(ColumnSortOrder::sortOrder)
            .orElse(SortOrder.UNSORTED);
  }

  @Override
  public int sortPriority(C columnIdentifier) {
    requireNonNull(columnIdentifier);
    for (int i = 0; i < columnSortOrders.size(); i++) {
      if (columnSortOrders.get(i).columnIdentifier().equals(columnIdentifier)) {
        return i;
      }
    }

    return -1;
  }

  @Override
  public void setSortOrder(C columnIdentifier, SortOrder sortOrder) {
    setSortOrder(columnIdentifier, sortOrder, false);
  }

  @Override
  public void addSortOrder(C columnIdentifier, SortOrder sortOrder) {
    setSortOrder(columnIdentifier, sortOrder, true);
  }

  @Override
  public boolean isSorted() {
    return !columnSortOrders.isEmpty();
  }

  @Override
  public List<ColumnSortOrder<C>> columnSortOrder() {
    return Collections.unmodifiableList(columnSortOrders);
  }

  @Override
  public void clear() {
    if (!columnSortOrders.isEmpty()) {
      C firstSortColumn = columnSortOrders.get(0).columnIdentifier();
      columnSortOrders.clear();
      sortingChangedEvent.accept(firstSortColumn);
    }
  }

  @Override
  public void setSortingEnabled(C columnIdentifier, boolean sortingEnabled) {
    requireNonNull(columnIdentifier);
    if (sortingEnabled) {
      columnSortingDisabled.remove(columnIdentifier);
    }
    else {
      columnSortingDisabled.add(columnIdentifier);
      if (removeSortOrder(columnIdentifier)) {
        sortingChangedEvent.accept(columnIdentifier);
      }
    }
  }

  @Override
  public boolean isSortingEnabled(C columnIdentifier) {
    return !columnSortingDisabled.contains(requireNonNull(columnIdentifier));
  }

  @Override
  public void addSortingChangedListener(Consumer<C> listener) {
    sortingChangedEvent.addDataListener(listener);
  }

  @Override
  public void removeSortingChangedListener(Consumer<C> listener) {
    sortingChangedEvent.removeDataListener(listener);
  }

  private void setSortOrder(C columnIdentifier, SortOrder sortOrder, boolean addColumnToSort) {
    requireNonNull(columnIdentifier);
    requireNonNull(sortOrder);
    if (!isSortingEnabled(columnIdentifier)) {
      throw new IllegalStateException("Sorting is disabled for column: " + columnIdentifier);
    }
    if (!addColumnToSort) {
      columnSortOrders.clear();
    }
    else {
      removeSortOrder(columnIdentifier);
    }
    if (sortOrder != SortOrder.UNSORTED) {
      columnSortOrders.add(new DefaultColumnSortOrder<>(columnIdentifier, sortOrder));
    }
    sortingChangedEvent.accept(columnIdentifier);
  }

  private boolean removeSortOrder(C columnIdentifier) {
    return columnSortOrders.removeIf(columnSortOrder -> columnSortOrder.columnIdentifier().equals(columnIdentifier));
  }

  private final class RowComparator implements Comparator<R> {

    @Override
    public int compare(R rowOne, R rowTwo) {
      for (ColumnSortOrder<C> columnSortOrder : columnSortOrders) {
        int comparison = compareRows(rowOne, rowTwo, columnSortOrder.columnIdentifier(), columnSortOrder.sortOrder());
        if (comparison != 0) {
          return comparison;
        }
      }

      return 0;
    }

    private int compareRows(R rowOne, R rowTwo, C columnIdentifier, SortOrder sortOrder) {
      Object valueOne = columnValueProvider.value(rowOne, columnIdentifier);
      Object valueTwo = columnValueProvider.value(rowTwo, columnIdentifier);
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
        comparison = ((Comparator<Object>) columnComparators.computeIfAbsent(columnIdentifier,
                k -> columnModel.column(columnIdentifier).comparator())).compare(valueOne, valueTwo);
      }
      if (comparison != 0) {
        return sortOrder == SortOrder.DESCENDING ? -comparison : comparison;
      }

      return 0;
    }
  }

  private static final class DefaultColumnSortOrder<C> implements ColumnSortOrder<C> {

    private final C columnIdentifier;
    private final SortOrder sortOrder;

    private DefaultColumnSortOrder(C columnIdentifier, SortOrder sortOrder) {
      this.columnIdentifier = columnIdentifier;
      this.sortOrder = sortOrder;
    }

    @Override
    public C columnIdentifier() {
      return columnIdentifier;
    }

    @Override
    public SortOrder sortOrder() {
      return sortOrder;
    }
  }
}
