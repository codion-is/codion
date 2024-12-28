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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.Event;
import is.codion.common.model.FilterModel;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.model.condition.TableConditionModel.ConditionModelFactory;
import is.codion.common.observable.Observer;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.AbstractFilterModelRefresher;
import is.codion.swing.common.model.component.table.FilterTableModel.FilterTableModelItems;
import is.codion.swing.common.model.component.table.FilterTableModel.RefreshStrategy;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.model.condition.TableConditionModel.tableConditionModel;
import static is.codion.common.value.Value.Notify.WHEN_SET;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableItems<R, C> implements FilterTableModelItems<R> {

	private final Lock lock = new Lock() {};

	private final AbstractTableModel tableModel;
	private final Predicate<R> validator;
	private final VisiblePredicate visiblePredicate;
	private final DefaultVisibleItems visible;
	private final DefaultFilteredItems filtered;

	final TableColumns<R, C> columns;
	final FilterTableModel.TableSelection<R> selection;
	final TableConditionModel<C> filters;
	final FilterTableSortModel<R, C> sorter;

	final DefaultRefresher refresher;
	final Value<RefreshStrategy> refreshStrategy;

	DefaultFilterTableItems(AbstractTableModel tableModel, Predicate<R> validator, Supplier<? extends Collection<R>> supplier,
													RefreshStrategy refreshStrategy, boolean asyncRefresh, TableColumns<R, C> columns, ConditionModelFactory<C> filterModelFactory) {
		this.tableModel = requireNonNull(tableModel);
		this.columns = requireNonNull(columns);
		this.validator = validator;
		this.refresher = new DefaultRefresher(supplier == null ? this::get : supplier, asyncRefresh);
		this.refreshStrategy = Value.builder()
						.nonNull(RefreshStrategy.CLEAR)
						.value(refreshStrategy)
						.build();
		this.visible = new DefaultVisibleItems();
		this.filtered = new DefaultFilteredItems();
		this.filters = tableConditionModel(createFilterConditionModels(filterModelFactory));
		this.visiblePredicate = new VisiblePredicate();
		this.selection = new DefaultFilterTableSelection<>(this);
		this.sorter = new DefaultFilterTableSortModel<>(columns);
		this.sorter.observer().addListener(visible::sort);
	}

	@Override
	public FilterModel.Refresher<R> refresher() {
		return refresher;
	}

	@Override
	public void refresh() {
		refresher.doRefresh(null);
	}

	@Override
	public void refresh(Consumer<Collection<R>> onRefresh) {
		refresher.doRefresh(requireNonNull(onRefresh));
	}

	@Override
	public Value<RefreshStrategy> refreshStrategy() {
		return refreshStrategy;
	}

	@Override
	public Collection<R> get() {
		synchronized (lock) {
			if (filtered.items.isEmpty()) {
				return unmodifiableCollection(new ArrayList<>(visible.items));
			}
			List<R> entities = new ArrayList<>(visible.items.size() + filtered.items.size());
			entities.addAll(visible.items);
			entities.addAll(filtered.items);

			return unmodifiableList(entities);
		}
	}

	@Override
	public void set(Collection<R> items) {
		rejectNulls(items);
		synchronized (lock) {
			if (refreshStrategy.isEqualTo(RefreshStrategy.MERGE) && !items.isEmpty()) {
				merge(items);
			}
			else {
				clearAndAdd(items);
			}
		}
	}

	@Override
	public boolean add(Collection<R> items) {
		synchronized (lock) {
			return addInternal(visible.items.size(), rejectNulls(items));
		}
	}

	@Override
	public boolean remove(R item) {
		synchronized (lock) {
			return removeInternal(requireNonNull(item), true);
		}
	}

	@Override
	public boolean remove(Collection<R> items) {
		rejectNulls(items);
		synchronized (lock) {
			selection.setValueIsAdjusting(true);
			boolean visibleRemoved = false;
			for (R item : items) {
				visibleRemoved = removeInternal(item, false) || visibleRemoved;
			}
			selection.setValueIsAdjusting(false);
			if (visibleRemoved) {
				visible.notifyChanges();
			}

			return visibleRemoved;
		}
	}

	@Override
	public boolean add(R item) {
		synchronized (lock) {
			return addInternal(requireNonNull(item));
		}
	}

	@Override
	public Observer<Collection<R>> observer() {
		return refresher.event.observer();
	}

	@Override
	public FilterModel.VisibleItems<R> visible() {
		return visible;
	}

	@Override
	public FilterModel.FilteredItems<R> filtered() {
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
		synchronized (lock) {
			visible.items.addAll(filtered.items);
			filtered.items.clear();
			for (ListIterator<R> visibleItemsIterator = visible.items.listIterator(); visibleItemsIterator.hasNext(); ) {
				R item = visibleItemsIterator.next();
				if (!visiblePredicate.test(item)) {
					visibleItemsIterator.remove();
					filtered.items.add(item);
				}
			}
			if (sorter.sorted()) {
				visible.items.sort(sorter.comparator());
			}
			tableModel.fireTableDataChanged();
			filtered.notifyChanges();
		}
		selection.items().set(selectedItems);
	}

	@Override
	public void clear() {
		synchronized (lock) {
			int filteredSize = filtered.items.size();
			filtered.items.clear();
			int visibleSize = visible.items.size();
			visible.items.clear();
			if (visibleSize > 0) {
				tableModel.fireTableRowsDeleted(0, visibleSize - 1);
			}
			if (filteredSize != 0) {
				filtered.notifyChanges();
			}
			if (visibleSize != 0) {
				visible.notifyChanges();
			}
		}
	}

	private void merge(Collection<R> items) {
		Set<R> itemSet = new HashSet<>(items);
		get().stream()
						.filter(item -> !itemSet.contains(item))
						.forEach(this::remove);
		items.forEach(this::merge);
	}

	private void merge(R item) {
		int index = visible.indexOf(item);
		if (index == -1) {
			addInternal(item);
		}
		else {
			visible.set(index, item);
		}
	}

	private void clearAndAdd(Collection<R> items) {
		List<R> selectedItems = selection.items().get();
		clear();
		if (addInternal(visible.items.size(), items)) {
			visible.sort();
		}
		selection.items().set(selectedItems);
	}

	private boolean addInternal(R item) {
		return addInternal(visible.count(), item);
	}

	private boolean addInternal(int index, R item) {
		validate(item);
		if (visiblePredicate.test(item)) {
			visible.items.add(index, item);
			tableModel.fireTableRowsInserted(index, index);

			return true;
		}
		filtered.items.add(item);
		filtered.notifyChanges();

		return false;
	}

	private boolean addInternal(int index, Collection<R> items) {
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
			tableModel.fireTableRowsInserted(index, index + visibleItems.size());
		}
		if (!filteredItems.isEmpty()) {
			filtered.items.addAll(filteredItems);
			filtered.notifyChanges();
		}

		return !visibleItems.isEmpty();
	}

	private boolean removeInternal(R item, boolean notifyDataChanged) {
		int visibleItemIndex = visible.items.indexOf(item);
		if (visibleItemIndex >= 0) {
			visible.items.remove(visibleItemIndex);
			tableModel.fireTableRowsDeleted(visibleItemIndex, visibleItemIndex);
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

	private Map<C, ConditionModel<?>> createFilterConditionModels(ConditionModelFactory<C> filterModelFactory) {
		Map<C, ConditionModel<?>> columnFilterModels = new HashMap<>();
		for (C identifier : columns.identifiers()) {
			filterModelFactory.create(identifier)
							.ifPresent(condition -> columnFilterModels.put(identifier, condition));
		}

		return columnFilterModels;
	}

	private static <T> Collection<T> rejectNulls(Collection<T> items) {
		for (T item : requireNonNull(items)) {
			requireNonNull(item);
		}

		return items;
	}

	final class DefaultRefresher extends AbstractFilterModelRefresher<R> {

		private final Event<Collection<R>> event = Event.event();

		private DefaultRefresher(Supplier<? extends Collection<R>> supplier, boolean asyncRefresh) {
			super((Supplier<Collection<R>>) supplier);
			async().set(asyncRefresh);
		}

		@Override
		protected void processResult(Collection<R> items) {
			set(items);
			event.accept(unmodifiableCollection(items));
		}

		void doRefresh(Consumer<Collection<R>> onRefresh) {
			super.refresh(onRefresh);
		}
	}

	private final class VisiblePredicate implements Predicate<R> {

		private final Value<Predicate<R>> predicate;

		private VisiblePredicate() {
			predicate = Value.builder()
							.<Predicate<R>>nullable()
							.notify(WHEN_SET)
							.listener(DefaultFilterTableItems.this::filter)
							.build();
			filters.changed().addListener(DefaultFilterTableItems.this::filter);
		}

		@Override
		public boolean test(R item) {
			if (predicate.isNotNull() && !predicate.getOrThrow().test(item)) {
				return false;
			}

			return filters.get().entrySet().stream()
							.filter(entry -> entry.getValue().enabled().get())
							.allMatch(entry -> accepts(item, entry.getValue(), entry.getKey(), columns));
		}

		private boolean accepts(R item, ConditionModel<?> condition, C identifier, TableColumns<R, C> columns) {
			if (condition.valueClass().equals(String.class)) {
				String string = columns.string(item, identifier);

				return ((ConditionModel<String>) condition).accepts(string.isEmpty() ? null : string);
			}

			return condition.accepts(columns.comparable(item, identifier));
		}
	}

	private final class DefaultVisibleItems implements FilterModel.VisibleItems<R> {

		private final List<R> items = new ArrayList<>();
		private final Event<List<R>> event = Event.event();

		private DefaultVisibleItems() {
			tableModel.addTableModelListener(e -> {
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
			synchronized (lock) {
				return unmodifiableList(items);
			}
		}

		@Override
		public Observer<List<R>> observer() {
			return event.observer();
		}

		@Override
		public boolean contains(R item) {
			synchronized (lock) {
				return items.contains(requireNonNull(item));
			}
		}

		@Override
		public int indexOf(R item) {
			synchronized (lock) {
				return items.indexOf(requireNonNull(item));
			}
		}

		@Override
		public R get(int index) {
			synchronized (lock) {
				return items.get(index);
			}
		}

		@Override
		public boolean add(int index, Collection<R> items) {
			synchronized (lock) {
				return addInternal(index, rejectNulls(items));
			}
		}

		@Override
		public boolean add(int index, R item) {
			synchronized (lock) {
				return addInternal(index, requireNonNull(item));
			}
		}

		@Override
		public boolean set(int index, R item) {
			validate(requireNonNull(item));
			synchronized (lock) {
				if (visiblePredicate.test(item)) {
					items.set(index, item);
					tableModel.fireTableRowsUpdated(index, index);

					return true;
				}
			}

			return false;
		}

		@Override
		public R remove(int index) {
			synchronized (lock) {
				R removed = items.remove(index);
				tableModel.fireTableRowsDeleted(index, index);
				notifyChanges();

				return removed;
			}
		}

		@Override
		public List<R> remove(int fromIndex, int toIndex) {
			synchronized (lock) {
				List<R> subList = items.subList(fromIndex, toIndex);
				List<R> removedItems = new ArrayList<>(subList);
				subList.clear();
				tableModel.fireTableRowsDeleted(fromIndex, toIndex);
				notifyChanges();

				return removedItems;
			}
		}

		@Override
		public int count() {
			synchronized (lock) {
				return items.size();
			}
		}

		@Override
		public void sort() {
			if (sorter.sorted()) {
				List<R> selectedItems = selection.items().get();
				synchronized (lock) {
					items.sort(sorter.comparator());
					tableModel.fireTableRowsUpdated(0, items.size());
				}
				selection.items().set(selectedItems);
			}
		}

		private void notifyChanges() {
			event.accept(get());
		}
	}

	private final class DefaultFilteredItems implements FilterModel.FilteredItems<R> {

		private final List<R> items = new ArrayList<>();
		private final Event<Collection<R>> event = Event.event();

		@Override
		public Collection<R> get() {
			synchronized (lock) {
				return unmodifiableCollection(items);
			}
		}

		@Override
		public Observer<Collection<R>> observer() {
			return event.observer();
		}

		@Override
		public boolean contains(R item) {
			synchronized (lock) {
				return items.contains(requireNonNull(item));
			}
		}

		@Override
		public int count() {
			synchronized (lock) {
				return items.size();
			}
		}

		private void notifyChanges() {
			event.accept(get());
		}
	}

	private interface Lock {}
}
