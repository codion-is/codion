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

import is.codion.common.event.Event;
import is.codion.common.model.FilterModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.observable.Observer;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.AbstractFilterModelRefresher;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultFilterTableModel<R, C> extends AbstractTableModel implements FilterTableModel<R, C> {

	/**
	 * A Comparator for comparing {@link Comparable} instances.
	 */
	static final Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = Comparable::compareTo;

	/**
	 * A Comparator for comparing Objects according to their toString() value.
	 */
	static final Comparator<?> STRING_COMPARATOR = Comparator.comparing(Object::toString);

	private final Event<?> dataChanged = Event.event();
	private final Event<?> cleared = Event.event();
	private final Columns<R, C> columns;
	private final List<R> visibleItems = new ArrayList<>();
	private final List<R> filteredItems = new ArrayList<>();
	private final FilterTableSelectionModel<R> selectionModel;
	private final TableConditionModel<C> filterModel;
	private final CombinedIncludeCondition combinedIncludeCondition;
	private final Predicate<R> validator;
	private final DefaultRefresher refresher;
	private final RemoveSelectionListener removeSelectionListener;
	private final Value<Comparator<R>> comparator = Value.builder()
					.<Comparator<R>>nullable()
					.notify(Value.Notify.WHEN_SET)
					.build();

	private DefaultFilterTableModel(DefaultBuilder<R, C> builder) {
		this.columns = requireNonNull(builder.columns);
		this.selectionModel = new DefaultFilterTableSelectionModel<>(this);
		this.filterModel = tableConditionModel(createColumnFilterModels(builder.filterModelFactory == null ?
						new DefaultFilterModelFactory() : builder.filterModelFactory));
		this.combinedIncludeCondition = new CombinedIncludeCondition(filterModel.conditionModels().values());
		this.refresher = new DefaultRefresher(builder.items == null ? this::items : builder.items);
		this.refresher.async().set(builder.asyncRefresh);
		this.refresher.refreshStrategy.set(builder.refreshStrategy);
		this.validator = builder.validator;
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
		return columns.identifiers().size();
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
		cleared.run();
	}

	@Override
	public FilterTableSelectionModel<R> selectionModel() {
		return selectionModel;
	}

	@Override
	public TableConditionModel<C> filterModel() {
		return filterModel;
	}

	@Override
	public <T> Collection<T> values(C identifier) {
		return (Collection<T>) columnValues(IntStream.range(0, visibleCount()).boxed(),
						columns.identifiers().indexOf(identifier));
	}

	@Override
	public <T> Collection<T> selectedValues(C identifier) {
		return (Collection<T>) columnValues(selectionModel().selectedIndexes().get().stream(),
						columns.identifiers().indexOf(identifier));
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
	public Value<Comparator<R>> comparator() {
		return comparator;
	}

	@Override
	public void sortItems() {
		if (comparator.isNotNull()) {
			List<R> selectedItems = selectionModel.selectedItems().get();
			visibleItems.sort(comparator.get());
			fireTableRowsUpdated(0, visibleItems.size());
			selectionModel.selectedItems().set(selectedItems);
		}
	}

	@Override
	public void filterItems() {
		List<R> selectedItems = selectionModel.selectedItems().get();
		visibleItems.addAll(filteredItems);
		filteredItems.clear();
		for (ListIterator<R> visibleItemsIterator = visibleItems.listIterator(); visibleItemsIterator.hasNext(); ) {
			R item = visibleItemsIterator.next();
			if (!include(item)) {
				visibleItemsIterator.remove();
				filteredItems.add(item);
			}
		}
		if (comparator.isNotNull()) {
			visibleItems.sort(comparator.get());
		}
		fireTableDataChanged();
		selectionModel.selectedItems().set(selectedItems);
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
		if (addItemsAtInternal(index, items) && comparator.isNotNull()) {
			visibleItems.sort(comparator.get());
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
		if (addItemInternal(item) && comparator.isNotNull()) {
			visibleItems.sort(comparator.get());
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
			dataChanged.run();
		}
	}

	@Override
	public R removeItemAt(int index) {
		R removed = visibleItems.remove(index);
		fireTableRowsDeleted(index, index);
		dataChanged.run();

		return removed;
	}

	@Override
	public List<R> removeItems(int fromIndex, int toIndex) {
		List<R> subList = visibleItems.subList(fromIndex, toIndex);
		List<R> removedItems = new ArrayList<>(subList);
		subList.clear();
		fireTableRowsDeleted(fromIndex, toIndex);
		dataChanged.run();

		return removedItems;
	}

	@Override
	public Class<?> getColumnClass(C identifier) {
		return columns.columnClass(requireNonNull(identifier));
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columns.columnClass(columns.identifier(columnIndex));
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return columns.value(itemAt(rowIndex), columns.identifier(columnIndex));
	}

	@Override
	public Columns<R, C> columns() {
		return columns;
	}

	@Override
	public String getStringAt(int rowIndex, C identifier) {
		return columns.string(itemAt(rowIndex), requireNonNull(identifier));
	}

	@Override
	public Observer<?> dataChanged() {
		return dataChanged.observer();
	}

	@Override
	public Observer<?> cleared() {
		return cleared.observer();
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
				dataChanged.run();
			}
		});
		addTableModelListener(removeSelectionListener);
		filterModel.conditionChanged().addListener(this::filterItems);
		comparator.addListener(this::sortItems);
	}

	private List<Object> columnValues(Stream<Integer> rowIndexStream, int columnModelIndex) {
		return rowIndexStream.map(rowIndex -> getValueAt(rowIndex, columnModelIndex)).collect(toList());
	}

	private boolean addItemInternal(R item) {
		return addItemAtInternal(visibleCount(), item);
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
				dataChanged.run();
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
		if (!validator.test(item)) {
			throw new IllegalArgumentException("Invalid item: " + item);
		}
	}

	private boolean include(R item) {
		return combinedIncludeCondition.test(item);
	}

	private Collection<ColumnConditionModel<C, ?>> createColumnFilterModels(ColumnConditionModel.Factory<C> filterModelFactory) {
		return columns.identifiers().stream()
						.map(filterModelFactory::createConditionModel)
						.flatMap(Optional::stream)
						.map(model -> (ColumnConditionModel<C, ?>) model)
						.collect(toList());
	}

	private final class DefaultRefresher extends AbstractFilterModelRefresher<R> {

		private final Value<RefreshStrategy> refreshStrategy = Value.builder()
						.nonNull(RefreshStrategy.CLEAR)
						.build();

		private DefaultRefresher(Supplier<Collection<R>> items) {
			super(items);
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
			DefaultFilterTableModel.this.items().stream()
							.filter(item -> !itemSet.contains(item))
							.forEach(DefaultFilterTableModel.this::removeItem);
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
			List<R> selectedItems = selectionModel.selectedItems().get();
			clear();
			addItemsSorted(items);
			selectionModel.selectedItems().set(selectedItems);
		}
	}

	private final class DefaultFilterModelFactory implements ColumnConditionModel.Factory<C> {

		@Override
		public Optional<ColumnConditionModel<C, ?>> createConditionModel(C identifier) {
			Class<?> columnClass = getColumnClass(identifier);
			if (Comparable.class.isAssignableFrom(columnClass)) {
				return Optional.of(ColumnConditionModel.builder(identifier, columnClass).build());
			}

			return Optional.empty();
		}
	}

	private final class CombinedIncludeCondition implements Predicate<R> {

		private final List<ColumnConditionModel<C, ?>> columnFilters;

		private final Value<Predicate<R>> includeCondition = Value.value();

		private CombinedIncludeCondition(Collection<ColumnConditionModel<C, ?>> columnFilters) {
			this.columnFilters = columnFilters == null ? Collections.emptyList() : new ArrayList<>(columnFilters);
			this.includeCondition.addListener(DefaultFilterTableModel.this::filterItems);
		}

		@Override
		public boolean test(R item) {
			if (includeCondition.isNotNull() && !includeCondition.get().test(item)) {
				return false;
			}

			return columnFilters.stream()
							.filter(conditionModel -> conditionModel.enabled().get())
							.allMatch(conditionModel -> accepts(item, conditionModel, columns));
		}

		private boolean accepts(R item, ColumnConditionModel<C, ?> conditionModel, Columns<R, C> columns) {
			if (conditionModel.columnClass().equals(String.class)) {
				String stringValue = columns.string(item, conditionModel.identifier());

				return ((ColumnConditionModel<?, String>) conditionModel).accepts(stringValue.isEmpty() ? null : stringValue);
			}

			return conditionModel.accepts(columns.comparable(item, conditionModel.identifier()));
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

	static final class DefaultBuilder<R, C> implements Builder<R, C> {

		private final Columns<R, C> columns;

		private Supplier<Collection<R>> items;
		private Predicate<R> validator = new ValidPredicate<>();
		private ColumnConditionModel.Factory<C> filterModelFactory;
		private RefreshStrategy refreshStrategy = RefreshStrategy.CLEAR;
		private boolean asyncRefresh = FilterModel.ASYNC_REFRESH.get();

		DefaultBuilder(Columns<R, C> columns) {
			if (requireNonNull(columns).identifiers().isEmpty()) {
				throw new IllegalArgumentException("No columns specified");
			}
			this.columns = validateIdentifiers(columns);
		}

		@Override
		public Builder<R, C> filterModelFactory(ColumnConditionModel.Factory<C> filterModelFactory) {
			this.filterModelFactory = requireNonNull(filterModelFactory);
			return this;
		}

		@Override
		public Builder<R, C> items(Supplier<Collection<R>> items) {
			this.items = requireNonNull(items);
			return this;
		}

		@Override
		public Builder<R, C> validator(Predicate<R> validator) {
			this.validator = requireNonNull(validator);
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
		public FilterTableModel<R, C> build() {
			return new DefaultFilterTableModel<>(this);
		}

		private Columns<R, C> validateIdentifiers(Columns<R, C> columns) {
			if (new HashSet<>(columns.identifiers()).size() != columns.identifiers().size()) {
				throw new IllegalArgumentException("Column identifiers are not unique");
			}

			return columns;
		}

		private static final class ValidPredicate<R> implements Predicate<R> {

			@Override
			public boolean test(R r) {
				return true;
			}
		}
	}
}
