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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Serializer;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomainExtended;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.ItemValidationException;
import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static is.codion.framework.domain.TestDomain.*;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntitiesTest {

	private final Entities entities = new TestDomain().entities();

	@Test
	void defineTypes() {
		EntityDefinition definition = entities.definition(Detail.TYPE);

		//assert types
		assertEquals(Long.class, definition.columns().definition(Detail.ID).attribute().type().valueClass());
		assertEquals(Short.class, definition.columns().definition(Detail.SHORT).attribute().type().valueClass());
		assertEquals(Integer.class, definition.columns().definition(Detail.INT).attribute().type().valueClass());
		assertEquals(Double.class, definition.columns().definition(Detail.DOUBLE).attribute().type().valueClass());
		assertEquals(String.class, definition.columns().definition(Detail.STRING).attribute().type().valueClass());
		assertEquals(LocalDate.class, definition.columns().definition(Detail.DATE).attribute().type().valueClass());
		assertEquals(LocalDateTime.class, definition.columns().definition(Detail.TIMESTAMP).attribute().type().valueClass());
		assertEquals(Boolean.class, definition.columns().definition(Detail.BOOLEAN).attribute().type().valueClass());
		assertEquals(Entity.class, definition.foreignKeys().definition(Detail.MASTER_FK).attribute().type().valueClass());
		assertEquals(Long.class, definition.columns().definition(Detail.MASTER_ID).attribute().type().valueClass());
		assertEquals(String.class, definition.attributes().definition(Detail.MASTER_NAME).attribute().type().valueClass());
		assertEquals(Integer.class, definition.attributes().definition(Detail.MASTER_CODE).attribute().type().valueClass());

		//assert column names
		assertEquals(Detail.ID, definition.columns().definition(Detail.ID).attribute());
		assertEquals(Detail.SHORT, definition.columns().definition(Detail.SHORT).attribute());
		assertEquals(Detail.INT, definition.columns().definition(Detail.INT).attribute());
		assertEquals(Detail.DOUBLE, definition.columns().definition(Detail.DOUBLE).attribute());
		assertEquals(Detail.STRING, definition.columns().definition(Detail.STRING).attribute());
		assertEquals(Detail.DATE, definition.columns().definition(Detail.DATE).attribute());
		assertEquals(Detail.TIMESTAMP, definition.columns().definition(Detail.TIMESTAMP).attribute());
		assertEquals(Detail.BOOLEAN, definition.columns().definition(Detail.BOOLEAN).attribute());
		assertEquals(Detail.MASTER_ID, definition.columns().definition(Detail.MASTER_ID).attribute());
		assertEquals(Detail.MASTER_NAME, definition.attributes().definition(Detail.MASTER_NAME).attribute());
		assertEquals(Detail.MASTER_CODE, definition.attributes().definition(Detail.MASTER_CODE).attribute());

		//assert captions
		assertNotNull(definition.columns().definition(Detail.ID).caption());
		assertEquals(Detail.SHORT.name(), definition.columns().definition(Detail.SHORT).caption());
		assertEquals(Detail.INT.name(), definition.columns().definition(Detail.INT).caption());
		assertEquals(Detail.DOUBLE.name(), definition.columns().definition(Detail.DOUBLE).caption());
		assertEquals("Detail string", definition.columns().definition(Detail.STRING).caption());
		assertEquals(Detail.DATE.name(), definition.columns().definition(Detail.DATE).caption());
		assertEquals(Detail.TIMESTAMP.name(), definition.columns().definition(Detail.TIMESTAMP).caption());
		assertEquals(Detail.BOOLEAN.name(), definition.columns().definition(Detail.BOOLEAN).caption());
		assertEquals(Detail.MASTER_FK.name(), definition.foreignKeys().definition(Detail.MASTER_FK).caption());
		assertEquals(Detail.MASTER_NAME.name(), definition.attributes().definition(Detail.MASTER_NAME).caption());
		assertEquals(Detail.MASTER_CODE.name(), definition.attributes().definition(Detail.MASTER_CODE).caption());

		//assert hidden status
		assertTrue(definition.columns().definition(Detail.ID).hidden());
		assertFalse(definition.columns().definition(Detail.SHORT).hidden());
		assertFalse(definition.columns().definition(Detail.INT).hidden());
		assertFalse(definition.columns().definition(Detail.DOUBLE).hidden());
		assertFalse(definition.columns().definition(Detail.STRING).hidden());
		assertFalse(definition.columns().definition(Detail.DATE).hidden());
		assertFalse(definition.columns().definition(Detail.TIMESTAMP).hidden());
		assertFalse(definition.columns().definition(Detail.BOOLEAN).hidden());
		assertFalse(definition.foreignKeys().definition(Detail.MASTER_FK).hidden());
		assertFalse(definition.attributes().definition(Detail.MASTER_NAME).hidden());
		assertFalse(definition.attributes().definition(Detail.MASTER_CODE).hidden());
	}

	@Test
	void attributeWrongEntityType() {
		EntityDefinition definition = entities.definition(Detail.TYPE);
		assertThrows(IllegalArgumentException.class, () -> definition.columns().definition(Master.CODE));
	}

	@Test
	void updatableAttributes() {
		EntityDefinition definition = entities.definition(Detail.TYPE);
		Collection<AttributeDefinition<?>> attributes = definition.attributes().updatable();
		assertEquals(11, attributes.size());
		assertFalse(attributes.contains(definition.attributes().definition(Detail.MASTER_NAME)));
		assertFalse(attributes.contains(definition.attributes().definition(Detail.MASTER_CODE)));
		assertFalse(attributes.contains(definition.attributes().definition(Detail.INT_DERIVED)));
	}

	@Test
	void selectedAttributes() {
		List<Attribute<?>> attributes = new ArrayList<>();
		attributes.add(Department.ID);
		attributes.add(Department.NAME);

		EntityDefinition definition = entities.definition(Department.TYPE);
		Collection<AttributeDefinition<?>> definitions = attributes.stream()
						.map(definition.attributes()::definition)
						.collect(toList());
		assertEquals(2, definitions.size());
		assertTrue(definitions.contains(definition.columns().definition(Department.ID)));
		assertTrue(definitions.contains(definition.columns().definition(Department.NAME)));
	}

	@Test
	void key() {
		Entity.Key key = entities.keyBuilder(KeyTest.TYPE).build();
		assertEquals(0, key.hashCode());
		assertTrue(key.columns().isEmpty());
		assertTrue(key.isNull());

		assertThrows(IllegalStateException.class, () -> entities.primaryKey(KeyTest.TYPE, 1));
		assertThrows(NoSuchElementException.class, key::get);
		assertThrows(NoSuchElementException.class, key::optional);
		assertThrows(NoSuchElementException.class, key::column);

		key = key.copy()
						.with(KeyTest.ID1, 1)
						.with(KeyTest.ID2, 2)
						.with(KeyTest.ID3, 3)
						.build();
		assertTrue(key.isNotNull());
		assertEquals(6, key.hashCode());
		assertTrue(key.optional(KeyTest.ID1).isPresent());

		key = key.copy()
						.with(KeyTest.ID2, 3)
						.build();
		assertEquals(7, key.hashCode());

		key = key.copy()
						.with(KeyTest.ID3, null)
						.build();
		assertTrue(key.isNotNull());
		assertEquals(4, key.hashCode());
		key = key.copy()
						.with(KeyTest.ID2, null)
						.build();
		assertTrue(key.isNull());
		assertFalse(key.optional(KeyTest.ID2).isPresent());
		assertEquals(0, key.hashCode());
		key = key.copy()
						.with(KeyTest.ID2, 4)
						.build();
		assertTrue(key.optional(KeyTest.ID2).isPresent());
		assertTrue(key.isNotNull());
		assertEquals(5, key.hashCode());

		key = key.copy()
						.with(KeyTest.ID2, 42)
						.build();
		assertTrue(key.isNotNull());
		assertEquals(43, key.hashCode());

		assertThrows(NullPointerException.class, () -> entities.keyBuilder(null));

		assertFalse(entities.keyBuilder(NoPk.TYPE)
						.with(NoPk.COL1, 1)
						.build()
						.primary());
		Entity.Key noPk = entities.keyBuilder(NoPk.TYPE).build();
		assertThrows(IllegalArgumentException.class, () -> noPk.get(NoPk.COL1));
	}

	@Test
	void keys() {
		List<Entity.Key> intKeys = entities.primaryKeys(Employee.TYPE, 1, 2, 3, 4);
		assertEquals(4, intKeys.size());
		assertEquals(Integer.valueOf(3), intKeys.get(2).get());
		List<Entity.Key> longKeys = entities.primaryKeys(Detail.TYPE, 1L, 2L, 3L, 4L);
		assertEquals(4, longKeys.size());
		assertEquals(Long.valueOf(3), longKeys.get(2).get());
	}

	@Test
	void entity() {
		Entity.Key key = entities.primaryKey(Master.TYPE, 10L);

		Entity master = Entity.entity(key);
		assertEquals(Master.TYPE, master.entityType());
		assertTrue(master.contains(Master.ID));
		assertEquals(10L, master.get(Master.ID));

		assertThrows(NullPointerException.class, () -> entities.entity(null));
	}

	@Test
	void attributes() {
		EntityDefinition definition = entities.definition(Department.TYPE);
		AttributeDefinition<Integer> id = definition.columns().definition(Department.ID);
		AttributeDefinition<String> location = definition.columns().definition(Department.LOCATION);
		AttributeDefinition<String> name = definition.columns().definition(Department.NAME);
		AttributeDefinition<Boolean> active = definition.columns().definition(Department.ACTIVE);
		Collection<AttributeDefinition<?>> attributes = Stream.of(Department.LOCATION, Department.NAME)
						.map(definition.columns()::definition)
						.collect(toList());
		assertEquals(2, attributes.size());
		assertFalse(attributes.contains(id));
		assertTrue(attributes.contains(location));
		assertTrue(attributes.contains(name));

		attributes = definition.attributes().definitions().stream()
						.filter(ad -> !ad.hidden())
						.collect(toList());
		assertTrue(attributes.contains(id));
		assertTrue(attributes.contains(location));
		assertTrue(attributes.contains(name));
		assertFalse(attributes.contains(active));

		Collection<AttributeDefinition<?>> allAttributes = definition.attributes().definitions();
		assertTrue(allAttributes.contains(id));
		assertTrue(allAttributes.contains(location));
		assertTrue(allAttributes.contains(name));
		assertTrue(allAttributes.contains(active));
	}

	@Test
	void definitionInvalid() {
		assertThrows(IllegalArgumentException.class, () -> entities.definition(Master.TYPE)
						.attributes().definition(Master.TYPE.attribute("unknown attribute", Integer.class)));
	}

	@Test
	void foreignKeys() {
		EntityDefinition definition = entities.definition(Detail.TYPE);
		Collection<ForeignKey> foreignKeys = definition.foreignKeys().get(Employee.TYPE);
		assertEquals(0, foreignKeys.size());
		foreignKeys = definition.foreignKeys().get(Master.TYPE);
		assertEquals(2, foreignKeys.size());
		assertTrue(foreignKeys.contains(Detail.MASTER_FK));
	}

	@Test
	void foreignKeyAttribute() {
		assertNotNull(entities.definition(Detail.TYPE).foreignKeys().definition(Detail.MASTER_FK));
	}

	@Test
	void foreignKeyAttributeInvalid() {
		ForeignKey foreignKey = Detail.TYPE.foreignKey("bla bla", Detail.MASTER_ID, Master.ID);
		assertThrows(IllegalArgumentException.class, () -> entities.definition(Detail.TYPE).foreignKeys().definition(foreignKey));
	}

	@Test
	void hasDerivedAttributes() {
		EntityDefinition definition = entities.definition(Detail.TYPE);
		assertTrue(definition.attributes().derivedFrom(Detail.BOOLEAN).isEmpty());
		assertFalse(definition.attributes().derivedFrom(Detail.INT).isEmpty());
	}

	@Test
	void derivedAttributes() {
		EntityDefinition definition = entities.definition(Detail.TYPE);
		Collection<Attribute<?>> derivedAttributes = definition.attributes().derivedFrom(Detail.BOOLEAN);
		assertTrue(derivedAttributes.isEmpty());
		derivedAttributes = definition.attributes().derivedFrom(Detail.INT);
		assertEquals(1, derivedAttributes.size());
		assertTrue(derivedAttributes.contains(Detail.INT_DERIVED));
	}

	@Test
	void isSmallDataset() {
		assertTrue(entities.definition(Detail.TYPE).smallDataset());
	}

	@Test
	void stringFactory() {
		assertNotNull(entities.definition(Department.TYPE).stringFactory());
	}

	@Test
	void nullValidation() {
		Entity emp = entities.builder(Employee.TYPE)
						.with(Employee.NAME, "Name")
						.with(Employee.HIREDATE, LocalDateTime.now())
						.with(Employee.SALARY, 1200.0)
						.build();

		DefaultEntityValidator validator = new DefaultEntityValidator();
		try {
			validator.validate(emp);
			fail();
		}
		catch (ValidationException e) {
			assertInstanceOf(NullValidationException.class, e);
			assertEquals(Employee.DEPARTMENT_FK, e.attribute());
		}
		emp.put(Employee.DEPARTMENT_NO, 1);
		try {
			validator.validate(emp);
		}
		catch (ValidationException e) {
			fail();
		}
		emp.put(Employee.SALARY, null);
		try {
			validator.validate(emp);
			fail();
		}
		catch (ValidationException e) {
			assertInstanceOf(NullValidationException.class, e);
			assertEquals(Employee.SALARY, e.attribute());
		}
	}

	@Test
	void maxLengthValidation() {
		Entity emp = entities.builder(Employee.TYPE)
						.with(Employee.DEPARTMENT_NO, 1)
						.with(Employee.NAME, "Name")
						.with(Employee.HIREDATE, LocalDateTime.now())
						.with(Employee.SALARY, 1200.0)
						.build();
		DefaultEntityValidator validator = new DefaultEntityValidator();
		assertDoesNotThrow(() -> validator.validate(emp));
		emp.put(Employee.NAME, "LooooongName");
		assertThrows(LengthValidationException.class, () -> validator.validate(emp));
	}

	@Test
	void rangeValidation() {
		Entity emp = entities.builder(Employee.TYPE)
						.with(Employee.DEPARTMENT_NO, 1)
						.with(Employee.NAME, "Name")
						.with(Employee.HIREDATE, LocalDateTime.now())
						.with(Employee.SALARY, 1200d)
						.with(Employee.COMMISSION, 300d)
						.build();
		DefaultEntityValidator validator = new DefaultEntityValidator();
		assertDoesNotThrow(() -> validator.validate(emp));
		emp.put(Employee.COMMISSION, 10d);
		assertThrows(RangeValidationException.class, () -> validator.validate(emp));
		emp.put(Employee.COMMISSION, 2100d);
		assertThrows(RangeValidationException.class, () -> validator.validate(emp));
	}

	@Test
	void itemValidation() {
		Map<Attribute<?>, Object> values = new HashMap<>();
		values.put(Employee.NAME, "Name");
		values.put(Employee.DEPARTMENT_NO, 1);
		values.put(Employee.JOB, "CLREK");
		Entity emp = entities.definition(Employee.TYPE).entity(values);
		DefaultEntityValidator validator = new DefaultEntityValidator();
		assertThrows(ItemValidationException.class, () -> validator.validate(emp));
	}

	@Test
	void strictValidation() {
		Entity emp = entities.builder(Employee.TYPE)
						.with(Employee.NAME, "1234567891000")
						.with(Employee.DEPARTMENT_NO, 1)
						.with(Employee.JOB, "CLERK")
						.with(Employee.SALARY, 1200d)
						.with(Employee.HIREDATE, LocalDateTime.now())
						.build();
		DefaultEntityValidator validator = new DefaultEntityValidator();
		assertThrows(LengthValidationException.class, () -> validator.validate(emp));
		emp.put(Employee.NAME, "Name");
		emp.save();

		emp.put(Employee.ID, 10);//now it "exists"
		emp.put(Employee.NAME, "1234567891000");
		assertThrows(LengthValidationException.class, () -> validator.validate(emp));
		emp.save();//but not modified
		validator.validate(emp);

		DefaultEntityValidator validator2 = new DefaultEntityValidator(true);

		assertThrows(LengthValidationException.class, () -> validator2.validate(emp));
		emp.put(Employee.NAME, "Name");
		emp.save();

		emp.put(Employee.ID, 10);//now it "exists"
		emp.put(Employee.NAME, "1234567891000");
		assertThrows(LengthValidationException.class, () -> validator2.validate(emp));
		emp.save();//but not modified
		assertThrows(LengthValidationException.class, () -> validator2.validate(emp));//strict
	}

	@Test
	void searchable() {
		EntityDefinition definition = entities.definition(Employee.TYPE);
		Collection<Column<String>> searchable = definition.columns().searchable();
		assertTrue(searchable.contains(Employee.JOB));
		assertTrue(searchable.contains(Employee.NAME));

		searchable = entities.definition(Department.TYPE).columns().searchable();
		//should contain all string based columns
		assertTrue(searchable.contains(Department.NAME));
	}

	@Test
	void validateTypeEntity() {
		Entity entity = entities.entity(Detail.TYPE);
		Entity entity1 = entities.entity(Detail.TYPE);
		assertThrows(IllegalArgumentException.class, () -> entity.put(Detail.MASTER_FK, entity1));
	}

	@Test
	void setValueDerived() {
		Entity entity = entities.entity(Detail.TYPE);
		assertThrows(IllegalArgumentException.class, () -> entity.put(Detail.INT_DERIVED, 10));
	}

	@Test
	void setValueItem() {
		Entity entity = entities.entity(Detail.TYPE);
		assertThrows(IllegalArgumentException.class, () -> entity.put(Detail.INT_ITEMS, -10));
	}

	@Test
	void copyEntities() {
		Entity dept1 = entities.builder(Department.TYPE)
						.with(Department.ID, 1)
						.with(Department.LOCATION, "location")
						.with(Department.NAME, "name")
						.build();
		Entity dept2 = entities.builder(Department.TYPE)
						.with(Department.ID, 2)
						.with(Department.LOCATION, "location2")
						.with(Department.NAME, "name2")
						.build();

		Iterator<Entity> copies = Stream.of(dept1, dept2)
						.map(Entity::immutable)
						.collect(toList())
						.iterator();
		Entity dept1Copy = copies.next();
		Entity dept2Copy = copies.next();
		assertNotSame(dept1Copy, dept1);
		assertTrue(dept1Copy.equalValues(dept1));
		assertNotSame(dept2Copy, dept2);
		assertTrue(dept2Copy.equalValues(dept2));

		Entity emp1 = entities.builder(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept1)
						.with(Employee.NAME, "name")
						.with(Employee.COMMISSION, 130.5)
						.build();

		Entity copy = emp1.copy().mutable();
		assertTrue(emp1.equalValues(copy));
		assertSame(emp1.get(Employee.DEPARTMENT_FK), copy.get(Employee.DEPARTMENT_FK));
		assertFalse(emp1.modified());

		copy = copy.immutable();
		assertNotSame(emp1.get(Employee.DEPARTMENT_FK), copy.get(Employee.DEPARTMENT_FK));

		copy = emp1.immutable();
		assertFalse(copy.mutable());
		assertTrue(emp1.equalValues(copy));
		assertNotSame(emp1.get(Employee.DEPARTMENT_FK), copy.get(Employee.DEPARTMENT_FK));
		assertFalse(emp1.modified());
	}

	@Test
	void extendedDomain() {
		TestDomainExtended extended = new TestDomainExtended();
		Entities entities = extended.entities();

		entities.entity(TestDomainExtended.T_EXTENDED);

		entities.entity(CompositeMaster.TYPE);

		TestDomainExtended.TestDomainSecondExtension second = new TestDomainExtended.TestDomainSecondExtension();
		entities = second.entities();

		entities.entity(TestDomainExtended.TestDomainSecondExtension.T_SECOND_EXTENDED);

		entities.entity(TestDomainExtended.T_EXTENDED);

		entities.entity(CompositeMaster.TYPE);

		assertNotNull(second.procedure(TestDomainExtended.PROC_TYPE));
		assertNotNull(second.function(TestDomainExtended.FUNC_TYPE));
		assertNotNull(second.report(TestDomainExtended.REP_TYPE));

		//entity type name clash
		assertThrows(IllegalArgumentException.class, TestDomainExtended.TestDomainThirdExtension::new);
	}

	@Test
	void transients() throws IOException, ClassNotFoundException {
		EntityDefinition definition = entities.definition(Employee.TYPE);
		assertNotNull(definition.tableName());
		assertNotNull(definition.selectTableName());
		assertNotNull(definition.primaryKey().generator());
		assertTrue(definition.optimisticLocking());
		assertTrue(definition.selectQuery().isPresent());
		assertNotNull(definition.condition(Employee.CONDITION));
		ColumnDefinition<String> nameDefinition = definition.columns().definition(Employee.NAME);
		assertNotNull(nameDefinition.name());
		assertNotNull(nameDefinition.expression());
		EntityDefinition deserialized = Serializer.deserialize(Serializer.serialize(definition));
		assertNull(deserialized.tableName());
		assertNull(deserialized.selectTableName());
		assertNull(deserialized.primaryKey().generator());
		assertFalse(deserialized.optimisticLocking());
		assertFalse(deserialized.selectQuery().isPresent());
		assertThrows(IllegalArgumentException.class, () -> deserialized.condition(Employee.CONDITION));
		nameDefinition = deserialized.columns().definition(Employee.NAME);
		assertNull(nameDefinition.name());
		assertNull(nameDefinition.expression());
	}
}
