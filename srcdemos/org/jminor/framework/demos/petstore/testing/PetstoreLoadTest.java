/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.swing.ui.tools.LoadTestPanel;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Collections;
import java.util.UUID;

public final class PetstoreLoadTest extends EntityLoadTestModel {

  public PetstoreLoadTest() {
    super(User.UNIT_TEST_USER, Collections.singletonList(new AbstractUsageScenario<EntityApplicationModel>("selectRecords") {
      @Override
      protected void performScenario(final EntityApplicationModel application) {
        final EntityModel categoryModel = application.getEntityModels().iterator().next();
        categoryModel.getTableModel().getSelectionModel().clearSelection();
        categoryModel.refresh();
        selectRandomRow(categoryModel.getTableModel());
        selectRandomRow(categoryModel.getDetailModels().iterator().next().getTableModel());
        selectRandomRow(categoryModel.getDetailModels().iterator().next().getDetailModels().iterator().next().getTableModel());
      }
    }));
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel applicationModel = new DefaultEntityApplicationModel(new RemoteEntityConnectionProvider(getUser(),
            UUID.randomUUID(), getClass().getSimpleName())) {
      @Override
      protected void loadDomainModel() {
        Petstore.init();
      }
    };
    final EntityModel categoryModel = applicationModel.getEntityModels().iterator().next();
    categoryModel.addLinkedDetailModel(categoryModel.getDetailModels().iterator().next());
    final EntityModel productModel = categoryModel.getDetailModels().iterator().next();
    productModel.addLinkedDetailModel(productModel.getDetailModels().iterator().next());
    final EntityModel itemModel = productModel.getDetailModels().iterator().next();
    itemModel.addLinkedDetailModel(itemModel.getDetailModels().iterator().next());

    return applicationModel;
  }

  public static void main(final String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runner());
  }

  private static final class Runner implements Runnable {
    @Override
    public void run() {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new LoadTestPanel(new PetstoreLoadTest()).showFrame();
      }
      catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }
}