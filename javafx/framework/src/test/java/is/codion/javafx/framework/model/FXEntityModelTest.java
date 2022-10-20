/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.model.test.AbstractEntityModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;
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
    FXEntityModel entityModel = new FXEntityModel(Department.TYPE, connectionProvider());
    new EntityTableView(entityModel.tableModel());
    FXEntityModel employeeModel = new FXEntityModel(new FXEntityEditModel(Employee.TYPE, connectionProvider()));
    employeeModel.editModel().refreshDataModels();
    FXEntityListModel employeeListModel = employeeModel.tableModel();
    new EntityTableView(employeeListModel);
    FXEntityEditModel employeeEditModel = employeeModel.editModel();
    new EntityTableView(employeeEditModel.foreignKeyListModel(Employee.DEPARTMENT_FK));
    entityModel.addDetailModel(employeeModel, Employee.DEPARTMENT_FK);
    entityModel.addLinkedDetailModel(employeeModel);
    employeeModel.tableModel().queryConditionRequiredState().set(false);

    return entityModel;
  }

  @Override
  protected FXEntityModel createDepartmentModelWithoutDetailModel() {
    FXEntityModel model = new FXEntityModel(Department.TYPE, connectionProvider());
    new EntityTableView(model.tableModel());

    return model;
  }

  @Override
  protected FXEntityEditModel createDepartmentEditModel() {
    return new FXEntityEditModel(Department.TYPE, connectionProvider());
  }

  @Override
  protected FXEntityListModel createEmployeeTableModel() {
    FXEntityListModel listModel = new FXEntityListModel(Employee.TYPE, connectionProvider());
    new EntityTableView(listModel);

    return listModel;
  }

  @Override
  protected FXEntityListModel createDepartmentTableModel() {
    FXEntityListModel listModel = new FXEntityListModel(Department.TYPE, connectionProvider());
    new EntityTableView(listModel);

    return listModel;
  }

  @Override
  protected FXEntityModel createEmployeeModel() {
    FXEntityModel model = new FXEntityModel(Employee.TYPE, connectionProvider());
    new EntityTableView(model.tableModel());

    return model;
  }
}
