/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import static org.jminor.framework.demos.empdept.domain.EmpDept.T_DEPARTMENT;
import static org.jminor.framework.demos.empdept.domain.EmpDept.T_EMPLOYEE;
import org.jminor.framework.tools.testing.EntityTestUnit;

import org.junit.Test;

public class EmpDeptTest extends EntityTestUnit {

  @Test
  public void department() throws Exception {
    testEntity(T_DEPARTMENT);
  }

  @Test
  public void employee() throws Exception {
    testEntity(T_EMPLOYEE);
  }

  @Override
  protected User getTestUser() throws CancelException {
    return User.UNIT_TEST_USER;
  }

  @Override
  protected void loadDomainModel() {
    EmpDept.init();
  }
}
