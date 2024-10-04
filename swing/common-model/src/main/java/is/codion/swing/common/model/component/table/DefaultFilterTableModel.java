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
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.model.condition.TableConditionModel.ConditionModelFactory;
import is.codion.common.observer.Observer;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.AbstractFilterModelRefresher;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
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
	private final DefaultItems modelItems;
	private final TableSelection<R> selection;
	private final TableConditionModel<C> conditionModel;
	private final VisiblePredicate visiblePredicate;
	private final DefaultRefresher refresher;
	private final RemoveSelectionListener removeSelectionListener;

	private DefaultFilterTableModel(DefaultBuilder<R, C> builder) {
		this.columns = requireNonNull(builder.columns);
		this.modelItems = new DefaultItems(builder.validator);
		this.selection = new DefaultFilterTableSelection<>(modelItems);
		this.conditionModel = tableConditionModel(createColumnFilterModels(builder.filterModelFactory == null ?
						new DefaultFilterModelFactory() : builder.filterModelFactory));
		this.visiblePredicate = new VisiblePredicate(conditionModel.conditions());
		this.refresher = new DefaultRefresher(builder.supplier == null ? modelItems::get : (Supplier<Collection<R>>) builder.supplier);
		this.refresher.async().set(builder.asyncRefresh);
		this.refresher.refreshStrategy.set(builder.refreshStrategy);
		this.removeSelectionListener = new RemoveSelectionListener();
		bindEvents();
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
		refresh(null);
	}

	@Override
	public void refresh(Consumer<Collection<R>> onRefresh) {
		refresher.doRefresh(onRefresh);
	}

	@Override
	public TableSelection<R> selection() {
		return selection;
	}

	@Override
	public TableConditionModel<C> conditions() {
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

	private void bindEvents() {
		addTableModelListener(removeSelectionListener);
		conditionModel.changed().addListener(modelItems::filter);
	}

	private List<Object> columnValues(Stream<Integer> rowIndexStream, int columnModelIndex) {
		return rowIndexStream.map(rowIndex -> getValueAt(rowIndex, columnModelIndex)).collect(toList());
	}

	private Map<C, ConditionModel<?>> createColumnFilterModels(ConditionModelFactory<C> filterModelFactory) {
		Map<C, ConditionModel<?>> columnFilterModels = new HashMap<>();
		for (C identifier : columns.identifiers()) {
			filterModelFactory.createConditionModel(identifier)
							.ifPresent(condition -> columnFilterModels.put(identifier, condition));
		}

		return columnFilterModels;
	}

	private final class DefaultRefresher extends AbstractFilterModelRefresher<R> {

		private final Event<Collection<R>> event = Event.event();
		private final Value<RefreshStrategy> refreshStrategy = Value.builder()
						.nonNull(RefreshStrategy.CLEAR)
						.build();

		private DefaultRefresher(Supplier<Collection<R>> supplier) {
			super(supplier);
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
							.forEach(items()::removeItem);
			items.forEach(this::merge);
		}

		private void merge(R item) {
			int index = modelItems.visible.indexOf(item);
			if (index == -1) {
				modelItems.addItemInternal(item);
			}
			else {
				modelItems.visible.setItemAt(index, item);
			}
		}

		private void clearAndAdd(Collection<R> items) {
			List<R> selectedItems = selection.items().get();
			modelItems.clear();
			if (items().addItems(items)) {
				modelItems.visible.sort();
			}
			selection.items().set(selectedItems);
		}

		private void doRefresh(Consumer<Collection<R>> onRefresh) {
			super.refresh(onRefresh);
		}
	}

	private final class DefaultFilterModelFactory implements ConditionModelFactory<C> {

		@Override
		public Optional<ConditionModel<?>> createConditionModel(C identifier) {
			Class<?> columnClass = getColumnClass(identifier);
			if (Comparable.class.isAssignableFrom(columnClass)) {
				return Optional.of(ConditionModel.builder(columnClass).build());
			}

			return Optional.empty();
		}
	}

	private final class DefaultItems implements Items<R> {

		private final Predicate<R> validator;
		private final DefaultVisibleItems visible = new DefaultVisibleItems();
		private final DefaultFilteredItems filtered = new DefaultFilteredItems();

		private DefaultItems(Predicate<R> validator) {
			this.validator = validator;
		}

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
			refresher.processResult(rejectNulls(items));
		}

		@Override
		public boolean addItems(Collection<R> items) {
			return addItemsAtInternal(visible.items.size(), rejectNulls(items));
		}

		@Override
		public boolean removeItem(R item) {
			return removeItemInternal(requireNonNull(item), true);
		}

		@Override
		public boolean removeItems(Collection<R> items) {
			rejectNulls(items);
			selection.setValueIsAdjusting(true);
			boolean visibleItemRemoved = false;
			for (R item : items) {
				visibleItemRemoved = removeItemInternal(item, false) || visibleItemRemoved;
			}
			selection.setValueIsAdjusting(false);
			if (visibleItemRemoved) {
				modelItems.visible.notifyChanges();
			}

			return visibleItemRemoved;
		}

		@Override
		public boolean addItem(R item) {
			return addItemInternal(requireNonNull(item));
		}

		@Override
		public Observer<Collection<R>> observer() {
			return refresher.event.observer();
		}

		@Override
		public VisibleItems<R> visible() {
			return visible;
		}

		@Override
		public FilteredItems<R> filtered() {
			return filtered;
		}

		@Override
		public boolean contains(R item) {
			return visible.contains(requireNonNull(item)) || filtered.contains(item);
		}

		@Override
		public int count() {
			return visible.count() + filtered.count();
		}

		@Override
		public void filter() {
			List<R> selectedItems = selection.items().get();
			visible.items.addAll(filtered.items);
			filtered.items.clear();
			for (ListIterator<R> visibleItemsIterator = visible.items.listIterator(); visibleItemsIterator.hasNext(); ) {
				R item = visibleItemsIterator.next();
				if (!visiblePredicate.test(item)) {
					visibleItemsIterator.remove();
					filtered.items.add(item);
				}
			}
			if (visible.comparator.isNotNull()) {
				visible.items.sort(visible.comparator.get());
			}
			fireTableDataChanged();
			filtered.notifyChanges();
			selection.items().set(selectedItems);
		}

		@Override
		public void clear() {
			int filteredSize = filtered.items.size();
			filtered.items.clear();
			int visibleSize = visible.items.size();
			visible.items.clear();
			if (visibleSize > 0) {
				fireTableRowsDeleted(0, visibleSize - 1);
			}
			if (filteredSize != 0) {
				filtered.notifyChanges();
			}
			if (visibleSize != 0) {
				visible.notifyChanges();
			}
		}

		private boolean addItemInternal(R item) {
			return addItemAtInternal(visible.count(), item);
		}

		private boolean addItemAtInternal(int index, R item) {
			validate(item);
			if (visiblePredicate.test(item)) {
				visible.items.add(index, item);
				fireTableRowsInserted(index, index);

				return true;
			}
			filtered.items.add(item);
			filtered.notifyChanges();

			return false;
		}

		private boolean addItemsAtInternal(int index, Collection<R> items) {
			requireNonNull(items);
			Collection<R> visibleItems = new ArrayList<>(items.size());
			Collection<R> filteredItems = new ArrayList<>(items.size());
			for (R item : items) {
				validate(item);
				if (visiblePredicate.test(item)) {
					visibleItems.add(item);
				}
				else {
					filteredItems.add(item);
				}
			}
			if (!visibleItems.isEmpty()) {
				visible.items.addAll(index, visibleItems);
				fireTableRowsInserted(index, index + visibleItems.size());
			}
			if (!filteredItems.isEmpty()) {
				filtered.items.addAll(filteredItems);
				filtered.notifyChanges();
			}

			return !visibleItems.isEmpty();
		}

		private boolean removeItemInternal(R item, boolean notifyDataChanged) {
			int visibleItemIndex = visible.items.indexOf(item);
			if (visibleItemIndex >= 0) {
				visible.items.remove(visibleItemIndex);
				fireTableRowsDeleted(visibleItemIndex, visibleItemIndex);
				if (notifyDataChanged) {
					visible.notifyChanges();
				}
			}
			else if (filtered.items.remove(item)) {
				filtered.notifyChanges();
			}

			return visibleItemIndex >= 0;
		}

		private void validate(R item) {
			if (!validator.test(item)) {
				throw new IllegalArgumentException("Invalid item: " + item);
			}
		}

		private static <T> Collection<T> rejectNulls(Collection<T> items) {
			for (T item : requireNonNull(items)) {
				requireNonNull(item);
			}

			return items;
		}

		private final class DefaultVisibleItems implements VisibleItems<R> {

			private final Value<Comparator<R>> comparator = Value.builder()
							.<Comparator<R>>nullable()
							.notify(Value.Notify.WHEN_SET)
							.listener(this::sort)
							.build();

			private final List<R> items = new ArrayList<>();
			private final Event<List<R>> event = Event.event();

			private DefaultVisibleItems() {
				addTableModelListener(e -> {
					if (e.getType() != TableModelEvent.DELETE) {
						// Deletions are handled differently, in order to trigger only a single
						// event when multiple visible items are removed, see removeItems()
						notifyChanges();
					}
				});
			}

			@Override
			public Value<Predicate<R>> predicate() {
				return visiblePredicate.predicate;
			}

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
				return items.contains(requireNonNull(item));
			}

			@Override
			public int indexOf(R item) {
				return items.indexOf(requireNonNull(item));
			}

			@Override
			public R itemAt(int index) {
				return items.get(index);
			}

			@Override
			public boolean addItemsAt(int index, Collection<R> items) {
				return addItemsAtInternal(index, rejectNulls(items));
			}

			@Override
			public boolean addItemAt(int index, R item) {
				return addItemAtInternal(index, requireNonNull(item));
			}

			@Override
			public boolean setItemAt(int index, R item) {
				validate(requireNonNull(item));
				if (visiblePredicate.test(item)) {
					items.set(index, item);
					fireTableRowsUpdated(index, index);

					return true;
				}

				return false;
			}

			@Override
			public R removeItemAt(int index) {
				R removed = items.remove(index);
				fireTableRowsDeleted(index, index);
				notifyChanges();

				return removed;
			}

			@Override
			public List<R> removeItems(int fromIndex, int toIndex) {
				List<R> subList = items.subList(fromIndex, toIndex);
				List<R> removedItems = new ArrayList<>(subList);
				subList.clear();
				fireTableRowsDeleted(fromIndex, toIndex);
				notifyChanges();

				return removedItems;
			}

			@Override
			public int count() {
				return items.size();
			}

			@Override
			public Value<Comparator<R>> comparator() {
				return comparator;
			}

			@Override
			public void sort() {
				if (comparator.isNotNull()) {
					List<R> selectedItems = selection.items().get();
					items.sort(comparator.get());
					fireTableRowsUpdated(0, items.size());
					selection.items().set(selectedItems);
				}
			}

			private void notifyChanges() {
				event.accept(get());
			}
		}

		private final class DefaultFilteredItems implements FilteredItems<R> {

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
				return items.contains(requireNonNull(item));
			}

			@Override
			public int count() {
				return items.size();
			}

			private void notifyChanges() {
				event.accept(get());
			}
		}
	}

	private final class VisiblePredicate implements Predicate<R> {

		private final Map<C, ConditionModel<?>> columnFilters;

		private final Value<Predicate<R>> predicate = Value.builder()
						.<Predicate<R>>nullable()
						.listener(modelItems::filter)
						.build();

		private VisiblePredicate(Map<C, ConditionModel<?>> columnFilters) {
			this.columnFilters = columnFilters;
		}

		@Override
		public boolean test(R item) {
			if (predicate.isNotNull() && !predicate.get().test(item)) {
				return false;
			}

			return columnFilters.entrySet().stream()
							.filter(entry -> entry.getValue().enabled().get())
							.allMatch(entry -> accepts(item, entry.getValue(), entry.getKey(), columns));
		}

		private boolean accepts(R item, ConditionModel<?> condition, C identifier, Columns<R, C> columns) {
			if (condition.valueClass().equals(String.class)) {
				String string = columns.string(item, identifier);

				return ((ConditionModel<String>) condition).accepts(string.isEmpty() ? null : string);
			}

			return condition.accepts(columns.comparable(item, identifier));
		}
	}

	private final class RemoveSelectionListener implements TableModelListener {

		@Override
		public void tableChanged(TableModelEvent e) {
			if (e.getType() == TableModelEvent.DELETE) {
				selection.removeIndexInterval(e.getFirstRow(), e.getLastRow());
			}
		}
	}

	static final class DefaultBuilder<R, C> implements Builder<R, C> {

		private final Columns<R, C> columns;

		private Supplier<? extends Collection<R>> supplier;
		private Predicate<R> validator = new ValidPredicate<>();
		private ConditionModelFactory<C> filterModelFactory;
		private RefreshStrategy refreshStrategy = RefreshStrategy.CLEAR;
		private boolean asyncRefresh = FilterModel.ASYNC_REFRESH.get();

		DefaultBuilder(Columns<R, C> columns) {
			if (requireNonNull(columns).identifiers().isEmpty()) {
				throw new IllegalArgumentException("No columns specified");
			}
			this.columns = validateIdentifiers(columns);
		}

		@Override
		public Builder<R, C> filterModelFactory(ConditionModelFactory<C> filterModelFactory) {
			this.filterModelFactory = requireNonNull(filterModelFactory);
			return this;
		}

		@Override
		public Builder<R, C> supplier(Supplier<? extends Collection<R>> supplier) {
			this.supplier = requireNonNull(supplier);
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
