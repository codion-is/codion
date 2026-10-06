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
import static java.util.stream.Collectors.toList;

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
	final List<Consumer<T>> itemSelectedListeners = new ArrayList<>();
	final List<Consumer<List<T>>> itemsSelectedListeners = new ArrayList<>();
	final List<Consumer<NodePath<T>>> pathSelectedListeners = new ArrayList<>();
	final List<Consumer<List<NodePath<T>>>> pathsSelectedListeners = new ArrayList<>();
	final List<Consumer<Integer>> indexSelectedListeners = new ArrayList<>();
	final List<Consumer<List<Integer>>> indexesSelectedListeners = new ArrayList<>();

	@Nullable Function<List<NodePath<T>>, Collection<NodePath<T>>> leaves;
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
	public final B leaves(Function<List<NodePath<T>>, Collection<NodePath<T>>> leaves) {
		this.leaves = requireNonNull(leaves);
		return self();
	}

	@Override
	public final B leaf(Predicate<NodePath<T>> leaf) {
		requireNonNull(leaf);
		this.leaves = paths -> paths.stream()
						.filter(leaf)
						.collect(toList());
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
	public final B onSelectedItem(Consumer<T> item) {
		itemSelectedListeners.add(requireNonNull(item));
		return self();
	}

	@Override
	public final B onSelectedItems(Consumer<List<T>> items) {
		itemsSelectedListeners.add(requireNonNull(items));
		return self();
	}

	@Override
	public final B onSelectedPath(Consumer<NodePath<T>> path) {
		pathSelectedListeners.add(requireNonNull(path));
		return self();
	}

	@Override
	public final B onSelectedPaths(Consumer<List<NodePath<T>>> paths) {
		pathsSelectedListeners.add(requireNonNull(paths));
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
	 * Builds a {@link FilterTreeModel} with the default {@link TreeSelection}, refreshed if {@link #refresh()}.
	 * @return a new {@link FilterTreeModel} instance
	 */
	@Override
	public FilterTreeModel<T> build() {
		FilterTreeModel<T> model = model(context -> context.treeSelection(MultiSelection.multiSelection(context.visible())), null);
		if (refresh) {
			model.nodes().refresh();
		}

		return model;
	}

	/**
	 * Builds a {@link FilterTreeModel} with the given {@link TreeSelection} and {@link NodesListener}, for a
	 * toolkit builder to wrap in its own model. The model is not refreshed, the toolkit model being the one to
	 * refresh once it is in place, see {@link #refresh()}.
	 * @param selection provides the {@link TreeSelection}, given the {@link SelectionContext}
	 * @param listener notified of changes to the included children of nodes, for bridging to the toolkit's
	 * own change notifications, such as {@code javax.swing.event.TreeModelEvent}s
	 * @return a new {@link FilterTreeModel} instance, not refreshed
	 */
	protected final FilterTreeModel<T> build(Function<SelectionContext<T>, TreeSelection<T>> selection,
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

	private FilterTreeModel<T> model(Function<SelectionContext<T>, TreeSelection<T>> selection,
																	 @Nullable NodesListener<T> listener) {
		return new DefaultFilterTreeModel<>(this, selection, listener);
	}

	/**
	 * Provides what a toolkit's {@link TreeSelection} needs from the model, see {@link #build(Function, NodesListener)}.
	 * @param <T> the item type
	 */
	public interface SelectionContext<T> {

		/**
		 * @return the visible nodes, the items the selection indexes
		 */
		VisibleNodes<T> visible();

		/**
		 * Returns a {@link TreeSelection} based on the given selection of paths, adding the tree behaviour: the items of
		 * the selected nodes, expanding the ancestors of the hidden nodes selected, and {@link TreeSelection#set(NodePath)}.
		 * A toolkit selection which is a {@link MultiSelection} of its own, a {@code javax.swing.tree.TreeSelectionModel}
		 * for example, forwards to the one returned.
		 * @param selection a selection of the paths of the {@link #visible()} nodes
		 * @return a {@link TreeSelection} based on the given selection
		 */
		TreeSelection<T> treeSelection(MultiSelection<NodePath<T>> selection);
	}
}
