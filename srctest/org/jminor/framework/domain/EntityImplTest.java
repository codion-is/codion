/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import java.io.File;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class EntityImplTest {

  private final int detailId = 1;
  private final int detailInt = 2;
  private final double detailDouble = 1.2;
  private final String detailString = "string";
  private final Date detailDate = new Date();
  private final Timestamp detailTimestamp = new Timestamp(new Date().getTime());
  private final Boolean detailBoolean = true;

  private final String masterName = "master";

  public static Entity getDetailEntity(final int id, final Integer intValue, final Double doubleValue,
                                       final String stringValue, final Date dateValue, final Timestamp timestampValue,
                                       final Boolean booleanValue, final Entity entityValue) {
    final Entity entity = Entities.entity(EntityTestDomain.T_DETAIL);
    entity.setValue(EntityTestDomain.DETAIL_ID, id);
    entity.setValue(EntityTestDomain.DETAIL_INT, intValue);
    entity.setValue(EntityTestDomain.DETAIL_DOUBLE, doubleValue);
    entity.setValue(EntityTestDomain.DETAIL_STRING, stringValue);
    entity.setValue(EntityTestDomain.DETAIL_DATE, dateValue);
    entity.setValue(EntityTestDomain.DETAIL_TIMESTAMP, timestampValue);
    entity.setValue(EntityTestDomain.DETAIL_BOOLEAN, booleanValue);
    entity.setValue(EntityTestDomain.DETAIL_ENTITY_FK, entityValue);

    return entity;
  }

  public EntityImplTest() {
    EntityTestDomain.init();
    EmpDept.init();
  }

  @Test
  public void serialization() throws Exception {
    final Entity referencedEntityValue = Entities.entity(EntityTestDomain.T_MASTER);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_ID, 1);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_NAME, "name");
    referencedEntityValue.setValue(EntityTestDomain.MASTER_CODE, 10);
    final String originalStringValue = "string value";
    final Entity entity = getDetailEntity(10, 34, 23.4, originalStringValue, new Date(), new Timestamp(System.currentTimeMillis()), true, referencedEntityValue);
    entity.setValue(EntityTestDomain.DETAIL_STRING, "a new String value");
    final File tmp = File.createTempFile("EntityImplTest", "serialization");
    Util.serializeToFile(Arrays.asList(entity), tmp);
    final List<Object> fromFile = Util.deserializeFromFile(tmp);
    assertEquals(1, fromFile.size());
    final Entity entityFromFile = (Entity) fromFile.get(0);
    assertTrue(entity.propertyValuesEqual(entityFromFile));
    assertTrue(entityFromFile.isModified());
    assertTrue(entityFromFile.isModified(EntityTestDomain.DETAIL_STRING));
    assertEquals(originalStringValue, entityFromFile.getOriginalValue(EntityTestDomain.DETAIL_STRING));

    final Entity.Key key = entity.getPrimaryKey();
    final File tmp2 = File.createTempFile("EntityImplTest", "serialization");
    Util.serializeToFile(Arrays.asList(key), tmp2);
    final List<Object> keyFromFile = Util.deserializeFromFile(tmp2);
    assertEquals(1, keyFromFile.size());
    assertEquals(key, keyFromFile.get(0));
  }

  @Test
  public void setAs() {
    final Entity referencedEntityValue = Entities.entity(EntityTestDomain.T_MASTER);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_ID, 2);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_NAME, masterName);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_CODE, 7);

    final Entity test = Entities.entity(EntityTestDomain.T_DETAIL);
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    test.setAs(testEntity);
    assertTrue("Entities should be equal after .setAs()", Util.equal(test, testEntity));
    assertTrue("Entity property values should be equal after .setAs()", test.propertyValuesEqual(testEntity));

    //assure that no cached foreign key values linger
    test.setValue(EntityTestDomain.DETAIL_ENTITY_FK, null);
    testEntity.setAs(test);
    assertNull(testEntity.getValue(EntityTestDomain.DETAIL_ENTITY_ID));
    assertNull(testEntity.getValue(EntityTestDomain.DETAIL_ENTITY_FK));
  }

  @Test
  public void saveRevertValue() {
    final Entity entity = Entities.entity(EntityTestDomain.T_MASTER);
    final String newName = "aname";

    entity.setValue(EntityTestDomain.MASTER_ID, 2);
    entity.setValue(EntityTestDomain.MASTER_NAME, masterName);
    entity.setValue(EntityTestDomain.MASTER_CODE, 7);

    entity.setValue(EntityTestDomain.MASTER_ID, -55);
    //the id is not updatable as it is part of the primary key, which is not updatable by default
    assertFalse(entity.getModifiedState().isActive());
    entity.saveValue(EntityTestDomain.MASTER_ID);
    assertFalse(entity.getModifiedState().isActive());

    entity.setValue(EntityTestDomain.MASTER_NAME, newName);
    assertTrue(entity.getModifiedState().isActive());
    entity.revertValue(EntityTestDomain.MASTER_NAME);
    assertEquals(masterName, entity.getValue(EntityTestDomain.MASTER_NAME));
    assertFalse(entity.getModifiedState().isActive());

    entity.setValue(EntityTestDomain.MASTER_NAME, newName);
    assertTrue(entity.isModified());
    assertTrue(entity.isModified(EntityTestDomain.MASTER_NAME));
    entity.saveValue(EntityTestDomain.MASTER_NAME);
    assertEquals(newName, entity.getValue(EntityTestDomain.MASTER_NAME));
    assertFalse(entity.isModified());
    assertFalse(entity.isModified(EntityTestDomain.MASTER_NAME));
  }

  @Test
  public void entity() throws Exception {
    final Entity referencedEntityValue = Entities.entity(EntityTestDomain.T_MASTER);
    //assert not modified
    assertFalse(referencedEntityValue.isModified());

    referencedEntityValue.setValue(EntityTestDomain.MASTER_ID, 2);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_NAME, masterName);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_CODE, 7);

    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    try {
      testEntity.getProperty(EntityTestDomain.MASTER_CODE);
      fail("Trying to retrieve a property from the wrong entity type");
    }
    catch (IllegalArgumentException e) {}

    //assert types
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ID).getType(), Types.INTEGER);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_INT).getType(), Types.INTEGER);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).getType(), Types.DOUBLE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).getType(), Types.VARCHAR);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DATE).getType(), Types.DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_TIMESTAMP).getType(), Types.TIMESTAMP);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).getType(), Types.BOOLEAN);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_FK).getType(), Types.REF);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_ID).getType(), Types.INTEGER);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).getType(), Types.VARCHAR);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).getType(), Types.INTEGER);

    //assert column names
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ID).getPropertyID(), EntityTestDomain.DETAIL_ID);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_INT).getPropertyID(), EntityTestDomain.DETAIL_INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).getPropertyID(), EntityTestDomain.DETAIL_DOUBLE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).getPropertyID(), EntityTestDomain.DETAIL_STRING);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DATE).getPropertyID(), EntityTestDomain.DETAIL_DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_TIMESTAMP).getPropertyID(), EntityTestDomain.DETAIL_TIMESTAMP);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).getPropertyID(), EntityTestDomain.DETAIL_BOOLEAN);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_ID).getPropertyID(), EntityTestDomain.DETAIL_ENTITY_ID);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).getPropertyID(), EntityTestDomain.DETAIL_MASTER_NAME);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).getPropertyID(), EntityTestDomain.DETAIL_MASTER_CODE);

    //assert captions
    assertNotNull(testEntity.getProperty(EntityTestDomain.DETAIL_ID).getCaption());
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_INT).getCaption(), EntityTestDomain.DETAIL_INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).getCaption(), EntityTestDomain.DETAIL_DOUBLE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).getCaption(), EntityTestDomain.DETAIL_STRING);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DATE).getCaption(), EntityTestDomain.DETAIL_DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_TIMESTAMP).getCaption(), EntityTestDomain.DETAIL_TIMESTAMP);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).getCaption(), EntityTestDomain.DETAIL_BOOLEAN);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_FK).getCaption(), EntityTestDomain.DETAIL_ENTITY_FK);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).getCaption(), EntityTestDomain.DETAIL_MASTER_NAME);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).getCaption(), EntityTestDomain.DETAIL_MASTER_CODE);

    //assert hidden status
    assertTrue(testEntity.getProperty(EntityTestDomain.DETAIL_ID).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_INT).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_DATE).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_TIMESTAMP).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_FK).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).isHidden());

    //assert values
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_ID), detailId);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_INT), detailInt);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_DOUBLE), detailDouble);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_STRING), detailString);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_DATE), detailDate);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_TIMESTAMP), detailTimestamp);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_BOOLEAN), detailBoolean);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_ENTITY_FK), referencedEntityValue);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_NAME), masterName);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_CODE), 7);
    assertFalse(testEntity.isValueNull(EntityTestDomain.DETAIL_ENTITY_ID));

    testEntity.getReferencedPrimaryKey(Entities.getForeignKeyProperty(EntityTestDomain.T_DETAIL, EntityTestDomain.DETAIL_ENTITY_FK));

    //test copy()
    final Entity test2 = (Entity) testEntity.getCopy();
    assertFalse("Entity copy should not be == the original", test2 == testEntity);
    assertTrue("Entities should be equal after .getCopy()", Util.equal(test2, testEntity));
    assertTrue("Entity property values should be equal after .getCopy()", test2.propertyValuesEqual(testEntity));

    test2.setValue(EntityTestDomain.DETAIL_DOUBLE, 2.1);
    assertTrue(test2.isModified());
    assertTrue(test2.getCopy().isModified());

    //test propagate entity reference/denormalized values
    testEntity.setValue(EntityTestDomain.DETAIL_ENTITY_FK, null);
    assertTrue(testEntity.isValueNull(EntityTestDomain.DETAIL_ENTITY_ID));
    assertTrue(testEntity.isValueNull(EntityTestDomain.DETAIL_MASTER_NAME));
    assertTrue(testEntity.isValueNull(EntityTestDomain.DETAIL_MASTER_CODE));

    testEntity.setValue(EntityTestDomain.DETAIL_ENTITY_FK, referencedEntityValue);
    assertFalse(testEntity.isValueNull(EntityTestDomain.DETAIL_ENTITY_ID));
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_ENTITY_ID),
            referencedEntityValue.getValue(EntityTestDomain.MASTER_ID));
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_NAME),
            referencedEntityValue.getValue(EntityTestDomain.MASTER_NAME));
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_CODE),
            referencedEntityValue.getValue(EntityTestDomain.MASTER_CODE));

    referencedEntityValue.setValue(EntityTestDomain.MASTER_CODE, 20);
    testEntity.setValue(EntityTestDomain.DETAIL_ENTITY_FK, referencedEntityValue);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_CODE),
            referencedEntityValue.getValue(EntityTestDomain.MASTER_CODE));
  }

  @Test
  public void isValueNull() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    testEntity.setValue(EntityTestDomain.DETAIL_ENTITY_ID, 10);

    assertFalse(testEntity.isLoaded(EntityTestDomain.DETAIL_ENTITY_FK));
    final Entity referencedEntityValue = (Entity) testEntity.getValue(EntityTestDomain.DETAIL_ENTITY_FK);
    assertEquals(Integer.valueOf(10), referencedEntityValue.getIntValue(EntityTestDomain.MASTER_ID));
    assertFalse(testEntity.isLoaded(EntityTestDomain.DETAIL_ENTITY_FK));
    assertTrue(testEntity.isValueNull(EntityTestDomain.DETAIL_ENTITY_FK));
    assertFalse(testEntity.isValueNull(EntityTestDomain.DETAIL_ENTITY_ID));
  }

  @Test
  public void clear() {
    final Entity referencedEntityValue = Entities.entity(EntityTestDomain.T_MASTER);
    Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    testEntity.clear();
    assertTrue(testEntity.getPrimaryKey().isNull());
    assertTrue(testEntity.isPrimaryKeyNull());
    assertFalse(testEntity.containsValue(EntityTestDomain.DETAIL_DATE));
    assertFalse(testEntity.containsValue(EntityTestDomain.DETAIL_STRING));
    assertFalse(testEntity.containsValue(EntityTestDomain.DETAIL_BOOLEAN));

    testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);

    testEntity.clearPrimaryKeyValues();
    assertTrue(testEntity.getPrimaryKey().isNull());
    assertTrue(testEntity.isPrimaryKeyNull());
    assertTrue(testEntity.containsValue(EntityTestDomain.DETAIL_DATE));
    assertTrue(testEntity.containsValue(EntityTestDomain.DETAIL_STRING));
    assertTrue(testEntity.containsValue(EntityTestDomain.DETAIL_BOOLEAN));
  }

  @Test
  public void setValue() {
    final Entity department = Entities.entity(EmpDept.T_DEPARTMENT);
    department.setValue(EmpDept.DEPARTMENT_ID, -10);

    final Entity employee = Entities.entity(EmpDept.T_EMPLOYEE);
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, 1);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, 1d);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, false);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, 'c');
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, department);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, new Timestamp(System.currentTimeMillis()));
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_SALARY, "test");
      fail();
    }
    catch (IllegalArgumentException e) {}
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    try {
      testEntity.setValue(EntityTestDomain.DETAIL_MASTER_NAME, "hello");
      fail("Set value for a denormalized view property should cause an error");
    }
    catch (IllegalArgumentException e) {}
    try {
      testEntity.setValue(EntityTestDomain.DETAIL_MASTER_CODE, 2);
      fail("Set value for a denormalized property should cause an error");
    }
    catch (IllegalArgumentException e) {}
    try {
      testEntity.setValue(EntityTestDomain.DETAIL_ID, "hello");
      fail("Set string value for a single integer key should cause an error");
    }
    catch (IllegalArgumentException e) {}

    employee.setValue(EmpDept.EMPLOYEE_COMMISSION, 1200d);
    assertEquals(employee.getValue(EmpDept.EMPLOYEE_COMMISSION), 1200d);

    employee.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
    assertEquals(employee.getValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), department);

    final Timestamp date = new Timestamp(DateUtil.floorDate(new Date()).getTime());
    employee.setValue(EmpDept.EMPLOYEE_HIREDATE, date);
    assertEquals(employee.getValue(EmpDept.EMPLOYEE_HIREDATE), date);

    employee.setValue(EmpDept.EMPLOYEE_ID, 123);
    assertEquals(employee.getValue(EmpDept.EMPLOYEE_ID), 123);

    employee.setValue(EmpDept.EMPLOYEE_NAME, "noname");
    assertEquals(employee.getValue(EmpDept.EMPLOYEE_NAME), "noname");

    employee.addValueListener(new ValueChangeListener() {
      @Override
      protected void valueChanged(final ValueChangeEvent changeEvent) {
        if (changeEvent.getKey().equals(EmpDept.EMPLOYEE_DEPARTMENT_FK)) {
          assertTrue(employee.isValueNull(EmpDept.EMPLOYEE_DEPARTMENT_FK));
          assertTrue(employee.isValueNull(EmpDept.EMPLOYEE_DEPARTMENT));
        }
      }
    });
    employee.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, null);
  }

  @Test
  public void propertyValuesEqual() {
    final Entity testEntityOne = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    final Entity testEntityTwo = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);

    assertTrue(testEntityOne.propertyValuesEqual(testEntityTwo));

    testEntityTwo.setValue(EntityTestDomain.DETAIL_INT, 42);

    assertFalse(testEntityOne.propertyValuesEqual(testEntityTwo));
  }

  @Test
  public void getDoubleValue() {
    final Entity employee = Entities.entity(EmpDept.T_EMPLOYEE);
    employee.setValue(EmpDept.EMPLOYEE_ID, -10);

    assertNull(employee.getDoubleValue(EmpDept.EMPLOYEE_SALARY));

    final double salary = 1000.1234;
    employee.setValue(EmpDept.EMPLOYEE_SALARY, salary);
    assertEquals(Double.valueOf(1000.12), employee.getDoubleValue(EmpDept.EMPLOYEE_SALARY));
  }

  @Test
  public void getForeignKeyValue() {
    final Entity department = Entities.entity(EmpDept.T_DEPARTMENT);
    department.setValue(EmpDept.DEPARTMENT_ID, -10);
    final Entity employee = Entities.entity(EmpDept.T_EMPLOYEE);
    employee.setValue(EmpDept.EMPLOYEE_ID, -10);
    assertTrue(employee.isForeignKeyNull(Entities.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK)));
    assertNull(employee.getValue(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertNull(employee.getValue(EmpDept.EMPLOYEE_DEPARTMENT));

    employee.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
    assertFalse(employee.isForeignKeyNull(Entities.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK)));
    assertNotNull(employee.getValue(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertNotNull(employee.getValue(EmpDept.EMPLOYEE_DEPARTMENT));
  }

  @Test (expected = IllegalArgumentException.class)
  public void getForeignKeyValueNonFKProperty() {
    final Entity department = Entities.entity(EmpDept.T_DEPARTMENT);
    department.setValue(EmpDept.DEPARTMENT_ID, -10);
    final Entity employee = Entities.entity(EmpDept.T_EMPLOYEE);
    employee.setValue(EmpDept.EMPLOYEE_ID, -10);
    employee.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);

    employee.getForeignKeyValue(EmpDept.EMPLOYEE_COMMISSION);
  }

  @Test
  public void removeValue() {
    final Entity department = Entities.entity(EmpDept.T_DEPARTMENT);
    department.setValue(EmpDept.DEPARTMENT_ID, -10);
    final Entity employee = Entities.entity(EmpDept.T_EMPLOYEE);
    employee.setValue(EmpDept.EMPLOYEE_ID, -10);
    employee.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
    assertNotNull(employee.getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertEquals(Integer.valueOf(-10), employee.getIntValue(EmpDept.EMPLOYEE_DEPARTMENT));

    employee.removeValue(EmpDept.EMPLOYEE_DEPARTMENT_FK);
    assertNull(employee.getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertNull(employee.getValue(EmpDept.EMPLOYEE_DEPARTMENT));
    assertFalse(employee.containsValue(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertFalse(employee.containsValue(Entities.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT)));
  }
}