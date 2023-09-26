/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StringFactoryTest {

  private final TestDomain domain = new TestDomain();
  private final Entities entities = domain.entities();

  @Test
  void builder() {
    Entity department = entities.builder(Department.TYPE)
            .with(Department.ID, -10)
            .with(Department.LOCATION, "Reykjavik")
            .with(Department.NAME, "Sales")
            .build();

    LocalDateTime hiredate = LocalDateTime.now();
    Entity employee = entities.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, department)
            .with(Employee.NAME, "Darri")
            .with(Employee.HIREDATE, hiredate)
            .build();

    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");

    Function<Entity, String> employeeToString = StringFactory.builder()
            .value(Employee.NAME)
            .text(" (department: ")
            .value(Employee.DEPARTMENT_FK)
            .text(", location: ")
            .value(Employee.DEPARTMENT_FK, Department.LOCATION)
            .text(", hiredate: ")
            .value(Employee.HIREDATE, dateFormat.toFormat())
            .text(")")
            .build();

    assertEquals("Darri (department: Sales, location: Reykjavik, hiredate: " + dateFormat.format(hiredate) + ")", employeeToString.apply(employee));

    department.put(Department.LOCATION, null);
    department.put(Department.NAME, null);

    employee.put(Employee.DEPARTMENT_FK, null);
    employee.put(Employee.NAME, null);
    employee.put(Employee.HIREDATE, null);

    employeeToString = StringFactory.builder()
            .value(Employee.NAME)
            .text(" (department: ")
            .value(Employee.DEPARTMENT_FK)
            .text(", location: ")
            .value(Employee.DEPARTMENT_FK, Department.LOCATION)
            .text(", hiredate: ")
            .value(Employee.HIREDATE, dateFormat.toFormat())
            .text(")")
            .build();

    assertEquals(" (department: , location: , hiredate: )", employeeToString.apply(employee));
  }

  @Test
  void entityTypeMismatch() {
    assertThrows(IllegalArgumentException.class, () -> StringFactory.builder()
            .value(Department.NAME)
            .value(Employee.HIREDATE));
  }
}
