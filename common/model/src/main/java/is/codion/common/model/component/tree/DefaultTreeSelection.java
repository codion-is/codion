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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.component.tree;

import is.codion.common.model.selection.MultiSelection;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.Value;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static is.codion.common.utilities.Nulls.rejectNulls;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A {@link TreeSelection} based on a {@link MultiSelection} of the paths of the visible nodes, its path facades
 * expanding the ancestors of hidden nodes before selecting them, its item facades mapping the items to the paths of
 * the nodes holding them, forwarding the notifications of the selection.
 */
final class DefaultTreeSelection<T> implements TreeSelection<T> {

	private final MultiSelection<NodePath<T>> selection;
	private final Consumer<Collection<NodePath<T>>> expand;
	private final Consumer<NodePath<T>> select;
	private final Function<Set<T>, Collection<NodePath<T>>> occurrences;
	private final Value<NodePath<T>> path;
	private final Items<NodePath<T>> paths;
	private final Value<T> item;
	private final Items<T> items;

	/**
	 * @param selection the selection of the paths of the visible nodes
	 * @param expand expands the ancestors of the nodes identified by the given paths which are hidden
	 * @param select selects the node identified by the given path once it is visible, see {@link #set(NodePath)}
	 * @param occurrences provides the path of one node holding each of the given items, the first visible one, or the
	 * first one in the model in case none is visible, the items not in the model, or filtered, left out
	 */
	DefaultTreeSelection(MultiSelection<NodePath<T>> selection, Consumer<Collection<NodePath<T>>> expand,
											 Consumer<NodePath<T>> select, Function<Set<T>, Collection<NodePath<T>>> occurrences) {
		this.selection = requireNonNull(selection);
		this.expand = requireNonNull(expand);
		this.select = requireNonNull(select);
		this.occurrences = requireNonNull(occurrences);
		this.path = new SelectedPath();
		this.paths = new SelectedPaths();
		this.item = new SelectedItem();
		this.items = new SelectedItems();
	}

	@Override
	public ObservableState present() {
		return selection.present();
	}

	@Override
	public Observer<?> changing() {
		return selection.changing();
	}

	@Override
	public Value<T> item() {
		return item;
	}

	@Override
	public void clear() {
		selection.clear();
	}

	@Override
	public ObservableState multiple() {
		return selection.multiple();
	}

	@Override
	public ObservableState single() {
		return selection.single();
	}

	@Override
	public State singleSelection() {
		return selection.singleSelection();
	}

	@Override
	public Value<Integer> index() {
		return selection.index();
	}

	@Override
	public Indexes indexes() {
		return selection.indexes();
	}

	@Override
	public Items<T> items() {
		return items;
	}

	@Override
	public Value<NodePath<T>> path() {
		return path;
	}

	@Override
	public Items<NodePath<T>> paths() {
		return paths;
	}

	@Override
	public void selectAll() {
		selection.selectAll();
	}

	@Override
	public int count() {
		return selection.count();
	}

	@Override
	public Grouping grouping() {
		return selection.grouping();
	}

	@Override
	public Observer<?> adjusting() {
		return selection.adjusting();
	}

	@Override
	public void set(NodePath<T> path) {
		select.accept(requireNonNull(path));
	}

	/**
	 * @return the path of one node holding each of the given items, a selected one, keeping the selection in place,
	 * otherwise the first visible one, otherwise the first one in the model, the items not found left out
	 */
	private List<NodePath<T>> occurrences(Collection<T> items) {
		Set<T> remaining = new LinkedHashSet<>(rejectNulls(items));
		List<NodePath<T>> occurrences = new ArrayList<>(remaining.size());
		for (NodePath<T> selected : selection.items().get()) {
			if (remaining.remove(selected.item())) {
				occurrences.add(selected);
			}
		}
		if (!remaining.isEmpty()) {
			occurrences.addAll(this.occurrences.apply(remaining));
		}

		return occurrences;
	}

	private static <T> boolean sameInstances(List<T> first, List<T> second) {
		if (first.size() != second.size()) {
			return false;
		}
		for (int i = 0; i < first.size(); i++) {
			if (first.get(i) != second.get(i)) {
				return false;
			}
		}

		return true;
	}

	private final class SelectedPath extends AbstractValue<NodePath<T>> {

		private SelectedPath() {
			selection.item().addListener(this::notifyObserver);
		}

		@Override
		protected @Nullable NodePath<T> getValue() {
			return selection.item().get();
		}

		@Override
		protected void setValue(@Nullable NodePath<T> path) {
			if (path != null) {
				expand.accept(singletonList(path));
			}
			selection.item().set(path);
		}
	}

	private final class SelectedPaths extends AbstractValue<List<NodePath<T>>> implements Items<NodePath<T>> {

		private SelectedPaths() {
			super(emptyList());
			selection.items().addListener(this::notifyObserver);
		}

		@Override
		protected List<NodePath<T>> getValue() {
			return selection.items().get();
		}

		@Override
		protected void setValue(List<NodePath<T>> paths) {
			expand.accept(paths);
			selection.items().set(paths);
		}

		@Override
		public void set(Collection<NodePath<T>> paths) {
			setValue(new ArrayList<>(requireNonNull(paths)));
		}

		@Override
		public void set(Predicate<NodePath<T>> predicate) {
			selection.items().set(predicate);
		}

		@Override
		public void restore(Collection<NodePath<T>> paths) {
			selection.items().restore(paths);
		}

		@Override
		public void add(Predicate<NodePath<T>> predicate) {
			selection.items().add(predicate);
		}

		@Override
		public void add(NodePath<T> path) {
			expand.accept(singletonList(requireNonNull(path)));
			selection.items().add(path);
		}

		@Override
		public void add(Collection<NodePath<T>> paths) {
			expand.accept(requireNonNull(paths));
			selection.items().add(paths);
		}

		@Override
		public void remove(NodePath<T> path) {
			selection.items().remove(path);
		}

		@Override
		public void remove(Collection<NodePath<T>> paths) {
			selection.items().remove(paths);
		}

		@Override
		public boolean contains(NodePath<T> path) {
			return selection.items().contains(path);
		}

		@Override
		public Optional<List<NodePath<T>>> optional() {
			return selection.items().optional();
		}
	}

	private final class SelectedItem extends AbstractValue<T> {

		private @Nullable T lastNotified;

		private SelectedItem() {
			selection.item().addListener(this::onChanged);
		}

		@Override
		protected @Nullable T getValue() {
			NodePath<T> selected = selection.item().get();

			return selected == null ? null : selected.item();
		}

		@Override
		protected void setValue(@Nullable T item) {
			if (item == null) {
				selection.clear();
			}
			else {
				paths.set(occurrences(singletonList(item)));
			}
		}

		private void onChanged() {
			//by identity, as the item facade of a selection does, the selection moving between nodes
			//holding the same instance changing the path only
			T current = getValue();
			if (lastNotified != current) {
				lastNotified = current;
				notifyObserver();
			}
		}
	}

	private final class SelectedItems extends AbstractValue<List<T>> implements Items<T> {

		private List<T> lastNotified = emptyList();

		private SelectedItems() {
			super(emptyList());
			selection.items().addListener(this::onChanged);
		}

		@Override
		protected List<T> getValue() {
			return unmodifiableList(selection.items().get().stream()
							.map(NodePath::item)
							.collect(toList()));
		}

		@Override
		protected void setValue(List<T> items) {
			paths.set(occurrences(items));
		}

		@Override
		public void set(Collection<T> items) {
			setValue(new ArrayList<>(requireNonNull(items)));
		}

		@Override
		public void set(Predicate<T> predicate) {
			requireNonNull(predicate);
			selection.items().set(path -> predicate.test(path.item()));
		}

		@Override
		public void restore(Collection<T> items) {
			selection.items().restore(occurrences(items));
		}

		@Override
		public void add(Predicate<T> predicate) {
			requireNonNull(predicate);
			selection.items().add(path -> predicate.test(path.item()));
		}

		@Override
		public void add(T item) {
			paths.add(occurrences(singletonList(requireNonNull(item))));
		}

		@Override
		public void add(Collection<T> items) {
			paths.add(occurrences(items));
		}

		@Override
		public void remove(T item) {
			remove(singletonList(requireNonNull(item)));
		}

		@Override
		public void remove(Collection<T> items) {
			//every selected node holding an equal item
			Set<T> toRemove = new HashSet<>(rejectNulls(items));
			selection.items().remove(selection.items().get().stream()
							.filter(path -> toRemove.contains(path.item()))
							.collect(toList()));
		}

		@Override
		public boolean contains(T item) {
			requireNonNull(item);

			return selection.items().get().stream()
							.anyMatch(path -> item.equals(path.item()));
		}

		@Override
		public Optional<List<T>> optional() {
			List<T> selectedItems = getOrThrow();

			return selectedItems.isEmpty() ? Optional.empty() : Optional.of(selectedItems);
		}

		private void onChanged() {
			//by identity, see SelectedItem
			List<T> current = getValue();
			if (!sameInstances(lastNotified, current)) {
				lastNotified = current;
				notifyObserver();
			}
		}
	}
}
