/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import java.util.Arrays;
import java.util.List;

public class DepartmentModel extends EntityModel {

  public DepartmentModel(final EntityDbProvider dbProvider) throws UserException {
    super(EmpDept.T_DEPARTMENT, dbProvider);
  }

  /** {@inheritDoc} */
  @Override
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return Arrays.asList(new EmployeeModel(getDbProvider()));
  }
}