/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client;

import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.model.EmpDept;

import java.util.List;

public class EmpDeptAppModel extends EntityApplicationModel {

  public EmpDeptAppModel (final User user) throws UserException {
    super(user, EmpDeptAppModel.class.getSimpleName());
  }

  public EmpDeptAppModel(final IEntityDbProvider dbProvider) throws UserException {
    super(dbProvider);
  }

  /** {@inheritDoc} */
  protected List<Class<? extends EntityModel>> getMainEntityModelClasses() throws UserException {
    return EntityModel.asList(DepartmentModel.class);
  }

  /** {@inheritDoc} */
  protected void loadDomainModel() {
    new EmpDept();
  }
}
