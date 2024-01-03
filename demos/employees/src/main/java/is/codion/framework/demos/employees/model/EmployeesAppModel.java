/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

// tag::applicationModel[]
public final class EmployeesAppModel extends SwingEntityApplicationModel {

  public EmployeesAppModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    SwingEntityModel departmentModel = new SwingEntityModel(Department.TYPE, connectionProvider);
    departmentModel.addDetailModel(new SwingEntityModel(new EmployeeEditModel(connectionProvider)));
    departmentModel.tableModel().refresh();
    addEntityModel(departmentModel);
  }
}
// end::applicationModel[]