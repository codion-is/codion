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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A {@link TreeSelection} based on a {@link MultiSelection} over the visible nodes, its item and items facades
 * expanding the ancestors of hidden nodes before selecting them, forwarding the notifications of the selection.
 */
final class DefaultTreeSelection<T> implements TreeSelection<T> {

	private final MultiSelection<NodePath<T>> selection;
	private final Consumer<Collection<NodePath<T>>> expand;
	private final Consumer<NodePath<T>> select;
	private final Value<NodePath<T>> item;
	private final Items<NodePath<T>> items;

	/**
	 * @param selection the selection over the visible nodes
	 * @param expand expands the ancestors of the nodes identified by the given paths which are hidden
	 * @param select selects the node identified by the given path once it is visible, see {@link #set(NodePath)}
	 */
	DefaultTreeSelection(MultiSelection<NodePath<T>> selection, Consumer<Collection<NodePath<T>>> expand,
											 Consumer<NodePath<T>> select) {
		this.selection = requireNonNull(selection);
		this.expand = requireNonNull(expand);
		this.select = requireNonNull(select);
		this.item = new ExpandingItem();
		this.items = new ExpandingItems();
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
	public Value<NodePath<T>> item() {
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
	public Items<NodePath<T>> items() {
		return items;
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

	private final class ExpandingItem extends AbstractValue<NodePath<T>> {

		private ExpandingItem() {
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

	private final class ExpandingItems extends AbstractValue<List<NodePath<T>>> implements Items<NodePath<T>> {

		private ExpandingItems() {
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
	}
}
