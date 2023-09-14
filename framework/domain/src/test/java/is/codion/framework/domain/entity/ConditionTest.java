/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Conjunction;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.Condition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static is.codion.framework.domain.TestDomain.*;
import static is.codion.framework.domain.entity.attribute.Condition.Combination;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ConditionTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void customConditionTest() {
    Condition condition = Condition.customCondition(Department.NAME_NOT_NULL_CONDITION);
    Assertions.assertTrue(condition.values().isEmpty());
    Assertions.assertTrue(condition.columns().isEmpty());
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
  void combinationEntityTypeMismatch() {
    assertThrows(IllegalArgumentException.class, () -> Condition.and(
            Employee.ID.equalTo(8),
            Department.NAME.equalTo("name")));
  }

  @Test
  void foreignKeyCondition() {
    Entity master = ENTITIES.builder(Master2.TYPE)
            .with(Master2.ID_1, 1)
            .with(Master2.ID_2, 2)
            .with(Master2.CODE, 3)
            .build();
    Condition condition = Detail2.MASTER_FK.equalTo(master);
    Assertions.assertEquals("(master_id = ? and master_id_2 = ?)", condition.toString(ENTITIES.definition(Detail2.TYPE)));
    Condition condition2 = Detail2.MASTER_VIA_CODE_FK.equalTo(master);
    Assertions.assertEquals("master_code = ?", condition2.toString(ENTITIES.definition(Detail2.TYPE)));
  }

  @Test
  void combination() {
    Combination combination1 = Condition.and(
            Detail.STRING.equalTo("value"),
            Detail.INT.equalTo(666));
    EntityDefinition detailDefinition = ENTITIES.definition(Detail.TYPE);
    Assertions.assertEquals("(string = ? and int = ?)", combination1.toString(detailDefinition));
    Combination combination2 = Condition.and(
            Detail.DOUBLE.equalTo(666.666),
            Detail.STRING.likeIgnoreCase("valu%e2"));
    Combination combination3 = Condition.or(combination1, combination2);
    Assertions.assertEquals("((string = ? and int = ?) or (double = ? and upper(string) like upper(?)))",
            combination3.toString(detailDefinition));
  }

  @Test
  void columnConditionTest() {
    Condition critOne = Department.LOCATION.equalTo("New York");
    Assertions.assertEquals("loc = ?", critOne.toString(ENTITIES.definition(Department.TYPE)));
    assertNotNull(critOne);
  }

  @Test
  void foreignKeyConditionNull() {
    EntityDefinition definition = ENTITIES.definition(Employee.TYPE);
    Condition condition = Employee.DEPARTMENT_FK.isNull();
    Assertions.assertEquals("deptno is null", condition.toString(definition));

    condition = Employee.DEPARTMENT_FK.isNotNull();
    Assertions.assertEquals("deptno is not null", condition.toString(definition));
  }

  @Test
  void foreignKeyConditionEntity() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();
    EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
    Condition condition = Employee.DEPARTMENT_FK.equalTo(department);
    Assertions.assertEquals("deptno = ?", condition.toString(empDefinition));

    Entity department2 = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 11)
            .build();
    condition = Employee.DEPARTMENT_FK.in(asList(department, department2));
    Assertions.assertEquals("deptno in (?, ?)", condition.toString(empDefinition));

    condition = Employee.DEPARTMENT_FK.notIn(asList(department, department2));
    Assertions.assertEquals("deptno not in (?, ?)", condition.toString(empDefinition));
  }

  @Test
  void foreignKeyConditionEntityKey() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();
    EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
    Condition condition = Employee.DEPARTMENT_FK.equalTo(department);
    Assertions.assertEquals("deptno = ?", condition.toString(empDefinition));
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
    Assertions.assertEquals("(master_id = ? and master_id_2 = ?)", condition.toString(detailDefinition));

    condition = Detail2.MASTER_FK.notEqualTo(master1);
    Assertions.assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.toString(detailDefinition));

    condition = Detail2.MASTER_FK.in(singletonList(master1));
    Assertions.assertEquals("(master_id = ? and master_id_2 = ?)", condition.toString(detailDefinition));

    condition = Detail2.MASTER_FK.in(asList(master1, master2));
    Assertions.assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", condition.toString(detailDefinition));

    condition = Detail2.MASTER_FK.notIn(asList(master1, master2));
    Assertions.assertEquals("((master_id <> ? and master_id_2 <> ?) or (master_id <> ? and master_id_2 <> ?))", condition.toString(detailDefinition));

    condition = Detail2.MASTER_FK.notIn(singletonList(master1));
    Assertions.assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.toString(detailDefinition));
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
    Assertions.assertEquals("(id = ? and id2 = ?)", condition.toString(masterDefinition));

    condition = Condition.keys(asList(master1.primaryKey(), master2.primaryKey()));
    Assertions.assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", condition.toString(masterDefinition));
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
    Assertions.assertEquals("deptno is null", condition.toString(empDefinition));

    condition = Employee.DEPARTMENT_FK.equalTo(null);
    Assertions.assertEquals("deptno is null", condition.toString(empDefinition));

    condition = Employee.DEPARTMENT_FK.in(emptyList());
    Assertions.assertEquals("deptno in ()", condition.toString(empDefinition));

    condition = Employee.DEPARTMENT_FK.isNull();
    Assertions.assertEquals("deptno is null", condition.toString(empDefinition));

    condition = Employee.DEPARTMENT_FK.isNotNull();
    Assertions.assertEquals("deptno is not null", condition.toString(empDefinition));

    condition = Employee.DEPARTMENT_FK.notEqualTo(null);
    Assertions.assertEquals("deptno is not null", condition.toString(empDefinition));

    condition = Employee.DEPARTMENT_FK.notIn(emptyList());
    Assertions.assertEquals("deptno not in ()", condition.toString(empDefinition));

    Entity master1 = ENTITIES.builder(Master2.TYPE)
            .with(Master2.ID_1, null)
            .with(Master2.ID_2, null)
            .build();

    EntityDefinition detailDefinition = ENTITIES.definition(Detail2.TYPE);
    condition = Detail2.MASTER_FK.equalTo(master1);
    Assertions.assertEquals("(master_id is null and master_id_2 is null)", condition.toString(detailDefinition));

    master1.put(Master2.ID_2, 1);
    condition = Detail2.MASTER_FK.equalTo(master1);
    Assertions.assertEquals("(master_id is null and master_id_2 = ?)", condition.toString(detailDefinition));

    Entity dept = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 42)
            .build();

    condition = Employee.DEPARTMENT_FK.equalTo(dept);
    Assertions.assertEquals("deptno = ?", condition.toString(empDefinition));
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
    assertDepartmentCondition(condition, deptDefinition, "dname not in (?, ?)", 2);

    condition = Condition.keys(singletonList(entity.primaryKey()));
    assertDepartmentKeyCondition(condition, deptDefinition, "deptno in (?)");

    condition = Department.NAME.notEqualTo("DEPT");
    assertDepartmentCondition(condition, deptDefinition, "dname <> ?", 1);
  }

  @Test
  void allCondition() {
    Condition condition = Condition.all(Department.TYPE);
    Assertions.assertTrue(condition.values().isEmpty());
    Assertions.assertTrue(condition.columns().isEmpty());
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
    ColumnDefinition<?> columnDefinition = departmentDefinition.columnDefinition(Department.NAME);
    Condition condition = Department.NAME.equalTo("upper");
    Assertions.assertEquals(columnDefinition.columnExpression() + " = ?", condition.toString(departmentDefinition));
    condition = Department.NAME.like("upper%");
    Assertions.assertEquals(columnDefinition.columnExpression() + " like ?", condition.toString(departmentDefinition));
    condition = Department.NAME.equalTo("upper");
    Assertions.assertEquals(columnDefinition.columnExpression() + " = ?", condition.toString(departmentDefinition));
    condition = Department.NAME.isNull();
    Assertions.assertEquals(columnDefinition.columnExpression() + " is null", condition.toString(departmentDefinition));
    condition = Department.NAME.equalTo((String) null);
    Assertions.assertEquals(columnDefinition.columnExpression() + " is null", condition.toString(departmentDefinition));
    condition = Department.NAME.in(emptyList());
    Assertions.assertEquals(columnDefinition.columnExpression() + " in ()", condition.toString(departmentDefinition));

    condition = Department.NAME.notEqualTo("upper");
    Assertions.assertEquals(columnDefinition.columnExpression() + " <> ?", condition.toString(departmentDefinition));
    condition = Department.NAME.notLike("upper%");
    Assertions.assertEquals(columnDefinition.columnExpression() + " not like ?", condition.toString(departmentDefinition));
    condition = Department.NAME.notEqualTo("upper");
    Assertions.assertEquals(columnDefinition.columnExpression() + " <> ?", condition.toString(departmentDefinition));
    condition = Department.NAME.isNotNull();
    Assertions.assertEquals(columnDefinition.columnExpression() + " is not null", condition.toString(departmentDefinition));
    condition = Department.NAME.notEqualTo(null);
    Assertions.assertEquals(columnDefinition.columnExpression() + " is not null", condition.toString(departmentDefinition));
    condition = Department.NAME.notIn(emptyList());
    Assertions.assertEquals(columnDefinition.columnExpression() + " not in ()", condition.toString(departmentDefinition));

    condition = Department.NAME.greaterThan("upper");
    Assertions.assertEquals(columnDefinition.columnExpression() + " > ?", condition.toString(departmentDefinition));
    condition = Department.NAME.greaterThanOrEqualTo("upper");
    Assertions.assertEquals(columnDefinition.columnExpression() + " >= ?", condition.toString(departmentDefinition));
    condition = Department.NAME.lessThan("upper");
    Assertions.assertEquals(columnDefinition.columnExpression() + " < ?", condition.toString(departmentDefinition));
    condition = Department.NAME.lessThanOrEqualTo("upper");
    Assertions.assertEquals(columnDefinition.columnExpression() + " <= ?", condition.toString(departmentDefinition));

    condition = Department.NAME.betweenExclusive("upper", "lower");
    Assertions.assertEquals("(" + columnDefinition.columnExpression() + " > ? and " + columnDefinition.columnExpression() + " < ?)", condition.toString(departmentDefinition));
    condition = Department.NAME.between("upper", "lower");
    Assertions.assertEquals("(" + columnDefinition.columnExpression() + " >= ? and " + columnDefinition.columnExpression() + " <= ?)", condition.toString(departmentDefinition));

    condition = Department.NAME.notBetweenExclusive("upper", "lower");
    Assertions.assertEquals("(" + columnDefinition.columnExpression() + " <= ? or " + columnDefinition.columnExpression() + " >= ?)", condition.toString(departmentDefinition));
    condition = Department.NAME.notBetween("upper", "lower");
    Assertions.assertEquals("(" + columnDefinition.columnExpression() + " < ? or " + columnDefinition.columnExpression() + " > ?)", condition.toString(departmentDefinition));

    condition = Department.NAME.equalTo("upper");
    Assertions.assertEquals(columnDefinition.columnExpression() + " = ?", condition.toString(departmentDefinition));
    condition = Department.NAME.like("%upper%");
    Assertions.assertEquals(columnDefinition.columnExpression() + " like ?", condition.toString(departmentDefinition));
    condition = Department.NAME.notEqualTo("upper");
    Assertions.assertEquals(columnDefinition.columnExpression() + " <> ?", condition.toString(departmentDefinition));
    condition = Department.NAME.notLike("%upper%");
    Assertions.assertEquals(columnDefinition.columnExpression() + " not like ?", condition.toString(departmentDefinition));
  }

  private static void assertDepartmentKeyCondition(Condition condition, EntityDefinition departmentDefinition,
                                                   String conditionString) {
    Assertions.assertEquals(conditionString, condition.toString(departmentDefinition));
    Assertions.assertEquals(1, condition.values().size());
    Assertions.assertEquals(1, condition.columns().size());
    Assertions.assertEquals(10, condition.values().get(0));
    Assertions.assertEquals(Department.ID, condition.columns().get(0));
  }

  private static void assertDepartmentCondition(Condition condition, EntityDefinition departmentDefinition,
                                                String conditionString, int valueCount) {
    Assertions.assertEquals(conditionString, condition.toString(departmentDefinition));
    Assertions.assertEquals(valueCount, condition.values().size());
    Assertions.assertEquals(valueCount, condition.columns().size());
    Assertions.assertEquals("DEPT", condition.values().get(0));
    Assertions.assertEquals(Department.NAME, condition.columns().get(0));
  }

  @Test
  void equals() {
    Condition condition1 = Condition.all(Department.TYPE);
    Condition condition2 = Condition.all(Department.TYPE);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Condition.all(Employee.TYPE);
    Assertions.assertNotEquals(condition1, condition2);

    Entity.Key key1 = ENTITIES.primaryKey(Employee.TYPE, 1);
    Entity.Key key2 = ENTITIES.primaryKey(Employee.TYPE, 2);
    condition1 = Condition.key(key1);
    condition2 = Condition.key(key1);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Condition.key(key2);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.DEPARTMENT_FK.isNull();
    condition2 = Employee.DEPARTMENT_FK.isNull();
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.DEPARTMENT_FK.isNotNull();
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.ID.equalTo(0);
    condition2 = Employee.ID.equalTo(0);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.ID.equalTo(1);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.NAME.equalTo("Luke");
    condition2 = Employee.NAME.equalTo("Luke");
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.NAME.equalToIgnoreCase("Luke");
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.ID.notEqualTo(0);
    condition2 = Employee.ID.notEqualTo(0);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.ID.equalTo(0);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.ID.notEqualTo(0);
    condition2 = Employee.ID.notEqualTo(0);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.ID.notEqualTo(1);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.ID.lessThan(0);
    condition2 = Employee.ID.lessThan(0);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.ID.lessThan(1);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.ID.lessThanOrEqualTo(0);
    condition2 = Employee.ID.lessThanOrEqualTo(0);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.ID.lessThanOrEqualTo(1);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.ID.greaterThan(0);
    condition2 = Employee.ID.greaterThan(0);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.ID.greaterThan(1);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.ID.greaterThanOrEqualTo(0);
    condition2 = Employee.ID.greaterThanOrEqualTo(0);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.ID.greaterThanOrEqualTo(1);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.ID.between(0, 1);
    condition2 = Employee.ID.between(0, 1);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.ID.between(1, 0);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.ID.betweenExclusive(0, 1);
    condition2 = Employee.ID.betweenExclusive(0, 1);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.ID.betweenExclusive(1, 0);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.ID.notBetween(0, 1);
    condition2 = Employee.ID.notBetween(0, 1);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.ID.notBetween(1, 0);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.ID.notBetweenExclusive(0, 1);
    condition2 = Employee.ID.notBetweenExclusive(0, 1);
    Assertions.assertEquals(condition1, condition2);
    condition2 = Employee.ID.notBetweenExclusive(1, 0);
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Condition.customCondition(Department.CONDITION,
            Collections.singletonList(Department.NAME), Collections.singletonList("Test"));
    condition2 = Condition.customCondition(Department.CONDITION,
            Collections.singletonList(Department.NAME), Collections.singletonList("Test"));
    Assertions.assertEquals(condition1, condition2);

    condition1 = Condition.or(Employee.ID.equalTo(0),
            Employee.ID.equalTo(1));
    condition2 = Condition.or(Employee.ID.equalTo(0),
            Employee.ID.equalTo(1));
    Assertions.assertEquals(condition1, condition2);
    condition2 = Condition.or(Employee.ID.equalTo(1),
            Employee.ID.equalTo(0));
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Condition.or(Employee.ID.equalTo(0),
            Employee.NAME.equalTo("Luke"));
    condition2 = Condition.or(Employee.ID.equalTo(0),
            Employee.NAME.equalTo("Luke"));
    Assertions.assertEquals(condition1, condition2);
    condition2 = Condition.or(Employee.ID.equalTo(0),
            Employee.NAME.equalTo("Lukas"));
    Assertions.assertNotEquals(condition1, condition2);

    condition1 = Employee.NAME.equalTo("Luke");
    condition2 = condition1;
    Assertions.assertEquals(condition1, condition2);

    condition2 = Employee.NAME.greaterThanOrEqualTo("Luke");
    Assertions.assertNotEquals(condition1, condition2);
    Assertions.assertNotEquals(condition2, condition1);

    condition1 = Employee.NAME.lessThanOrEqualTo("Luke");
    condition2 = condition1;
    Assertions.assertEquals(condition1, condition2);

    condition2 = Employee.NAME.greaterThanOrEqualTo("Luke");
    Assertions.assertNotEquals(condition1, condition2);
    Assertions.assertNotEquals(condition2, condition1);

    condition1 = Employee.NAME.betweenExclusive("John", "Luke");
    condition2 = condition1;
    Assertions.assertEquals(condition1, condition2);

    condition2 = Employee.NAME.greaterThanOrEqualTo("Luke");
    Assertions.assertNotEquals(condition1, condition2);
    Assertions.assertNotEquals(condition2, condition1);

    condition1 = Employee.NAME.notBetweenExclusive("John", "Luke");
    condition2 = condition1;
    Assertions.assertEquals(condition1, condition2);

    condition2 = Employee.NAME.lessThanOrEqualTo("Luke");
    Assertions.assertNotEquals(condition1, condition2);
    Assertions.assertNotEquals(condition2, condition1);
  }
}
