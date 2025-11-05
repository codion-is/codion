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

import is.codion.common.model.filter.FilterModel.FilteredItems;
import is.codion.common.model.filter.FilterModel.IncludedItems;
import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;
import is.codion.common.model.filter.FilterModel.IncludedPredicate;
import is.codion.common.model.filter.FilterModel.Items;
import is.codion.common.model.filter.FilterModel.Refresher;
import is.codion.common.model.filter.FilterModel.Sort;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.value.AbstractValue;

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
import java.util.stream.Stream;

import static is.codion.common.reactive.value.Value.Notify.SET;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultFilterModelItems<R> implements Items<R> {

	private final Lock lock = new Lock() {};

	private final Predicate<R> validator;
	private final DefaultIncludedItems included;
	private final DefaultFilteredItems filtered;
	private final ItemsListener itemsListener;

	private final MultiSelection<R> selection;
	private final Sort<R> sort;
	private final Refresher<R> refresher;

	private DefaultFilterModelItems(DefaultBuilder<R> builder) {
		this.sort = builder.sort;
		this.validator = builder.validator;
		this.included = new DefaultIncludedItems(builder.included);
		this.filtered = new DefaultFilteredItems();
		this.refresher = builder.refresher.apply(this);
		this.selection = builder.selection.apply(included);
		this.itemsListener = builder.itemsListener;
		this.included.predicate.addListener(DefaultFilterModelItems.this::filter);
		this.sort.observer().addListener(included::sort);
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
	public Collection<R> get() {
		synchronized (lock) {
			if (filtered.items.isEmpty()) {
				return unmodifiableCollection(new ArrayList<>(included.items));
			}
			List<R> entities = new ArrayList<>(included.items.size() + filtered.items.size());
			entities.addAll(included.items);
			entities.addAll(filtered.items);

			return unmodifiableList(entities);
		}
	}

	@Override
	public void set(Collection<R> items) {
		rejectNulls(items);
		synchronized (lock) {
			List<R> selectedItems = selection.items().get();
			selection.adjusting(true);
			try {
				clear();
				addInternal(0, items);
				selection.items().set(selectedItems);
			}
			finally {
				selection.adjusting(false);
			}
		}
	}

	@Override
	public void add(Collection<R> items) {
		synchronized (lock) {
			addInternal(included.items.size(), rejectNulls(items));
		}
	}

	@Override
	public void remove(R item) {
		requireNonNull(item);
		synchronized (lock) {
			if (!filtered.items.remove(item)) {
				int index = included.items.indexOf(item);
				if (index >= 0) {
					included.items.remove(index);
					itemsListener.deleted(index, index);
					included.notifyChanges();
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
			boolean includedRemoved = false;
			selection.adjusting(true);
			ListIterator<R> iterator = included.items.listIterator(included.items.size());
			while (!toRemove.isEmpty() && iterator.hasPrevious()) {
				int index = iterator.previousIndex();
				R item = iterator.previous();
				if (toRemove.remove(item)) {
					iterator.remove();
					itemsListener.deleted(index, index);
					includedRemoved = true;
				}
			}
			selection.adjusting(false);
			if (includedRemoved) {
				included.notifyChanges();
			}
		}
	}

	@Override
	public void remove(Predicate<R> predicate) {
		requireNonNull(predicate);
		synchronized (lock) {
			remove(Stream.concat(included.items.stream(), filtered.items.stream())
							.filter(predicate)
							.collect(toList()));
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
					if (included.predicate.test(replacement)) {
						included.items.add(replacement);
					}
					else {
						filtered.items.add(replacement);
					}
				}
			}
			ListIterator<R> iterator = included.items.listIterator();
			while (!toReplace.isEmpty() && iterator.hasNext()) {
				R item = iterator.next();
				R replacement = toReplace.remove(item);
				if (replacement != null) {
					if (included.predicate.test(replacement)) {
						iterator.set(replacement);
					}
					else {
						iterator.remove();
						filtered.items.add(replacement);
					}
				}
			}
			itemsListener.changed();
			included.sort();
		}
	}

	@Override
	public void add(R item) {
		synchronized (lock) {
			addInternal(included.items.size(), singleton(requireNonNull(item)));
		}
	}

	@Override
	public IncludedItems<R> included() {
		return included;
	}

	@Override
	public FilteredItems<R> filtered() {
		return filtered;
	}

	@Override
	public boolean contains(R item) {
		return included.contains(requireNonNull(item)) || filtered.contains(item);
	}

	@Override
	public int size() {
		return included.size() + filtered.size();
	}

	@Override
	public void filter() {
		List<R> selectedItems = selection.items().get();
		synchronized (lock) {
			filterIncremental();
			if (sort.sorted()) {
				included.items.sort(sort);
			}
			itemsListener.changed();
			included.notifyChanges();
		}
		selection.items().set(selectedItems);
	}

	@Override
	public void clear() {
		synchronized (lock) {
			filtered.items.clear();
			int includedSize = included.items.size();
			included.items.clear();
			if (includedSize > 0) {
				itemsListener.deleted(0, includedSize - 1);
				included.notifyChanges();
			}
		}
	}

	/**
	 * Performs incremental filtering by only moving items between included and filtered collections
	 * instead of rebuilding the entire included list. This optimizes performance for large datasets
	 * by avoiding unnecessary operations on items that are already in the correct collection.
	 */
	private void filterIncremental() {
		// First pass: move items from excluded to included if they now pass the predicate
		filtered.items.removeIf(item -> {
			if (included.predicate.test(item)) {
				included.items.add(item);

				return true; // Remove from filtered
			}

			return false; // Keep in filtered
		});

		// Second pass: move items from included to filtered if they no longer pass the predicate
		included.items.removeIf(item -> {
			if (!included.predicate.test(item)) {
				filtered.items.add(item);

				return true; // Remove from included
			}

			return false; // Keep in included
		});
	}

	private boolean addInternal(int index, Collection<R> items) {
		Collection<R> includedItems = new ArrayList<>(items.size());
		Collection<R> filteredItems = new ArrayList<>(items.size());
		for (R item : items) {
			validate(item);
			if (included.predicate.test(item)) {
				includedItems.add(item);
			}
			else {
				filteredItems.add(item);
			}
		}
		if (!includedItems.isEmpty()) {
			included.items.addAll(index, includedItems);
			itemsListener.inserted(index, index + includedItems.size());
			included.notifyChanges();
			included.sort();
			included.notifyAdded(includedItems);
		}
		if (!filteredItems.isEmpty()) {
			filtered.items.addAll(filteredItems);
		}

		return !includedItems.isEmpty();
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

	private final class DefaultIncludedItems implements IncludedItems<R> {

		private final List<R> items = new ArrayList<>();
		private final IncludedPredicate<R> predicate;
		private final Event<List<R>> changed = Event.event();
		private final Event<Collection<R>> added = Event.event();

		private DefaultIncludedItems(IncludedPredicate<R> predicate) {
			this.predicate = predicate;
		}

		@Override
		public IncludedPredicate<R> predicate() {
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
			return changed.observer();
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
					included.notifyChanges();

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
		public int size() {
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
			changed.accept(get());
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
		public int size() {
			synchronized (lock) {
				return items.size();
			}
		}
	}

	private static final class DefaultRefresherStep implements Builder.RefresherStep {

		@Override
		public <T> Builder.SelectionStep<T> refresher(Function<Items<T>, Refresher<T>> refresher) {
			return new DefaultSelectionStep<>(requireNonNull(refresher));
		}
	}

	private static final class DefaultSelectionStep<T> implements Builder.SelectionStep<T> {

		private final Function<Items<T>, Refresher<T>> refresher;

		private DefaultSelectionStep(Function<Items<T>, Refresher<T>> refresher) {
			this.refresher = refresher;
		}

		@Override
		public Builder.SortStep<T> selection(Function<IncludedItems<T>, MultiSelection<T>> selection) {
			return new DefaultSortStep<>(refresher, requireNonNull(selection));
		}
	}

	private static final class DefaultSortStep<T> implements Builder.SortStep<T> {

		private final Function<Items<T>, Refresher<T>> refresher;
		private final Function<IncludedItems<T>, MultiSelection<T>> selectionFunction;

		private DefaultSortStep(Function<Items<T>, Refresher<T>> refresher,
														Function<IncludedItems<T>, MultiSelection<T>> selection) {
			this.refresher = refresher;
			this.selectionFunction = selection;
		}

		@Override
		public Builder<T> sort(Sort<T> sort) {
			return new DefaultBuilder<>(selectionFunction, refresher, requireNonNull(sort));
		}
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		static final Builder.RefresherStep REFRESHER = new DefaultRefresherStep();

		private final Function<IncludedItems<T>, MultiSelection<T>> selection;
		private final Function<Items<T>, Refresher<T>> refresher;
		private final Sort<T> sort;

		private IncludedPredicate<T> included = new DefaultIncludedPredicate<>();
		private Predicate<T> validator = new ValidPredicate<>();
		private ItemsListener itemsListener = new DefaultItemsListener();

		private DefaultBuilder(Function<IncludedItems<T>, MultiSelection<T>> selection,
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
		public Builder<T> included(IncludedPredicate<T> included) {
			this.included = requireNonNull(included);
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

	private static final class DefaultIncludedPredicate<R>
					extends AbstractValue<Predicate<R>> implements IncludedPredicate<R> {

		private @Nullable Predicate<R> predicate;

		private DefaultIncludedPredicate() {
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
