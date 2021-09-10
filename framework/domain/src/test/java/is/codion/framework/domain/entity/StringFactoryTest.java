/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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

public class StringFactoryTest {

  private final TestDomain domain = new TestDomain();
  private final Entities entities = domain.getEntities();

  @Test
  void stringProvider() {
    final Entity department = entities.builder(Department.TYPE)
            .with(Department.NO, -10)
            .with(Department.LOCATION, "Reykjavik")
            .with(Department.NAME, "Sales")
            .build();

    final LocalDateTime hiredate = LocalDateTime.now();
    final Entity employee = entities.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, department)
            .with(Employee.NAME, "Darri")
            .with(Employee.HIREDATE, hiredate)
            .build();

    final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");

    Function<Entity, String> employeeToString = StringFactory.stringFactory(Employee.NAME)
            .text(" (department: ").value(Employee.DEPARTMENT_FK).text(", location: ")
            .foreignKeyValue(Employee.DEPARTMENT_FK, Department.LOCATION).text(", hiredate: ")
            .formattedValue(Employee.HIREDATE, dateFormat.toFormat()).text(")").get();

    assertEquals("Darri (department: Sales, location: Reykjavik, hiredate: " + dateFormat.format(hiredate) + ")", employeeToString.apply(employee));

    department.put(Department.LOCATION, null);
    department.put(Department.NAME, null);

    employee.put(Employee.DEPARTMENT_FK, null);
    employee.put(Employee.NAME, null);
    employee.put(Employee.HIREDATE, null);

    employeeToString = StringFactory.stringFactory(Employee.NAME)
            .text(" (department: ").value(Employee.DEPARTMENT_FK).text(", location: ")
            .foreignKeyValue(Employee.DEPARTMENT_FK, Department.LOCATION).text(", hiredate: ")
            .formattedValue(Employee.HIREDATE, dateFormat.toFormat()).text(")").get();

    assertEquals(" (department: , location: , hiredate: )", employeeToString.apply(employee));
  }
}
