/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnSummaryModel;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.model.worker.ProgressWorker;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A TableModel implementation that supports filtering, searching and sorting.
 * <pre>
 * DefaultFilteredTableModel tableModel = ...;
 * JTable table = new JTable(tableModel, tableModel.columnModel(), tableModel.selectionModel());
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
  private final Event<?> dataChangedEvent = Event.event();
  private final Event<?> clearEvent = Event.event();
  private final Event<RemovedRows> rowsRemovedEvent = Event.event();
  private final State refreshingState = State.state();
  private final ColumnValueProvider<R, C> columnValueProvider;
  private final List<R> visibleItems = new ArrayList<>();
  private final List<R> filteredItems = new ArrayList<>();
  private final FilteredTableSelectionModel<R> selectionModel;
  private final FilteredTableColumnModel<C> columnModel;
  private final FilteredTableSortModel<R, C> sortModel;
  private final FilteredTableSearchModel searchModel;
  private final Map<C, ColumnConditionModel<? extends C, ?>> columnFilterModels;
  private final Map<C, ColumnSummaryModel> columnSummaryModels = new HashMap<>();
  private final CombinedIncludeCondition combinedIncludeCondition;

  private ProgressWorker<Collection<R>, ?> refreshWorker;
  private boolean mergeOnRefresh = false;
  private boolean asyncRefresh = FilteredModel.ASYNC_REFRESH.get();

  /**
   * Instantiates a new table model.
   * @param tableColumns the table columns to base this table model on
   * @param columnValueProvider the column value provider
   * @throws IllegalArgumentException in case {@code tableColumns} is empty
   * @throws NullPointerException in case {@code tableColumns} or {@code columnValueProvider} is null
   */
  public DefaultFilteredTableModel(List<FilteredTableColumn<C>> tableColumns, ColumnValueProvider<R, C> columnValueProvider) {
    this(tableColumns, columnValueProvider, null);
  }

  /**
   * Instantiates a new table model.
   * @param tableColumns the table columns to base this table model on
   * @param columnValueProvider the column value provider
   * @param columnFilterModels the filter models if any, may be null
   * @throws IllegalArgumentException in case {@code tableColumns} is empty
   * @throws NullPointerException in case {@code tableColumns} or {@code columnValueProvider} is null
   */
  public DefaultFilteredTableModel(List<FilteredTableColumn<C>> tableColumns, ColumnValueProvider<R, C> columnValueProvider,
                                   Collection<? extends ColumnConditionModel<? extends C, ?>> columnFilterModels) {
    this.columnModel = new DefaultFilteredTableColumnModel<>(tableColumns);
    this.searchModel = new DefaultFilteredTableSearchModel<>(this);
    this.columnValueProvider = requireNonNull(columnValueProvider);
    this.sortModel = new DefaultFilteredTableSortModel<>(columnValueProvider);
    this.selectionModel = new DefaultFilteredTableSelectionModel<>(this);
    this.columnFilterModels = initializeColumnFilterModels(columnFilterModels);
    this.combinedIncludeCondition = new CombinedIncludeCondition(columnFilterModels);
    bindEventsInternal();
  }

  @Override
  public final List<R> items() {
    List<R> items = new ArrayList<>(visibleItems);
    items.addAll(filteredItems);

    return unmodifiableList(items);
  }

  @Override
  public final List<R> visibleItems() {
    return unmodifiableList(visibleItems);
  }

  @Override
  public final List<R> filteredItems() {
    return unmodifiableList(filteredItems);
  }

  @Override
  public final int visibleItemCount() {
    return getRowCount();
  }

  @Override
  public final int filteredItemCount() {
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
    refreshThen(null);
  }

  @Override
  public final void refreshThen(Consumer<Collection<R>> afterRefresh) {
    if (asyncRefresh && SwingUtilities.isEventDispatchThread()) {
      refreshAsync(afterRefresh);
    }
    else {
      refreshSync(afterRefresh);
    }
  }

  @Override
  public final StateObserver refreshingObserver() {
    return refreshingState.observer();
  }

  @Override
  public final void clear() {
    filteredItems.clear();
    int size = visibleItems.size();
    if (size > 0) {
      visibleItems.clear();
      fireTableRowsDeleted(0, size - 1);
    }
    clearEvent.onEvent();
  }

  @Override
  public final FilteredTableSelectionModel<R> selectionModel() {
    return selectionModel;
  }

  @Override
  public final FilteredTableSortModel<R, C> sortModel() {
    return sortModel;
  }

  @Override
  public final FilteredTableSearchModel searchModel() {
    return searchModel;
  }

  @Override
  public final Optional<ColumnSummaryModel> columnSummaryModel(C columnIdentifier) {
    return Optional.ofNullable(columnSummaryModels.computeIfAbsent(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER), identifier ->
            createColumnValueProvider(columnIdentifier)
                    .map(ColumnSummaryModel::columnSummaryModel)
                    .orElse(null)));
  }

  @Override
  public final Map<C, ColumnConditionModel<? extends C, ?>> columnFilterModels() {
    return columnFilterModels;
  }

  @Override
  public final <T> ColumnConditionModel<C, T> columnFilterModel(C columnIdentifier) {
    ColumnConditionModel<C, T> filterModel = (ColumnConditionModel<C, T>) columnFilterModels.get(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
    if (filterModel == null) {
      throw new IllegalArgumentException("No filter model exists for column: " + columnIdentifier);
    }

    return filterModel;
  }

  @Override
  public final <T> Collection<T> values(C columnIdentifier) {
    return (Collection<T>) columnValues(IntStream.range(0, visibleItemCount()).boxed(),
            columnModel.tableColumn(columnIdentifier).getModelIndex());
  }

  @Override
  public final <T> Collection<T> selectedValues(C columnIdentifier) {
    return (Collection<T>) columnValues(selectionModel().getSelectedIndexes().stream(),
            columnModel.tableColumn(columnIdentifier).getModelIndex());
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
  public final R itemAt(int rowIndex) {
    return visibleItems.get(rowIndex);
  }

  @Override
  public final int indexOf(R item) {
    return visibleItems.indexOf(item);
  }

  @Override
  public final void sortItems() {
    if (sortModel.isSortingEnabled()) {
      List<R> selectedItems = selectionModel.getSelectedItems();
      sortModel.sort(visibleItems);
      fireTableRowsUpdated(0, visibleItems.size());
      selectionModel.setSelectedItems(selectedItems);
      sortEvent.onEvent();
    }
  }

  @Override
  public final void filterItems() {
    List<R> selectedItems = selectionModel.getSelectedItems();
    visibleItems.addAll(filteredItems);
    filteredItems.clear();
    for (ListIterator<R> visibleItemsIterator = visibleItems.listIterator(); visibleItemsIterator.hasNext();) {
      R item = visibleItemsIterator.next();
      if (!include(item)) {
        visibleItemsIterator.remove();
        filteredItems.add(item);
      }
    }
    sortModel.sort(visibleItems);
    fireTableDataChanged();
    selectionModel.setSelectedItems(selectedItems);
    filterEvent.onEvent();
  }

  @Override
  public final Predicate<R> getIncludeCondition() {
    return combinedIncludeCondition.includeCondition;
  }

  @Override
  public final void setIncludeCondition(Predicate<R> includeCondition) {
    combinedIncludeCondition.includeCondition = includeCondition;
    filterItems();
  }

  @Override
  public final void addItems(Collection<R> items) {
    addItemsAt(visibleItems.size(), items);
  }

  @Override
  public final void addItemsSorted(Collection<R> items) {
    addItemsAtSorted(visibleItems.size(), items);
  }

  @Override
  public final void addItemsAt(int index, Collection<R> items) {
    if (addItemsAtInternal(index, items)) {
      fireTableDataChanged();
    }
  }

  @Override
  public final void addItemsAtSorted(int index, Collection<R> items) {
    if (addItemsAtInternal(index, items)) {
      if (sortModel.isSortingEnabled()) {
        sortModel.sort(visibleItems);
      }
      fireTableDataChanged();
    }
  }

  @Override
  public final void addItem(R item) {
    addItemInternal(item);
  }

  @Override
  public final void addItemSorted(R item) {
    if (addItemInternal(item)) {
      if (sortModel.isSortingEnabled()) {
        sortModel.sort(visibleItems);
      }
      fireTableDataChanged();
    }
  }

  @Override
  public final void setItemAt(int index, R item) {
    validate(item);
    if (include(item)) {
      visibleItems.set(index, item);
      fireTableRowsUpdated(index, index);
    }
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
  public final void addRowsRemovedListener(EventDataListener<RemovedRows> listener) {
    rowsRemovedEvent.addDataListener(listener);
  }

  @Override
  public final void removeRowsRemovedListener(EventDataListener<RemovedRows> listener) {
    rowsRemovedEvent.removeDataListener(listener);
  }

  @Override
  public final FilteredTableColumnModel<C> columnModel() {
    return columnModel;
  }

  @Override
  public final Class<?> getColumnClass(C columnIdentifier) {
    return columnValueProvider.columnClass(columnIdentifier);
  }

  @Override
  public final Class<?> getColumnClass(int columnIndex) {
    return getColumnClass(columnModel().columnIdentifier(columnIndex));
  }

  @Override
  public final Object getValueAt(int rowIndex, int columnIndex) {
    return columnValueProvider.value(itemAt(rowIndex), columnModel().columnIdentifier(columnIndex));
  }

  @Override
  public final String getStringValueAt(int rowIndex, C columnIdentifier) {
    return columnValueProvider.string(itemAt(rowIndex), columnIdentifier);
  }

  @Override
  public final String rowsAsDelimitedString(char delimiter) {
    List<Integer> rows = selectionModel.isSelectionEmpty() ?
            IntStream.range(0, getRowCount())
                    .boxed()
                    .collect(toList()) :
            selectionModel.getSelectedIndexes();

    List<FilteredTableColumn<C>> visibleColumns = columnModel().visibleColumns();

    return Text.delimitedString(visibleColumns.stream()
            .map(column -> String.valueOf(column.getHeaderValue()))
            .collect(toList()), rows.stream()
            .map(row -> stringValues(row, visibleColumns))
            .collect(toList()), String.valueOf(delimiter));
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
  public final void addDataChangedListener(EventListener listener) {
    dataChangedEvent.addListener(listener);
  }

  @Override
  public final void removeDataChangedListener(EventListener listener) {
    dataChangedEvent.removeListener(listener);
  }

  @Override
  public final void addClearListener(EventListener listener) {
    clearEvent.addListener(listener);
  }

  @Override
  public final void removeClearListener(EventListener listener) {
    clearEvent.removeListener(listener);
  }

  /**
   * Returns the items this table model should contain.
   * By default, this simply returns the items already in the model.
   * Override to fetch data from a datasource of some kind.
   * @return the items this table model should contain, an empty Collection in case of no items.
   */
  protected Collection<R> refreshItems() {
    return items();
  }

  /**
   * Override to validate that the given item can be added to this model.
   * @param item the item being added
   * @return true if the item can be added to this model, false otherwise
   */
  protected boolean validItem(R item) {
    return true;
  }

  /**
   * Creates a ColumnValueProvider for the given column
   * @param columnIdentifier the column identifier
   * @param <T> the value type
   * @return a ColumnValueProvider for the column identified by the given identifier, an empty Optional if not applicable
   */
  protected <T extends Number> Optional<SummaryValueProvider<T>> createColumnValueProvider(C columnIdentifier) {
    return Optional.of(new DefaultSummaryValueProvider<>(columnIdentifier, this, null));
  }

  private void bindEventsInternal() {
    addTableModelListener(e -> dataChangedEvent.onEvent());
    columnFilterModels.values().forEach(conditionModel ->
            conditionModel.addConditionChangedListener(this::filterItems));
    sortModel.addSortingChangedListener(columnIdentifier -> sortItems());
    addTableModelListener(e -> {
      if (e.getType() == TableModelEvent.DELETE) {
        rowsRemovedEvent.onEvent(new DefaultRemovedRows(e.getFirstRow(), e.getLastRow()));
      }
    });
  }

  private List<Object> columnValues(Stream<Integer> rowIndexStream, int columnModelIndex) {
    return rowIndexStream.map(rowIndex -> getValueAt(rowIndex, columnModelIndex)).collect(toList());
  }

  private List<String> stringValues(int row, List<FilteredTableColumn<C>> columns) {
    return columns.stream()
            .map(column -> getStringValueAt(row, column.getIdentifier()))
            .collect(toList());
  }

  private boolean addItemInternal(R item) {
    validate(item);
    if (include(item)) {
      visibleItems.add(item);

      return true;
    }
    filteredItems.add(item);

    return false;
  }

  private boolean addItemsAtInternal(int index, Collection<R> items) {
    requireNonNull(items).forEach(this::validate);
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

  private void validate(R item) {
    requireNonNull(item);
    if (!validItem(item)) {
      throw new IllegalArgumentException("Invalid item: " + item);
    }
  }

  private boolean include(R item) {
    return combinedIncludeCondition.test(item);
  }

  private void refreshAsync(Consumer<Collection<R>> afterRefresh) {
    cancelCurrentRefresh();
    refreshWorker = ProgressWorker.builder(this::refreshItems)
            .onStarted(this::onRefreshStarted)
            .onResult(items -> onRefreshResult(items, afterRefresh))
            .onException(this::onRefreshFailedAsync)
            .execute();
  }

  private void refreshSync(Consumer<Collection<R>> afterRefresh) {
    onRefreshStarted();
    try {
      onRefreshResult(refreshItems(), afterRefresh);
    }
    catch (Exception e) {
      onRefreshFailedSync(e);
    }
  }

  private void onRefreshStarted() {
    refreshingState.set(true);
  }

  private void onRefreshFailedAsync(Throwable throwable) {
    refreshWorker = null;
    refreshingState.set(false);
    refreshFailedEvent.onEvent(throwable);
  }

  private void onRefreshFailedSync(Throwable throwable) {
    refreshingState.set(false);
    if (throwable instanceof RuntimeException) {
      throw (RuntimeException) throwable;
    }

    throw new RuntimeException(throwable);
  }

  private void onRefreshResult(Collection<R> items, Consumer<Collection<R>> afterRefresh) {
    refreshWorker = null;
    refreshingState.set(false);
    if (mergeOnRefresh && !items.isEmpty()) {
      merge(items);
    }
    else {
      clearAndAdd(items);
    }
    if (afterRefresh != null) {
      afterRefresh.accept(items);
    }
    refreshEvent.onEvent();
  }

  private void cancelCurrentRefresh() {
    ProgressWorker<?, ?> worker = refreshWorker;
    if (worker != null) {
      worker.cancel(true);
    }
  }

  private void merge(Collection<R> items) {
    Set<R> itemSet = new HashSet<>(items);
    items().forEach(item -> {
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

  private Map<C, ColumnConditionModel<? extends C, ?>> initializeColumnFilterModels(Collection<? extends ColumnConditionModel<? extends C, ?>> filterModels) {
    if (filterModels == null) {
      return emptyMap();
    }

    Map<C, ColumnConditionModel<? extends C, ?>> filterMap = new HashMap<>();
    for (ColumnConditionModel<? extends C, ?> columnFilterModel : filterModels) {
      filterMap.put(columnFilterModel.columnIdentifier(), columnFilterModel);
    }

    return unmodifiableMap(filterMap);
  }

  private final class CombinedIncludeCondition implements Predicate<R> {

    private final List<? extends ColumnConditionModel<? extends C, ?>> columnFilters;

    private Predicate<R> includeCondition;

    private CombinedIncludeCondition(Collection<? extends ColumnConditionModel<? extends C, ?>> columnFilters) {
      this.columnFilters = columnFilters == null ? Collections.emptyList() : new ArrayList<>(columnFilters);
    }

    @Override
    public boolean test(R item) {
      for (int i = 0; i < columnFilters.size(); i++) {
        ColumnConditionModel<? extends C, ?> conditionModel = columnFilters.get(i);
        if (conditionModel.isEnabled() && !conditionModel.accepts(columnValueProvider.comparable(item, conditionModel.columnIdentifier()))) {
          return false;
        }
      }

      return includeCondition == null || includeCondition.test(item);
    }
  }

  private static final class DefaultRemovedRows implements RemovedRows {

    private final int fromRow;
    private final int toRow;

    private DefaultRemovedRows(int fromRow, int toRow) {
      this.fromRow = fromRow;
      this.toRow = toRow;
    }

    @Override
    public int fromRow() {
      return fromRow;
    }

    @Override
    public int toRow() {
      return toRow;
    }
  }

  /**
   * A default SummaryValueProvider implementation
   */
  public static final class DefaultSummaryValueProvider<T extends Number, C> implements SummaryValueProvider<T> {

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
    public void addValuesListener(EventListener listener) {
      tableModel.addDataChangedListener(listener);
      tableModel.selectionModel().addSelectionListener(listener);
    }

    @Override
    public ColumnSummaryModel.SummaryValues<T> values() {
      FilteredTableSelectionModel<?> tableSelectionModel = tableModel.selectionModel();
      boolean subset = tableSelectionModel.isSelectionNotEmpty() &&
              tableSelectionModel.selectionCount() != tableModel.visibleItemCount();

      return ColumnSummaryModel.summaryValues(subset ? tableModel.selectedValues(columnIdentifier) : tableModel.values(columnIdentifier), subset);
    }
  }
}
