/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client;

import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.EntityDbProvider;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import java.util.Arrays;
import java.util.List;

public class EmpDeptAppModel extends EntityApplicationModel {

  public EmpDeptAppModel(final User user) throws UserException {
    super(user, EmpDeptAppModel.class.getSimpleName());
  }

  public EmpDeptAppModel(final EntityDbProvider dbProvider) throws UserException {
    super(dbProvider);
  }

  @Override
  protected List<? extends EntityModel> initializeMainApplicationModels(final EntityDbProvider dbProvider) throws UserException {
    return Arrays.asList(new DepartmentModel(dbProvider));
  }

  /** {@inheritDoc} */
  @Override
  protected void loadDomainModel() {
    new EmpDept();
  }
}
