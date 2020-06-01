/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.FileUtil;
import is.codion.common.Serializer;
import is.codion.common.event.EventDataListener;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.TransientProperty;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.framework.domain.entity.Entities.entityIdentity;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityTest {

  private final long detailId = 1;
  private final int detailInt = 2;
  private final double detailDouble = 1.2;
  private final String detailString = "string";
  private final LocalDate detailDate = LocalDate.now();
  private final LocalDateTime detailTimestamp = LocalDateTime.now();
  private final Boolean detailBoolean = true;

  private final String masterName = "master";

  private static final Entities ENTITIES = new TestDomain().registerEntities();

  @Test
  public void construction() {
    final EntityDefinition masterDefinition = ENTITIES.getDefinition(TestDomain.T_MASTER);

    final Map<Attribute<?>, Object> values = new HashMap<>();
    values.put(TestDomain.DETAIL_BOOLEAN, false);
    values.put(TestDomain.MASTER_CODE, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, values, null));

    final Map<Attribute<?>, Object> originalValues = new HashMap<>();
    originalValues.put(TestDomain.DETAIL_BOOLEAN, false);
    originalValues.put(TestDomain.MASTER_CODE, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, null, originalValues));

    final Map<Attribute<?>, Object> invalidTypeValues = new HashMap<>();
    invalidTypeValues.put(TestDomain.MASTER_CODE, false);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, invalidTypeValues, null));

    final Map<Attribute<?>, Object> invalidTypeOriginalValues = new HashMap<>();
    invalidTypeOriginalValues.put(TestDomain.MASTER_CODE, false);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, null, invalidTypeOriginalValues));

    final Entity.Identity entityId = entityIdentity("entityId");
    final Attribute<?> invalid = entityId.integerAttribute("invalid");
    final Map<Attribute<?>, Object> invalidPropertyValues = new HashMap<>();
    invalidPropertyValues.put(invalid, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, invalidPropertyValues, null));
  }

  @Test
  public void serialization() throws Exception {
    final Entity referencedEntityValue = ENTITIES.entity(TestDomain.T_MASTER);
    referencedEntityValue.put(TestDomain.MASTER_ID, 1L);
    referencedEntityValue.put(TestDomain.MASTER_NAME, "name");
    referencedEntityValue.put(TestDomain.MASTER_CODE, 10);
    final String originalStringValue = "string value";
    final Entity entity = getDetailEntity(10, 34, 23.4, originalStringValue, LocalDate.now(),
            LocalDateTime.now(), true, referencedEntityValue);
    entity.put(TestDomain.DETAIL_STRING, "a new String value");
    final File tmp = File.createTempFile("DefaultEntityTest", "serialization");
    FileUtil.serializeToFile(singletonList(entity), tmp);
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
    FileUtil.serializeToFile(singletonList(key), tmp2);
    final List<Object> keyFromFile = FileUtil.deserializeFromFile(tmp2);
    assertEquals(1, keyFromFile.size());
    assertEquals(key, keyFromFile.get(0));

    final Entity master = ENTITIES.entity(TestDomain.T_MASTER);
    master.put(TestDomain.MASTER_ID, 1L);
    master.put(TestDomain.MASTER_CODE, 11);

    final Entity masterDeserialized = Serializer.deserialize(Serializer.serialize(master));
    assertEquals(master.get(TestDomain.MASTER_ID), masterDeserialized.get(TestDomain.MASTER_ID));
    assertEquals(master.get(TestDomain.MASTER_CODE), masterDeserialized.get(TestDomain.MASTER_CODE));
    assertFalse(masterDeserialized.containsKey(TestDomain.MASTER_NAME));
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
    assertEquals(test, testEntity, "Entities should be equal after .setAs()");
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
    assertFalse(entity.isModified());
    entity.save(TestDomain.MASTER_ID);
    assertFalse(entity.isModified());

    entity.put(TestDomain.MASTER_NAME, newName);
    assertTrue(entity.isModified());
    entity.revert(TestDomain.MASTER_NAME);
    assertEquals(masterName, entity.get(TestDomain.MASTER_NAME));
    assertFalse(entity.isModified());

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

    final Entity.Key referencedKey = detail.getReferencedKey(TestDomain.COMPOSITE_DETAIL_MASTER_FK);
    final Entity.Key cachedKey = detail.getReferencedKey(TestDomain.COMPOSITE_DETAIL_MASTER_FK);

    assertSame(cachedKey, referencedKey);
  }

  @Test
  public void compositeReferenceKey() {
    final Entity master = ENTITIES.entity(TestDomain.T_COMPOSITE_MASTER);
    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    master.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);

    final Entity detail = ENTITIES.entity(TestDomain.T_COMPOSITE_DETAIL);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_FK, master);

    assertEquals(master.getKey(), detail.getReferencedKey(TestDomain.COMPOSITE_DETAIL_MASTER_FK));

    //otherwise the values are equal and put() returns before propagating foreign key values
    final Entity masterCopy = ENTITIES.deepCopyEntity(master);
    masterCopy.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    masterCopy.put(TestDomain.COMPOSITE_MASTER_ID_2, null);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_FK, masterCopy);

    assertNull(detail.getReferencedKey(TestDomain.COMPOSITE_DETAIL_MASTER_FK));
  }

  @Test
  public void compositeKeyNull() {
    final Entity master = ENTITIES.entity(TestDomain.T_COMPOSITE_MASTER);
    assertTrue(master.getKey().isNull());
    assertFalse(master.getKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    assertFalse(master.getKey().isNull());
    assertTrue(master.getKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    assertFalse(master.getKey().isNull());
    assertTrue(master.getKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, 2);
    master.put(TestDomain.COMPOSITE_MASTER_ID_2, null);
    assertTrue(master.getKey().isNull());
    assertFalse(master.getKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    assertTrue(master.getKey().isNull());
    assertFalse(master.getKey().isNotNull());
  }

  @Test
  public void singleKeyNull() {
    final Entity.Key key = ENTITIES.key(TestDomain.T_DETAIL);
    assertTrue(key.isNull());
    key.put(TestDomain.DETAIL_ID, null);
    assertTrue(key.isNull());
    key.put(TestDomain.DETAIL_ID, 1L);
    assertTrue(key.isNotNull());
  }

  @Test
  public void compositeKeySingleValueConstructor() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntityKey(ENTITIES.getDefinition(TestDomain.T_COMPOSITE_MASTER), 1));
  }

  @Test
  public void noPrimaryKey() {
    final Entity noPk = ENTITIES.entity(TestDomain.T_NO_PK);
    noPk.put(TestDomain.NO_PK_COL1, 1);
    noPk.put(TestDomain.NO_PK_COL2, 2);
    noPk.put(TestDomain.NO_PK_COL3, 3);
    final Entity.Key key = noPk.getKey();
    assertEquals(0, key.size());
    final Entity.Key originalKey = noPk.getOriginalKey();
    assertEquals(0, originalKey.size());
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
    assertFalse(testEntity.isNull(TestDomain.DETAIL_MASTER_ID));
    assertTrue(testEntity.isNotNull(TestDomain.DETAIL_MASTER_ID));

    testEntity.getReferencedKey(TestDomain.DETAIL_MASTER_FK);

    //test copy()
    final Entity test2 = ENTITIES.deepCopyEntity(testEntity);
    assertNotSame(test2, testEntity, "Entity copy should not be == the original");
    assertEquals(test2, testEntity, "Entities should be equal after .getCopy()");
    assertTrue(test2.valuesEqual(testEntity), "Entity property values should be equal after .getCopy()");
    assertNotSame(testEntity.getForeignKey(TestDomain.DETAIL_MASTER_FK), test2.getForeignKey(TestDomain.DETAIL_MASTER_FK), "This should be a deep copy");

    test2.put(TestDomain.DETAIL_DOUBLE, 2.1);
    assertTrue(test2.isModified());
    final Entity test2Copy = ENTITIES.copyEntity(test2);
    assertTrue(test2Copy.isModified());

    //test propagate entity reference/denormalized values
    testEntity.put(TestDomain.DETAIL_MASTER_FK, null);
    assertTrue(testEntity.isNull(TestDomain.DETAIL_MASTER_ID));
    assertTrue(testEntity.isNull(TestDomain.DETAIL_MASTER_NAME));
    assertTrue(testEntity.isNull(TestDomain.DETAIL_MASTER_CODE));
    assertTrue(testEntity.isNull(TestDomain.DETAIL_MASTER_CODE_DENORM));

    testEntity.put(TestDomain.DETAIL_MASTER_FK, referencedEntityValue);
    assertFalse(testEntity.isNull(TestDomain.DETAIL_MASTER_ID));
    assertEquals(testEntity.get(TestDomain.DETAIL_MASTER_ID),
            referencedEntityValue.get(TestDomain.MASTER_ID));
    assertEquals(testEntity.get(TestDomain.DETAIL_MASTER_NAME),
            referencedEntityValue.get(TestDomain.MASTER_NAME));
    assertEquals(testEntity.get(TestDomain.DETAIL_MASTER_CODE),
            referencedEntityValue.get(TestDomain.MASTER_CODE));
    assertEquals(testEntity.get(TestDomain.DETAIL_MASTER_CODE_DENORM),
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
    assertThrows(IllegalArgumentException.class, () ->
            testEntity.getReferencedKey(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  public void isNull() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertTrue(testEntity.isNull(TestDomain.DETAIL_MASTER_ID));
    assertTrue(testEntity.isNull(TestDomain.DETAIL_MASTER_FK));
    assertTrue(testEntity.isForeignKeyNull(TestDomain.DETAIL_MASTER_FK));
    testEntity.put(TestDomain.DETAIL_MASTER_ID, 10L);

    assertFalse(testEntity.isLoaded(TestDomain.DETAIL_MASTER_FK));
    final Entity referencedEntityValue = testEntity.getForeignKey(TestDomain.DETAIL_MASTER_FK);
    assertEquals(10L, referencedEntityValue.get(TestDomain.MASTER_ID));
    assertFalse(testEntity.isLoaded(TestDomain.DETAIL_MASTER_FK));
    assertFalse(testEntity.isNull(TestDomain.DETAIL_MASTER_FK));
    assertFalse(testEntity.isNull(TestDomain.DETAIL_MASTER_ID));

    final Entity composite = ENTITIES.entity(TestDomain.T_COMPOSITE_DETAIL);
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID, null);
    assertTrue(composite.isForeignKeyNull(TestDomain.COMPOSITE_DETAIL_MASTER_FK));
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID, 1);
    assertTrue(composite.isForeignKeyNull(TestDomain.COMPOSITE_DETAIL_MASTER_FK));
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_2, null);
    assertTrue(composite.isForeignKeyNull(TestDomain.COMPOSITE_DETAIL_MASTER_FK));
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_2, 1);
    assertFalse(composite.isForeignKeyNull(TestDomain.COMPOSITE_DETAIL_MASTER_FK));
  }

  @Test
  public void removeAll() {
    final Entity referencedEntityValue = ENTITIES.entity(TestDomain.T_MASTER);
    Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    testEntity.put(TestDomain.DETAIL_STRING, "TestString");
    assertTrue(testEntity.isModified());

    testEntity.setAs(null);
    assertTrue(testEntity.getKey().isNull());
    assertFalse(testEntity.containsKey(TestDomain.DETAIL_DATE));
    assertFalse(testEntity.containsKey(TestDomain.DETAIL_STRING));
    assertFalse(testEntity.containsKey(TestDomain.DETAIL_BOOLEAN));
    assertFalse(testEntity.isModified());

    testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);

    testEntity.clearKeyValues();
    assertTrue(testEntity.getKey().isNull());
    assertTrue(testEntity.containsKey(TestDomain.DETAIL_DATE));
    assertTrue(testEntity.containsKey(TestDomain.DETAIL_STRING));
    assertTrue(testEntity.containsKey(TestDomain.DETAIL_BOOLEAN));
  }

  @Test
  public void putDenormalizedViewValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.put(TestDomain.DETAIL_MASTER_NAME, "hello"));
  }

  @Test
  public void putDenormalizedValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.put(TestDomain.DETAIL_MASTER_CODE, 2));
  }

  @Test
  public void putValue() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);

    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);

    employee.put(TestDomain.EMP_COMMISSION, 1200d);
    assertEquals(employee.get(TestDomain.EMP_COMMISSION), 1200d);

    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    assertEquals(employee.get(TestDomain.EMP_DEPARTMENT_FK), department);

    final LocalDateTime date = LocalDateTime.now();
    employee.put(TestDomain.EMP_HIREDATE, date);
    assertEquals(employee.get(TestDomain.EMP_HIREDATE), date);

    employee.put(TestDomain.EMP_ID, 123);
    assertEquals(employee.get(TestDomain.EMP_ID), 123);

    employee.put(TestDomain.EMP_NAME, "noname");
    assertEquals(employee.get(TestDomain.EMP_NAME), "noname");

    final EventDataListener<ValueChange> valueListener = valueChange -> {
      if (valueChange.getProperty().is(TestDomain.EMP_DEPARTMENT_FK)) {
        assertTrue(employee.isNull(TestDomain.EMP_DEPARTMENT_FK));
        assertTrue(employee.isNull(TestDomain.EMP_DEPARTMENT));
      }
    };
    employee.addValueListener(valueListener);
    employee.put(TestDomain.EMP_DEPARTMENT_FK, null);

    employee.removeValueListener(valueListener);
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

    assertNull(employee.get(TestDomain.EMP_SALARY));

    final double salary = 1000.1234;
    employee.put(TestDomain.EMP_SALARY, salary);
    assertEquals(Double.valueOf(1000.12), employee.get(TestDomain.EMP_SALARY));
  }

  @Test
  public void getForeignKeyValue() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, -10);
    assertTrue(employee.isForeignKeyNull(TestDomain.EMP_DEPARTMENT_FK));
    assertNull(employee.get(TestDomain.EMP_DEPARTMENT_FK));
    assertNull(employee.get(TestDomain.EMP_DEPARTMENT));

    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    assertFalse(employee.isForeignKeyNull(TestDomain.EMP_DEPARTMENT_FK));
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
    assertEquals("ename - dname", employee.get(TestDomain.EMP_NAME_DEPARTMENT));

    final Entity detail = ENTITIES.entity(TestDomain.T_DETAIL);
    detail.put(TestDomain.DETAIL_INT, 42);
    assertEquals("420", detail.getAsString(TestDomain.DETAIL_INT_DERIVED));
  }

  @Test
  public void removeValue() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, -10);
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    assertNotNull(employee.getForeignKey(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals(Integer.valueOf(-10), employee.get(TestDomain.EMP_DEPARTMENT));

    employee.remove(TestDomain.EMP_DEPARTMENT_FK);
    assertNull(employee.get(TestDomain.EMP_DEPARTMENT_FK));
    final Entity empDepartment = employee.getForeignKey(TestDomain.EMP_DEPARTMENT_FK);
    assertNotNull(empDepartment);
    //non loaded entity, created from foreign key
    assertFalse(empDepartment.containsKey(TestDomain.DEPARTMENT_NAME));
    assertNotNull(employee.get(TestDomain.EMP_DEPARTMENT));
    assertFalse(employee.containsKey(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(employee.containsKey(TestDomain.EMP_DEPARTMENT));
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
    final Entity.Key empKey1 = ENTITIES.key(TestDomain.T_EMP, 1);
    Entity.Key copy = ENTITIES.copyKey(empKey1);
    assertEquals(empKey1, copy);

    empKey1.put(TestDomain.EMP_ID, 2);
    copy = ENTITIES.copyKey(empKey1);
    assertEquals(empKey1, copy);
  }

  @Test
  public void keyEquality() {
    final List<Entity.Key> keys = ENTITIES.keys(TestDomain.T_EMP, 1, 2);
    final Entity.Key empKey1 = keys.get(0);
    final Entity.Key empKey2 = keys.get(1);
    assertNotEquals(empKey1, empKey2);

    empKey2.put(TestDomain.EMP_ID, 1);
    assertEquals(empKey1, empKey2);

    final Entity.Key deptKey = ENTITIES.key(TestDomain.T_DEPARTMENT, 1);
    assertNotEquals(empKey1, deptKey);

    final Entity.Key compMasterKey = ENTITIES.key(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    assertEquals(compMasterKey, compMasterKey);
    assertNotEquals(empKey1, compMasterKey);
    assertNotEquals(compMasterKey, new Object());

    final Entity.Key compMasterKey2 = ENTITIES.key(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    assertNotEquals(compMasterKey, compMasterKey2);

    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    assertEquals(compMasterKey, compMasterKey2);

    final Entity.Key detailKey = ENTITIES.key(TestDomain.T_DETAIL, 1L);
    final Entity.Key detailKey2 = ENTITIES.key(TestDomain.T_DETAIL, 2L);
    assertNotEquals(detailKey, detailKey2);

    detailKey2.put(TestDomain.DETAIL_ID, 1L);
    assertEquals(detailKey2, detailKey);
  }

  @Test
  public void nullKeyEquals() {
    final Entity.Key nullKey = ENTITIES.key(TestDomain.T_EMP);
    final Entity.Key zeroKey = ENTITIES.key(TestDomain.T_EMP, 0);
    assertNotEquals(nullKey, zeroKey);
  }

  @Test
  public void transientPropertyModifiesEntity() throws IOException, ClassNotFoundException {
    final Entity.Identity entityId = entityIdentity("entityId");
    final Attribute<Integer> trans = entityId.integerAttribute("trans");
    final Attribute<Integer> id = entityId.integerAttribute("id");
    final TransientProperty.Builder<?> transientProperty = Properties.transientProperty(trans);
    class TestDomain extends Domain {
      public TestDomain() {
        super("transient");
        define(entityId,
                Properties.primaryKeyProperty(id),
                transientProperty);  }
    }
    final Entities entities = new TestDomain().registerEntities();

    final Entity entity = entities.entity(entityId);
    entity.put(id, 42);
    entity.put(trans, null);
    entity.saveAll();

    entity.put(trans, 1);
    assertTrue(entity.isModified());

    transientProperty.modifiesEntity(false);
    assertFalse(entity.isModified());

    final Entity deserialized = Serializer.deserialize(Serializer.serialize(entity));
    assertTrue(deserialized.isModified(trans));
  }

  @Test
  public void foreignKeyModification() {
    final Entity emp = ENTITIES.entity(TestDomain.T_EMP);
    final Entity dept = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 1);
    dept.put(TestDomain.DEPARTMENT_NAME, "Name1");
    emp.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    assertEquals(1, emp.get(TestDomain.EMP_DEPARTMENT));
    emp.put(TestDomain.EMP_DEPARTMENT, 2);
    assertNull(emp.get(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(emp.isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    final Entity empDept = emp.getForeignKey(TestDomain.EMP_DEPARTMENT_FK);
    assertEquals(2, empDept.getKey().getFirstValue());

    final Entity dept2 = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, 3);
    dept2.put(TestDomain.DEPARTMENT_NAME, "Name2");
    emp.put(TestDomain.EMP_DEPARTMENT_FK, dept2);
    emp.put(TestDomain.EMP_DEPARTMENT, 3);
    assertNotNull(emp.get(TestDomain.EMP_DEPARTMENT_FK));
    emp.put(TestDomain.EMP_DEPARTMENT, 4);
    assertNull(emp.get(TestDomain.EMP_DEPARTMENT_FK));
    emp.put(TestDomain.EMP_DEPARTMENT_FK, dept2);
    assertNotNull(emp.get(TestDomain.EMP_DEPARTMENT_FK));
    emp.put(TestDomain.EMP_DEPARTMENT, null);
    assertNull(emp.get(TestDomain.EMP_DEPARTMENT_FK));

    final Entity manager = ENTITIES.entity(TestDomain.T_EMP);
    manager.put(TestDomain.EMP_ID, 10);
    manager.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp.put(TestDomain.EMP_MGR_FK, manager);
    emp.put(TestDomain.EMP_DEPARTMENT_FK, dept2);

    final Entity copy = ENTITIES.deepCopyEntity(emp);
    assertNotSame(emp, copy);
    assertTrue(emp.valuesEqual(copy));
    assertNotSame(emp.get(TestDomain.EMP_MGR_FK), copy.get(TestDomain.EMP_MGR_FK));
    assertTrue(emp.getForeignKey(TestDomain.EMP_MGR_FK).valuesEqual(copy.getForeignKey(TestDomain.EMP_MGR_FK)));
    assertNotSame(emp.getForeignKey(TestDomain.EMP_MGR_FK).getForeignKey(TestDomain.EMP_DEPARTMENT_FK),
            copy.getForeignKey(TestDomain.EMP_MGR_FK).getForeignKey(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(emp.getForeignKey(TestDomain.EMP_MGR_FK).getForeignKey(TestDomain.EMP_DEPARTMENT_FK)
            .valuesEqual(copy.getForeignKey(TestDomain.EMP_MGR_FK).getForeignKey(TestDomain.EMP_DEPARTMENT_FK)));
  }

  private Entity getDetailEntity(final long id, final Integer intValue, final Double doubleValue,
                                 final String stringValue, final LocalDate dateValue, final LocalDateTime timestampValue,
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