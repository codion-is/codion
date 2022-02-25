/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.test.AbstractEntityApplicationModelTest;
import is.codion.framework.model.test.TestDomain;
import is.codion.javafx.framework.ui.EntityTableView;

import org.junit.jupiter.api.BeforeAll;
import org.testfx.api.FxToolkit;

public final class FXEntityApplicationModelTest extends AbstractEntityApplicationModelTest<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Override
  protected FXEntityModel createDepartmentModel() {
    FXEntityModel model = new DeptModel(getConnectionProvider());
    new EntityTableView(model.getTableModel());
    new EntityTableView(model.getDetailModel(TestDomain.T_EMP).getTableModel());

    return model;
  }

  private static class DeptModel extends FXEntityModel {
    private DeptModel(EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_DEPARTMENT, connectionProvider);
      addDetailModel(new FXEntityModel(TestDomain.T_EMP, connectionProvider));
    }
  }
}
