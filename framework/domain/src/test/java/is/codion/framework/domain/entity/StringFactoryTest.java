/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.DateFormats;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringFactoryTest {

  private final TestDomain domain = new TestDomain();
  private final Entities entities = domain.getEntities();

  @Test
  public void stringProvider() {
    final Entity department = entities.entity(Department.TYPE);
    department.put(Department.NO, -10);
    department.put(Department.LOCATION, "Reykjavik");
    department.put(Department.NAME, "Sales");

    final Entity employee = entities.entity(Employee.TYPE);
    final LocalDateTime hiredate = LocalDateTime.now();
    employee.put(Employee.DEPARTMENT_FK, department);
    employee.put(Employee.NAME, "Darri");
    employee.put(Employee.HIREDATE, hiredate);

    final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DateFormats.SHORT_TIMESTAMP);

    StringFactory employeeToString = new StringFactory(Employee.NAME)
            .addText(" (department: ").addValue(Employee.DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(Employee.DEPARTMENT_FK, Department.LOCATION).addText(", hiredate: ")
            .addFormattedValue(Employee.HIREDATE, dateFormat.toFormat()).addText(")");

    assertEquals("Darri (department: Sales, location: Reykjavik, hiredate: " + dateFormat.format(hiredate) + ")", employeeToString.apply(employee));

    department.put(Department.LOCATION, null);
    department.put(Department.NAME, null);

    employee.put(Employee.DEPARTMENT_FK, null);
    employee.put(Employee.NAME, null);
    employee.put(Employee.HIREDATE, null);

    employeeToString = new StringFactory(Employee.NAME)
            .addText(" (department: ").addValue(Employee.DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(Employee.DEPARTMENT_FK, Department.LOCATION).addText(", hiredate: ")
            .addFormattedValue(Employee.HIREDATE, dateFormat.toFormat()).addText(")");

    assertEquals(" (department: , location: , hiredate: )", employeeToString.apply(employee));
  }
}
