/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.domain;

import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

// tag::domainTest[]
public class EmpDeptTest extends EntityTestUnit {

  public EmpDeptTest() {
    super(EmpDept.class.getName());
  }

  @Test
  public void department() throws Exception {
    test(EmpDept.Department.TYPE);
  }

  @Test
  public void employee() throws Exception {
    test(EmpDept.Employee.TYPE);
  }
}
// end::domainTest[]