/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.domain;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.domain.testing.EntityTestUnit;

import org.junit.Test;

import static org.jminor.framework.demos.empdept.domain.EmpDept.T_DEPARTMENT;
import static org.jminor.framework.demos.empdept.domain.EmpDept.T_EMPLOYEE;

public class EmpDeptTest extends EntityTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  public EmpDeptTest() {
    super(EmpDept.class.getName());
  }

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
    return UNIT_TEST_USER;
  }
}
