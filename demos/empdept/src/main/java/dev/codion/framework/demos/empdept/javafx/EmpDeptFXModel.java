/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.empdept.javafx;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.empdept.domain.EmpDept;
import dev.codion.javafx.framework.model.FXEntityApplicationModel;
import dev.codion.javafx.framework.model.FXEntityEditModel;
import dev.codion.javafx.framework.model.FXEntityListModel;
import dev.codion.javafx.framework.model.FXEntityModel;

public final class EmpDeptFXModel extends FXEntityApplicationModel {

  public EmpDeptFXModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels();
  }

  protected void setupEntityModels() {
    final FXEntityModel departmentModel = new FXEntityModel(
            new FXEntityEditModel(EmpDept.T_DEPARTMENT, getConnectionProvider()),
            new FXEntityListModel(EmpDept.T_DEPARTMENT, getConnectionProvider()));
    final FXEntityModel employeeModel = new FXEntityModel(
            new FXEntityEditModel(EmpDept.T_EMPLOYEE, getConnectionProvider()),
            new FXEntityListModel(EmpDept.T_EMPLOYEE, getConnectionProvider()));
    departmentModel.addDetailModel(employeeModel);

    addEntityModel(departmentModel);
  }
}
