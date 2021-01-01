/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.model.tests.AbstractEntityModelTest;
import is.codion.framework.model.tests.TestDomain;
import is.codion.javafx.framework.ui.EntityTableView;

import org.junit.jupiter.api.BeforeAll;
import org.testfx.api.FxToolkit;

public final class FXEntityModelTest extends AbstractEntityModelTest<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Override
  protected FXEntityModel createDepartmentModel() {
    final FXEntityModel entityModel = new FXEntityModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
    new EntityTableView(entityModel.getTableModel());
    final FXEntityModel employeeModel = new FXEntityModel(new FXEntityEditModel(TestDomain.T_EMP, getConnectionProvider()));
    employeeModel.getEditModel().refreshDataModels();
    final FXEntityListModel employeeListModel = employeeModel.getTableModel();
    new EntityTableView(employeeListModel);
    final FXEntityEditModel employeeEditModel = employeeModel.getEditModel();
    new EntityTableView(employeeEditModel.getForeignKeyListModel(TestDomain.EMP_DEPARTMENT_FK));
    entityModel.addDetailModel(employeeModel);
    entityModel.setDetailModelForeignKey(employeeModel, TestDomain.EMP_DEPARTMENT_FK);
    entityModel.addLinkedDetailModel(employeeModel);
    employeeModel.getTableModel().getQueryConditionRequiredState().set(false);

    return entityModel;
  }

  @Override
  protected FXEntityModel createDepartmentModelWithoutDetailModel() {
    final FXEntityModel model = new FXEntityModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
    new EntityTableView(model.getTableModel());

    return model;
  }

  @Override
  protected FXEntityEditModel createDepartmentEditModel() {
    return new FXEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
  }

  @Override
  protected FXEntityListModel createEmployeeTableModel() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, getConnectionProvider());
    new EntityTableView(listModel);

    return listModel;
  }

  @Override
  protected FXEntityListModel createDepartmentTableModel() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
    new EntityTableView(listModel);

    return listModel;
  }

  @Override
  protected FXEntityModel createEmployeeModel() {
    final FXEntityModel model = new FXEntityModel(TestDomain.T_EMP, getConnectionProvider());
    new EntityTableView(model.getTableModel());

    return model;
  }
}
