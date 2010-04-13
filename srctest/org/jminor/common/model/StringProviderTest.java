/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.model.formats.DateFormats;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.util.Date;

/**
 * User: Björn Darri
 * Date: 12.4.2010
 * Time: 23:42:56
 */
public class StringProviderTest {

  private final static String DEPARTMENT_ID = "dept_id";
  private final static String DEPARTMENT_LOCATION = "loc";
  private final static String DEPARTMENT_NAME = "deptname";
  private final static String EMPLOYEE_DEPARTMENT_FK = "empdeptfk";
  private final static String EMPLOYEE_NAME = "empname";
  private final static String EMPLOYEE_HIREDATE = "emphire";

  @Test
  public void test() {
    final ValueMap department = new ValueMapModel() {
      @Override
      public String toString() {
        return (String) getValue(DEPARTMENT_NAME);
      }
      @Override
      public ActionEvent getValueChangeEvent(final String key, final Object newValue, final Object oldValue,
                                                final boolean initialization) {
        return new ActionEvent(this, 0, "none");
      }
    };
    department.setValue(DEPARTMENT_ID, -10);
    department.setValue(DEPARTMENT_LOCATION, "Reykjavik");
    department.setValue(DEPARTMENT_NAME, "Sales");

    final ValueMap employee = new ValueMapModel() {
      @Override
      public ActionEvent getValueChangeEvent(String key, Object newValue, Object oldValue, boolean initialization) {
        return new ActionEvent(this, 0, "none");
      }
    };
    final Date hiredate = new Date();
    employee.setValue(EMPLOYEE_DEPARTMENT_FK, department);
    employee.setValue(EMPLOYEE_NAME, "Darri");
    employee.setValue(EMPLOYEE_HIREDATE, hiredate);

    final DateFormat dateFormat = DateFormats.getDateFormat(DateFormats.SHORT_DOT);

    final StringProvider employeeToString = new StringProvider(EMPLOYEE_NAME)
            .addText(" (department: ").addValue(EMPLOYEE_DEPARTMENT_FK).addText(", location: ")
            .addReferencedValue(EMPLOYEE_DEPARTMENT_FK, DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(EMPLOYEE_HIREDATE, dateFormat).addText(")");

    assertEquals("Darri (department: Sales, location: Reykjavik, hiredate: " + dateFormat.format(hiredate) + ")", employeeToString.toString(employee));
  }
}
