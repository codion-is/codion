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

import is.codion.common.model.component.tree.FilterTreeModel.NodesListener;
import is.codion.common.model.component.tree.FilterTreeModel.VisibleNodes;
import is.codion.common.model.selection.MultiSelection;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A base class for {@link FilterTreeModel.Builder} implementations. A toolkit builder extends this class, adding
 * its own options and overriding {@link #build()} to wrap the {@link FilterTreeModel} built by
 * {@link #build(Function, NodesListener)} in its own model.
 * @param <T> the item type
 * @param <B> the builder type
 * @see #build(Function, NodesListener)
 * @see #refresh()
 */
public abstract class AbstractFilterTreeModelBuilder<T, B extends FilterTreeModel.Builder<T, B>>
				implements FilterTreeModel.Builder<T, B> {

	final Supplier<Collection<T>> roots;
	final Function<NodePath<T>, Collection<T>> children;
	final List<Runnable> selectionListeners = new ArrayList<>();
	final List<Consumer<NodePath<T>>> itemSelectedListeners = new ArrayList<>();
	final List<Consumer<List<NodePath<T>>>> itemsSelectedListeners = new ArrayList<>();
	final List<Consumer<Integer>> indexSelectedListeners = new ArrayList<>();
	final List<Consumer<List<Integer>>> indexesSelectedListeners = new ArrayList<>();

	@Nullable Predicate<NodePath<T>> leaf;
	@Nullable Comparator<T> comparator;
	@Nullable Predicate<NodePath<T>> included;
	@Nullable Consumer<Exception> onLoadException;

	private boolean refresh = false;

	/**
	 * @param roots supplies the top level items
	 * @param children provides the children of a node
	 */
	protected AbstractFilterTreeModelBuilder(Supplier<Collection<T>> roots, Function<NodePath<T>, Collection<T>> children) {
		this.roots = requireNonNull(roots);
		this.children = requireNonNull(children);
	}

	@Override
	public final B leaf(Predicate<NodePath<T>> leaf) {
		this.leaf = requireNonNull(leaf);
		return self();
	}

	@Override
	public final B comparator(@Nullable Comparator<T> comparator) {
		this.comparator = comparator;
		return self();
	}

	@Override
	public final B included(Predicate<NodePath<T>> included) {
		this.included = requireNonNull(included);
		return self();
	}

	@Override
	public final B onLoadException(Consumer<Exception> onLoadException) {
		this.onLoadException = requireNonNull(onLoadException);
		return self();
	}

	@Override
	public final B refresh(boolean refresh) {
		this.refresh = refresh;
		return self();
	}

	@Override
	public final B onSelectionChanged(Runnable listener) {
		selectionListeners.add(requireNonNull(listener));
		return self();
	}

	@Override
	public final B onSelectedItem(Consumer<NodePath<T>> item) {
		itemSelectedListeners.add(requireNonNull(item));
		return self();
	}

	@Override
	public final B onSelectedItems(Consumer<List<NodePath<T>>> items) {
		itemsSelectedListeners.add(requireNonNull(items));
		return self();
	}

	@Override
	public final B onSelectedIndex(Consumer<Integer> index) {
		indexSelectedListeners.add(requireNonNull(index));
		return self();
	}

	@Override
	public final B onSelectedIndexes(Consumer<List<Integer>> indexes) {
		indexesSelectedListeners.add(requireNonNull(indexes));
		return self();
	}

	/**
	 * Builds a {@link FilterTreeModel} with the default {@link MultiSelection}, refreshed if {@link #refresh()}.
	 * @return a new {@link FilterTreeModel} instance
	 */
	@Override
	public FilterTreeModel<T> build() {
		FilterTreeModel<T> model = model(MultiSelection::multiSelection, null);
		if (refresh) {
			model.nodes().refresh();
		}

		return model;
	}

	/**
	 * Builds a {@link FilterTreeModel} with the given {@link MultiSelection} and {@link NodesListener}, for a
	 * toolkit builder to wrap in its own model. The model is not refreshed, the toolkit model being the one to
	 * refresh once it is in place, see {@link #refresh()}.
	 * @param selection provides the {@link MultiSelection} given the {@link VisibleNodes}
	 * @param listener notified of changes to the included children of nodes, for bridging to the toolkit's
	 * own change notifications, such as {@code javax.swing.event.TreeModelEvent}s
	 * @return a new {@link FilterTreeModel} instance, not refreshed
	 */
	protected final FilterTreeModel<T> build(Function<VisibleNodes<T>, MultiSelection<NodePath<T>>> selection,
																					 NodesListener<T> listener) {
		return model(requireNonNull(selection), requireNonNull(listener));
	}

	/**
	 * @return true if the model should be refreshed once built
	 * @see #refresh(boolean)
	 */
	protected final boolean refresh() {
		return refresh;
	}

	/**
	 * @return this builder instance
	 */
	protected final B self() {
		return (B) this;
	}

	private FilterTreeModel<T> model(Function<VisibleNodes<T>, MultiSelection<NodePath<T>>> selection,
																	 @Nullable NodesListener<T> listener) {
		return new DefaultFilterTreeModel<>(this, selection, listener);
	}
}
