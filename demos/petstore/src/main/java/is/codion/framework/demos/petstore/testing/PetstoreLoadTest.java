/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.testing;

import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petstore.domain.Petstore;
import is.codion.framework.demos.petstore.model.PetstoreAppModel;
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityScenario;

import java.util.function.Function;

import static java.util.Collections.singletonList;

public final class PetstoreLoadTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final class PetstoreAppModelFactory
          implements Function<User, PetstoreAppModel> {

    @Override
    public PetstoreAppModel apply(User user) {
      PetstoreAppModel applicationModel = new PetstoreAppModel(
              EntityConnectionProvider.builder()
                      .domainType(Petstore.DOMAIN)
                      .clientTypeId(getClass().getSimpleName())
                      .user(user)
                      .build());
      SwingEntityModel categoryModel = applicationModel.entityModels().iterator().next();
      categoryModel.detailModelLink(categoryModel.detailModels().iterator().next()).active().set(true);
      SwingEntityModel productModel = categoryModel.detailModels().iterator().next();
      productModel.detailModelLink(productModel.detailModels().iterator().next()).active().set(true);
      SwingEntityModel itemModel = productModel.detailModels().iterator().next();
      itemModel.detailModelLink(itemModel.detailModels().iterator().next()).active().set(true);

      return applicationModel;
    }
  }

  private static final class PetstoreUsageScenario extends AbstractEntityScenario<PetstoreAppModel> {

    private PetstoreUsageScenario() {
      super("selectRecords");
    }

    @Override
    protected void perform(PetstoreAppModel application) throws Exception {
      SwingEntityModel categoryModel = application.entityModels().iterator().next();
      categoryModel.tableModel().selectionModel().clearSelection();
      categoryModel.tableModel().refresh();
      selectRandomRow(categoryModel.tableModel());
      selectRandomRow(categoryModel.detailModels().iterator().next().tableModel());
      selectRandomRow(categoryModel.detailModels().iterator().next().detailModels().iterator().next().tableModel());
    }
  }

  public static void main(String[] args) {
    LoadTest<PetstoreAppModel> loadTest =
            LoadTest.builder(new PetstoreAppModelFactory(),
                            application -> application.connectionProvider().close())
                    .user(UNIT_TEST_USER)
                    .scenarios(singletonList(new PetstoreUsageScenario()))
                    .titleFactory(model -> "Petstore LoadTest - " + EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get())
                    .build();
    new LoadTestPanel<>(LoadTestModel.loadTestModel(loadTest)).run();
  }
}