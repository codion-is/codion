/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.javafx.framework.model.FXEntityApplicationModel;
import is.codion.javafx.framework.model.FXEntityListModel;
import is.codion.javafx.framework.model.FXEntityModel;
import is.codion.javafx.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

public final class EntityApplicationViewTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Test
  void constructor() {
    EntityApplicationView<FXEntityApplicationModel> applicationView = new EntityApplicationView<FXEntityApplicationModel>("EntityApplicationViewTest") {
      @Override
      protected void createEntityViews() {
        FXEntityListModel listModel = new FXEntityListModel(Employee.TYPE, CONNECTION_PROVIDER);
        FXEntityModel model = new FXEntityModel(listModel);

        addEntityView(new EntityView(model, new EntityEditViewTest.EmpEditView(listModel.editModel()), new EntityTableView(listModel)));
      }

      @Override
      protected FXEntityApplicationModel createApplicationModel(EntityConnectionProvider connectionProvider) {
        return new FXEntityApplicationModel(CONNECTION_PROVIDER);
      }
    };
    applicationView.createApplicationModel(CONNECTION_PROVIDER);
    applicationView.createEntityViews();
  }
}
