/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.tools.LoadTestModel;
import org.jminor.common.ui.tools.LoadTestPanel;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.framework.server.provider.RemoteEntityConnectionProvider;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.UUID;

public final class PetstoreLoadTest extends EntityLoadTestModel {

  public PetstoreLoadTest() {
    super(User.UNIT_TEST_USER, new LoadTestModel.AbstractUsageScenario("selectRecords") {
      @Override
      protected void performScenario(final Object application) throws ScenarioException {
        final EntityModel categoryModel = ((EntityApplicationModel) application).getEntityModels().iterator().next();
        categoryModel.getTableModel().clearSelection();
        categoryModel.refresh();
        selectRandomRow(categoryModel.getTableModel());
        selectRandomRow(categoryModel.getDetailModels().iterator().next().getTableModel());
        selectRandomRow(categoryModel.getDetailModels().iterator().next().getDetailModels().iterator().next().getTableModel());
      }
    });
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
    categoryModel.setLinkedDetailModels(categoryModel.getDetailModels().iterator().next());
    final EntityModel productModel = categoryModel.getDetailModels().iterator().next();
    productModel.setLinkedDetailModels(productModel.getDetailModels().iterator().next());
    final EntityModel itemModel = productModel.getDetailModels().iterator().next();
    itemModel.setLinkedDetailModels(itemModel.getDetailModels().iterator().next());

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
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}