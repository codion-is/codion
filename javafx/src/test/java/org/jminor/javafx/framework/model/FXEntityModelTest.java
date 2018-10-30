/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.model.AbstractEntityModelTest;
import org.jminor.framework.model.TestDomain;
import org.jminor.javafx.framework.ui.EntityTableView;

import org.junit.jupiter.api.BeforeAll;
import org.testfx.api.FxToolkit;

public final class FXEntityModelTest extends AbstractEntityModelTest<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Override
  protected FXEntityModel createDepartmentModel() {
    final FXEntityModel entityModel = new FXEntityModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    new EntityTableView(entityModel.getTableModel());
    final FXEntityModel employeeModel = new FXEntityModel(new FXEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
    employeeModel.getEditModel().refreshDataModels();
    final FXEntityListModel employeeListModel = (FXEntityListModel) employeeModel.getTableModel();
    new EntityTableView(employeeListModel);
    final FXEntityEditModel employeeEditModel = (FXEntityEditModel) employeeModel.getEditModel();
    new EntityTableView(employeeEditModel.getForeignKeyListModel(TestDomain.EMP_DEPARTMENT_FK));
    entityModel.addDetailModel(employeeModel);
    entityModel.setDetailModelForeignKey(employeeModel, TestDomain.EMP_DEPARTMENT_FK);
    entityModel.addLinkedDetailModel(employeeModel);
    employeeModel.getTableModel().setQueryConditionRequired(false);

    return entityModel;
  }

  @Override
  protected FXEntityModel createDepartmentModelWithoutDetailModel() {
    final FXEntityModel model = new FXEntityModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    new EntityTableView(model.getTableModel());

    return model;
  }

  @Override
  protected FXEntityEditModel createDepartmentEditModel() {
    return new FXEntityEditModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
  }

  @Override
  protected FXEntityListModel createEmployeeTableModel() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    new EntityTableView(listModel);

    return listModel;
  }

  @Override
  protected FXEntityListModel createDepartmentTableModel() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    new EntityTableView(listModel);

    return listModel;
  }

  @Override
  protected FXEntityModel createEmployeeModel() {
    final FXEntityModel model = new FXEntityModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    new EntityTableView((FXEntityListModel) model.getTableModel());

    return model;
  }
}
