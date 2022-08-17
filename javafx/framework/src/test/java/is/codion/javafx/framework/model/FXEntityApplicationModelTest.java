/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.test.AbstractEntityApplicationModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;
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
    FXEntityModel model = new DeptModel(connectionProvider());
    new EntityTableView(model.tableModel());
    new EntityTableView(model.detailModel(Employee.TYPE).tableModel());

    return model;
  }

  private static class DeptModel extends FXEntityModel {
    private DeptModel(EntityConnectionProvider connectionProvider) {
      super(Department.TYPE, connectionProvider);
      addDetailModel(new FXEntityModel(Employee.TYPE, connectionProvider));
    }
  }
}
