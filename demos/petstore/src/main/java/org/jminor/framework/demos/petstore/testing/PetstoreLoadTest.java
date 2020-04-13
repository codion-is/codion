/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.framework.demos.petstore.model.PetstoreAppModel;
import org.jminor.swing.common.tools.loadtest.ui.LoadTestPanel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.tools.loadtest.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import static java.util.Collections.singletonList;

public final class PetstoreLoadTest extends EntityLoadTestModel<PetstoreAppModel> {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  public PetstoreLoadTest() {
    super(UNIT_TEST_USER, singletonList(new AbstractUsageScenario<PetstoreAppModel>("selectRecords") {
      @Override
      protected void performScenario(final PetstoreAppModel application) {
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
  protected PetstoreAppModel initializeApplication() throws CancelException {
    final PetstoreAppModel applicationModel = new PetstoreAppModel(
            EntityConnectionProviders.connectionProvider().setDomainClassName(Petstore.class.getName())
                    .setClientTypeId(getClass().getSimpleName()).setUser(getUser()));
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