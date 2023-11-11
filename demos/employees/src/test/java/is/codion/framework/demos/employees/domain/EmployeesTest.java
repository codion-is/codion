/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.domain;

import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

// tag::domainTest[]
public class EmployeesTest extends EntityTestUnit {

  public EmployeesTest() {
    super(new Employees());
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