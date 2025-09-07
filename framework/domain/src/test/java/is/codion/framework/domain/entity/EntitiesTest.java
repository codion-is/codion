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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

@DisplayName("Entities")
public final class EntitiesTest {

	private Entities entities;

	@BeforeEach
	void setUp() {
		entities = new TestDomain().entities();
	}

	@Nested
	@DisplayName("Entity Definition")
	class EntityDefinitionTest {

		@Test
		@DisplayName("definition defines attribute types correctly")
		void defineTypes_attributeTypes_definedCorrectly() {
			EntityDefinition definition = entities.definition(Detail.TYPE);

			// Column types
			assertEquals(Long.class, definition.columns().definition(Detail.ID).attribute().type().valueClass());
			assertEquals(Short.class, definition.columns().definition(Detail.SHORT).attribute().type().valueClass());
			assertEquals(Integer.class, definition.columns().definition(Detail.INT).attribute().type().valueClass());
			assertEquals(Double.class, definition.columns().definition(Detail.DOUBLE).attribute().type().valueClass());
			assertEquals(String.class, definition.columns().definition(Detail.STRING).attribute().type().valueClass());
			assertEquals(LocalDate.class, definition.columns().definition(Detail.DATE).attribute().type().valueClass());
			assertEquals(LocalDateTime.class, definition.columns().definition(Detail.TIMESTAMP).attribute().type().valueClass());
			assertEquals(Boolean.class, definition.columns().definition(Detail.BOOLEAN).attribute().type().valueClass());

			// Foreign key types
			assertEquals(Entity.class, definition.foreignKeys().definition(Detail.MASTER_FK).attribute().type().valueClass());
			assertEquals(Long.class, definition.columns().definition(Detail.MASTER_ID).attribute().type().valueClass());

			// Attribute types
			assertEquals(String.class, definition.attributes().definition(Detail.MASTER_NAME).attribute().type().valueClass());
			assertEquals(Integer.class, definition.attributes().definition(Detail.MASTER_CODE).attribute().type().valueClass());
		}

		@Test
		@DisplayName("definition maps attributes to correct columns")
		void defineTypes_attributeMapping_correct() {
			EntityDefinition definition = entities.definition(Detail.TYPE);

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
		}

		@Test
		@DisplayName("definition sets captions appropriately")
		void defineTypes_captions_setCorrectly() {
			EntityDefinition definition = entities.definition(Detail.TYPE);

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
		}

		@Test
		@DisplayName("definition sets hidden status correctly")
		void defineTypes_hiddenStatus_setCorrectly() {
			EntityDefinition definition = entities.definition(Detail.TYPE);

			// ID column should be hidden
			assertTrue(definition.columns().definition(Detail.ID).hidden());

			// Other columns should be visible
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
		@DisplayName("definition rejects attributes from wrong entity type")
		void attributeWrongEntityType_throwsException() {
			EntityDefinition definition = entities.definition(Detail.TYPE);
			assertThrows(IllegalArgumentException.class,
							() -> definition.columns().definition(Master.CODE),
							"Should reject attribute from different entity type");
		}

		@Test
		@DisplayName("definition with invalid attribute name throws exception")
		void definitionInvalid_unknownAttribute_throwsException() {
			assertThrows(IllegalArgumentException.class,
							() -> entities.definition(Master.TYPE)
											.attributes().definition(Master.TYPE.attribute("unknown attribute", Integer.class)),
							"Should reject unknown attribute");
		}

		@Test
		@DisplayName("smallDataset returns true for test entities")
		void isSmallDataset_returnsTrue() {
			assertTrue(entities.definition(Detail.TYPE).smallDataset());
		}

		@Test
		@DisplayName("formatter exists for Department entity")
		void formatter_exists() {
			assertNotNull(entities.definition(Department.TYPE).formatter());
		}
	}

	@Nested
	@DisplayName("Attributes")
	class AttributesTest {

		@Test
		@DisplayName("updatable attributes exclude derived and foreign key attributes")
		void updatableAttributes_excludesDerivedAndForeignKeyAttributes() {
			EntityDefinition definition = entities.definition(Detail.TYPE);
			Collection<AttributeDefinition<?>> attributes = definition.attributes().updatable();

			assertEquals(11, attributes.size());
			assertFalse(attributes.contains(definition.attributes().definition(Detail.MASTER_NAME)));
			assertFalse(attributes.contains(definition.attributes().definition(Detail.MASTER_CODE)));
			assertFalse(attributes.contains(definition.attributes().definition(Detail.INT_DERIVED)));
		}

		@Test
		@DisplayName("selected attributes returns requested attribute definitions")
		void selectedAttributes_returnsRequestedDefinitions() {
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
		@DisplayName("attributes method returns correct collections")
		void attributes_returnsCorrectCollections() {
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

			// Test non-hidden attributes
			attributes = definition.attributes().definitions().stream()
							.filter(ad -> !ad.hidden())
							.collect(toList());
			assertTrue(attributes.contains(id));
			assertTrue(attributes.contains(location));
			assertTrue(attributes.contains(name));
			assertFalse(attributes.contains(active));

			// Test all attributes
			Collection<AttributeDefinition<?>> allAttributes = definition.attributes().definitions();
			assertTrue(allAttributes.contains(id));
			assertTrue(allAttributes.contains(location));
			assertTrue(allAttributes.contains(name));
			assertTrue(allAttributes.contains(active));
		}

		@Test
		@DisplayName("searchable columns returns all string columns")
		void searchable_returnsAllStringColumns() {
			EntityDefinition definition = entities.definition(Employee.TYPE);
			Collection<Column<String>> searchable = definition.columns().searchable();
			assertTrue(searchable.contains(Employee.JOB));
			assertTrue(searchable.contains(Employee.NAME));

			searchable = entities.definition(Department.TYPE).columns().searchable();
			// Should contain all string based columns
			assertTrue(searchable.contains(Department.NAME));
		}
	}

	@Nested
	@DisplayName("Entity Key")
	class EntityKeyTest {

		@Test
		@DisplayName("empty key has correct default state")
		void key_emptyKey_hasCorrectDefaultState() {
			Entity.Key key = entities.key(KeyTest.TYPE).build();

			assertEquals(0, key.hashCode());
			assertTrue(key.columns().isEmpty());
			assertTrue(key.isNull());
		}

		@Test
		@DisplayName("empty key operations throw appropriate exceptions")
		void key_emptyKey_throwsExpectedExceptions() {
			Entity.Key key = entities.key(KeyTest.TYPE).build();

			assertThrows(IllegalStateException.class,
							() -> entities.primaryKey(KeyTest.TYPE, 1),
							"Should not create primary key for composite key entity");
			assertThrows(NoSuchElementException.class, key::value);
			assertThrows(NoSuchElementException.class, key::optional);
			assertThrows(NoSuchElementException.class, key::column);
		}

		@Test
		@DisplayName("key builder creates and modifies keys correctly")
		void key_builderOperations_workCorrectly() {
			// Build composite key
			Entity.Key key = entities.key(KeyTest.TYPE)
							.with(KeyTest.ID1, 1)
							.with(KeyTest.ID2, 2)
							.with(KeyTest.ID3, 3)
							.build();

			assertFalse(key.isNull());
			assertEquals(6, key.hashCode());
			assertTrue(key.optional(KeyTest.ID1).isPresent());

			// Update one value
			key = key.copy().with(KeyTest.ID2, 3).build();
			assertEquals(7, key.hashCode());

			// Set value to null
			key = key.copy().with(KeyTest.ID3, null).build();
			assertFalse(key.isNull());
			assertEquals(4, key.hashCode());

			// Set another value to null making key null
			key = key.copy().with(KeyTest.ID2, null).build();
			assertTrue(key.isNull());
			assertFalse(key.optional(KeyTest.ID2).isPresent());
			assertEquals(0, key.hashCode());

			// Add value back
			key = key.copy().with(KeyTest.ID2, 4).build();
			assertTrue(key.optional(KeyTest.ID2).isPresent());
			assertFalse(key.isNull());
			assertEquals(5, key.hashCode());

			// Update to different value
			key = key.copy().with(KeyTest.ID2, 42).build();
			assertFalse(key.isNull());
			assertEquals(43, key.hashCode());
		}

		@Test
		@DisplayName("non-primary key entity has primary() false")
		void key_nonPrimaryKey_primaryReturnsFalse() {
			Entity.Key key = entities.key(NoPk.TYPE)
							.with(NoPk.COL1, 1)
							.build();

			assertFalse(key.primary());
		}

		@Test
		@DisplayName("accessing undefined column throws exception")
		void key_undefinedColumn_throwsException() {
			Entity.Key noPk = entities.key(NoPk.TYPE).build();

			assertThrows(IllegalArgumentException.class,
							() -> noPk.get(NoPk.COL1),
							"Should not access undefined column in key");
		}

		@Test
		@DisplayName("primaryKeys creates list of keys from values")
		void keys_primaryKeys_createsListCorrectly() {
			List<Entity.Key> intKeys = entities.primaryKeys(Employee.TYPE, 1, 2, 3, 4);
			assertEquals(4, intKeys.size());
			assertEquals(Integer.valueOf(3), intKeys.get(2).value());

			List<Entity.Key> longKeys = entities.primaryKeys(Detail.TYPE, 1L, 2L, 3L, 4L);
			assertEquals(4, longKeys.size());
			assertEquals(Long.valueOf(3), longKeys.get(2).value());
		}

		@Test
		@DisplayName("entity created from key contains key values")
		void entity_fromKey_containsKeyValues() {
			Entity.Key key = entities.primaryKey(Master.TYPE, 10L);

			Entity master = Entity.entity(key);
			assertEquals(Master.TYPE, master.type());
			assertTrue(master.contains(Master.ID));
			assertEquals(10L, master.get(Master.ID));
		}

		@Test
		@DisplayName("entity from null key throws exception")
		void entity_nullKey_throwsException() {
			assertThrows(NullPointerException.class,
							() -> entities.entity(null),
							"Should not create entity from null key");
		}
	}


	@Nested
	@DisplayName("Foreign Keys")
	class ForeignKeysTest {

		@Test
		@DisplayName("foreignKeys returns keys by referenced entity type")
		void foreignKeys_byReferencedType_returnsCorrectKeys() {
			EntityDefinition definition = entities.definition(Detail.TYPE);

			// No foreign keys to Employee type
			Collection<ForeignKey> foreignKeys = definition.foreignKeys().get(Employee.TYPE);
			assertEquals(0, foreignKeys.size());

			// Two foreign keys to Master type
			foreignKeys = definition.foreignKeys().get(Master.TYPE);
			assertEquals(2, foreignKeys.size());
			assertTrue(foreignKeys.contains(Detail.MASTER_FK));
		}

		@Test
		@DisplayName("foreignKey definition exists for valid foreign key")
		void foreignKeyAttribute_validKey_returnsDefinition() {
			assertNotNull(entities.definition(Detail.TYPE).foreignKeys().definition(Detail.MASTER_FK));
		}

		@Test
		@DisplayName("foreignKey definition throws exception for invalid key")
		void foreignKeyAttributeInvalid_unknownKey_throwsException() {
			ForeignKey foreignKey = Detail.TYPE.foreignKey("bla bla", Detail.MASTER_ID, Master.ID);

			assertThrows(IllegalArgumentException.class,
							() -> entities.definition(Detail.TYPE).foreignKeys().definition(foreignKey),
							"Should reject unknown foreign key");
		}
	}

	@Nested
	@DisplayName("Derived Attributes")
	class DerivedAttributesTest {

		@Test
		@DisplayName("hasDerivedAttributes correctly identifies source attributes")
		void hasDerivedAttributes_identifiesSourceAttributes() {
			EntityDefinition definition = entities.definition(Detail.TYPE);

			// BOOLEAN has no derived attributes
			assertTrue(definition.attributes().derivedFrom(Detail.BOOLEAN).isEmpty());

			// INT has derived attributes
			assertFalse(definition.attributes().derivedFrom(Detail.INT).isEmpty());
		}

		@Test
		@DisplayName("derivedAttributes returns correct derived attributes")
		void derivedAttributes_returnsCorrectAttributes() {
			EntityDefinition definition = entities.definition(Detail.TYPE);

			// BOOLEAN has no derived attributes
			Collection<Attribute<?>> derivedAttributes = definition.attributes().derivedFrom(Detail.BOOLEAN);
			assertTrue(derivedAttributes.isEmpty());

			// INT has INT_DERIVED
			derivedAttributes = definition.attributes().derivedFrom(Detail.INT);
			assertEquals(1, derivedAttributes.size());
			assertTrue(derivedAttributes.contains(Detail.INT_DERIVED));
		}
	}

	@Nested
	@DisplayName("Validation")
	class ValidationTest {

		@Test
		@DisplayName("null validation catches missing required values")
		void nullValidation_missingRequiredValues_throwsException() {
			Entity emp = entities.entity(Employee.TYPE)
							.with(Employee.NAME, "Name")
							.with(Employee.HIREDATE, LocalDateTime.now())
							.with(Employee.SALARY, 1200.0)
							.build();

			DefaultEntityValidator validator = new DefaultEntityValidator();

			// Missing required foreign key
			ValidationException exception = assertThrows(ValidationException.class,
							() -> validator.validate(emp));
			assertInstanceOf(NullValidationException.class, exception);
			assertEquals(Employee.DEPARTMENT_FK, exception.attribute());

			// Fix foreign key
			emp.set(Employee.DEPARTMENT_NO, 1);
			assertDoesNotThrow(() -> validator.validate(emp));

			// Remove required salary
			emp.set(Employee.SALARY, null);
			exception = assertThrows(ValidationException.class,
							() -> validator.validate(emp));
			assertInstanceOf(NullValidationException.class, exception);
			assertEquals(Employee.SALARY, exception.attribute());
		}

		@Test
		@DisplayName("length validation enforces maximum length")
		void maxLengthValidation_exceedsMaxLength_throwsException() {
			Entity emp = entities.entity(Employee.TYPE)
							.with(Employee.DEPARTMENT_NO, 1)
							.with(Employee.NAME, "Name")
							.with(Employee.HIREDATE, LocalDateTime.now())
							.with(Employee.SALARY, 1200.0)
							.build();

			DefaultEntityValidator validator = new DefaultEntityValidator();
			assertDoesNotThrow(() -> validator.validate(emp));

			// Exceed max length
			emp.set(Employee.NAME, "LooooongName");
			assertThrows(LengthValidationException.class,
							() -> validator.validate(emp));
		}

		@Test
		@DisplayName("range validation enforces min and max values")
		void rangeValidation_outsideRange_throwsException() {
			Entity emp = entities.entity(Employee.TYPE)
							.with(Employee.DEPARTMENT_NO, 1)
							.with(Employee.NAME, "Name")
							.with(Employee.HIREDATE, LocalDateTime.now())
							.with(Employee.SALARY, 1200d)
							.with(Employee.COMMISSION, 300d)
							.build();

			DefaultEntityValidator validator = new DefaultEntityValidator();
			assertDoesNotThrow(() -> validator.validate(emp));

			// Below minimum
			emp.set(Employee.COMMISSION, 10d);
			assertThrows(RangeValidationException.class,
							() -> validator.validate(emp));

			// Above maximum
			emp.set(Employee.COMMISSION, 2100d);
			assertThrows(RangeValidationException.class,
							() -> validator.validate(emp));
		}

		@Test
		@DisplayName("item validation ensures value is in allowed list")
		void itemValidation_invalidItem_throwsException() {
			Map<Attribute<?>, Object> values = new HashMap<>();
			values.put(Employee.NAME, "Name");
			values.put(Employee.DEPARTMENT_NO, 1);
			values.put(Employee.JOB, "CLREK"); // Invalid job code

			Entity emp = entities.definition(Employee.TYPE).entity(values);
			DefaultEntityValidator validator = new DefaultEntityValidator();

			assertThrows(ItemValidationException.class,
							() -> validator.validate(emp));
		}

		@Test
		@DisplayName("strict validation validates all values including unmodified")
		void strictValidation_validatesAllValues() {
			Entity emp = entities.entity(Employee.TYPE)
							.with(Employee.NAME, "1234567891000") // Too long
							.with(Employee.DEPARTMENT_NO, 1)
							.with(Employee.JOB, "CLERK")
							.with(Employee.SALARY, 1200d)
							.with(Employee.HIREDATE, LocalDateTime.now())
							.build();

			// Non-strict validation
			DefaultEntityValidator validator = new DefaultEntityValidator();
			assertThrows(LengthValidationException.class, () -> validator.validate(emp));

			emp.set(Employee.NAME, "Name");
			emp.save();

			// Simulate existing entity
			emp.set(Employee.ID, 10);
			emp.set(Employee.NAME, "1234567891000");
			assertThrows(LengthValidationException.class, () -> validator.validate(emp));

			emp.save(); // Not modified
			assertDoesNotThrow(() -> validator.validate(emp)); // Non-strict passes

			// Strict validation
			DefaultEntityValidator strictValidator = new DefaultEntityValidator(true);
			assertThrows(LengthValidationException.class, () -> strictValidator.validate(emp));

			emp.set(Employee.NAME, "Name");
			emp.save();

			emp.set(Employee.ID, 10);
			emp.set(Employee.NAME, "1234567891000");
			assertThrows(LengthValidationException.class, () -> strictValidator.validate(emp));

			emp.save(); // Not modified
			assertThrows(LengthValidationException.class,
							() -> strictValidator.validate(emp)); // Strict still validates
		}
	}


	@Nested
	@DisplayName("Entity Operations")
	class EntityOperationsTest {

		@Test
		@DisplayName("setting foreign key with wrong entity type throws exception")
		void validateTypeEntity_wrongEntityType_throwsException() {
			Entity entity = entities.entity(Detail.TYPE).build();
			Entity wrongTypeEntity = entities.entity(Detail.TYPE).build();

			assertThrows(IllegalArgumentException.class,
							() -> entity.set(Detail.MASTER_FK, wrongTypeEntity),
							"Should not set foreign key with wrong entity type");
		}

		@Test
		@DisplayName("setting derived attribute value throws exception")
		void setValueDerived_throwsException() {
			Entity entity = entities.entity(Detail.TYPE).build();

			assertThrows(IllegalArgumentException.class,
							() -> entity.set(Detail.INT_DERIVED, 10),
							"Should not set derived attribute value");
		}

		@Test
		@DisplayName("setting invalid item value throws exception")
		void setValueItem_invalidValue_throwsException() {
			Entity entity = entities.entity(Detail.TYPE).build();

			assertThrows(IllegalArgumentException.class,
							() -> entity.set(Detail.INT_ITEMS, -10),
							"Should not set invalid item value");
		}
	}

	@Nested
	@DisplayName("Entity Copying")
	class EntityCopyingTest {

		@Test
		@DisplayName("immutable copies create deep copies of entities")
		void copyEntities_immutableCopies_createDeepCopies() {
			Entity dept1 = entities.entity(Department.TYPE)
							.with(Department.ID, 1)
							.with(Department.LOCATION, "location")
							.with(Department.NAME, "name")
							.build();
			Entity dept2 = entities.entity(Department.TYPE)
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

			// Copies are different instances
			assertNotSame(dept1Copy, dept1);
			assertNotSame(dept2Copy, dept2);

			// But have same values
			assertTrue(dept1Copy.equalValues(dept1));
			assertTrue(dept2Copy.equalValues(dept2));
		}

		@Test
		@DisplayName("mutable copy shares referenced entities")
		void copyEntities_mutableCopy_sharesReferences() {
			Entity dept1 = entities.entity(Department.TYPE)
							.with(Department.ID, 1)
							.with(Department.LOCATION, "location")
							.with(Department.NAME, "name")
							.build();

			Entity emp1 = entities.entity(Employee.TYPE)
							.with(Employee.DEPARTMENT_FK, dept1)
							.with(Employee.NAME, "name")
							.with(Employee.COMMISSION, 130.5)
							.build();

			// Mutable copy shares references
			Entity copy = emp1.copy().mutable();
			assertTrue(emp1.equalValues(copy));
			assertSame(emp1.get(Employee.DEPARTMENT_FK), copy.get(Employee.DEPARTMENT_FK));
			assertFalse(emp1.modified());
		}

		@Test
		@DisplayName("immutable copy creates deep copies of referenced entities")
		void copyEntities_immutableCopy_deepCopiesReferences() {
			Entity dept1 = entities.entity(Department.TYPE)
							.with(Department.ID, 1)
							.with(Department.LOCATION, "location")
							.with(Department.NAME, "name")
							.build();

			Entity emp1 = entities.entity(Employee.TYPE)
							.with(Employee.DEPARTMENT_FK, dept1)
							.with(Employee.NAME, "name")
							.with(Employee.COMMISSION, 130.5)
							.build();

			// Immutable copy creates deep copies
			Entity copy = emp1.immutable();
			assertFalse(copy.mutable());
			assertTrue(emp1.equalValues(copy));
			assertNotSame(emp1.get(Employee.DEPARTMENT_FK), copy.get(Employee.DEPARTMENT_FK));
			assertFalse(emp1.modified());
		}
	}

	@Nested
	@DisplayName("Domain Extensions")
	class DomainExtensionsTest {

		@Test
		@DisplayName("extended domain includes parent and new entity types")
		void extendedDomain_includesParentAndNewTypes() {
			TestDomainExtended extended = new TestDomainExtended();
			Entities extendedEntities = extended.entities();

			// Can create extended entity
			assertDoesNotThrow(() -> extendedEntities.entity(TestDomainExtended.T_EXTENDED));

			// Can still create parent entities
			assertDoesNotThrow(() -> extendedEntities.entity(CompositeMaster.TYPE));
		}

		@Test
		@DisplayName("second extension includes all parent types")
		void secondExtension_includesAllParentTypes() {
			TestDomainExtended.TestDomainSecondExtension second = new TestDomainExtended.TestDomainSecondExtension();
			Entities secondEntities = second.entities();

			// Can create second extended entity
			assertDoesNotThrow(() -> secondEntities.entity(TestDomainExtended.TestDomainSecondExtension.T_SECOND_EXTENDED));

			// Can create first extended entity
			assertDoesNotThrow(() -> secondEntities.entity(TestDomainExtended.T_EXTENDED));

			// Can create base entities
			assertDoesNotThrow(() -> secondEntities.entity(CompositeMaster.TYPE));

			// Has procedures, functions, and reports
			assertNotNull(second.procedure(TestDomainExtended.PROC_TYPE));
			assertNotNull(second.function(TestDomainExtended.FUNC_TYPE));
			assertNotNull(second.report(TestDomainExtended.REP_TYPE));
		}

		@Test
		@DisplayName("entity type name clash throws exception")
		void extendedDomain_entityTypeNameClash_throwsException() {
			assertThrows(IllegalArgumentException.class,
							TestDomainExtended.TestDomainThirdExtension::new,
							"Should not allow entity type name clash");
		}
	}

	@Nested
	@DisplayName("Serialization")
	class SerializationTest {

		@Test
		@DisplayName("transient fields are not serialized")
		void transients_notSerialized() throws IOException, ClassNotFoundException {
			EntityDefinition definition = entities.definition(Employee.TYPE);

			// Original has transient fields
			assertNotNull(definition.table());
			assertNotNull(definition.selectTable());
			assertNotNull(definition.primaryKey().generator());
			assertTrue(definition.optimisticLocking());
			assertTrue(definition.selectQuery().isPresent());
			assertNotNull(definition.condition(Employee.CONDITION));

			ColumnDefinition<String> nameDefinition = definition.columns().definition(Employee.NAME);
			assertNotNull(nameDefinition.name());
			assertNotNull(nameDefinition.expression());

			// Deserialize
			EntityDefinition deserialized = Serializer.deserialize(Serializer.serialize(definition));

			// Transient fields are null/default after deserialization
			assertNull(deserialized.table());
			assertNull(deserialized.selectTable());
			assertNull(deserialized.primaryKey().generator());
			assertFalse(deserialized.optimisticLocking());
			assertFalse(deserialized.selectQuery().isPresent());
			assertThrows(IllegalArgumentException.class,
							() -> deserialized.condition(Employee.CONDITION));

			nameDefinition = deserialized.columns().definition(Employee.NAME);
			assertNull(nameDefinition.name());
			assertNull(nameDefinition.expression());
		}
	}
}
