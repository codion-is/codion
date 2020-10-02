/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Serializer;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Master;
import is.codion.framework.domain.entity.ForeignKeyDomain.Maturity;
import is.codion.framework.domain.entity.ForeignKeyDomain.Otolith;
import is.codion.framework.domain.entity.ForeignKeyDomain.OtolithCategory;
import is.codion.framework.domain.entity.ForeignKeyDomain.Species;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

  private static final Entities ENTITIES = new TestDomain().getEntities();

  @Test
  public void construction() {
    final EntityDefinition masterDefinition = ENTITIES.getDefinition(Master.TYPE);

    final Map<Attribute<?>, Object> values = new HashMap<>();
    values.put(Detail.BOOLEAN, false);
    values.put(Master.CODE, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, values, null));

    final Map<Attribute<?>, Object> originalValues = new HashMap<>();
    originalValues.put(Detail.BOOLEAN, false);
    originalValues.put(Master.CODE, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, null, originalValues));

    final Map<Attribute<?>, Object> invalidTypeValues = new HashMap<>();
    invalidTypeValues.put(Master.CODE, false);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, invalidTypeValues, null));

    final Map<Attribute<?>, Object> invalidTypeOriginalValues = new HashMap<>();
    invalidTypeOriginalValues.put(Master.CODE, false);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, null, invalidTypeOriginalValues));

    final EntityType<Entity> entityType = TestDomain.DOMAIN.entityType("entityType");
    final Attribute<?> invalid = entityType.integerAttribute("invalid");
    final Map<Attribute<?>, Object> invalidPropertyValues = new HashMap<>();
    invalidPropertyValues.put(invalid, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, invalidPropertyValues, null));
  }

  @Test
  public void serialization() throws Exception {
    final Entity referencedEntityValue = ENTITIES.entity(Master.TYPE);
    referencedEntityValue.put(Master.ID, 1L);
    referencedEntityValue.put(Master.NAME, "name");
    referencedEntityValue.put(Master.CODE, 10);
    final String originalStringValue = "string value";
    final Entity entity = getDetailEntity(10, 34, 23.4, originalStringValue, LocalDate.now(),
            LocalDateTime.now(), true, referencedEntityValue);
    entity.put(Detail.STRING, "a new String value");
    final List<Object> fromFile = Serializer.deserialize(Serializer.serialize(singletonList(entity)));
    assertEquals(1, fromFile.size());
    final Entity entityFromFile = (Entity) fromFile.get(0);
    assertEquals(Detail.TYPE, entity.getEntityType());
    assertTrue(entity.columnValuesEqual(entityFromFile));
    assertTrue(entityFromFile.isModified());
    assertTrue(entityFromFile.isModified(Detail.STRING));
    assertEquals(originalStringValue, entityFromFile.getOriginal(Detail.STRING));

    final Key key = entity.getPrimaryKey();
    final byte[] serialized = Serializer.serialize(singletonList(key));
    final List<Object> keyFromFile = Serializer.deserialize(serialized);
    assertEquals(1, keyFromFile.size());
    assertEquals(key, keyFromFile.get(0));

    final Entity master = ENTITIES.entity(Master.TYPE);
    master.put(Master.ID, 1L);
    master.put(Master.CODE, 11);

    final Entity masterDeserialized = Serializer.deserialize(Serializer.serialize(master));
    assertEquals(master.get(Master.ID), masterDeserialized.get(Master.ID));
    assertEquals(master.get(Master.CODE), masterDeserialized.get(Master.CODE));
    assertFalse(masterDeserialized.containsKey(Master.NAME));
  }

  @Test
  public void setAs() {
    final Entity referencedEntityValue = ENTITIES.entity(Master.TYPE);
    referencedEntityValue.put(Master.ID, 2L);
    referencedEntityValue.put(Master.NAME, masterName);
    referencedEntityValue.put(Master.CODE, 7);

    final Entity test = ENTITIES.entity(Detail.TYPE);
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    test.setAs(testEntity);
    assertEquals(test, testEntity, "Entities should be equal after .setAs()");
    assertTrue(test.columnValuesEqual(testEntity), "Entity property values should be equal after .setAs()");

    //assure that no cached foreign key values linger
    test.put(Detail.MASTER_FK, null);
    testEntity.setAs(test);
    assertNull(testEntity.get(Detail.MASTER_ID));
    assertNull(testEntity.get(Detail.MASTER_FK));

    assertThrows(IllegalArgumentException.class, () -> testEntity.setAs(referencedEntityValue));
  }

  @Test
  public void saveRevertValue() {
    final Entity entity = ENTITIES.entity(Master.TYPE);
    final String newName = "aname";

    entity.put(Master.ID, 2L);
    entity.put(Master.NAME, masterName);
    entity.put(Master.CODE, 7);

    entity.put(Master.ID, -55L);
    //the id is not updatable as it is part of the primary key, which is not updatable by default
    assertFalse(entity.isModified());
    entity.save(Master.ID);
    assertFalse(entity.isModified());

    entity.put(Master.NAME, newName);
    assertTrue(entity.isModified());
    entity.revert(Master.NAME);
    assertEquals(masterName, entity.get(Master.NAME));
    assertFalse(entity.isModified());

    entity.put(Master.NAME, newName);
    assertTrue(entity.isModified());
    assertTrue(entity.isModified(Master.NAME));
    entity.save(Master.NAME);
    assertEquals(newName, entity.get(Master.NAME));
    assertFalse(entity.isModified());
    assertFalse(entity.isModified(Master.NAME));
  }

  @Test
  public void getReferencedKeyCache() {
    final Entity compositeDetail = ENTITIES.entity(TestDomain.T_COMPOSITE_DETAIL);
    compositeDetail.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID, 1);
    compositeDetail.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_2, 2);

    final Key referencedKey = compositeDetail.getReferencedKey(TestDomain.COMPOSITE_DETAIL_MASTER_FK);
    final Key cachedKey = compositeDetail.getReferencedKey(TestDomain.COMPOSITE_DETAIL_MASTER_FK);

    assertSame(cachedKey, referencedKey);

    final Entity master = ENTITIES.entity(Master.TYPE);
    master.put(Master.CODE, 3);

    final Entity detail = ENTITIES.entity(Detail.TYPE);
    detail.put(Detail.MASTER_VIA_CODE_FK, master);

    final Key codeKey = detail.getReferencedKey(Detail.MASTER_VIA_CODE_FK);
    assertEquals(Integer.valueOf(3), codeKey.get());
    final Key cachedCodeKey = detail.getReferencedKey(Detail.MASTER_VIA_CODE_FK);
    assertEquals(Integer.valueOf(3), cachedCodeKey.get());

    assertSame(codeKey, cachedCodeKey);
  }

  @Test
  public void compositeReferenceKey() {
    final Entity master = ENTITIES.entity(TestDomain.T_COMPOSITE_MASTER);
    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    master.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    master.put(TestDomain.COMPOSITE_MASTER_ID_3, 3);

    final Entity detail = ENTITIES.entity(TestDomain.T_COMPOSITE_DETAIL);
    //can not update read only attribute reference
    assertThrows(IllegalArgumentException.class, () -> detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_FK, master));

    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_3, 3);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_FK, master);

    //otherwise the values are equal and put() returns before propagating foreign key values
    final Entity masterCopy = ENTITIES.deepCopyEntity(master);
    masterCopy.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    masterCopy.put(TestDomain.COMPOSITE_MASTER_ID_2, null);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_FK, masterCopy);

    assertNull(detail.getReferencedKey(TestDomain.COMPOSITE_DETAIL_MASTER_FK));

    master.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    master.put(TestDomain.COMPOSITE_MASTER_ID_2, 3);
    master.put(TestDomain.COMPOSITE_MASTER_ID_3, 3);

    final Entity detail2 = ENTITIES.entity(TestDomain.T_COMPOSITE_DETAIL);
    detail2.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_3, 3);
    detail2.put(TestDomain.COMPOSITE_DETAIL_MASTER_FK, master);

    assertEquals(3, detail2.get(TestDomain.COMPOSITE_DETAIL_MASTER_ID_2));

    detail2.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_2, null);
    assertTrue(detail2.isNull(TestDomain.COMPOSITE_DETAIL_MASTER_FK));
    assertNull(detail2.getReferencedKey(TestDomain.COMPOSITE_DETAIL_MASTER_FK));
  }

  @Test
  public void compositeKeyNull() {
    final Entity master = ENTITIES.entity(TestDomain.T_COMPOSITE_MASTER);
    assertTrue(master.getPrimaryKey().isNull());
    assertFalse(master.getPrimaryKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    master.put(TestDomain.COMPOSITE_MASTER_ID_3, 3);
    assertFalse(master.getPrimaryKey().isNull());
    assertTrue(master.getPrimaryKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    assertFalse(master.getPrimaryKey().isNull());
    assertTrue(master.getPrimaryKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, 2);
    master.put(TestDomain.COMPOSITE_MASTER_ID_2, null);
    assertTrue(master.getPrimaryKey().isNull());
    assertFalse(master.getPrimaryKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    assertTrue(master.getPrimaryKey().isNull());
    assertFalse(master.getPrimaryKey().isNotNull());
  }

  @Test
  public void singleKeyNull() {
    final Key key = ENTITIES.primaryKey(Detail.TYPE);
    assertTrue(key.isNull());
    key.put(null);
    assertTrue(key.isNull());
    key.put(1L);
    assertTrue(key.isNotNull());
  }

  @Test
  public void noPrimaryKey() {
    final Entity noPk = ENTITIES.entity(TestDomain.T_NO_PK);
    noPk.put(TestDomain.NO_PK_COL1, 1);
    noPk.put(TestDomain.NO_PK_COL2, 2);
    noPk.put(TestDomain.NO_PK_COL3, 3);
    final Key key = noPk.getPrimaryKey();
    assertTrue(key.isNull());
    final Key originalKey = noPk.getOriginalPrimaryKey();
    assertTrue(originalKey.isNull());
  }

  @Test
  public void entity() throws Exception {
    final Entity referencedEntityValue = ENTITIES.entity(Master.TYPE);
    //assert not modified
    assertFalse(referencedEntityValue.isModified());

    referencedEntityValue.put(Master.ID, 2L);
    referencedEntityValue.put(Master.NAME, masterName);
    referencedEntityValue.put(Master.CODE, 7);

    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);

    //assert values
    assertEquals(testEntity.get(Detail.ID), detailId);
    assertTrue(testEntity.getOptional(Detail.ID).isPresent());
    assertEquals(testEntity.get(Detail.INT), detailInt);
    assertEquals(testEntity.get(Detail.DOUBLE), detailDouble);
    assertEquals(testEntity.get(Detail.STRING), detailString);
    assertEquals(testEntity.get(Detail.DATE), detailDate);
    assertEquals(testEntity.get(Detail.TIMESTAMP), detailTimestamp);
    assertEquals(testEntity.get(Detail.BOOLEAN), detailBoolean);
    assertEquals(testEntity.get(Detail.MASTER_FK), referencedEntityValue);
    assertEquals(testEntity.get(Detail.MASTER_NAME), masterName);
    assertEquals(testEntity.get(Detail.MASTER_CODE), 7);
    assertFalse(testEntity.isNull(Detail.MASTER_ID));
    assertTrue(testEntity.isNotNull(Detail.MASTER_ID));

    testEntity.getReferencedKey(Detail.MASTER_FK);

    //test copy()
    final Entity test2 = ENTITIES.deepCopyEntity(testEntity);
    assertNotSame(test2, testEntity, "Entity copy should not be == the original");
    assertEquals(test2, testEntity, "Entities should be equal after .getCopy()");
    assertTrue(test2.columnValuesEqual(testEntity), "Entity property values should be equal after .getCopy()");
    assertNotSame(testEntity.getForeignKey(Detail.MASTER_FK), test2.getForeignKey(Detail.MASTER_FK), "This should be a deep copy");

    test2.put(Detail.DOUBLE, 2.1);
    assertTrue(test2.isModified());
    final Entity test2Copy = ENTITIES.copyEntity(test2);
    assertTrue(test2Copy.isModified());

    //test propagate entity reference/denormalized values
    testEntity.put(Detail.MASTER_FK, null);
    assertTrue(testEntity.isNull(Detail.MASTER_ID));
    assertTrue(testEntity.isNull(Detail.MASTER_NAME));
    assertFalse(testEntity.getOptional(Detail.MASTER_NAME).isPresent());
    assertTrue(testEntity.isNull(Detail.MASTER_CODE));
    assertTrue(testEntity.isNull(Detail.MASTER_CODE_DENORM));

    testEntity.put(Detail.MASTER_FK, referencedEntityValue);
    assertFalse(testEntity.isNull(Detail.MASTER_ID));
    assertEquals(testEntity.get(Detail.MASTER_ID),
            referencedEntityValue.get(Master.ID));
    assertEquals(testEntity.get(Detail.MASTER_NAME),
            referencedEntityValue.get(Master.NAME));
    assertEquals(testEntity.get(Detail.MASTER_CODE),
            referencedEntityValue.get(Master.CODE));
    assertEquals(testEntity.get(Detail.MASTER_CODE_DENORM),
            referencedEntityValue.get(Master.CODE));

    referencedEntityValue.put(Master.CODE, 20);
    testEntity.put(Detail.MASTER_FK, referencedEntityValue);
    assertEquals(testEntity.get(Detail.MASTER_CODE),
            referencedEntityValue.get(Master.CODE));
  }

  @Test
  public void getReferencedKeyIncorrectFK() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () ->
            testEntity.getReferencedKey(TestDomain.Employee.DEPARTMENT_FK));
  }

  @Test
  public void isNull() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertTrue(testEntity.isNull(Detail.MASTER_ID));
    assertTrue(testEntity.isNull(Detail.MASTER_FK));
    assertTrue(testEntity.isForeignKeyNull(Detail.MASTER_FK));
    testEntity.put(Detail.MASTER_ID, 10L);

    assertFalse(testEntity.isLoaded(Detail.MASTER_FK));
    final Entity referencedEntityValue = testEntity.getForeignKey(Detail.MASTER_FK);
    assertEquals(10L, referencedEntityValue.get(Master.ID));
    assertFalse(testEntity.isLoaded(Detail.MASTER_FK));
    assertFalse(testEntity.isNull(Detail.MASTER_FK));
    assertFalse(testEntity.isNull(Detail.MASTER_ID));

    final Entity composite = ENTITIES.entity(TestDomain.T_COMPOSITE_DETAIL);
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID, null);
    assertTrue(composite.isForeignKeyNull(TestDomain.COMPOSITE_DETAIL_MASTER_FK));
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID, 1);
    assertTrue(composite.isForeignKeyNull(TestDomain.COMPOSITE_DETAIL_MASTER_FK));
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_2, null);
    assertTrue(composite.isForeignKeyNull(TestDomain.COMPOSITE_DETAIL_MASTER_FK));
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_2, 1);
    composite.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_3, 2);
    assertFalse(composite.isForeignKeyNull(TestDomain.COMPOSITE_DETAIL_MASTER_FK));
  }

  @Test
  public void removeAll() {
    final Entity referencedEntityValue = ENTITIES.entity(Master.TYPE);
    Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    testEntity.put(Detail.STRING, "TestString");
    assertTrue(testEntity.isModified());

    testEntity.setAs(null);
    assertTrue(testEntity.getPrimaryKey().isNull());
    assertFalse(testEntity.containsKey(Detail.DATE));
    assertFalse(testEntity.containsKey(Detail.STRING));
    assertFalse(testEntity.containsKey(Detail.BOOLEAN));
    assertFalse(testEntity.isModified());

    testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);

    testEntity.clearPrimaryKeyValues();
    assertTrue(testEntity.getPrimaryKey().isNull());
    assertTrue(testEntity.containsKey(Detail.DATE));
    assertTrue(testEntity.containsKey(Detail.STRING));
    assertTrue(testEntity.containsKey(Detail.BOOLEAN));
  }

  @Test
  public void putDenormalizedViewValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.put(Detail.MASTER_NAME, "hello"));
  }

  @Test
  public void putDenormalizedValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.put(Detail.MASTER_CODE, 2));
  }

  @Test
  public void putValue() {
    final Entity department = ENTITIES.entity(TestDomain.Department.TYPE);
    department.put(TestDomain.Department.NO, -10);

    final Entity employee = ENTITIES.entity(TestDomain.Employee.TYPE);

    employee.put(TestDomain.Employee.COMMISSION, 1200d);
    assertEquals(employee.get(TestDomain.Employee.COMMISSION), 1200d);

    employee.put(TestDomain.Employee.DEPARTMENT_FK, department);
    assertEquals(employee.get(TestDomain.Employee.DEPARTMENT_FK), department);

    final LocalDateTime date = LocalDateTime.now();
    employee.put(TestDomain.Employee.HIREDATE, date);
    assertEquals(employee.get(TestDomain.Employee.HIREDATE), date);

    employee.put(TestDomain.Employee.ID, 123);
    assertEquals(employee.get(TestDomain.Employee.ID), 123);

    employee.put(TestDomain.Employee.NAME, "noname");
    assertEquals(employee.get(TestDomain.Employee.NAME), "noname");
  }

  @Test
  public void columnValuesEqual() {
    final Entity testEntityOne = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    final Entity testEntityTwo = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);

    assertTrue(testEntityOne.columnValuesEqual(testEntityTwo));

    testEntityTwo.put(Detail.INT, 42);
    assertFalse(testEntityOne.columnValuesEqual(testEntityTwo));

    testEntityOne.put(Detail.INT, 42);
    assertTrue(testEntityOne.columnValuesEqual(testEntityTwo));

    testEntityTwo.remove(Detail.INT);
    assertFalse(testEntityOne.columnValuesEqual(testEntityTwo));

    testEntityOne.remove(Detail.INT);
    assertTrue(testEntityOne.columnValuesEqual(testEntityTwo));

    final Random random = new Random();
    final byte[] bytes = new byte[1024];
    random.nextBytes(bytes);

    testEntityOne.put(Detail.BYTES, bytes);
    assertFalse(testEntityOne.columnValuesEqual(testEntityTwo));

    testEntityTwo.put(Detail.BYTES, bytes);
    assertTrue(testEntityOne.columnValuesEqual(testEntityTwo));

    assertThrows(IllegalArgumentException.class, () -> testEntityOne.columnValuesEqual(ENTITIES.entity(Master.TYPE)));
  }

  @Test
  public void getDoubleValue() {
    final Entity employee = ENTITIES.entity(TestDomain.Employee.TYPE);
    employee.put(TestDomain.Employee.ID, -10);

    assertNull(employee.get(TestDomain.Employee.SALARY));

    final double salary = 1000.1234;
    employee.put(TestDomain.Employee.SALARY, salary);
    assertEquals(Double.valueOf(1000.12), employee.get(TestDomain.Employee.SALARY));
  }

  @Test
  public void getForeignKeyValue() {
    final Entity department = ENTITIES.entity(TestDomain.Department.TYPE);
    department.put(TestDomain.Department.NO, -10);
    final Entity employee = ENTITIES.entity(TestDomain.Employee.TYPE);
    employee.put(TestDomain.Employee.ID, -10);
    assertTrue(employee.isForeignKeyNull(TestDomain.Employee.DEPARTMENT_FK));
    assertNull(employee.get(TestDomain.Employee.DEPARTMENT_FK));
    assertNull(employee.get(TestDomain.Employee.DEPARTMENT));

    employee.put(TestDomain.Employee.DEPARTMENT_FK, department);
    assertFalse(employee.isForeignKeyNull(TestDomain.Employee.DEPARTMENT_FK));
    assertNotNull(employee.get(TestDomain.Employee.DEPARTMENT_FK));
    assertNotNull(employee.get(TestDomain.Employee.DEPARTMENT));
  }

  @Test
  public void getDerivedValue() {
    final Entity department = ENTITIES.entity(TestDomain.Department.TYPE);
    department.put(TestDomain.Department.NAME, "dname");
    final Entity employee = ENTITIES.entity(TestDomain.Employee.TYPE);
    employee.put(TestDomain.Employee.NAME, "ename");
    employee.put(TestDomain.Employee.DEPARTMENT_FK, department);
    assertEquals("ename - dname", employee.get(TestDomain.Employee.DEPARTMENT_NAME));

    final Entity detail = ENTITIES.entity(Detail.TYPE);
    detail.put(Detail.INT, 42);
    assertEquals("420", detail.getAsString(Detail.INT_DERIVED));
  }

  @Test
  public void removeValue() {
    final Entity department = ENTITIES.entity(TestDomain.Department.TYPE);
    department.put(TestDomain.Department.NO, -10);
    final Entity employee = ENTITIES.entity(TestDomain.Employee.TYPE);
    employee.put(TestDomain.Employee.ID, -10);
    employee.put(TestDomain.Employee.DEPARTMENT_FK, department);
    assertNotNull(employee.getForeignKey(TestDomain.Employee.DEPARTMENT_FK));
    assertEquals(Integer.valueOf(-10), employee.get(TestDomain.Employee.DEPARTMENT));

    employee.remove(TestDomain.Employee.DEPARTMENT_FK);
    assertNull(employee.get(TestDomain.Employee.DEPARTMENT_FK));
    final Entity empDepartment = employee.getForeignKey(TestDomain.Employee.DEPARTMENT_FK);
    assertNotNull(empDepartment);
    //non loaded entity, created from foreign key
    assertFalse(empDepartment.containsKey(TestDomain.Department.NAME));
    assertNotNull(employee.get(TestDomain.Employee.DEPARTMENT));
    assertFalse(employee.containsKey(TestDomain.Employee.DEPARTMENT_FK));
    assertTrue(employee.containsKey(TestDomain.Employee.DEPARTMENT));
  }

  @Test
  public void maximumFractionDigits() {
    final Entity employee = ENTITIES.entity(TestDomain.Employee.TYPE);
    employee.put(TestDomain.Employee.COMMISSION, 1.1234);
    assertEquals(1.12, employee.get(TestDomain.Employee.COMMISSION));
    employee.put(TestDomain.Employee.COMMISSION, 1.1255);
    assertEquals(1.13, employee.get(TestDomain.Employee.COMMISSION));

    final Entity detail = ENTITIES.entity(Detail.TYPE);
    detail.put(Detail.DOUBLE, 1.123456789567);
    assertEquals(1.1234567896, detail.get(Detail.DOUBLE));//default 10 fraction digits
  }

  @Test
  public void keyInvalidPropertyGet() {
    final Key empKey1 = ENTITIES.primaryKey(TestDomain.Employee.TYPE);
    assertThrows(IllegalArgumentException.class, () -> empKey1.get(TestDomain.Employee.NAME));
  }

  @Test
  public void keyInvalidPropertyPut() {
    final Key empKey1 = ENTITIES.primaryKey(TestDomain.Employee.TYPE);
    assertThrows(IllegalArgumentException.class, () -> empKey1.put("test"));
  }

  @Test
  public void keyEquality() {
    final List<Key> keys = ENTITIES.primaryKeys(TestDomain.Employee.TYPE, 1, 2);
    final Key empKey1 = keys.get(0);
    final Key empKey2 = keys.get(1);
    assertNotEquals(empKey1, empKey2);

    empKey2.put(1);
    assertEquals(empKey1, empKey2);

    final Key deptKey = ENTITIES.primaryKey(TestDomain.Department.TYPE, 1);
    assertNotEquals(empKey1, deptKey);

    final Key compMasterKey = ENTITIES.primaryKey(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    assertEquals(compMasterKey, compMasterKey);
    assertNotEquals(empKey1, compMasterKey);
    assertNotEquals(compMasterKey, new Object());

    final Key compMasterKey2 = ENTITIES.primaryKey(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    assertNotEquals(compMasterKey, compMasterKey2);

    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    //keys are still null, since COMPOSITE_MASTER_ID_3 is null
    assertNotEquals(compMasterKey, compMasterKey2);

    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID_3, 3);
    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID_3, 3);
    assertEquals(compMasterKey, compMasterKey2);

    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID, null);
    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID, null);
    //not null since COMPOSITE_MASTER_ID is nullable
    assertEquals(compMasterKey, compMasterKey2);

    final Key detailKey = ENTITIES.primaryKey(Detail.TYPE, 1L);
    final Key detailKey2 = ENTITIES.primaryKey(Detail.TYPE, 2L);
    assertNotEquals(detailKey, detailKey2);

    detailKey2.put(1L);
    assertEquals(detailKey2, detailKey);

    final Entity department1 = ENTITIES.entity(TestDomain.Department.TYPE);
    department1.put(TestDomain.Department.NO, 1);
    final Entity department2 = ENTITIES.entity(TestDomain.Department.TYPE);
    department2.put(TestDomain.Department.NO, 1);

    assertEquals(department1.getPrimaryKey(), department2.getPrimaryKey());

    department2.put(TestDomain.Department.NO, 2);
    assertNotEquals(department1.getPrimaryKey(), department2.getPrimaryKey());

    department1.put(TestDomain.Department.NO, null);
    assertNotEquals(department1.getPrimaryKey(), department2.getPrimaryKey());

    department2.put(TestDomain.Department.NO, null);
    assertNotEquals(department1.getPrimaryKey(), department2.getPrimaryKey());

    department1.remove(TestDomain.Department.NO);
    assertNotEquals(department1.getPrimaryKey(), department2.getPrimaryKey());

    department2.remove(TestDomain.Department.NO);
    assertNotEquals(department1.getPrimaryKey(), department2.getPrimaryKey());
  }

  @Test
  public void nullKeyEquals() {
    final Key nullKey = ENTITIES.primaryKey(TestDomain.Employee.TYPE);
    final Key zeroKey = ENTITIES.primaryKey(TestDomain.Employee.TYPE, 0);
    assertNotEquals(nullKey, zeroKey);
  }

  @Test
  public void transientPropertyModifiesEntity() throws IOException, ClassNotFoundException {
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(TestDomain.T_TRANS);
    entity.put(TestDomain.TRANS_ID, 42);
    entity.put(TestDomain.TRANS_TRANS, null);
    entity.saveAll();

    entity.put(TestDomain.TRANS_TRANS, 1);
    assertTrue(entity.isModified());

    TestDomain.TRANS_BUILDER.modifiesEntity(false);
    assertFalse(entity.isModified());

    final Entity deserialized = Serializer.deserialize(Serializer.serialize(entity));
    assertTrue(deserialized.isModified(TestDomain.TRANS_TRANS));
  }

  @Test
  public void foreignKeyModification() {
    final Entity emp = ENTITIES.entity(TestDomain.Employee.TYPE);
    final Entity dept = ENTITIES.entity(TestDomain.Department.TYPE);
    dept.put(TestDomain.Department.NO, 1);
    dept.put(TestDomain.Department.NAME, "Name1");
    emp.put(TestDomain.Employee.DEPARTMENT_FK, dept);
    assertEquals(1, emp.get(TestDomain.Employee.DEPARTMENT));
    emp.put(TestDomain.Employee.DEPARTMENT, 2);
    assertNull(emp.get(TestDomain.Employee.DEPARTMENT_FK));
    assertFalse(emp.isLoaded(TestDomain.Employee.DEPARTMENT_FK));
    final Entity empDept = emp.getForeignKey(TestDomain.Employee.DEPARTMENT_FK);
    assertEquals(Integer.valueOf(2), empDept.getPrimaryKey().get());

    final Entity dept2 = ENTITIES.entity(TestDomain.Department.TYPE);
    dept2.put(TestDomain.Department.NO, 3);
    dept2.put(TestDomain.Department.NAME, "Name2");
    emp.put(TestDomain.Employee.DEPARTMENT_FK, dept2);
    emp.put(TestDomain.Employee.DEPARTMENT, 3);
    assertNotNull(emp.get(TestDomain.Employee.DEPARTMENT_FK));
    emp.put(TestDomain.Employee.DEPARTMENT, 4);
    assertNull(emp.get(TestDomain.Employee.DEPARTMENT_FK));
    emp.put(TestDomain.Employee.DEPARTMENT_FK, dept2);
    assertNotNull(emp.get(TestDomain.Employee.DEPARTMENT_FK));
    emp.put(TestDomain.Employee.DEPARTMENT, null);
    assertNull(emp.get(TestDomain.Employee.DEPARTMENT_FK));

    final Entity manager = ENTITIES.entity(TestDomain.Employee.TYPE);
    manager.put(TestDomain.Employee.ID, 10);
    manager.put(TestDomain.Employee.DEPARTMENT_FK, dept);
    emp.put(TestDomain.Employee.MANAGER_FK, manager);
    emp.put(TestDomain.Employee.DEPARTMENT_FK, dept2);

    final Entity copy = ENTITIES.deepCopyEntity(emp);
    assertNotSame(emp, copy);
    assertTrue(emp.columnValuesEqual(copy));
    assertNotSame(emp.get(TestDomain.Employee.MANAGER_FK), copy.get(TestDomain.Employee.MANAGER_FK));
    assertTrue(emp.getForeignKey(TestDomain.Employee.MANAGER_FK).columnValuesEqual(copy.getForeignKey(TestDomain.Employee.MANAGER_FK)));
    assertNotSame(emp.getForeignKey(TestDomain.Employee.MANAGER_FK).getForeignKey(TestDomain.Employee.DEPARTMENT_FK),
            copy.getForeignKey(TestDomain.Employee.MANAGER_FK).getForeignKey(TestDomain.Employee.DEPARTMENT_FK));
    assertTrue(emp.getForeignKey(TestDomain.Employee.MANAGER_FK).getForeignKey(TestDomain.Employee.DEPARTMENT_FK)
            .columnValuesEqual(copy.getForeignKey(TestDomain.Employee.MANAGER_FK).getForeignKey(TestDomain.Employee.DEPARTMENT_FK)));
  }

  @Test
  public void readOnlyForeignKeyReferences() {
    final ForeignKeyDomain domain = new ForeignKeyDomain();
    final Entities entities = domain.getEntities();

    final Entity cod = entities.entity(Species.TYPE);
    cod.put(Species.NO, 1);
    cod.put(Species.NAME, "Cod");

    final Entity codMaturity10 = entities.entity(Maturity.TYPE);
    codMaturity10.put(Maturity.SPECIES_FK, cod);
    codMaturity10.put(Maturity.NO, 10);
    final Entity codMaturity20 = entities.entity(Maturity.TYPE);
    codMaturity20.put(Maturity.SPECIES_FK, cod);
    codMaturity20.put(Maturity.NO, 20);

    final Entity haddock = entities.entity(Species.TYPE);
    haddock.put(Species.NO, 2);
    haddock.put(Species.NAME, "Haddock");

    final Entity haddockMaturity10 = entities.entity(Maturity.TYPE);
    haddockMaturity10.put(Maturity.SPECIES_FK, haddock);
    haddockMaturity10.put(Maturity.NO, 10);
    final Entity haddockMaturity20 = entities.entity(Maturity.TYPE);
    haddockMaturity20.put(Maturity.SPECIES_FK, haddock);
    haddockMaturity20.put(Maturity.NO, 20);

    final Entity otolithCategoryCod100 = entities.entity(OtolithCategory.TYPE);
    otolithCategoryCod100.put(OtolithCategory.SPECIES_FK, cod);
    otolithCategoryCod100.put(OtolithCategory.NO, 100);
    final Entity otolithCategoryCod200 = entities.entity(OtolithCategory.TYPE);
    otolithCategoryCod200.put(OtolithCategory.SPECIES_FK, cod);
    otolithCategoryCod200.put(OtolithCategory.NO, 200);

    final Entity otolithCategoryHaddock100 = entities.entity(OtolithCategory.TYPE);
    otolithCategoryHaddock100.put(OtolithCategory.SPECIES_FK, haddock);
    otolithCategoryHaddock100.put(OtolithCategory.NO, 100);
    final Entity otolithCategoryHaddock200 = entities.entity(OtolithCategory.TYPE);
    otolithCategoryHaddock200.put(OtolithCategory.SPECIES_FK, haddock);
    otolithCategoryHaddock200.put(OtolithCategory.NO, 200);

    final Entity otolith = entities.entity(Otolith.TYPE);
    otolith.put(Otolith.SPECIES_FK, cod);
    //try to set a haddock maturity
    assertThrows(IllegalArgumentException.class, () -> otolith.put(Otolith.MATURITY_FK, haddockMaturity10));
    assertThrows(IllegalArgumentException.class, () -> otolith.put(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryHaddock100));
    otolith.put(Otolith.MATURITY_FK, codMaturity10);
    otolith.put(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryCod100);
    //remove species, maturity and category should be removed
    otolith.put(Otolith.SPECIES_FK, null);
    //removes the invalid foreign key entity
    assertTrue(otolith.isNull(Otolith.MATURITY_FK));
    assertTrue(otolith.isNull(Otolith.OTOLITH_CATEGORY_FK));
    //should not remove the actual column value
    assertFalse(otolith.isNull(Otolith.MATURITY_NO));
    assertFalse(otolith.isNull(Otolith.OTOLITH_CATEGORY_NO));
    assertTrue(otolith.isNull(Otolith.SPECIES_NO));
    //try to set haddock maturity and category
    assertThrows(IllegalArgumentException.class, () -> otolith.put(Otolith.MATURITY_FK, haddockMaturity10));
    assertThrows(IllegalArgumentException.class, () -> otolith.put(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryHaddock100));
    //set the species to haddock
    otolith.put(Otolith.SPECIES_FK, haddock);
    otolith.put(Otolith.MATURITY_FK, haddockMaturity10);
    otolith.put(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryHaddock200);
    assertFalse(otolith.isNull(Otolith.MATURITY_FK));
    assertFalse(otolith.isNull(Otolith.OTOLITH_CATEGORY_FK));
    //set the species back to cod, haddock maturity should be removed
    otolith.put(Otolith.SPECIES_FK, cod);
    assertNull(otolith.get(Otolith.MATURITY_FK));
    assertNull(otolith.get(Otolith.OTOLITH_CATEGORY_FK));
    //set the cod maturity
    otolith.put(Otolith.MATURITY_FK, codMaturity20);
    //set the underlying cod maturity column value
    otolith.put(Otolith.MATURITY_NO, 10);
    //maturity foreign key value should be removed
    assertNull(otolith.get(Otolith.MATURITY_FK));
    //set the species column value
    otolith.put(Otolith.SPECIES_NO, 2);
    //species foreign key value should be removed
    assertNull(otolith.get(Otolith.SPECIES_FK));
    //set the maturity
    otolith.put(Otolith.MATURITY_FK, haddockMaturity10);
    otolith.put(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryHaddock200);
    //set the species column value to null, should remove both species and maturity fk values
    otolith.put(Otolith.SPECIES_NO, null);
    assertNull(otolith.get(Otolith.SPECIES_FK));
    assertNull(otolith.get(Otolith.MATURITY_FK));
    assertNull(otolith.get(Otolith.OTOLITH_CATEGORY_FK));
    //set the species column value to cod
    otolith.put(Otolith.SPECIES_NO, 1);
    //should be able to set the cod maturity and category
    otolith.put(Otolith.MATURITY_FK, codMaturity20);
    otolith.put(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryCod200);
    assertNull(otolith.get(Otolith.SPECIES_FK));
    assertNotNull(otolith.get(Otolith.MATURITY_FK));
    assertNotNull(otolith.get(Otolith.OTOLITH_CATEGORY_FK));
    otolith.put(Otolith.SPECIES_FK, cod);
    assertNotNull(otolith.get(Otolith.SPECIES_FK));
    assertNotNull(otolith.get(Otolith.MATURITY_FK));
    assertNotNull(otolith.get(Otolith.OTOLITH_CATEGORY_FK));
  }

  private Entity getDetailEntity(final long id, final Integer intValue, final Double doubleValue,
                                 final String stringValue, final LocalDate dateValue, final LocalDateTime timestampValue,
                                 final Boolean booleanValue, final Entity entityValue) {
    final Entity entity = ENTITIES.entity(Detail.TYPE);
    entity.put(Detail.ID, id);
    entity.put(Detail.INT, intValue);
    entity.put(Detail.DOUBLE, doubleValue);
    entity.put(Detail.STRING, stringValue);
    entity.put(Detail.DATE, dateValue);
    entity.put(Detail.TIMESTAMP, timestampValue);
    entity.put(Detail.BOOLEAN, booleanValue);
    entity.put(Detail.MASTER_FK, entityValue);

    return entity;
  }
}