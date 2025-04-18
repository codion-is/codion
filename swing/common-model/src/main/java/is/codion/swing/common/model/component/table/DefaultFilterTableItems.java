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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.Event;
import is.codion.common.model.FilterModel;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.observable.Observer;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.AbstractFilterModelRefresher;
import is.codion.swing.common.model.component.table.FilterTableModel.FilterTableModelItems;
import is.codion.swing.common.model.component.table.FilterTableModel.RefreshStrategy;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.swing.common.model.component.table.FilterTableModel.TableSelection;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.model.condition.TableConditionModel.tableConditionModel;
import static is.codion.common.value.Value.Notify.WHEN_SET;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableItems<R, C> implements FilterTableModelItems<R> {

	private final Lock lock = new Lock() {};

	private final AbstractTableModel tableModel;
	private final Predicate<R> validator;
	private final VisiblePredicate visiblePredicate;
	private final DefaultVisibleItems visible;
	private final DefaultFilteredItems filtered;

	final TableColumns<R, C> columns;
	final TableSelection<R> selection;
	final TableConditionModel<C> filters;
	final FilterTableSort<R, C> sort;

	final DefaultRefresher refresher;
	final Value<RefreshStrategy> refreshStrategy;

	DefaultFilterTableItems(AbstractTableModel tableModel, Predicate<R> validator, Supplier<? extends Collection<R>> supplier,
													RefreshStrategy refreshStrategy, boolean asyncRefresh, TableColumns<R, C> columns,
													Supplier<Map<C, ConditionModel<?>>> filterModelFactory) {
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
		this.filters = tableConditionModel(filterModelFactory);
		this.visiblePredicate = new VisiblePredicate();
		this.selection = new DefaultFilterTableSelection<>(this);
		this.sort = new DefaultFilterTableSort<>(columns);
		this.sort.observer().addListener(visible::sort);
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
	public void refresh(Consumer<Collection<R>> onResult) {
		refresher.doRefresh(requireNonNull(onResult));
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
	public void add(Collection<R> items) {
		synchronized (lock) {
			addInternal(visible.items.size(), rejectNulls(items));
		}
	}

	@Override
	public void remove(R item) {
		requireNonNull(item);
		synchronized (lock) {
			if (!filtered.items.remove(item)) {
				int index = visible.items.indexOf(item);
				if (index >= 0) {
					visible.items.remove(index);
					tableModel.fireTableRowsDeleted(index, index);
					visible.notifyChanges();
				}
			}
		}
	}

	@Override
	public void remove(Collection<R> items) {
		rejectNulls(items);
		synchronized (lock) {
			Set<R> toRemove = new HashSet<>(items);
			for (R itemToRemove : items) {
				if (filtered.items.remove(itemToRemove)) {
					toRemove.remove(itemToRemove);
				}
			}
			boolean visibleRemoved = false;
			selection.setValueIsAdjusting(true);
			ListIterator<R> iterator = visible.items.listIterator(visible.items.size());
			while (!toRemove.isEmpty() && iterator.hasPrevious()) {
				int index = iterator.previousIndex();
				R item = iterator.previous();
				if (toRemove.remove(item)) {
					iterator.remove();
					tableModel.fireTableRowsDeleted(index, index);
					visibleRemoved = true;
				}
			}
			selection.setValueIsAdjusting(false);
			if (visibleRemoved) {
				visible.notifyChanges();
			}
		}
	}

	@Override
	public void replace(R item, R replacement) {
		replace(singletonMap(requireNonNull(item), requireNonNull(replacement)));
	}

	@Override
	public void replace(Map<R, R> items) {
		// There is practically a carbon copy of this method in DefaultFilterComboBoxModel, fix both please
		requireNonNull(items).values().forEach(this::validate);
		synchronized (lock) {
			Map<R, R> toReplace = new HashMap<>(items);
			for (R itemToReplace : items.keySet()) {
				if (filtered.items.remove(itemToReplace)) {
					R replacement = toReplace.remove(itemToReplace);
					if (visiblePredicate.test(replacement)) {
						visible.items.add(replacement);
					}
					else {
						filtered.items.add(replacement);
					}
				}
			}
			ListIterator<R> iterator = visible.items.listIterator();
			while (!toReplace.isEmpty() && iterator.hasNext()) {
				R item = iterator.next();
				R replacement = toReplace.remove(item);
				if (replacement != null) {
					if (visiblePredicate.test(replacement)) {
						iterator.set(replacement);
					}
					else {
						iterator.remove();
						filtered.items.add(replacement);
					}
				}
			}
			tableModel.fireTableDataChanged();
			visible.sort();
		}
	}

	@Override
	public void add(R item) {
		synchronized (lock) {
			addInternal(visible.items.size(), singleton(requireNonNull(item)));
		}
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
			if (sort.sorted()) {
				visible.items.sort(sort.comparator());
			}
			tableModel.fireTableDataChanged();
		}
		selection.items().set(selectedItems);
	}

	@Override
	public void clear() {
		synchronized (lock) {
			filtered.items.clear();
			int visibleSize = visible.items.size();
			visible.items.clear();
			if (visibleSize > 0) {
				tableModel.fireTableRowsDeleted(0, visibleSize - 1);
				visible.notifyChanges();
			}
		}
	}

	private void merge(Collection<R> items) {
		items.forEach(this::validate);
		Set<R> itemSet = new HashSet<>(items);
		get().stream()
						.filter(item -> !itemSet.contains(item))
						.forEach(this::remove);
		items.forEach(this::merge);
	}

	private void merge(R item) {
		int index = visible.indexOf(item);
		if (index == -1) {
			int visibleCount = visible.count();
			if (visiblePredicate.test(item)) {
				visible.items.add(visibleCount, item);
				tableModel.fireTableRowsInserted(visibleCount, visibleCount);
				visible.notifyAdded(singleton(item));

				return;
			}
			filtered.items.add(item);
		}
		else if (visiblePredicate.test(item)) {
			visible.items.set(index, item);
			tableModel.fireTableRowsUpdated(index, index);
		}
	}

	private void clearAndAdd(Collection<R> items) {
		List<R> selectedItems = selection.items().get();
		clear();
		addInternal(0, items);
		selection.items().set(selectedItems);
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
			visible.sort();
			visible.notifyAdded(visibleItems);
		}
		if (!filteredItems.isEmpty()) {
			filtered.items.addAll(filteredItems);
		}

		return !visibleItems.isEmpty();
	}

	private void validate(R item) {
		if (!validator.test(requireNonNull(item))) {
			throw new IllegalArgumentException("Invalid item: " + item);
		}
	}

	private static <T> Collection<T> rejectNulls(Collection<T> items) {
		for (T item : requireNonNull(items)) {
			requireNonNull(item);
		}

		return items;
	}

	final class DefaultRefresher extends AbstractFilterModelRefresher<R> {

		private final Event<Collection<R>> onResult = Event.event();

		private DefaultRefresher(Supplier<? extends Collection<R>> supplier, boolean asyncRefresh) {
			super((Supplier<Collection<R>>) supplier);
			async().set(asyncRefresh);
		}

		@Override
		protected void processResult(Collection<R> result) {
			set(result);
			onResult.accept(unmodifiableCollection(result));
		}

		void doRefresh(Consumer<Collection<R>> onResult) {
			super.refresh(onResult);
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
			if (!predicate.isNull() && !predicate.getOrThrow().test(item)) {
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
		private final Event<Collection<R>> added = Event.event();

		private DefaultVisibleItems() {
			tableModel.addTableModelListener(e -> {
				if (e.getType() != TableModelEvent.DELETE) {
					// Deletions are handled differently, in order to trigger only a single
					// event when multiple visible items are removed, see remove(Collection)
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
		public Observer<Collection<R>> added() {
			return added.observer();
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
				return addInternal(index, singleton(requireNonNull(item)));
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
			if (sort.sorted()) {
				List<R> selectedItems = selection.items().get();
				synchronized (lock) {
					items.sort(sort.comparator());
					tableModel.fireTableRowsUpdated(0, items.size());
				}
				selection.items().set(selectedItems);
			}
		}

		private void notifyAdded(Collection<R> addedItems) {
			added.accept(addedItems);
		}

		private void notifyChanges() {
			event.accept(get());
		}
	}

	private final class DefaultFilteredItems implements FilterModel.FilteredItems<R> {

		private final Set<R> items = new LinkedHashSet<>();

		@Override
		public Collection<R> get() {
			synchronized (lock) {
				return unmodifiableCollection(items);
			}
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
	}

	private interface Lock {}
}
