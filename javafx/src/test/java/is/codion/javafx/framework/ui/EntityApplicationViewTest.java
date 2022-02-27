/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.javafx.framework.model.FXEntityApplicationModel;
import is.codion.javafx.framework.model.FXEntityListModel;
import is.codion.javafx.framework.model.FXEntityModel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

public final class EntityApplicationViewTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Test
  void constructor() {
    EntityApplicationView<FXEntityApplicationModel> applicationView = new EntityApplicationView<FXEntityApplicationModel>("EntityApplicationViewTest") {
      @Override
      protected void initializeEntityViews() {
        FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
        FXEntityModel model = new FXEntityModel(listModel);

        addEntityView(new EntityView(model, new EntityEditViewTest.EmpEditView(listModel.getEditModel()), new EntityTableView(listModel)));
      }

      @Override
      protected FXEntityApplicationModel initializeApplicationModel(EntityConnectionProvider connectionProvider) {
        return new FXEntityApplicationModel(CONNECTION_PROVIDER);
      }
    };
    applicationView.initializeApplicationModel(CONNECTION_PROVIDER);
    applicationView.initializeEntityViews();
  }
}
