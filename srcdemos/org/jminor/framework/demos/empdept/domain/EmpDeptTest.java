/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.domain;

import org.jminor.common.db.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.tools.testing.EntityTestUnit;

import org.junit.Test;

public class EmpDeptTest extends EntityTestUnit {

  @Test
  public void department() throws Exception {
    testEntity(EmpDept.T_DEPARTMENT);
  }

  @Test
  public void employee() throws Exception {
    testEntity(EmpDept.T_EMPLOYEE);
  }

  @Override
  protected User getTestUser() throws CancelException {
    return new User("scott", "tiger");
  }

  @Override
  protected void loadDomainModel() {
    new EmpDept();
  }
}
