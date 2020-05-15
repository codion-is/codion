/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.domain;

import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import static is.codion.framework.demos.empdept.domain.EmpDept.T_DEPARTMENT;
import static is.codion.framework.demos.empdept.domain.EmpDept.T_EMPLOYEE;

// tag::domainTest[]
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
// end::domainTest[]