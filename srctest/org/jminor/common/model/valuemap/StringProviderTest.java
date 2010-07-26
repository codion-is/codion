/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.formats.DateFormats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

import java.text.DateFormat;
import java.util.Date;

/**
 * User: Björn Darri
 * Date: 12.4.2010
 * Time: 23:42:56
 */
public class StringProviderTest {

  private static final String DEPARTMENT_ID = "dept_id";
  private static final String DEPARTMENT_LOCATION = "loc";
  private static final String DEPARTMENT_NAME = "deptname";
  private static final String EMPLOYEE_DEPARTMENT_FK = "empdeptfk";
  private static final String EMPLOYEE_NAME = "empname";
  private static final String EMPLOYEE_HIREDATE = "emphire";

  @Test
  public void test() {
    final ValueMap<String, Object> department = new ValueChangeMapImpl<String, Object>() {
      @Override
      public String toString() {
        return (String) getValue(DEPARTMENT_NAME);
      }
    };
    department.setValue(DEPARTMENT_ID, -10);
    department.setValue(DEPARTMENT_LOCATION, "Reykjavik");
    department.setValue(DEPARTMENT_NAME, "Sales");

    final ValueMap<String, Object> employee = new ValueChangeMapImpl<String, Object>();
    final Date hiredate = new Date();
    employee.setValue(EMPLOYEE_DEPARTMENT_FK, department);
    employee.setValue(EMPLOYEE_NAME, "Darri");
    employee.setValue(EMPLOYEE_HIREDATE, hiredate);

    final DateFormat dateFormat = DateFormats.getDateFormat(DateFormats.SHORT_DOT);

    StringProvider<String> employeeToString = new StringProvider<String>(EMPLOYEE_NAME)
            .addText(" (department: ").addValue(EMPLOYEE_DEPARTMENT_FK).addText(", location: ")
            .addReferencedValue(EMPLOYEE_DEPARTMENT_FK, DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(EMPLOYEE_HIREDATE, dateFormat).addText(")");

    assertEquals("Darri (department: Sales, location: Reykjavik, hiredate: " + dateFormat.format(hiredate) + ")", employeeToString.toString(employee));


    department.setValue(DEPARTMENT_LOCATION, null);
    department.setValue(DEPARTMENT_NAME, null);

    employee.setValue(EMPLOYEE_DEPARTMENT_FK, null);
    employee.setValue(EMPLOYEE_DEPARTMENT_FK, department);
    employee.setValue(EMPLOYEE_NAME, null);
    employee.setValue(EMPLOYEE_HIREDATE, null);

    employeeToString = new StringProvider<String>(EMPLOYEE_NAME)
            .addText(" (department: ").addValue(EMPLOYEE_DEPARTMENT_FK).addText(", location: ")
            .addReferencedValue(EMPLOYEE_DEPARTMENT_FK, DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(EMPLOYEE_HIREDATE, dateFormat).addText(")");

    assertEquals(" (department: null, location: , hiredate: )", employeeToString.toString(employee));

    employee.setValue(EMPLOYEE_DEPARTMENT_FK, null);
    assertEquals(" (department: , location: , hiredate: )", employeeToString.toString(employee));

    employee.setValue(EMPLOYEE_DEPARTMENT_FK, "hello");
    try {
      employeeToString.toString(employee);
      fail();
    }
    catch (RuntimeException e) {}

    new StringProvider();
  }
}
