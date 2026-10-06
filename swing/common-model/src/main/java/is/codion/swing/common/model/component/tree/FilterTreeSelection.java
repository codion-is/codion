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

import is.codion.common.model.component.tree.NodePath;
import is.codion.common.model.component.tree.TreeSelection;

import javax.swing.tree.TreeSelectionModel;

/**
 * <p>A {@link TreeSelectionModel} serving as the {@link TreeSelection} of a {@link SwingFilterTreeModel}, its indexes
 * the rows of the visible nodes of the model.
 * <p>Single selection mode is {@link TreeSelectionModel#SINGLE_TREE_SELECTION}, multiple selection
 * {@link TreeSelectionModel#DISCONTIGUOUS_TREE_SELECTION}.
 * <p>The {@link javax.swing.tree.TreePath}s of the {@link TreeSelectionModel} identify the selected nodes, while
 * holding the items the nodes had when selected. The paths provided by {@link #item()} and {@link #items()} are
 * the current ones, holding the items a refresh has replaced them with, see {@link NodePath}.
 * @param <T> the item type
 * @see SwingFilterTreeModel#visible()
 */
public interface FilterTreeSelection<T> extends TreeSelectionModel, TreeSelection<T> {}
