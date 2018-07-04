/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.DateUtil;
import org.jminor.common.FileUtil;
import org.jminor.common.Util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityTest {

  private final long detailId = 1;
  private final int detailInt = 2;
  private final double detailDouble = 1.2;
  private final String detailString = "string";
  private final Date detailDate = new Date();
  private final Timestamp detailTimestamp = new Timestamp(new Date().getTime());
  private final Boolean detailBoolean = true;

  private final String masterName = "master";

  private static final Entities ENTITIES = new TestDomain();

  @Test
  public void serialization() throws Exception {
    final Entity referencedEntityValue = ENTITIES.entity(TestDomain.T_MASTER);
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
    final Entity referencedEntityValue = ENTITIES.entity(TestDomain.T_MASTER);
    referencedEntityValue.put(TestDomain.MASTER_ID, 2L);
    referencedEntityValue.put(TestDomain.MASTER_NAME, masterName);
    referencedEntityValue.put(TestDomain.MASTER_CODE, 7);

    final Entity test = ENTITIES.entity(TestDomain.T_DETAIL);
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    test.setAs(testEntity);
    assertTrue(Objects.equals(test, testEntity), "Entities should be equal after .setAs()");
    assertTrue(test.valuesEqual(testEntity), "Entity property values should be equal after .setAs()");

    //assure that no cached foreign key values linger
    test.put(TestDomain.DETAIL_MASTER_FK, null);
    testEntity.setAs(test);
    assertNull(testEntity.get(TestDomain.DETAIL_MASTER_ID));
    assertNull(testEntity.get(TestDomain.DETAIL_MASTER_FK));
  }

  @Test
  public void saveRevertValue() {
    final Entity entity = ENTITIES.entity(TestDomain.T_MASTER);
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
  public void getReferencedKeyCache() {
    final Entity detail = ENTITIES.entity(TestDomain.T_COMPOSITE_DETAIL);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID, 1);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_2, 2);

    final Property.ForeignKeyProperty foreignKeyProperty = ENTITIES.getForeignKeyProperty(TestDomain.T_COMPOSITE_DETAIL,
            TestDomain.COMPOSITE_DETAIL_MASTER_FK);
    final Entity.Key referencedKey = detail.getReferencedKey(foreignKeyProperty);
    final Entity.Key cachedKey = detail.getReferencedKey(foreignKeyProperty);

    assertSame(cachedKey, referencedKey);
  }

  @Test
  public void compositeReferenceKey() {
    final Entity master = ENTITIES.entity(TestDomain.T_COMPOSITE_MASTER);
    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    master.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);

    final Entity detail = ENTITIES.entity(TestDomain.T_COMPOSITE_DETAIL);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_FK, master);

    final Property.ForeignKeyProperty foreignKeyProperty = ENTITIES.getForeignKeyProperty(TestDomain.T_COMPOSITE_DETAIL, TestDomain.COMPOSITE_DETAIL_MASTER_FK);
    assertEquals(master.getKey(), detail.getReferencedKey(foreignKeyProperty));

    master.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    master.put(TestDomain.COMPOSITE_MASTER_ID_2, null);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_FK, master);

    assertNull(detail.getReferencedKey(foreignKeyProperty));
  }

  @Test
  public void compositeKeyNull() {
    final Entity master = ENTITIES.entity(TestDomain.T_COMPOSITE_MASTER);
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

  @Test
  public void singleKeyNull() {
    final Entity.Key key = ENTITIES.key(TestDomain.T_DETAIL);
    assertTrue(key.isNull());
    key.put(TestDomain.DETAIL_ID, null);
    assertTrue(key.isNull());
    key.put(TestDomain.DETAIL_ID, 1L);
    assertFalse(key.isNull());
  }

  @Test
  public void compositeKeySingleValueConstructor() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity.DefaultKey(ENTITIES.getDefinition(TestDomain.T_COMPOSITE_MASTER), 1));
  }

  @Test
  public void getPropertyWrongEntityType() {
    final Entity testEntity = ENTITIES.entity(TestDomain.T_DETAIL);
    assertThrows(IllegalArgumentException.class, () -> testEntity.getProperty(TestDomain.MASTER_CODE));
  }

  @Test
  public void entity() throws Exception {
    final Entity referencedEntityValue = ENTITIES.entity(TestDomain.T_MASTER);
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
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_ID).getPropertyId(), TestDomain.DETAIL_ID);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_INT).getPropertyId(), TestDomain.DETAIL_INT);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_DOUBLE).getPropertyId(), TestDomain.DETAIL_DOUBLE);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_STRING).getPropertyId(), TestDomain.DETAIL_STRING);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_DATE).getPropertyId(), TestDomain.DETAIL_DATE);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_TIMESTAMP).getPropertyId(), TestDomain.DETAIL_TIMESTAMP);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_BOOLEAN).getPropertyId(), TestDomain.DETAIL_BOOLEAN);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_ID).getPropertyId(), TestDomain.DETAIL_MASTER_ID);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_NAME).getPropertyId(), TestDomain.DETAIL_MASTER_NAME);
    assertEquals(testEntity.getProperty(TestDomain.DETAIL_MASTER_CODE).getPropertyId(), TestDomain.DETAIL_MASTER_CODE);

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

    testEntity.getReferencedKey(ENTITIES.getForeignKeyProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK));

    //test copy()
    final Entity test2 = (Entity) testEntity.getCopy();
    assertNotSame(test2, testEntity, "Entity copy should not be == the original");
    assertTrue(Objects.equals(test2, testEntity), "Entities should be equal after .getCopy()");
    assertTrue(test2.valuesEqual(testEntity), "Entity property values should be equal after .getCopy()");
    assertNotSame(testEntity.getForeignKey(TestDomain.DETAIL_MASTER_FK), test2.getForeignKey(TestDomain.DETAIL_MASTER_FK), "This should be a deep copy");

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
  public void getReferencedKeyIncorrectFK() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.getReferencedKey(ENTITIES.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK)));
  }

  @Test
  public void isValueNull() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_MASTER_ID));
    assertTrue(testEntity.isValueNull(TestDomain.DETAIL_MASTER_FK));
    assertTrue(testEntity.isForeignKeyNull(ENTITIES.getForeignKeyProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK)));
    testEntity.put(TestDomain.DETAIL_MASTER_ID, 10L);

    assertFalse(testEntity.isLoaded(TestDomain.DETAIL_MASTER_FK));
    final Entity referencedEntityValue = testEntity.getForeignKey(TestDomain.DETAIL_MASTER_FK);
    assertEquals(10L, referencedEntityValue.get(TestDomain.MASTER_ID));
    assertFalse(testEntity.isLoaded(TestDomain.DETAIL_MASTER_FK));
    assertFalse(testEntity.isValueNull(TestDomain.DETAIL_MASTER_FK));
    assertFalse(testEntity.isValueNull(TestDomain.DETAIL_MASTER_ID));

    final Property.ForeignKeyProperty foreignKeyProperty = ENTITIES.getForeignKeyProperty(TestDomain.T_COMPOSITE_DETAIL, TestDomain.COMPOSITE_DETAIL_MASTER_FK);
    final Entity composite = ENTITIES.entity(TestDomain.T_COMPOSITE_DETAIL);
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID, null);
    assertTrue(composite.isForeignKeyNull(foreignKeyProperty));
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID, 1);
    assertTrue(composite.isForeignKeyNull(foreignKeyProperty));
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_2, null);
    assertTrue(composite.isForeignKeyNull(foreignKeyProperty));
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_2, 1);
    assertFalse(composite.isForeignKeyNull(foreignKeyProperty));
  }

  @Test
  public void clear() {
    final Entity referencedEntityValue = ENTITIES.entity(TestDomain.T_MASTER);
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

  @Test
  public void setStringValueInt() {
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () -> employee.put(TestDomain.EMP_NAME, 1));
  }

  @Test
  public void setStringValueDouble() {
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () -> employee.put(TestDomain.EMP_NAME, 1d));
  }

  @Test
  public void setStringValueBoolean() {
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () -> employee.put(TestDomain.EMP_NAME, false));
  }

  @Test
  public void setStringValueChar() {
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () -> employee.put(TestDomain.EMP_NAME, 'c'));
  }

  @Test
  public void setStringValueEntity() {
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);

    assertThrows(IllegalArgumentException.class, () -> employee.put(TestDomain.EMP_NAME, department));
  }

  @Test
  public void setStringValueDate() {
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () -> employee.put(TestDomain.EMP_NAME, new Date()));
  }

  @Test
  public void setStringValueTimestamp() {
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () -> employee.put(TestDomain.EMP_NAME, new Timestamp(System.currentTimeMillis())));
  }

  @Test
  public void setDoubleValueString() {
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () -> employee.put(TestDomain.EMP_SALARY, "test"));
  }

  @Test
  public void setDenormalizedViewValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.put(TestDomain.DETAIL_MASTER_NAME, "hello"));
  }

  @Test
  public void setDenormalizedValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.put(TestDomain.DETAIL_MASTER_CODE, 2));
  }

  @Test
  public void setIntegerKeyString() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.put(TestDomain.DETAIL_ID, "hello"));
  }

  @Test
  public void setValue() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);

    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);

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
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, -10);

    assertNull(employee.getDouble(TestDomain.EMP_SALARY));

    final double salary = 1000.1234;
    employee.put(TestDomain.EMP_SALARY, salary);
    assertEquals(Double.valueOf(1000.12), employee.getDouble(TestDomain.EMP_SALARY));
  }

  @Test
  public void getForeignKeyValue() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, -10);
    assertTrue(employee.isForeignKeyNull(ENTITIES.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK)));
    assertNull(employee.get(TestDomain.EMP_DEPARTMENT_FK));
    assertNull(employee.get(TestDomain.EMP_DEPARTMENT));

    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    assertFalse(employee.isForeignKeyNull(ENTITIES.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK)));
    assertNotNull(employee.get(TestDomain.EMP_DEPARTMENT_FK));
    assertNotNull(employee.get(TestDomain.EMP_DEPARTMENT));
  }

  @Test
  public void getDerivedValue() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_NAME, "dname");
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_NAME, "ename");
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    assertEquals("ename - dname", employee.getString(TestDomain.EMP_NAME_DEPARTMENT));
  }

  @Test
  public void getForeignKeyValueNonFKProperty() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, -10);
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);

    assertThrows(IllegalArgumentException.class, () -> employee.getForeignKey(TestDomain.EMP_COMMISSION));
  }

  @Test
  public void removeValue() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, -10);
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    assertNotNull(employee.getForeignKey(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals(Integer.valueOf(-10), employee.getInteger(TestDomain.EMP_DEPARTMENT));

    employee.remove(TestDomain.EMP_DEPARTMENT_FK);
    assertNull(employee.getForeignKey(TestDomain.EMP_DEPARTMENT_FK));
    assertNull(employee.get(TestDomain.EMP_DEPARTMENT));
    assertFalse(employee.containsKey(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(employee.containsKey(ENTITIES.getProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT)));
  }

  @Test
  public void maximumFractionDigits() {
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_COMMISSION, 1.1234);
    assertEquals(1.12, employee.get(TestDomain.EMP_COMMISSION));
    employee.put(TestDomain.EMP_COMMISSION, 1.1255);
    assertEquals(1.13, employee.get(TestDomain.EMP_COMMISSION));

    final Entity detail = ENTITIES.entity(TestDomain.T_DETAIL);
    detail.put(TestDomain.DETAIL_DOUBLE, 1.123456789567);
    assertEquals(1.1234567896, detail.get(TestDomain.DETAIL_DOUBLE));//default 10 fraction digits
  }

  @Test
  public void keyInvalidPropertyGet() {
    final Entity.Key empKey1 = ENTITIES.key(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () -> empKey1.get(TestDomain.EMP_NAME));
  }

  @Test
  public void keyInvalidPropertyPut() {
    final Entity.Key empKey1 = ENTITIES.key(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () -> empKey1.put(TestDomain.EMP_NAME, "test"));
  }

  @Test
  public void keyGetCopy() {
    final Entity.Key empKey1 = ENTITIES.key(TestDomain.T_EMP);
    empKey1.put(TestDomain.EMP_ID, 1);
    final Entity.Key copy = (Entity.Key) empKey1.getCopy();
    assertEquals(empKey1, copy);

    empKey1.put(TestDomain.EMP_ID, 2);
    final Entity.Key originalCopy = (Entity.Key) empKey1.getOriginalCopy();
    final Entity.Key originalCreated = ENTITIES.key(TestDomain.T_EMP);
    originalCreated.put(TestDomain.EMP_ID, 1);
    assertEquals(originalCopy, originalCreated);
  }

  @Test
  public void keyEquality() {
    final Entity.Key empKey1 = ENTITIES.key(TestDomain.T_EMP);
    empKey1.put(TestDomain.EMP_ID, 1);
    final Entity.Key empKey2 = ENTITIES.key(TestDomain.T_EMP);
    empKey2.put(TestDomain.EMP_ID, 2);
    assertNotEquals(empKey1, empKey2);

    empKey2.put(TestDomain.EMP_ID, 1);
    assertEquals(empKey1, empKey2);

    final Entity.Key deptKey = ENTITIES.key(TestDomain.T_DEPARTMENT);
    deptKey.put(TestDomain.DEPARTMENT_ID, 1);
    assertNotEquals(empKey1, deptKey);

    final Entity.Key compMasterKey = ENTITIES.key(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    //noinspection EqualsWithItself
    assertEquals(compMasterKey, compMasterKey);
    assertNotEquals(empKey1, compMasterKey);
    assertNotEquals(compMasterKey, new Object());

    final Entity.Key compMasterKey2 = ENTITIES.key(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    assertNotEquals(compMasterKey, compMasterKey2);

    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    assertEquals(compMasterKey, compMasterKey2);

    final Entity.Key detailKey = ENTITIES.key(TestDomain.T_DETAIL);
    detailKey.put(TestDomain.DETAIL_ID, 1L);
    final Entity.Key detailKey2 = ENTITIES.key(TestDomain.T_DETAIL);
    detailKey2.put(TestDomain.DETAIL_ID, 2L);
    assertNotEquals(detailKey, detailKey2);

    detailKey2.put(TestDomain.DETAIL_ID, 1L);
    assertEquals(detailKey2, detailKey);
  }

  @Test
  public void nullKeyEquals() {
    final Entity.Key nullKey = ENTITIES.key(TestDomain.T_EMP);
    final Entity.Key zeroKey = ENTITIES.key(TestDomain.T_EMP);
    zeroKey.put(TestDomain.EMP_ID, 0);
    assertNotEquals(nullKey, zeroKey);
  }

  @Test
  public void transientPropertyModifiesEntity() throws IOException, ClassNotFoundException {
    final Entities domain = new Entities("transient").registerDomain();
    final Property.TransientProperty transientProperty = Properties.transientProperty("trans", Types.INTEGER);
    domain.define("entityId",
            Properties.primaryKeyProperty("id"),
            transientProperty);

    final Entity entity = domain.entity("entityId");
    entity.put("id", 42);
    entity.put("trans", null);
    entity.saveAll();

    entity.put("trans", 1);
    assertTrue(entity.isModified());

    transientProperty.setModifiesEntity(false);
    assertFalse(entity.isModified());

    final Entity deserialized = Util.deserialize(Util.serialize(entity));
    assertTrue(deserialized.isModified("trans"));
  }

  private Entity getDetailEntity(final long id, final Integer intValue, final Double doubleValue,
                                 final String stringValue, final Date dateValue, final Timestamp timestampValue,
                                 final Boolean booleanValue, final Entity entityValue) {
    final Entity entity = ENTITIES.entity(TestDomain.T_DETAIL);
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