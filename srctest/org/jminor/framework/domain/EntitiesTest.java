/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.formats.DateFormats;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import java.text.DateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EntitiesTest {

  @Test
  public void test() {
    final String entityID = "entityID";
    Entities.define(entityID);
    try {
      Entities.define(entityID);
      fail("Should not be able to re-define an entity");
    }
    catch (Exception e) {}
  }

  @Test
  public void stringProvider() {
    EmpDept.init();
    final Entity department = Entities.entity(EmpDept.T_DEPARTMENT);
    department.setValue(EmpDept.DEPARTMENT_ID, -10);
    department.setValue(EmpDept.DEPARTMENT_LOCATION, "Reykjavik");
    department.setValue(EmpDept.DEPARTMENT_NAME, "Sales");

    final Entity employee = Entities.entity(EmpDept.T_EMPLOYEE);
    final Date hiredate = new Date();
    employee.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
    employee.setValue(EmpDept.EMPLOYEE_NAME, "Darri");
    employee.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);

    final DateFormat dateFormat = DateFormats.getDateFormat(DateFormats.SHORT_DOT);

    Entities.StringProvider employeeToString = new Entities.StringProvider(EmpDept.EMPLOYEE_NAME)
            .addText(" (department: ").addValue(EmpDept.EMPLOYEE_DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, EmpDept.DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(EmpDept.EMPLOYEE_HIREDATE, dateFormat).addText(")");

    assertEquals("Darri (department: Sales, location: Reykjavik, hiredate: " + dateFormat.format(hiredate) + ")", employeeToString.toString(employee));

    department.setValue(EmpDept.DEPARTMENT_LOCATION, null);
    department.setValue(EmpDept.DEPARTMENT_NAME, null);

    employee.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, null);
    employee.setValue(EmpDept.EMPLOYEE_NAME, null);
    employee.setValue(EmpDept.EMPLOYEE_HIREDATE, null);

    employeeToString = new Entities.StringProvider(EmpDept.EMPLOYEE_NAME)
            .addText(" (department: ").addValue(EmpDept.EMPLOYEE_DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, EmpDept.DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(EmpDept.EMPLOYEE_HIREDATE, dateFormat).addText(")");

    assertEquals(" (department: , location: , hiredate: )", employeeToString.toString(employee));
  }
}
