/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.combo;

import org.jminor.common.db.DbException;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.empdept.model.EmpDept;
import org.jminor.framework.model.Entity;

import java.util.List;

public class ManagerComboBoxModel extends EntityComboBoxModel {

  public ManagerComboBoxModel(final IEntityDbProvider dbProvider) {
    super(dbProvider, EmpDept.T_EMPLOYEE, false, EmpDept.getString(EmpDept.NONE), true);
  }

  /** {@inheritDoc} */
  protected List<Entity> getEntitiesFromDb() throws UserException, DbException {
    try {
      return getDbProvider().getEntityDb().selectMany(EmpDept.T_EMPLOYEE,
              EmpDept.EMPLOYEE_JOB, "MANAGER", "PRESIDENT");
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }
}
