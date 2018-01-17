/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.testing;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.demos.petstore.client.ui.PetstoreAppPanel;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.swing.common.tools.ui.LoadTestPanel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.tools.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Collections;
import java.util.UUID;

public final class PetstoreLoadTest extends EntityLoadTestModel<PetstoreAppPanel.PetstoreApplicationModel> {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  public PetstoreLoadTest() {
    super(UNIT_TEST_USER, Collections.singletonList(new AbstractUsageScenario<PetstoreAppPanel.PetstoreApplicationModel>("selectRecords") {
      @Override
      protected void performScenario(final PetstoreAppPanel.PetstoreApplicationModel application) {
        final SwingEntityModel categoryModel = application.getEntityModels().iterator().next();
        categoryModel.getTableModel().getSelectionModel().clearSelection();
        categoryModel.refresh();
        selectRandomRow(categoryModel.getTableModel());
        selectRandomRow(categoryModel.getDetailModels().iterator().next().getTableModel());
        selectRandomRow(categoryModel.getDetailModels().iterator().next().getDetailModels().iterator().next().getTableModel());
      }
    }));
  }

  @Override
  protected PetstoreAppPanel.PetstoreApplicationModel initializeApplication() throws CancelException {
    final PetstoreAppPanel.PetstoreApplicationModel applicationModel = new PetstoreAppPanel.PetstoreApplicationModel(
            new RemoteEntityConnectionProvider(Petstore.class.getName(), getUser(), UUID.randomUUID(), getClass().getSimpleName()));
    final SwingEntityModel categoryModel = applicationModel.getEntityModels().iterator().next();
    categoryModel.addLinkedDetailModel(categoryModel.getDetailModels().iterator().next());
    final SwingEntityModel productModel = categoryModel.getDetailModels().iterator().next();
    productModel.addLinkedDetailModel(productModel.getDetailModels().iterator().next());
    final SwingEntityModel itemModel = productModel.getDetailModels().iterator().next();
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