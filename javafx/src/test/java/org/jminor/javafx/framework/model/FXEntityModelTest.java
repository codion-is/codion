/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.AbstractEntityModelTest;
import org.jminor.javafx.framework.ui.EntityTableView;

import javafx.embed.swing.JFXPanel;

public final class FXEntityModelTest extends AbstractEntityModelTest<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  static {
    new JFXPanel();
  }

  @Override
  protected FXEntityModel createDepartmentModel() {
    final FXEntityModel entityModel = new FXEntityModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    new EntityTableView(entityModel.getTableModel());
    final FXEntityModel employeeModel = new EmpModel(entityModel.getConnectionProvider());
    final FXEntityListModel employeeListModel = (FXEntityListModel) employeeModel.getTableModel();
    new EntityTableView(employeeListModel);
    final FXEntityEditModel employeeEditModel = (FXEntityEditModel) employeeModel.getEditModel();
    new EntityTableView(employeeEditModel.getForeignKeyListModel(TestDomain.EMP_DEPARTMENT_FK));
    entityModel.addDetailModel(employeeModel);
    entityModel.setDetailModelForeignKey(employeeModel, TestDomain.EMP_DEPARTMENT_FK);
    entityModel.addLinkedDetailModel(employeeModel);
    employeeModel.getTableModel().setQueryCriteriaRequired(false);

    return entityModel;
  }

  @Override
  protected FXEntityModel createDepartmentModelWithoutDetailModel() {
    final FXEntityModel model = new FXEntityModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    new EntityTableView(model.getTableModel());

    return model;
  }

  @Override
  protected FXEntityEditModel createDepartmentEditModel() {
    return new FXEntityEditModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Override
  protected FXEntityListModel createEmployeeTableModel() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    new EntityTableView(listModel);

    return listModel;
  }

  @Override
  protected FXEntityListModel createDepartmentTableModel() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    new EntityTableView(listModel);

    return listModel;
  }

  @Override
  protected FXEntityModel createEmployeeModel() {
    final FXEntityModel model = new FXEntityModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    new EntityTableView((FXEntityListModel) model.getTableModel());

    return model;
  }

  public static class EmpModel extends FXEntityModel {
    public EmpModel(final EntityConnectionProvider connectionProvider) {
      super(new FXEntityEditModel(TestDomain.T_EMP, connectionProvider));
      ((FXEntityEditModel) getEditModel()).getForeignKeyListModel(TestDomain.EMP_DEPARTMENT_FK).refresh();
      ((FXEntityEditModel) getEditModel()).getForeignKeyListModel(TestDomain.EMP_MGR_FK).refresh();
    }
  }
}
