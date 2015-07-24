/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChange;

import org.junit.Test;

import java.io.File;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class DefaultEntityTest {

  private final long detailId = 1;
  private final int detailInt = 2;
  private final double detailDouble = 1.2;
  private final String detailString = "string";
  private final Date detailDate = new Date();
  private final Timestamp detailTimestamp = new Timestamp(new Date().getTime());
  private final Boolean detailBoolean = true;

  private final String masterName = "master";

  public DefaultEntityTest() {
    TestDomain.init();
    TestDomain.init();
  }

  @Test
  public void serialization() throws Exception {
    final Entity referencedEntityValue = Entities.entity(TestDomain.T_MASTER);
    referencedEntityValue.setValue(TestDomain.MASTER_ID, 1l);
    referencedEntityValue.setValue(TestDomain.MASTER_NAME, "name");
    referencedEntityValue.setValue(TestDomain.MASTER_CODE, 10);
    final String originalStringValue = "string value";
    final Entity entity = getDetailEntity(10, 34, 23.4, originalStringValue, new Date(), new Timestamp(System.currentTimeMillis()), true, referencedEntityValue);
    entity.setValue(TestDomain.DETAIL_STRING, "a new String value");
    final File tmp = File.createTempFile("DefaultEntityTest", "serialization");
    Util.serializeToFile(Collections.singletonList(entity), tmp);
    final List<Object> fromFile = Util.deserializeFromFile(tmp);
    assertEquals(1, fromFile.size());
    final Entity entityFromFile = (Entity) fromFile.get(0);
    assertTrue(entity.propertyValuesEqual(entityFromFile));
    assertTrue(entityFromFile.isModified());
    assertTrue(entityFromFile.isModified(TestDomain.DETAIL_STRING));
    assertEquals(originalStringValue, entityFromFile.getOriginalValue(TestDomain.DETAIL_STRING));

    final Entity.Key key = entity.getPrimaryKey();
    final File tmp2 = File.createTempFile("DefaultEntityTest", "serialization");
    Util.serializeToFile(Collections.singletonList(key), tmp2);
    final List<Object> keyFromFile = Util.deserializeFromFile(tmp2);
    assertEquals(1, keyFromFile.size());
    assertEquals(key, keyFromFile.get(0));
  }

  @Test
  public void setAs() {
    final Entity referencedEntityValue = Entities.entity(TestDomain.T_MASTER);
    referencedEntityValue.setValue(TestDomain.MASTER_ID, 2l);
    referencedEntityValue.setValue(TestDomain.MASTER_NAME, masterName);
    referencedEntityValue.setValue(TestDomain.MASTER_CODE, 7);

    final Entity test = Entities.entity(TestDomain.T_DETAIL);
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    test.setAs(testEntity);
    assertTrue("Entities should be equal after .setAs()", Util.equal(test, testEntity));
    assertTrue("Entity property values should be equal after .setAs()", test.propertyValuesEqual(testEntity));

    //assure that no cached foreign key values linger
    test.setValue(TestDomain.DETAIL_ENTITY_FK, null);
    testEntity.setAs(test);
    assertNull(testEntity.getValue(TestDomain.DETAIL_ENTITY_ID));
    assertNull(testEntity.getValue(TestDomain.DETAIL_ENTITY_FK));
  }

  @Test
  public void saveRevertValue() {
    final Entity entity = Entities.entity(TestDomain.T_MASTER);
    final String newName = "aname";

    entity.setValue(TestDomain.MASTER_ID, 2l);
    entity.setValue(TestDomain.MASTER_NAME, masterName);
    entity.setValue(TestDomain.MASTER_CODE, 7);

    entity.setValue(TestDomain.MASTER_ID, -55l);
    //the id is not updatable as it is part of the primary key, which is not updatable by default
    assertFalse(entity.getModifiedObserver().isActive());
    entity.saveValue(TestDomain.MASTER_ID);
    assertFalse(entity.getModifiedObserver().isActive());

    entity.setValue(TestDomain.MASTER_NAME, newName);
    assertTrue(entity.getModifiedObserver().isActive());
    entity.revertValue(TestDomain.MASTER_NAME);
    assertEquals(masterName, entity.getValue(TestDomain.MASTER_NAME));
    assertFalse(entity.getModifiedObserver().isActive());

    entity.setValue(TestDomain.MASTER_NAME, newName);
    assertTrue(entity.isModified());
    assertTrue(entity.isModified(TestDomain.MASTER_NAME));
    entity.saveValue(TestDomain.MASTER_NAME);
    assertEquals(newName, entity.getValue(TestDomain.MASTER_NAME));
    assertFalse(entity.isModified());
    assertFalse(entity.isModified(TestDomain.MASTER_NAME));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getPropertyWrongEntityType() {
    final Entity testEntity = Entities.entity(TestDomain.T_DETAIL);
    testEntity.getProperty(TestDomain.MASTER_CODE);
  }

  @Test
  public void entity() throws Exception {
    final Entity referencedEntityValue = Entities.entity(TestDomain.T_MASTER);
    //assert not modified
    assertFalse(referencedEntityValue.isModified());

    referencedEntityValue.setValue(TestDomain.MASTER_ID, 2l);
    referencedEntityValue.setValue(TestDomain.MASTER_NAME, masterName);
    referencedEntityValue.setValue(TestDomain.MASTER_CODE, 7);

    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    //assert types
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_ID).getType(), Types.BIGINT);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_INT).getType(), Types.INTEGER);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_DOUBLE).getType(), Types.DOUBLE);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_STRING).getType(), Types.VARCHAR);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_DATE).getType(), Types.DATE);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_TIMESTAMP).getType(), Types.TIMESTAMP);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_BOOLEAN).getType(), Types.BOOLEAN);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_ENTITY_FK).getType(), Types.REF);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_ENTITY_ID).getType(), Types.BIGINT);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_NAME).getType(), Types.VARCHAR);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_CODE).getType(), Types.INTEGER);

    //assert column names
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_ID).getPropertyID(), TestDomain.DETAIL_ID);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_INT).getPropertyID(), TestDomain.DETAIL_INT);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_DOUBLE).getPropertyID(), TestDomain.DETAIL_DOUBLE);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_STRING).getPropertyID(), TestDomain.DETAIL_STRING);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_DATE).getPropertyID(), TestDomain.DETAIL_DATE);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_TIMESTAMP).getPropertyID(), TestDomain.DETAIL_TIMESTAMP);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_BOOLEAN).getPropertyID(), TestDomain.DETAIL_BOOLEAN);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_ENTITY_ID).getPropertyID(), TestDomain.DETAIL_ENTITY_ID);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_NAME).getPropertyID(), TestDomain.DETAIL_MASTER_NAME);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_CODE).getPropertyID(), TestDomain.DETAIL_MASTER_CODE);

    //assert captions
    assertNotNull(testEntity.getProperty(TestDomain.DETAIL_ID).getCaption());
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_INT).getCaption(), TestDomain.DETAIL_INT);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_DOUBLE).getCaption(), TestDomain.DETAIL_DOUBLE);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_STRING).getCaption(), "Detail string");
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_DATE).getCaption(), TestDomain.DETAIL_DATE);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_TIMESTAMP).getCaption(), TestDomain.DETAIL_TIMESTAMP);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_BOOLEAN).getCaption(), TestDomain.DETAIL_BOOLEAN);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_ENTITY_FK).getCaption(), TestDomain.DETAIL_ENTITY_FK);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_NAME).getCaption(), TestDomain.DETAIL_MASTER_NAME);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_CODE).getCaption(), TestDomain.DETAIL_MASTER_CODE);

    //assert hidden status
    assertTrue(testEntity.getProperty(TestDomain.DETAIL_ID).isHidden());
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_INT).isHidden());
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_DOUBLE).isHidden());
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_STRING).isHidden());
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_DATE).isHidden());
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_TIMESTAMP).isHidden());
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_BOOLEAN).isHidden());
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_ENTITY_FK).isHidden());
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_MASTER_NAME).isHidden());
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_MASTER_CODE).isHidden());

    //assert values
    assertEquals(testEntity.getValue(TestDomain.DETAIL_ID), detailId);
    assertEquals(testEntity.getValue(TestDomain.DETAIL_INT), detailInt);
    assertEquals(testEntity.getValue(TestDomain.DETAIL_DOUBLE), detailDouble);
    assertEquals(testEntity.getValue(TestDomain.DETAIL_STRING), detailString);
    assertEquals(testEntity.getValue(TestDomain.DETAIL_DATE), detailDate);
    assertEquals(testEntity.getValue(TestDomain.DETAIL_TIMESTAMP), detailTimestamp);
    assertEquals(testEntity.getValue(TestDomain.DETAIL_BOOLEAN), detailBoolean);
    assertEquals(testEntity.getValue(TestDomain.DETAIL_ENTITY_FK), referencedEntityValue);
    assertEquals(testEntity.getValue(TestDomain.DETAIL_MASTER_NAME), masterName);
    assertEquals(testEntity.getValue(TestDomain.DETAIL_MASTER_CODE), 7);
    assertFalse(testEntity.isValueNull(TestDomain.DETAIL_ENTITY_ID));

    testEntity.getReferencedPrimaryKey(Entities.getForeignKeyProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_ENTITY_FK));

    //test copy()
    final Entity test2 = (Entity) testEntity.getCopy();
    assertFalse("Entity copy should not be == the original", test2 == testEntity);
    assertTrue("Entities should be equal after .getCopy()", Util.equal(test2, testEntity));
    assertTrue("Entity property values should be equal after .getCopy()", test2.propertyValuesEqual(testEntity));
    assertFalse("This should be a deep copy",
            testEntity.getForeignKeyValue(TestDomain.DETAIL_ENTITY_FK) == test2.getForeignKeyValue(TestDomain.DETAIL_ENTITY_FK));

    test2.setValue(TestDomain.DETAIL_DOUBLE, 2.1);
    assertTrue(test2.isModified());
    assertTrue(test2.getCopy().isModified());

    //test propagate entity reference/denormalized values
    testEntity.setValue(TestDomain.DETAIL_ENTITY_FK, null);
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_ENTITY_ID));
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_MASTER_NAME));
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_MASTER_CODE));

    testEntity.setValue(TestDomain.DETAIL_ENTITY_FK, referencedEntityValue);
    assertFalse(testEntity.isValueNull(TestDomain.DETAIL_ENTITY_ID));
    assertEquals(testEntity.getValue(TestDomain.DETAIL_ENTITY_ID),
            referencedEntityValue.getValue(TestDomain.MASTER_ID));
    assertEquals(testEntity.getValue(TestDomain.DETAIL_MASTER_NAME),
            referencedEntityValue.getValue(TestDomain.MASTER_NAME));
    assertEquals(testEntity.getValue(TestDomain.DETAIL_MASTER_CODE),
            referencedEntityValue.getValue(TestDomain.MASTER_CODE));

    referencedEntityValue.setValue(TestDomain.MASTER_CODE, 20);
    testEntity.setValue(TestDomain.DETAIL_ENTITY_FK, referencedEntityValue);
    assertEquals(testEntity.getValue(TestDomain.DETAIL_MASTER_CODE),
            referencedEntityValue.getValue(TestDomain.MASTER_CODE));
  }

  @Test
  public void isValueNull() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_ENTITY_ID));
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_ENTITY_FK));
    assertTrue(testEntity.isForeignKeyNull(Entities.getForeignKeyProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_ENTITY_FK)));
    testEntity.setValue(TestDomain.DETAIL_ENTITY_ID, 10l);

    assertFalse(testEntity.isLoaded(TestDomain.DETAIL_ENTITY_FK));
    final Entity referencedEntityValue = testEntity.getForeignKeyValue(TestDomain.DETAIL_ENTITY_FK);
    assertEquals(Long.valueOf(10), referencedEntityValue.getValue(TestDomain.MASTER_ID));
    assertFalse(testEntity.isLoaded(TestDomain.DETAIL_ENTITY_FK));
    assertFalse(testEntity.isValueNull(TestDomain.DETAIL_ENTITY_FK));
    assertFalse(testEntity.isValueNull(TestDomain.DETAIL_ENTITY_ID));
  }

  @Test
  public void clear() {
    final Entity referencedEntityValue = Entities.entity(TestDomain.T_MASTER);
    Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    testEntity.setValue(TestDomain.DETAIL_STRING, "TestString");
    assertTrue(testEntity.isModified());

    testEntity.clear();
    assertTrue(testEntity.getPrimaryKey().isNull());
    assertTrue(testEntity.isPrimaryKeyNull());
    assertFalse(testEntity.containsValue(TestDomain.DETAIL_DATE));
    assertFalse(testEntity.containsValue(TestDomain.DETAIL_STRING));
    assertFalse(testEntity.containsValue(TestDomain.DETAIL_BOOLEAN));
    assertFalse(testEntity.isModified());

    testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);

    testEntity.clearPrimaryKeyValues();
    assertTrue(testEntity.getPrimaryKey().isNull());
    assertTrue(testEntity.isPrimaryKeyNull());
    assertTrue(testEntity.containsValue(TestDomain.DETAIL_DATE));
    assertTrue(testEntity.containsValue(TestDomain.DETAIL_STRING));
    assertTrue(testEntity.containsValue(TestDomain.DETAIL_BOOLEAN));
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueInt() {
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_NAME, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueDouble() {
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_NAME, 1d);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueBoolean() {
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_NAME, false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueChar() {
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_NAME, 'c');
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueEntity() {
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.setValue(TestDomain.DEPARTMENT_ID, -10);

    employee.setValue(TestDomain.EMPLOYEE_NAME, department);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueDate() {
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_NAME, new Date());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueTimestamp() {
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_NAME, new Timestamp(System.currentTimeMillis()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void setDoubleValueString() {
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_SALARY, "test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setDenormalizedViewValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    testEntity.setValue(TestDomain.DETAIL_MASTER_NAME, "hello");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setDenormalizedValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    testEntity.setValue(TestDomain.DETAIL_MASTER_CODE, 2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setIntegerKeyString() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    testEntity.setValue(TestDomain.DETAIL_ID, "hello");
  }

  @Test
  public void setValue() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.setValue(TestDomain.DEPARTMENT_ID, -10);

    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);

    employee.setValue(TestDomain.EMPLOYEE_COMMISSION, 1200d);
    assertEquals(employee.getValue(TestDomain.EMPLOYEE_COMMISSION), 1200d);

    employee.setValue(TestDomain.EMPLOYEE_DEPARTMENT_FK, department);
    assertEquals(employee.getValue(TestDomain.EMPLOYEE_DEPARTMENT_FK), department);

    final Timestamp date = new Timestamp(DateUtil.floorDate(new Date()).getTime());
    employee.setValue(TestDomain.EMPLOYEE_HIREDATE, date);
    assertEquals(employee.getValue(TestDomain.EMPLOYEE_HIREDATE), date);

    employee.setValue(TestDomain.EMPLOYEE_ID, 123);
    assertEquals(employee.getValue(TestDomain.EMPLOYEE_ID), 123);

    employee.setValue(TestDomain.EMPLOYEE_NAME, "noname");
    assertEquals(employee.getValue(TestDomain.EMPLOYEE_NAME), "noname");

    employee.addValueListener(new EventInfoListener<ValueChange<String, ?>>() {
      @Override
      public void eventOccurred(final ValueChange info) {
        if (info.getKey().equals(TestDomain.EMPLOYEE_DEPARTMENT_FK)) {
          assertTrue(employee.isValueNull(TestDomain.EMPLOYEE_DEPARTMENT_FK));
          assertTrue(employee.isValueNull(TestDomain.EMPLOYEE_DEPARTMENT));
        }
      }
    });
    employee.setValue(TestDomain.EMPLOYEE_DEPARTMENT_FK, null);
  }

  @Test
  public void propertyValuesEqual() {
    final Entity testEntityOne = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    final Entity testEntityTwo = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);

    assertTrue(testEntityOne.propertyValuesEqual(testEntityTwo));

    testEntityTwo.setValue(TestDomain.DETAIL_INT, 42);

    assertFalse(testEntityOne.propertyValuesEqual(testEntityTwo));
  }

  @Test
  public void getDoubleValue() {
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_ID, -10);

    assertNull(employee.getDoubleValue(TestDomain.EMPLOYEE_SALARY));

    final double salary = 1000.1234;
    employee.setValue(TestDomain.EMPLOYEE_SALARY, salary);
    assertEquals(Double.valueOf(1000.12), employee.getDoubleValue(TestDomain.EMPLOYEE_SALARY));
  }

  @Test
  public void getForeignKeyValue() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.setValue(TestDomain.DEPARTMENT_ID, -10);
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_ID, -10);
    assertTrue(employee.isForeignKeyNull(Entities.getForeignKeyProperty(TestDomain.T_EMPLOYEE, TestDomain.EMPLOYEE_DEPARTMENT_FK)));
    assertNull(employee.getValue(TestDomain.EMPLOYEE_DEPARTMENT_FK));
    assertNull(employee.getValue(TestDomain.EMPLOYEE_DEPARTMENT));

    employee.setValue(TestDomain.EMPLOYEE_DEPARTMENT_FK, department);
    assertFalse(employee.isForeignKeyNull(Entities.getForeignKeyProperty(TestDomain.T_EMPLOYEE, TestDomain.EMPLOYEE_DEPARTMENT_FK)));
    assertNotNull(employee.getValue(TestDomain.EMPLOYEE_DEPARTMENT_FK));
    assertNotNull(employee.getValue(TestDomain.EMPLOYEE_DEPARTMENT));
  }

  @Test (expected = IllegalArgumentException.class)
  public void getForeignKeyValueNonFKProperty() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.setValue(TestDomain.DEPARTMENT_ID, -10);
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_ID, -10);
    employee.setValue(TestDomain.EMPLOYEE_DEPARTMENT_FK, department);

    employee.getForeignKeyValue(TestDomain.EMPLOYEE_COMMISSION);
  }

  @Test
  public void removeValue() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.setValue(TestDomain.DEPARTMENT_ID, -10);
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_ID, -10);
    employee.setValue(TestDomain.EMPLOYEE_DEPARTMENT_FK, department);
    assertNotNull(employee.getForeignKeyValue(TestDomain.EMPLOYEE_DEPARTMENT_FK));
    assertEquals(Integer.valueOf(-10), employee.getIntValue(TestDomain.EMPLOYEE_DEPARTMENT));

    employee.removeValue(TestDomain.EMPLOYEE_DEPARTMENT_FK);
    assertNull(employee.getForeignKeyValue(TestDomain.EMPLOYEE_DEPARTMENT_FK));
    assertNull(employee.getValue(TestDomain.EMPLOYEE_DEPARTMENT));
    assertFalse(employee.containsValue(TestDomain.EMPLOYEE_DEPARTMENT_FK));
    assertFalse(employee.containsValue(Entities.getProperty(TestDomain.T_EMPLOYEE, TestDomain.EMPLOYEE_DEPARTMENT)));
  }

  @Test
  public void maximumFractionDigits() {
    final Entity employee = Entities.entity(TestDomain.T_EMPLOYEE);
    employee.setValue(TestDomain.EMPLOYEE_COMMISSION, 1.1234);
    assertEquals(1.12, employee.getValue(TestDomain.EMPLOYEE_COMMISSION));
    employee.setValue(TestDomain.EMPLOYEE_COMMISSION, 1.1255);
    assertEquals(1.13, employee.getValue(TestDomain.EMPLOYEE_COMMISSION));

    final Entity detail = Entities.entity(TestDomain.T_DETAIL);
    detail.setValue(TestDomain.DETAIL_DOUBLE, 1.123456789567);
    assertEquals(1.1234567896, detail.getValue(TestDomain.DETAIL_DOUBLE));//default 10 fraction digits
  }

  private static Entity getDetailEntity(final long id, final Integer intValue, final Double doubleValue,
                                        final String stringValue, final Date dateValue, final Timestamp timestampValue,
                                        final Boolean booleanValue, final Entity entityValue) {
    final Entity entity = Entities.entity(TestDomain.T_DETAIL);
    entity.setValue(TestDomain.DETAIL_ID, id);
    entity.setValue(TestDomain.DETAIL_INT, intValue);
    entity.setValue(TestDomain.DETAIL_DOUBLE, doubleValue);
    entity.setValue(TestDomain.DETAIL_STRING, stringValue);
    entity.setValue(TestDomain.DETAIL_DATE, dateValue);
    entity.setValue(TestDomain.DETAIL_TIMESTAMP, timestampValue);
    entity.setValue(TestDomain.DETAIL_BOOLEAN, booleanValue);
    entity.setValue(TestDomain.DETAIL_ENTITY_FK, entityValue);

    return entity;
  }
}