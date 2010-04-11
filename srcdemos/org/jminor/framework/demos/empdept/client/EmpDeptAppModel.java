/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client;

import org.jminor.common.model.User;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import java.util.Arrays;
import java.util.List;

public class EmpDeptAppModel extends EntityApplicationModel {

  public EmpDeptAppModel(final User user) {
    super(user, EmpDeptAppModel.class.getSimpleName());
  }

  public EmpDeptAppModel(final EntityDbProvider dbProvider) {
    super(dbProvider);
  }

  @Override
  protected List<? extends EntityModel> initializeMainApplicationModels(final EntityDbProvider dbProvider) {
    return Arrays.asList(new DepartmentModel(dbProvider));
  }

  @Override
  protected void loadDomainModel() {
    new EmpDept();
  }
}
