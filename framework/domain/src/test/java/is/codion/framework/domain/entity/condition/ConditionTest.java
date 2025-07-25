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
package is.codion.framework.domain.entity.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;

import static is.codion.framework.domain.TestDomain.*;
import static is.codion.framework.domain.entity.condition.Condition.Combination;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Condition")
public final class ConditionTest {

	private Entities entities;

	@BeforeEach
	void setUp() {
		entities = new TestDomain().entities();
	}

	@Nested
	@DisplayName("Custom Conditions")
	class CustomConditionsTest {

		@Test
		@DisplayName("custom condition has no default values or columns")
		void customCondition_noDefaultValuesOrColumns() {
			Condition condition = Department.NAME_NOT_NULL_CONDITION.get();

			assertTrue(condition.values().isEmpty());
			assertTrue(condition.columns().isEmpty());
		}

		@Test
		@DisplayName("custom condition with mismatched columns and values throws exception")
		void customCondition_mismatchedColumnsAndValues_throwsException() {
			assertThrows(IllegalArgumentException.class,
							() -> Department.NAME_NOT_NULL_CONDITION.get(singletonList(Department.NAME), asList("Test", "Test2")),
							"Column and value count must match");
		}

		@Test
		@DisplayName("custom condition accepts values without columns")
		void customCondition_valuesWithoutColumns_accepted() {
			// Should not throw exception
			assertDoesNotThrow(() -> Department.NAME_NOT_NULL_CONDITION.get(asList("Test", "Test2")));
		}
	}

	@Nested
	@DisplayName("Factory Methods")
	class FactoryMethodsTest {

		@Test
		@DisplayName("keys condition with empty list throws exception")
		void keys_emptyList_throwsException() {
			assertThrows(IllegalArgumentException.class,
							() -> Condition.keys(emptyList()),
							"Should not create condition from empty key list");
		}

		@Test
		@DisplayName("combination with no conditions throws exception")
		void combination_noConditions_throwsException() {
			assertThrows(IllegalArgumentException.class,
							() -> Condition.combination(Conjunction.AND),
							"Should not create empty combination");
		}
	}

	@Nested
	@DisplayName("Foreign Key Conditions")
	class ForeignKeyConditionsTest {

		@Test
		@DisplayName("composite foreign key creates compound condition")
		void foreignKeyCondition_compositeForeignKey_createsCompoundCondition() {
			Entity master = entities.builder(Master2.TYPE)
							.with(Master2.ID_1, 1)
							.with(Master2.ID_2, 2)
							.with(Master2.CODE, 3)
							.build();

			Condition condition = Detail2.MASTER_FK.equalTo(master);

			assertEquals("(master_id = ? AND master_id_2 = ?)",
							condition.toString(entities.definition(Detail2.TYPE)));
		}

		@Test
		@DisplayName("single column foreign key creates simple condition")
		void foreignKeyCondition_singleColumn_createsSimpleCondition() {
			Entity master = entities.builder(Master2.TYPE)
							.with(Master2.ID_1, 1)
							.with(Master2.ID_2, 2)
							.with(Master2.CODE, 3)
							.build();

			Condition condition = Detail2.MASTER_VIA_CODE_FK.equalTo(master);

			assertEquals("master_code = ?",
							condition.toString(entities.definition(Detail2.TYPE)));
		}

		@Test
		@DisplayName("foreign key null conditions format correctly")
		void foreignKeyCondition_null_formatsCorrectly() {
			EntityDefinition definition = entities.definition(Employee.TYPE);

			Condition nullCondition = Employee.DEPARTMENT_FK.isNull();
			assertEquals("deptno IS NULL", nullCondition.toString(definition));

			Condition notNullCondition = Employee.DEPARTMENT_FK.isNotNull();
			assertEquals("deptno IS NOT NULL", notNullCondition.toString(definition));
		}

		@Test
		@DisplayName("foreign key entity conditions format correctly")
		void foreignKeyCondition_entity_formatsCorrectly() {
			Entity department = entities.builder(Department.TYPE)
							.with(Department.ID, 10)
							.build();
			Entity department2 = entities.builder(Department.TYPE)
							.with(Department.ID, 11)
							.build();

			EntityDefinition empDefinition = entities.definition(Employee.TYPE);

			// Equality
			Condition condition = Employee.DEPARTMENT_FK.equalTo(department);
			assertEquals("deptno = ?", condition.toString(empDefinition));

			// IN
			condition = Employee.DEPARTMENT_FK.in(asList(department, department2));
			assertEquals("deptno IN (?, ?)", condition.toString(empDefinition));

			// NOT IN
			condition = Employee.DEPARTMENT_FK.notIn(asList(department, department2));
			assertEquals("deptno NOT IN (?, ?)", condition.toString(empDefinition));
		}

		@Test
		@DisplayName("composite foreign key conditions format correctly")
		void compositeForeignKey_formatsCorrectly() {
			Entity master1 = entities.builder(Master2.TYPE)
							.with(Master2.ID_1, 1)
							.with(Master2.ID_2, 2)
							.build();

			Entity master2 = entities.builder(Master2.TYPE)
							.with(Master2.ID_1, 3)
							.with(Master2.ID_2, 4)
							.build();

			EntityDefinition detailDefinition = entities.definition(Detail2.TYPE);

			// Equality
			Condition condition = Detail2.MASTER_FK.equalTo(master1);
			assertEquals("(master_id = ? AND master_id_2 = ?)", condition.toString(detailDefinition));

			// Inequality
			condition = Detail2.MASTER_FK.notEqualTo(master1);
			assertEquals("(master_id <> ? AND master_id_2 <> ?)", condition.toString(detailDefinition));

			// IN with single value
			condition = Detail2.MASTER_FK.in(singletonList(master1));
			assertEquals("(master_id = ? AND master_id_2 = ?)", condition.toString(detailDefinition));

			// IN with multiple values
			condition = Detail2.MASTER_FK.in(asList(master1, master2));
			assertEquals("((master_id = ? AND master_id_2 = ?) OR (master_id = ? AND master_id_2 = ?))",
							condition.toString(detailDefinition));

			// NOT IN with multiple values
			condition = Detail2.MASTER_FK.notIn(asList(master1, master2));
			assertEquals("((master_id <> ? AND master_id_2 <> ?) OR (master_id <> ? AND master_id_2 <> ?))",
							condition.toString(detailDefinition));

			// NOT IN with single value
			condition = Detail2.MASTER_FK.notIn(singletonList(master1));
			assertEquals("(master_id <> ? AND master_id_2 <> ?)", condition.toString(detailDefinition));
		}
	}

	@Nested
	@DisplayName("Combinations")
	class CombinationsTest {

		@Test
		@DisplayName("AND combination formats correctly")
		void combination_and_formatsCorrectly() {
			Combination combination = Condition.and(
							Detail.STRING.equalTo("value"),
							Detail.INT.equalTo(666));

			EntityDefinition detailDefinition = entities.definition(Detail.TYPE);

			assertEquals("(string = ? AND int = ?)",
							combination.toString(detailDefinition));
		}

		@Test
		@DisplayName("nested combinations format correctly")
		void combination_nested_formatsCorrectly() {
			Combination combination1 = Condition.and(
							Detail.STRING.equalTo("value"),
							Detail.INT.equalTo(666));

			Combination combination2 = Condition.and(
							Detail.DOUBLE.equalTo(666.666),
							Detail.STRING.likeIgnoreCase("valu%e2"));

			Combination combination3 = Condition.or(combination1, combination2);

			EntityDefinition detailDefinition = entities.definition(Detail.TYPE);

			assertEquals("((string = ? AND int = ?) OR (double = ? AND UPPER(string) LIKE UPPER(?)))",
							combination3.toString(detailDefinition));
		}
	}

	@Nested
	@DisplayName("Column Conditions")
	class ColumnConditionsTest {

		@Test
		@DisplayName("column equality condition formats correctly")
		void columnCondition_equality_formatsCorrectly() {
			Condition condition = Department.LOCATION.equalTo("New York");

			assertEquals("loc = ?",
							condition.toString(entities.definition(Department.TYPE)));
			assertNotNull(condition);
		}
	}

	@Nested
	@DisplayName("Key Conditions")
	class KeyConditionsTest {

		@Test
		@DisplayName("composite primary key with null values creates valid condition")
		void compositePrimaryKeyCondition_withNullValues_createsValidCondition() {
			Entity.Key masterKey = entities.key(Master2.TYPE)
							.with(Master2.ID_1, 1)
							.with(Master2.ID_2, null)
							.with(Master2.CODE, 3)
							.build();

			// Should not throw exception
			assertDoesNotThrow(() -> Condition.key(masterKey));

			Entity.Key masterKey2 = entities.key(Master2.TYPE)
							.with(Master2.ID_1, null)
							.with(Master2.ID_2, null)
							.with(Master2.CODE, 42)
							.build();

			// Should not throw exception
			assertDoesNotThrow(() -> Condition.keys(Arrays.asList(masterKey, masterKey2)));
		}

		@Test
		@DisplayName("composite key conditions format correctly")
		void keyCondition_compositeKey_formatsCorrectly() {
			Entity master1 = entities.builder(Master2.TYPE)
							.with(Master2.ID_1, 1)
							.with(Master2.ID_2, 2)
							.build();

			Entity master2 = entities.builder(Master2.TYPE)
							.with(Master2.ID_1, 3)
							.with(Master2.ID_2, 4)
							.build();

			EntityDefinition masterDefinition = entities.definition(Master2.TYPE);

			// Single key
			Condition condition = Condition.key(master1.primaryKey());
			assertEquals("(id = ? AND id2 = ?)", condition.toString(masterDefinition));

			// Multiple keys
			condition = Condition.keys(asList(master1.primaryKey(), master2.primaryKey()));
			assertEquals("((id = ? AND id2 = ?) OR (id = ? AND id2 = ?))",
							condition.toString(masterDefinition));
		}
	}

	@Nested
	@DisplayName("Null Handling")
	class NullHandlingTest {

		@Test
		@DisplayName("null arguments throw NullPointerException")
		void nullArguments_throwNullPointerException() {
			assertThrows(NullPointerException.class,
							() -> Employee.DEPARTMENT_FK.in((Entity[]) null));
			assertThrows(NullPointerException.class,
							() -> Employee.DEPARTMENT_FK.in((Collection<Entity>) null));
			assertThrows(NullPointerException.class,
							() -> Employee.DEPARTMENT_FK.notIn((Entity[]) null));
			assertThrows(NullPointerException.class,
							() -> Employee.DEPARTMENT_FK.notIn((Collection<Entity>) null));
		}

		@Test
		@DisplayName("empty collections throw IllegalArgumentException")
		void emptyCollections_throwIllegalArgumentException() {
			assertThrows(IllegalArgumentException.class,
							() -> Employee.DEPARTMENT_FK.in(emptyList()));
			assertThrows(IllegalArgumentException.class,
							() -> Employee.DEPARTMENT_FK.notIn(emptyList()));
		}

		@Test
		@DisplayName("null value conditions format as IS NULL")
		void nullValueConditions_formatAsIsNull() {
			EntityDefinition empDefinition = entities.definition(Employee.TYPE);

			// isNull()
			Condition condition = Employee.DEPARTMENT_FK.isNull();
			assertEquals("deptno IS NULL", condition.toString(empDefinition));

			// equalTo(null)
			condition = Employee.DEPARTMENT_FK.equalTo(null);
			assertEquals("deptno IS NULL", condition.toString(empDefinition));

			// isNotNull()
			condition = Employee.DEPARTMENT_FK.isNotNull();
			assertEquals("deptno IS NOT NULL", condition.toString(empDefinition));

			// notEqualTo(null)
			condition = Employee.DEPARTMENT_FK.notEqualTo(null);
			assertEquals("deptno IS NOT NULL", condition.toString(empDefinition));
		}

		@Test
		@DisplayName("composite foreign key with null values formats correctly")
		void compositeForeignKey_withNullValues_formatsCorrectly() {
			Entity master1 = entities.builder(Master2.TYPE)
							.with(Master2.ID_1, null)
							.with(Master2.ID_2, null)
							.build();

			EntityDefinition detailDefinition = entities.definition(Detail2.TYPE);

			// All null
			Condition condition = Detail2.MASTER_FK.equalTo(master1);
			assertEquals("(master_id IS NULL AND master_id_2 IS NULL)",
							condition.toString(detailDefinition));

			// Partial null
			master1.set(Master2.ID_2, 1);
			condition = Detail2.MASTER_FK.equalTo(master1);
			assertEquals("(master_id IS NULL AND master_id_2 = ?)",
							condition.toString(detailDefinition));
		}

		@Test
		@DisplayName("foreign key with entity value formats correctly")
		void foreignKey_withEntityValue_formatsCorrectly() {
			Entity dept = entities.builder(Department.TYPE)
							.with(Department.ID, 42)
							.build();

			EntityDefinition empDefinition = entities.definition(Employee.TYPE);
			Condition condition = Employee.DEPARTMENT_FK.equalTo(dept);
			assertEquals("deptno = ?", condition.toString(empDefinition));
		}
	}

	@Nested
	@DisplayName("Validation")
	class ValidationTest {

		@Test
		@DisplayName("keys from different entity types throws exception")
		void keyMismatch_differentEntityTypes_throwsException() {
			Entity master1 = entities.builder(Master2.TYPE)
							.with(Master2.ID_1, 1)
							.with(Master2.ID_2, 2)
							.build();
			Entity detail = entities.builder(Detail2.TYPE)
							.with(Detail2.ID, 3L)
							.build();

			assertThrows(IllegalArgumentException.class,
							() -> Condition.keys(Arrays.asList(master1.primaryKey(), detail.primaryKey())),
							"Keys from different entity types should not be combined");
		}
	}

	@Nested
	@DisplayName("General Conditions")
	class GeneralConditionsTest {

		@Test
		@DisplayName("various condition types format correctly")
		void conditionTest_variousTypes_formatCorrectly() {
			Entity entity = entities.builder(Department.TYPE)
							.with(Department.ID, 10)
							.build();

			EntityDefinition deptDefinition = entities.definition(Department.TYPE);

			// Key condition
			Condition condition = Condition.key(entity.primaryKey());
			assertDepartmentKeyCondition(condition, deptDefinition, "deptno = ?");

			// Not equal
			condition = Department.NAME.notEqualTo("DEPT");
			assertDepartmentCondition(condition, deptDefinition, "dname <> ?", 1);

			// NOT IN
			condition = Department.NAME.notIn("DEPT", "DEPT2");
			assertDepartmentCondition(condition, deptDefinition, "dname NOT IN (?, ?)", 2);

			// Keys IN
			condition = Condition.keys(singletonList(entity.primaryKey()));
			assertDepartmentKeyCondition(condition, deptDefinition, "deptno IN (?)");
		}

		@Test
		@DisplayName("all condition has no values or columns")
		void allCondition_hasNoValuesOrColumns() {
			Condition condition = Condition.all(Department.TYPE);

			assertTrue(condition.values().isEmpty());
			assertTrue(condition.columns().isEmpty());
		}

		@Test
		@DisplayName("non-column attribute in condition throws exception")
		void attributeCondition_withNonColumn_throwsException() {
			EntityDefinition definition = entities.definition(Employee.TYPE);

			assertThrows(IllegalArgumentException.class,
							() -> Employee.DEPARTMENT_LOCATION.isNull().toString(definition),
							"Non-column attributes should not be used in conditions");
		}

		@Test
		@DisplayName("null value arguments throw NullPointerException")
		void conditionNullValues_throwNullPointerException() {
			assertThrows(NullPointerException.class,
							() -> Department.NAME.in((String[]) null));
			assertThrows(NullPointerException.class,
							() -> Department.NAME.in((Collection<String>) null));
			assertThrows(NullPointerException.class,
							() -> Department.NAME.notIn((String[]) null));
			assertThrows(NullPointerException.class,
							() -> Department.NAME.notIn((Collection<String>) null));
		}

		@Test
		@DisplayName("where clause formatting respects column expressions")
		void whereClause_respectsColumnExpressions() {
			EntityDefinition departmentDefinition = entities.definition(Department.TYPE);
			ColumnDefinition<?> columnDefinition = departmentDefinition.columns().definition(Department.NAME);

			// Equality conditions
			Condition condition = Department.NAME.equalTo("upper");
			assertEquals(columnDefinition.expression() + " = ?", condition.toString(departmentDefinition));
			// Like conditions
			condition = Department.NAME.like("upper%");
			assertEquals(columnDefinition.expression() + " LIKE ?", condition.toString(departmentDefinition));

			// NULL conditions
			condition = Department.NAME.isNull();
			assertEquals(columnDefinition.expression() + " IS NULL", condition.toString(departmentDefinition));
			condition = Department.NAME.equalTo(null);
			assertEquals(columnDefinition.expression() + " IS NULL", condition.toString(departmentDefinition));

			// Empty list validation
			assertThrows(IllegalArgumentException.class, () -> Department.NAME.in(emptyList()));

			// Not equal conditions
			condition = Department.NAME.notEqualTo("upper");
			assertEquals(columnDefinition.expression() + " <> ?", condition.toString(departmentDefinition));
			condition = Department.NAME.notLike("upper%");
			assertEquals(columnDefinition.expression() + " NOT LIKE ?", condition.toString(departmentDefinition));

			// NOT NULL conditions
			condition = Department.NAME.isNotNull();
			assertEquals(columnDefinition.expression() + " IS NOT NULL", condition.toString(departmentDefinition));
			condition = Department.NAME.notEqualTo(null);
			assertEquals(columnDefinition.expression() + " IS NOT NULL", condition.toString(departmentDefinition));

			// Empty list validation for NOT IN
			assertThrows(IllegalArgumentException.class, () -> Department.NAME.notIn(emptyList()));

			// Comparison conditions
			condition = Department.NAME.greaterThan("upper");
			assertEquals(columnDefinition.expression() + " > ?", condition.toString(departmentDefinition));
			condition = Department.NAME.greaterThanOrEqualTo("upper");
			assertEquals(columnDefinition.expression() + " >= ?", condition.toString(departmentDefinition));
			condition = Department.NAME.lessThan("upper");
			assertEquals(columnDefinition.expression() + " < ?", condition.toString(departmentDefinition));
			condition = Department.NAME.lessThanOrEqualTo("upper");
			assertEquals(columnDefinition.expression() + " <= ?", condition.toString(departmentDefinition));

			// Between conditions
			condition = Department.NAME.betweenExclusive("upper", "lower");
			assertEquals("(" + columnDefinition.expression() + " > ? AND " + columnDefinition.expression() + " < ?)",
							condition.toString(departmentDefinition));
			condition = Department.NAME.between("upper", "lower");
			assertEquals("(" + columnDefinition.expression() + " >= ? AND " + columnDefinition.expression() + " <= ?)",
							condition.toString(departmentDefinition));

			// Not between conditions
			condition = Department.NAME.notBetweenExclusive("upper", "lower");
			assertEquals("(" + columnDefinition.expression() + " < ? OR " + columnDefinition.expression() + " > ?)",
							condition.toString(departmentDefinition));
			condition = Department.NAME.notBetween("upper", "lower");
			assertEquals("(" + columnDefinition.expression() + " <= ? OR " + columnDefinition.expression() + " >= ?)",
							condition.toString(departmentDefinition));

			// Case-sensitive vs case-insensitive
			columnDefinition = departmentDefinition.columns().definition(Department.CODE);
			condition = Department.CODE.equalTo('h');
			assertEquals(columnDefinition.expression() + " = ?", condition.toString(departmentDefinition));
			condition = Department.CODE.equalToIgnoreCase('h');
			assertEquals("UPPER(" + columnDefinition.expression() + ") = UPPER(?)",
							condition.toString(departmentDefinition));
			condition = Department.CODE.notEqualTo('h');
			assertEquals(columnDefinition.expression() + " <> ?", condition.toString(departmentDefinition));
			condition = Department.CODE.notEqualToIgnoreCase('h');
			assertEquals("UPPER(" + columnDefinition.expression() + ") <> UPPER(?)",
							condition.toString(departmentDefinition));
		}

		private void assertDepartmentKeyCondition(Condition condition, EntityDefinition departmentDefinition,
																							String conditionString) {
			assertEquals(conditionString, condition.toString(departmentDefinition));
			assertEquals(1, condition.values().size());
			assertEquals(1, condition.columns().size());
			assertEquals(10, condition.values().get(0));
			assertEquals(Department.ID, condition.columns().get(0));
		}

		private void assertDepartmentCondition(Condition condition, EntityDefinition departmentDefinition,
																					 String conditionString, int valueCount) {
			assertEquals(conditionString, condition.toString(departmentDefinition));
			assertEquals(valueCount, condition.values().size());
			assertEquals(valueCount, condition.columns().size());
			assertEquals("DEPT", condition.values().get(0));
			assertEquals(Department.NAME, condition.columns().get(0));
		}
	}

	@Nested
	@DisplayName("Equality")
	class EqualityTest {

		@Test
		@DisplayName("condition equality test")
		void equals_conditions_compareCorrectly() {
			Condition condition1 = Condition.all(Department.TYPE);
			Condition condition2 = Condition.all(Department.TYPE);
			assertEquals(condition1, condition2);

			condition2 = Condition.all(Employee.TYPE);
			assertNotEquals(condition1, condition2);

			// Key conditions
			Entity.Key key1 = entities.primaryKey(Employee.TYPE, 1);
			Entity.Key key2 = entities.primaryKey(Employee.TYPE, 2);
			condition1 = Condition.key(key1);
			condition2 = Condition.key(key1);
			assertEquals(condition1, condition2);
			condition2 = Condition.key(key2);
			assertNotEquals(condition1, condition2);

			// Null conditions
			condition1 = Employee.DEPARTMENT_FK.isNull();
			condition2 = Employee.DEPARTMENT_FK.isNull();
			assertEquals(condition1, condition2);
			condition2 = Employee.DEPARTMENT_FK.isNotNull();
			assertNotEquals(condition1, condition2);

			condition1 = Employee.ID.equalTo(0);
			condition2 = Employee.ID.equalTo(0);
			assertEquals(condition1, condition2);
			condition2 = Employee.ID.equalTo(1);
			assertNotEquals(condition1, condition2);
			assertEquals(Employee.ID.equalTo(null), Employee.ID.isNull());

			condition1 = Employee.NAME.equalTo("Luke");
			condition2 = Employee.NAME.equalTo("Luke");
			assertEquals(condition1, condition2);
			condition2 = Employee.NAME.equalToIgnoreCase("Luke");
			assertNotEquals(condition1, condition2);

			condition1 = Employee.ID.notEqualTo(0);
			condition2 = Employee.ID.notEqualTo(0);
			assertEquals(condition1, condition2);
			condition2 = Employee.ID.equalTo(0);
			assertNotEquals(condition1, condition2);
			assertEquals(Employee.ID.notEqualTo(null), Employee.ID.isNotNull());

			condition1 = Employee.ID.notEqualTo(0);
			condition2 = Employee.ID.notEqualTo(0);
			assertEquals(condition1, condition2);
			condition2 = Employee.ID.notEqualTo(1);
			assertNotEquals(condition1, condition2);

			condition1 = Employee.ID.lessThan(0);
			condition2 = Employee.ID.lessThan(0);
			assertEquals(condition1, condition2);
			condition2 = Employee.ID.lessThan(1);
			assertNotEquals(condition1, condition2);
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.lessThan(null));

			condition1 = Employee.ID.lessThanOrEqualTo(0);
			condition2 = Employee.ID.lessThanOrEqualTo(0);
			assertEquals(condition1, condition2);
			condition2 = Employee.ID.lessThanOrEqualTo(1);
			assertNotEquals(condition1, condition2);
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.lessThanOrEqualTo(null));

			condition1 = Employee.ID.greaterThan(0);
			condition2 = Employee.ID.greaterThan(0);
			assertEquals(condition1, condition2);
			condition2 = Employee.ID.greaterThan(1);
			assertNotEquals(condition1, condition2);
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.greaterThan(null));

			condition1 = Employee.ID.greaterThanOrEqualTo(0);
			condition2 = Employee.ID.greaterThanOrEqualTo(0);
			assertEquals(condition1, condition2);
			condition2 = Employee.ID.greaterThanOrEqualTo(1);
			assertNotEquals(condition1, condition2);
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.greaterThanOrEqualTo(null));

			condition1 = Employee.ID.between(0, 1);
			condition2 = Employee.ID.between(0, 1);
			assertEquals(condition1, condition2);
			condition2 = Employee.ID.between(1, 0);
			assertNotEquals(condition1, condition2);
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.between(1, null));
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.between(null, 1));
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.between(null, null));

			condition1 = Employee.ID.betweenExclusive(0, 1);
			condition2 = Employee.ID.betweenExclusive(0, 1);
			assertEquals(condition1, condition2);
			condition2 = Employee.ID.betweenExclusive(1, 0);
			assertNotEquals(condition1, condition2);
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.betweenExclusive(1, null));
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.betweenExclusive(null, 1));
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.betweenExclusive(null, null));

			condition1 = Employee.ID.notBetween(0, 1);
			condition2 = Employee.ID.notBetween(0, 1);
			assertEquals(condition1, condition2);
			condition2 = Employee.ID.notBetween(1, 0);
			assertNotEquals(condition1, condition2);
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.notBetween(1, null));
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.notBetween(null, 1));
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.notBetween(null, null));

			condition1 = Employee.ID.notBetweenExclusive(0, 1);
			condition2 = Employee.ID.notBetweenExclusive(0, 1);
			assertEquals(condition1, condition2);
			condition2 = Employee.ID.notBetweenExclusive(1, 0);
			assertNotEquals(condition1, condition2);
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.notBetweenExclusive(1, null));
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.notBetweenExclusive(null, 1));
			assertThrows(IllegalArgumentException.class, () -> Employee.ID.notBetweenExclusive(null, null));

			condition1 = Department.CONDITION.get(Department.NAME, "Test");
			condition2 = Department.CONDITION.get(singletonList(Department.NAME), singletonList("Test"));
			assertEquals(condition1, condition2);

			condition1 = Condition.or(Employee.ID.equalTo(0),
							Employee.ID.equalTo(1));
			condition2 = Condition.or(Employee.ID.equalTo(0),
							Employee.ID.equalTo(1));
			assertEquals(condition1, condition2);
			condition2 = Condition.or(Employee.ID.equalTo(1),
							Employee.ID.equalTo(0));
			assertNotEquals(condition1, condition2);

			condition1 = Condition.or(Employee.ID.equalTo(0),
							Employee.NAME.equalTo("Luke"));
			condition2 = Condition.or(Employee.ID.equalTo(0),
							Employee.NAME.equalTo("Luke"));
			assertEquals(condition1, condition2);
			condition2 = Condition.or(Employee.ID.equalTo(0),
							Employee.NAME.equalTo("Lukas"));
			assertNotEquals(condition1, condition2);

			condition1 = Employee.NAME.equalTo("Luke");
			condition2 = condition1;
			assertEquals(condition1, condition2);

			condition2 = Employee.NAME.greaterThanOrEqualTo("Luke");
			assertNotEquals(condition1, condition2);
			assertNotEquals(condition2, condition1);

			condition1 = Employee.NAME.lessThanOrEqualTo("Luke");
			condition2 = condition1;
			assertEquals(condition1, condition2);

			condition2 = Employee.NAME.greaterThanOrEqualTo("Luke");
			assertNotEquals(condition1, condition2);
			assertNotEquals(condition2, condition1);

			condition1 = Employee.NAME.betweenExclusive("John", "Luke");
			condition2 = condition1;
			assertEquals(condition1, condition2);

			condition2 = Employee.NAME.greaterThanOrEqualTo("Luke");
			assertNotEquals(condition1, condition2);
			assertNotEquals(condition2, condition1);

			condition1 = Employee.NAME.notBetweenExclusive("John", "Luke");
			condition2 = condition1;
			assertEquals(condition1, condition2);

			condition2 = Employee.NAME.lessThanOrEqualTo("Luke");
			assertNotEquals(condition1, condition2);
			assertNotEquals(condition2, condition1);
		}
	}
}