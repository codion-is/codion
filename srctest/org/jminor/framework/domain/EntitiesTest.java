/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.formats.DateFormats;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.*;

public class EntitiesTest {

  @Test
  public void define() {
    final String entityID = "entityID";
    Entities.define(entityID, Properties.primaryKeyProperty("propertyID"));
    try {
      Entities.define(entityID, Properties.primaryKeyProperty("propertyID"));
      fail("Should not be able to re-define an entity");
    }
    catch (Exception e) {}
  }

  @Test
  public void getSearchProperties() {
    Chinook.init();
    Collection<Property.ColumnProperty> searchProperties = Entities.getSearchProperties(Chinook.T_CUSTOMER);
    assertTrue(searchProperties.contains(Entities.getColumnProperty(Chinook.T_CUSTOMER, Chinook.CUSTOMER_FIRSTNAME)));
    assertTrue(searchProperties.contains(Entities.getColumnProperty(Chinook.T_CUSTOMER, Chinook.CUSTOMER_LASTNAME)));
    assertTrue(searchProperties.contains(Entities.getColumnProperty(Chinook.T_CUSTOMER, Chinook.CUSTOMER_EMAIL)));

    searchProperties = Entities.getSearchProperties(Chinook.T_CUSTOMER, Chinook.CUSTOMER_FIRSTNAME, Chinook.CUSTOMER_EMAIL);
    assertTrue(searchProperties.contains(Entities.getColumnProperty(Chinook.T_CUSTOMER, Chinook.CUSTOMER_FIRSTNAME)));
    assertFalse(searchProperties.contains(Entities.getColumnProperty(Chinook.T_CUSTOMER, Chinook.CUSTOMER_LASTNAME)));
    assertTrue(searchProperties.contains(Entities.getColumnProperty(Chinook.T_CUSTOMER, Chinook.CUSTOMER_EMAIL)));

    EmpDept.init();
    searchProperties = Entities.getSearchProperties(EmpDept.T_DEPARTMENT);
    //should contain all string based properties
    assertTrue(searchProperties.contains(Entities.getColumnProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME)));
    assertTrue(searchProperties.contains(Entities.getColumnProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_LOCATION)));
  }

  @Test
  public void getSearchPropertyIDs() {
    Chinook.init();
    Collection<String> searchPropertyIDs = Entities.getSearchPropertyIDs(Chinook.T_CUSTOMER);
    assertTrue(searchPropertyIDs.contains(Chinook.CUSTOMER_FIRSTNAME));
    assertTrue(searchPropertyIDs.contains(Chinook.CUSTOMER_LASTNAME));
    assertTrue(searchPropertyIDs.contains(Chinook.CUSTOMER_EMAIL));

    EmpDept.init();
    searchPropertyIDs = Entities.getSearchPropertyIDs(EmpDept.T_DEPARTMENT);
    assertTrue(searchPropertyIDs.isEmpty());
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
