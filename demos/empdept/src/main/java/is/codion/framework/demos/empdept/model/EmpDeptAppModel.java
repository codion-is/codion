/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

// tag::applicationModel[]
public final class EmpDeptAppModel extends SwingEntityApplicationModel {

  public EmpDeptAppModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    SwingEntityModel departmentModel = new SwingEntityModel(Department.TYPE, connectionProvider);
    departmentModel.addDetailModel(new SwingEntityModel(new EmployeeEditModel(connectionProvider)));
    departmentModel.tableModel().refresh();
    addEntityModel(departmentModel);
  }
}
// end::applicationModel[]