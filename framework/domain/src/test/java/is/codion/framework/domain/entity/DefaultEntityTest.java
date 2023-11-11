/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Serializer;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.CompositeDetail;
import is.codion.framework.domain.TestDomain.CompositeMaster;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.TestDomain.InvalidDerived;
import is.codion.framework.domain.TestDomain.Master;
import is.codion.framework.domain.TestDomain.NoPk;
import is.codion.framework.domain.TestDomain.NullString;
import is.codion.framework.domain.TestDomain.TransModifies;
import is.codion.framework.domain.TestDomain.TransModifiesNot;
import is.codion.framework.domain.entity.ForeignKeyDomain.Maturity;
import is.codion.framework.domain.entity.ForeignKeyDomain.Otolith;
import is.codion.framework.domain.entity.ForeignKeyDomain.OtolithCategory;
import is.codion.framework.domain.entity.ForeignKeyDomain.Species;
import is.codion.framework.domain.entity.attribute.Attribute;

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

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void construction() {
    EntityDefinition masterDefinition = ENTITIES.definition(Master.TYPE);

    Map<Attribute<?>, Object> values = new HashMap<>();
    values.put(Detail.BOOLEAN, false);
    values.put(Master.CODE, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, values, null));

    Map<Attribute<?>, Object> originalValues = new HashMap<>();
    originalValues.put(Detail.BOOLEAN, false);
    originalValues.put(Master.CODE, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, null, originalValues));

    Map<Attribute<?>, Object> invalidTypeValues = new HashMap<>();
    invalidTypeValues.put(Master.CODE, false);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, invalidTypeValues, null));

    Map<Attribute<?>, Object> invalidTypeOriginalValues = new HashMap<>();
    invalidTypeOriginalValues.put(Master.CODE, false);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, null, invalidTypeOriginalValues));

    EntityType entityType = TestDomain.DOMAIN.entityType("entityType");
    Attribute<?> invalid = entityType.integerAttribute("invalid");
    Map<Attribute<?>, Object> invalidAttributeValues = new HashMap<>();
    invalidAttributeValues.put(invalid, 1);

    assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, invalidAttributeValues, null));
  }

  @Test
  void serialization() throws Exception {
    Entity referencedEntityValue = ENTITIES.builder(Master.TYPE)
            .with(Master.ID, 1L)
            .with(Master.NAME, "name")
            .with(Master.CODE, 10)
            .build();
    final String originalStringValue = "string value";
    Entity entity = detailEntity(10, 34, 23.4, originalStringValue, LocalDate.now(),
            LocalDateTime.now(), true, referencedEntityValue);
    entity.put(Detail.STRING, "a new String value");
    List<Object> fromFile = Serializer.deserialize(Serializer.serialize(singletonList(entity)));
    assertEquals(1, fromFile.size());
    Entity entityFromFile = (Entity) fromFile.get(0);
    assertEquals(Detail.TYPE, entity.entityType());
    assertTrue(entity.columnValuesEqual(entityFromFile));
    assertTrue(entityFromFile.modified());
    assertTrue(entityFromFile.modified(Detail.STRING));
    assertEquals(originalStringValue, entityFromFile.original(Detail.STRING));

    Entity.Key key = entity.primaryKey();
    byte[] serialized = Serializer.serialize(singletonList(key));
    List<Object> keyFromFile = Serializer.deserialize(serialized);
    assertEquals(1, keyFromFile.size());
    assertEquals(key, keyFromFile.get(0));

    Entity master = ENTITIES.builder(Master.TYPE)
            .with(Master.ID, 1L)
            .with(Master.CODE, 11)
            .build();

    Entity masterDeserialized = Serializer.deserialize(Serializer.serialize(master));
    assertEquals(master.get(Master.ID), masterDeserialized.get(Master.ID));
    assertEquals(master.get(Master.CODE), masterDeserialized.get(Master.CODE));
    assertFalse(masterDeserialized.contains(Master.NAME));

    masterDeserialized = Serializer.deserialize(Serializer.serialize(master.immutable()));
    assertEquals(master.get(Master.ID), masterDeserialized.get(Master.ID));
    assertEquals(master.get(Master.CODE), masterDeserialized.get(Master.CODE));
    assertFalse(masterDeserialized.contains(Master.NAME));
  }

  @Test
  void set() {
    Entity referencedEntityValue = ENTITIES.builder(Master.TYPE)
            .with(Master.ID, 2L)
            .with(Master.NAME, masterName)
            .with(Master.CODE, 7)
            .build();

    Entity test = ENTITIES.entity(Detail.TYPE);
    Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    test.set(testEntity);
    assertEquals(test, testEntity, "Entities should be equal after a calling set()");
    assertTrue(test.columnValuesEqual(testEntity), "Entity values should be equal after a calling set()");

    assertTrue(test.set(test).isEmpty());

    //assure that no cached foreign key values linger
    test.put(Detail.MASTER_FK, null);
    testEntity.set(test);
    assertNull(testEntity.get(Detail.MASTER_ID));
    assertNull(testEntity.get(Detail.MASTER_FK));

    assertThrows(IllegalArgumentException.class, () -> testEntity.set(referencedEntityValue));

    Entity entity = ENTITIES.builder(Master.TYPE)
            .with(Master.ID, 2L)
            .with(Master.NAME, masterName)
            .with(Master.CODE, 7)
            .with(Master.READ_ONLY, 42)
            .build();

    entity.put(Master.READ_ONLY, 1);

    Entity setAsEntity = ENTITIES.entity(Master.TYPE);
    setAsEntity.set(entity);

    assertTrue(setAsEntity.modified(Master.READ_ONLY));
  }

  @Test
  void setAffectedAttributes() {
    Entity original = ENTITIES.builder(Detail.TYPE)
            .with(Detail.ID, 1L)
            .build();

    Entity entity = ENTITIES.builder(Detail.TYPE)
            .with(Detail.ID, 1L)
            .build();

    assertEquals(0, original.set(entity).size());
    assertTrue(Entity.valuesEqual(original, entity));

    original.put(Detail.BOOLEAN, true);
    entity.put(Detail.BOOLEAN, false);

    assertEquals(1, original.set(entity).size());
    assertTrue(Entity.valuesEqual(original, entity));

    original.put(Detail.INT, 1);
    entity.put(Detail.INT, 2);
    entity.put(Detail.INT, 3);//modified

    assertEquals(2, original.set(entity).size());//int + int_derived
    assertTrue(Entity.valuesEqual(original, entity));
    assertTrue(original.modified());
    assertTrue(entity.modified());

    original.put(Detail.DOUBLE, 1.2);
    original.put(Detail.STRING, "str");
    entity.put(Detail.DOUBLE, 1.3);
    entity.put(Detail.STRING, "strng");

    assertEquals(2, original.set(entity).size());
    assertTrue(Entity.valuesEqual(original, entity));

    assertEquals(0, original.set(entity).size());
    assertEquals(0, entity.set(original).size());

    entity.remove(Detail.STRING);
    assertFalse(Entity.valuesEqual(entity, original));
  }

  @Test
  void derivedOriginal() {
    Entity entity = ENTITIES.builder(Detail.TYPE)
            .with(Detail.ID, 0L)
            .with(Detail.INT, 1)
            .build();
    assertEquals(10, entity.get(Detail.INT_DERIVED));
    assertEquals(10, entity.original(Detail.INT_DERIVED));

    entity.put(Detail.INT, 2);
    assertEquals(10, entity.original(Detail.INT_DERIVED));
    assertEquals(20, entity.get(Detail.INT_DERIVED));

    entity.put(Detail.INT, 1);
    assertEquals(10, entity.get(Detail.INT_DERIVED));
    assertEquals(10, entity.original(Detail.INT_DERIVED));

    Entity invalidDerived = ENTITIES.builder(InvalidDerived.TYPE)
            .with(InvalidDerived.ID, 0)
            .with(InvalidDerived.INT, 1)
            .build();

    assertThrows(IllegalArgumentException.class, () -> invalidDerived.get(InvalidDerived.INVALID_DERIVED));
  }

  @Test
  void saveRevertValue() {
    Entity entity = ENTITIES.builder(Master.TYPE)
            .with(Master.ID, 2L)
            .with(Master.NAME, masterName)
            .with(Master.CODE, 7)
            .build();

    entity.put(Master.ID, -55L);
    //the id is not updatable as it is part of the primary key, which is not updatable by default
    assertFalse(entity.modified());
    entity.save(Master.ID);
    assertFalse(entity.modified());

    final String newName = "aname";

    entity.put(Master.NAME, newName);
    assertTrue(entity.modified());
    entity.revert(Master.NAME);
    assertEquals(masterName, entity.get(Master.NAME));
    assertFalse(entity.modified());

    entity.put(Master.NAME, newName);
    assertTrue(entity.modified());
    assertTrue(entity.modified(Master.NAME));
    entity.save(Master.NAME);
    assertEquals(newName, entity.get(Master.NAME));
    assertFalse(entity.modified());
    assertFalse(entity.modified(Master.NAME));

    Entity entity2 = ENTITIES.builder(Master.TYPE)
            .with(Master.NAME, "name")
            .build();
    entity2.put(Master.NAME, "newname");
    assertTrue(entity2.modified());
    assertEquals("name", entity2.original(Master.NAME));
    entity2.save(Master.NAME);
    entity2.put(Master.NAME, "name");
    assertEquals("newname", entity2.original(Master.NAME));

    assertTrue(entity2.modified());
    entity2.revert();
    assertFalse(entity2.modified());
  }

  @Test
  void referencedKeyCache() {
    Entity compositeDetail = ENTITIES.builder(CompositeDetail.TYPE)
            .with(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID, 1)
            .with(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2, 2)
            .build();

    Entity.Key referencedKey = compositeDetail.referencedKey(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK);
    Entity.Key cachedKey = compositeDetail.referencedKey(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK);

    assertSame(cachedKey, referencedKey);

    Entity master = ENTITIES.builder(Master.TYPE)
            .with(Master.CODE, 3)
            .build();

    Entity detail = ENTITIES.builder(Detail.TYPE)
            .with(Detail.MASTER_VIA_CODE_FK, master)
            .build();

    Entity.Key codeKey = detail.referencedKey(Detail.MASTER_VIA_CODE_FK);
    assertEquals(Integer.valueOf(3), codeKey.get());
    Entity.Key cachedCodeKey = detail.referencedKey(Detail.MASTER_VIA_CODE_FK);
    assertEquals(Integer.valueOf(3), cachedCodeKey.get());

    assertSame(codeKey, cachedCodeKey);
  }

  @Test
  void compositeReferenceKey() {
    Entity master = ENTITIES.builder(CompositeMaster.TYPE)
            .with(CompositeMaster.COMPOSITE_MASTER_ID, null)
            .with(CompositeMaster.COMPOSITE_MASTER_ID_2, 2)
            .with(CompositeMaster.COMPOSITE_MASTER_ID_3, 3)
            .build();

    Entity detail = ENTITIES.builder(CompositeDetail.TYPE)
            .with(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 1)
            .build();
    //can not update read only attribute reference, with a different value
    assertThrows(IllegalArgumentException.class, () -> detail.put(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, master));

    detail.put(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 3);
    detail.put(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, master);

    //otherwise the values are equal and put() returns before propagating foreign key values
    Entity masterCopy = master.deepCopy();
    masterCopy.put(CompositeMaster.COMPOSITE_MASTER_ID, 1);
    masterCopy.put(CompositeMaster.COMPOSITE_MASTER_ID_2, null);
    detail.put(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, masterCopy);

    assertNull(detail.referencedKey(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));

    master.put(CompositeMaster.COMPOSITE_MASTER_ID, 1);
    master.put(CompositeMaster.COMPOSITE_MASTER_ID_2, 3);
    master.put(CompositeMaster.COMPOSITE_MASTER_ID_3, 3);

    Entity detail2 = ENTITIES.builder(CompositeDetail.TYPE)
            .with(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 3)
            .with(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, master)
            .build();

    assertEquals(3, detail2.get(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2));

    detail2.put(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2, null);
    assertTrue(detail2.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
    assertNull(detail2.referencedKey(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
  }

  @Test
  void noPrimaryKey() {
    Entity noPk = ENTITIES.builder(NoPk.TYPE)
            .with(NoPk.COL1, 1)
            .with(NoPk.COL2, 2)
            .with(NoPk.COL3, 3)
            .build();
    Entity.Key key = noPk.primaryKey();
    assertTrue(key.isNull());
    Entity.Key originalKey = noPk.originalPrimaryKey();
    assertTrue(originalKey.isNull());
  }

  @Test
  void entity() {
    Entity referencedEntityValue = ENTITIES.builder(Master.TYPE)
            .with(Master.ID, 2L)
            .with(Master.NAME, masterName)
            .with(Master.CODE, 7)
            .build();
    //assert not modified
    assertFalse(referencedEntityValue.modified());


    Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);

    //assert values
    assertEquals(testEntity.get(Detail.ID), detailId);
    assertTrue(testEntity.optional(Detail.ID).isPresent());
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

    testEntity.referencedKey(Detail.MASTER_FK);

    //test copy()
    Entity test2 = testEntity.deepCopy();
    assertNotSame(test2, testEntity, "Entity copy should not be == the original");
    assertEquals(test2, testEntity, "Entities should be equal after .getCopy()");
    assertTrue(test2.columnValuesEqual(testEntity), "Entity attribute values should be equal after .getCopy()");
    assertNotSame(testEntity.referencedEntity(Detail.MASTER_FK), test2.referencedEntity(Detail.MASTER_FK), "This should be a deep copy");

    test2.put(Detail.DOUBLE, 2.1);
    assertTrue(test2.modified());
    Entity test2Copy = test2.copy();
    assertTrue(test2Copy.modified());

    //cyclical deep copy
    Entity manager1 = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, 10)
            .build();
    Entity manager2 = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, 11)
            .with(Employee.MANAGER_FK, manager1)
            .build();
    Entity manager3 = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, 12)
            .with(Employee.MANAGER_FK, manager2)
            .build();
    manager1.put(Employee.MANAGER_FK, manager3);

    manager1.deepCopy();//stack overflow if not careful

    //test propagate entity reference/denormalized values
    testEntity.put(Detail.MASTER_FK, null);
    assertTrue(testEntity.isNull(Detail.MASTER_ID));
    assertTrue(testEntity.isNull(Detail.MASTER_NAME));
    assertFalse(testEntity.optional(Detail.MASTER_NAME).isPresent());
    assertTrue(testEntity.isNull(Detail.MASTER_CODE));

    testEntity.put(Detail.MASTER_FK, referencedEntityValue);
    assertFalse(testEntity.isNull(Detail.MASTER_ID));
    assertEquals(testEntity.get(Detail.MASTER_ID),
            referencedEntityValue.get(Master.ID));
    assertEquals(testEntity.get(Detail.MASTER_NAME),
            referencedEntityValue.get(Master.NAME));
    assertEquals(testEntity.get(Detail.MASTER_CODE),
            referencedEntityValue.get(Master.CODE));

    referencedEntityValue.put(Master.CODE, 20);
    testEntity.put(Detail.MASTER_FK, referencedEntityValue);
    assertEquals(testEntity.get(Detail.MASTER_CODE),
            referencedEntityValue.get(Master.CODE));
  }

  @Test
  void copyBuilder() {
    Entity dept = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 1)
            .with(Department.NAME, "Name")
            .with(Department.LOCATION, "Location")
            .with(Department.ACTIVE, true)
            .build();
    assertTrue(dept.columnValuesEqual(dept.copyBuilder().build()));
    assertFalse(dept.columnValuesEqual(dept.copyBuilder().with(Department.NAME, "new name").build()));

    dept.put(Department.NAME, "New name");
    assertTrue(dept.copyBuilder().build().modified());
    assertFalse(dept.copyBuilder().with(Department.NAME, "Name").build().modified());
  }

  @Test
  void referencedKeyIncorrectFk() {
    Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () ->
            testEntity.referencedKey(Employee.DEPARTMENT_FK));
  }

  @Test
  void isNull() {
    Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertTrue(testEntity.isNull(Detail.MASTER_ID));
    assertTrue(testEntity.isNull(Detail.MASTER_FK));
    testEntity.put(Detail.MASTER_ID, 10L);

    assertFalse(testEntity.loaded(Detail.MASTER_FK));
    Entity referencedEntityValue = testEntity.referencedEntity(Detail.MASTER_FK);
    assertEquals(10L, referencedEntityValue.get(Master.ID));
    assertFalse(testEntity.loaded(Detail.MASTER_FK));
    assertFalse(testEntity.isNull(Detail.MASTER_FK));
    assertFalse(testEntity.isNull(Detail.MASTER_ID));

    Entity composite = ENTITIES.entity(CompositeDetail.TYPE);
    composite.put(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID, null);
    assertTrue(composite.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
    composite.put(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID, 1);
    assertTrue(composite.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
    composite.put(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2, null);
    assertTrue(composite.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
    composite.put(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2, 1);
    composite.put(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 2);
    assertFalse(composite.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
  }

  @Test
  void removeAll() {
    Entity referencedEntityValue = ENTITIES.entity(Master.TYPE);
    Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    testEntity.put(Detail.STRING, "TestString");
    assertTrue(testEntity.modified());

    testEntity.set(null);
    assertTrue(testEntity.primaryKey().isNull());
    assertFalse(testEntity.contains(Detail.DATE));
    assertFalse(testEntity.contains(Detail.STRING));
    assertFalse(testEntity.contains(Detail.BOOLEAN));
    assertFalse(testEntity.modified());

    testEntity = detailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);

    testEntity.clearPrimaryKey();
    assertTrue(testEntity.primaryKey().isNull());
    assertTrue(testEntity.contains(Detail.DATE));
    assertTrue(testEntity.contains(Detail.STRING));
    assertTrue(testEntity.contains(Detail.BOOLEAN));
  }

  @Test
  void putDenormalizedViewValue() {
    Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.put(Detail.MASTER_NAME, "hello"));
  }

  @Test
  void putDenormalizedValue() {
    Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    assertThrows(IllegalArgumentException.class, () -> testEntity.put(Detail.MASTER_CODE, 2));
  }

  @Test
  void putValue() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, -10)
            .build();

    Entity employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.COMMISSION, 1200d)
            .build();
    assertEquals(employee.get(Employee.COMMISSION), 1200d);

    employee.put(Employee.DEPARTMENT_FK, department);
    assertEquals(employee.get(Employee.DEPARTMENT_FK), department);

    LocalDateTime date = LocalDateTime.now();
    employee.put(Employee.HIREDATE, date);
    assertEquals(employee.get(Employee.HIREDATE), date);

    employee.put(Employee.ID, 123);
    assertEquals(employee.get(Employee.ID), 123);

    employee.put(Employee.NAME, "noname");
    assertEquals(employee.get(Employee.NAME), "noname");

    //noinspection rawtypes
    assertThrows(IllegalArgumentException.class, () -> employee.put((Attribute) Employee.NAME, 42));
  }

  @Test
  void columnValuesEqual() {
    Entity testEntityOne = detailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, null);
    Entity testEntityTwo = detailEntity(detailId, detailInt, detailDouble,
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

    Random random = new Random();
    byte[] bytes = new byte[1024];
    random.nextBytes(bytes);

    testEntityOne.put(Detail.BYTES, bytes);
    assertFalse(testEntityOne.columnValuesEqual(testEntityTwo));

    testEntityTwo.put(Detail.BYTES, bytes);
    assertTrue(testEntityOne.columnValuesEqual(testEntityTwo));

    assertThrows(IllegalArgumentException.class, () -> testEntityOne.columnValuesEqual(ENTITIES.entity(Master.TYPE)));
  }

  @Test
  void doubleValue() {
    Entity employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, -10)
            .build();

    assertNull(employee.get(Employee.SALARY));

    final double salary = 1000.1234;
    employee.put(Employee.SALARY, salary);
    assertEquals(Double.valueOf(1000.12), employee.get(Employee.SALARY));
  }

  @Test
  void foreignKeyValue() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, -10)
            .build();
    Entity employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, -10)
            .build();
    assertTrue(employee.isNull(Employee.DEPARTMENT_FK));
    assertNull(employee.get(Employee.DEPARTMENT_FK));
    assertNull(employee.get(Employee.DEPARTMENT_NO));

    employee.put(Employee.DEPARTMENT_FK, department);
    assertFalse(employee.isNull(Employee.DEPARTMENT_FK));
    assertNotNull(employee.get(Employee.DEPARTMENT_FK));
    assertNotNull(employee.get(Employee.DEPARTMENT_NO));
  }

  @Test
  void derivedValue() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.NAME, "dname")
            .build();
    Entity employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.NAME, "ename")
            .with(Employee.DEPARTMENT_FK, department)
            .build();
    assertEquals("ename - dname", employee.get(Employee.DEPARTMENT_NAME));

    Entity detail = ENTITIES.builder(Detail.TYPE)
            .with(Detail.INT, 42)
            .build();
    assertEquals("420", detail.string(Detail.INT_DERIVED));
  }

  @Test
  void removeValue() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, -10)
            .build();
    Entity employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, -10)
            .with(Employee.DEPARTMENT_FK, department)
            .build();
    assertNotNull(employee.referencedEntity(Employee.DEPARTMENT_FK));
    assertEquals(Integer.valueOf(-10), employee.get(Employee.DEPARTMENT_NO));

    employee.remove(Employee.DEPARTMENT_FK);
    assertNull(employee.get(Employee.DEPARTMENT_FK));
    Entity empDepartment = employee.referencedEntity(Employee.DEPARTMENT_FK);
    assertNotNull(empDepartment);
    //non loaded entity, created from foreign key
    assertFalse(empDepartment.contains(Department.NAME));
    assertNotNull(employee.get(Employee.DEPARTMENT_NO));
    assertFalse(employee.contains(Employee.DEPARTMENT_FK));
    assertTrue(employee.contains(Employee.DEPARTMENT_NO));

    employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, -10)
            .with(Employee.DEPARTMENT_FK, department)
            .build();
    employee.remove(Employee.DEPARTMENT_NO);
    assertTrue(employee.isNull(Employee.DEPARTMENT_FK));
  }

  @Test
  void maximumFractionDigits() {
    Entity employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.COMMISSION, 1.1234)
            .build();
    assertEquals(1.12, employee.get(Employee.COMMISSION));
    employee.put(Employee.COMMISSION, 1.1255);
    assertEquals(1.13, employee.get(Employee.COMMISSION));

    Entity detail = ENTITIES.builder(Detail.TYPE)
            .with(Detail.DOUBLE, 1.123456789567)
            .build();
    assertEquals(1.1234567896, detail.get(Detail.DOUBLE));//default 10 fraction digits
  }

  @Test
  void keyInvalidAttributeGet() {
    assertThrows(IllegalArgumentException.class, () -> ENTITIES.keyBuilder(Employee.TYPE).build().get(Employee.NAME));
  }

  @Test
  void transientAttributeModifiesEntity() throws IOException, ClassNotFoundException {
    Entity entity = ENTITIES.builder(TransModifies.TYPE)
            .with(TransModifies.ID, 42)
            .with(TransModifies.TRANS, null)
            .build();

    entity.put(TransModifies.TRANS, 1);
    assertTrue(entity.modified());

    Entity deserialized = Serializer.deserialize(Serializer.serialize(entity));
    assertTrue(deserialized.modified(TransModifies.TRANS));
    assertTrue(entity.modified());

    entity = ENTITIES.builder(TransModifiesNot.TYPE)
            .with(TransModifiesNot.ID, 42)
            .with(TransModifiesNot.TRANS, null)
            .build();

    entity.put(TransModifiesNot.TRANS, 1);
    assertFalse(entity.modified());

    deserialized = Serializer.deserialize(Serializer.serialize(entity));
    assertTrue(deserialized.modified(TransModifiesNot.TRANS));
    assertFalse(deserialized.modified());
  }

  @Test
  void foreignKeyModification() {
    Entity dept = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 1)
            .with(Department.NAME, "Name1")
            .build();
    Entity emp = ENTITIES.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept)
            .build();
    assertEquals(1, emp.get(Employee.DEPARTMENT_NO));
    emp.put(Employee.DEPARTMENT_NO, 2);
    assertNull(emp.get(Employee.DEPARTMENT_FK));
    assertFalse(emp.loaded(Employee.DEPARTMENT_FK));
    Entity referencedDept = emp.referencedEntity(Employee.DEPARTMENT_FK);
    assertEquals(Integer.valueOf(2), referencedDept.primaryKey().get());

    Entity dept2 = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 3)
            .with(Department.NAME, "Name2")
            .build();
    emp.put(Employee.DEPARTMENT_FK, dept2);
    emp.put(Employee.DEPARTMENT_NO, 3);
    assertNotNull(emp.get(Employee.DEPARTMENT_FK));
    emp.put(Employee.DEPARTMENT_NO, 4);
    assertNull(emp.get(Employee.DEPARTMENT_FK));
    emp.put(Employee.DEPARTMENT_FK, dept2);
    assertNotNull(emp.get(Employee.DEPARTMENT_FK));
    emp.put(Employee.DEPARTMENT_NO, null);
    assertNull(emp.get(Employee.DEPARTMENT_FK));

    Entity manager = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, 10)
            .with(Employee.DEPARTMENT_FK, dept)
            .build();
    emp.put(Employee.MANAGER_FK, manager);
    emp.put(Employee.DEPARTMENT_FK, dept2);

    Entity copy = emp.deepCopy();
    assertNotSame(emp, copy);
    assertTrue(emp.columnValuesEqual(copy));
    assertNotSame(emp.get(Employee.MANAGER_FK), copy.get(Employee.MANAGER_FK));
    assertTrue(emp.referencedEntity(Employee.MANAGER_FK).columnValuesEqual(copy.referencedEntity(Employee.MANAGER_FK)));
    assertNotSame(emp.referencedEntity(Employee.MANAGER_FK).referencedEntity(Employee.DEPARTMENT_FK),
            copy.referencedEntity(Employee.MANAGER_FK).referencedEntity(Employee.DEPARTMENT_FK));
    assertTrue(emp.referencedEntity(Employee.MANAGER_FK).referencedEntity(Employee.DEPARTMENT_FK)
            .columnValuesEqual(copy.referencedEntity(Employee.MANAGER_FK).referencedEntity(Employee.DEPARTMENT_FK)));

    emp.save();
    emp.definition().foreignKeys().get().forEach(foreignKey -> assertTrue(emp.loaded(foreignKey)));
    emp.definition().foreignKeys().get().forEach(emp::remove);
    assertFalse(emp.modified());
    emp.definition().foreignKeys().get().forEach(foreignKey -> assertFalse(emp.loaded(foreignKey)));
  }

  @Test
  void readOnlyForeignKeyReferences() {
    ForeignKeyDomain domain = new ForeignKeyDomain();
    Entities entities = domain.entities();

    Entity cod = entities.builder(Species.TYPE)
            .with(Species.NO, 1)
            .with(Species.NAME, "Cod")
            .build();

    Entity codMaturity10 = entities.builder(Maturity.TYPE)
            .with(Maturity.SPECIES_FK, cod)
            .with(Maturity.NO, 10)
            .build();
    Entity codMaturity20 = entities.builder(Maturity.TYPE)
            .with(Maturity.SPECIES_FK, cod)
            .with(Maturity.NO, 20)
            .build();

    Entity haddock = entities.builder(Species.TYPE)
            .with(Species.NO, 2)
            .with(Species.NAME, "Haddock")
            .build();

    Entity haddockMaturity10 = entities.builder(Maturity.TYPE)
            .with(Maturity.SPECIES_FK, haddock)
            .with(Maturity.NO, 10)
            .build();

    Entity otolithCategoryCod100 = entities.builder(OtolithCategory.TYPE)
            .with(OtolithCategory.SPECIES_FK, cod)
            .with(OtolithCategory.NO, 100)
            .build();
    Entity otolithCategoryCod200 = entities.builder(OtolithCategory.TYPE)
            .with(OtolithCategory.SPECIES_FK, cod)
            .with(OtolithCategory.NO, 200)
            .build();

    Entity otolithCategoryHaddock100 = entities.builder(OtolithCategory.TYPE)
            .with(OtolithCategory.SPECIES_FK, haddock)
            .with(OtolithCategory.NO, 100)
            .build();
    Entity otolithCategoryHaddock200 = entities.builder(OtolithCategory.TYPE)
            .with(OtolithCategory.SPECIES_FK, haddock)
            .with(OtolithCategory.NO, 200)
            .build();

    Entity otolith = entities.builder(Otolith.TYPE)
            .with(Otolith.SPECIES_FK, cod)
            .build();
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

  @Test
  void nullString() {
    Entity entity = ENTITIES.builder(NullString.TYPE)
            .with(NullString.ID, 42)
            .with(NullString.ATTR, null)
            .build();
    assertEquals("null_string: id: 42, attr: null", entity.toString());
  }

  @Test
  void immutableEntity() {
    Entity employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, 1)
            .with(Employee.NAME, "Name")
            .with(Employee.DEPARTMENT_FK, ENTITIES.builder(Department.TYPE)
                    .with(Department.ID, 42)
                    .with(Department.NAME, "Dept name")
                    .build())
            .build();
    employee.put(Employee.DEPARTMENT_FK, ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 99)
            .with(Department.NAME, "Another")
            .build());

    Entity emp = employee.immutable();

    assertSame(emp, emp.immutable());

    assertThrows(UnsupportedOperationException.class, () -> emp.put(Department.ID, 2));
    assertThrows(UnsupportedOperationException.class, emp::clearPrimaryKey);
    assertThrows(UnsupportedOperationException.class, () -> emp.save(Department.ID));
    assertThrows(UnsupportedOperationException.class, emp::save);
    assertThrows(UnsupportedOperationException.class, () -> emp.revert(Department.ID));
    assertThrows(UnsupportedOperationException.class, emp::revert);
    assertThrows(UnsupportedOperationException.class, () -> emp.remove(Department.ID));
    assertThrows(UnsupportedOperationException.class, () -> emp.set(emp));

    Entity dept = emp.get(Employee.DEPARTMENT_FK);
    assertThrows(UnsupportedOperationException.class, () -> dept.put(Department.ID, 2));
    assertThrows(UnsupportedOperationException.class, dept::clearPrimaryKey);
    assertThrows(UnsupportedOperationException.class, () -> dept.save(Department.ID));
    assertThrows(UnsupportedOperationException.class, dept::save);
    assertThrows(UnsupportedOperationException.class, () -> dept.revert(Department.ID));
    assertThrows(UnsupportedOperationException.class, dept::revert);
    assertThrows(UnsupportedOperationException.class, () -> dept.remove(Department.ID));
    assertThrows(UnsupportedOperationException.class, () -> dept.set(dept));
  }

  @Test
  void exists() {
    Entity emp = ENTITIES.builder(Employee.TYPE)
            .with(Employee.NAME, "Name")
            .with(Employee.ID, null)
            .build();
    assertTrue(emp.originalPrimaryKey().isNull());
    assertFalse(emp.exists());
    emp.put(Employee.ID, 1);
    assertTrue(emp.exists());
    emp.save();
    emp.put(Employee.ID, 2);
    assertTrue(emp.exists());
    emp.remove(Employee.ID);
    assertFalse(emp.exists());
  }

  @Test
  void misc() {
    Entity aron = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, 42)
            .with(Employee.NAME, "Aron")
            .with(Employee.DEPARTMENT_NO, 1)
            .build();
    assertEquals("deptno:1", aron.string(Employee.DEPARTMENT_FK));
    assertEquals(42, aron.hashCode());

    Entity bjorn = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, 99)
            .with(Employee.NAME, "Björn")
            .build();

    assertEquals(-1, aron.compareTo(bjorn));
  }

  private static Entity detailEntity(long id, Integer intValue, Double doubleValue,
                                     String stringValue, LocalDate dateValue, LocalDateTime timestampValue,
                                     Boolean booleanValue, Entity entityValue) {
    return ENTITIES.builder(Detail.TYPE)
            .with(Detail.ID, id)
            .with(Detail.INT, intValue)
            .with(Detail.DOUBLE, doubleValue)
            .with(Detail.STRING, stringValue)
            .with(Detail.DATE, dateValue)
            .with(Detail.TIMESTAMP, timestampValue)
            .with(Detail.BOOLEAN, booleanValue)
            .with(Detail.MASTER_FK, entityValue)
            .build();
  }
}