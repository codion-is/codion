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

import is.codion.common.utilities.Serializer;
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
import is.codion.framework.domain.entity.ForeignKeyDomain.Species;
import is.codion.framework.domain.entity.attribute.Attribute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

@DisplayName("DefaultEntity")
public class DefaultEntityTest {

	// Test constants
	private static final long TEST_DETAIL_ID = 1L;
	private static final int TEST_DETAIL_INT = 2;
	private static final double TEST_DETAIL_DOUBLE = 1.2;
	private static final String TEST_DETAIL_STRING = "string";
	private static final Boolean TEST_DETAIL_BOOLEAN = true;
	private static final String TEST_MASTER_NAME = "master";
	private static final String TEST_NEW_STRING_VALUE = "a new String value";
	private static final String TEST_ORIGINAL_STRING_VALUE = "string value";

	private static final Entities ENTITIES = new TestDomain().entities();

	private LocalDate testDetailDate;
	private LocalDateTime testDetailTimestamp;

	@BeforeEach
	void setUp() {
		testDetailDate = LocalDate.now();
		testDetailTimestamp = LocalDateTime.now();
	}

	@Nested
	@DisplayName("Construction")
	class ConstructionTest {

		@Test
		@DisplayName("constructor rejects attributes from wrong entity type")
		void constructor_withWrongEntityTypeAttributes_throwsException() {
			EntityDefinition masterDefinition = ENTITIES.definition(Master.TYPE);

			Map<Attribute<?>, Object> values = new HashMap<>();
			values.put(Detail.BOOLEAN, false);
			values.put(Master.CODE, 1);

			assertThrows(IllegalArgumentException.class,
							() -> new DefaultEntity(masterDefinition, values, emptyMap()),
							"Should reject attributes from different entity type");
		}

		@Test
		@DisplayName("constructor rejects original values from wrong entity type")
		void constructor_withWrongEntityTypeOriginalValues_throwsException() {
			EntityDefinition masterDefinition = ENTITIES.definition(Master.TYPE);

			Map<Attribute<?>, Object> originalValues = new HashMap<>();
			originalValues.put(Detail.BOOLEAN, false);
			originalValues.put(Master.CODE, 1);

			assertThrows(IllegalArgumentException.class,
							() -> new DefaultEntity(masterDefinition, emptyMap(), originalValues),
							"Should reject original values from different entity type");
		}

		@Test
		@DisplayName("constructor validates attribute value types")
		void constructor_withInvalidValueType_throwsException() {
			EntityDefinition masterDefinition = ENTITIES.definition(Master.TYPE);

			Map<Attribute<?>, Object> invalidTypeValues = new HashMap<>();
			invalidTypeValues.put(Master.CODE, false); // CODE expects Integer, not Boolean

			assertThrows(IllegalArgumentException.class,
							() -> new DefaultEntity(masterDefinition, invalidTypeValues, emptyMap()),
							"Should reject values with incorrect type");
		}

		@Test
		@DisplayName("constructor validates original value types")
		void constructor_withInvalidOriginalValueType_throwsException() {
			EntityDefinition masterDefinition = ENTITIES.definition(Master.TYPE);

			Map<Attribute<?>, Object> invalidTypeOriginalValues = new HashMap<>();
			invalidTypeOriginalValues.put(Master.CODE, false); // CODE expects Integer, not Boolean

			assertThrows(IllegalArgumentException.class,
							() -> new DefaultEntity(masterDefinition, emptyMap(), invalidTypeOriginalValues),
							"Should reject original values with incorrect type");
		}

		@Test
		@DisplayName("constructor rejects undefined attributes")
		void constructor_withUndefinedAttribute_throwsException() {
			EntityDefinition masterDefinition = ENTITIES.definition(Master.TYPE);
			EntityType entityType = TestDomain.DOMAIN.entityType("entityType");
			Attribute<?> invalid = entityType.integerAttribute("invalid");

			Map<Attribute<?>, Object> invalidAttributeValues = new HashMap<>();
			invalidAttributeValues.put(invalid, 1);

			assertThrows(IllegalArgumentException.class,
							() -> new DefaultEntity(masterDefinition, invalidAttributeValues, emptyMap()),
							"Should reject attributes not defined in entity");
		}
	}

	@Nested
	@DisplayName("Serialization")
	class SerializationTest {

		@Test
		@DisplayName("entity serialization preserves all values and state")
		void serialization_preservesValuesAndState() throws Exception {
			Entity referencedEntity = createTestMaster(1L, "name", 10);
			Entity entity = detailEntity(10, 34, 23.4, TEST_ORIGINAL_STRING_VALUE,
							LocalDate.now(), LocalDateTime.now(), true, referencedEntity);

			// Modify entity to test modified state preservation
			entity.set(Detail.STRING, TEST_NEW_STRING_VALUE);

			// Serialize and deserialize
			List<Object> deserialized = Serializer.deserialize(
							Serializer.serialize(singletonList(entity)));

			assertEquals(1, deserialized.size(), "Should deserialize single entity");
			Entity deserializedEntity = (Entity) deserialized.get(0);

			// Verify entity properties preserved
			assertEquals(Detail.TYPE, deserializedEntity.type(), "Entity type should be preserved");
			assertTrue(entity.equalValues(deserializedEntity), "Values should be equal");
			assertTrue(deserializedEntity.modified(), "Modified state should be preserved");
			assertTrue(deserializedEntity.modified(Detail.STRING), "Attribute modified state should be preserved");
			assertEquals(TEST_ORIGINAL_STRING_VALUE, deserializedEntity.original(Detail.STRING),
							"Original value should be preserved");
		}

		@Test
		@DisplayName("primary key serialization works correctly")
		void serialization_primaryKey_preservesKey() throws Exception {
			Entity entity = createTestMaster(1L, "name", 10);
			Entity.Key key = entity.primaryKey();

			byte[] serialized = Serializer.serialize(singletonList(key));
			List<Object> deserialized = Serializer.deserialize(serialized);

			assertEquals(1, deserialized.size(), "Should deserialize single key");
			assertEquals(key, deserialized.get(0), "Key should be equal after serialization");
		}

		@Test
		@DisplayName("entity with missing optional attributes serializes correctly")
		void serialization_withMissingOptionalAttributes_preservesState() throws Exception {
			Entity master = ENTITIES.entity(Master.TYPE)
							.with(Master.ID, 1L)
							.with(Master.CODE, 11)
							// NAME is optional and not set
							.build();

			Entity deserialized = Serializer.deserialize(Serializer.serialize(master));

			assertEquals(master.get(Master.ID), deserialized.get(Master.ID), "ID should be preserved");
			assertEquals(master.get(Master.CODE), deserialized.get(Master.CODE), "CODE should be preserved");
			assertFalse(deserialized.contains(Master.NAME), "Missing NAME should remain missing");
		}

		@Test
		@DisplayName("immutable entity serialization preserves immutability")
		void serialization_immutableEntity_preservesState() throws Exception {
			Entity master = ENTITIES.entity(Master.TYPE)
							.with(Master.ID, 1L)
							.with(Master.CODE, 11)
							.build();

			Entity deserialized = Serializer.deserialize(Serializer.serialize(master.immutable()));

			assertEquals(master.get(Master.ID), deserialized.get(Master.ID), "ID should be preserved");
			assertEquals(master.get(Master.CODE), deserialized.get(Master.CODE), "CODE should be preserved");
			assertFalse(deserialized.contains(Master.NAME), "Missing NAME should remain missing");
		}
	}

	@Nested
	@DisplayName("Value Operations")
	class ValueOperationsTest {

		@Test
		@DisplayName("set copies all values from another entity")
		void set_copiesAllValues() {
			Entity source = createTestDetail();
			Entity target = ENTITIES.entity(Detail.TYPE).build();

			target.set(source);

			assertEquals(source, target, "Entities should be equal after set()");
			assertTrue(target.equalValues(source), "Values should be equal after set()");
		}

		@Test
		@DisplayName("set with same entity returns empty affected attributes")
		void set_withSameEntity_returnsEmpty() {
			Entity entity = createTestDetail();

			Map<Attribute<?>, Object> affected = entity.set(entity);

			assertTrue(affected.isEmpty(), "Setting entity to itself should affect no attributes");
		}

		@Test
		@DisplayName("set clears cached foreign key values correctly")
		void set_clearsCachedForeignKeyValues() {
			Entity source = ENTITIES.entity(Detail.TYPE).build();
			Entity target = createTestDetail();

			// Clear foreign key in source
			source.set(Detail.MASTER_FK, null);

			// Set target from source
			target.set(source);

			assertNull(target.get(Detail.MASTER_ID), "Master ID should be cleared");
			assertNull(target.get(Detail.MASTER_FK), "Master FK should be cleared");
		}

		@Test
		@DisplayName("set with wrong entity type throws exception")
		void set_withWrongEntityType_throwsException() {
			Entity master = createTestMaster(1L, "name", 7);
			Entity detail = createTestDetail();

			assertThrows(IllegalArgumentException.class,
							() -> detail.set(master),
							"Should not allow setting from different entity type");
		}

		@Test
		@DisplayName("set preserves read-only attribute modifications")
		void set_preservesReadOnlyModifications() {
			Entity entity = ENTITIES.entity(Master.TYPE)
							.with(Master.ID, 2L)
							.with(Master.NAME, TEST_MASTER_NAME)
							.with(Master.CODE, 7)
							.with(Master.READ_ONLY, 42)
							.build();

			entity.set(Master.READ_ONLY, 1);

			Entity target = ENTITIES.entity(Master.TYPE).build();
			target.set(entity);

			assertTrue(target.modified(Master.READ_ONLY),
							"Read-only attribute modification should be preserved");
		}
	}

	@Nested
	@DisplayName("Set with Affected Attributes")
	class SetAffectedAttributesTest {

		@Test
		@DisplayName("set returns empty when entities have same values")
		void set_withSameValues_returnsEmpty() {
			Entity original = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 1L)
							.build();

			Entity entity = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 1L)
							.build();

			Map<Attribute<?>, Object> affected = original.set(entity);

			assertEquals(0, affected.size(), "No attributes should be affected");
			assertTrue(original.equalValues(entity), "Values should be equal");
		}

		@Test
		@DisplayName("set returns single affected attribute")
		void set_withSingleDifference_returnsSingleAttribute() {
			Entity original = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 1L)
							.build();
			Entity entity = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 1L)
							.build();

			original.set(Detail.BOOLEAN, true);
			entity.set(Detail.BOOLEAN, false);

			Map<Attribute<?>, Object> affected = original.set(entity);

			assertEquals(1, affected.size(), "One attribute should be affected");
			assertTrue(original.equalValues(entity), "Values should be equal after set");
		}

		@Test
		@DisplayName("set includes derived attributes when source changes")
		void set_withDerivedAttribute_includesDerived() {
			Entity original = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 1L)
							.build();
			Entity entity = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 1L)
							.build();

			original.set(Detail.INT, 1);
			entity.set(Detail.INT, 2);
			entity.set(Detail.INT, 3); // Modified

			Map<Attribute<?>, Object> affected = original.set(entity);

			assertEquals(2, affected.size(), "Should include INT and INT_DERIVED");
			assertTrue(original.equalValues(entity), "Values should be equal");
			assertTrue(original.modified(), "Original should be modified");
			assertTrue(entity.modified(), "Entity should remain modified");
		}

		@Test
		@DisplayName("set handles multiple differences correctly")
		void set_withMultipleDifferences_returnsAllAffected() {
			Entity original = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 1L)
							.build();
			Entity entity = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 1L)
							.build();

			original.set(Detail.DOUBLE, 1.2);
			original.set(Detail.STRING, "str");
			entity.set(Detail.DOUBLE, 1.3);
			entity.set(Detail.STRING, "strng");

			Map<Attribute<?>, Object> affected = original.set(entity);

			assertEquals(2, affected.size(), "Two attributes should be affected");
			assertTrue(original.equalValues(entity), "Values should be equal");
		}

		@Test
		@DisplayName("entities not equal after removing attribute")
		void set_afterRemovingAttribute_notEqual() {
			Entity original = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 1L)
							.with(Detail.STRING, "test")
							.build();
			Entity entity = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 1L)
							.with(Detail.STRING, "test")
							.build();

			entity.remove(Detail.STRING);

			assertFalse(entity.equalValues(original),
							"Entities should not be equal after removing attribute");
		}
	}

	@Nested
	@DisplayName("Derived Attributes")
	class DerivedAttributesTest {

		@Test
		@DisplayName("derived attribute has correct initial value")
		void derivedAttribute_initialValue_calculated() {
			Entity entity = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 0L)
							.with(Detail.INT, 1)
							.build();

			assertEquals(10, entity.get(Detail.INT_DERIVED),
							"Initial derived value should be calculated");
			assertEquals(10, entity.original(Detail.INT_DERIVED),
							"Original derived value should match initial");
		}

		@Test
		@DisplayName("derived attribute updates when source changes")
		void derivedAttribute_sourceChange_updatesValue() {
			Entity entity = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 0L)
							.with(Detail.INT, 1)
							.build();

			entity.set(Detail.INT, 2);

			assertEquals(10, entity.original(Detail.INT_DERIVED),
							"Original derived value should remain unchanged");
			assertEquals(20, entity.get(Detail.INT_DERIVED),
							"Derived value should update with source");
		}

		@Test
		@DisplayName("derived attribute reverts when source reverts")
		void derivedAttribute_sourceRevert_revertsValue() {
			Entity entity = ENTITIES.entity(Detail.TYPE)
							.with(Detail.ID, 0L)
							.with(Detail.INT, 1)
							.build();

			entity.set(Detail.INT, 2);
			entity.set(Detail.INT, 1);

			assertEquals(10, entity.get(Detail.INT_DERIVED),
							"Derived value should revert with source");
			assertEquals(10, entity.original(Detail.INT_DERIVED),
							"Original derived value should remain unchanged");
		}

		@Test
		@DisplayName("invalid derived attribute throws exception")
		void derivedAttribute_invalid_throwsException() {
			Entity invalidDerived = ENTITIES.entity(InvalidDerived.TYPE)
							.with(InvalidDerived.ID, 0)
							.with(InvalidDerived.INT, 1)
							.build();

			assertThrows(IllegalArgumentException.class,
							() -> invalidDerived.get(InvalidDerived.INVALID_DERIVED),
							"Invalid derived attribute should throw exception");
		}
	}

	@Nested
	@DisplayName("Save and Revert Operations")
	class SaveRevertTest {

		@Test
		@DisplayName("save non-updatable attribute has no effect")
		void save_nonUpdatableAttribute_noEffect() {
			Entity entity = createTestMaster(2L, TEST_MASTER_NAME, 7);

			entity.set(Master.ID, -55L);

			// ID is part of primary key, not updatable by default
			assertFalse(entity.modified(), "Non-updatable attribute should not modify entity");

			entity.save(Master.ID);

			assertFalse(entity.modified(), "Save should have no effect on non-updatable attribute");
		}

		@Test
		@DisplayName("revert restores original value")
		void revert_restoresOriginalValue() {
			Entity entity = createTestMaster(2L, TEST_MASTER_NAME, 7);
			final String newName = "aname";

			entity.set(Master.NAME, newName);
			assertTrue(entity.modified(), "Entity should be modified after change");

			entity.revert(Master.NAME);

			assertEquals(TEST_MASTER_NAME, entity.get(Master.NAME),
							"Value should revert to original");
			assertFalse(entity.modified(), "Entity should not be modified after revert");
		}

		@Test
		@DisplayName("save makes current value the original")
		void save_makesCurrentValueOriginal() {
			Entity entity = createTestMaster(2L, TEST_MASTER_NAME, 7);
			final String newName = "aname";

			entity.set(Master.NAME, newName);
			assertTrue(entity.modified(), "Entity should be modified");
			assertTrue(entity.modified(Master.NAME), "Attribute should be modified");

			entity.save(Master.NAME);

			assertEquals(newName, entity.get(Master.NAME), "Value should remain new");
			assertFalse(entity.modified(), "Entity should not be modified after save");
			assertFalse(entity.modified(Master.NAME), "Attribute should not be modified after save");
		}

		@Test
		@DisplayName("save and revert track original values correctly")
		void saveRevert_tracksOriginalValues() {
			Entity entity = ENTITIES.entity(Master.TYPE)
							.with(Master.NAME, "name")
							.build();

			entity.set(Master.NAME, "newname");
			assertTrue(entity.modified(), "Should be modified");
			assertEquals("name", entity.original(Master.NAME), "Original should be 'name'");

			entity.save(Master.NAME);
			entity.set(Master.NAME, "name");

			assertEquals("newname", entity.original(Master.NAME),
							"Original should be 'newname' after save");
			assertTrue(entity.modified(), "Should be modified after changing back");

			entity.revert();
			assertEquals("newname", entity.get(Master.NAME),
							"Should revert to saved value");
			assertFalse(entity.modified(), "Should not be modified after revert");
		}
	}

	@Nested
	@DisplayName("Foreign Key Operations")
	class ForeignKeyOperationsTest {

		@Test
		@DisplayName("referenced key caching works correctly")
		void referencedKeyCache_returnsSameInstance() {
			Entity compositeDetail = ENTITIES.entity(CompositeDetail.TYPE)
							.with(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID, 1)
							.with(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2, 2)
							.with(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 3)
							.build();

			Entity.Key referencedKey = compositeDetail.key(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK);
			Entity.Key cachedKey = compositeDetail.key(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK);

			assertSame(cachedKey, referencedKey, "Should return same cached key instance");

			Entity master = ENTITIES.entity(Master.TYPE)
							.with(Master.CODE, 3)
							.build();

			Entity detail = ENTITIES.entity(Detail.TYPE)
							.with(Detail.MASTER_VIA_CODE_FK, master)
							.build();

			Entity.Key codeKey = detail.key(Detail.MASTER_VIA_CODE_FK);
			assertEquals(Integer.valueOf(3), codeKey.value());
			Entity.Key cachedCodeKey = detail.key(Detail.MASTER_VIA_CODE_FK);
			assertEquals(Integer.valueOf(3), cachedCodeKey.value());

			assertSame(codeKey, cachedCodeKey, "Should return same cached code key instance");
		}

		@Test
		@DisplayName("composite reference key handles null values correctly")
		void compositeReferenceKey_handlesNullValues() {
			Entity master = ENTITIES.entity(CompositeMaster.TYPE)
							.with(CompositeMaster.COMPOSITE_MASTER_ID, null)
							.with(CompositeMaster.COMPOSITE_MASTER_ID_2, 2)
							.with(CompositeMaster.COMPOSITE_MASTER_ID_3, 3)
							.build();

			Entity detail = ENTITIES.entity(CompositeDetail.TYPE)
							.with(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 1)
							.build();

			// Cannot update read-only attribute reference with a different value
			assertThrows(IllegalArgumentException.class,
							() -> detail.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, master),
							"Should not update read-only attribute with different value");

			detail.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 3);
			detail.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, master);

			// Otherwise the values are equal and put() returns before propagating foreign key values
			Entity masterCopy = master.copy().mutable();
			masterCopy.set(CompositeMaster.COMPOSITE_MASTER_ID, 1);
			masterCopy.set(CompositeMaster.COMPOSITE_MASTER_ID_2, null);
			detail.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, masterCopy);

			assertNull(detail.key(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK),
							"Key should be null with incomplete composite key");

			master.set(CompositeMaster.COMPOSITE_MASTER_ID, 1);
			master.set(CompositeMaster.COMPOSITE_MASTER_ID_2, 3);
			master.set(CompositeMaster.COMPOSITE_MASTER_ID_3, 3);

			Entity detail2 = ENTITIES.entity(CompositeDetail.TYPE)
							.with(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 3)
							.with(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK, master)
							.build();

			assertEquals(3, detail2.get(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2));

			detail2.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2, null);
			assertTrue(detail2.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
			assertNull(detail2.key(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
		}

		@Test
		@DisplayName("foreign key value handles null correctly")
		void foreignKeyValue_handlesNull() {
			Entity department = ENTITIES.entity(Department.TYPE)
							.with(Department.ID, -10)
							.build();
			Entity employee = ENTITIES.entity(Employee.TYPE)
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
		@DisplayName("foreign key modification propagates correctly")
		void foreignKeyModification_propagatesValues() {
			Entity dept = ENTITIES.entity(Department.TYPE)
							.with(Department.ID, 1)
							.with(Department.NAME, "Name1")
							.build();
			Entity emp = ENTITIES.entity(Employee.TYPE)
							.with(Employee.DEPARTMENT_FK, dept)
							.build();

			assertEquals(1, emp.get(Employee.DEPARTMENT_NO));

			emp.set(Employee.DEPARTMENT_NO, 2);
			assertNull(emp.get(Employee.DEPARTMENT_FK));

			Entity referencedDept = emp.entity(Employee.DEPARTMENT_FK);
			assertEquals(Integer.valueOf(2), referencedDept.primaryKey().value());

			Entity dept2 = ENTITIES.entity(Department.TYPE)
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
		}

		@Test
		@DisplayName("read-only foreign key references validate correctly")
		void readOnlyForeignKeyReferences_validateCorrectly() {
			ForeignKeyDomain domain = new ForeignKeyDomain();
			Entities entities = domain.entities();

			Entity cod = entities.entity(Species.TYPE)
							.with(Species.NO, 1)
							.with(Species.NAME, "Cod")
							.build();

			Entity codMaturity10 = entities.entity(Maturity.TYPE)
							.with(Maturity.SPECIES_FK, cod)
							.with(Maturity.NO, 10)
							.build();

			Entity haddock = entities.entity(Species.TYPE)
							.with(Species.NO, 2)
							.with(Species.NAME, "Haddock")
							.build();

			Entity haddockMaturity10 = entities.entity(Maturity.TYPE)
							.with(Maturity.SPECIES_FK, haddock)
							.with(Maturity.NO, 10)
							.build();

			Entity otolith = entities.entity(Otolith.TYPE)
							.with(Otolith.SPECIES_FK, cod)
							.build();

			// Try to set a haddock maturity on cod otolith
			assertThrows(IllegalArgumentException.class,
							() -> otolith.set(Otolith.MATURITY_FK, haddockMaturity10),
							"Should not allow maturity from different species");

			otolith.set(Otolith.MATURITY_FK, codMaturity10);

			// Remove species, maturity should be removed
			otolith.set(Otolith.SPECIES_FK, null);
			assertTrue(otolith.isNull(Otolith.MATURITY_FK), "Maturity FK should be null");
			assertFalse(otolith.isNull(Otolith.MATURITY_NO), "Maturity NO should remain");

			// Set species to haddock
			otolith.set(Otolith.SPECIES_FK, haddock);
			otolith.set(Otolith.MATURITY_FK, haddockMaturity10);
			assertFalse(otolith.isNull(Otolith.MATURITY_FK));

			// Set species back to cod, haddock maturity should be removed
			otolith.set(Otolith.SPECIES_FK, cod);
			assertNull(otolith.get(Otolith.MATURITY_FK));
		}
	}

	@Nested
	@DisplayName("Entity Core Operations")
	class EntityCoreOperationsTest {

		@Test
		@DisplayName("entity no primary key works correctly")
		void noPrimaryKey_handledCorrectly() {
			Entity noPk = ENTITIES.entity(NoPk.TYPE)
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
		@DisplayName("entity basic operations work correctly")
		void entity_basicOperations() {
			Entity referencedEntityValue = createTestMaster(2L, TEST_MASTER_NAME, 7);
			assertFalse(referencedEntityValue.modified());

			Entity testEntity = detailEntity(TEST_DETAIL_ID, TEST_DETAIL_INT, TEST_DETAIL_DOUBLE,
							TEST_DETAIL_STRING, testDetailDate, testDetailTimestamp, TEST_DETAIL_BOOLEAN, referencedEntityValue);

			// Assert values
			assertEquals(TEST_DETAIL_ID, testEntity.get(Detail.ID));
			assertTrue(testEntity.optional(Detail.ID).isPresent());
			assertEquals(TEST_DETAIL_INT, testEntity.get(Detail.INT));
			assertEquals(TEST_DETAIL_DOUBLE, testEntity.get(Detail.DOUBLE));
			assertEquals(TEST_DETAIL_STRING, testEntity.get(Detail.STRING));
			assertEquals(testDetailDate, testEntity.get(Detail.DATE));
			assertEquals(testDetailTimestamp, testEntity.get(Detail.TIMESTAMP));
			assertEquals(TEST_DETAIL_BOOLEAN, testEntity.get(Detail.BOOLEAN));
			assertEquals(referencedEntityValue, testEntity.get(Detail.MASTER_FK));
			assertEquals(TEST_MASTER_NAME, testEntity.get(Detail.MASTER_NAME));
			assertEquals(7, testEntity.get(Detail.MASTER_CODE));
			assertFalse(testEntity.isNull(Detail.MASTER_ID));

			testEntity.key(Detail.MASTER_FK);

			// Test copy()
			Entity test2 = testEntity.immutable().copy().mutable();
			assertNotSame(test2, testEntity, "Entity copy should not be == the original");
			assertEquals(test2, testEntity, "Entities should be equal after copy()");
			assertTrue(test2.equalValues(testEntity), "Entity attribute values should be equal after deepCopy()");
			assertNotSame(testEntity.entity(Detail.MASTER_FK), test2.entity(Detail.MASTER_FK),
							"This should be a deep copy");
		}

		@Test
		@DisplayName("copy builder preserves entity state")
		void copyBuilder_preservesState() {
			Entity dept = ENTITIES.entity(Department.TYPE)
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
		@DisplayName("referenced key with incorrect FK throws exception")
		void referencedKeyIncorrectFk_throwsException() {
			Entity testEntity = detailEntity(TEST_DETAIL_ID, TEST_DETAIL_INT, TEST_DETAIL_DOUBLE,
							TEST_DETAIL_STRING, testDetailDate, testDetailTimestamp, TEST_DETAIL_BOOLEAN, null);

			assertThrows(IllegalArgumentException.class,
							() -> testEntity.key(Employee.DEPARTMENT_FK),
							"Should throw exception for incorrect foreign key");
		}

		@Test
		@DisplayName("isNull checks work correctly")
		void isNull_checksWorkCorrectly() {
			Entity testEntity = detailEntity(TEST_DETAIL_ID, TEST_DETAIL_INT, TEST_DETAIL_DOUBLE,
							TEST_DETAIL_STRING, testDetailDate, testDetailTimestamp, TEST_DETAIL_BOOLEAN, null);

			assertTrue(testEntity.isNull(Detail.MASTER_ID));
			assertTrue(testEntity.isNull(Detail.MASTER_FK));

			testEntity.set(Detail.MASTER_ID, 10L);

			assertNull(testEntity.get(Detail.MASTER_FK));
			Entity referencedEntityValue = testEntity.entity(Detail.MASTER_FK);
			assertEquals(10L, referencedEntityValue.get(Master.ID));
			assertFalse(testEntity.isNull(Detail.MASTER_FK));
			assertFalse(testEntity.isNull(Detail.MASTER_ID));

			// Composite key null checks
			Entity composite = ENTITIES.entity(CompositeDetail.TYPE).build();
			composite.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID, null);
			assertTrue(composite.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));

			composite.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID, 1);
			assertTrue(composite.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));

			composite.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2, 1);
			composite.set(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3, 2);
			assertFalse(composite.isNull(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK));
		}

		@Test
		@DisplayName("remove all clears entity state")
		void removeAll_clearsState() {
			Entity referencedEntityValue = ENTITIES.entity(Master.TYPE).build();
			Entity testEntity = detailEntity(TEST_DETAIL_ID, TEST_DETAIL_INT, TEST_DETAIL_DOUBLE,
							TEST_DETAIL_STRING, testDetailDate, testDetailTimestamp, TEST_DETAIL_BOOLEAN, referencedEntityValue);

			testEntity.set(Detail.STRING, "TestString");
			assertTrue(testEntity.modified());

			testEntity.set(null);

			assertTrue(testEntity.primaryKey().isNull());
			assertFalse(testEntity.contains(Detail.DATE));
			assertFalse(testEntity.contains(Detail.STRING));
			assertFalse(testEntity.contains(Detail.BOOLEAN));
			assertFalse(testEntity.modified());
		}

		@Test
		@DisplayName("exists tracks entity persistence state")
		void exists_tracksPersistenceState() {
			Entity emp = ENTITIES.entity(Employee.TYPE)
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
		}
	}

	@Nested
	@DisplayName("Value Type Handling")
	class ValueTypeHandlingTest {

		@Test
		@DisplayName("set denormalized view value throws exception")
		void setDenormalizedViewValue_throwsException() {
			Entity testEntity = detailEntity(TEST_DETAIL_ID, TEST_DETAIL_INT, TEST_DETAIL_DOUBLE,
							TEST_DETAIL_STRING, testDetailDate, testDetailTimestamp, TEST_DETAIL_BOOLEAN, null);

			assertThrows(IllegalArgumentException.class,
							() -> testEntity.set(Detail.MASTER_NAME, "hello"),
							"Should not allow setting denormalized view value");
		}

		@Test
		@DisplayName("set denormalized value throws exception")
		void setDenormalizedValue_throwsException() {
			Entity testEntity = detailEntity(TEST_DETAIL_ID, TEST_DETAIL_INT, TEST_DETAIL_DOUBLE,
							TEST_DETAIL_STRING, testDetailDate, testDetailTimestamp, TEST_DETAIL_BOOLEAN, null);

			assertThrows(IllegalArgumentException.class,
							() -> testEntity.set(Detail.MASTER_CODE, 2),
							"Should not allow setting denormalized value");
		}

		@Test
		@DisplayName("setValue validates attribute types")
		void setValue_validatesTypes() {
			Entity department = ENTITIES.entity(Department.TYPE)
							.with(Department.ID, -10)
							.build();

			Entity employee = ENTITIES.entity(Employee.TYPE)
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
			assertThrows(IllegalArgumentException.class,
							() -> employee.set((Attribute) Employee.NAME, 42),
							"Should validate attribute value type");
		}

		@Test
		@DisplayName("equalValues handles different value types")
		void equalValues_handlesDifferentTypes() {
			Entity testEntityOne = detailEntity(TEST_DETAIL_ID, TEST_DETAIL_INT, TEST_DETAIL_DOUBLE,
							TEST_DETAIL_STRING, testDetailDate, testDetailTimestamp, TEST_DETAIL_BOOLEAN, null);
			Entity testEntityTwo = detailEntity(TEST_DETAIL_ID, TEST_DETAIL_INT, TEST_DETAIL_DOUBLE,
							TEST_DETAIL_STRING, testDetailDate, testDetailTimestamp, TEST_DETAIL_BOOLEAN, null);

			assertTrue(testEntityOne.equalValues(testEntityTwo));

			testEntityTwo.set(Detail.INT, 42);
			assertFalse(testEntityOne.equalValues(testEntityTwo));

			testEntityOne.set(Detail.INT, 42);
			assertTrue(testEntityOne.equalValues(testEntityTwo));

			// Test with byte arrays
			Random random = new Random();
			byte[] bytes = new byte[1024];
			random.nextBytes(bytes);

			testEntityOne.set(Detail.BYTES, bytes);
			assertFalse(testEntityOne.equalValues(testEntityTwo));

			testEntityTwo.set(Detail.BYTES, bytes);
			assertTrue(testEntityOne.equalValues(testEntityTwo));

			assertThrows(IllegalArgumentException.class,
							() -> testEntityOne.equalValues(ENTITIES.entity(Master.TYPE).build()),
							"Should not compare different entity types");
		}

		@Test
		@DisplayName("double value respects fraction digits")
		void doubleValue_respectsFractionDigits() {
			Entity employee = ENTITIES.entity(Employee.TYPE)
							.with(Employee.ID, -10)
							.build();

			assertNull(employee.get(Employee.SALARY));

			final double salary = 1000.1234;
			employee.set(Employee.SALARY, salary);
			assertEquals(Double.valueOf(1000.12), employee.get(Employee.SALARY));
		}

		@Test
		@DisplayName("derived value calculates correctly")
		void derivedValue_calculatesCorrectly() {
			Entity department = ENTITIES.entity(Department.TYPE)
							.with(Department.NAME, "dname")
							.build();
			Entity employee = ENTITIES.entity(Employee.TYPE)
							.with(Employee.NAME, "ename")
							.with(Employee.DEPARTMENT_FK, department)
							.build();

			assertEquals("ename - dname", employee.get(Employee.DEPARTMENT_NAME));

			Entity detail = ENTITIES.entity(Detail.TYPE)
							.with(Detail.INT, 42)
							.build();

			assertEquals("420", detail.formatted(Detail.INT_DERIVED));
		}

		@Test
		@DisplayName("remove value handles foreign keys correctly")
		void removeValue_handlesForeignKeys() {
			Entity department = ENTITIES.entity(Department.TYPE)
							.with(Department.ID, -10)
							.build();
			Entity employee = ENTITIES.entity(Employee.TYPE)
							.with(Employee.ID, -10)
							.with(Employee.DEPARTMENT_FK, department)
							.build();

			assertNotNull(employee.entity(Employee.DEPARTMENT_FK));
			assertEquals(Integer.valueOf(-10), employee.get(Employee.DEPARTMENT_NO));

			employee.remove(Employee.DEPARTMENT_FK);

			assertNull(employee.get(Employee.DEPARTMENT_FK));
			Entity empDepartment = employee.entity(Employee.DEPARTMENT_FK);
			assertNotNull(empDepartment);
			// Non-loaded entity, created from foreign key
			assertFalse(empDepartment.contains(Department.NAME));
			assertNotNull(employee.get(Employee.DEPARTMENT_NO));
			assertFalse(employee.contains(Employee.DEPARTMENT_FK));
			assertTrue(employee.contains(Employee.DEPARTMENT_NO));
		}

		@Test
		@DisplayName("fraction digits enforced")
		void fractionDigits_enforced() {
			Entity employee = ENTITIES.entity(Employee.TYPE)
							.with(Employee.COMMISSION, 1.1234)
							.build();

			assertEquals(1.12, employee.get(Employee.COMMISSION));

			employee.set(Employee.COMMISSION, 1.1255);
			assertEquals(1.13, employee.get(Employee.COMMISSION));

			Entity detail = ENTITIES.entity(Detail.TYPE)
							.with(Detail.DOUBLE, 1.123456789567)
							.build();

			assertEquals(1.1234567896, detail.get(Detail.DOUBLE)); // Default 10 fraction digits
		}
	}

	@Nested
	@DisplayName("Special Cases")
	class SpecialCasesTest {

		@Test
		@DisplayName("key invalid attribute get throws exception")
		void keyInvalidAttributeGet_throwsException() {
			assertThrows(IllegalArgumentException.class,
							() -> ENTITIES.key(Employee.TYPE).build().get(Employee.NAME),
							"Key should not allow get on non-key attributes");
		}

		@Test
		@DisplayName("transient attribute modifies entity when configured")
		void transientAttributeModifiesEntity_whenConfigured() throws IOException, ClassNotFoundException {
			Entity entity = ENTITIES.entity(TransModifies.TYPE)
							.with(TransModifies.ID, 42)
							.with(TransModifies.TRANS, null)
							.build();

			entity.set(TransModifies.TRANS, 1);
			assertTrue(entity.modified());

			Entity deserialized = Serializer.deserialize(Serializer.serialize(entity));
			assertTrue(deserialized.modified(TransModifies.TRANS));
			assertTrue(entity.modified());

			// TransModifiesNot - transient that doesn't modify
			entity = ENTITIES.entity(TransModifiesNot.TYPE)
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
		@DisplayName("null string representation works correctly")
		void nullString_representationCorrect() {
			Entity entity = ENTITIES.entity(NullString.TYPE)
							.with(NullString.ID, 42)
							.with(NullString.ATTR, null)
							.build();

			assertEquals("null_string: id: 42, attr: null", entity.toString());
		}

		@Test
		@DisplayName("toString caching works correctly")
		void cacheToString_worksCorrectly() {
			Entity employee = ENTITIES.entity(Employee.TYPE)
							.with(Employee.ID, 1)
							.with(Employee.NAME, "Name")
							.build();

			String toString = employee.toString();
			assertSame(toString, employee.toString(), "Should return cached toString");

			Entity entity = ENTITIES.entity(NonCachedToString.TYPE)
							.with(NonCachedToString.ID, 1)
							.with(NonCachedToString.STRING, "value")
							.build();

			toString = entity.toString();
			assertEquals(toString, entity.toString());
			assertNotSame(toString, entity.toString(), "Should not cache toString");
		}

		@Test
		@DisplayName("immutable entity prevents modifications")
		void immutableEntity_preventsModifications() {
			Entity employee = ENTITIES.entity(Employee.TYPE)
							.with(Employee.ID, 1)
							.with(Employee.NAME, "Name")
							.with(Employee.DEPARTMENT_FK, ENTITIES.entity(Department.TYPE)
											.with(Department.ID, 42)
											.with(Department.NAME, "Dept name")
											.build())
							.build();

			Entity emp = employee.immutable();

			assertSame(emp, emp.immutable(), "Immutable should return self");

			assertThrows(UnsupportedOperationException.class, () -> emp.set(Department.ID, 2));
			assertThrows(UnsupportedOperationException.class, () -> emp.save(Department.ID));
			assertThrows(UnsupportedOperationException.class, emp::save);
			assertThrows(UnsupportedOperationException.class, () -> emp.revert(Department.ID));
			assertThrows(UnsupportedOperationException.class, emp::revert);
			assertThrows(UnsupportedOperationException.class, () -> emp.remove(Department.ID));
			assertThrows(UnsupportedOperationException.class, () -> emp.set(emp));

			Entity dept = emp.get(Employee.DEPARTMENT_FK);
			assertThrows(UnsupportedOperationException.class, () -> dept.set(Department.ID, 2));
		}

		@Test
		@DisplayName("foreign key lazy column excluded from selected")
		void foreignKeyLazyColumn_excludedFromSelected() {
			Collection<Attribute<?>> selectedAttributes = ENTITIES.definition(ForeignKeyLazyColumn.TYPE)
							.attributes().selected();

			assertFalse(selectedAttributes.contains(ForeignKeyLazyColumn.DEPARTMENT_FK),
							"FK based on lazy loaded column should not be selected");
		}

		@Test
		@DisplayName("miscellaneous operations work correctly")
		void misc_operationsWork() {
			Entity aron = ENTITIES.entity(Employee.TYPE)
							.with(Employee.ID, 42)
							.with(Employee.NAME, "Aron")
							.with(Employee.DEPARTMENT_NO, 1)
							.build();

			assertEquals("deptno:1", aron.formatted(Employee.DEPARTMENT_FK));
			assertEquals(42, aron.hashCode());

			Entity bjorn = ENTITIES.entity(Employee.TYPE)
							.with(Employee.ID, 99)
							.with(Employee.NAME, "Björn")
							.build();

			assertEquals(-1, aron.compareTo(bjorn));

			aron.set(Employee.COMMISSION, 10.1234);
			// Decimal fraction digits adjusted
			assertEquals(0, Double.compare(aron.get(Employee.COMMISSION), 10.12));
		}

		@Test
		@DisplayName("trim whitespace from string values")
		void trim_whitespaceFromStrings() {
			Entity aron = ENTITIES.entity(Employee.TYPE)
							.with(Employee.NAME, " Aron\n ")
							.build();

			assertEquals("Aron", aron.get(Employee.NAME));
		}

		@Test
		@DisplayName("cached derived attributes work correctly")
		void cachedDerived_worksCorrectly() {
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
																	.derived()
																	.from(stringAttribute)
																	.value(source ->
																					source.get(stringAttribute) + "-derived"),
													derivedAttributeNonCached.define()
																	.derived()
																	.from(stringAttribute)
																	.value(source ->
																					source.get(stringAttribute) + "-derived")
																	.cached(false),
													derivedAttributeNoSource.define()
																	.derived()
																	.from()
																	.value(source -> UUID.randomUUID()))
									.build());
				}
			}

			Entity entity = new DerivedDomain().entities().entity(type).build();
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
	}

	// Helper methods
	private Entity createTestMaster(Long id, String name, Integer code) {
		return ENTITIES.entity(Master.TYPE)
						.with(Master.ID, id)
						.with(Master.NAME, name)
						.with(Master.CODE, code)
						.build();
	}

	private Entity createTestDetail() {
		Entity master = createTestMaster(2L, TEST_MASTER_NAME, 7);
		return detailEntity(TEST_DETAIL_ID, TEST_DETAIL_INT, TEST_DETAIL_DOUBLE,
						TEST_DETAIL_STRING, testDetailDate, testDetailTimestamp,
						TEST_DETAIL_BOOLEAN, master);
	}

	private static Entity detailEntity(long id, Integer intValue, Double doubleValue,
																		 String stringValue, LocalDate dateValue, LocalDateTime timestampValue,
																		 Boolean booleanValue, Entity entityValue) {
		return ENTITIES.entity(Detail.TYPE)
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