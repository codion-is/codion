/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnFilterModel;
import is.codion.common.model.table.ColumnSummaryModel;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;
import is.codion.common.model.table.DefaultColumnSummaryModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.model.worker.ProgressWorker;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A TableModel implementation that supports filtering, searching and sorting.
 * <pre>
 * DefaultFilteredTableModel tableModel = ...;
 * JTable table = new JTable(tableModel, tableModel.getColumnModel(), tableModel.getSelectionModel());
 * </pre><br>
 * User: Björn Darri<br>
 * Date: 18.4.2010<br>
 * Time: 09:48:07<br>
 * @param <R> the type representing the rows in this table model
 * @param <C> the type used to identify columns in this table model, Integer for indexed identification for example
 */
public class DefaultFilteredTableModel<R, C> extends AbstractTableModel implements FilteredTableModel<R, C> {

  private static final String COLUMN_IDENTIFIER = "columnIdentifier";

  private final Event<?> filterEvent = Event.event();
  private final Event<?> sortEvent = Event.event();
  private final Event<Throwable> refreshFailedEvent = Event.event();
  private final Event<?> refreshEvent = Event.event();
  private final Event<?> tableDataChangedEvent = Event.event();
  private final Event<?> tableModelClearedEvent = Event.event();
  private final Event<Removal> rowsRemovedEvent = Event.event();
  private final State refreshingState = State.state();

  private final ColumnValueProvider<R, C> columnValueProvider;

  /**
   * Holds visible items
   */
  private final List<R> visibleItems = new ArrayList<>();

  /**
   * Holds items that are filtered
   */
  private final List<R> filteredItems = new ArrayList<>();

  /**
   * The selection model
   */
  private final FilteredTableSelectionModel<R> selectionModel;

  /**
   * The TableColumnModel
   */
  private final FilteredTableColumnModel<C> columnModel;

  /**
   * The sort model
   */
  private final FilteredTableSortModel<R, C> sortModel;

  /**
   * The search model
   */
  private final FilteredTableSearchModel searchModel;

  /**
   * The ColumnFilterModels used for filtering
   */
  private final Map<C, ColumnFilterModel<R, C, ?>> columnFilterModels = new HashMap<>();

  /**
   * Maps PropertySummaryModels to their respective properties
   */
  private final Map<C, ColumnSummaryModel> columnSummaryModels = new HashMap<>();

  /**
   * the include condition used by this model
   */
  private Predicate<R> includeCondition;

  /**
   * true if refresh should merge, in order to not clear the selection during refresh
   */
  private boolean mergeOnRefresh = false;

  /**
   * If true then refreshing is performed off the EDT using a {@link ProgressWorker}.
   */
  private boolean asyncRefresh = FilteredModel.ASYNC_REFRESH.get();

  /**
   * Instantiates a new table model.
   * @param tableColumns the table columns to base this table model on
   * @param columnValueProvider the column value provider
   */
  public DefaultFilteredTableModel(List<TableColumn> tableColumns,
                                   ColumnValueProvider<R, C> columnValueProvider) {
    this(tableColumns, columnValueProvider, null);
  }

  /**
   * Instantiates a new table model.
   * @param tableColumns the table columns to base this table model on
   * @param columnValueProvider the column value provider
   * @param columnFilterModels the filter models if any
   */
  public DefaultFilteredTableModel(List<TableColumn> tableColumns,
                                   ColumnValueProvider<R, C> columnValueProvider,
                                   Collection<? extends ColumnFilterModel<R, C, ?>> columnFilterModels) {
    this.columnModel = new DefaultFilteredTableColumnModel<>(tableColumns);
    this.searchModel = new DefaultFilteredTableSearchModel<>(this);
    this.columnValueProvider = requireNonNull(columnValueProvider);
    this.sortModel = new DefaultFilteredTableSortModel<>(columnValueProvider);
    this.selectionModel = new DefaultFilteredTableSelectionModel<>(this);
    if (columnFilterModels != null) {
      for (ColumnFilterModel<R, C, ?> columnFilterModel : columnFilterModels) {
        this.columnFilterModels.put(columnFilterModel.getColumnIdentifier(), columnFilterModel);
      }
    }
    this.includeCondition = new DefaultIncludeCondition<>(columnFilterModels);
    bindEventsInternal();
  }

  @Override
  public final List<R> getItems() {
    List<R> items = new ArrayList<>(visibleItems);
    items.addAll(filteredItems);

    return unmodifiableList(items);
  }

  @Override
  public final List<R> getVisibleItems() {
    return unmodifiableList(visibleItems);
  }

  @Override
  public final List<R> getFilteredItems() {
    return unmodifiableList(filteredItems);
  }

  @Override
  public final int getVisibleItemCount() {
    return getRowCount();
  }

  @Override
  public final int getFilteredItemCount() {
    return filteredItems.size();
  }

  @Override
  public final int getColumnCount() {
    return columnModel.getColumnCount();
  }

  @Override
  public final int getRowCount() {
    return visibleItems.size();
  }

  @Override
  public final boolean containsItem(R item) {
    return visibleItems.contains(item) || filteredItems.contains(item);
  }

  @Override
  public final boolean isVisible(R item) {
    return visibleItems.contains(item);
  }

  @Override
  public final boolean isFiltered(R item) {
    return filteredItems.contains(item);
  }

  /**
   * {@inheritDoc}
   * @see #refreshItems()
   */
  @Override
  public final void refresh() {
    if (asyncRefresh && SwingUtilities.isEventDispatchThread()) {
      refreshAsync();
    }
    else {
      refreshSync();
    }
  }

  @Override
  public final StateObserver getRefreshingObserver() {
    return refreshingState.getObserver();
  }

  @Override
  public final void clear() {
    filteredItems.clear();
    int size = visibleItems.size();
    if (size > 0) {
      visibleItems.clear();
      fireTableRowsDeleted(0, size - 1);
    }
    tableModelClearedEvent.onEvent();
  }

  @Override
  public final FilteredTableSelectionModel<R> getSelectionModel() {
    return selectionModel;
  }

  @Override
  public final FilteredTableSortModel<R, C> getSortModel() {
    return sortModel;
  }

  @Override
  public final FilteredTableSearchModel getSearchModel() {
    return searchModel;
  }

  @Override
  public final Optional<ColumnSummaryModel> getColumnSummaryModel(C columnIdentifier) {
    return Optional.ofNullable(columnSummaryModels.computeIfAbsent(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER), identifier ->
            createColumnValueProvider(columnIdentifier)
                    .map(DefaultColumnSummaryModel::new)
                    .orElse(null)));
  }

  @Override
  public final Map<C, ColumnFilterModel<R, C, ?>> getColumnFilterModels() {
    return unmodifiableMap(columnFilterModels);
  }

  @Override
  public final <T> ColumnFilterModel<R, C, T> getColumnFilterModel(C columnIdentifier) {
    ColumnFilterModel<R, C, T> filterModel = (ColumnFilterModel<R, C, T>) columnFilterModels.get(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
    if (filterModel == null) {
      throw new IllegalArgumentException("No filter model exists for column: " + columnIdentifier);
    }

    return filterModel;
  }

  @Override
  public final <T> Collection<T> getValues(C columnIdentifier) {
    return (Collection<T>) getColumnValues(IntStream.range(0, getVisibleItemCount()).boxed(),
            columnModel.getTableColumn(columnIdentifier).getModelIndex());
  }

  @Override
  public final <T> Collection<T> getSelectedValues(C columnIdentifier) {
    return (Collection<T>) getColumnValues(getSelectionModel().getSelectedIndexes().stream(),
            columnModel.getTableColumn(columnIdentifier).getModelIndex());
  }

  @Override
  public final boolean isMergeOnRefresh() {
    return mergeOnRefresh;
  }

  @Override
  public final void setMergeOnRefresh(boolean mergeOnRefresh) {
    this.mergeOnRefresh = mergeOnRefresh;
  }

  @Override
  public final boolean isAsyncRefresh() {
    return asyncRefresh;
  }

  @Override
  public final void setAsyncRefresh(boolean asyncRefresh) {
    this.asyncRefresh = asyncRefresh;
  }

  @Override
  public final R getItemAt(int rowIndex) {
    return visibleItems.get(rowIndex);
  }

  @Override
  public final int indexOf(R item) {
    return visibleItems.indexOf(item);
  }

  @Override
  public final void sort() {
    if (sortModel.isSortingEnabled()) {
      List<R> selectedItems = selectionModel.getSelectedItems();
      sortModel.sort(visibleItems);
      fireTableRowsUpdated(0, visibleItems.size());
      selectionModel.setSelectedItems(selectedItems);
      sortEvent.onEvent();
    }
  }

  @Override
  public final void filterContents() {
    List<R> selectedItems = selectionModel.getSelectedItems();
    visibleItems.addAll(filteredItems);
    filteredItems.clear();
    if (includeCondition != null) {
      for (ListIterator<R> iterator = visibleItems.listIterator(); iterator.hasNext(); ) {
        R item = iterator.next();
        if (!includeCondition.test(item)) {
          filteredItems.add(item);
          iterator.remove();
        }
      }
    }
    sortModel.sort(visibleItems);
    fireTableDataChanged();
    selectionModel.setSelectedItems(selectedItems);
    filterEvent.onEvent();
  }

  @Override
  public final Predicate<R> getIncludeCondition() {
    return includeCondition;
  }

  @Override
  public final void setIncludeCondition(Predicate<R> includeCondition) {
    this.includeCondition = includeCondition;
    filterContents();
  }

  @Override
  public final void removeItem(R item) {
    int visibleItemIndex = visibleItems.indexOf(item);
    if (visibleItemIndex >= 0) {
      visibleItems.remove(visibleItemIndex);
      fireTableRowsDeleted(visibleItemIndex, visibleItemIndex);
    }
    else {
      int filteredIndex = filteredItems.indexOf(item);
      if (filteredIndex >= 0) {
        filteredItems.remove(item);
      }
    }
  }

  @Override
  public final void removeItems(Collection<R> items) {
    for (R item : items) {
      removeItem(item);
    }
  }

  @Override
  public final void removeItemAt(int index) {
    visibleItems.remove(index);
    fireTableRowsDeleted(index, index);
  }

  @Override
  public final void removeItems(int fromIndex, int toIndex) {
    visibleItems.subList(fromIndex, toIndex).clear();
    fireTableRowsDeleted(fromIndex, toIndex);
  }

  /**
   * A default implementation returning true
   * @return true
   */
  @Override
  public boolean allowSelectionChange() {
    return true;
  }

  @Override
  public final void addRowsRemovedListener(EventDataListener<Removal> listener) {
    rowsRemovedEvent.addDataListener(listener);
  }

  @Override
  public final void removeRowsRemovedListener(EventDataListener<Removal> listener) {
    rowsRemovedEvent.removeDataListener(listener);
  }

  @Override
  public final FilteredTableColumnModel<C> getColumnModel() {
    return columnModel;
  }

  @Override
  public final Class<?> getColumnClass(C columnIdentifier) {
    return columnValueProvider.getColumnClass(columnIdentifier);
  }

  @Override
  public final Class<?> getColumnClass(int columnIndex) {
    return getColumnClass(getColumnModel().getColumnIdentifier(columnIndex));
  }

  @Override
  public final Object getValueAt(int rowIndex, int columnIndex) {
    return columnValueProvider.getValue(getItemAt(rowIndex), getColumnModel().getColumnIdentifier(columnIndex));
  }

  @Override
  public final String getStringAt(int rowIndex, C columnIdentifier) {
    return columnValueProvider.getString(getItemAt(rowIndex), columnIdentifier);
  }

  @Override
  public final void addRefreshListener(EventListener listener) {
    refreshEvent.addListener(listener);
  }

  @Override
  public final void removeRefreshListener(EventListener listener) {
    refreshEvent.removeListener(listener);
  }

  @Override
  public final void addRefreshFailedListener(EventDataListener<Throwable> listener) {
    refreshFailedEvent.addDataListener(listener);
  }

  @Override
  public final void removeRefreshFailedListener(EventDataListener<Throwable> listener) {
    refreshFailedEvent.removeDataListener(listener);
  }

  @Override
  public final void addFilterListener(EventListener listener) {
    filterEvent.addListener(listener);
  }

  @Override
  public final void removeFilterListener(EventListener listener) {
    filterEvent.removeListener(listener);
  }

  @Override
  public final void addSortListener(EventListener listener) {
    sortEvent.addListener(listener);
  }

  @Override
  public final void removeSortListener(EventListener listener) {
    sortEvent.removeListener(listener);
  }

  @Override
  public final void addTableDataChangedListener(EventListener listener) {
    tableDataChangedEvent.addListener(listener);
  }

  @Override
  public final void removeTableDataChangedListener(EventListener listener) {
    tableDataChangedEvent.removeListener(listener);
  }

  @Override
  public final void addTableModelClearedListener(EventListener listener) {
    tableModelClearedEvent.addListener(listener);
  }

  @Override
  public final void removeTableModelClearedListener(EventListener listener) {
    tableModelClearedEvent.removeListener(listener);
  }

  /**
   * Returns the items this table model should contain.
   * By default, this simply returns the items already in the model.
   * Override to fetch data from a datasource of some kind.
   * @return the items this table model should contain, an empty Collection in case of no items.
   */
  protected Collection<R> refreshItems() {
    return getItems();
  }

  /**
   * Creates a ColumnValueProvider for the given column
   * @param columnIdentifier the column identifier
   * @param <T> the value type
   * @return a ColumnValueProvider for the column identified by {@code columnIdentifier}, an empty Optional if not applicable
   */
  protected <T extends Number> Optional<SummaryValueProvider<T>> createColumnValueProvider(C columnIdentifier) {
    return Optional.of(new DefaultSummaryValueProvider<>(columnIdentifier, this, null));
  }

  /**
   * Adds the given items to the bottom of this table model.
   * @param items the items to add
   */
  protected final void addItems(Collection<R> items) {
    addItemsAt(visibleItems.size(), items);
  }

  /**
   * Adds the given items to the bottom of this table model.
   * If sorting is enabled this model is sorted after the items have been added.
   * @param items the items to add
   */
  protected final void addItemsSorted(Collection<R> items) {
    addItemsAtSorted(visibleItems.size(), items);
  }

  /**
   * Adds the given items to this table model, non-filtered items are added at the given index.
   * @param index the index at which to add the items
   * @param items the items to add
   */
  protected final void addItemsAt(int index, Collection<R> items) {
    if (addItemsAtInternal(index, items)) {
      fireTableDataChanged();
    }
  }

  /**
   * Adds the given items to this table model, non-filtered items are added at the given index.
   * If sorting is enabled this model is sorted after the items have been added.
   * @param index the index at which to add the items
   * @param items the items to add
   * @see FilteredTableSortModel#isSortingEnabled()
   */
  protected final void addItemsAtSorted(int index, Collection<R> items) {
    if (addItemsAtInternal(index, items)) {
      if (sortModel.isSortingEnabled()) {
        sortModel.sort(visibleItems);
      }
      fireTableDataChanged();
    }
  }

  /**
   * Adds the given item to the bottom of this table model.
   * @param item the item to add
   */
  protected final void addItem(R item) {
    addItemInternal(item);
  }

  /**
   * Adds the given item to the bottom of this table model.
   * If sorting is enabled this model is sorted after the item has been added.
   * @param item the item to add
   */
  protected final void addItemSorted(R item) {
    if (addItemInternal(item)) {
      if (sortModel.isSortingEnabled()) {
        sortModel.sort(visibleItems);
      }
      fireTableDataChanged();
    }
  }

  /**
   * Sets the item at the given index.
   * If the item should be filtered calling this method has no effect.
   * @param index the index
   * @param item the item
   * @see #setIncludeCondition(Predicate)
   */
  protected final void setItemAt(int index, R item) {
    requireNonNull(item, "item");
    if (include(item)) {
      visibleItems.set(index, item);
      fireTableRowsUpdated(index, index);
    }
  }

  private void bindEventsInternal() {
    addTableModelListener(e -> tableDataChangedEvent.onEvent());
    for (ColumnConditionModel<C, ?> conditionModel : columnFilterModels.values()) {
      conditionModel.addConditionChangedListener(this::filterContents);
    }
    sortModel.addSortingChangedListener(columnIdentifier -> sort());
    addTableModelListener(e -> {
      if (e.getType() == TableModelEvent.DELETE) {
        rowsRemovedEvent.onEvent(new DefaultRemoval(e.getFirstRow(), e.getLastRow()));
      }
    });
  }

  private List<Object> getColumnValues(Stream<Integer> rowIndexStream, int columnModelIndex) {
    return rowIndexStream.map(rowIndex -> getValueAt(rowIndex, columnModelIndex)).collect(toList());
  }

  private boolean addItemInternal(R item) {
    if (include(item)) {
      visibleItems.add(item);

      return true;
    }
    filteredItems.add(item);

    return false;
  }

  private boolean addItemsAtInternal(int index, Collection<R> items) {
    requireNonNull(items);
    boolean visible = false;
    int counter = 0;
    for (R item : items) {
      if (include(item)) {
        visible = true;
        visibleItems.add(index + counter++, item);
      }
      else {
        filteredItems.add(item);
      }
    }

    return visible;
  }

  private boolean include(R item) {
    return includeCondition == null || includeCondition.test(item);
  }

  private void refreshAsync() {
    ProgressWorker.builder(this::refreshItems)
            .onStarted(this::onRefreshStarted)
            .onResult(this::onRefreshResult)
            .onException(this::onRefreshFailed)
            .execute();
  }

  private void refreshSync() {
    onRefreshStarted();
    try {
      onRefreshResult(refreshItems());
    }
    catch (Exception e) {
      onRefreshFailed(e);
    }
  }

  private void onRefreshStarted() {
    refreshingState.set(true);
  }

  private void onRefreshFailed(Throwable throwable) {
    refreshingState.set(false);
    refreshFailedEvent.onEvent(throwable);
  }

  private void onRefreshResult(Collection<R> items) {
    refreshingState.set(false);
    if (mergeOnRefresh && !items.isEmpty()) {
      merge(items);
    }
    else {
      clearAndAdd(items);
    }
    refreshEvent.onEvent();
  }

  private void merge(Collection<R> items) {
    Set<R> itemSet = new HashSet<>(items);
    getItems().forEach(item -> {
      if (!itemSet.contains(item)) {
        removeItem(item);
      }
    });
    items.forEach(item -> {
      int index = indexOf(item);
      if (index == -1) {
        addItemSorted(item);
      }
      else {
        setItemAt(index, item);
      }
    });
  }

  private void clearAndAdd(Collection<R> items) {
    Collection<R> selectedItems = selectionModel.getSelectedItems();
    clear();
    addItemsSorted(items);
    selectionModel.setSelectedItems(selectedItems);
  }

  private static final class DefaultIncludeCondition<R, C> implements Predicate<R> {

    private final Collection<? extends ColumnFilterModel<R, C, ?>> columnFilters;

    private DefaultIncludeCondition(Collection<? extends ColumnFilterModel<R, C, ?>> columnFilters) {
      this.columnFilters = columnFilters;
    }

    @Override
    public boolean test(R item) {
      return columnFilters == null || columnFilters.stream()
              .allMatch(columnFilter -> columnFilter.include(item));
    }
  }

  private static final class DefaultRemoval implements Removal {

    private final int fromRow;
    private final int toRow;

    private DefaultRemoval(int fromRow, int toRow) {
      this.fromRow = fromRow;
      this.toRow = toRow;
    }

    @Override
    public int getFromRow() {
      return fromRow;
    }

    @Override
    public int getToRow() {
      return toRow;
    }
  }

  /**
   * A default SummaryValueProvider implementation
   */
  protected static final class DefaultSummaryValueProvider<T extends Number, C> implements SummaryValueProvider<T> {

    private final C columnIdentifier;
    private final FilteredTableModel<?, C> tableModel;
    private final Format format;

    /**
     * @param columnIdentifier the identifier of the column which values are provided
     * @param tableModel the table model
     * @param format the format to use for presenting the summary value
     */
    public DefaultSummaryValueProvider(C columnIdentifier, FilteredTableModel<?, C> tableModel, Format format) {
      this.columnIdentifier = requireNonNull(columnIdentifier);
      this.tableModel = requireNonNull(tableModel);
      this.format = format;
    }

    @Override
    public String format(Object value) {
      return format == null ? value.toString() : format.format(value);
    }

    @Override
    public void addValuesChangedListener(EventListener listener) {
      tableModel.addTableDataChangedListener(listener);
      tableModel.getSelectionModel().addSelectionChangedListener(listener);
    }

    @Override
    public Collection<T> getValues() {
      return isValueSubset() ? tableModel.getSelectedValues(columnIdentifier) : tableModel.getValues(columnIdentifier);
    }

    @Override
    public boolean isValueSubset() {
      return tableModel.getSelectionModel().isSelectionNotEmpty();
    }
  }
}
