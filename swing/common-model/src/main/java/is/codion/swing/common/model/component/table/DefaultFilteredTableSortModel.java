/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;

import javax.swing.SortOrder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultFilteredTableSortModel<R, C> implements FilteredTableSortModel<R, C> {

  private static final SortingState EMPTY_SORTING_STATE = new DefaultSortingState(SortOrder.UNSORTED, -1);

  private final ColumnValueProvider<R, C> columnValueProvider;
  private final Map<C, Comparator<?>> columnComparators = new HashMap<>();
  private final Event<C> sortingChangedEvent = Event.event();
  private final Map<C, SortingState> sortingStates = new HashMap<>();
  private final SortingStatesComparator sortingStatesComparator = new SortingStatesComparator();

  DefaultFilteredTableSortModel(ColumnValueProvider<R, C> columnValueProvider) {
    this.columnValueProvider = requireNonNull(columnValueProvider);
  }

  @Override
  public void sort(List<R> items) {
    requireNonNull(items, "items").sort(new RowComparator(sortingStatesOrderedByPriority()));
  }

  @Override
  public SortingState sortingState(C columnIdentifier) {
    requireNonNull(columnIdentifier, "columnIdentifier");

    return sortingStates.getOrDefault(columnIdentifier, EMPTY_SORTING_STATE);
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
  public boolean isSortingEnabled() {
    return sortingStates.values().stream()
            .anyMatch(state -> !state.equals(EMPTY_SORTING_STATE));
  }

  @Override
  public LinkedHashMap<C, SortOrder> columnSortOrder() {
    LinkedHashMap<C, SortOrder> columnSortOrder = new LinkedHashMap<>();
    sortingStatesOrderedByPriority().forEach(entry ->
            columnSortOrder.put(entry.getKey(), entry.getValue().sortOrder()));

    return (LinkedHashMap<C, SortOrder>) Collections.unmodifiableMap(columnSortOrder);
  }

  @Override
  public void clear() {
    if (!sortingStates.isEmpty()) {
      C firstSortColumn = sortingStatesOrderedByPriority().get(0).getKey();
      sortingStates.clear();
      sortingChangedEvent.onEvent(firstSortColumn);
    }
  }

  @Override
  public void addSortingChangedListener(EventDataListener<C> listener) {
    sortingChangedEvent.addDataListener(listener);
  }

  private void setSortOrder(C columnIdentifier, SortOrder sortOrder, boolean addColumnToSort) {
    requireNonNull(columnIdentifier, "columnIdentifier");
    requireNonNull(sortOrder, "sortOrder");
    if (!addColumnToSort) {
      sortingStates.clear();
    }
    if (sortOrder == SortOrder.UNSORTED) {
      sortingStates.remove(columnIdentifier);
    }
    else {
      SortingState state = sortingState(columnIdentifier);
      if (state.equals(EMPTY_SORTING_STATE)) {
        sortingStates.put(columnIdentifier, new DefaultSortingState(sortOrder, nextSortPriority()));
      }
      else {
        sortingStates.put(columnIdentifier, new DefaultSortingState(sortOrder, state.priority()));
      }
    }
    sortingChangedEvent.onEvent(columnIdentifier);
  }

  private List<Map.Entry<C, SortingState>> sortingStatesOrderedByPriority() {
    return sortingStates.entrySet().stream()
            .sorted(sortingStatesComparator)
            .collect(toList());
  }

  private int nextSortPriority() {
    int maxPriority = -1;
    for (SortingState state : sortingStates.values()) {
      maxPriority = Math.max(state.priority(), maxPriority);
    }

    return maxPriority + 1;
  }

  private final class RowComparator implements Comparator<R> {

    private final List<Map.Entry<C, SortingState>> sortedSortingStates;

    private RowComparator(List<Map.Entry<C, SortingState>> sortedSortingStates) {
      this.sortedSortingStates = sortedSortingStates;
    }

    @Override
    public int compare(R rowOne, R rowTwo) {
      for (Map.Entry<C, FilteredTableSortModel.SortingState> state : sortedSortingStates) {
        int comparison = compareRows(rowOne, rowTwo, state.getKey(), state.getValue().sortOrder());
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
                k -> columnValueProvider.comparator(columnIdentifier))).compare(valueOne, valueTwo);
      }
      if (comparison != 0) {
        return sortOrder == SortOrder.DESCENDING ? -comparison : comparison;
      }

      return 0;
    }
  }

  private final class SortingStatesComparator implements Comparator<Map.Entry<C, SortingState>> {
    @Override
    public int compare(Map.Entry<C, SortingState> state1, Map.Entry<C, SortingState> state2) {
      return Integer.compare(state1.getValue().priority(), state2.getValue().priority());
    }
  }

  private static final class DefaultSortingState implements SortingState {

    private final SortOrder sortOrder;
    private final int priority;

    private DefaultSortingState(SortOrder sortOrder, int priority) {
      this.sortOrder = sortOrder;
      this.priority = priority;
    }

    @Override
    public int priority() {
      return priority;
    }

    @Override
    public SortOrder sortOrder() {
      return sortOrder;
    }
  }
}
