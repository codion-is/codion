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
package is.codion.common.model.filter;

import is.codion.common.event.Event;
import is.codion.common.model.filter.FilterModel.FilteredItems;
import is.codion.common.model.filter.FilterModel.Items;
import is.codion.common.model.filter.FilterModel.RefreshStrategy;
import is.codion.common.model.filter.FilterModel.Refresher;
import is.codion.common.model.filter.FilterModel.Sort;
import is.codion.common.model.filter.FilterModel.VisibleItems;
import is.codion.common.model.filter.FilterModel.VisibleItems.ItemsListener;
import is.codion.common.model.filter.FilterModel.VisiblePredicate;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.observable.Observer;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;

import org.jspecify.annotations.Nullable;

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
import java.util.function.Function;
import java.util.function.Predicate;

import static is.codion.common.value.Value.Notify.SET;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultFilterModelItems<R> implements Items<R> {

	static final Builder.RefresherBuilder REFRESHER = new DefaultRefresherBuilder();

	private final Lock lock = new Lock() {};

	private final Predicate<R> validator;
	private final DefaultVisibleItems visible;
	private final DefaultFilteredItems filtered;
	private final ItemsListener itemsListener;

	private final MultiSelection<R> selection;
	private final Sort<R> sort;
	private final Refresher<R> refresher;
	private final Value<RefreshStrategy> refreshStrategy;

	private DefaultFilterModelItems(DefaultBuilder<R> builder) {
		this.sort = builder.sort;
		this.validator = builder.validator;
		this.visible = new DefaultVisibleItems(builder.visiblePredicate);
		this.filtered = new DefaultFilteredItems();
		this.refresher = builder.refresher.apply(this);
		this.selection = builder.selection.apply(visible);
		this.itemsListener = builder.itemsListener;
		this.refreshStrategy = Value.builder()
						.nonNull(RefreshStrategy.CLEAR)
						.value(builder.refreshStrategy)
						.build();
		this.visible.predicate.addListener(DefaultFilterModelItems.this::filter);
		this.sort.observer().addListener(visible::sort);
	}

	@Override
	public Refresher<R> refresher() {
		return refresher;
	}

	@Override
	public void refresh() {
		refresher.refresh(null);
	}

	@Override
	public void refresh(Consumer<Collection<R>> onResult) {
		refresher.refresh(requireNonNull(onResult));
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
					itemsListener.deleted(index, index);
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
			selection.adjusting(true);
			ListIterator<R> iterator = visible.items.listIterator(visible.items.size());
			while (!toRemove.isEmpty() && iterator.hasPrevious()) {
				int index = iterator.previousIndex();
				R item = iterator.previous();
				if (toRemove.remove(item)) {
					iterator.remove();
					itemsListener.deleted(index, index);
					visibleRemoved = true;
				}
			}
			selection.adjusting(false);
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
		// Note: Similar logic exists in DefaultFilterComboBoxModel in swing-common-model module.
		// Both implementations handle item replacement with filtering but have different collection types
		// and threading requirements, making extraction to a common utility non-trivial.
		requireNonNull(items).values().forEach(this::validate);
		synchronized (lock) {
			Map<R, R> toReplace = new HashMap<>(items);
			for (R itemToReplace : items.keySet()) {
				if (filtered.items.remove(itemToReplace)) {
					R replacement = toReplace.remove(itemToReplace);
					if (visible.predicate.test(replacement)) {
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
					if (visible.predicate.test(replacement)) {
						iterator.set(replacement);
					}
					else {
						iterator.remove();
						filtered.items.add(replacement);
					}
				}
			}
			itemsListener.changed();
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
		synchronized (lock) {
			filterIncremental();
			if (sort.sorted()) {
				visible.items.sort(sort);
			}
			itemsListener.changed();
			visible.notifyChanges();
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
				itemsListener.deleted(0, visibleSize - 1);
				visible.notifyChanges();
			}
		}
	}

	/**
	 * Performs incremental filtering by only moving items between visible and filtered collections
	 * instead of rebuilding the entire visible list. This optimizes performance for large datasets
	 * by avoiding unnecessary operations on items that are already in the correct collection.
	 */
	private void filterIncremental() {
		// First pass: move items from filtered to visible if they now pass the predicate
		filtered.items.removeIf(item -> {
			if (visible.predicate.test(item)) {
				visible.items.add(item);

				return true; // Remove from filtered
			}

			return false; // Keep in filtered
		});

		// Second pass: move items from visible to filtered if they no longer pass the predicate
		visible.items.removeIf(item -> {
			if (!visible.predicate.test(item)) {
				filtered.items.add(item);

				return true; // Remove from visible
			}

			return false; // Keep in visible
		});
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
			if (visible.predicate.test(item)) {
				visible.items.add(visibleCount, item);
				itemsListener.inserted(visibleCount, visibleCount);
				visible.notifyChanges();
				visible.notifyAdded(singleton(item));

				return;
			}
			filtered.items.add(item);
		}
		else if (visible.predicate.test(item)) {
			visible.items.set(index, item);
			itemsListener.updated(index, index);
			visible.notifyChanges();
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
			if (visible.predicate.test(item)) {
				visibleItems.add(item);
			}
			else {
				filteredItems.add(item);
			}
		}
		if (!visibleItems.isEmpty()) {
			visible.items.addAll(index, visibleItems);
			itemsListener.inserted(index, index + visibleItems.size());
			visible.notifyChanges();
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

	private final class DefaultVisibleItems implements VisibleItems<R> {

		private final List<R> items = new ArrayList<>();
		private final VisiblePredicate<R> predicate;
		private final Event<List<R>> event = Event.event();
		private final Event<Collection<R>> added = Event.event();

		private DefaultVisibleItems(VisiblePredicate<R> predicate) {
			this.predicate = predicate;
		}

		@Override
		public VisiblePredicate<R> predicate() {
			return predicate;
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
		public MultiSelection<R> selection() {
			return selection;
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
				if (predicate.test(item)) {
					items.set(index, item);
					itemsListener.updated(index, index);
					visible.notifyChanges();

					return true;
				}
			}

			return false;
		}

		@Override
		public R remove(int index) {
			synchronized (lock) {
				R removed = items.remove(index);
				itemsListener.deleted(index, index);
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
				itemsListener.deleted(fromIndex, toIndex);
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
					items.sort(sort);
					itemsListener.updated(0, items.size());
					notifyChanges();
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

	private final class DefaultFilteredItems implements FilteredItems<R> {

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

	private static final class DefaultRefresherBuilder implements Builder.RefresherBuilder {

		@Override
		public <T> Builder.SelectionBuilder<T> refresher(Function<Items<T>, Refresher<T>> refresher) {
			return new DefaultSelectionBuilder<>(requireNonNull(refresher));
		}
	}

	private static final class DefaultSelectionBuilder<T> implements Builder.SelectionBuilder<T> {

		private final Function<Items<T>, Refresher<T>> refresher;

		private DefaultSelectionBuilder(Function<Items<T>, Refresher<T>> refresher) {
			this.refresher = refresher;
		}

		@Override
		public Builder.SortBuilder<T> selection(Function<VisibleItems<T>, MultiSelection<T>> selection) {
			return new DefaultSortBuilder<>(refresher, requireNonNull(selection));
		}
	}

	private static final class DefaultSortBuilder<T> implements Builder.SortBuilder<T> {

		private final Function<Items<T>, Refresher<T>> refresher;
		private final Function<VisibleItems<T>, MultiSelection<T>> selectionFunction;

		private DefaultSortBuilder(Function<Items<T>, Refresher<T>> refresher,
															 Function<VisibleItems<T>, MultiSelection<T>> selection) {
			this.refresher = refresher;
			this.selectionFunction = selection;
		}

		@Override
		public Builder<T> sort(Sort<T> sort) {
			return new DefaultBuilder<>(selectionFunction, refresher, requireNonNull(sort));
		}
	}

	private static final class DefaultBuilder<T> implements Builder<T> {

		private final Function<VisibleItems<T>, MultiSelection<T>> selection;
		private final Function<Items<T>, Refresher<T>> refresher;
		private final Sort<T> sort;

		private VisiblePredicate<T> visiblePredicate = new DefaultVisiblePredicate<>();
		private Predicate<T> validator = new ValidPredicate<>();
		private RefreshStrategy refreshStrategy = RefreshStrategy.CLEAR;
		private ItemsListener itemsListener = new DefaultItemsListener();

		private DefaultBuilder(Function<VisibleItems<T>, MultiSelection<T>> selection,
													 Function<Items<T>, Refresher<T>> refresher, Sort<T> sort) {
			this.selection = requireNonNull(selection);
			this.refresher = requireNonNull(refresher);
			this.sort = requireNonNull(sort);
		}

		@Override
		public Builder<T> validator(Predicate<T> validator) {
			this.validator = requireNonNull(validator);
			return this;
		}

		@Override
		public Builder<T> visiblePredicate(VisiblePredicate<T> visiblePredicate) {
			this.visiblePredicate = requireNonNull(visiblePredicate);
			return this;
		}

		@Override
		public Builder<T> refreshStrategy(RefreshStrategy refreshStrategy) {
			this.refreshStrategy = requireNonNull(refreshStrategy);
			return this;
		}

		@Override
		public Builder<T> listener(ItemsListener itemsListener) {
			this.itemsListener = requireNonNull(itemsListener);
			return this;
		}

		@Override
		public Items<T> build() {
			return new DefaultFilterModelItems<>(this);
		}
	}

	private static final class DefaultItemsListener implements ItemsListener {

		@Override
		public void inserted(int firstIndex, int lastIndex) {}

		@Override
		public void updated(int firstIndex, int lastIndex) {}

		@Override
		public void deleted(int firstIndex, int lastIndex) {}

		@Override
		public void changed() {}
	}

	private static final class ValidPredicate<R> implements Predicate<R> {

		@Override
		public boolean test(R item) {
			return true;
		}
	}

	private static final class DefaultVisiblePredicate<R>
					extends AbstractValue<Predicate<R>> implements VisiblePredicate<R> {

		private @Nullable Predicate<R> predicate;

		private DefaultVisiblePredicate() {
			super(SET);
		}

		@Override
		protected @Nullable Predicate<R> getValue() {
			return predicate;
		}

		@Override
		protected void setValue(@Nullable Predicate<R> predicate) {
			this.predicate = predicate;
		}
	}

	private interface Lock {}
}
