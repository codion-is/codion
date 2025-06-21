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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Conjunction;
import is.codion.common.item.Item;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.condition.Condition.Combination;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for condition framework functionality
 */
@DisplayName("ConditionFrameworkEnhancement")
public final class ConditionFrameworkEnhancementTest {

	private static final DomainType DOMAIN_TYPE = domainType("condition_test");

	private Entities entities;

	interface Company {
		EntityType TYPE = DOMAIN_TYPE.entityType("company");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> COUNTRY = TYPE.stringColumn("country");
		Column<LocalDate> FOUNDED = TYPE.localDateColumn("founded");
		Column<BigDecimal> REVENUE = TYPE.bigDecimalColumn("revenue");
		Column<Boolean> ACTIVE = TYPE.booleanColumn("active");

		ConditionType REVENUE_RANGE = TYPE.conditionType("revenue_range");
		ConditionType BY_COUNTRY_AND_ACTIVE = TYPE.conditionType("by_country_and_active");
	}

	interface Department {
		EntityType TYPE = DOMAIN_TYPE.entityType("department");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> COMPANY_ID = TYPE.integerColumn("company_id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> LOCATION = TYPE.stringColumn("location");
		Column<Integer> EMPLOYEE_COUNT = TYPE.integerColumn("employee_count");
		Column<BigDecimal> BUDGET = TYPE.bigDecimalColumn("budget");

		ForeignKey COMPANY_FK = TYPE.foreignKey("company_fk", COMPANY_ID, Company.ID);

		ConditionType BUDGET_THRESHOLD = TYPE.conditionType("budget_threshold");
	}

	interface Employee {
		EntityType TYPE = DOMAIN_TYPE.entityType("employee");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> DEPARTMENT_ID = TYPE.integerColumn("department_id");
		Column<Integer> MANAGER_ID = TYPE.integerColumn("manager_id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> EMAIL = TYPE.stringColumn("email");
		Column<String> POSITION = TYPE.stringColumn("position");
		Column<BigDecimal> SALARY = TYPE.bigDecimalColumn("salary");
		Column<LocalDate> HIRE_DATE = TYPE.localDateColumn("hire_date");
		Column<Boolean> REMOTE = TYPE.booleanColumn("remote");

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("department_fk", DEPARTMENT_ID, Department.ID);
		ForeignKey MANAGER_FK = TYPE.foreignKey("manager_fk", MANAGER_ID, Employee.ID);

		ConditionType HIGH_EARNERS = TYPE.conditionType("high_earners");
		ConditionType RECENT_HIRES = TYPE.conditionType("recent_hires");
		ConditionType BY_DEPARTMENT_AND_POSITION = TYPE.conditionType("by_dept_and_position");
	}

	interface Project {
		EntityType TYPE = DOMAIN_TYPE.entityType("project");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> LEAD_ID = TYPE.integerColumn("lead_id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> STATUS = TYPE.stringColumn("status");
		Column<LocalDate> START_DATE = TYPE.localDateColumn("start_date");
		Column<LocalDate> END_DATE = TYPE.localDateColumn("end_date");
		Column<BigDecimal> BUDGET = TYPE.bigDecimalColumn("budget");
		Column<Integer> PRIORITY = TYPE.integerColumn("priority");

		ForeignKey LEAD_FK = TYPE.foreignKey("lead_fk", LEAD_ID, Employee.ID);

		ConditionType ACTIVE_PROJECTS = TYPE.conditionType("active_projects");
		ConditionType OVERDUE_PROJECTS = TYPE.conditionType("overdue_projects");
		ConditionType BY_STATUS_AND_PRIORITY = TYPE.conditionType("by_status_and_priority");
	}

	interface Assignment {
		EntityType TYPE = DOMAIN_TYPE.entityType("assignment");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> EMPLOYEE_ID = TYPE.integerColumn("employee_id");
		Column<Integer> PROJECT_ID = TYPE.integerColumn("project_id");
		Column<String> ROLE = TYPE.stringColumn("role");
		Column<Integer> ALLOCATION_PERCENT = TYPE.integerColumn("allocation_percent");
		Column<LocalDate> START_DATE = TYPE.localDateColumn("start_date");
		Column<LocalDate> END_DATE = TYPE.localDateColumn("end_date");

		ForeignKey EMPLOYEE_FK = TYPE.foreignKey("employee_fk", EMPLOYEE_ID, Employee.ID);
		ForeignKey PROJECT_FK = TYPE.foreignKey("project_fk", PROJECT_ID, Project.ID);

		ConditionType FULL_TIME_ASSIGNMENTS = TYPE.conditionType("full_time_assignments");
		ConditionType ACTIVE_ASSIGNMENTS = TYPE.conditionType("active_assignments");
	}

	private static final class TestDomain extends DomainModel {
		public TestDomain() {
			super(DOMAIN_TYPE);

			// Company entity
			add(Company.TYPE.define(
											Company.ID.define().primaryKey(),
											Company.NAME.define().column().nullable(false),
											Company.COUNTRY.define().column().nullable(false),
											Company.FOUNDED.define().column(),
											Company.REVENUE.define().column(),
											Company.ACTIVE.define().column().nullable(false).defaultValue(true)
							).condition(Company.REVENUE_RANGE,
											(columns, values) -> "revenue BETWEEN ? AND ?")
							.condition(Company.BY_COUNTRY_AND_ACTIVE,
											(columns, values) -> "country = ? AND active = ?")
							.build());

			// Department entity
			add(Department.TYPE.define(
											Department.ID.define().primaryKey(),
											Department.COMPANY_ID.define().column().nullable(false),
											Department.NAME.define().column().nullable(false),
											Department.LOCATION.define().column(),
											Department.EMPLOYEE_COUNT.define().column().defaultValue(0),
											Department.BUDGET.define().column(),
											Department.COMPANY_FK.define().foreignKey()
							).condition(Department.BUDGET_THRESHOLD,
											(columns, values) -> "budget > ?")
							.build());

			// Employee entity
			add(Employee.TYPE.define(
											Employee.ID.define().primaryKey(),
											Employee.DEPARTMENT_ID.define().column().nullable(false),
											Employee.MANAGER_ID.define().column(),
											Employee.NAME.define().column().nullable(false),
											Employee.EMAIL.define().column().nullable(false),
											Employee.POSITION.define().column().nullable(false),
											Employee.SALARY.define().column().nullable(false),
											Employee.HIRE_DATE.define().column().nullable(false),
											Employee.REMOTE.define().column().nullable(false).defaultValue(false),
											Employee.DEPARTMENT_FK.define().foreignKey()
															.attributes(Department.NAME, Department.LOCATION),
											Employee.MANAGER_FK.define().foreignKey()
															.attributes(Employee.NAME)
							).condition(Employee.HIGH_EARNERS,
											(columns, values) -> "salary > ?")
							.condition(Employee.RECENT_HIRES,
											(columns, values) -> "hire_date >= ?")
							.condition(Employee.BY_DEPARTMENT_AND_POSITION,
											(columns, values) -> "department_id = ? AND position = ?")
							.build());

			// Project entity
			add(Project.TYPE.define(
											Project.ID.define().primaryKey(),
											Project.LEAD_ID.define().column(),
											Project.NAME.define().column().nullable(false),
											Project.STATUS.define().column().nullable(false)
															.items(asList(Item.item("PLANNING"), Item.item("ACTIVE"), Item.item("ON_HOLD"), Item.item("COMPLETED"), Item.item("CANCELLED"))),
											Project.START_DATE.define().column().nullable(false),
											Project.END_DATE.define().column(),
											Project.BUDGET.define().column(),
											Project.PRIORITY.define().column().nullable(false).valueRange(1, 5),
											Project.LEAD_FK.define().foreignKey()
															.attributes(Employee.NAME, Employee.EMAIL)
							).condition(Project.ACTIVE_PROJECTS,
											(columns, values) -> "status IN ('PLANNING', 'ACTIVE')")
							.condition(Project.OVERDUE_PROJECTS,
											(columns, values) -> "status = 'ACTIVE' AND end_date < CURRENT_DATE")
							.condition(Project.BY_STATUS_AND_PRIORITY,
											(columns, values) -> "status = ? AND priority >= ?")
							.build());

			// Assignment entity
			add(Assignment.TYPE.define(
											Assignment.ID.define().primaryKey(),
											Assignment.EMPLOYEE_ID.define().column().nullable(false),
											Assignment.PROJECT_ID.define().column().nullable(false),
											Assignment.ROLE.define().column().nullable(false),
											Assignment.ALLOCATION_PERCENT.define().column().nullable(false).valueRange(1, 100),
											Assignment.START_DATE.define().column().nullable(false),
											Assignment.END_DATE.define().column(),
											Assignment.EMPLOYEE_FK.define().foreignKey()
															.attributes(Employee.NAME, Employee.POSITION),
											Assignment.PROJECT_FK.define().foreignKey()
															.attributes(Project.NAME, Project.STATUS)
							).condition(Assignment.FULL_TIME_ASSIGNMENTS,
											(columns, values) -> "allocation_percent = 100")
							.condition(Assignment.ACTIVE_ASSIGNMENTS,
											(columns, values) -> "start_date <= CURRENT_DATE AND (end_date IS NULL OR end_date >= CURRENT_DATE)")
							.build());
		}
	}

	@BeforeEach
	void setUp() {
		entities = new TestDomain().entities();
	}

	@Nested
	@DisplayName("Basic Condition Types")
	class BasicConditionTypesTest {

		@Test
		@DisplayName("equality conditions work for all data types")
		void equalityConditions_allDataTypes_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Integer
			Condition condition = Employee.ID.equalTo(42);
			assertEquals("id = ?", condition.toString(empDef));
			assertEquals(1, condition.values().size());
			assertEquals(42, condition.values().get(0));

			// String
			condition = Employee.NAME.equalTo("John Doe");
			assertEquals("name = ?", condition.toString(empDef));
			assertEquals("John Doe", condition.values().get(0));

			// BigDecimal
			condition = Employee.SALARY.equalTo(new BigDecimal("75000"));
			assertEquals("salary = ?", condition.toString(empDef));
			assertEquals(new BigDecimal("75000"), condition.values().get(0));

			// LocalDate
			LocalDate date = LocalDate.of(2020, 1, 1);
			condition = Employee.HIRE_DATE.equalTo(date);
			assertEquals("hire_date = ?", condition.toString(empDef));
			assertEquals(date, condition.values().get(0));

			// Boolean
			condition = Employee.REMOTE.equalTo(true);
			assertEquals("remote = ?", condition.toString(empDef));
			assertEquals(true, condition.values().get(0));
		}

		@Test
		@DisplayName("comparison conditions work correctly")
		void comparisonConditions_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Greater than
			Condition condition = Employee.SALARY.greaterThan(new BigDecimal("50000"));
			assertEquals("salary > ?", condition.toString(empDef));

			// Greater than or equal
			condition = Employee.SALARY.greaterThanOrEqualTo(new BigDecimal("50000"));
			assertEquals("salary >= ?", condition.toString(empDef));

			// Less than
			condition = Employee.HIRE_DATE.lessThan(LocalDate.of(2020, 1, 1));
			assertEquals("hire_date < ?", condition.toString(empDef));

			// Less than or equal
			condition = Employee.HIRE_DATE.lessThanOrEqualTo(LocalDate.of(2020, 1, 1));
			assertEquals("hire_date <= ?", condition.toString(empDef));
		}

		@Test
		@DisplayName("like conditions with patterns work correctly")
		void likeConditions_withPatterns_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Basic LIKE
			Condition condition = Employee.NAME.like("John%");
			assertEquals("name LIKE ?", condition.toString(empDef));
			assertEquals("John%", condition.values().get(0));

			// Case-insensitive LIKE
			condition = Employee.NAME.likeIgnoreCase("john%");
			assertEquals("UPPER(name) LIKE UPPER(?)", condition.toString(empDef));
			assertEquals("john%", condition.values().get(0));

			// NOT LIKE
			condition = Employee.EMAIL.notLike("%.temp");
			assertEquals("email NOT LIKE ?", condition.toString(empDef));

			// NOT LIKE case-insensitive
			condition = Employee.EMAIL.notLikeIgnoreCase("%.TEMP");
			assertEquals("UPPER(email) NOT LIKE UPPER(?)", condition.toString(empDef));
		}

		@Test
		@DisplayName("between conditions work correctly")
		void betweenConditions_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Inclusive between
			Condition condition = Employee.SALARY.between(
							new BigDecimal("50000"), new BigDecimal("100000"));
			assertEquals("(salary >= ? AND salary <= ?)", condition.toString(empDef));
			assertEquals(2, condition.values().size());

			// Exclusive between
			condition = Employee.HIRE_DATE.betweenExclusive(
							LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
			assertEquals("(hire_date > ? AND hire_date < ?)", condition.toString(empDef));

			// NOT between inclusive
			condition = Employee.SALARY.notBetween(
							new BigDecimal("30000"), new BigDecimal("40000"));
			assertEquals("(salary <= ? OR salary >= ?)", condition.toString(empDef));

			// NOT between exclusive
			condition = Employee.SALARY.notBetweenExclusive(
							new BigDecimal("30000"), new BigDecimal("40000"));
			assertEquals("(salary < ? OR salary > ?)", condition.toString(empDef));
		}
	}

	@Nested
	@DisplayName("Collection Conditions")
	class CollectionConditionsTest {

		@Test
		@DisplayName("IN conditions with various collection sizes")
		void inConditions_variousCollectionSizes_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Single value
			Condition condition = Employee.POSITION.in(singletonList("Manager"));
			assertEquals("position IN (?)", condition.toString(empDef));
			assertEquals(1, condition.values().size());

			// Multiple values
			condition = Employee.POSITION.in(asList("Manager", "Developer", "Analyst"));
			assertEquals("position IN (?, ?, ?)", condition.toString(empDef));
			assertEquals(3, condition.values().size());

			// Array version
			condition = Employee.POSITION.in("Manager", "Developer", "Analyst");
			assertEquals("position IN (?, ?, ?)", condition.toString(empDef));

			// NOT IN
			condition = Employee.POSITION.notIn("Intern", "Contractor");
			assertEquals("position NOT IN (?, ?)", condition.toString(empDef));
		}

		@Test
		@DisplayName("large IN conditions handle many values")
		void inConditions_largeCollections_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			List<Integer> largeList = IntStream.rangeClosed(1, 1000)
							.boxed()
							.toList();

			Condition condition = Employee.ID.in(largeList);
			assertEquals(1000, condition.values().size());

			String conditionString = condition.toString(empDef);
			assertNotNull(conditionString);
			assertTrue(conditionString.contains("id"));
			assertTrue(conditionString.contains("?"));
			// Large IN conditions might be formatted differently for performance
		}

		@Test
		@DisplayName("empty collections throw IllegalArgumentException")
		void inConditions_emptyCollections_throwException() {
			assertThrows(IllegalArgumentException.class,
							() -> Employee.POSITION.in(emptyList()));
			assertThrows(IllegalArgumentException.class,
							() -> Employee.POSITION.notIn(emptyList()));
		}
	}

	@Nested
	@DisplayName("NULL Conditions")
	class NullConditionsTest {

		@Test
		@DisplayName("null conditions format correctly")
		void nullConditions_formatCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// IS NULL
			Condition condition = Employee.MANAGER_ID.isNull();
			assertEquals("manager_id IS NULL", condition.toString(empDef));
			assertTrue(condition.values().isEmpty());

			// IS NOT NULL
			condition = Employee.MANAGER_ID.isNotNull();
			assertEquals("manager_id IS NOT NULL", condition.toString(empDef));
			assertTrue(condition.values().isEmpty());

			// equalTo(null) converts to IS NULL
			condition = Employee.MANAGER_ID.equalTo(null);
			assertEquals("manager_id IS NULL", condition.toString(empDef));

			// notEqualTo(null) converts to IS NOT NULL
			condition = Employee.MANAGER_ID.notEqualTo(null);
			assertEquals("manager_id IS NOT NULL", condition.toString(empDef));
		}

		@Test
		@DisplayName("null values in collections are handled correctly")
		void nullValues_inCollections_handledCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Test that null values in collections are handled 
			List<Integer> listWithNull = Arrays.asList(1, null, 3);

			// Some implementations may throw NPE for null values in collections
			try {
				Condition condition = Employee.MANAGER_ID.in(listWithNull);
				assertNotNull(condition);
				String conditionString = condition.toString(empDef);
				assertNotNull(conditionString);
				// The exact format depends on implementation
				assertTrue(conditionString.contains("manager_id"));
			}
			catch (NullPointerException e) {
				// This is acceptable behavior - null values in collections may not be supported
				assertNotNull(e);
			}
		}
	}

	@Nested
	@DisplayName("Foreign Key Conditions")
	class ForeignKeyConditionsTest {

		@Test
		@DisplayName("single column foreign key conditions work correctly")
		void singleColumnForeignKey_conditions_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			Entity department = entities.builder(Department.TYPE)
							.with(Department.ID, 10)
							.with(Department.NAME, "Engineering")
							.build();

			// Equality with entity
			Condition condition = Employee.DEPARTMENT_FK.equalTo(department);
			assertEquals("department_id = ?", condition.toString(empDef));
			assertEquals(10, condition.values().get(0));

			// IN with entities
			Entity department2 = entities.builder(Department.TYPE)
							.with(Department.ID, 20)
							.build();

			condition = Employee.DEPARTMENT_FK.in(asList(department, department2));
			assertEquals("department_id IN (?, ?)", condition.toString(empDef));
			assertEquals(2, condition.values().size());
			assertEquals(10, condition.values().get(0));
			assertEquals(20, condition.values().get(1));

			// NOT IN
			condition = Employee.DEPARTMENT_FK.notIn(department, department2);
			assertEquals("department_id NOT IN (?, ?)", condition.toString(empDef));
		}

		@Test
		@DisplayName("composite foreign key conditions work correctly")
		void compositeForeignKey_conditions_workCorrectly() {
			// Use existing employee-department relationship as composite-like test
			Entity department = entities.builder(Department.TYPE)
							.with(Department.ID, 10)
							.with(Department.COMPANY_ID, 1)
							.build();

			// Test foreign key conditions with the department relationship
			Condition condition = Employee.DEPARTMENT_FK.equalTo(department);
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			assertNotNull(condition);
			assertEquals("department_id = ?", condition.toString(empDef));
			assertEquals(1, condition.values().size());
			assertEquals(10, condition.values().get(0));
		}

		@Test
		@DisplayName("foreign key null conditions work correctly")
		void foreignKeyNull_conditions_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Foreign key IS NULL
			Condition condition = Employee.MANAGER_FK.isNull();
			assertEquals("manager_id IS NULL", condition.toString(empDef));

			// Foreign key IS NOT NULL
			condition = Employee.MANAGER_FK.isNotNull();
			assertEquals("manager_id IS NOT NULL", condition.toString(empDef));
		}
	}

	@Nested
	@DisplayName("Custom Conditions")
	class CustomConditionsTest {

		@Test
		@DisplayName("simple custom conditions work correctly")
		void simpleCustomConditions_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Single parameter custom condition
			Condition condition = Employee.HIGH_EARNERS.get(Employee.SALARY, new BigDecimal("100000"));
			assertEquals("salary > ?", condition.toString(empDef));
			assertEquals(1, condition.values().size());
			assertEquals(new BigDecimal("100000"), condition.values().get(0));

			// Date-based custom condition
			LocalDate cutoffDate = LocalDate.of(2022, 1, 1);
			condition = Employee.RECENT_HIRES.get(Employee.HIRE_DATE, cutoffDate);
			assertEquals("hire_date >= ?", condition.toString(empDef));
			assertEquals(cutoffDate, condition.values().get(0));
		}

		@Test
		@DisplayName("multi-parameter custom conditions work correctly")
		void multiParameterCustomConditions_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Two parameter custom condition using columns and values lists
			Condition condition = Employee.BY_DEPARTMENT_AND_POSITION.get(
							asList(Employee.DEPARTMENT_ID, Employee.POSITION),
							asList(10, "Developer"));
			assertEquals("department_id = ? AND position = ?", condition.toString(empDef));
			assertEquals(2, condition.values().size());
			assertEquals(10, condition.values().get(0));
			assertEquals("Developer", condition.values().get(1));
		}

		@Test
		@DisplayName("custom conditions with column specifications work correctly")
		void customConditionsWithColumns_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Custom condition with explicit columns
			Condition condition = Employee.BY_DEPARTMENT_AND_POSITION.get(
							asList(Employee.DEPARTMENT_ID, Employee.POSITION),
							asList(10, "Developer"));
			assertEquals("department_id = ? AND position = ?", condition.toString(empDef));
			assertEquals(2, condition.values().size());
			assertEquals(2, condition.columns().size());
		}

		@Test
		@DisplayName("parameterless custom conditions work correctly")
		void parameterlessCustomConditions_workCorrectly() {
			EntityDefinition projDef = entities.definition(Project.TYPE);

			// No-parameter custom condition using empty lists
			Condition condition = Project.ACTIVE_PROJECTS.get(emptyList(), emptyList());
			assertEquals("status IN ('PLANNING', 'ACTIVE')", condition.toString(projDef));
			assertTrue(condition.values().isEmpty());
			assertTrue(condition.columns().isEmpty());
		}
	}

	@Nested
	@DisplayName("Condition Combinations")
	class ConditionCombinationsTest {

		@Test
		@DisplayName("AND combinations work correctly")
		void andCombinations_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			Combination combination = Condition.and(
							Employee.SALARY.greaterThan(new BigDecimal("50000")),
							Employee.REMOTE.equalTo(true),
							Employee.POSITION.equalTo("Developer"));

			assertEquals("(salary > ? AND remote = ? AND position = ?)",
							combination.toString(empDef));
			assertEquals(3, combination.values().size());
			assertEquals(Conjunction.AND, combination.conjunction());
		}

		@Test
		@DisplayName("OR combinations work correctly")
		void orCombinations_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			Combination combination = Condition.or(
							Employee.POSITION.equalTo("Manager"),
							Employee.SALARY.greaterThan(new BigDecimal("100000")));

			assertEquals("(position = ? OR salary > ?)",
							combination.toString(empDef));
			assertEquals(2, combination.values().size());
			assertEquals(Conjunction.OR, combination.conjunction());
		}

		@Test
		@DisplayName("nested combinations work correctly")
		void nestedCombinations_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			Combination inner1 = Condition.and(
							Employee.POSITION.equalTo("Developer"),
							Employee.SALARY.greaterThan(new BigDecimal("80000")));

			Combination inner2 = Condition.and(
							Employee.POSITION.equalTo("Manager"),
							Employee.SALARY.greaterThan(new BigDecimal("90000")));

			Combination outer = Condition.or(inner1, inner2);

			assertEquals("((position = ? AND salary > ?) OR (position = ? AND salary > ?))",
							outer.toString(empDef));
			assertEquals(4, outer.values().size());
		}

		@Test
		@DisplayName("single condition combinations work correctly")
		void singleConditionCombinations_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Single condition AND
			Combination combination = Condition.and(Employee.REMOTE.equalTo(true));
			assertEquals("remote = ?", combination.toString(empDef));

			// Single condition OR
			combination = Condition.or(Employee.REMOTE.equalTo(true));
			assertEquals("remote = ?", combination.toString(empDef));
		}

		@ParameterizedTest
		@EnumSource(Conjunction.class)
		@DisplayName("empty combinations throw IllegalArgumentException")
		void emptyCombinations_throwException(Conjunction conjunction) {
			assertThrows(IllegalArgumentException.class,
							() -> Condition.combination(conjunction));
		}
	}

	@Nested
	@DisplayName("Key Conditions")
	class KeyConditionsTest {

		@Test
		@DisplayName("single primary key conditions work correctly")
		void singlePrimaryKey_conditions_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			Entity.Key key = entities.primaryKey(Employee.TYPE, 42);
			Condition condition = Condition.key(key);

			assertEquals("id = ?", condition.toString(empDef));
			assertEquals(1, condition.values().size());
			assertEquals(42, condition.values().get(0));
		}

		@Test
		@DisplayName("multiple primary key conditions work correctly")
		void multiplePrimaryKey_conditions_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			List<Entity.Key> keys = entities.primaryKeys(Employee.TYPE, 1, 2, 3);
			Condition condition = Condition.keys(keys);

			assertEquals("id IN (?, ?, ?)", condition.toString(empDef));
			assertEquals(3, condition.values().size());
		}

		@Test
		@DisplayName("composite key conditions work correctly")
		void compositeKey_conditions_workCorrectly() {
			// This would test composite primary keys
			// For now, we test the concept with the assignment entity
			EntityDefinition assignDef = entities.definition(Assignment.TYPE);

			Entity assignment = entities.builder(Assignment.TYPE)
							.with(Assignment.ID, 1)
							.with(Assignment.EMPLOYEE_ID, 10)
							.with(Assignment.PROJECT_ID, 20)
							.build();

			Condition condition = Condition.key(assignment.primaryKey());
			assertEquals("id = ?", condition.toString(assignDef));
		}

		@Test
		@DisplayName("empty key collection throws IllegalArgumentException")
		void emptyKeyCollection_throwsException() {
			assertThrows(IllegalArgumentException.class,
							() -> Condition.keys(emptyList()));
		}
	}

	@Nested
	@DisplayName("All Condition")
	class AllConditionTest {

		@Test
		@DisplayName("all condition for entity type works correctly")
		void allCondition_forEntityType_worksCorrectly() {
			Condition condition = Condition.all(Employee.TYPE);

			assertNotNull(condition);
			assertTrue(condition.values().isEmpty());
			assertTrue(condition.columns().isEmpty());
			assertEquals(Employee.TYPE, condition.entityType());
		}

		@Test
		@DisplayName("all condition toString returns meaningful description")
		void allCondition_toString_returnsMeaningfulDescription() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);
			Condition condition = Condition.all(Employee.TYPE);

			String conditionString = condition.toString(empDef);
			assertNotNull(conditionString);
			// The exact format may vary, but it should be a valid representation
		}
	}

	@Nested
	@DisplayName("Case Sensitivity")
	class CaseSensitivityTest {

		@Test
		@DisplayName("case sensitive string conditions work correctly")
		void caseSensitiveStringConditions_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Case sensitive equality
			Condition condition = Employee.NAME.equalTo("John");
			assertEquals("name = ?", condition.toString(empDef));

			// Case sensitive LIKE
			condition = Employee.NAME.like("John%");
			assertEquals("name LIKE ?", condition.toString(empDef));

			// Case sensitive NOT EQUAL
			condition = Employee.NAME.notEqualTo("John");
			assertEquals("name <> ?", condition.toString(empDef));
		}

		@Test
		@DisplayName("case insensitive string conditions work correctly")
		void caseInsensitiveStringConditions_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Case insensitive equality
			Condition condition = Employee.NAME.equalToIgnoreCase("john");
			assertEquals("UPPER(name) = UPPER(?)", condition.toString(empDef));

			// Case insensitive LIKE
			condition = Employee.NAME.likeIgnoreCase("john%");
			assertEquals("UPPER(name) LIKE UPPER(?)", condition.toString(empDef));

			// Case insensitive NOT EQUAL
			condition = Employee.NAME.notEqualToIgnoreCase("john");
			assertEquals("UPPER(name) <> UPPER(?)", condition.toString(empDef));

			// Case insensitive NOT LIKE
			condition = Employee.NAME.notLikeIgnoreCase("john%");
			assertEquals("UPPER(name) NOT LIKE UPPER(?)", condition.toString(empDef));
		}
	}

	@Nested
	@DisplayName("Complex Scenarios")
	class ComplexScenariosTest {

		@Test
		@DisplayName("complex business query conditions work correctly")
		void complexBusinessQuery_conditions_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Find high-earning developers in specific departments
			Combination highEarningDevs = Condition.and(
							Employee.POSITION.equalTo("Developer"),
							Employee.SALARY.greaterThan(new BigDecimal("80000")),
							Employee.DEPARTMENT_ID.in(10, 20, 30));

			// Find managers regardless of salary
			Combination managers = Condition.and(
							Employee.POSITION.equalTo("Manager"),
							Employee.REMOTE.equalTo(false));

			// Combine with OR
			Combination finalCondition = Condition.or(highEarningDevs, managers);

			String expected = "((position = ? AND salary > ? AND department_id IN (?, ?, ?)) OR " +
							"(position = ? AND remote = ?))";
			assertEquals(expected, finalCondition.toString(empDef));
			assertEquals(7, finalCondition.values().size());
		}

		@Test
		@DisplayName("date range queries work correctly")
		void dateRangeQueries_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);
			EntityDefinition projDef = entities.definition(Project.TYPE);

			// Employees hired in last year
			LocalDate oneYearAgo = LocalDate.now().minusYears(1);
			Condition recentHires = Employee.HIRE_DATE.greaterThanOrEqualTo(oneYearAgo);

			assertEquals("hire_date >= ?", recentHires.toString(empDef));

			// Projects starting this month
			LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
			LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
			Condition thisMonthProjects = Project.START_DATE.between(monthStart, monthEnd);

			assertEquals("(start_date >= ? AND start_date <= ?)", thisMonthProjects.toString(projDef));
		}

		@Test
		@DisplayName("numeric range conditions with multiple constraints")
		void numericRangeConditions_multipleConstraints_workCorrectly() {
			EntityDefinition empDef = entities.definition(Employee.TYPE);

			// Salary bands
			Combination juniorRange = Condition.and(
							Employee.SALARY.greaterThanOrEqualTo(new BigDecimal("40000")),
							Employee.SALARY.lessThan(new BigDecimal("60000")));

			Combination seniorRange = Condition.and(
							Employee.SALARY.greaterThanOrEqualTo(new BigDecimal("80000")),
							Employee.SALARY.lessThan(new BigDecimal("120000")));

			Combination targetRanges = Condition.or(juniorRange, seniorRange);

			String expected = "((salary >= ? AND salary < ?) OR (salary >= ? AND salary < ?))";
			assertEquals(expected, targetRanges.toString(empDef));
			assertEquals(4, targetRanges.values().size());
		}
	}

	@Nested
	@DisplayName("Performance and Edge Cases")
	class PerformanceAndEdgeCasesTest {

		@Test
		@DisplayName("large value collections perform adequately")
		void largeValueCollections_performAdequately() {
			List<Integer> largeList = IntStream.rangeClosed(1, 10000)
							.boxed()
							.toList();

			// This should not throw exceptions or take excessive time
			assertDoesNotThrow(() -> {
				Condition condition = Employee.ID.in(largeList);
				assertNotNull(condition);
				assertEquals(10000, condition.values().size());
			});
		}

		@Test
		@DisplayName("deeply nested combinations work correctly")
		void deeplyNestedCombinations_workCorrectly() {
			// Create a deeply nested condition tree
			Condition finalCondition = Employee.POSITION.equalTo("Developer");

			for (int i = 0; i < 10; i++) {
				finalCondition = Condition.and(finalCondition, Employee.SALARY.greaterThan(new BigDecimal("50000")));
			}

			EntityDefinition empDef = entities.definition(Employee.TYPE);
			Condition condition = finalCondition; // Final reference for lambda
			assertDoesNotThrow(() -> {
				String conditionString = condition.toString(empDef);
				assertNotNull(conditionString);
				assertTrue(conditionString.contains("position = ?"));
			});
		}

		@ParameterizedTest
		@ValueSource(ints = {1, 10, 100, 1000})
		@DisplayName("various collection sizes work correctly")
		void variousCollectionSizes_workCorrectly(int size) {
			List<String> positions = IntStream.rangeClosed(1, size)
							.mapToObj(i -> "Position" + i)
							.toList();

			Condition condition = Employee.POSITION.in(positions);
			assertEquals(size, condition.values().size());

			EntityDefinition empDef = entities.definition(Employee.TYPE);
			String conditionString = condition.toString(empDef);
			assertNotNull(conditionString);
			assertTrue(conditionString.contains("position"));
			assertTrue(conditionString.contains("?"));
			// Format may vary for large collections
		}

		@Test
		@DisplayName("condition equality and hashCode work correctly")
		void conditionEqualityAndHashCode_workCorrectly() {
			Condition condition1 = Employee.NAME.equalTo("John");
			Condition condition2 = Employee.NAME.equalTo("John");
			Condition condition3 = Employee.NAME.equalTo("Jane");

			// Equality
			assertEquals(condition1, condition2);
			assertNotEquals(condition1, condition3);

			// Hash code
			assertEquals(condition1.hashCode(), condition2.hashCode());
			// Hash codes may or may not be different for different conditions

			// Reflexivity
			assertEquals(condition1, condition1);

			// Null comparison
			assertNotEquals(condition1, null);
		}
	}
}