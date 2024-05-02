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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.Separators;
import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.model.FilteredModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValues;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.model.table.TableSummaryModel;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.AbstractFilteredModelRefresher;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static is.codion.common.model.table.TableConditionModel.tableConditionModel;
import static is.codion.common.model.table.TableSummaryModel.tableSummaryModel;
import static java.lang.String.join;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class DefaultFilteredTableModel<R, C> extends AbstractTableModel implements FilteredTableModel<R, C> {

	private final Event<?> dataChangedEvent = Event.event();
	private final Event<?> clearedEvent = Event.event();
	private final ColumnValues<R, C> columnValues;
	private final List<R> visibleItems = new ArrayList<>();
	private final List<R> filteredItems = new ArrayList<>();
	private final FilteredTableSelectionModel<R> selectionModel;
	private final FilteredTableColumnModel<C> columnModel;
	private final FilteredTableSortModel<R, C> sortModel;
	private final FilteredTableSearchModel searchModel;
	private final TableConditionModel<C> filterModel;
	private final TableSummaryModel<C> summaryModel;
	private final CombinedIncludeCondition combinedIncludeCondition;
	private final Predicate<R> itemValidator;
	private final DefaultRefresher refresher;
	private final RemoveSelectionListener removeSelectionListener;

	private DefaultFilteredTableModel(DefaultBuilder<R, C> builder) {
		this.columnModel = new DefaultFilteredTableColumnModel<>(requireNonNull(builder.columnFactory).createColumns());
		this.searchModel = new DefaultFilteredTableSearchModel<>(this);
		this.columnValues = requireNonNull(builder.columnValues);
		this.sortModel = new DefaultFilteredTableSortModel<>(columnModel, columnValues);
		this.selectionModel = new DefaultFilteredTableSelectionModel<>(this);
		this.filterModel = tableConditionModel(createColumnFilterModels(builder.filterModelFactory == null ?
						new DefaultFilterModelFactory() : builder.filterModelFactory));
		this.summaryModel = tableSummaryModel(builder.summaryValuesFactory == null ?
						new DefaultSummaryValuesFactory() : builder.summaryValuesFactory);
		this.combinedIncludeCondition = new CombinedIncludeCondition(filterModel.conditionModels().values());
		this.refresher = new DefaultRefresher(builder.itemSupplier == null ? this::items : builder.itemSupplier);
		this.refresher.async().set(builder.asyncRefresh);
		this.refresher.refreshStrategy.set(builder.refreshStrategy);
		this.itemValidator = builder.itemValidator;
		this.removeSelectionListener = new RemoveSelectionListener();
		bindEventsInternal();
	}

	@Override
	public Collection<R> items() {
		List<R> items = new ArrayList<>(visibleItems);
		items.addAll(filteredItems);

		return unmodifiableList(items);
	}

	@Override
	public List<R> visibleItems() {
		return unmodifiableList(visibleItems);
	}

	@Override
	public Collection<R> filteredItems() {
		return unmodifiableList(filteredItems);
	}

	@Override
	public int visibleCount() {
		return getRowCount();
	}

	@Override
	public int filteredCount() {
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
		return visible(item) || filtered(item);
	}

	@Override
	public boolean visible(R item) {
		return visibleItems.contains(item);
	}

	@Override
	public boolean filtered(R item) {
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
		clearedEvent.run();
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
		return (Collection<T>) columnValues(IntStream.range(0, visibleCount()).boxed(),
						columnModel.column(columnIdentifier).getModelIndex());
	}

	@Override
	public <T> Collection<T> selectedValues(C columnIdentifier) {
		return (Collection<T>) columnValues(selectionModel().getSelectedIndexes().stream(),
						columnModel.column(columnIdentifier).getModelIndex());
	}

	@Override
	public Value<RefreshStrategy> refreshStrategy() {
		return refresher.refreshStrategy;
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
		if (sortModel.sorted()) {
			List<R> selectedItems = selectionModel.getSelectedItems();
			visibleItems.sort(sortModel.comparator());
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
		visibleItems.sort(sortModel.comparator());
		fireTableDataChanged();
		selectionModel.setSelectedItems(selectedItems);
	}

	@Override
	public Value<Predicate<R>> includeCondition() {
		return combinedIncludeCondition.includeCondition;
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
		addItemsAtInternal(index, items);
	}

	@Override
	public void addItemsAtSorted(int index, Collection<R> items) {
		if (addItemsAtInternal(index, items) && sortModel.sorted()) {
			visibleItems.sort(sortModel.comparator());
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
		if (addItemInternal(item) && sortModel.sorted()) {
			visibleItems.sort(sortModel.comparator());
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
		removeItemInternal(item, true);
	}

	@Override
	public void removeItems(Collection<R> items) {
		selectionModel.setValueIsAdjusting(true);
		boolean visibleItemRemoved = false;
		for (R item : requireNonNull(items)) {
			visibleItemRemoved = removeItemInternal(item, false) || visibleItemRemoved;
		}
		selectionModel.setValueIsAdjusting(false);
		if (visibleItemRemoved) {
			dataChangedEvent.run();
		}
	}

	@Override
	public R removeItemAt(int index) {
		R removed = visibleItems.remove(index);
		fireTableRowsDeleted(index, index);
		dataChangedEvent.run();

		return removed;
	}

	@Override
	public List<R> removeItems(int fromIndex, int toIndex) {
		List<R> subList = visibleItems.subList(fromIndex, toIndex);
		List<R> removedItems = new ArrayList<>(subList);
		subList.clear();
		fireTableRowsDeleted(fromIndex, toIndex);
		dataChangedEvent.run();

		return removedItems;
	}

	@Override
	public Class<?> getColumnClass(C columnIdentifier) {
		return columnModel.column(columnIdentifier).columnClass();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return getColumnClass(columnModel().columnIdentifier(columnIndex));
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return columnValues.value(itemAt(rowIndex), columnModel().columnIdentifier(columnIndex));
	}

	@Override
	public String getStringAt(int rowIndex, C columnIdentifier) {
		return columnValues.string(itemAt(rowIndex), columnIdentifier);
	}

	@Override
	public Export export() {
		return new DefaultExport();
	}

	@Override
	public EventObserver<?> dataChangedEvent() {
		return dataChangedEvent.observer();
	}

	@Override
	public EventObserver<?> clearedEvent() {
		return clearedEvent.observer();
	}

	@Override
	public void addTableModelListener(TableModelListener listener) {
		super.addTableModelListener(listener);
		if (listener instanceof JTable) {
			// JTable handles removing the selected indexes on row removal
			removeTableModelListener(removeSelectionListener);
		}
	}

	@Override
	public void removeTableModelListener(TableModelListener listener) {
		super.removeTableModelListener(listener);
		if (listener instanceof JTable) {
			// JTable handles removing the selected indexes on row removal
			addTableModelListener(removeSelectionListener);
		}
	}

	private void bindEventsInternal() {
		addTableModelListener(e -> {
			if (e.getType() != TableModelEvent.DELETE) {
				// Removals are handled specially, in order to trigger only a single
				// event when multiple rows are removed, see remove... methods
				dataChangedEvent.run();
			}
		});
		addTableModelListener(removeSelectionListener);
		filterModel.conditionChangedEvent().addListener(this::filterItems);
		sortModel.sortingChangedEvent().addListener(this::sortItems);
	}

	private List<Object> columnValues(Stream<Integer> rowIndexStream, int columnModelIndex) {
		return rowIndexStream.map(rowIndex -> getValueAt(rowIndex, columnModelIndex)).collect(toList());
	}

	private boolean addItemInternal(R item) {
		return addItemAtInternal(getRowCount(), item);
	}

	private boolean addItemAtInternal(int index, R item) {
		validate(item);
		if (include(item)) {
			visibleItems.add(index, item);
			fireTableRowsInserted(index, index);

			return true;
		}
		filteredItems.add(item);

		return false;
	}

	private boolean addItemsAtInternal(int index, Collection<R> items) {
		requireNonNull(items);
		Collection<R> visible = new ArrayList<>(items.size());
		Collection<R> filtered = new ArrayList<>(items.size());
		for (R item : items) {
			validate(item);
			if (include(item)) {
				visible.add(item);
			}
			else {
				filtered.add(item);
			}
		}
		if (!visible.isEmpty()) {
			visibleItems.addAll(index, visible);
			fireTableRowsInserted(index, index + visible.size());
		}
		filteredItems.addAll(filtered);

		return !visible.isEmpty();
	}

	private boolean removeItemInternal(R item, boolean notifyDataChanged) {
		int visibleItemIndex = visibleItems.indexOf(item);
		if (visibleItemIndex >= 0) {
			visibleItems.remove(visibleItemIndex);
			fireTableRowsDeleted(visibleItemIndex, visibleItemIndex);
			if (notifyDataChanged) {
				dataChangedEvent.run();
			}
		}
		else {
			int filteredIndex = filteredItems.indexOf(item);
			if (filteredIndex >= 0) {
				filteredItems.remove(item);
			}
		}

		return visibleItemIndex >= 0;
	}

	private void validate(R item) {
		requireNonNull(item);
		if (!itemValidator.test(item)) {
			throw new IllegalArgumentException("Invalid item: " + item);
		}
	}

	private boolean include(R item) {
		return combinedIncludeCondition.test(item);
	}

	private Collection<ColumnConditionModel<C, ?>> createColumnFilterModels(ColumnConditionModel.Factory<C> filterModelFactory) {
		return columnModel.columns().stream()
						.map(FilteredTableColumn::getIdentifier)
						.map(filterModelFactory::createConditionModel)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.map(conditionModel -> (ColumnConditionModel<C, ?>) conditionModel)
						.collect(Collectors.toList());
	}

	private final class DefaultRefresher extends AbstractFilteredModelRefresher<R> {

		private final Value<RefreshStrategy> refreshStrategy = Value.nonNull(RefreshStrategy.CLEAR).build();

		private DefaultRefresher(Supplier<Collection<R>> itemSupplier) {
			super(itemSupplier);
		}

		@Override
		protected void processResult(Collection<R> items) {
			if (refreshStrategy.isEqualTo(RefreshStrategy.MERGE) && !items.isEmpty()) {
				merge(items);
			}
			else {
				clearAndAdd(items);
			}
		}

		private void merge(Collection<R> items) {
			Set<R> itemSet = new HashSet<>(items);
			items().stream()
							.filter(item -> !itemSet.contains(item))
							.forEach(DefaultFilteredTableModel.this::removeItem);
			items.forEach(this::merge);
		}

		private void merge(R item) {
			int index = indexOf(item);
			if (index == -1) {
				addItemInternal(item);
			}
			else {
				setItemAt(index, item);
			}
		}

		private void clearAndAdd(Collection<R> items) {
			Collection<R> selectedItems = selectionModel.getSelectedItems();
			clear();
			addItemsSorted(items);
			selectionModel.setSelectedItems(selectedItems);
		}
	}

	private final class DefaultFilterModelFactory implements ColumnConditionModel.Factory<C> {

		@Override
		public Optional<ColumnConditionModel<? extends C, ?>> createConditionModel(C columnIdentifier) {
			Class<?> columnClass = getColumnClass(columnIdentifier);
			if (Comparable.class.isAssignableFrom(columnClass)) {
				return Optional.of(ColumnConditionModel.builder(columnIdentifier, columnClass).build());
			}

			return Optional.empty();
		}
	}

	private final class DefaultSummaryValuesFactory implements SummaryValues.Factory<C> {

		@Override
		public <T extends Number> Optional<SummaryValues<T>> createSummaryValues(C columnIdentifier, Format format) {
			Class<?> columnClass = getColumnClass(columnIdentifier);
			if (Number.class.isAssignableFrom(columnClass)) {
				return Optional.of(new DefaultSummaryValues<>(columnIdentifier, DefaultFilteredTableModel.this, format));
			}

			return Optional.empty();
		}
	}

	private final class CombinedIncludeCondition implements Predicate<R> {

		private final List<? extends ColumnConditionModel<? extends C, ?>> columnFilters;

		private final Value<Predicate<R>> includeCondition = Value.value();

		private CombinedIncludeCondition(Collection<? extends ColumnConditionModel<? extends C, ?>> columnFilters) {
			this.columnFilters = columnFilters == null ? Collections.emptyList() : new ArrayList<>(columnFilters);
			this.includeCondition.addListener(DefaultFilteredTableModel.this::filterItems);
		}

		@Override
		public boolean test(R item) {
			if (includeCondition.isNotNull() && !includeCondition.get().test(item)) {
				return false;
			}

			return columnFilters.stream()
							.filter(conditionModel -> conditionModel.enabled().get())
							.allMatch(conditionModel -> accepts(item, conditionModel, columnValues));
		}

		private boolean accepts(R item, ColumnConditionModel<? extends C, ?> conditionModel, ColumnValues<R, C> columnValues) {
			if (conditionModel.columnClass().equals(String.class)) {
				String stringValue = columnValues.string(item, conditionModel.columnIdentifier());

				return ((ColumnConditionModel<?, String>) conditionModel).accepts(stringValue.isEmpty() ? null : stringValue);
			}

			return conditionModel.accepts(columnValues.comparable(item, conditionModel.columnIdentifier()));
		}
	}

	private final class RemoveSelectionListener implements TableModelListener {

		@Override
		public void tableChanged(TableModelEvent e) {
			if (e.getType() == TableModelEvent.DELETE) {
				selectionModel.removeIndexInterval(e.getFirstRow(), e.getLastRow());
			}
		}
	}

	/**
	 * A default SummaryValues implementation
	 */
	static final class DefaultSummaryValues<T extends Number, C> implements SummaryValues<T> {

		private final C columnIdentifier;
		private final FilteredTableModel<?, C> tableModel;
		private final Format format;
		private final Event<?> changeEvent = Event.event();

		/**
		 * @param columnIdentifier the identifier of the column which values are provided
		 * @param tableModel the table model
		 * @param format the format to use for presenting the summary value
		 */
		DefaultSummaryValues(C columnIdentifier, FilteredTableModel<?, C> tableModel, Format format) {
			this.columnIdentifier = requireNonNull(columnIdentifier);
			this.tableModel = requireNonNull(tableModel);
			this.format = requireNonNull(format);
			this.tableModel.dataChangedEvent().addListener(changeEvent);
			this.tableModel.selectionModel().selectionEvent().addListener(changeEvent);
		}

		@Override
		public String format(Object value) {
			return format.format(value);
		}

		@Override
		public EventObserver<?> changeEvent() {
			return changeEvent.observer();
		}

		@Override
		public Collection<T> values() {
			return subset() ? tableModel.selectedValues(columnIdentifier) : tableModel.values(columnIdentifier);
		}

		@Override
		public boolean subset() {
			FilteredTableSelectionModel<?> tableSelectionModel = tableModel.selectionModel();

			return tableSelectionModel.selectionNotEmpty().get() &&
							tableSelectionModel.selectionCount() != tableModel.visibleCount();
		}
	}

	static final class DefaultBuilder<R, C> implements Builder<R, C> {

		private final ColumnFactory<C> columnFactory;
		private final ColumnValues<R, C> columnValues;

		private Supplier<Collection<R>> itemSupplier;
		private Predicate<R> itemValidator = new ValidPredicate<>();
		private ColumnConditionModel.Factory<C> filterModelFactory;
		private SummaryValues.Factory<C> summaryValuesFactory;
		private RefreshStrategy refreshStrategy = RefreshStrategy.CLEAR;
		private boolean asyncRefresh = FilteredModel.ASYNC_REFRESH.get();

		DefaultBuilder(ColumnFactory<C> columnFactory, ColumnValues<R, C> columnValues) {
			this.columnFactory = requireNonNull(columnFactory);
			this.columnValues = requireNonNull(columnValues);
		}

		@Override
		public Builder<R, C> filterModelFactory(ColumnConditionModel.Factory<C> filterModelFactory) {
			this.filterModelFactory = requireNonNull(filterModelFactory);
			return this;
		}

		@Override
		public Builder<R, C> summaryValuesFactory(SummaryValues.Factory<C> summaryValuesFactory) {
			this.summaryValuesFactory = requireNonNull(summaryValuesFactory);
			return this;
		}

		@Override
		public Builder<R, C> itemSupplier(Supplier<Collection<R>> itemSupplier) {
			this.itemSupplier = requireNonNull(itemSupplier);
			return this;
		}

		@Override
		public Builder<R, C> itemValidator(Predicate<R> itemValidator) {
			this.itemValidator = requireNonNull(itemValidator);
			return this;
		}

		@Override
		public Builder<R, C> refreshStrategy(RefreshStrategy refreshStrategy) {
			this.refreshStrategy = requireNonNull(refreshStrategy);
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

	private final class DefaultExport implements Export {

		private char delimiter = '\t';
		private boolean header = true;
		private boolean hidden = false;
		private boolean selected = false;

		@Override
		public Export delimiter(char delimiter) {
			this.delimiter = delimiter;
			return this;
		}

		@Override
		public Export header(boolean header) {
			this.header = header;
			return this;
		}

		@Override
		public Export hidden(boolean hidden) {
			this.hidden = hidden;
			return this;
		}

		@Override
		public Export selected(boolean selected) {
			this.selected = selected;
			return this;
		}

		@Override
		public String get() {
			List<Integer> rows = selected ?
							selectionModel.getSelectedIndexes() :
							IntStream.range(0, getRowCount())
											.boxed()
											.collect(toList());

			List<FilteredTableColumn<C>> columns = new ArrayList<>(columnModel().visible());
			if (hidden) {
				columns.addAll(columnModel().hidden());
			}

			List<List<String>> lines = new ArrayList<>();
			if (header) {
				lines.add(columns.stream()
								.map(column -> String.valueOf(column.getHeaderValue()))
								.collect(toList()));
			}
			lines.addAll(rows.stream()
							.map(row -> stringValues(row, columns))
							.collect(toList()));

			return new StringBuilder()
							.append(lines.stream().map(line -> join(String.valueOf(delimiter), line))
											.collect(joining(Separators.LINE_SEPARATOR)))
							.toString();
		}

		private List<String> stringValues(int row, List<FilteredTableColumn<C>> columns) {
			return columns.stream()
							.map(column -> getStringAt(row, column.getIdentifier()))
							.collect(toList());
		}
	}
}
