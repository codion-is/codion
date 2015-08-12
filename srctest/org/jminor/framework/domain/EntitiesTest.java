/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;

import org.junit.Test;

import java.sql.Types;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class EntitiesTest {

  @Test
  public void key() {
    final String entityID = "EntitiesTest.key";
    final String propertyID1 = "id1";
    final String propertyID2 = "id2";
    final String propertyID3 = "id3";
    Entities.define(entityID,
            Properties.primaryKeyProperty(propertyID1),
            Properties.primaryKeyProperty(propertyID2).setPrimaryKeyIndex(1),
            Properties.primaryKeyProperty(propertyID3).setPrimaryKeyIndex(2));

    final Entity.Key key = Entities.key(entityID);
    assertEquals(0, key.hashCode());
    assertTrue(key.isCompositeKey());
    assertTrue(key.isNull());

    key.setValue(propertyID1, 1);
    key.setValue(propertyID2, 2);
    key.setValue(propertyID3, 3);
    assertFalse(key.isNull());
    assertEquals(6, key.hashCode());

    key.setValue(propertyID2, 3);
    assertEquals(7, key.hashCode());

    key.setValue(propertyID3, null);
    assertFalse(key.isNull());
    assertEquals(4, key.hashCode());
    key.setValue(propertyID2, null);
    assertFalse(key.isNull());
    assertEquals(1, key.hashCode());
    key.setValue(propertyID1, null);
    assertTrue(key.isNull());
    assertEquals(0, key.hashCode());

    key.setValue(propertyID2, 42);
    assertFalse(key.isNull());
    assertEquals(42, key.hashCode());
  }

  @Test(expected = IllegalArgumentException.class)
  public void keyWithSameIndex() {
    Entities.define("keyWithSameIndex",
            Properties.primaryKeyProperty("1").setPrimaryKeyIndex(0),
            Properties.primaryKeyProperty("2").setPrimaryKeyIndex(1),
            Properties.primaryKeyProperty("3").setPrimaryKeyIndex(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void keyWithSameIndex2() {
    Entities.define("keyWithSameIndex2",
            Properties.primaryKeyProperty("1"),
            Properties.primaryKeyProperty("2"),
            Properties.primaryKeyProperty("3"));
  }

  @Test
  public void entity() {
    TestDomain.init();
    final Entity.Key key = Entities.key(TestDomain.T_MASTER);
    key.setValue(TestDomain.MASTER_ID, 10l);

    final Entity master = Entities.entity(key);
    assertEquals(TestDomain.T_MASTER, master.getEntityID());
    assertTrue(master.containsValue(TestDomain.MASTER_ID));
    assertEquals(10l, master.getValue(TestDomain.MASTER_ID));
  }

  @Test
  public void getProperties() {
    TestDomain.init();
    final Property id = Entities.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID);
    final Property location = Entities.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_LOCATION);
    final Property name = Entities.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME);
    final List<Property> properties = Entities.getProperties(TestDomain.T_DEPARTMENT, Arrays.asList(TestDomain.DEPARTMENT_LOCATION, TestDomain.DEPARTMENT_NAME));
    assertEquals(2, properties.size());
    assertFalse(properties.contains(id));
    assertTrue(properties.contains(location));
    assertTrue(properties.contains(name));

    final Collection<Property> visibleProperties = Entities.getProperties(TestDomain.T_DEPARTMENT, false);
    assertEquals(3, visibleProperties.size());
    assertTrue(visibleProperties.contains(id));
    assertTrue(visibleProperties.contains(location));
    assertTrue(visibleProperties.contains(name));

    final Collection<Property> allProperties = Entities.getProperties(TestDomain.T_DEPARTMENT, true);
    assertTrue(visibleProperties.containsAll(allProperties));
  }

  @Test
  public void getStringProvider() {
    TestDomain.init();
    assertNotNull(Entities.getStringProvider(TestDomain.T_DEPARTMENT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void redefine() {
    final String entityID = "entityID";
    Entities.define(entityID, Properties.primaryKeyProperty("propertyID"));
    Entities.define(entityID, Properties.primaryKeyProperty("propertyID"));
  }

  @Test
  public void redefineAllowed() {
    final String entityID = "entityID2";
    Entities.define(entityID, Properties.primaryKeyProperty("id"));
    assertEquals("id", Entities.getPrimaryKeyProperties(entityID).get(0).getPropertyID());
    Configuration.setValue(Configuration.ALLOW_REDEFINE_ENTITY, true);
    Entities.define(entityID, Properties.primaryKeyProperty("id2"));
    assertEquals("id2", Entities.getPrimaryKeyProperties(entityID).get(0).getPropertyID());
    Configuration.setValue(Configuration.ALLOW_REDEFINE_ENTITY, false);
  }

  @Test
  public void nullValidation() {
    TestDomain.init();
    final Entity emp = Entities.entity(TestDomain.T_EMP);
    emp.setValue(TestDomain.EMP_NAME, "Name");
    emp.setValue(TestDomain.EMP_HIREDATE, new Date());
    emp.setValue(TestDomain.EMP_SALARY, 1200.0);

    final Entities.Validator validator = new Entities.Validator(TestDomain.T_EMP);
    try {
      validator.validate(emp);
      fail();
    }
    catch (final ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.EMP_DEPARTMENT_FK, e.getKey());
    }
    emp.setValue(TestDomain.EMP_DEPARTMENT, 1);
    try {
      validator.validate(emp);
    }
    catch (final ValidationException e) {
      fail();
    }
    emp.setValue(TestDomain.EMP_SALARY, null);
    try {
      validator.validate(emp);
      fail();
    }
    catch (final ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.EMP_SALARY, e.getKey());
    }
  }

  @Test
  public void getSearchProperties() {
    TestDomain.init();
    Collection<Property.ColumnProperty> searchProperties = Entities.getSearchProperties(TestDomain.T_EMP);
    assertTrue(searchProperties.contains(Entities.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB)));
    assertTrue(searchProperties.contains(Entities.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_NAME)));

    searchProperties = Entities.getSearchProperties(TestDomain.T_EMP, TestDomain.EMP_NAME);
    assertTrue(searchProperties.contains(Entities.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_NAME)));

    searchProperties = Entities.getSearchProperties(TestDomain.T_DEPARTMENT);
    //should contain all string based properties
    assertTrue(searchProperties.contains(Entities.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME)));
    assertTrue(searchProperties.contains(Entities.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_LOCATION)));
  }

  @Test
  public void getSearchPropertyIDs() {
    TestDomain.init();
    Collection<String> searchPropertyIDs = Entities.getSearchPropertyIDs(TestDomain.T_EMP);
    assertTrue(searchPropertyIDs.contains(TestDomain.EMP_JOB));
    assertTrue(searchPropertyIDs.contains(TestDomain.EMP_NAME));

    searchPropertyIDs = Entities.getSearchPropertyIDs(TestDomain.T_DEPARTMENT);
    assertTrue(searchPropertyIDs.isEmpty());
  }

  @Test
  public void stringProvider() {
    TestDomain.init();
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.setValue(TestDomain.DEPARTMENT_ID, -10);
    department.setValue(TestDomain.DEPARTMENT_LOCATION, "Reykjavik");
    department.setValue(TestDomain.DEPARTMENT_NAME, "Sales");

    final Entity employee = Entities.entity(TestDomain.T_EMP);
    final Date hiredate = new Date();
    employee.setValue(TestDomain.EMP_DEPARTMENT_FK, department);
    employee.setValue(TestDomain.EMP_NAME, "Darri");
    employee.setValue(TestDomain.EMP_HIREDATE, hiredate);

    final DateFormat dateFormat = DateFormats.getDateFormat(DateFormats.SHORT_DOT);

    Entities.StringProvider employeeToString = new Entities.StringProvider(TestDomain.EMP_NAME)
            .addText(" (department: ").addValue(TestDomain.EMP_DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(TestDomain.EMP_DEPARTMENT_FK, TestDomain.DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(TestDomain.EMP_HIREDATE, dateFormat).addText(")");

    assertEquals("Darri (department: Sales, location: Reykjavik, hiredate: " + dateFormat.format(hiredate) + ")", employeeToString.toString(employee));

    department.setValue(TestDomain.DEPARTMENT_LOCATION, null);
    department.setValue(TestDomain.DEPARTMENT_NAME, null);

    employee.setValue(TestDomain.EMP_DEPARTMENT_FK, null);
    employee.setValue(TestDomain.EMP_NAME, null);
    employee.setValue(TestDomain.EMP_HIREDATE, null);

    employeeToString = new Entities.StringProvider(TestDomain.EMP_NAME)
            .addText(" (department: ").addValue(TestDomain.EMP_DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(TestDomain.EMP_DEPARTMENT_FK, TestDomain.DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(TestDomain.EMP_HIREDATE, dateFormat).addText(")");

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

  @Test (expected = IllegalArgumentException.class)
  public void setSearchPropertyIDsInvalidProperty() {
    Entities.define("spids",
            Properties.primaryKeyProperty("1"),
            Properties.columnProperty("test"))
            .setSearchPropertyIDs("invalid");
  }

  @Test
  public void hasSingleIntegerPrimaryKey() {
    String entityId = "hasSingleIntegerPrimaryKey";
    Entities.define(entityId,
            Properties.columnProperty("test")
                    .setPrimaryKeyIndex(0));
    assertTrue(Entities.hasSingleIntegerPrimaryKey(entityId));
    entityId = "hasSingleIntegerPrimaryKey2";
    Entities.define(entityId,
            Properties.columnProperty("test")
                    .setPrimaryKeyIndex(0),
            Properties.columnProperty("test2")
                    .setPrimaryKeyIndex(1));
    assertFalse(Entities.hasSingleIntegerPrimaryKey(entityId));
    entityId = "hasSingleIntegerPrimaryKey3";
    Entities.define(entityId,
            Properties.columnProperty("test", Types.VARCHAR)
                    .setPrimaryKeyIndex(0));
    assertFalse(Entities.hasSingleIntegerPrimaryKey(entityId));
  }

  @Test
  public void havingClause() {
    final String havingClause = "p1 > 1";
    Entities.define("entityID3",
            Properties.primaryKeyProperty("p0")).setHavingClause(havingClause);
    assertEquals(havingClause, Entities.getHavingClause("entityID3"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateTypeEntity() {
    final Entity entity = Entities.entity(TestDomain.T_DETAIL);
    final Entity entity1 = Entities.entity(TestDomain.T_DETAIL);
    entity.setValue(TestDomain.DETAIL_ENTITY_FK, entity1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setValueDerived() {
    final Entity entity = Entities.entity(TestDomain.T_DETAIL);
    entity.setValue(TestDomain.DETAIL_INT_DERIVED, 10);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setValueValueList() {
    final Entity entity = Entities.entity(TestDomain.T_DETAIL);
    entity.setValue(TestDomain.DETAIL_INT_VALUE_LIST, -10);
  }
}
