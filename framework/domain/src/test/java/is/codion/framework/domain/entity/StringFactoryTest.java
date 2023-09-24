/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
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
            .foreignKeyValue(Employee.DEPARTMENT_FK, Department.LOCATION)
            .text(", hiredate: ")
            .formattedValue(Employee.HIREDATE, dateFormat.toFormat())
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
            .foreignKeyValue(Employee.DEPARTMENT_FK, Department.LOCATION)
            .text(", hiredate: ")
            .formattedValue(Employee.HIREDATE, dateFormat.toFormat())
            .text(")")
            .build();

    assertEquals(" (department: , location: , hiredate: )", employeeToString.apply(employee));
  }
}
