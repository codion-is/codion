/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Collection;

public class FrameworkModelUtil {

  public static DefaultTreeModel createApplicationTree(final Collection<? extends EntityModel> entityModels) {
    final DefaultTreeModel applicationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    addModelsToTree((DefaultMutableTreeNode) applicationTreeModel.getRoot(), entityModels);

    return applicationTreeModel;
  }

  public static void addModelsToTree(final DefaultMutableTreeNode root, final Collection<? extends EntityModel> models) {
    for (final EntityModel model : models) {
      final DefaultMutableTreeNode node = new DefaultMutableTreeNode(model);
      root.add(node);
      if (model.getDetailModels().size() > 0)
        addModelsToTree(node, model.getDetailModels());
    }
  }
}
