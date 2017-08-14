/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.DateUtil;
import org.jminor.common.FileUtil;

import org.junit.Test;

import java.io.File;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
  }

  @Test
  public void serialization() throws Exception {
    final Entity referencedEntityValue = Entities.entity(TestDomain.T_MASTER);
    referencedEntityValue.put(TestDomain.MASTER_ID, 1L);
    referencedEntityValue.put(TestDomain.MASTER_NAME, "name");
    referencedEntityValue.put(TestDomain.MASTER_CODE, 10);
    final String originalStringValue = "string value";
    final Entity entity = getDetailEntity(10, 34, 23.4, originalStringValue, new Date(), new Timestamp(System.currentTimeMillis()), true, referencedEntityValue);
    entity.put(TestDomain.DETAIL_STRING, "a new String value");
    final File tmp = File.createTempFile("DefaultEntityTest", "serialization");
    FileUtil.serializeToFile(Collections.singletonList(entity), tmp);
    final List<Object> fromFile = FileUtil.deserializeFromFile(tmp);
    assertEquals(1, fromFile.size());
    final Entity entityFromFile = (Entity) fromFile.get(0);
    assertTrue(entity.is(TestDomain.T_DETAIL));
    assertTrue(entity.valuesEqual(entityFromFile));
    assertTrue(entityFromFile.isModified());
    assertTrue(entityFromFile.isModified(TestDomain.DETAIL_STRING));
    assertEquals(originalStringValue, entityFromFile.getOriginal(TestDomain.DETAIL_STRING));

    final Entity.Key key = entity.getKey();
    final File tmp2 = File.createTempFile("DefaultEntityTest", "serialization");
    FileUtil.serializeToFile(Collections.singletonList(key), tmp2);
    final List<Object> keyFromFile = FileUtil.deserializeFromFile(tmp2);
    assertEquals(1, keyFromFile.size());
    assertEquals(key, keyFromFile.get(0));
  }

  @Test
  public void setAs() {
    final Entity referencedEntityValue = Entities.entity(TestDomain.T_MASTER);
    referencedEntityValue.put(TestDomain.MASTER_ID, 2L);
    referencedEntityValue.put(TestDomain.MASTER_NAME, masterName);
    referencedEntityValue.put(TestDomain.MASTER_CODE, 7);

    final Entity test = Entities.entity(TestDomain.T_DETAIL);
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    test.setAs(testEntity);
    assertTrue("Entities should be equal after .setAs()", Objects.equals(test, testEntity));
    assertTrue("Entity property values should be equal after .setAs()", test.valuesEqual(testEntity));

    //assure that no cached foreign key values linger
    test.put(TestDomain.DETAIL_MASTER_FK, null);
    testEntity.setAs(test);
    assertNull(testEntity.get(TestDomain.DETAIL_MASTER_ID));
    assertNull(testEntity.get(TestDomain.DETAIL_MASTER_FK));
  }

  @Test
  public void saveRevertValue() {
    final Entity entity = Entities.entity(TestDomain.T_MASTER);
    final String newName = "aname";

    entity.put(TestDomain.MASTER_ID, 2L);
    entity.put(TestDomain.MASTER_NAME, masterName);
    entity.put(TestDomain.MASTER_CODE, 7);

    entity.put(TestDomain.MASTER_ID, -55L);
    //the id is not updatable as it is part of the primary key, which is not updatable by default
    assertFalse(entity.getModifiedObserver().isActive());
    entity.save(TestDomain.MASTER_ID);
    assertFalse(entity.getModifiedObserver().isActive());

    entity.put(TestDomain.MASTER_NAME, newName);
    assertTrue(entity.getModifiedObserver().isActive());
    entity.revert(TestDomain.MASTER_NAME);
    assertEquals(masterName, entity.get(TestDomain.MASTER_NAME));
    assertFalse(entity.getModifiedObserver().isActive());

    entity.put(TestDomain.MASTER_NAME, newName);
    assertTrue(entity.isModified());
    assertTrue(entity.isModified(TestDomain.MASTER_NAME));
    entity.save(TestDomain.MASTER_NAME);
    assertEquals(newName, entity.get(TestDomain.MASTER_NAME));
    assertFalse(entity.isModified());
    assertFalse(entity.isModified(TestDomain.MASTER_NAME));
  }

  @Test
  public void compositeReferenceKey() {
    final Entity master = Entities.entity(TestDomain.T_COMPOSITE_MASTER);
    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    master.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);

    final Entity detail = Entities.entity(TestDomain.T_COMPOSITE_DETAIL);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_FK, master);

    final Property.ForeignKeyProperty foreignKeyProperty = Entities.getForeignKeyProperty(TestDomain.T_COMPOSITE_DETAIL, TestDomain.COMPOSITE_DETAIL_MASTER_FK);
    assertEquals(master.getKey(), detail.getReferencedKey(foreignKeyProperty));

    master.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    master.put(TestDomain.COMPOSITE_MASTER_ID_2, null);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_FK, master);

    assertNull(detail.getReferencedKey(foreignKeyProperty));
  }

  @Test
  public void compositeKeyNull() {
    final Entity master = Entities.entity(TestDomain.T_COMPOSITE_MASTER);
    assertTrue(master.getKey().isNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    assertFalse(master.getKey().isNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    assertFalse(master.getKey().isNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, 2);
    master.put(TestDomain.COMPOSITE_MASTER_ID_2, null);
    assertTrue(master.getKey().isNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    assertTrue(master.getKey().isNull());
  }

  @Test(expected = IllegalArgumentException.class)
  public void compositeKeySingleValueConstructor() {
    new DefaultEntity.DefaultKey(DefaultEntityDefinition.getDefinitionMap().get(TestDomain.T_COMPOSITE_MASTER), 1);
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

    referencedEntityValue.put(TestDomain.MASTER_ID, 2L);
    referencedEntityValue.put(TestDomain.MASTER_NAME, masterName);
    referencedEntityValue.put(TestDomain.MASTER_CODE, 7);

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
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_FK).getType(), Types.REF);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_ID).getType(), Types.BIGINT);
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
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_ID).getPropertyID(), TestDomain.DETAIL_MASTER_ID);
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
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_FK).getCaption(), TestDomain.DETAIL_MASTER_FK);
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
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_MASTER_FK).isHidden());
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_MASTER_NAME).isHidden());
    assertFalse(testEntity.getProperty(TestDomain.DETAIL_MASTER_CODE).isHidden());

    //assert values
    assertEquals(testEntity.get(TestDomain.DETAIL_ID), detailId);
    assertEquals(testEntity.get(TestDomain.DETAIL_INT), detailInt);
    assertEquals(testEntity.get(TestDomain.DETAIL_DOUBLE), detailDouble);
    assertEquals(testEntity.get(TestDomain.DETAIL_STRING), detailString);
    assertEquals(testEntity.get(TestDomain.DETAIL_DATE), detailDate);
    assertEquals(testEntity.get(TestDomain.DETAIL_TIMESTAMP), detailTimestamp);
    assertEquals(testEntity.get(TestDomain.DETAIL_BOOLEAN), detailBoolean);
    assertEquals(testEntity.get(TestDomain.DETAIL_MASTER_FK), referencedEntityValue);
    assertEquals(testEntity.get(TestDomain.DETAIL_MASTER_NAME), masterName);
    assertEquals(testEntity.get(TestDomain.DETAIL_MASTER_CODE), 7);
    assertFalse(testEntity.isValueNull(TestDomain.DETAIL_MASTER_ID));

    testEntity.getReferencedKey(Entities.getForeignKeyProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK));

    //test copy()
    final Entity test2 = (Entity) testEntity.getCopy();
    assertFalse("Entity copy should not be == the original", test2 == testEntity);
    assertTrue("Entities should be equal after .getCopy()", Objects.equals(test2, testEntity));
    assertTrue("Entity property values should be equal after .getCopy()", test2.valuesEqual(testEntity));
    assertFalse("This should be a deep copy",
            testEntity.getForeignKey(TestDomain.DETAIL_MASTER_FK) == test2.getForeignKey(TestDomain.DETAIL_MASTER_FK));

    test2.put(TestDomain.DETAIL_DOUBLE, 2.1);
    assertTrue(test2.isModified());
    assertTrue(test2.getCopy().isModified());

    //test propagate entity reference/denormalized values
    testEntity.put(TestDomain.DETAIL_MASTER_FK, null);
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_MASTER_ID));
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_MASTER_NAME));
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_MASTER_CODE));

    testEntity.put(TestDomain.DETAIL_MASTER_FK, referencedEntityValue);
    assertFalse(testEntity.isValueNull(TestDomain.DETAIL_MASTER_ID));
    assertEquals(testEntity.get(TestDomain.DETAIL_MASTER_ID),
            referencedEntityValue.get(TestDomain.MASTER_ID));
    assertEquals(testEntity.get(TestDomain.DETAIL_MASTER_NAME),
            referencedEntityValue.get(TestDomain.MASTER_NAME));
    assertEquals(testEntity.get(TestDomain.DETAIL_MASTER_CODE),
            referencedEntityValue.get(TestDomain.MASTER_CODE));

    referencedEntityValue.put(TestDomain.MASTER_CODE, 20);
    testEntity.put(TestDomain.DETAIL_MASTER_FK, referencedEntityValue);
    assertEquals(testEntity.get(TestDomain.DETAIL_MASTER_CODE),
            referencedEntityValue.get(TestDomain.MASTER_CODE));
  }

  @Test
  public void isValueNull() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_MASTER_ID));
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_MASTER_FK));
    assertTrue(testEntity.isForeignKeyNull(Entities.getForeignKeyProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK)));
    testEntity.put(TestDomain.DETAIL_MASTER_ID, 10L);

    assertFalse(testEntity.isLoaded(TestDomain.DETAIL_MASTER_FK));
    final Entity referencedEntityValue = testEntity.getForeignKey(TestDomain.DETAIL_MASTER_FK);
    assertEquals(10L, referencedEntityValue.get(TestDomain.MASTER_ID));
    assertFalse(testEntity.isLoaded(TestDomain.DETAIL_MASTER_FK));
    assertFalse(testEntity.isValueNull(TestDomain.DETAIL_MASTER_FK));
    assertFalse(testEntity.isValueNull(TestDomain.DETAIL_MASTER_ID));
  }

  @Test
  public void clear() {
    final Entity referencedEntityValue = Entities.entity(TestDomain.T_MASTER);
    Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    testEntity.put(TestDomain.DETAIL_STRING, "TestString");
    assertTrue(testEntity.isModified());

    testEntity.clear();
    assertTrue(testEntity.getKey().isNull());
    assertTrue(testEntity.isKeyNull());
    assertFalse(testEntity.containsKey(TestDomain.DETAIL_DATE));
    assertFalse(testEntity.containsKey(TestDomain.DETAIL_STRING));
    assertFalse(testEntity.containsKey(TestDomain.DETAIL_BOOLEAN));
    assertFalse(testEntity.isModified());

    testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);

    testEntity.clearKeyValues();
    assertTrue(testEntity.getKey().isNull());
    assertTrue(testEntity.isKeyNull());
    assertTrue(testEntity.containsKey(TestDomain.DETAIL_DATE));
    assertTrue(testEntity.containsKey(TestDomain.DETAIL_STRING));
    assertTrue(testEntity.containsKey(TestDomain.DETAIL_BOOLEAN));
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueInt() {
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_NAME, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueDouble() {
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_NAME, 1d);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueBoolean() {
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_NAME, false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueChar() {
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_NAME, 'c');
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueEntity() {
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);

    employee.put(TestDomain.EMP_NAME, department);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueDate() {
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_NAME, new Date());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setStringValueTimestamp() {
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_NAME, new Timestamp(System.currentTimeMillis()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void setDoubleValueString() {
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_SALARY, "test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setDenormalizedViewValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    testEntity.put(TestDomain.DETAIL_MASTER_NAME, "hello");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setDenormalizedValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    testEntity.put(TestDomain.DETAIL_MASTER_CODE, 2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setIntegerKeyString() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    testEntity.put(TestDomain.DETAIL_ID, "hello");
  }

  @Test
  public void setValue() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);

    final Entity employee = Entities.entity(TestDomain.T_EMP);

    employee.put(TestDomain.EMP_COMMISSION, 1200d);
    assertEquals(employee.get(TestDomain.EMP_COMMISSION), 1200d);

    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    assertEquals(employee.get(TestDomain.EMP_DEPARTMENT_FK), department);

    final Timestamp date = new Timestamp(DateUtil.floorDate(new Date()).getTime());
    employee.put(TestDomain.EMP_HIREDATE, date);
    assertEquals(employee.get(TestDomain.EMP_HIREDATE), date);

    employee.put(TestDomain.EMP_ID, 123);
    assertEquals(employee.get(TestDomain.EMP_ID), 123);

    employee.put(TestDomain.EMP_NAME, "noname");
    assertEquals(employee.get(TestDomain.EMP_NAME), "noname");

    employee.addValueListener(info -> {
      if (info.getKey().equals(TestDomain.EMP_DEPARTMENT_FK)) {
        assertTrue(employee.isValueNull(TestDomain.EMP_DEPARTMENT_FK));
        assertTrue(employee.isValueNull(TestDomain.EMP_DEPARTMENT));
      }
    });
    employee.put(TestDomain.EMP_DEPARTMENT_FK, null);
  }

  @Test
  public void propertyValuesEqual() {
    final Entity testEntityOne = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    final Entity testEntityTwo = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);

    assertTrue(testEntityOne.valuesEqual(testEntityTwo));

    testEntityTwo.put(TestDomain.DETAIL_INT, 42);

    assertFalse(testEntityOne.valuesEqual(testEntityTwo));
  }

  @Test
  public void getDoubleValue() {
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, -10);

    assertNull(employee.getDouble(TestDomain.EMP_SALARY));

    final double salary = 1000.1234;
    employee.put(TestDomain.EMP_SALARY, salary);
    assertEquals(Double.valueOf(1000.12), employee.getDouble(TestDomain.EMP_SALARY));
  }

  @Test
  public void getForeignKeyValue() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, -10);
    assertTrue(employee.isForeignKeyNull(Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK)));
    assertNull(employee.get(TestDomain.EMP_DEPARTMENT_FK));
    assertNull(employee.get(TestDomain.EMP_DEPARTMENT));

    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    assertFalse(employee.isForeignKeyNull(Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK)));
    assertNotNull(employee.get(TestDomain.EMP_DEPARTMENT_FK));
    assertNotNull(employee.get(TestDomain.EMP_DEPARTMENT));
  }

  @Test (expected = IllegalArgumentException.class)
  public void getForeignKeyValueNonFKProperty() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, -10);
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);

    employee.getForeignKey(TestDomain.EMP_COMMISSION);
  }

  @Test
  public void removeValue() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, -10);
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    assertNotNull(employee.getForeignKey(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals(Integer.valueOf(-10), employee.getInteger(TestDomain.EMP_DEPARTMENT));

    employee.remove(TestDomain.EMP_DEPARTMENT_FK);
    assertNull(employee.getForeignKey(TestDomain.EMP_DEPARTMENT_FK));
    assertNull(employee.get(TestDomain.EMP_DEPARTMENT));
    assertFalse(employee.containsKey(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(employee.containsKey(Entities.getProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT)));
  }

  @Test
  public void maximumFractionDigits() {
    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_COMMISSION, 1.1234);
    assertEquals(1.12, employee.get(TestDomain.EMP_COMMISSION));
    employee.put(TestDomain.EMP_COMMISSION, 1.1255);
    assertEquals(1.13, employee.get(TestDomain.EMP_COMMISSION));

    final Entity detail = Entities.entity(TestDomain.T_DETAIL);
    detail.put(TestDomain.DETAIL_DOUBLE, 1.123456789567);
    assertEquals(1.1234567896, detail.get(TestDomain.DETAIL_DOUBLE));//default 10 fraction digits
  }

  @Test
  public void keyEquality() {
    final Entity.Key empKey1 = Entities.key(TestDomain.T_EMP);
    empKey1.put(TestDomain.EMP_ID, 1);
    final Entity.Key empKey2 = Entities.key(TestDomain.T_EMP);
    empKey2.put(TestDomain.EMP_ID, 2);
    assertFalse(empKey1.equals(empKey2));

    empKey2.put(TestDomain.EMP_ID, 1);
    assertTrue(empKey1.equals(empKey2));

    final Entity.Key deptKey = Entities.key(TestDomain.T_DEPARTMENT);
    deptKey.put(TestDomain.DEPARTMENT_ID, 1);
    assertFalse(empKey1.equals(deptKey));

    final Entity.Key compMasterKey = Entities.key(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    //noinspection EqualsWithItself
    assertTrue(compMasterKey.equals(compMasterKey));
    assertFalse(empKey1.equals(compMasterKey));
    assertFalse(compMasterKey.equals(new Object()));

    final Entity.Key compMasterKey2 = Entities.key(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    assertFalse(compMasterKey.equals(compMasterKey2));

    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    assertTrue(compMasterKey.equals(compMasterKey2));

    final Entity.Key detailKey = Entities.key(TestDomain.T_DETAIL);
    detailKey.put(TestDomain.DETAIL_ID, 1L);
    final Entity.Key detailKey2 = Entities.key(TestDomain.T_DETAIL);
    detailKey2.put(TestDomain.DETAIL_ID, 2L);
    assertFalse(detailKey.equals(detailKey2));

    detailKey2.put(TestDomain.DETAIL_ID, 1L);
    assertTrue(detailKey2.equals(detailKey));
  }

  @Test
  public void nullKeyEquals() {
    final Entity.Key nullKey = Entities.key(TestDomain.T_EMP);
    final Entity.Key zeroKey = Entities.key(TestDomain.T_EMP);
    zeroKey.put(TestDomain.EMP_ID, 0);
    assertFalse(nullKey.equals(zeroKey));
  }

  private static Entity getDetailEntity(final long id, final Integer intValue, final Double doubleValue,
                                        final String stringValue, final Date dateValue, final Timestamp timestampValue,
                                        final Boolean booleanValue, final Entity entityValue) {
    final Entity entity = Entities.entity(TestDomain.T_DETAIL);
    entity.put(TestDomain.DETAIL_ID, id);
    entity.put(TestDomain.DETAIL_INT, intValue);
    entity.put(TestDomain.DETAIL_DOUBLE, doubleValue);
    entity.put(TestDomain.DETAIL_STRING, stringValue);
    entity.put(TestDomain.DETAIL_DATE, dateValue);
    entity.put(TestDomain.DETAIL_TIMESTAMP, timestampValue);
    entity.put(TestDomain.DETAIL_BOOLEAN, booleanValue);
    entity.put(TestDomain.DETAIL_MASTER_FK, entityValue);

    return entity;
  }
}