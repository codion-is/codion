/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.javafx;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.javafx.framework.model.EntityApplicationModel;
import org.jminor.javafx.framework.model.EntityEditModel;
import org.jminor.javafx.framework.model.EntityModel;
import org.jminor.javafx.framework.model.EntityTableModel;

public final class EmpDeptFXModel extends EntityApplicationModel {

  public EmpDeptFXModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels(connectionProvider);
  }

  private void setupEntityModels(final EntityConnectionProvider connectionProvider) {
    final EntityModel departmentModel = new EntityModel(
            new EntityEditModel(EmpDept.T_DEPARTMENT, connectionProvider),
            new EntityTableModel(EmpDept.T_DEPARTMENT, connectionProvider));
    final EntityModel employeeModel = new EntityModel(
            new EntityEditModel(EmpDept.T_EMPLOYEE, connectionProvider),
            new EntityTableModel(EmpDept.T_EMPLOYEE, connectionProvider));
    departmentModel.addDetailModel(employeeModel);

    addEntityModel(departmentModel);
  }
}
