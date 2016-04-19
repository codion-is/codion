/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.javafx;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.model.DefaultEntityApplicationModel;
import org.jminor.framework.model.EntityModel;
import org.jminor.javafx.framework.model.FXEntityEditModel;
import org.jminor.javafx.framework.model.FXEntityListModel;
import org.jminor.javafx.framework.model.FXEntityModel;

public final class EmpDeptFXModel extends DefaultEntityApplicationModel {

  public EmpDeptFXModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels();
  }

  @Override
  protected void loadDomainModel() {
    EmpDept.init();
  }

  protected void setupEntityModels() {
    final EntityModel departmentModel = new FXEntityModel(
            new FXEntityEditModel(EmpDept.T_DEPARTMENT, getConnectionProvider()),
            new FXEntityListModel(EmpDept.T_DEPARTMENT, getConnectionProvider()));
    final EntityModel employeeModel = new FXEntityModel(
            new FXEntityEditModel(EmpDept.T_EMPLOYEE, getConnectionProvider()),
            new FXEntityListModel(EmpDept.T_EMPLOYEE, getConnectionProvider()));
    departmentModel.addDetailModel(employeeModel);

    addEntityModel(departmentModel);
  }
}
