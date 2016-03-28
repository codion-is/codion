/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.javafx;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.javafx.framework.model.EntityApplicationModel;
import org.jminor.javafx.framework.model.EntityEditModel;
import org.jminor.javafx.framework.model.EntityListModel;
import org.jminor.javafx.framework.model.EntityModel;

public final class EmpDeptFXModel extends EntityApplicationModel {

  public EmpDeptFXModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }

  @Override
  protected void loadDomainModel() {
    EmpDept.init();
  }

  protected void setupEntityModels() {
    final EntityModel departmentModel = new EntityModel(
            new EntityEditModel(EmpDept.T_DEPARTMENT, getConnectionProvider()),
            new EntityListModel(EmpDept.T_DEPARTMENT, getConnectionProvider()));
    final EntityModel employeeModel = new EntityModel(
            new EntityEditModel(EmpDept.T_EMPLOYEE, getConnectionProvider()),
            new EntityListModel(EmpDept.T_EMPLOYEE, getConnectionProvider()));
    departmentModel.addDetailModel(employeeModel);

    addEntityModel(departmentModel);
  }
}
