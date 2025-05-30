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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Serializer;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.CompositeDetail;
import is.codion.framework.domain.TestDomain.CompositeMaster;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.TestDomain.ForeignKeyLazyColumn;
import is.codion.framework.domain.TestDomain.InvalidDerived;
import is.codion.framework.domain.TestDomain.Master;
import is.codion.framework.domain.TestDomain.NoPk;
import is.codion.framework.domain.TestDomain.NonCachedToString;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static java.util.Collections.emptyMap;
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

		assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, values, emptyMap()));

		Map<Attribute<?>, Object> originalValues = new HashMap<>();
		originalValues.put(Detail.BOOLEAN, false);
		originalValues.put(Master.CODE, 1);

		assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, emptyMap(), originalValues));

		Map<Attribute<?>, Object> invalidTypeValues = new HashMap<>();
		invalidTypeValues.put(Master.CODE, false);

		assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, invalidTypeValues, emptyMap()));

		Map<Attribute<?>, Object> invalidTypeOriginalValues = new HashMap<>();
		invalidTypeOriginalValues.put(Master.CODE, false);

		assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, emptyMap(), invalidTypeOriginalValues));

		EntityType entityType = TestDomain.DOMAIN.entityType("entityType");
		Attribute<?> invalid = entityType.integerAttribute("invalid");
		Map<Attribute<?>, Object> invalidAttributeValues = new HashMap<>();
		invalidAttributeValues.put(invalid, 1);

		assertThrows(IllegalArgumentException.class, () -> new DefaultEntity(masterDefinition, invalidAttributeValues, emptyMap()));
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
		entity.set(Detail.STRING, "a new String value");
		List<Object> fromFile = Serializer.deserialize(Serializer.serialize(singletonList(entity)));
		assertEquals(1, fromFile.size());
		Entity entityFromFile = (Entity) fromFile.get(0);
		assertEquals(Detail.TYPE, entity.type());
		assertTrue(entity.equalValues(entityFromFile));
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
		assertTrue(test.equalValues(testEntity), "Entity values should be equal after a calling set()");

		assertTrue(test.set(test).isEmpty());

		//assure that no cached foreign key values linger
		test.set(Detail.MASTER_FK, null);
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

		entity.set(Master.READ_ONLY, 1);

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
		assertTrue(original.equalValues(entity));

		original.set(Detail.BOOLEAN, true);
		entity.set(Detail.BOOLEAN, false);

		assertEquals(1, original.set(entity).size());
		assertTrue(original.equalValues(entity));

		original.set(Detail.INT, 1);
		entity.set(Detail.INT, 2);
		entity.set(Detail.INT, 3);//modified

		assertEquals(2, original.set(entity).size());//int + int_derived
		assertTrue(original.equalValues(entity));
		assertTrue(original.modified());
		assertTrue(entity.modified());

		original.set(Detail.DOUBLE, 1.2);
		original.set(Detail.STRING, "str");
		entity.set(Detail.DOUBLE, 1.3);
		entity.set(Detail.STRING, "strng");

		assertEquals(2, original.set(entity).size());
		assertTrue(original.equalValues(entity));

		assertEquals(0, original.set(entity).size());
		assertEquals(0, entity.set(original).size());

		entity.remove(Detail.STRING);
		assertFalse(entity.equalValues(original));
	}

	@Test
	void derivedOriginal() {
		Entity entity = ENTITIES.builder(Detail.TYPE)
						.with(Detail.ID, 0L)
						.with(Detail.INT, 1)
						.build();
		assertEquals(10, entity.get(Detail.INT_DERIVED));
		assertEquals(10, entity.original(Detail.INT_DERIVED));

		entity.set(Detail.INT, 2);
		assertEquals(10, entity.original(Detail.INT_DERIVED));
		assertEquals(20, entity.get(Detail.INT_DERIVED));

		entity.set(Detail.INT, 1);
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

		entity.set(Master.ID, -55L);
		//the id is not updatable as it is part of the primary key, which is not updatable by default
		assertFalse(entity.modified());
		entity.save(Master.ID);
		assertFalse(entity.modified());

		final String newName = "aname";

		entity.set(Master.NAME, newName);
		assertTrue(entity.modified());
		entity.revert(Master.NAME);
		assertEquals(masterName, entity.get(Master.NAME));
		assertFalse(entity.modified());

		entity.set(Master.NAME, newName);
		assertTrue(entity.modified());
		assertTrue(entity.modified(Master.NAME));
		entity.save(Master.NAME);
		assertEquals(newName, entity.get(Master.NAME));
		assertFalse(entity.modified());
		assertFalse(entity.modified(Master.NAME));

		Entity entity2 = ENTITIES.builder(Master.TYPE)
						.with(Master.NAME, "name")
						.build();
		entity2.set(Master.NAME, "newname");
		assertTrue(entity2.modified());
		assertEquals("name", entity2.original(Master.NAME));
		entity2.save(Master.NAME);
		entity2.set(Master.NAME, "name");
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
						.with(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 3)
						.build();

		Entity.Key referencedKey = compositeDetail.key(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK);
		Entity.Key cachedKey = compositeDetail.key(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK);

		assertSame(cachedKey, referencedKey);

		Entity master = ENTITIES.builder(Master.TYPE)
						.with(Master.CODE, 3)
						.build();

		Entity detail = ENTITIES.builder(Detail.TYPE)
						.with(Detail.MASTER_VIA_CODE_FK, master)
						.build();

		Entity.Key codeKey = detail.key(Detail.MASTER_VIA_CODE_FK);
		assertEquals(Integer.valueOf(3), codeKey.value());
		Entity.Key cachedCodeKey = detail.key(Detail.MASTER_VIA_CODE_FK);
		assertEquals(Integer.valueOf(3), cachedCodeKey.value());

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
		assertThrows(IllegalArgumentException.class, () -> detail.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, master));

		detail.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 3);
		detail.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, master);

		//otherwise the values are equal and put() returns before propagating foreign key values
		Entity masterCopy = master.copy().mutable();
		masterCopy.set(CompositeMaster.COMPOSITE_MASTER_ID, 1);
		masterCopy.set(CompositeMaster.COMPOSITE_MASTER_ID_2, null);
		detail.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, masterCopy);

		assertNull(detail.key(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));

		master.set(CompositeMaster.COMPOSITE_MASTER_ID, 1);
		master.set(CompositeMaster.COMPOSITE_MASTER_ID_2, 3);
		master.set(CompositeMaster.COMPOSITE_MASTER_ID_3, 3);

		Entity detail2 = ENTITIES.builder(CompositeDetail.TYPE)
						.with(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 3)
						.with(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, master)
						.build();

		assertEquals(3, detail2.get(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2));

		detail2.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2, null);
		assertTrue(detail2.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
		assertNull(detail2.key(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
	}

	@Test
	void noPrimaryKey() {
		Entity noPk = ENTITIES.builder(NoPk.TYPE)
						.with(NoPk.COL1, 1)
						.with(NoPk.COL2, 2)
						.with(NoPk.COL3, 3)
						.build();
		Entity.Key key = noPk.primaryKey();
		assertFalse(key.isNull());
		assertFalse(key.primary());
		Entity.Key originalKey = noPk.originalPrimaryKey();
		assertFalse(originalKey.isNull());
		assertFalse(originalKey.primary());
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
		assertEquals(detailId, testEntity.get(Detail.ID));
		assertTrue(testEntity.optional(Detail.ID).isPresent());
		assertEquals(detailInt, testEntity.get(Detail.INT));
		assertEquals(detailDouble, testEntity.get(Detail.DOUBLE));
		assertEquals(detailString, testEntity.get(Detail.STRING));
		assertEquals(detailDate, testEntity.get(Detail.DATE));
		assertEquals(detailTimestamp, testEntity.get(Detail.TIMESTAMP));
		assertEquals(detailBoolean, testEntity.get(Detail.BOOLEAN));
		assertEquals(referencedEntityValue, testEntity.get(Detail.MASTER_FK));
		assertEquals(masterName, testEntity.get(Detail.MASTER_NAME));
		assertEquals(7, testEntity.get(Detail.MASTER_CODE));
		assertFalse(testEntity.isNull(Detail.MASTER_ID));

		testEntity.key(Detail.MASTER_FK);

		//test copy()
		Entity test2 = testEntity.immutable().copy().mutable();
		assertNotSame(test2, testEntity, "Entity copy should not be == the original");
		assertEquals(test2, testEntity, "Entities should be equal after .getCopy()");
		assertTrue(test2.equalValues(testEntity), "Entity attribute values should be equal after deepCopy()");
		assertNotSame(testEntity.entity(Detail.MASTER_FK), test2.entity(Detail.MASTER_FK), "This should be a deep copy");

		test2.set(Detail.DOUBLE, 2.1);
		assertTrue(test2.modified());
		Entity test2Copy = test2.immutable();
		assertTrue(test2Copy.modified());

		//test propagate entity reference/denormalized values
		testEntity.set(Detail.MASTER_FK, null);
		assertTrue(testEntity.isNull(Detail.MASTER_ID));
		assertTrue(testEntity.isNull(Detail.MASTER_NAME));
		assertFalse(testEntity.optional(Detail.MASTER_NAME).isPresent());
		assertTrue(testEntity.isNull(Detail.MASTER_CODE));

		testEntity.set(Detail.MASTER_FK, referencedEntityValue);
		assertFalse(testEntity.isNull(Detail.MASTER_ID));
		assertEquals(testEntity.get(Detail.MASTER_ID),
						referencedEntityValue.get(Master.ID));
		assertEquals(testEntity.get(Detail.MASTER_NAME),
						referencedEntityValue.get(Master.NAME));
		assertEquals(testEntity.get(Detail.MASTER_CODE),
						referencedEntityValue.get(Master.CODE));

		referencedEntityValue.set(Master.CODE, 20);
		testEntity.set(Detail.MASTER_FK, referencedEntityValue);
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
		assertTrue(dept.equalValues(dept.copy().builder().build()));
		assertFalse(dept.equalValues(dept.copy().builder().with(Department.NAME, "new name").build()));

		dept.set(Department.NAME, "New name");
		assertTrue(dept.copy().builder().build().modified());
		assertFalse(dept.copy().builder().with(Department.NAME, "Name").build().modified());

		dept.set(Department.ID, 2);
		Entity entity = dept.copy().builder().originalPrimaryKey().build();
		assertEquals(1, entity.get(Department.ID));
	}

	@Test
	void referencedKeyIncorrectFk() {
		Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
						detailString, detailDate, detailTimestamp, detailBoolean, null);
		assertThrows(IllegalArgumentException.class, () ->
						testEntity.key(Employee.DEPARTMENT_FK));
	}

	@Test
	void isNull() {
		Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
						detailString, detailDate, detailTimestamp, detailBoolean, null);
		assertTrue(testEntity.isNull(Detail.MASTER_ID));
		assertTrue(testEntity.isNull(Detail.MASTER_FK));
		testEntity.set(Detail.MASTER_ID, 10L);

		assertNull(testEntity.get(Detail.MASTER_FK));
		Entity referencedEntityValue = testEntity.entity(Detail.MASTER_FK);
		assertEquals(10L, referencedEntityValue.get(Master.ID));
		assertFalse(testEntity.isNull(Detail.MASTER_FK));
		assertFalse(testEntity.isNull(Detail.MASTER_ID));

		Entity composite = ENTITIES.entity(CompositeDetail.TYPE);
		composite.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID, null);
		assertTrue(composite.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
		composite.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID, 1);
		assertTrue(composite.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
		composite.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2, null);
		assertTrue(composite.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
		composite.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2, 1);
		composite.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 2);
		assertFalse(composite.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
	}

	@Test
	void removeAll() {
		Entity referencedEntityValue = ENTITIES.entity(Master.TYPE);
		Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
						detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
		testEntity.set(Detail.STRING, "TestString");
		assertTrue(testEntity.modified());

		testEntity.set(null);
		assertTrue(testEntity.primaryKey().isNull());
		assertFalse(testEntity.contains(Detail.DATE));
		assertFalse(testEntity.contains(Detail.STRING));
		assertFalse(testEntity.contains(Detail.BOOLEAN));
		assertFalse(testEntity.modified());

		testEntity = detailEntity(detailId, detailInt, detailDouble,
						detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);

		testEntity = testEntity.copy().builder().clearPrimaryKey().build();
		assertTrue(testEntity.primaryKey().isNull());
		assertTrue(testEntity.contains(Detail.DATE));
		assertTrue(testEntity.contains(Detail.STRING));
		assertTrue(testEntity.contains(Detail.BOOLEAN));
	}

	@Test
	void setDenormalizedViewValue() {
		Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
						detailString, detailDate, detailTimestamp, detailBoolean, null);
		assertThrows(IllegalArgumentException.class, () -> testEntity.set(Detail.MASTER_NAME, "hello"));
	}

	@Test
	void setDenormalizedValue() {
		Entity testEntity = detailEntity(detailId, detailInt, detailDouble,
						detailString, detailDate, detailTimestamp, detailBoolean, null);
		assertThrows(IllegalArgumentException.class, () -> testEntity.set(Detail.MASTER_CODE, 2));
	}

	@Test
	void setValue() {
		Entity department = ENTITIES.builder(Department.TYPE)
						.with(Department.ID, -10)
						.build();

		Entity employee = ENTITIES.builder(Employee.TYPE)
						.with(Employee.COMMISSION, 1200d)
						.build();
		assertEquals(1200d, employee.get(Employee.COMMISSION));

		employee.set(Employee.DEPARTMENT_FK, department);
		assertEquals(department, employee.get(Employee.DEPARTMENT_FK));

		LocalDateTime date = LocalDateTime.now();
		employee.set(Employee.HIREDATE, date);
		assertEquals(date, employee.get(Employee.HIREDATE));

		employee.set(Employee.ID, 123);
		assertEquals(123, employee.get(Employee.ID));

		employee.set(Employee.NAME, "noname");
		assertEquals("noname", employee.get(Employee.NAME));

		//noinspection rawtypes
		assertThrows(IllegalArgumentException.class, () -> employee.set((Attribute) Employee.NAME, 42));
	}

	@Test
	void equalValues() {
		Entity testEntityOne = detailEntity(detailId, detailInt, detailDouble,
						detailString, detailDate, detailTimestamp, detailBoolean, null);
		Entity testEntityTwo = detailEntity(detailId, detailInt, detailDouble,
						detailString, detailDate, detailTimestamp, detailBoolean, null);

		assertTrue(testEntityOne.equalValues(testEntityTwo));

		testEntityTwo.set(Detail.INT, 42);
		assertFalse(testEntityOne.equalValues(testEntityTwo));

		testEntityOne.set(Detail.INT, 42);
		assertTrue(testEntityOne.equalValues(testEntityTwo));

		testEntityTwo.remove(Detail.INT);
		assertFalse(testEntityOne.equalValues(testEntityTwo));

		testEntityOne.remove(Detail.INT);
		assertTrue(testEntityOne.equalValues(testEntityTwo));

		Random random = new Random();
		byte[] bytes = new byte[1024];
		random.nextBytes(bytes);

		testEntityOne.set(Detail.BYTES, bytes);
		assertFalse(testEntityOne.equalValues(testEntityTwo));

		testEntityTwo.set(Detail.BYTES, bytes);
		assertTrue(testEntityOne.equalValues(testEntityTwo));

		assertThrows(IllegalArgumentException.class, () -> testEntityOne.equalValues(ENTITIES.entity(Master.TYPE)));
	}

	@Test
	void doubleValue() {
		Entity employee = ENTITIES.builder(Employee.TYPE)
						.with(Employee.ID, -10)
						.build();

		assertNull(employee.get(Employee.SALARY));

		final double salary = 1000.1234;
		employee.set(Employee.SALARY, salary);
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

		employee.set(Employee.DEPARTMENT_FK, department);
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
		assertNotNull(employee.entity(Employee.DEPARTMENT_FK));
		assertEquals(Integer.valueOf(-10), employee.get(Employee.DEPARTMENT_NO));

		employee.remove(Employee.DEPARTMENT_FK);
		assertNull(employee.get(Employee.DEPARTMENT_FK));
		Entity empDepartment = employee.entity(Employee.DEPARTMENT_FK);
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
		employee.set(Employee.COMMISSION, 1.1255);
		assertEquals(1.13, employee.get(Employee.COMMISSION));

		Entity detail = ENTITIES.builder(Detail.TYPE)
						.with(Detail.DOUBLE, 1.123456789567)
						.build();
		assertEquals(1.1234567896, detail.get(Detail.DOUBLE));//default 10 fraction digits
	}

	@Test
	void keyInvalidAttributeGet() {
		assertThrows(IllegalArgumentException.class, () -> ENTITIES.builder(Employee.TYPE).key().build().get(Employee.NAME));
	}

	@Test
	void transientAttributeModifiesEntity() throws IOException, ClassNotFoundException {
		Entity entity = ENTITIES.builder(TransModifies.TYPE)
						.with(TransModifies.ID, 42)
						.with(TransModifies.TRANS, null)
						.build();

		entity.set(TransModifies.TRANS, 1);
		assertTrue(entity.modified());

		Entity deserialized = Serializer.deserialize(Serializer.serialize(entity));
		assertTrue(deserialized.modified(TransModifies.TRANS));
		assertTrue(entity.modified());

		entity = ENTITIES.builder(TransModifiesNot.TYPE)
						.with(TransModifiesNot.ID, 42)
						.with(TransModifiesNot.TRANS, null)
						.build();

		entity.set(TransModifiesNot.TRANS, 1);
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
		emp.set(Employee.DEPARTMENT_NO, 2);
		assertNull(emp.get(Employee.DEPARTMENT_FK));
		Entity referencedDept = emp.entity(Employee.DEPARTMENT_FK);
		assertEquals(Integer.valueOf(2), referencedDept.primaryKey().value());

		Entity dept2 = ENTITIES.builder(Department.TYPE)
						.with(Department.ID, 3)
						.with(Department.NAME, "Name2")
						.build();
		emp.set(Employee.DEPARTMENT_FK, dept2);
		emp.set(Employee.DEPARTMENT_NO, 3);
		assertNotNull(emp.get(Employee.DEPARTMENT_FK));
		emp.set(Employee.DEPARTMENT_NO, 4);
		assertNull(emp.get(Employee.DEPARTMENT_FK));
		emp.set(Employee.DEPARTMENT_FK, dept2);
		assertNotNull(emp.get(Employee.DEPARTMENT_FK));
		emp.set(Employee.DEPARTMENT_NO, null);
		assertNull(emp.get(Employee.DEPARTMENT_FK));

		Entity manager = ENTITIES.builder(Employee.TYPE)
						.with(Employee.ID, 10)
						.with(Employee.DEPARTMENT_FK, dept)
						.build();
		emp.set(Employee.MANAGER_FK, manager);
		emp.set(Employee.DEPARTMENT_FK, dept2);

		Entity copy = emp.immutable();
		assertNotSame(emp, copy);
		assertTrue(emp.equalValues(copy));
		assertNotSame(emp.get(Employee.MANAGER_FK), copy.get(Employee.MANAGER_FK));
		assertTrue(emp.entity(Employee.MANAGER_FK).equalValues(copy.entity(Employee.MANAGER_FK)));
		assertNotSame(emp.entity(Employee.MANAGER_FK).entity(Employee.DEPARTMENT_FK),
						copy.entity(Employee.MANAGER_FK).entity(Employee.DEPARTMENT_FK));
		assertTrue(emp.entity(Employee.MANAGER_FK).entity(Employee.DEPARTMENT_FK)
						.equalValues(copy.entity(Employee.MANAGER_FK).entity(Employee.DEPARTMENT_FK)));

		emp.save();
		emp.definition().foreignKeys().get().forEach(foreignKey -> assertNotNull(emp.get(foreignKey)));
		emp.definition().foreignKeys().get().forEach(emp::remove);
		assertFalse(emp.modified());
		emp.definition().foreignKeys().get().forEach(foreignKey -> assertNull(emp.get(foreignKey)));
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
		assertThrows(IllegalArgumentException.class, () -> otolith.set(Otolith.MATURITY_FK, haddockMaturity10));
		assertThrows(IllegalArgumentException.class, () -> otolith.set(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryHaddock100));
		otolith.set(Otolith.MATURITY_FK, codMaturity10);
		otolith.set(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryCod100);
		//remove species, maturity and category should be removed
		otolith.set(Otolith.SPECIES_FK, null);
		//removes the invalid foreign key entity
		assertTrue(otolith.isNull(Otolith.MATURITY_FK));
		assertTrue(otolith.isNull(Otolith.OTOLITH_CATEGORY_FK));
		//should not remove the actual column value
		assertFalse(otolith.isNull(Otolith.MATURITY_NO));
		assertFalse(otolith.isNull(Otolith.OTOLITH_CATEGORY_NO));
		assertTrue(otolith.isNull(Otolith.SPECIES_NO));
		//try to set haddock maturity and category
		assertThrows(IllegalArgumentException.class, () -> otolith.set(Otolith.MATURITY_FK, haddockMaturity10));
		assertThrows(IllegalArgumentException.class, () -> otolith.set(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryHaddock100));
		//set the species to haddock
		otolith.set(Otolith.SPECIES_FK, haddock);
		otolith.set(Otolith.MATURITY_FK, haddockMaturity10);
		otolith.set(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryHaddock200);
		assertFalse(otolith.isNull(Otolith.MATURITY_FK));
		assertFalse(otolith.isNull(Otolith.OTOLITH_CATEGORY_FK));
		//set the species back to cod, haddock maturity should be removed
		otolith.set(Otolith.SPECIES_FK, cod);
		assertNull(otolith.get(Otolith.MATURITY_FK));
		assertNull(otolith.get(Otolith.OTOLITH_CATEGORY_FK));
		//set the cod maturity
		otolith.set(Otolith.MATURITY_FK, codMaturity20);
		//set the underlying cod maturity column value
		otolith.set(Otolith.MATURITY_NO, 10);
		//maturity foreign key value should be removed
		assertNull(otolith.get(Otolith.MATURITY_FK));
		//set the species column value
		otolith.set(Otolith.SPECIES_NO, 2);
		//species foreign key value should be removed
		assertNull(otolith.get(Otolith.SPECIES_FK));
		//set the maturity
		otolith.set(Otolith.MATURITY_FK, haddockMaturity10);
		otolith.set(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryHaddock200);
		//set the species column value to null, should remove both species and maturity fk values
		otolith.set(Otolith.SPECIES_NO, null);
		assertNull(otolith.get(Otolith.SPECIES_FK));
		assertNull(otolith.get(Otolith.MATURITY_FK));
		assertNull(otolith.get(Otolith.OTOLITH_CATEGORY_FK));
		//set the species column value to cod
		otolith.set(Otolith.SPECIES_NO, 1);
		//should be able to set the cod maturity and category
		otolith.set(Otolith.MATURITY_FK, codMaturity20);
		otolith.set(Otolith.OTOLITH_CATEGORY_FK, otolithCategoryCod200);
		assertNull(otolith.get(Otolith.SPECIES_FK));
		assertNotNull(otolith.get(Otolith.MATURITY_FK));
		assertNotNull(otolith.get(Otolith.OTOLITH_CATEGORY_FK));
		otolith.set(Otolith.SPECIES_FK, cod);
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
	void cacheToString() {
		Entity employee = ENTITIES.builder(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.NAME, "Name")
						.build();
		String toString = employee.toString();
		assertSame(toString, employee.toString());
		Entity entity = ENTITIES.builder(NonCachedToString.TYPE)
						.with(NonCachedToString.ID, 1)
						.with(NonCachedToString.STRING, "value")
						.build();
		toString = entity.toString();
		assertEquals(toString, entity.toString());
		assertNotSame(toString, entity.toString());
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
		employee.set(Employee.DEPARTMENT_FK, ENTITIES.builder(Department.TYPE)
						.with(Department.ID, 99)
						.with(Department.NAME, "Another")
						.build());

		Entity emp = employee.immutable();

		assertSame(emp, emp.immutable());

		assertThrows(UnsupportedOperationException.class, () -> emp.set(Department.ID, 2));
		assertThrows(UnsupportedOperationException.class, () -> emp.save(Department.ID));
		assertThrows(UnsupportedOperationException.class, emp::save);
		assertThrows(UnsupportedOperationException.class, () -> emp.revert(Department.ID));
		assertThrows(UnsupportedOperationException.class, emp::revert);
		assertThrows(UnsupportedOperationException.class, () -> emp.remove(Department.ID));
		assertThrows(UnsupportedOperationException.class, () -> emp.set(emp));

		Entity dept = emp.get(Employee.DEPARTMENT_FK);
		assertThrows(UnsupportedOperationException.class, () -> dept.set(Department.ID, 2));
		assertThrows(UnsupportedOperationException.class, () -> dept.save(Department.ID));
		assertThrows(UnsupportedOperationException.class, dept::save);
		assertThrows(UnsupportedOperationException.class, () -> dept.revert(Department.ID));
		assertThrows(UnsupportedOperationException.class, dept::revert);
		assertThrows(UnsupportedOperationException.class, () -> dept.remove(Department.ID));
		assertThrows(UnsupportedOperationException.class, () -> dept.set(dept));

		// Cyclical dependencies
		Entity manager1 = ENTITIES.builder(Employee.TYPE)
						.with(Employee.ID, 10)
						.with(Employee.NAME, "Man 1")
						.build();
		Entity manager2 = ENTITIES.builder(Employee.TYPE)
						.with(Employee.ID, 11)
						.with(Employee.NAME, "Man 2")
						.with(Employee.MANAGER_FK, manager1)
						.build();
		Entity manager3 = ENTITIES.builder(Employee.TYPE)
						.with(Employee.ID, 12)
						.with(Employee.NAME, "Man 3")
						.with(Employee.MANAGER_FK, manager2)
						.build();
		manager1.set(Employee.MANAGER_FK, manager3);

		manager1.immutable();

		manager1.set(Employee.MANAGER_FK, manager1);

		manager1.immutable();
	}

	@Test
	void exists() {
		Entity emp = ENTITIES.builder(Employee.TYPE)
						.with(Employee.ID, null)
						.build();
		assertTrue(emp.originalPrimaryKey().isNull());
		assertFalse(emp.exists());
		emp.set(Employee.ID, 1);
		assertTrue(emp.originalPrimaryKey().isNull());
		assertFalse(emp.exists());
		emp.save();
		assertTrue(emp.exists());
		emp.set(Employee.ID, 2);
		assertTrue(emp.exists());
		emp.save();
		assertTrue(emp.exists());
		emp.set(Employee.ID, null);
		assertTrue(emp.exists());
		emp.save();
		assertFalse(emp.exists());
		emp.remove(Employee.ID);
		assertFalse(emp.exists());

		emp = ENTITIES.entity(Employee.TYPE);
		assertFalse(emp.exists());
		emp.set(Employee.ID, 1);
		assertTrue(emp.exists());
		emp.set(Employee.ID, null);
		assertTrue(emp.exists());
		emp.save();
		assertFalse(emp.exists());

		emp = ENTITIES.builder(Employee.TYPE)
						.with(Employee.ID, 1)
						.build();
		assertTrue(emp.exists());
		emp.set(Employee.ID, null);
		assertTrue(emp.exists());
		emp.save();
		assertFalse(emp.exists());
	}

	@Test
	void foreignKeyLazyColumn() {
		Collection<Attribute<?>> selectedAttributes = ENTITIES.definition(ForeignKeyLazyColumn.TYPE).attributes().selected();
		assertFalse(selectedAttributes.contains(ForeignKeyLazyColumn.DEPARTMENT_FK));//based on a lazy loaded column
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

		aron.set(Employee.COMMISSION, 10.1234);
		// decimal fraction digits adjusted
		assertEquals(0, Double.compare(aron.get(Employee.COMMISSION), 10.12));
	}

	@Test
	void trim() {
		Entity aron = ENTITIES.builder(Employee.TYPE)
						.with(Employee.NAME, " Aron\n ")
						.build();
		assertEquals("Aron", aron.get(Employee.NAME));
	}

	@Test
	void cachedDerived() {
		DomainType domainType = DomainType.domainType("cached_derived");
		EntityType type = domainType.entityType("derived");
		Attribute<String> stringAttribute = type.stringAttribute("string");
		Attribute<String> derivedAttributeNonCached = type.stringAttribute("derived_non_cached");
		Attribute<String> derivedAttributeCached = type.stringAttribute("derived_cached");
		Attribute<UUID> derivedAttributeNoSource = type.attribute("derived_no_source", UUID.class);
		class DerivedDomain extends DomainModel {
			DerivedDomain() {
				super(domainType);
				add(type.define(
												stringAttribute.define()
																.attribute(),
												derivedAttributeCached.define()
																.derived(stringAttribute)
																.provider(values ->
																				values.get(stringAttribute) + "-derived"),
												derivedAttributeNonCached.define()
																.derived(stringAttribute)
																.provider(values ->
																				values.get(stringAttribute) + "-derived")
																.cached(false),
												derivedAttributeNoSource.define()
																.derived()
																.provider(values -> UUID.randomUUID()))
								.build());
			}
		}
		Entity entity = new DerivedDomain().entities().entity(type);
		entity.set(stringAttribute, "hello");
		String derivedCachedValue = entity.get(derivedAttributeCached);
		String derivedNonCachedValue = entity.get(derivedAttributeNonCached);
		// Cached instance
		assertSame(derivedCachedValue, entity.get(derivedAttributeCached));
		assertNotSame(derivedNonCachedValue, entity.get(derivedAttributeNonCached));

		entity.set(stringAttribute, "hello hello");
		derivedCachedValue = entity.get(derivedAttributeCached);
		assertSame(derivedCachedValue, entity.get(derivedAttributeCached));
		assertNotSame(derivedNonCachedValue, entity.get(derivedAttributeNonCached));

		entity.remove(derivedAttributeCached);
		assertNotSame(derivedCachedValue, entity.get(derivedAttributeCached));

		// Attributes based on no source values should not be cached
		assertNotEquals(entity.get(derivedAttributeNoSource), entity.get(derivedAttributeNoSource));
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