/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.domain;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

// tag::domainTest[]
public class EmpDeptTest extends EntityTestUnit {

  public EmpDeptTest() {
    super(new EmpDept());
  }

  @Test
  void department() throws Exception {
    test(Department.TYPE);
  }

  @Test
  void employee() throws Exception {
    test(Employee.TYPE);
  }
}
// end::domainTest[]