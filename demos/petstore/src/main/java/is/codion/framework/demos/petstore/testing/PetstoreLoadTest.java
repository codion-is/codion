/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petstore.domain.Petstore;
import is.codion.framework.demos.petstore.model.PetstoreAppModel;
import is.codion.swing.common.tools.loadtest.AbstractUsageScenario;
import is.codion.swing.common.tools.ui.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import javax.swing.SwingUtilities;

import static java.util.Collections.singletonList;

public final class PetstoreLoadTest extends EntityLoadTestModel<PetstoreAppModel> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public PetstoreLoadTest() {
    super(UNIT_TEST_USER, singletonList(new AbstractUsageScenario<PetstoreAppModel>("selectRecords") {
      @Override
      protected void perform(PetstoreAppModel application) {
        SwingEntityModel categoryModel = application.entityModels().iterator().next();
        categoryModel.tableModel().selectionModel().clearSelection();
        categoryModel.tableModel().refresh();
        selectRandomRow(categoryModel.tableModel());
        selectRandomRow(categoryModel.detailModels().iterator().next().tableModel());
        selectRandomRow(categoryModel.detailModels().iterator().next().detailModels().iterator().next().tableModel());
      }
    }));
  }

  @Override
  protected PetstoreAppModel createApplication() throws CancelException {
    PetstoreAppModel applicationModel = new PetstoreAppModel(
            EntityConnectionProvider.builder()
                    .domainClassName(Petstore.class.getName())
                    .clientTypeId(getClass().getSimpleName())
                    .user(getUser())
                    .build());
    SwingEntityModel categoryModel = applicationModel.entityModels().iterator().next();
    categoryModel.addLinkedDetailModel(categoryModel.detailModels().iterator().next());
    SwingEntityModel productModel = categoryModel.detailModels().iterator().next();
    productModel.addLinkedDetailModel(productModel.detailModels().iterator().next());
    SwingEntityModel itemModel = productModel.detailModels().iterator().next();
    itemModel.addLinkedDetailModel(itemModel.detailModels().iterator().next());

    return applicationModel;
  }

  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runner());
  }

  private static final class Runner implements Runnable {
    @Override
    public void run() {
      try {
        new LoadTestPanel<>(new PetstoreLoadTest()).showFrame();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}