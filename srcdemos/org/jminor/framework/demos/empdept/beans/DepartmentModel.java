/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import java.util.Arrays;
import java.util.List;

public class DepartmentModel extends EntityModel {

  public DepartmentModel(final EntityDbProvider dbProvider) {
    super(EmpDept.T_DEPARTMENT, dbProvider);
  }

  @Override
  protected List<? extends EntityModel> initializeDetailModels() {
    return Arrays.asList(new EmployeeModel(getDbProvider()));
  }
}