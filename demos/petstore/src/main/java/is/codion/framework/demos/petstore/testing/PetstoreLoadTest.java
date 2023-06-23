/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petstore.domain.Petstore;
import is.codion.framework.demos.petstore.model.PetstoreAppModel;
import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel;

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
    categoryModel.detailModelLink(categoryModel.detailModels().iterator().next()).setActive(true);
    SwingEntityModel productModel = categoryModel.detailModels().iterator().next();
    productModel.detailModelLink(productModel.detailModels().iterator().next()).setActive(true);
    SwingEntityModel itemModel = productModel.detailModels().iterator().next();
    itemModel.detailModelLink(itemModel.detailModels().iterator().next()).setActive(true);

    return applicationModel;
  }

  public static void main(String[] args) {
    new LoadTestPanel<>(new PetstoreLoadTest()).run();
  }
}