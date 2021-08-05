/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.javafx;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.javafx.framework.model.FXEntityApplicationModel;
import is.codion.javafx.framework.model.FXEntityEditModel;
import is.codion.javafx.framework.model.FXEntityListModel;
import is.codion.javafx.framework.model.FXEntityModel;

public final class EmpDeptFXModel extends FXEntityApplicationModel {

  public EmpDeptFXModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels();
  }

  private void setupEntityModels() {
    final FXEntityModel departmentModel = new FXEntityModel(
            new FXEntityEditModel(Department.TYPE, getConnectionProvider()),
            new FXEntityListModel(Department.TYPE, getConnectionProvider()));
    final FXEntityModel employeeModel = new FXEntityModel(
            new FXEntityEditModel(Employee.TYPE, getConnectionProvider()),
            new FXEntityListModel(Employee.TYPE, getConnectionProvider()));
    departmentModel.addDetailModel(employeeModel);

    addEntityModel(departmentModel);
  }
}
