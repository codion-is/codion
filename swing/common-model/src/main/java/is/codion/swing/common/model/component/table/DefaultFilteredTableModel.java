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
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.model.table.TableSummaryModel;
import is.codion.swing.common.model.component.AbstractFilteredModelRefresher;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static is.codion.common.model.table.TableConditionModel.tableConditionModel;
import static is.codion.common.model.table.TableSummaryModel.tableSummaryModel;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultFilteredTableModel<R, C> extends AbstractTableModel implements FilteredTableModel<R, C> {

  private final Event<?> dataChangedEvent = Event.event();
  private final Event<?> clearEvent = Event.event();
  private final Event<RemovedRows> rowsRemovedEvent = Event.event();
  private final ColumnValueProvider<R, C> columnValueProvider;
  private final List<R> visibleItems = new ArrayList<>();
  private final List<R> filteredItems = new ArrayList<>();
  private final FilteredTableSelectionModel<R> selectionModel;
  private final FilteredTableColumnModel<C> columnModel;
  private final FilteredTableSortModel<R, C> sortModel;
  private final FilteredTableSearchModel searchModel;
  private final TableConditionModel<C> filterModel;
  private final TableSummaryModel<C> summaryModel;
  private final CombinedIncludeCondition combinedIncludeCondition;
  private final Predicate<R> rowValidator;
  private final Refresher<R> refresher;

  private boolean mergeOnRefresh = false;

  private DefaultFilteredTableModel(DefaultBuilder<R, C> builder) {
    this.columnModel = new DefaultFilteredTableColumnModel<>(builder.columns);
    this.searchModel = new DefaultFilteredTableSearchModel<>(this);
    this.columnValueProvider = requireNonNull(builder.columnValueProvider);
    this.sortModel = new DefaultFilteredTableSortModel<>(columnModel, columnValueProvider);
    this.selectionModel = new DefaultFilteredTableSelectionModel<>(this);
    this.filterModel = tableConditionModel(createColumnFilterModels(builder.filterModelFactory == null ?
            new DefaultFilterModelFactory() : builder.filterModelFactory));
    this.summaryModel = tableSummaryModel(builder.summaryValueProviderFactory == null ?
            new DefaultSummaryValueProviderFactory() : builder.summaryValueProviderFactory);
    this.combinedIncludeCondition = new CombinedIncludeCondition(filterModel.conditionModels().values());
    this.refresher = new FilteredTableModelRefresher(builder.rowSupplier == null ? this::items : builder.rowSupplier);
    this.refresher.setAsyncRefresh(builder.asyncRefresh);
    this.rowValidator = builder.rowValidator;
    this.mergeOnRefresh = builder.mergeOnRefresh;
    bindEventsInternal();
  }

  @Override
  public List<R> items() {
    List<R> items = new ArrayList<>(visibleItems);
    items.addAll(filteredItems);

    return unmodifiableList(items);
  }

  @Override
  public List<R> visibleItems() {
    return unmodifiableList(visibleItems);
  }

  @Override
  public List<R> filteredItems() {
    return unmodifiableList(filteredItems);
  }

  @Override
  public int visibleItemCount() {
    return getRowCount();
  }

  @Override
  public int filteredItemCount() {
    return filteredItems.size();
  }

  @Override
  public int getColumnCount() {
    return columnModel.getColumnCount();
  }

  @Override
  public int getRowCount() {
    return visibleItems.size();
  }

  @Override
  public boolean containsItem(R item) {
    return visibleItems.contains(item) || filteredItems.contains(item);
  }

  @Override
  public boolean isVisible(R item) {
    return visibleItems.contains(item);
  }

  @Override
  public boolean isFiltered(R item) {
    return filteredItems.contains(item);
  }

  @Override
  public Refresher<R> refresher() {
    return refresher;
  }

  @Override
  public void refresh() {
    refreshThen(null);
  }

  @Override
  public void refreshThen(Consumer<Collection<R>> afterRefresh) {
    refresher.refreshThen(afterRefresh);
  }

  @Override
  public void clear() {
    filteredItems.clear();
    int size = visibleItems.size();
    if (size > 0) {
      visibleItems.clear();
      fireTableRowsDeleted(0, size - 1);
    }
    clearEvent.onEvent();
  }

  @Override
  public FilteredTableColumnModel<C> columnModel() {
    return columnModel;
  }

  @Override
  public FilteredTableSelectionModel<R> selectionModel() {
    return selectionModel;
  }

  @Override
  public FilteredTableSortModel<R, C> sortModel() {
    return sortModel;
  }

  @Override
  public FilteredTableSearchModel searchModel() {
    return searchModel;
  }

  @Override
  public TableConditionModel<C> filterModel() {
    return filterModel;
  }

  @Override
  public TableSummaryModel<C> summaryModel() {
    return summaryModel;
  }

  @Override
  public <T> Collection<T> values(C columnIdentifier) {
    return (Collection<T>) columnValues(IntStream.range(0, visibleItemCount()).boxed(),
            columnModel.column(columnIdentifier).getModelIndex());
  }

  @Override
  public <T> Collection<T> selectedValues(C columnIdentifier) {
    return (Collection<T>) columnValues(selectionModel().getSelectedIndexes().stream(),
            columnModel.column(columnIdentifier).getModelIndex());
  }

  @Override
  public boolean isMergeOnRefresh() {
    return mergeOnRefresh;
  }

  @Override
  public void setMergeOnRefresh(boolean mergeOnRefresh) {
    this.mergeOnRefresh = mergeOnRefresh;
  }

  @Override
  public R itemAt(int rowIndex) {
    return visibleItems.get(rowIndex);
  }

  @Override
  public int indexOf(R item) {
    return visibleItems.indexOf(item);
  }

  @Override
  public void sortItems() {
    if (sortModel.isSorted()) {
      List<R> selectedItems = selectionModel.getSelectedItems();
      sortModel.sort(visibleItems);
      fireTableRowsUpdated(0, visibleItems.size());
      selectionModel.setSelectedItems(selectedItems);
    }
  }

  @Override
  public void filterItems() {
    List<R> selectedItems = selectionModel.getSelectedItems();
    visibleItems.addAll(filteredItems);
    filteredItems.clear();
    for (ListIterator<R> visibleItemsIterator = visibleItems.listIterator(); visibleItemsIterator.hasNext(); ) {
      R item = visibleItemsIterator.next();
      if (!include(item)) {
        visibleItemsIterator.remove();
        filteredItems.add(item);
      }
    }
    sortModel.sort(visibleItems);
    fireTableDataChanged();
    selectionModel.setSelectedItems(selectedItems);
  }

  @Override
  public Predicate<R> getIncludeCondition() {
    return combinedIncludeCondition.includeCondition;
  }

  @Override
  public void setIncludeCondition(Predicate<R> includeCondition) {
    combinedIncludeCondition.includeCondition = includeCondition;
    filterItems();
  }

  @Override
  public void addItems(Collection<R> items) {
    addItemsAt(visibleItems.size(), items);
  }

  @Override
  public void addItemsSorted(Collection<R> items) {
    addItemsAtSorted(visibleItems.size(), items);
  }

  @Override
  public void addItemsAt(int index, Collection<R> items) {
    if (addItemsAtInternal(index, items)) {
      fireTableDataChanged();
    }
  }

  @Override
  public void addItemsAtSorted(int index, Collection<R> items) {
    if (addItemsAtInternal(index, items)) {
      if (sortModel.isSorted()) {
        sortModel.sort(visibleItems);
      }
      fireTableDataChanged();
    }
  }

  @Override
  public void addItem(R item) {
    addItemInternal(item);
  }

  @Override
  public void addItemAt(int index, R item) {
    addItemAtInternal(index, item);
  }

  @Override
  public void addItemSorted(R item) {
    if (addItemInternal(item)) {
      if (sortModel.isSorted()) {
        sortModel.sort(visibleItems);
      }
      fireTableDataChanged();
    }
  }

  @Override
  public void setItemAt(int index, R item) {
    validate(item);
    if (include(item)) {
      visibleItems.set(index, item);
      fireTableRowsUpdated(index, index);
    }
  }

  @Override
  public void removeItem(R item) {
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
  public void removeItems(Collection<R> items) {
    for (R item : items) {
      removeItem(item);
    }
  }

  @Override
  public void removeItemAt(int index) {
    visibleItems.remove(index);
    fireTableRowsDeleted(index, index);
  }

  @Override
  public void removeItems(int fromIndex, int toIndex) {
    visibleItems.subList(fromIndex, toIndex).clear();
    fireTableRowsDeleted(fromIndex, toIndex);
  }

  @Override
  public void addRowsRemovedListener(EventDataListener<RemovedRows> listener) {
    rowsRemovedEvent.addDataListener(listener);
  }

  @Override
  public void removeRowsRemovedListener(EventDataListener<RemovedRows> listener) {
    rowsRemovedEvent.removeDataListener(listener);
  }

  @Override
  public Class<?> getColumnClass(C columnIdentifier) {
    return columnModel.column(columnIdentifier).getColumnClass();
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return getColumnClass(columnModel().columnIdentifier(columnIndex));
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    return columnValueProvider.value(itemAt(rowIndex), columnModel().columnIdentifier(columnIndex));
  }

  @Override
  public String getStringValueAt(int rowIndex, C columnIdentifier) {
    return columnValueProvider.string(itemAt(rowIndex), columnIdentifier);
  }

  @Override
  public String rowsAsDelimitedString(char delimiter) {
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
  public void addDataChangedListener(EventListener listener) {
    dataChangedEvent.addListener(listener);
  }

  @Override
  public void removeDataChangedListener(EventListener listener) {
    dataChangedEvent.removeListener(listener);
  }

  @Override
  public void addClearListener(EventListener listener) {
    clearEvent.addListener(listener);
  }

  @Override
  public void removeClearListener(EventListener listener) {
    clearEvent.removeListener(listener);
  }

  private void bindEventsInternal() {
    addTableModelListener(e -> dataChangedEvent.onEvent());
    filterModel.addChangeListener(this::filterItems);
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
    return addItemAtInternal(getRowCount(), item);
  }

  private boolean addItemAtInternal(int index, R item) {
    validate(item);
    if (include(item)) {
      visibleItems.add(index, item);

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
    if (!rowValidator.test(item)) {
      throw new IllegalArgumentException("Invalid item: " + item);
    }
  }

  private boolean include(R item) {
    return combinedIncludeCondition.test(item);
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

  private Collection<ColumnConditionModel<C, ?>> createColumnFilterModels(ColumnConditionModel.Factory<C> filterModelFactory) {
    Collection<ColumnConditionModel<C, ?>> filterModels = new ArrayList<>();
    columnModel.columns().stream()
            .map(FilteredTableColumn::getIdentifier)
            .map(filterModelFactory::createConditionModel)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(filterModel -> filterModels.add((ColumnConditionModel<C, ?>) filterModel));

    return filterModels;
  }

  private final class FilteredTableModelRefresher extends AbstractFilteredModelRefresher<R> {

    private FilteredTableModelRefresher(Supplier<Collection<R>> rowSupplier) {
      super(rowSupplier);
    }

    @Override
    protected void handleRefreshResult(Collection<R> items) {
      if (mergeOnRefresh && !items.isEmpty()) {
        merge(items);
      }
      else {
        clearAndAdd(items);
      }
    }
  }

  private final class DefaultFilterModelFactory implements ColumnConditionModel.Factory<C> {

    @Override
    public Optional<ColumnConditionModel<? extends C, ?>> createConditionModel(C columnIdentifier) {
      Class<?> columnClass = getColumnClass(columnIdentifier);
      if (Comparable.class.isAssignableFrom(columnClass)) {
        return Optional.ofNullable(ColumnConditionModel.builder(columnIdentifier, columnClass).build());
      }

      return Optional.empty();
    }
  }

  private final class DefaultSummaryValueProviderFactory implements SummaryValueProvider.Factory<C> {

    @Override
    public <T extends Number> Optional<SummaryValueProvider<T>> createSummaryValueProvider(C columnIdentifier, Format format) {
      Class<?> columnClass = getColumnClass(columnIdentifier);
      if (Number.class.isAssignableFrom(columnClass)) {
        return Optional.of(new DefaultSummaryValueProvider<T, C>(columnIdentifier, DefaultFilteredTableModel.this, format));
      }

      return Optional.empty();
    }
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

  static final class DefaultBuilder<R, C> implements Builder<R, C> {

    private final ColumnValueProvider<R, C> columnValueProvider;

    private List<FilteredTableColumn<C>> columns;
    private Supplier<Collection<R>> rowSupplier;
    private Predicate<R> rowValidator = new ValidPredicate<>();
    private ColumnConditionModel.Factory<C> filterModelFactory;
    private SummaryValueProvider.Factory<C> summaryValueProviderFactory;
    private boolean mergeOnRefresh = false;
    private boolean asyncRefresh = FilteredModel.ASYNC_REFRESH.get();

    DefaultBuilder(ColumnValueProvider<R, C> columnValueProvider) {
      this.columnValueProvider = requireNonNull(columnValueProvider);
    }

    @Override
    public Builder<R, C> columns(List<FilteredTableColumn<C>> columns) {
      if (requireNonNull(columns, "columns").isEmpty()) {
        throw new IllegalArgumentException("One or more columns must be specified");
      }
      this.columns = columns;
      return this;
    }

    @Override
    public Builder<R, C> filterModelFactory(ColumnConditionModel.Factory<C> filterModelFactory) {
      this.filterModelFactory = requireNonNull(filterModelFactory);
      return this;
    }

    @Override
    public Builder<R, C> summaryValueProviderFactory(SummaryValueProvider.Factory<C> summaryValueProviderFactory) {
      this.summaryValueProviderFactory = requireNonNull(summaryValueProviderFactory);
      return this;
    }

    @Override
    public Builder<R, C> rowSupplier(Supplier<Collection<R>> rowSupplier) {
      this.rowSupplier = requireNonNull(rowSupplier);
      return this;
    }

    @Override
    public Builder<R, C> rowValidator(Predicate<R> rowValidator) {
      this.rowValidator = requireNonNull(rowValidator);
      return this;
    }

    @Override
    public Builder<R, C> mergeOnRefresh(boolean mergeOnRefresh) {
      this.mergeOnRefresh = mergeOnRefresh;
      return this;
    }

    @Override
    public Builder<R, C> asyncRefresh(boolean asyncRefresh) {
      this.asyncRefresh = asyncRefresh;
      return this;
    }

    @Override
    public FilteredTableModel<R, C> build() {
      return new DefaultFilteredTableModel<>(this);
    }

    private static final class ValidPredicate<R> implements Predicate<R> {

      @Override
      public boolean test(R r) {
        return true;
      }
    }
  }
}
