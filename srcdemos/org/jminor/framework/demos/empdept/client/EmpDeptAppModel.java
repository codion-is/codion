/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client;

import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

public class EmpDeptAppModel extends EntityApplicationModel {

  public EmpDeptAppModel(final EntityDbProvider dbProvider) {
    super(dbProvider);
    addMainApplicationModel(new DepartmentModel(dbProvider));
  }

  @Override
  protected void loadDomainModel() {
    new EmpDept();
  }
}
