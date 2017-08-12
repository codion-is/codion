/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.model.testing.AbstractEntityModelTest;
import org.jminor.framework.model.testing.TestDomain;
import org.jminor.javafx.framework.ui.EntityTableView;

import javafx.embed.swing.JFXPanel;

public final class FXEntityModelTest extends AbstractEntityModelTest<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.getInstance());

  static {
    new JFXPanel();
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
