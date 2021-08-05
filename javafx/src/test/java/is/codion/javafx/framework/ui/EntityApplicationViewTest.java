/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.javafx.framework.model.FXEntityApplicationModel;
import is.codion.javafx.framework.model.FXEntityEditModel;
import is.codion.javafx.framework.model.FXEntityListModel;
import is.codion.javafx.framework.model.FXEntityModel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

public final class EntityApplicationViewTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Test
  void constructor() {
    final EntityApplicationView<FXEntityApplicationModel> applicationView = new EntityApplicationView<FXEntityApplicationModel>("EntityApplicationViewTest") {
      @Override
      protected void initializeEntityViews() {
        final FXEntityEditModel editModel = new FXEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
        final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
        final FXEntityModel model = new FXEntityModel(editModel, listModel);

        addEntityView(new EntityView(model, new EntityEditViewTest.EmpEditView(editModel), new EntityTableView(listModel)));
      }

      @Override
      protected FXEntityApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
        return new FXEntityApplicationModel(CONNECTION_PROVIDER);
      }
    };
    applicationView.initializeApplicationModel(CONNECTION_PROVIDER);
    applicationView.initializeEntityViews();
  }
}
