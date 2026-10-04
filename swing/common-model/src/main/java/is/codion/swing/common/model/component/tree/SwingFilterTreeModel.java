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
package is.codion.swing.common.model.component.tree;

import is.codion.common.model.component.tree.FilterTreeModel;
import is.codion.common.model.component.tree.NodePath;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>A Swing {@link TreeModel} based on the UI-agnostic {@link FilterTreeModel}, adding the {@link TreeModel}
 * interface, with {@link #selection()} narrowed to a {@link FilterTreeSelection}, a
 * {@link javax.swing.tree.TreeSelectionModel}.
 * <p>The tree nodes are the current {@link NodePath}s of the model, the root being {@link NodePath#nodePath()}, so the
 * components of a {@link TreePath} are {@link NodePath}s, and {@link TreePath}s are equal when they identify the same
 * node, whichever item instances they hold.
 * <p>{@link javax.swing.event.TreeModelListener}s are notified in the order they were added.
 * <p>Expansion is model state, see {@link #expansion()}, a {@code JTree} over this model must be kept in step with it,
 * as {@code is.codion.swing.common.ui.component.tree.FilterTree} does.
 * @param <T> the item type
 * @see #builder()
 */
public interface SwingFilterTreeModel<T> extends FilterTreeModel<T>, TreeModel {

	/**
	 * @return the {@link FilterTreeSelection} instance used by this tree model
	 */
	@Override
	FilterTreeSelection<T> selection();

	/**
	 * @return the root, {@link NodePath#nodePath()}
	 */
	@Override
	NodePath<T> getRoot();

	/**
	 * Notifies all listeners that the given nodes have changed, for when external state changes how they are rendered.
	 * Paths not included in the model are ignored.
	 * @param paths the paths of the changed nodes
	 */
	void fireNodesChanged(Collection<NodePath<T>> paths);

	/**
	 * @param path the path
	 * @return a {@link TreePath} identifying the node identified by the given path, its components the paths from the
	 * root down to it
	 */
	TreePath treePath(NodePath<T> path);

	/**
	 * @return a {@link Builder.RootsStep} instance
	 */
	static Builder.RootsStep builder() {
		return DefaultSwingFilterTreeModel.DefaultBuilder.ROOTS;
	}

	/**
	 * Builds a {@link SwingFilterTreeModel}, the {@link FilterTreeModel.Builder} options with the selection based
	 * on a {@link javax.swing.tree.TreeSelectionModel}.
	 * @param <T> the item type
	 */
	interface Builder<T> extends FilterTreeModel.Builder<T, Builder<T>> {

		/**
		 * Provides a {@link ChildrenStep}
		 */
		interface RootsStep extends FilterTreeModel.Builder.RootsStep {

			@Override
			<T> ChildrenStep<T> roots(Supplier<Collection<T>> roots);
		}

		/**
		 * Provides a {@link Builder}
		 * @param <T> the item type
		 */
		interface ChildrenStep<T> extends FilterTreeModel.Builder.ChildrenStep<T> {

			@Override
			Builder<T> children(Function<NodePath<T>, Collection<T>> children);
		}

		@Override
		SwingFilterTreeModel<T> build();
	}
}
