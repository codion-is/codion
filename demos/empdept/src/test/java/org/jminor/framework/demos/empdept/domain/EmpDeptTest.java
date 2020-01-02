/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.domain;

import org.jminor.framework.domain.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import static org.jminor.framework.demos.empdept.domain.EmpDept.T_DEPARTMENT;
import static org.jminor.framework.demos.empdept.domain.EmpDept.T_EMPLOYEE;

public class EmpDeptTest extends EntityTestUnit {

  public EmpDeptTest() {
    super(EmpDept.class.getName());
  }

  @Test
  public void department() throws Exception {
    test(T_DEPARTMENT);
  }

  @Test
  public void employee() throws Exception {
    test(T_EMPLOYEE);
  }
}
