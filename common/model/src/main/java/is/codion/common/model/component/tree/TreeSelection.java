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
import is.codion.common.reactive.value.Value;

import java.util.function.Function;

/**
 * <p>The selection of a {@link FilterTreeModel}, over its {@link FilterTreeModel#visible() visible} nodes, addressed by
 * row via the indexes, by item via the items, and by node via {@link #path()} and {@link #paths()}.
 * <p>The same item may appear in several places in a tree. Selecting by item selects one node holding it, while the
 * paths address particular nodes. A node hidden below a collapsed ancestor, selected by item or by path, has its
 * ancestors expanded, so that it can be selected right away. A node not yet in the model, its parent not loaded, is
 * selected via {@link #set(NodePath)}, which loads as needed and selects it once it is visible.
 * @param <T> the item type
 * @see FilterTreeModel#selection()
 */
public interface TreeSelection<T> extends MultiSelection<T> {

	/**
	 * {@inheritDoc}
	 * <p>Setting an item selects one node holding it: a selected one, keeping the selection in place, otherwise the
	 * first visible one, otherwise the first one in the model, depth first, its ancestors expanded. An item not in the
	 * model, or filtered, clears the selection. Use {@link #path()} for a particular node, and {@link #set(NodePath)}
	 * for a node not yet loaded.
	 * <p>The selection moving between nodes holding the same item instance changes {@link #path()}, not the item.
	 */
	@Override
	Value<T> item();

	/**
	 * <p>The items of the selected nodes, in row order, an item selected in several places appearing once for each.
	 * <p>Setting or adding an item selects one node holding it, as {@link #item()} does, ignoring an item not in the
	 * model, or filtered, while {@link Items#remove(Object)} and {@link Items#contains(Object)} cover every selected
	 * node holding it. Use {@link #paths()} for particular nodes.
	 * @return the selected items, the items of the selected nodes
	 */
	@Override
	Items<T> items();

	/**
	 * <p>The path of the selected node, as {@link #item()} is its item.
	 * <p>Setting the path of a node hidden below a collapsed ancestor expands its ancestors and selects it, while the
	 * path of a node not in the model, or filtered, clears the selection. Use {@link #set(NodePath)} for a node not yet
	 * loaded.
	 * @return the {@link Value} controlling the path of the selected node
	 */
	Value<NodePath<T>> path();

	/**
	 * <p>The paths of the selected nodes, in row order, as {@link #items()} are their items.
	 * <p>Setting or adding the paths of nodes hidden below collapsed ancestors expands their ancestors and selects them,
	 * while the paths of nodes not in the model, or filtered, are ignored. Use {@link #set(NodePath)} for a node not yet
	 * loaded.
	 * @return the {@link Items} controlling the paths of the selected nodes
	 */
	Items<NodePath<T>> paths();

	/**
	 * <p>Selects the node identified by the given path once it is visible, replacing the selection, expanding its
	 * ancestors and loading as needed. The node itself is not expanded. Where {@link #path()} takes effect right away,
	 * on a node in the model, this takes effect once the nodes on the path have been loaded, before this method returns
	 * when loading synchronously.
	 * <p>A node which exists but is not visible, being filtered or below a node collapsed since, is replaced by its
	 * nearest visible ancestor. In case the path turns out not to exist, or can not be loaded further, a load failing
	 * or a node being a leaf according to the leaves function, the deepest node on the path which does exist is selected,
	 * or its nearest visible ancestor. The selection is left as is in case no node on the path is in the model.
	 * <p>A pending selection is cancelled by a selection change from outside, by the application or the user, and
	 * replaced by another one, while loading and refreshing do not cancel it. A selection change vetoed via
	 * {@link #changing()} ends it unselected.
	 * @param path the path of the node to select
	 * @throws IllegalArgumentException in case of the root path
	 * @see FilterTreeModel.Builder#leaves(Function)
	 */
	void set(NodePath<T> path);
}
