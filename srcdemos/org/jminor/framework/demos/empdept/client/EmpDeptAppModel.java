/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client;

import org.jminor.common.model.User;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

public class EmpDeptAppModel extends EntityApplicationModel {

  public EmpDeptAppModel(final User user) {
    super(user, EmpDeptAppModel.class.getSimpleName());
    addMainApplicationModel(new DepartmentModel(getDbProvider()));
  }

  @Override
  protected void loadDomainModel() {
    new EmpDept();
  }
}
