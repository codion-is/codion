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
import is.codion.common.model.FilterModel.Items.Filtered;
import is.codion.common.model.FilterModel.Items.Visible;
import is.codion.common.model.condition.ColumnConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.observer.Observer;
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

import static is.codion.common.model.condition.TableConditionModel.tableConditionModel;
import static java.util.Collections.unmodifiableCollection;
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

	private final Columns<R, C> columns;
	private final DefaultItems modelItems = new DefaultItems();
	private final FilterTableSelectionModel<R> selectionModel;
	private final TableConditionModel<C> conditionModel;
	private final CombinedVisiblePredicate combinedVisiblePredicate;
	private final Predicate<R> validator;
	private final DefaultRefresher refresher;
	private final RemoveSelectionListener removeSelectionListener;
	private final Value<Comparator<R>> comparator = Value.builder()
					.<Comparator<R>>nullable()
					.notify(Value.Notify.WHEN_SET)
					.build();

	private DefaultFilterTableModel(DefaultBuilder<R, C> builder) {
		this.columns = requireNonNull(builder.columns);
		this.selectionModel = new DefaultFilterTableSelectionModel<>(modelItems);
		this.conditionModel = tableConditionModel(createColumnFilterModels(builder.filterModelFactory == null ?
						new DefaultFilterModelFactory() : builder.filterModelFactory));
		this.combinedVisiblePredicate = new CombinedVisiblePredicate(conditionModel.conditions().values());
		this.refresher = new DefaultRefresher(builder.items == null ? modelItems::get : (Supplier<Collection<R>>) builder.items);
		this.refresher.async().set(builder.asyncRefresh);
		this.refresher.refreshStrategy.set(builder.refreshStrategy);
		this.validator = builder.validator;
		this.removeSelectionListener = new RemoveSelectionListener();
		bindEventsInternal();
	}

	@Override
	public Items<R> items() {
		return modelItems;
	}

	@Override
	public int getColumnCount() {
		return columns.identifiers().size();
	}

	@Override
	public int getRowCount() {
		return modelItems.visible.items.size();
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
		int filteredSize = modelItems.filtered.items.size();
		modelItems.filtered.items.clear();
		int visibleSize = modelItems.visible.items.size();
		modelItems.visible.items.clear();
		if (visibleSize > 0) {
			fireTableRowsDeleted(0, visibleSize - 1);
		}
		if (filteredSize != 0) {
			modelItems.filtered.notifyChanges();
		}
		if (visibleSize != 0) {
			modelItems.visible.notifyChanges();
		}
	}

	@Override
	public FilterTableSelectionModel<R> selection() {
		return selectionModel;
	}

	@Override
	public TableConditionModel<C> conditionModel() {
		return conditionModel;
	}

	@Override
	public <T> Collection<T> values(C identifier) {
		return (Collection<T>) columnValues(IntStream.range(0, modelItems.visible().count()).boxed(),
						columns.identifiers().indexOf(identifier));
	}

	@Override
	public <T> Collection<T> selectedValues(C identifier) {
		return (Collection<T>) columnValues(selection().indexes().get().stream(),
						columns.identifiers().indexOf(identifier));
	}

	@Override
	public Value<RefreshStrategy> refreshStrategy() {
		return refresher.refreshStrategy;
	}

	@Override
	public Value<Comparator<R>> comparator() {
		return comparator;
	}

	@Override
	public void sort() {
		if (comparator.isNotNull()) {
			List<R> selectedItems = selectionModel.items().get();
			modelItems.visible.items.sort(comparator.get());
			fireTableRowsUpdated(0, modelItems.visible.items.size());
			selectionModel.items().set(selectedItems);
		}
	}

	@Override
	public void addItems(Collection<R> items) {
		addItemsAt(this.modelItems.visible.items.size(), items);
	}

	@Override
	public void addItemsSorted(Collection<R> items) {
		addItemsAtSorted(this.modelItems.visible.items.size(), items);
	}

	@Override
	public void addItemsAt(int index, Collection<R> items) {
		addItemsAtInternal(index, items);
	}

	@Override
	public void addItemsAtSorted(int index, Collection<R> items) {
		if (addItemsAtInternal(index, items) && comparator.isNotNull()) {
			modelItems.visible.items.sort(comparator.get());
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
			this.modelItems.visible.items.sort(comparator.get());
			fireTableDataChanged();
		}
	}

	@Override
	public void setItemAt(int index, R item) {
		validate(item);
		if (include(item)) {
			modelItems.visible.items.set(index, item);
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
			this.modelItems.visible.notifyChanges();
		}
	}

	@Override
	public R removeItemAt(int index) {
		R removed = modelItems.visible.items.remove(index);
		fireTableRowsDeleted(index, index);
		modelItems.visible.notifyChanges();

		return removed;
	}

	@Override
	public List<R> removeItems(int fromIndex, int toIndex) {
		List<R> subList = modelItems.visible.items.subList(fromIndex, toIndex);
		List<R> removedItems = new ArrayList<>(subList);
		subList.clear();
		fireTableRowsDeleted(fromIndex, toIndex);
		modelItems.visible.notifyChanges();

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
		return columns.value(modelItems.visible.itemAt(rowIndex), columns.identifier(columnIndex));
	}

	@Override
	public Columns<R, C> columns() {
		return columns;
	}

	@Override
	public String getStringAt(int rowIndex, C identifier) {
		return columns.string(modelItems.visible.itemAt(rowIndex), requireNonNull(identifier));
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
				modelItems.visible.notifyChanges();
			}
		});
		addTableModelListener(removeSelectionListener);
		conditionModel.conditionChanged().addListener(modelItems::filter);
		comparator.addListener(this::sort);
	}

	private List<Object> columnValues(Stream<Integer> rowIndexStream, int columnModelIndex) {
		return rowIndexStream.map(rowIndex -> getValueAt(rowIndex, columnModelIndex)).collect(toList());
	}

	private boolean addItemInternal(R item) {
		return addItemAtInternal(modelItems.visible().count(), item);
	}

	private boolean addItemAtInternal(int index, R item) {
		validate(item);
		if (include(item)) {
			modelItems.visible.items.add(index, item);
			fireTableRowsInserted(index, index);

			return true;
		}
		modelItems.filtered.items.add(item);
		modelItems.filtered.notifyChanges();

		return false;
	}

	private boolean addItemsAtInternal(int index, Collection<R> items) {
		requireNonNull(items);
		Collection<R> visibleItems = new ArrayList<>(items.size());
		Collection<R> filteredItems = new ArrayList<>(items.size());
		for (R item : items) {
			validate(item);
			if (include(item)) {
				visibleItems.add(item);
			}
			else {
				filteredItems.add(item);
			}
		}
		if (!visibleItems.isEmpty()) {
			modelItems.visible.items.addAll(index, visibleItems);
			fireTableRowsInserted(index, index + visibleItems.size());
		}
		if (!filteredItems.isEmpty()) {
			modelItems.filtered.items.addAll(filteredItems);
			modelItems.filtered.notifyChanges();
		}

		return !visibleItems.isEmpty();
	}

	private boolean removeItemInternal(R item, boolean notifyDataChanged) {
		int visibleItemIndex = modelItems.visible.items.indexOf(item);
		if (visibleItemIndex >= 0) {
			modelItems.visible.items.remove(visibleItemIndex);
			fireTableRowsDeleted(visibleItemIndex, visibleItemIndex);
			if (notifyDataChanged) {
				modelItems.visible.notifyChanges();
			}
		}
		else {
			int filteredIndex = modelItems.filtered.items.indexOf(item);
			if (filteredIndex >= 0) {
				modelItems.filtered.items.remove(item);
				modelItems.filtered.notifyChanges();
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
		return combinedVisiblePredicate.test(item);
	}

	private Collection<ColumnConditionModel<C, ?>> createColumnFilterModels(ColumnConditionModel.Factory<C> filterModelFactory) {
		return columns.identifiers().stream()
						.map(filterModelFactory::createConditionModel)
						.flatMap(Optional::stream)
						.map(model -> (ColumnConditionModel<C, ?>) model)
						.collect(toList());
	}

	private final class DefaultRefresher extends AbstractFilterModelRefresher<R> {

		private final Event<Collection<R>> event = Event.event();
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
			event.accept(unmodifiableCollection(items));
		}

		private void merge(Collection<R> items) {
			Set<R> itemSet = new HashSet<>(items);
			modelItems.get().stream()
							.filter(item -> !itemSet.contains(item))
							.forEach(DefaultFilterTableModel.this::removeItem);
			items.forEach(this::merge);
		}

		private void merge(R item) {
			int index = modelItems.visible.indexOf(item);
			if (index == -1) {
				addItemInternal(item);
			}
			else {
				setItemAt(index, item);
			}
		}

		private void clearAndAdd(Collection<R> items) {
			List<R> selectedItems = selectionModel.items().get();
			clear();
			addItemsSorted(items);
			selectionModel.items().set(selectedItems);
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

	private final class DefaultItems implements Items<R> {

		private final VisibleItems visible = new VisibleItems();
		private final FilteredItems filtered = new FilteredItems();

		@Override
		public Collection<R> get() {
			if (filtered.items.isEmpty()) {
				return unmodifiableCollection(new ArrayList<>(visible.items));
			}
			List<R> entities = new ArrayList<>(visible.items.size() + filtered.items.size());
			entities.addAll(visible.items);
			entities.addAll(filtered.items);

			return unmodifiableList(entities);
		}

		@Override
		public void set(Collection<R> items) {
			refresher.processResult(requireNonNull(items));
		}

		@Override
		public Observer<Collection<R>> observer() {
			return refresher.event.observer();
		}

		@Override
		public Value<Predicate<R>> visiblePredicate() {
			return combinedVisiblePredicate.visiblePredicate;
		}

		@Override
		public Visible<R> visible() {
			return visible;
		}

		@Override
		public Filtered<R> filtered() {
			return filtered;
		}

		@Override
		public boolean contains(R item) {
			return visible.contains(item) || filtered.contains(item);
		}

		@Override
		public int count() {
			return visible.count() + filtered.count();
		}

		@Override
		public void filter() {
			List<R> selectedItems = selectionModel.items().get();
			visible.items.addAll(filtered.items);
			filtered.items.clear();
			for (ListIterator<R> visibleItemsIterator = visible.items.listIterator(); visibleItemsIterator.hasNext(); ) {
				R item = visibleItemsIterator.next();
				if (!include(item)) {
					visibleItemsIterator.remove();
					filtered.items.add(item);
				}
			}
			if (comparator.isNotNull()) {
				visible.items.sort(comparator.get());
			}
			fireTableDataChanged();
			filtered.notifyChanges();
			selectionModel.items().set(selectedItems);
		}
	}

	private final class VisibleItems implements Visible<R> {

		private final List<R> items = new ArrayList<>();
		private final Event<List<R>> event = Event.event();

		@Override
		public List<R> get() {
			return unmodifiableList(items);
		}

		@Override
		public Observer<List<R>> observer() {
			return event.observer();
		}

		@Override
		public boolean contains(R item) {
			return items.contains(item);
		}

		@Override
		public int indexOf(R item) {
			return items.indexOf(item);
		}

		@Override
		public R itemAt(int index) {
			return items.get(index);
		}

		@Override
		public int count() {
			return items.size();
		}

		private void notifyChanges() {
			event.accept(get());
		}
	}

	private final class FilteredItems implements Filtered<R> {

		private final List<R> items = new ArrayList<>();
		private final Event<Collection<R>> event = Event.event();

		@Override
		public Collection<R> get() {
			return unmodifiableCollection(items);
		}

		@Override
		public Observer<Collection<R>> observer() {
			return event.observer();
		}

		@Override
		public boolean contains(R item) {
			return items.contains(item);
		}

		@Override
		public int count() {
			return items.size();
		}

		private void notifyChanges() {
			event.accept(get());
		}
	}

	private final class CombinedVisiblePredicate implements Predicate<R> {

		private final List<ColumnConditionModel<C, ?>> columnFilters;

		private final Value<Predicate<R>> visiblePredicate = Value.builder()
						.<Predicate<R>>nullable()
						.listener(modelItems::filter)
						.build();

		private CombinedVisiblePredicate(Collection<ColumnConditionModel<C, ?>> columnFilters) {
			this.columnFilters = columnFilters == null ? Collections.emptyList() : new ArrayList<>(columnFilters);
		}

		@Override
		public boolean test(R item) {
			if (visiblePredicate.isNotNull() && !visiblePredicate.get().test(item)) {
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

		private Supplier<? extends Collection<R>> items;
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
		public Builder<R, C> items(Supplier<? extends Collection<R>> items) {
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
