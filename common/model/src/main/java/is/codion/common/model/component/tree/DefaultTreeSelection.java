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

import is.codion.common.model.component.tree.FilterTreeModel.VisibleNodes;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.Value;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
	private final VisibleNodes<T> visible;
	private final Consumer<Collection<NodePath<T>>> expand;
	private final Consumer<NodePath<T>> select;
	private final Function<Set<T>, List<NodePath<T>>> occurrences;
	private final Value<NodePath<T>> path;
	private final Items<NodePath<T>> paths;
	private final Value<T> item;
	private final Items<T> items;

	/**
	 * @param selection the selection of the paths of the visible nodes
	 * @param visible the visible nodes
	 * @param expand expands the ancestors of the nodes identified by the given paths which are hidden
	 * @param select selects the node identified by the given path once it is visible, see {@link #set(NodePath)}
	 * @param occurrences provides the paths of the included nodes holding any of the given items, in the order the nodes
	 * are shown once their ancestors are expanded
	 */
	DefaultTreeSelection(MultiSelection<NodePath<T>> selection, VisibleNodes<T> visible,
											 Consumer<Collection<NodePath<T>>> expand, Consumer<NodePath<T>> select,
											 Function<Set<T>, List<NodePath<T>>> occurrences) {
		this.selection = requireNonNull(selection);
		this.visible = requireNonNull(visible);
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
	 * Returns a distinct path for each of the given items, in order of preference: a visible node holding the same
	 * instance, otherwise one holding an equal item, a selected one preferred in either case in case of
	 * {@code preferSelected}, the first one otherwise, and in case of {@code hidden}, a node in the model below a
	 * collapsed ancestor, holding the same instance, otherwise the first one holding an equal item, in display order,
	 * the items not found left out.
	 * @param items the items to find
	 * @param preferSelected true if selected nodes should be preferred
	 * @param hidden true if nodes below collapsed ancestors should be included
	 * @return the paths of nodes holding the given items
	 */
	private List<NodePath<T>> paths(Collection<T> items, boolean preferSelected, boolean hidden) {
		if (items.isEmpty()) {
			return emptyList();
		}
		List<T> toFind = new ArrayList<>(items);
		Set<T> wanted = new HashSet<>(toFind);
		boolean[] found = new boolean[toFind.size()];
		Set<NodePath<T>> paths = new LinkedHashSet<>();
		Set<NodePath<T>> selected = preferSelected ? new HashSet<>(selection.items().get()) : emptySet();
		find(toFind, candidates(visible.get(), wanted), selected, found, paths);
		if (hidden && paths.size() < toFind.size()) {
			Set<T> remaining = new HashSet<>();
			for (int i = 0; i < toFind.size(); i++) {
				if (!found[i]) {
					remaining.add(toFind.get(i));
				}
			}
			//the visible nodes holding the remaining items have all been taken
			find(toFind, candidates(occurrences.apply(remaining), remaining), emptySet(), found, paths);
		}

		return new ArrayList<>(paths);
	}

	/**
	 * @return the given paths holding one of the given items, by item, in order
	 */
	private static <T> Map<T, List<NodePath<T>>> candidates(List<NodePath<T>> paths, Set<T> items) {
		Map<T, List<NodePath<T>>> candidates = new HashMap<>();
		for (NodePath<T> path : paths) {
			if (items.contains(path.item())) {
				candidates.computeIfAbsent(path.item(), k -> new ArrayList<>()).add(path);
			}
		}

		return candidates;
	}

	/**
	 * Finds a path not already found for each item not already found, each preference in turn for all the items before
	 * the next one: the same instance and selected, the same instance, an equal item and selected, then any.
	 */
	private static <T> void find(List<T> items, Map<T, List<NodePath<T>>> candidates, Set<NodePath<T>> selected,
															 boolean[] found, Set<NodePath<T>> paths) {
		for (int preference = 0; preference < 4; preference++) {
			boolean sameInstance = preference < 2;
			boolean selectedOnly = preference % 2 == 0;
			for (int i = 0; i < items.size(); i++) {
				if (!found[i]) {
					T item = items.get(i);
					for (NodePath<T> path : candidates.getOrDefault(item, emptyList())) {
						if (!paths.contains(path)
										&& (!sameInstance || path.item() == item)
										&& (!selectedOnly || selected.contains(path))) {
							paths.add(path);
							found[i] = true;
							break;
						}
					}
				}
			}
		}
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
				paths.set(paths(singletonList(item), true, true));
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
			paths.set(paths(rejectNulls(items), true, true));
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
			//not preferring the selected nodes, nor expanding, as a selection restores
			selection.items().restore(paths(rejectNulls(items), false, false));
		}

		@Override
		public void add(Predicate<T> predicate) {
			requireNonNull(predicate);
			selection.items().add(path -> predicate.test(path.item()));
		}

		@Override
		public void add(T item) {
			paths.add(paths(singletonList(requireNonNull(item)), true, true));
		}

		@Override
		public void add(Collection<T> items) {
			paths.add(paths(rejectNulls(items), true, true));
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
