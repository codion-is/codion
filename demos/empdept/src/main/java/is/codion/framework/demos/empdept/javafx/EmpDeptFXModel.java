/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.javafx;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.javafx.framework.model.FXEntityApplicationModel;
import is.codion.javafx.framework.model.FXEntityListModel;
import is.codion.javafx.framework.model.FXEntityModel;

public final class EmpDeptFXModel extends FXEntityApplicationModel {

  public EmpDeptFXModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels();
  }

  private void setupEntityModels() {
    FXEntityModel departmentModel = new FXEntityModel(
            new FXEntityListModel(Department.TYPE, getConnectionProvider()));
    FXEntityModel employeeModel = new FXEntityModel(
            new FXEntityListModel(Employee.TYPE, getConnectionProvider()));
    departmentModel.addDetailModel(employeeModel);

    departmentModel.getTableModel().refresh();

    addEntityModel(departmentModel);
  }
}
