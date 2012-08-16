/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class EntitiesTest {

  @Test
  public void entity() {
    Chinook.init();
    final Entity.Key key = Entities.key(Chinook.T_ALBUM);
    key.setValue(Chinook.ALBUM_ALBUMID, 10);

    final Entity album = Entities.entity(key);
    assertEquals(Chinook.T_ALBUM, album.getEntityID());
    assertTrue(album.containsValue(Chinook.ALBUM_ALBUMID));
    assertEquals(10, album.getValue(Chinook.ALBUM_ALBUMID));
  }

  @Test
  public void getProperties() {
    EmpDept.init();
    final Property id = Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_ID);
    final Property location = Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_LOCATION);
    final Property name = Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME);
    final List<Property> properties = Entities.getProperties(EmpDept.T_DEPARTMENT, Arrays.asList(EmpDept.DEPARTMENT_LOCATION, EmpDept.DEPARTMENT_NAME));
    assertEquals(2, properties.size());
    assertFalse(properties.contains(id));
    assertTrue(properties.contains(location));
    assertTrue(properties.contains(name));

    final Collection<Property> visibleProperties = Entities.getProperties(EmpDept.T_DEPARTMENT, false);
    assertEquals(3, visibleProperties.size());
    assertTrue(visibleProperties.contains(id));
    assertTrue(visibleProperties.contains(location));
    assertTrue(visibleProperties.contains(name));

    final Collection<Property> allProperties = Entities.getProperties(EmpDept.T_DEPARTMENT, true);
    assertTrue(visibleProperties.containsAll(allProperties));
  }

  @Test
  public void getStringProvider() {
    EmpDept.init();
    assertNotNull(Entities.getStringProvider(EmpDept.T_DEPARTMENT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void redefine() {
    final String entityID = "entityID";
    Entities.define(entityID, Properties.primaryKeyProperty("propertyID"));
    Entities.define(entityID, Properties.primaryKeyProperty("propertyID"));
  }

  @Test
  public void nullValidation() {
    Chinook.init();
    final Entity invoiceLine = Entities.entity(Chinook.T_INVOICELINE);
    invoiceLine.setValue(Chinook.INVOICELINE_INVOICELINEID, 1);
    invoiceLine.setValue(Chinook.INVOICELINE_QUANTITY, 1);
    invoiceLine.setValue(Chinook.INVOICELINE_UNITPRICE, 1.0);
    invoiceLine.setValue(Chinook.INVOICELINE_TRACKID, 1);

    final Entities.Validator validator = new Entities.Validator(Chinook.T_INVOICELINE);
    try {
      validator.validate(invoiceLine);
      fail();
    }
    catch (ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(Chinook.INVOICELINE_INVOICEID_FK, e.getKey());
    }
    invoiceLine.setValue(Chinook.INVOICELINE_UNITPRICE, null);
    invoiceLine.setValue(Chinook.INVOICELINE_INVOICEID, 1);
    try {
      validator.validate(invoiceLine);
      fail();
    }
    catch (ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(Chinook.INVOICELINE_UNITPRICE, e.getKey());
    }
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

  @Test (expected = IllegalArgumentException.class)
  public void foreignKeyReferencingUndefinedEntity() {
    Entities.define("test.entity",
            Properties.primaryKeyProperty("id"),
            Properties.foreignKeyProperty("fk_id_fk", "caption", "test.referenced_entity",
                    Properties.columnProperty("fk_id")));
  }

  @Test
  public void foreignKeyReferencingUndefinedEntityNonStrict() {
    Configuration.setValue(Configuration.STRICT_FOREIGN_KEYS, false);
    Entities.define("test.entity",
            Properties.primaryKeyProperty("id"),
            Properties.foreignKeyProperty("fk_id_fk", "caption", "test.referenced_entity",
                    Properties.columnProperty("fk_id")));
    Configuration.setValue(Configuration.STRICT_FOREIGN_KEYS, true);
  }
}
