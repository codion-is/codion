/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Serializer;
import is.codion.framework.domain.TestDomain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    final EntityDefinition masterDefinition = ENTITIES.getDefinition(TestDomain.Master.TYPE);

    final Map<Attribute<?>, Object> values = new HashMap<>();
    values.put(TestDomain.Detail.BOOLEAN, false);
    values.put(TestDomain.Master.CODE, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, values, null));

    final Map<Attribute<?>, Object> originalValues = new HashMap<>();
    originalValues.put(TestDomain.Detail.BOOLEAN, false);
    originalValues.put(TestDomain.Master.CODE, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, null, originalValues));

    final Map<Attribute<?>, Object> invalidTypeValues = new HashMap<>();
    invalidTypeValues.put(TestDomain.Master.CODE, false);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, invalidTypeValues, null));

    final Map<Attribute<?>, Object> invalidTypeOriginalValues = new HashMap<>();
    invalidTypeOriginalValues.put(TestDomain.Master.CODE, false);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, null, invalidTypeOriginalValues));

    final EntityType<Entity> entityType = TestDomain.DOMAIN.entityType("entityType");
    final Attribute<?> invalid = entityType.integerAttribute("invalid");
    final Map<Attribute<?>, Object> invalidPropertyValues = new HashMap<>();
    invalidPropertyValues.put(invalid, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, invalidPropertyValues, null));
  }

  @Test
  public void serialization() throws Exception {
    final Entity referencedEntityValue = ENTITIES.entity(TestDomain.Master.TYPE);
    referencedEntityValue.put(TestDomain.Master.ID, 1L);
    referencedEntityValue.put(TestDomain.Master.NAME, "name");
    referencedEntityValue.put(TestDomain.Master.CODE, 10);
    final String originalStringValue = "string value";
    final Entity entity = getDetailEntity(10, 34, 23.4, originalStringValue, LocalDate.now(),
            LocalDateTime.now(), true, referencedEntityValue);
    entity.put(TestDomain.Detail.STRING, "a new String value");
    final List<Object> fromFile = Serializer.deserialize(Serializer.serialize(singletonList(entity)));
    assertEquals(1, fromFile.size());
    final Entity entityFromFile = (Entity) fromFile.get(0);
    assertTrue(entity.getEntityType().equals(TestDomain.Detail.TYPE));
    assertTrue(entity.valuesEqual(entityFromFile));
    assertTrue(entityFromFile.isModified());
    assertTrue(entityFromFile.isModified(TestDomain.Detail.STRING));
    assertEquals(originalStringValue, entityFromFile.getOriginal(TestDomain.Detail.STRING));

    final Key key = entity.getKey();
    final byte[] serialized = Serializer.serialize(singletonList(key));
    final List<Object> keyFromFile = Serializer.deserialize(serialized);
    assertEquals(1, keyFromFile.size());
    assertEquals(key, keyFromFile.get(0));

    final Entity master = ENTITIES.entity(TestDomain.Master.TYPE);
    master.put(TestDomain.Master.ID, 1L);
    master.put(TestDomain.Master.CODE, 11);

    final Entity masterDeserialized = Serializer.deserialize(Serializer.serialize(master));
    assertEquals(master.get(TestDomain.Master.ID), masterDeserialized.get(TestDomain.Master.ID));
    assertEquals(master.get(TestDomain.Master.CODE), masterDeserialized.get(TestDomain.Master.CODE));
    assertFalse(masterDeserialized.containsKey(TestDomain.Master.NAME));
  }

  @Test
  public void setAs() {
    final Entity referencedEntityValue = ENTITIES.entity(TestDomain.Master.TYPE);
    referencedEntityValue.put(TestDomain.Master.ID, 2L);
    referencedEntityValue.put(TestDomain.Master.NAME, masterName);
    referencedEntityValue.put(TestDomain.Master.CODE, 7);

    final Entity test = ENTITIES.entity(TestDomain.Detail.TYPE);
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    test.setAs(testEntity);
    assertEquals(test, testEntity, "Entities should be equal after .setAs()");
    assertTrue(test.valuesEqual(testEntity), "Entity property values should be equal after .setAs()");

    //assure that no cached foreign key values linger
    test.put(TestDomain.Detail.MASTER_FK, null);
    testEntity.setAs(test);
    assertNull(testEntity.get(TestDomain.Detail.MASTER_ID));
    assertNull(testEntity.get(TestDomain.Detail.MASTER_FK));

    assertThrows(IllegalArgumentException.class, () -> testEntity.setAs(referencedEntityValue));
  }

  @Test
  public void saveRevertValue() {
    final Entity entity = ENTITIES.entity(TestDomain.Master.TYPE);
    final String newName = "aname";

    entity.put(TestDomain.Master.ID, 2L);
    entity.put(TestDomain.Master.NAME, masterName);
    entity.put(TestDomain.Master.CODE, 7);

    entity.put(TestDomain.Master.ID, -55L);
    //the id is not updatable as it is part of the primary key, which is not updatable by default
    assertFalse(entity.isModified());
    entity.save(TestDomain.Master.ID);
    assertFalse(entity.isModified());

    entity.put(TestDomain.Master.NAME, newName);
    assertTrue(entity.isModified());
    entity.revert(TestDomain.Master.NAME);
    assertEquals(masterName, entity.get(TestDomain.Master.NAME));
    assertFalse(entity.isModified());

    entity.put(TestDomain.Master.NAME, newName);
    assertTrue(entity.isModified());
    assertTrue(entity.isModified(TestDomain.Master.NAME));
    entity.save(TestDomain.Master.NAME);
    assertEquals(newName, entity.get(TestDomain.Master.NAME));
    assertFalse(entity.isModified());
    assertFalse(entity.isModified(TestDomain.Master.NAME));
  }

  @Test
  public void getReferencedKeyCache() {
    final Entity detail = ENTITIES.entity(TestDomain.T_COMPOSITE_DETAIL);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID, 1);
    detail.put(TestDomain.COMPOSITE_DETAIL_MASTER_ID_2, 2);

    final Key referencedKey = detail.getReferencedKey(TestDomain.COMPOSITE_DETAIL_MASTER_FK);
    final Key cachedKey = detail.getReferencedKey(TestDomain.COMPOSITE_DETAIL_MASTER_FK);

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
    final Key key = ENTITIES.key(TestDomain.Detail.TYPE);
    assertTrue(key.isNull());
    key.put(null);
    assertTrue(key.isNull());
    key.put(1L);
    assertTrue(key.isNotNull());
  }

  @Test
  public void compositeKeySingleValueConstructor() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultKey(ENTITIES.getDefinition(TestDomain.T_COMPOSITE_MASTER), 1));
  }

  @Test
  public void noPrimaryKey() {
    final Entity noPk = ENTITIES.entity(TestDomain.T_NO_PK);
    noPk.put(TestDomain.NO_PK_COL1, 1);
    noPk.put(TestDomain.NO_PK_COL2, 2);
    noPk.put(TestDomain.NO_PK_COL3, 3);
    final Key key = noPk.getKey();
    assertTrue(key.isNull());
    final Key originalKey = noPk.getOriginalKey();
    assertTrue(originalKey.isNull());
  }

  @Test
  public void entity() throws Exception {
    final Entity referencedEntityValue = ENTITIES.entity(TestDomain.Master.TYPE);
    //assert not modified
    assertFalse(referencedEntityValue.isModified());

    referencedEntityValue.put(TestDomain.Master.ID, 2L);
    referencedEntityValue.put(TestDomain.Master.NAME, masterName);
    referencedEntityValue.put(TestDomain.Master.CODE, 7);

    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);

    //assert values
    assertEquals(testEntity.get(TestDomain.Detail.ID), detailId);
    assertTrue(testEntity.getOptional(TestDomain.Detail.ID).isPresent());
    assertEquals(testEntity.get(TestDomain.Detail.INT), detailInt);
    assertEquals(testEntity.get(TestDomain.Detail.DOUBLE), detailDouble);
    assertEquals(testEntity.get(TestDomain.Detail.STRING), detailString);
    assertEquals(testEntity.get(TestDomain.Detail.DATE), detailDate);
    assertEquals(testEntity.get(TestDomain.Detail.TIMESTAMP), detailTimestamp);
    assertEquals(testEntity.get(TestDomain.Detail.BOOLEAN), detailBoolean);
    assertEquals(testEntity.get(TestDomain.Detail.MASTER_FK), referencedEntityValue);
    assertEquals(testEntity.get(TestDomain.Detail.MASTER_NAME), masterName);
    assertEquals(testEntity.get(TestDomain.Detail.MASTER_CODE), 7);
    assertFalse(testEntity.isNull(TestDomain.Detail.MASTER_ID));
    assertTrue(testEntity.isNotNull(TestDomain.Detail.MASTER_ID));

    testEntity.getReferencedKey(TestDomain.Detail.MASTER_FK);

    //test copy()
    final Entity test2 = ENTITIES.deepCopyEntity(testEntity);
    assertNotSame(test2, testEntity, "Entity copy should not be == the original");
    assertEquals(test2, testEntity, "Entities should be equal after .getCopy()");
    assertTrue(test2.valuesEqual(testEntity), "Entity property values should be equal after .getCopy()");
    assertNotSame(testEntity.getForeignKey(TestDomain.Detail.MASTER_FK), test2.getForeignKey(TestDomain.Detail.MASTER_FK), "This should be a deep copy");

    test2.put(TestDomain.Detail.DOUBLE, 2.1);
    assertTrue(test2.isModified());
    final Entity test2Copy = ENTITIES.copyEntity(test2);
    assertTrue(test2Copy.isModified());

    //test propagate entity reference/denormalized values
    testEntity.put(TestDomain.Detail.MASTER_FK, null);
    assertTrue(testEntity.isNull(TestDomain.Detail.MASTER_ID));
    assertTrue(testEntity.isNull(TestDomain.Detail.MASTER_NAME));
    assertFalse(testEntity.getOptional(TestDomain.Detail.MASTER_NAME).isPresent());
    assertTrue(testEntity.isNull(TestDomain.Detail.MASTER_CODE));
    assertTrue(testEntity.isNull(TestDomain.Detail.MASTER_CODE_DENORM));

    testEntity.put(TestDomain.Detail.MASTER_FK, referencedEntityValue);
    assertFalse(testEntity.isNull(TestDomain.Detail.MASTER_ID));
    assertEquals(testEntity.get(TestDomain.Detail.MASTER_ID),
            referencedEntityValue.get(TestDomain.Master.ID));
    assertEquals(testEntity.get(TestDomain.Detail.MASTER_NAME),
            referencedEntityValue.get(TestDomain.Master.NAME));
    assertEquals(testEntity.get(TestDomain.Detail.MASTER_CODE),
            referencedEntityValue.get(TestDomain.Master.CODE));
    assertEquals(testEntity.get(TestDomain.Detail.MASTER_CODE_DENORM),
            referencedEntityValue.get(TestDomain.Master.CODE));

    referencedEntityValue.put(TestDomain.Master.CODE, 20);
    testEntity.put(TestDomain.Detail.MASTER_FK, referencedEntityValue);
    assertEquals(testEntity.get(TestDomain.Detail.MASTER_CODE),
            referencedEntityValue.get(TestDomain.Master.CODE));
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
    assertTrue(testEntity.isNull(TestDomain.Detail.MASTER_ID));
    assertTrue(testEntity.isNull(TestDomain.Detail.MASTER_FK));
    assertTrue(testEntity.isForeignKeyNull(TestDomain.Detail.MASTER_FK));
    testEntity.put(TestDomain.Detail.MASTER_ID, 10L);

    assertFalse(testEntity.isLoaded(TestDomain.Detail.MASTER_FK));
    final Entity referencedEntityValue = testEntity.getForeignKey(TestDomain.Detail.MASTER_FK);
    assertEquals(10L, referencedEntityValue.get(TestDomain.Master.ID));
    assertFalse(testEntity.isLoaded(TestDomain.Detail.MASTER_FK));
    assertFalse(testEntity.isNull(TestDomain.Detail.MASTER_FK));
    assertFalse(testEntity.isNull(TestDomain.Detail.MASTER_ID));

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
    final Entity referencedEntityValue = ENTITIES.entity(TestDomain.Master.TYPE);
    Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    testEntity.put(TestDomain.Detail.STRING, "TestString");
    assertTrue(testEntity.isModified());

    testEntity.setAs(null);
    assertTrue(testEntity.getKey().isNull());
    assertFalse(testEntity.containsKey(TestDomain.Detail.DATE));
    assertFalse(testEntity.containsKey(TestDomain.Detail.STRING));
    assertFalse(testEntity.containsKey(TestDomain.Detail.BOOLEAN));
    assertFalse(testEntity.isModified());

    testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);

    testEntity.clearKeyValues();
    assertTrue(testEntity.getKey().isNull());
    assertTrue(testEntity.containsKey(TestDomain.Detail.DATE));
    assertTrue(testEntity.containsKey(TestDomain.Detail.STRING));
    assertTrue(testEntity.containsKey(TestDomain.Detail.BOOLEAN));
  }

  @Test
  public void putDenormalizedViewValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.put(TestDomain.Detail.MASTER_NAME, "hello"));
  }

  @Test
  public void putDenormalizedValue() {
    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.put(TestDomain.Detail.MASTER_CODE, 2));
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
  public void propertyValuesEqual() {
    final Entity testEntityOne = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    final Entity testEntityTwo = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);

    assertTrue(testEntityOne.valuesEqual(testEntityTwo));

    testEntityTwo.put(TestDomain.Detail.INT, 42);

    assertFalse(testEntityOne.valuesEqual(testEntityTwo));
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

    final Entity detail = ENTITIES.entity(TestDomain.Detail.TYPE);
    detail.put(TestDomain.Detail.INT, 42);
    assertEquals("420", detail.getAsString(TestDomain.Detail.INT_DERIVED));
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

    final Entity detail = ENTITIES.entity(TestDomain.Detail.TYPE);
    detail.put(TestDomain.Detail.DOUBLE, 1.123456789567);
    assertEquals(1.1234567896, detail.get(TestDomain.Detail.DOUBLE));//default 10 fraction digits
  }

  @Test
  public void keyInvalidPropertyGet() {
    final Key empKey1 = ENTITIES.key(TestDomain.Employee.TYPE);
    assertThrows(IllegalArgumentException.class, () -> empKey1.get(TestDomain.Employee.NAME));
  }

  @Test
  public void keyInvalidPropertyPut() {
    final Key empKey1 = ENTITIES.key(TestDomain.Employee.TYPE);
    assertThrows(IllegalArgumentException.class, () -> empKey1.put("test"));
  }

  @Test
  public void keyEquality() {
    final List<Key> keys = ENTITIES.keys(TestDomain.Employee.TYPE, 1, 2);
    final Key empKey1 = keys.get(0);
    final Key empKey2 = keys.get(1);
    assertNotEquals(empKey1, empKey2);

    empKey2.put(1);
    assertEquals(empKey1, empKey2);

    final Key deptKey = ENTITIES.key(TestDomain.Department.TYPE, 1);
    assertNotEquals(empKey1, deptKey);

    final Key compMasterKey = ENTITIES.key(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    assertEquals(compMasterKey, compMasterKey);
    assertNotEquals(empKey1, compMasterKey);
    assertNotEquals(compMasterKey, new Object());

    final Key compMasterKey2 = ENTITIES.key(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    assertNotEquals(compMasterKey, compMasterKey2);

    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    assertEquals(compMasterKey, compMasterKey2);

    final Key detailKey = ENTITIES.key(TestDomain.Detail.TYPE, 1L);
    final Key detailKey2 = ENTITIES.key(TestDomain.Detail.TYPE, 2L);
    assertNotEquals(detailKey, detailKey2);

    detailKey2.put(1L);
    assertEquals(detailKey2, detailKey);
  }

  @Test
  public void nullKeyEquals() {
    final Key nullKey = ENTITIES.key(TestDomain.Employee.TYPE);
    final Key zeroKey = ENTITIES.key(TestDomain.Employee.TYPE, 0);
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
    assertEquals(Integer.valueOf(2), empDept.getKey().get());

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
    assertTrue(emp.valuesEqual(copy));
    assertNotSame(emp.get(TestDomain.Employee.MANAGER_FK), copy.get(TestDomain.Employee.MANAGER_FK));
    assertTrue(emp.getForeignKey(TestDomain.Employee.MANAGER_FK).valuesEqual(copy.getForeignKey(TestDomain.Employee.MANAGER_FK)));
    assertNotSame(emp.getForeignKey(TestDomain.Employee.MANAGER_FK).getForeignKey(TestDomain.Employee.DEPARTMENT_FK),
            copy.getForeignKey(TestDomain.Employee.MANAGER_FK).getForeignKey(TestDomain.Employee.DEPARTMENT_FK));
    assertTrue(emp.getForeignKey(TestDomain.Employee.MANAGER_FK).getForeignKey(TestDomain.Employee.DEPARTMENT_FK)
            .valuesEqual(copy.getForeignKey(TestDomain.Employee.MANAGER_FK).getForeignKey(TestDomain.Employee.DEPARTMENT_FK)));
  }

  private Entity getDetailEntity(final long id, final Integer intValue, final Double doubleValue,
                                 final String stringValue, final LocalDate dateValue, final LocalDateTime timestampValue,
                                 final Boolean booleanValue, final Entity entityValue) {
    final Entity entity = ENTITIES.entity(TestDomain.Detail.TYPE);
    entity.put(TestDomain.Detail.ID, id);
    entity.put(TestDomain.Detail.INT, intValue);
    entity.put(TestDomain.Detail.DOUBLE, doubleValue);
    entity.put(TestDomain.Detail.STRING, stringValue);
    entity.put(TestDomain.Detail.DATE, dateValue);
    entity.put(TestDomain.Detail.TIMESTAMP, timestampValue);
    entity.put(TestDomain.Detail.BOOLEAN, booleanValue);
    entity.put(TestDomain.Detail.MASTER_FK, entityValue);

    return entity;
  }
}