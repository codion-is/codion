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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;

import static is.codion.framework.domain.TestDomain.*;
import static is.codion.framework.domain.entity.condition.Condition.Combination;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionTest {

	private static final Entities ENTITIES = new TestDomain().entities();

	@Test
	void customConditionTest() {
		Condition condition = Department.NAME_NOT_NULL_CONDITION.get();
		assertTrue(condition.values().isEmpty());
		assertTrue(condition.columns().isEmpty());
		// Column and value count mismatch
		assertThrows(IllegalArgumentException.class, () ->
						Department.NAME_NOT_NULL_CONDITION.get(singletonList(Department.NAME), asList("Test", "Test2")));
		Department.NAME_NOT_NULL_CONDITION.get(asList("Test", "Test2"));
	}

	@Test
	void keyConditionKeyNoKeys() {
		assertThrows(IllegalArgumentException.class, () -> Condition.keys(emptyList()));
	}

	@Test
	void combinationEmpty() {
		assertThrows(IllegalArgumentException.class, () -> Condition.combination(Conjunction.AND));
	}

	@Test
	void foreignKeyCondition() {
		Entity master = ENTITIES.builder(Master2.TYPE)
						.with(Master2.ID_1, 1)
						.with(Master2.ID_2, 2)
						.with(Master2.CODE, 3)
						.build();
		Condition condition = Detail2.MASTER_FK.equalTo(master);
		assertEquals("(master_id = ? AND master_id_2 = ?)", condition.toString(ENTITIES.definition(Detail2.TYPE)));
		Condition condition2 = Detail2.MASTER_VIA_CODE_FK.equalTo(master);
		assertEquals("master_code = ?", condition2.toString(ENTITIES.definition(Detail2.TYPE)));
	}

	@Test
	void combination() {
		Combination combination1 = Condition.and(
						Detail.STRING.equalTo("value"),
						Detail.INT.equalTo(666));
		EntityDefinition detailDefinition = ENTITIES.definition(Detail.TYPE);
		assertEquals("(string = ? AND int = ?)", combination1.toString(detailDefinition));
		Combination combination2 = Condition.and(
						Detail.DOUBLE.equalTo(666.666),
						Detail.STRING.likeIgnoreCase("valu%e2"));
		Combination combination3 = Condition.or(combination1, combination2);
		assertEquals("((string = ? AND int = ?) OR (double = ? AND UPPER(string) LIKE UPPER(?)))",
						combination3.toString(detailDefinition));
	}

	@Test
	void columnConditionTest() {
		Condition critOne = Department.LOCATION.equalTo("New York");
		assertEquals("loc = ?", critOne.toString(ENTITIES.definition(Department.TYPE)));
		assertNotNull(critOne);
	}

	@Test
	void foreignKeyConditionNull() {
		EntityDefinition definition = ENTITIES.definition(Employee.TYPE);
		Condition condition = Employee.DEPARTMENT_FK.isNull();
		assertEquals("deptno IS NULL", condition.toString(definition));

		condition = Employee.DEPARTMENT_FK.isNotNull();
		assertEquals("deptno IS NOT NULL", condition.toString(definition));
	}

	@Test
	void foreignKeyConditionEntity() {
		Entity department = ENTITIES.builder(Department.TYPE)
						.with(Department.ID, 10)
						.build();
		EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
		Condition condition = Employee.DEPARTMENT_FK.equalTo(department);
		assertEquals("deptno = ?", condition.toString(empDefinition));

		Entity department2 = ENTITIES.builder(Department.TYPE)
						.with(Department.ID, 11)
						.build();
		condition = Employee.DEPARTMENT_FK.in(asList(department, department2));
		assertEquals("deptno IN (?, ?)", condition.toString(empDefinition));

		condition = Employee.DEPARTMENT_FK.notIn(asList(department, department2));
		assertEquals("deptno NOT IN (?, ?)", condition.toString(empDefinition));
	}

	@Test
	void foreignKeyConditionEntityKey() {
		Entity department = ENTITIES.builder(Department.TYPE)
						.with(Department.ID, 10)
						.build();
		EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
		Condition condition = Employee.DEPARTMENT_FK.equalTo(department);
		assertEquals("deptno = ?", condition.toString(empDefinition));
	}

	@Test
	void compositeForeignKey() {
		Entity master1 = ENTITIES.builder(Master2.TYPE)
						.with(Master2.ID_1, 1)
						.with(Master2.ID_2, 2)
						.build();

		Entity master2 = ENTITIES.builder(Master2.TYPE)
						.with(Master2.ID_1, 3)
						.with(Master2.ID_2, 4)
						.build();

		EntityDefinition detailDefinition = ENTITIES.definition(Detail2.TYPE);
		Condition condition = Detail2.MASTER_FK.equalTo(master1);
		assertEquals("(master_id = ? AND master_id_2 = ?)", condition.toString(detailDefinition));

		condition = Detail2.MASTER_FK.notEqualTo(master1);
		assertEquals("(master_id <> ? AND master_id_2 <> ?)", condition.toString(detailDefinition));

		condition = Detail2.MASTER_FK.in(singletonList(master1));
		assertEquals("(master_id = ? AND master_id_2 = ?)", condition.toString(detailDefinition));

		condition = Detail2.MASTER_FK.in(asList(master1, master2));
		assertEquals("((master_id = ? AND master_id_2 = ?) OR (master_id = ? AND master_id_2 = ?))", condition.toString(detailDefinition));

		condition = Detail2.MASTER_FK.notIn(asList(master1, master2));
		assertEquals("((master_id <> ? AND master_id_2 <> ?) OR (master_id <> ? AND master_id_2 <> ?))", condition.toString(detailDefinition));

		condition = Detail2.MASTER_FK.notIn(singletonList(master1));
		assertEquals("(master_id <> ? AND master_id_2 <> ?)", condition.toString(detailDefinition));
	}

	@Test
	void compositePrimaryKeyConditionWithNullValues() {
		Entity.Key masterKey = ENTITIES.keyBuilder(Master2.TYPE)
						.with(Master2.ID_1, 1)
						.with(Master2.ID_2, null)
						.with(Master2.CODE, 3)
						.build();
		Condition.key(masterKey);

		Entity.Key masterKey2 = ENTITIES.keyBuilder(Master2.TYPE)
						.with(Master2.ID_1, null)
						.with(Master2.ID_2, null)
						.with(Master2.CODE, 42)
						.build();

		Condition.keys(Arrays.asList(masterKey, masterKey2));
	}

	@Test
	void keyConditionCompositeKey() {
		Entity master1 = ENTITIES.builder(Master2.TYPE)
						.with(Master2.ID_1, 1)
						.with(Master2.ID_2, 2)
						.build();

		Entity master2 = ENTITIES.builder(Master2.TYPE)
						.with(Master2.ID_1, 3)
						.with(Master2.ID_2, 4)
						.build();

		EntityDefinition masterDefinition = ENTITIES.definition(Master2.TYPE);
		Condition condition = Condition.key(master1.primaryKey());
		assertEquals("(id = ? AND id2 = ?)", condition.toString(masterDefinition));

		condition = Condition.keys(asList(master1.primaryKey(), master2.primaryKey()));
		assertEquals("((id = ? AND id2 = ?) OR (id = ? AND id2 = ?))", condition.toString(masterDefinition));
	}

	@Test
	void keyNullCondition() {
		assertThrows(NullPointerException.class, () ->
						Employee.DEPARTMENT_FK.in((Entity[]) null));
		assertThrows(NullPointerException.class, () ->
						Employee.DEPARTMENT_FK.in((Collection<Entity>) null));
		assertThrows(NullPointerException.class, () ->
						Employee.DEPARTMENT_FK.notIn((Entity[]) null));
		assertThrows(NullPointerException.class, () ->
						Employee.DEPARTMENT_FK.notIn((Collection<Entity>) null));

		EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
		Condition condition = Employee.DEPARTMENT_FK.isNull();
		assertEquals("deptno IS NULL", condition.toString(empDefinition));

		condition = Employee.DEPARTMENT_FK.equalTo(null);
		assertEquals("deptno IS NULL", condition.toString(empDefinition));

		condition = Employee.DEPARTMENT_FK.in(emptyList());
		assertEquals("deptno IN ()", condition.toString(empDefinition));

		condition = Employee.DEPARTMENT_FK.isNull();
		assertEquals("deptno IS NULL", condition.toString(empDefinition));

		condition = Employee.DEPARTMENT_FK.isNotNull();
		assertEquals("deptno IS NOT NULL", condition.toString(empDefinition));

		condition = Employee.DEPARTMENT_FK.notEqualTo(null);
		assertEquals("deptno IS NOT NULL", condition.toString(empDefinition));

		condition = Employee.DEPARTMENT_FK.notIn(emptyList());
		assertEquals("deptno NOT IN ()", condition.toString(empDefinition));

		Entity master1 = ENTITIES.builder(Master2.TYPE)
						.with(Master2.ID_1, null)
						.with(Master2.ID_2, null)
						.build();

		EntityDefinition detailDefinition = ENTITIES.definition(Detail2.TYPE);
		condition = Detail2.MASTER_FK.equalTo(master1);
		assertEquals("(master_id IS NULL AND master_id_2 IS NULL)", condition.toString(detailDefinition));

		master1.put(Master2.ID_2, 1);
		condition = Detail2.MASTER_FK.equalTo(master1);
		assertEquals("(master_id IS NULL AND master_id_2 = ?)", condition.toString(detailDefinition));

		Entity dept = ENTITIES.builder(Department.TYPE)
						.with(Department.ID, 42)
						.build();

		condition = Employee.DEPARTMENT_FK.equalTo(dept);
		assertEquals("deptno = ?", condition.toString(empDefinition));
	}

	@Test
	void keyMismatch() {
		Entity master1 = ENTITIES.builder(Master2.TYPE)
						.with(Master2.ID_1, 1)
						.with(Master2.ID_2, 2)
						.build();
		Entity detail = ENTITIES.builder(Detail2.TYPE)
						.with(Detail2.ID, 3L)
						.build();

		assertThrows(IllegalArgumentException.class, () -> Condition.keys(Arrays.asList(master1.primaryKey(), detail.primaryKey())));
	}

	@Test
	void conditionTest() {
		Entity entity = ENTITIES.builder(Department.TYPE)
						.with(Department.ID, 10)
						.build();

		EntityDefinition deptDefinition = ENTITIES.definition(Department.TYPE);

		Condition condition = Condition.key(entity.primaryKey());
		assertDepartmentKeyCondition(condition, deptDefinition, "deptno = ?");

		condition = Department.NAME.notEqualTo("DEPT");
		assertDepartmentCondition(condition, deptDefinition, "dname <> ?", 1);

		condition = Department.NAME.notIn("DEPT", "DEPT2");
		assertDepartmentCondition(condition, deptDefinition, "dname NOT IN (?, ?)", 2);

		condition = Condition.keys(singletonList(entity.primaryKey()));
		assertDepartmentKeyCondition(condition, deptDefinition, "deptno IN (?)");

		condition = Department.NAME.notEqualTo("DEPT");
		assertDepartmentCondition(condition, deptDefinition, "dname <> ?", 1);
	}

	@Test
	void allCondition() {
		Condition condition = Condition.all(Department.TYPE);
		assertTrue(condition.values().isEmpty());
		assertTrue(condition.columns().isEmpty());
	}

	@Test
	void attributeConditionWithNonColumn() {
		EntityDefinition definition = ENTITIES.definition(Employee.TYPE);
		assertThrows(IllegalArgumentException.class, () ->
						Employee.DEPARTMENT_LOCATION.isNull().toString(definition));
	}

	@Test
	void conditionNullOrEmptyValues() {
		assertThrows(NullPointerException.class, () -> Department.NAME.in((String[]) null));
		assertThrows(NullPointerException.class, () -> Department.NAME.in((Collection<String>) null));

		assertThrows(NullPointerException.class, () -> Department.NAME.notIn((String[]) null));
		assertThrows(NullPointerException.class, () -> Department.NAME.notIn((Collection<String>) null));
	}

	@Test
	void whereClause() {
		EntityDefinition departmentDefinition = ENTITIES.definition(Department.TYPE);
		ColumnDefinition<?> columnDefinition = departmentDefinition.columns().definition(Department.NAME);
		Condition condition = Department.NAME.equalTo("upper");
		assertEquals(columnDefinition.expression() + " = ?", condition.toString(departmentDefinition));
		condition = Department.NAME.like("upper%");
		assertEquals(columnDefinition.expression() + " LIKE ?", condition.toString(departmentDefinition));
		condition = Department.NAME.equalTo("upper");
		assertEquals(columnDefinition.expression() + " = ?", condition.toString(departmentDefinition));
		condition = Department.NAME.isNull();
		assertEquals(columnDefinition.expression() + " IS NULL", condition.toString(departmentDefinition));
		condition = Department.NAME.equalTo(null);
		assertEquals(columnDefinition.expression() + " IS NULL", condition.toString(departmentDefinition));
		condition = Department.NAME.in(emptyList());
		assertEquals(columnDefinition.expression() + " IN ()", condition.toString(departmentDefinition));

		condition = Department.NAME.notEqualTo("upper");
		assertEquals(columnDefinition.expression() + " <> ?", condition.toString(departmentDefinition));
		condition = Department.NAME.notLike("upper%");
		assertEquals(columnDefinition.expression() + " NOT LIKE ?", condition.toString(departmentDefinition));
		condition = Department.NAME.notEqualTo("upper");
		assertEquals(columnDefinition.expression() + " <> ?", condition.toString(departmentDefinition));
		condition = Department.NAME.isNotNull();
		assertEquals(columnDefinition.expression() + " IS NOT NULL", condition.toString(departmentDefinition));
		condition = Department.NAME.notEqualTo(null);
		assertEquals(columnDefinition.expression() + " IS NOT NULL", condition.toString(departmentDefinition));
		condition = Department.NAME.notIn(emptyList());
		assertEquals(columnDefinition.expression() + " NOT IN ()", condition.toString(departmentDefinition));

		condition = Department.NAME.greaterThan("upper");
		assertEquals(columnDefinition.expression() + " > ?", condition.toString(departmentDefinition));
		condition = Department.NAME.greaterThanOrEqualTo("upper");
		assertEquals(columnDefinition.expression() + " >= ?", condition.toString(departmentDefinition));
		condition = Department.NAME.lessThan("upper");
		assertEquals(columnDefinition.expression() + " < ?", condition.toString(departmentDefinition));
		condition = Department.NAME.lessThanOrEqualTo("upper");
		assertEquals(columnDefinition.expression() + " <= ?", condition.toString(departmentDefinition));

		condition = Department.NAME.betweenExclusive("upper", "lower");
		assertEquals("(" + columnDefinition.expression() + " > ? AND " + columnDefinition.expression() + " < ?)", condition.toString(departmentDefinition));
		condition = Department.NAME.between("upper", "lower");
		assertEquals("(" + columnDefinition.expression() + " >= ? AND " + columnDefinition.expression() + " <= ?)", condition.toString(departmentDefinition));

		condition = Department.NAME.notBetweenExclusive("upper", "lower");
		assertEquals("(" + columnDefinition.expression() + " <= ? OR " + columnDefinition.expression() + " >= ?)", condition.toString(departmentDefinition));
		condition = Department.NAME.notBetween("upper", "lower");
		assertEquals("(" + columnDefinition.expression() + " < ? OR " + columnDefinition.expression() + " > ?)", condition.toString(departmentDefinition));

		condition = Department.NAME.equalTo("upper");
		assertEquals(columnDefinition.expression() + " = ?", condition.toString(departmentDefinition));
		condition = Department.NAME.like("%upper%");
		assertEquals(columnDefinition.expression() + " LIKE ?", condition.toString(departmentDefinition));
		condition = Department.NAME.notEqualTo("upper");
		assertEquals(columnDefinition.expression() + " <> ?", condition.toString(departmentDefinition));
		condition = Department.NAME.notLike("%upper%");
		assertEquals(columnDefinition.expression() + " NOT LIKE ?", condition.toString(departmentDefinition));

		columnDefinition = departmentDefinition.columns().definition(Department.CODE);
		condition = Department.CODE.equalTo('h');
		assertEquals(columnDefinition.expression() + " = ?", condition.toString(departmentDefinition));
		condition = Department.CODE.equalToIgnoreCase('h');
		assertEquals("UPPER(" + columnDefinition.expression() + ") = UPPER(?)", condition.toString(departmentDefinition));
		condition = Department.CODE.notEqualTo('h');
		assertEquals(columnDefinition.expression() + " <> ?", condition.toString(departmentDefinition));
		condition = Department.CODE.notEqualToIgnoreCase('h');
		assertEquals("UPPER(" + columnDefinition.expression() + ") <> UPPER(?)", condition.toString(departmentDefinition));
	}

	private static void assertDepartmentKeyCondition(Condition condition, EntityDefinition departmentDefinition,
																									 String conditionString) {
		assertEquals(conditionString, condition.toString(departmentDefinition));
		assertEquals(1, condition.values().size());
		assertEquals(1, condition.columns().size());
		assertEquals(10, condition.values().get(0));
		assertEquals(Department.ID, condition.columns().get(0));
	}

	private static void assertDepartmentCondition(Condition condition, EntityDefinition departmentDefinition,
																								String conditionString, int valueCount) {
		assertEquals(conditionString, condition.toString(departmentDefinition));
		assertEquals(valueCount, condition.values().size());
		assertEquals(valueCount, condition.columns().size());
		assertEquals("DEPT", condition.values().get(0));
		assertEquals(Department.NAME, condition.columns().get(0));
	}

	@Test
	void equals() {
		Condition condition1 = Condition.all(Department.TYPE);
		Condition condition2 = Condition.all(Department.TYPE);
		assertEquals(condition1, condition2);
		condition2 = Condition.all(Employee.TYPE);
		assertNotEquals(condition1, condition2);

		Entity.Key key1 = ENTITIES.primaryKey(Employee.TYPE, 1);
		Entity.Key key2 = ENTITIES.primaryKey(Employee.TYPE, 2);
		condition1 = Condition.key(key1);
		condition2 = Condition.key(key1);
		assertEquals(condition1, condition2);
		condition2 = Condition.key(key2);
		assertNotEquals(condition1, condition2);

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

		condition1 = Employee.ID.lessThanOrEqualTo(0);
		condition2 = Employee.ID.lessThanOrEqualTo(0);
		assertEquals(condition1, condition2);
		condition2 = Employee.ID.lessThanOrEqualTo(1);
		assertNotEquals(condition1, condition2);

		condition1 = Employee.ID.greaterThan(0);
		condition2 = Employee.ID.greaterThan(0);
		assertEquals(condition1, condition2);
		condition2 = Employee.ID.greaterThan(1);
		assertNotEquals(condition1, condition2);

		condition1 = Employee.ID.greaterThanOrEqualTo(0);
		condition2 = Employee.ID.greaterThanOrEqualTo(0);
		assertEquals(condition1, condition2);
		condition2 = Employee.ID.greaterThanOrEqualTo(1);
		assertNotEquals(condition1, condition2);

		condition1 = Employee.ID.between(0, 1);
		condition2 = Employee.ID.between(0, 1);
		assertEquals(condition1, condition2);
		condition2 = Employee.ID.between(1, 0);
		assertNotEquals(condition1, condition2);

		condition1 = Employee.ID.betweenExclusive(0, 1);
		condition2 = Employee.ID.betweenExclusive(0, 1);
		assertEquals(condition1, condition2);
		condition2 = Employee.ID.betweenExclusive(1, 0);
		assertNotEquals(condition1, condition2);

		condition1 = Employee.ID.notBetween(0, 1);
		condition2 = Employee.ID.notBetween(0, 1);
		assertEquals(condition1, condition2);
		condition2 = Employee.ID.notBetween(1, 0);
		assertNotEquals(condition1, condition2);

		condition1 = Employee.ID.notBetweenExclusive(0, 1);
		condition2 = Employee.ID.notBetweenExclusive(0, 1);
		assertEquals(condition1, condition2);
		condition2 = Employee.ID.notBetweenExclusive(1, 0);
		assertNotEquals(condition1, condition2);

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
