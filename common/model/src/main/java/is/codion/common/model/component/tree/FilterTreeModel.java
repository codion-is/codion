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

import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.filter.FilterModel.IncludePredicate;
import is.codion.common.model.selection.MultiSelection.IndexedItems;
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>A UI-agnostic tree model, supporting lazy loading, expansion, selection, filtering and sorting.
 * <p>A node is identified by its {@link NodePath}, the items from the top level down to it, so the same item may
 * appear in several places in a tree. Siblings must be distinct, by {@code equals()}.
 * <p>The structure is loaded lazily: the roots supplier provides the top level, and the children function the
 * children of a node, called when the node is expanded or refreshed. Loading is asynchronous when
 * {@link Loader#async()} is enabled and a dispatch context is bound, the UI thread on UI platforms.
 * <p>Expansion is model state, see {@link #expansion()}, so it survives refreshes, and the selection is kept by path,
 * see {@link #selection()}.
 * <p>The model is confined to the UI thread, the roots supplier, the children function and the leaves function alone
 * being called off it.
 * <p>The Swing-specific {@code is.codion.swing.common.model.component.tree.SwingFilterTreeModel} extends this with
 * {@code javax.swing.tree.TreeModel}.
 * @param <T> the item type
 * @see #builder()
 */
public interface FilterTreeModel<T> {

	/**
	 * @return the {@link Nodes} providing the structure, loading, mutation and filtering
	 */
	Nodes<T> nodes();

	/**
	 * @return the {@link Expansion} controlling the expanded nodes
	 */
	Expansion<T> expansion();

	/**
	 * @return the {@link VisibleNodes}, the rows a view displays
	 */
	VisibleNodes<T> visible();

	/**
	 * <p>The selection, over the {@link #visible()} nodes, by item, with {@link TreeSelection#paths()} addressing
	 * particular nodes, the same item appearing in several places. Selecting a node hidden below a collapsed ancestor
	 * expands its ancestors, and a node not yet loaded is selected via {@link TreeSelection#set(NodePath)}.
	 * <p>The selection is kept by path, through refreshes, filtering, sorting and expansion. A selected node hidden by
	 * collapsing one of its ancestors is replaced by its nearest visible ancestor, and one removed or filtered is
	 * dropped from the selection.
	 * @return the selection
	 */
	TreeSelection<T> selection();

	/**
	 * @return the {@link FilterTreeSort} instance used by this model
	 */
	FilterTreeSort<T> sort();

	/**
	 * @return a {@link Builder.RootsStep} instance
	 */
	static Builder.RootsStep builder() {
		return DefaultFilterTreeModel.DefaultBuilder.ROOTS;
	}

	/**
	 * <p>The loaded nodes of a {@link FilterTreeModel}.
	 * <p>Mutations preserve the selection by path and notify the {@link VisibleNodes} once, when done.
	 * @param <T> the item type
	 */
	interface Nodes<T> {

		/**
		 * @param parent the parent path
		 * @return the paths of the included children of the given parent, in order, an empty list in case the parent is
		 * not loaded or not in the model. An unmodifiable view, not a copy, valid until the next mutation.
		 */
		List<NodePath<T>> children(NodePath<T> parent);

		/**
		 * @param path the path
		 * @return true if the node identified by the given path is in the model, included or filtered
		 */
		boolean contains(NodePath<T> path);

		/**
		 * Returns the paths of the nodes holding the given item, included or filtered, depth first, since the same item
		 * may appear in several places. A node is in the model once its parent has been loaded.
		 * @param item the item
		 * @return the current paths of the nodes holding the given item, an empty list in case of none
		 * @see NodePath
		 */
		List<NodePath<T>> paths(T item);

		/**
		 * @param path the path
		 * @return true if the node identified by the given path is in the model and included, passing the predicate
		 * or having a loaded descendant that does, the root always included
		 * @see #predicate()
		 */
		boolean included(NodePath<T> path);

		/**
		 * @param path the path
		 * @return true if the children of the node identified by the given path have been loaded
		 */
		boolean loaded(NodePath<T> path);

		/**
		 * Returns true if the given node is a leaf: a node not yet loaded which the leaves function reported as a leaf
		 * when last called for it, or a loaded node without any included children. Does not call the leaves function.
		 * @param path the path
		 * @return true if the node identified by the given path is a leaf, or not in the model
		 * @see Builder#leaves(Function)
		 */
		boolean leaf(NodePath<T> path);

		/**
		 * @return the {@link Loader} instance
		 */
		Loader<T> loader();

		/**
		 * Refreshes the whole tree, see {@link #refresh(NodePath)}.
		 */
		void refresh();

		/**
		 * <p>Reloads the children of the node identified by the given path, and below them every loaded node still
		 * present, so what was loaded stays loaded, calling the leaves function for the children not yet loaded, see
		 * {@link Builder#leaves(Function)}. A node not yet loaded is loaded, unless the leaves function, called for it
		 * again, reports it as a leaf.
		 * <p>Nodes are kept by item, a node whose item is still present keeping its expansion, selection and loaded
		 * subtree, its item replaced with the fresh instance. Nodes no longer present are removed, along with their
		 * subtrees and expansion.
		 * <p>Asynchronous when {@link Loader#async()} is enabled and this method is called where a dispatch context is
		 * bound, otherwise performed on the calling thread. A refresh cancels loads in progress at or below the given
		 * path. A path not in the model is ignored.
		 * @param path the path of the node to refresh
		 */
		void refresh(NodePath<T> path);

		/**
		 * <p>Adds the given items as children of the given loaded parent, appended or at their sorted position when
		 * sorting. Items are added unloaded, the leaves function called for them off the UI thread when loading
		 * asynchronously, the items being leaves until it reports, otherwise on the calling thread.
		 * <p>Has no effect in case the parent is not loaded, the items arriving when it loads.
		 * @param parent the parent path
		 * @param items the items to add
		 * @throws IllegalArgumentException in case an item is already a child of the parent or repeated
		 */
		void add(NodePath<T> parent, Collection<T> items);

		/**
		 * Removes the nodes identified by the given paths, along with their subtrees and expansion.
		 * Paths not in the model are ignored.
		 * @param paths the paths of the nodes to remove
		 * @throws IllegalArgumentException in case of the root path
		 */
		void remove(Collection<NodePath<T>> paths);

		/**
		 * <p>Replaces the item of the node identified by the given path, the leaves function called for the replacement
		 * off the UI thread when loading asynchronously, the node keeping its leaf status until it reports, otherwise on
		 * the calling thread.
		 * <p>An item equal to the current one replaces the instance, the node keeping its expansion, selection and
		 * subtree. A different item changes the identity of the node, along with the paths below it, the expansion and
		 * selection carried over to the new paths.
		 * <p>A path not in the model is ignored.
		 * @param path the path of the node
		 * @param item the replacement item
		 * @throws IllegalArgumentException in case of the root path, or in case a sibling already holds the item
		 */
		void replace(NodePath<T> path, T item);

		/**
		 * <p>Controls which nodes are included. A node is included when it passes the predicate, or when one of its
		 * loaded descendants does, so a match keeps its ancestors. Setting the predicate filters the nodes.
		 * <p>Filtering applies to the loaded nodes only, a node not yet loaded can not be included on account of its
		 * descendants. Filtered nodes stay in the model, so clearing the predicate includes them again without a reload.
		 * <p>To hide whole subtrees, have the children function return fewer children and refresh, which keeps the
		 * expansion and selection of the remaining nodes.
		 * @return the {@link IncludePredicate} controlling which nodes are included
		 */
		IncludePredicate<NodePath<T>> predicate();

		/**
		 * Filters the nodes according to the {@link #predicate()}, testing them all, for when the predicate depends on
		 * external state. Otherwise a node is tested when it is loaded or added, and when its item is replaced.
		 */
		void filter();
	}

	/**
	 * Loads the children of nodes, see {@link Nodes#refresh(NodePath)}.
	 * @param <T> the item type
	 */
	interface Loader<T> {

		/**
		 * @return the {@link State} controlling whether loading is asynchronous, initialized with {@link FilterModel#ASYNC}
		 */
		State async();

		/**
		 * <p>Changes to this state are triggered on the UI thread when loading asynchronously,
		 * otherwise on the calling thread.
		 * @return an observable indicating that a load is in progress, or a call to the leaves function for nodes added or
		 * replaced
		 */
		ObservableState active();

		/**
		 * @param path the path
		 * @return true if the children of the node identified by the given path are being loaded
		 */
		boolean active(NodePath<T> path);

		/**
		 * @return an observer notified on an exception during loading
		 */
		Observer<Exception> exception();
	}

	/**
	 * <p>The expanded nodes of a {@link FilterTreeModel}.
	 * <p>The expanded paths express intent, the loading follows: expanding a node not yet loaded loads it, and loaded
	 * children which are expanded load in turn, so expanding a path not yet loaded loads it level by level. The
	 * expansion of nodes below a collapsed node is remembered, their loading waiting until it is expanded again.
	 * <p>A node which fails to load is collapsed, expanding it again loads it again.
	 * <p>A path naming a node which does not exist is dropped, when the node above it is loaded, or right away in case
	 * it is loaded already, or in case it is not yet loaded and reported as a leaf by the leaves function, so never loaded.
	 * <p>The root is always expanded.
	 * @param <T> the item type
	 */
	interface Expansion<T> {

		/**
		 * Expands the given path, along with its ancestors, loading as needed.
		 * @param path the path to expand
		 */
		void expand(NodePath<T> path);

		/**
		 * Collapses the given path, the expansion of its descendants remembered. A selected descendant is replaced by
		 * the collapsed node in the selection.
		 * @param path the path to collapse
		 */
		void collapse(NodePath<T> path);

		/**
		 * @param path the path
		 * @return true if the given path and all its ancestors are expanded
		 */
		boolean expanded(NodePath<T> path);

		/**
		 * @return the expanded paths, including the ones remembered below collapsed nodes, the current paths
		 * of the nodes in the model
		 */
		Collection<NodePath<T>> get();

		/**
		 * Replaces the expanded paths, loading as needed, for restoring the expansion.
		 * @param paths the paths to expand
		 */
		void set(Collection<NodePath<T>> paths);

		/**
		 * @return an observer notified with each path expanded, ancestors before descendants
		 */
		Observer<NodePath<T>> expanded();

		/**
		 * @return an observer notified with each path collapsed
		 */
		Observer<NodePath<T>> collapsed();
	}

	/**
	 * <p>The visible nodes, the included nodes whose ancestors are all expanded, in depth first order, the root excluded.
	 * <p>These are the rows a view displays, and the {@link FilterTreeModel#selection()} indexes.
	 * <p>Notified once per mutation, when the visible nodes changed.
	 * @param <T> the item type
	 */
	interface VisibleNodes<T> extends Observable<List<NodePath<T>>>, IndexedItems<NodePath<T>> {

		/**
		 * @return an unmodifiable snapshot of the visible nodes
		 */
		@Override
		@NonNull List<NodePath<T>> get();
	}

	/**
	 * <p>Notified of changes to the included children of nodes, for bridging to a toolkit's own change notifications,
	 * such as {@code javax.swing.event.TreeModelEvent}s.
	 * <p>Called as a mutation happens, the model reflecting the state after the mutation. Indexes refer to the
	 * included children of the parent, ascending, in an unmodifiable list.
	 * @param <T> the item type
	 * @see AbstractFilterTreeModelBuilder#build(Function, NodesListener)
	 */
	interface NodesListener<T> {

		/**
		 * Called when children have been inserted
		 * @param parent the parent path
		 * @param indexes the indexes of the inserted children
		 */
		void inserted(NodePath<T> parent, List<Integer> indexes);

		/**
		 * Called when children have been removed
		 * @param parent the parent path
		 * @param indexes the indexes of the removed children, before removal
		 * @param children the removed children
		 */
		void removed(NodePath<T> parent, List<Integer> indexes, List<NodePath<T>> children);

		/**
		 * Called when children have changed, their item instances replaced, or their leaf status changed
		 * @param parent the parent path
		 * @param indexes the indexes of the changed children
		 */
		void changed(NodePath<T> parent, List<Integer> indexes);

		/**
		 * Called when the structure below the given node may have changed arbitrarily
		 * @param path the path of the node
		 */
		void structureChanged(NodePath<T> path);
	}

	/**
	 * Builds a {@link FilterTreeModel}.
	 * @param <T> the item type
	 * @param <B> the builder type
	 * @see AbstractFilterTreeModelBuilder
	 */
	interface Builder<T, B extends Builder<T, B>> {

		/**
		 * Provides a {@link ChildrenStep}
		 */
		interface RootsStep {

			/**
			 * The roots are called on a background thread when loading asynchronously.
			 * @param roots supplies the top level items
			 * @param <T> the item type
			 * @return a {@link ChildrenStep}
			 */
			<T> ChildrenStep<T> roots(Supplier<Collection<T>> roots);
		}

		/**
		 * Provides a {@link Builder}
		 * @param <T> the item type
		 */
		interface ChildrenStep<T> {

			/**
			 * <p>The children function is never called for the root, and on a background thread when loading asynchronously.
			 * <p>It may not return null items or equal items, siblings must be distinct.
			 * @param children provides the children of a node, given its path
			 * @return a {@link Builder}
			 */
			Builder<T, ?> children(Function<NodePath<T>, Collection<T>> children);
		}

		/**
		 * <p>The leaves function decides which nodes not yet loaded are leaves, a loaded node being a leaf when it has
		 * no included children. Without one, a node not yet loaded is not a leaf, so a view displays an expand handle
		 * until it is loaded.
		 * <p>It is given the nodes not yet loaded among the items the roots supplier or the children function return,
		 * all of a node's children at once, so it can find the leaves among them with a single query, and returns those
		 * which are leaves. It is called along with the children function, so on a background thread when loading
		 * asynchronously, its result kept until a refresh reaches the nodes. A view asking whether a node is a leaf does
		 * not call it, so it may be slow. {@link Nodes#add(NodePath, Collection)} calls it with the nodes added, and
		 * {@link Nodes#replace(NodePath, Object)} with the node replaced, on the calling thread.
		 * <p>A node not yet loaded, which the leaves function reports as a leaf, is not loaded when expanded, so the
		 * children function is not called for it, nor when refreshed, unless the leaves function, called for it again,
		 * no longer reports it as a leaf, see {@link Nodes#refresh(NodePath)}.
		 * <p>Replaces the function set via {@link #leaf(Predicate)}, the same option.
		 * @param leaves given the paths of nodes not yet loaded, returns those which are leaves, paths not given being
		 * ignored
		 * @return this builder instance
		 * @see Nodes#leaf(NodePath)
		 */
		B leaves(Function<List<NodePath<T>>, Collection<NodePath<T>>> leaves);

		/**
		 * <p>The leaves function, node by node, for when a node can tell whether it is a leaf on its own, from its item
		 * for example, see {@link #leaves(Function)}.
		 * <p>Replaces the function set via {@link #leaves(Function)}, the same option.
		 * @param leaf returns true if the node identified by the given path is a leaf
		 * @return this builder instance
		 * @see Nodes#leaf(NodePath)
		 */
		B leaf(Predicate<NodePath<T>> leaf);

		/**
		 * Specifies the comparator to use when sorting siblings, the same for all, see {@link #comparators(Function)}.
		 * @param comparator the comparator to use when sorting siblings, null for none
		 * @return this builder instance
		 */
		B comparator(@Nullable Comparator<T> comparator);

		/**
		 * <p>Specifies the comparator to use when sorting the children of each parent, given the path of the parent,
		 * the root path for the top level, a null comparator leaving the children in the order the children function
		 * returned them in.
		 * <p>Called each time the children of a parent are sorted, when they are loaded, added, replaced, refreshed or
		 * filtered, or when the sort order changes. Sets the same option as {@link #comparator(Comparator)}, so the last
		 * one set wins.
		 * @param comparators provides the comparator for the children of a parent, given its path
		 * @return this builder instance
		 * @see FilterTreeModel#sort()
		 */
		B comparators(Function<NodePath<T>, @Nullable Comparator<T>> comparators);

		/**
		 * @param included the predicate controlling which nodes are included
		 * @return this builder instance
		 * @see Nodes#predicate()
		 */
		B included(Predicate<NodePath<T>> included);

		/**
		 * By default, exceptions during loading are rethrown,
		 * use this method to handle async exceptions differently
		 * @param onLoadException the exception handler to use during loading
		 * @return this builder instance
		 */
		B onLoadException(Consumer<Exception> onLoadException);

		/**
		 * @param refresh true if the model should be refreshed on initialization, false by default
		 * @return this builder instance
		 */
		B refresh(boolean refresh);

		/**
		 * @param listener the selection listener
		 * @return this builder instance
		 */
		B onSelectionChanged(Runnable listener);

		/**
		 * @param item receives the item of the selected node
		 * @return this builder instance
		 * @see TreeSelection#item()
		 */
		B onSelectedItem(Consumer<T> item);

		/**
		 * @param items receives the items of the selected nodes
		 * @return this builder instance
		 * @see TreeSelection#items()
		 */
		B onSelectedItems(Consumer<List<T>> items);

		/**
		 * @param path receives the path of the selected node
		 * @return this builder instance
		 * @see TreeSelection#path()
		 */
		B onSelectedPath(Consumer<NodePath<T>> path);

		/**
		 * @param paths receives the paths of the selected nodes
		 * @return this builder instance
		 * @see TreeSelection#paths()
		 */
		B onSelectedPaths(Consumer<List<NodePath<T>>> paths);

		/**
		 * @param index receives the selected index
		 * @return this builder instance
		 */
		B onSelectedIndex(Consumer<Integer> index);

		/**
		 * @param indexes receives the selected indexes
		 * @return this builder instance
		 */
		B onSelectedIndexes(Consumer<List<Integer>> indexes);

		/**
		 * @return a new {@link FilterTreeModel} instance
		 */
		FilterTreeModel<T> build();
	}
}
