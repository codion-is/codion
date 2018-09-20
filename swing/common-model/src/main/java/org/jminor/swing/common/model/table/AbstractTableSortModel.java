/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.TextUtil;
import org.jminor.common.model.table.SortingDirective;
import org.jminor.common.model.table.TableSortModel;

import javax.swing.table.TableColumn;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A default TableSortModel implementation
 * @param <R> the type representing a row in the table model
 * @param <C> the type representing the column identifier in the table model
 */
public abstract class AbstractTableSortModel<R, C> implements TableSortModel<R, C, TableColumn> {

  private static final Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = Comparable::compareTo;
  private static final Comparator LEXICAL_COMPARATOR = TextUtil.getSpaceAwareCollator();

  private static final SortingState EMPTY_SORTING_STATE = new DefaultSortingState(SortingDirective.UNSORTED, -1);

  /**
   * The columns available for sorting
   */
  private final List<TableColumn> columns;

  /**
   * The comparators used to compare column values
   */
  private final Map<C, Comparator> columnComparators = new HashMap<>();

  /**
   * Fired when a column sorting state changes
   */
  private final Event sortingStateChangedEvent = Events.event();

  /**
   * holds the column sorting states
   */
  private final Map<C, SortingState> sortingStates = new HashMap<>();

  /**
   * The comparator used when comparing row objects
   */
  private final Comparator<R> rowComparator = new RowComparator();

  /**
   * Instantiates a new AbstractTableSortModel
   * @param columns the table columns
   */
  public AbstractTableSortModel(final List<TableColumn> columns) {
    this.columns = Collections.unmodifiableList(columns);
    resetSortingStates();
  }

  /** {@inheritDoc} */
  @Override
  public final void sort(final List<R> items) {
    items.sort(rowComparator);
  }

  /** {@inheritDoc} */
  @Override
  public final List<TableColumn> getColumns() {
    return columns;
  }

  /** {@inheritDoc} */
  @Override
  public final int getSortingPriority(final C columnIdentifier) {
    return getSortingState(columnIdentifier).getPriority();
  }

  /** {@inheritDoc} */
  @Override
  public final SortingDirective getSortingDirective(final C columnIdentifier) {
    return getSortingState(columnIdentifier).getDirective();
  }

  /** {@inheritDoc} */
  @Override
  public final void setSortingDirective(final C columnIdentifier, final SortingDirective directive,
                                        final boolean addColumnToSort) {
    if (!addColumnToSort) {
      resetSortingStates();
    }
    if (directive == SortingDirective.UNSORTED) {
      sortingStates.put(columnIdentifier, EMPTY_SORTING_STATE);
    }
    else {
      final SortingState state = getSortingState(columnIdentifier);
      if (state.equals(EMPTY_SORTING_STATE)) {
        final int priority = getNextSortPriority();
        sortingStates.put(columnIdentifier, new DefaultSortingState(directive, priority));
      }
      else {
        sortingStates.put(columnIdentifier, new DefaultSortingState(directive, state.getPriority()));
      }
    }
    sortingStateChangedEvent.fire();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isSortingEnabled() {
    return sortingStates.values().stream().anyMatch(state -> !state.equals(EMPTY_SORTING_STATE));
  }

  /** {@inheritDoc} */
  @Override
  public final void addSortingStateChangedListener(final EventListener listener) {
    sortingStateChangedEvent.addListener(listener);
  }

  /**
   * Returns a Comparable instance for the given rowObject and columnIdentifier, used when sorting
   * @param rowObject the object representing a given row
   * @param columnIdentifier the column identifier
   * @return a Comparable for the given row and column
   * @see #sort(java.util.List)
   */
  protected abstract Comparable getComparable(final R rowObject, final C columnIdentifier);

  /**
   * Initializes a comparator used when sorting by the give column,
   * the comparator receives the column values, but never null.
   * @param columnIdentifier the column identifier
   * @return the comparator to use when sorting by the given column
   */
  protected Comparator initializeColumnComparator(final C columnIdentifier) {
    final Class columnClass = getColumnClass(columnIdentifier);
    if (columnClass.equals(String.class)) {
      return LEXICAL_COMPARATOR;
    }
    if (Comparable.class.isAssignableFrom(columnClass)) {
      return COMPARABLE_COMPARATOR;
    }

    return LEXICAL_COMPARATOR;
  }

  private SortingState getSortingState(final C columnIdentifier) {
    final SortingState state = sortingStates.get(columnIdentifier);
    if (state == null) {
      throw new IllegalArgumentException("No sorting state assigned to column identified by : " + columnIdentifier);
    }

    return state;
  }

  @SuppressWarnings({"unchecked"})
  private void resetSortingStates() {
    for (final TableColumn column : columns) {
      sortingStates.put((C) column.getIdentifier(), EMPTY_SORTING_STATE);
    }
  }

  @SuppressWarnings({"unchecked"})
  private int getNextSortPriority() {
    int maxPriority = -1;
    for (final SortingState state : sortingStates.values()) {
      maxPriority = Math.max(state.getPriority(), maxPriority);
    }

    return maxPriority + 1;
  }

  private final class RowComparator implements Comparator<R> {
    @Override
    public int compare(final R o1, final R o2) {
      for (final Map.Entry<C, TableSortModel.SortingState> state : getSortingStatesOrderedByPriority()) {
        final int comparison = compareRows(o1, o2, state.getKey(), state.getValue().getDirective());
        if (comparison != 0) {
          return comparison;
        }
      }

      return 0;
    }

    private int compareRows(final R rowOne, final R rowTwo, final C columnIdentifier, final SortingDirective directive) {
      final Comparable valueOne = getComparable(rowOne, columnIdentifier);
      final Comparable valueTwo = getComparable(rowTwo, columnIdentifier);
      final int comparison;
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
        comparison = columnComparators.computeIfAbsent(columnIdentifier,
                k -> initializeColumnComparator(columnIdentifier)).compare(valueOne, valueTwo);
      }
      if (comparison != 0) {
        return directive == SortingDirective.DESCENDING ? -comparison : comparison;
      }

      return 0;
    }

    private List<Map.Entry<C, SortingState>> getSortingStatesOrderedByPriority() {
      return sortingStates.entrySet().stream().filter(entry -> !EMPTY_SORTING_STATE.equals(entry.getValue())).sorted((o1, o2) -> {
        final Integer priorityOne = o1.getValue().getPriority();
        final Integer priorityTwo = o2.getValue().getPriority();

        return priorityOne.compareTo(priorityTwo);
      }).collect(Collectors.toList());
    }
  }

  private static final class DefaultSortingState implements SortingState {

    private final SortingDirective direction;
    private final int priority;

    private DefaultSortingState(final SortingDirective direction, final int priority) {
      Objects.requireNonNull(direction, "direction");
      this.direction = direction;
      this.priority = priority;
    }

    @Override
    public int getPriority() {
      return priority;
    }

    @Override
    public SortingDirective getDirective() {
      return direction;
    }
  }
}
