/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.profiling;

import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.petstore.client.PetstoreAppModel;
import org.jminor.framework.demos.petstore.model.Petstore;
import org.jminor.framework.profiling.ProfilingModel;
import org.jminor.framework.profiling.ui.ProfilingPanel;
import org.jminor.framework.server.EntityDbRemoteProvider;

import javax.swing.UIManager;

/**
 * User: Björn Darri
 * Date: 30.11.2007
 * Time: 03:33:10
 */
public class PetstoreProfiling extends ProfilingModel {

  /** Constructs a new PetstoreProfiling.*/
  public PetstoreProfiling() {
    super(new User("scott", "tiger"));
  }

  /** {@inheritDoc} */
  protected void loadDomainModel() {
    new Petstore();
  }

  /** {@inheritDoc} */
  protected void performWork(final EntityApplicationModel applicationModel) {
    try {
      final EntityModel categoryModel = applicationModel.getMainApplicationModels().iterator().next();
      categoryModel.getTableModel().clearSelection();
      categoryModel.refresh();
      selectRandomRow(categoryModel);
      selectRandomRow(categoryModel.getDetailModels().get(0));
      selectRandomRow(categoryModel.getDetailModels().get(0).getDetailModels().get(0));
    }
    catch (UserException e) {
      e.printStackTrace();
    }
  }

  /** {@inheritDoc} */
  protected EntityApplicationModel initializeApplicationModel() throws UserException {
    final EntityApplicationModel applicationModel =
            new PetstoreAppModel(new EntityDbRemoteProvider(getUser(), "scott@"+new Object(), getClass().getSimpleName()));
    final EntityModel categoryModel = applicationModel.getMainApplicationModels().iterator().next();
    categoryModel.setLinkedDetailModel(categoryModel.getDetailModels().get(0));
    final EntityModel productModel = categoryModel.getDetailModels().get(0);
    productModel.setLinkedDetailModel(productModel.getDetailModels().get(0));
    final EntityModel itemModel = productModel.getDetailModels().get(0);
    itemModel.setLinkedDetailModel(itemModel.getDetailModels().get(0));

    return applicationModel;
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    new ProfilingPanel(new PetstoreProfiling());
  }
}