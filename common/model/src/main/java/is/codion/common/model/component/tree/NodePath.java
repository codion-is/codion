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

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * <p>Identifies a node in a {@link FilterTreeModel} by the items on the path from the top level down to it.
 * <p>An immutable value, equal to another path holding equal items in the same order, so the same item may appear in
 * several places in a tree, each its own node. The root is the empty path, the invisible node above the top level.
 * <p>A {@link FilterTreeModel} hands out the current path of each node, holding the item instances the model holds,
 * which a refresh replaces with fresh ones, equal by {@code equals()}. A path held on to across a refresh still
 * identifies the same node but holds the items it was created with.
 * @param <T> the item type
 * @see #nodePath()
 * @see #nodePath(List)
 */
public interface NodePath<T> {

	/**
	 * @return the last item on this path, the item of the node it identifies
	 * @throws IllegalStateException in case this is the root path
	 */
	T item();

	/**
	 * @return the items on this path, the top level item first, an empty list for the root path
	 */
	List<T> items();

	/**
	 * @return the path of the parent node
	 * @throws IllegalStateException in case this is the root path
	 */
	NodePath<T> parent();

	/**
	 * @param item the child item
	 * @return the path of the child node holding the given item
	 */
	NodePath<T> child(T item);

	/**
	 * @return the number of items on this path, 0 for the root path, 1 for the top level
	 */
	int depth();

	/**
	 * @return true if this is the root path
	 */
	boolean root();

	/**
	 * @param path the path
	 * @return true if the given path is this path or the path of a descendant
	 */
	boolean contains(NodePath<T> path);

	/**
	 * @param <T> the item type
	 * @return the root path
	 */
	static <T> NodePath<T> nodePath() {
		return DefaultNodePath.rootPath();
	}

	/**
	 * @param items the items, the top level item first
	 * @param <T> the item type
	 * @return a path holding the given items, the root path in case of no items
	 * @throws NullPointerException in case {@code items} is null or contains a null item
	 */
	static <T> NodePath<T> nodePath(List<T> items) {
		return DefaultNodePath.nodePath(requireNonNull(items));
	}
}
