/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
