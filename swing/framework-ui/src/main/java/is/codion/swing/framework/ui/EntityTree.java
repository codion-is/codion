/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.swing.framework.model.SwingEntityTreeModel;

import javax.swing.JTree;

import static java.util.Objects.requireNonNull;

/**
 * A {@link JTree} extension based on {@link SwingEntityTreeModel}.
 */
public final class EntityTree extends JTree {

  /**
   * Instantiates a new {@link EntityTree};
   * @param treeModel the tree model
   */
  public EntityTree(final SwingEntityTreeModel treeModel) {
    super(requireNonNull(treeModel, "treeModel"));
    setSelectionModel(treeModel.getTreeSelectionModel());
    setRootVisible(false);
    setShowsRootHandles(true);
    bindEvents();
  }

  private void bindEvents() {
    getSelectionModel().addTreeSelectionListener(selectionEvent -> scrollPathToVisible(selectionEvent.getPath()));
  }
}
