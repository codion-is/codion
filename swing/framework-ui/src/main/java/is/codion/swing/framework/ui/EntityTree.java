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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.swing.framework.model.SwingEntityTreeModel;

import javax.swing.JTree;

import static java.util.Objects.requireNonNull;

/**
 * A {@link JTree} extension based on {@link SwingEntityTreeModel}.
 * For instances use the {@link #entityTree(SwingEntityTreeModel)} factory method.
 * @see #entityTree(SwingEntityTreeModel)
 */
public final class EntityTree extends JTree {

  private EntityTree(SwingEntityTreeModel treeModel) {
    super(requireNonNull(treeModel, "treeModel"));
    setSelectionModel(treeModel.treeSelectionModel());
    setRootVisible(false);
    setShowsRootHandles(true);
    bindEvents();
  }

  /**
   * Instantiates a new {@link EntityTree}
   * @param treeModel the tree model
   * @return a new {@link EntityTree} instance
   */
  public static EntityTree entityTree(SwingEntityTreeModel treeModel) {
    return new EntityTree(treeModel);
  }

  private void bindEvents() {
    getSelectionModel().addTreeSelectionListener(selectionEvent -> scrollPathToVisible(selectionEvent.getPath()));
  }
}
