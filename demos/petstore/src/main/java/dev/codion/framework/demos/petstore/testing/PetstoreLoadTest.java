/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petstore.testing;

import dev.codion.common.model.CancelException;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProviders;
import dev.codion.framework.demos.petstore.domain.Petstore;
import dev.codion.framework.demos.petstore.model.PetstoreAppModel;
import dev.codion.swing.common.tools.loadtest.AbstractUsageScenario;
import dev.codion.swing.common.tools.ui.loadtest.LoadTestPanel;
import dev.codion.swing.framework.model.SwingEntityModel;
import dev.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import javax.swing.SwingUtilities;

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
        new LoadTestPanel(new PetstoreLoadTest()).showFrame();
      }
      catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }
}