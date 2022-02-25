/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.table;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;

import javax.swing.SortOrder;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default TableSortModel implementation
 * @param <R> the type representing a row in the table model
 * @param <C> the type representing the column identifier in the table model
 */
public abstract class AbstractTableSortModel<R, C> implements TableSortModel<R, C> {

  private static final Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = Comparable::compareTo;
  private static final Comparator<?> TO_STRING_COMPARATOR = new ToStringComparator();

  private static final SortingState EMPTY_SORTING_STATE = new DefaultSortingState(SortOrder.UNSORTED, -1);

  /**
   * The comparators used to compare column values
   */
  private final Map<C, Comparator<?>> columnComparators = new HashMap<>();

  /**
   * Fired when a column sorting state changes
   */
  private final Event<C> sortingChangedEvent = Event.event();

  /**
   * holds the column sorting states
   */
  private final Map<C, SortingState> sortingStates = new HashMap<>();
  private final SortingStatesComparator sortingStatesComparator = new SortingStatesComparator();

  @Override
  public final void sort(final List<R> items) {
    requireNonNull(items, "items").sort(new RowComparator(getSortingStatesOrderedByPriority()));
  }

  @Override
  public final SortingState getSortingState(final C columnIdentifier) {
    requireNonNull(columnIdentifier, "columnIdentifier");

    return sortingStates.getOrDefault(columnIdentifier, EMPTY_SORTING_STATE);
  }

  @Override
  public final void setSortOrder(final C columnIdentifier, final SortOrder sortOrder) {
    setSortOrder(columnIdentifier, sortOrder, false);
  }

  @Override
  public final void addSortOrder(final C columnIdentifier, final SortOrder sortOrder) {
    setSortOrder(columnIdentifier, sortOrder, true);
  }

  @Override
  public final boolean isSortingEnabled() {
    return sortingStates.values().stream()
            .anyMatch(state -> !state.equals(EMPTY_SORTING_STATE));
  }

  @Override
  public final LinkedHashMap<C, SortOrder> getColumnSortOrder() {
    LinkedHashMap<C, SortOrder> columnSortOrder = new LinkedHashMap<>();
    getSortingStatesOrderedByPriority().forEach(entry ->
            columnSortOrder.put(entry.getKey(), entry.getValue().getSortOrder()));

    return columnSortOrder;
  }

  @Override
  public final void clear() {
    if (!sortingStates.isEmpty()) {
      C firstSortColumn = getSortingStatesOrderedByPriority().get(0).getKey();
      sortingStates.clear();
      sortingChangedEvent.onEvent(firstSortColumn);
    }
  }

  @Override
  public final void addSortingChangedListener(final EventDataListener<C> listener) {
    sortingChangedEvent.addDataListener(listener);
  }

  /**
   * Returns a value the given row and columnIdentifier, used for sorting
   * @param row the object representing a given row
   * @param columnIdentifier the column identifier
   * @return a value for the given row and column
   */
  protected abstract Object getColumnValue(R row, C columnIdentifier);

  /**
   * Initializes a comparator used when sorting by the give column,
   * the comparator receives the column values, but never null.
   * @param columnIdentifier the column identifier
   * @return the comparator to use when sorting by the given column
   */
  protected Comparator<?> initializeColumnComparator(final C columnIdentifier) {
    Class<?> columnClass = getColumnClass(columnIdentifier);
    if (Comparable.class.isAssignableFrom(columnClass)) {
      return COMPARABLE_COMPARATOR;
    }

    return TO_STRING_COMPARATOR;
  }

  private void setSortOrder(final C columnIdentifier, final SortOrder sortOrder, final boolean addColumnToSort) {
    requireNonNull(columnIdentifier, "columnIdentifier");
    requireNonNull(sortOrder, "sortOrder");
    if (!addColumnToSort) {
      sortingStates.clear();
    }
    if (sortOrder == SortOrder.UNSORTED) {
      sortingStates.remove(columnIdentifier);
    }
    else {
      SortingState state = getSortingState(columnIdentifier);
      if (state.equals(EMPTY_SORTING_STATE)) {
        sortingStates.put(columnIdentifier, new DefaultSortingState(sortOrder, getNextSortPriority()));
      }
      else {
        sortingStates.put(columnIdentifier, new DefaultSortingState(sortOrder, state.getPriority()));
      }
    }
    sortingChangedEvent.onEvent(columnIdentifier);
  }

  private List<Map.Entry<C, SortingState>> getSortingStatesOrderedByPriority() {
    return sortingStates.entrySet().stream()
            .sorted(sortingStatesComparator)
            .collect(toList());
  }

  private int getNextSortPriority() {
    int maxPriority = -1;
    for (final SortingState state : sortingStates.values()) {
      maxPriority = Math.max(state.getPriority(), maxPriority);
    }

    return maxPriority + 1;
  }

  private final class RowComparator implements Comparator<R> {

    private final List<Map.Entry<C, SortingState>> sortedSortingStates;

    private RowComparator(final List<Map.Entry<C, SortingState>> sortedSortingStates) {
      this.sortedSortingStates = sortedSortingStates;
    }

    @Override
    public int compare(final R o1, final R o2) {
      for (final Map.Entry<C, TableSortModel.SortingState> state : sortedSortingStates) {
        int comparison = compareRows(o1, o2, state.getKey(), state.getValue().getSortOrder());
        if (comparison != 0) {
          return comparison;
        }
      }

      return 0;
    }

    private int compareRows(final R rowOne, final R rowTwo, final C columnIdentifier, final SortOrder sortOrder) {
      Object valueOne = getColumnValue(rowOne, columnIdentifier);
      Object valueTwo = getColumnValue(rowTwo, columnIdentifier);
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
                k -> initializeColumnComparator(columnIdentifier))).compare(valueOne, valueTwo);
      }
      if (comparison != 0) {
        return sortOrder == SortOrder.DESCENDING ? -comparison : comparison;
      }

      return 0;
    }
  }

  private static final class ToStringComparator implements Comparator<Object> {
    @Override
    public int compare(final Object o1, final Object o2) {
      return o1.toString().compareTo(o2.toString());
    }
  }

  private final class SortingStatesComparator implements Comparator<Map.Entry<C, SortingState>> {
    @Override
    public int compare(final Map.Entry<C, SortingState> state1, final Map.Entry<C, SortingState> state2) {
      return Integer.compare(state1.getValue().getPriority(), state2.getValue().getPriority());
    }
  }

  private static final class DefaultSortingState implements SortingState {

    private final SortOrder sortOrder;
    private final int priority;

    private DefaultSortingState(final SortOrder sortOrder, final int priority) {
      this.sortOrder = sortOrder;
      this.priority = priority;
    }

    @Override
    public int getPriority() {
      return priority;
    }

    @Override
    public SortOrder getSortOrder() {
      return sortOrder;
    }
  }
}
