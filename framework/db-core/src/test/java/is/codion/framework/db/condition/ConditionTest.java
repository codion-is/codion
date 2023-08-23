/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.db.TestDomain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static is.codion.framework.db.TestDomain.*;
import static is.codion.framework.db.condition.Condition.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void customConditionTest() {
    Condition condition = customCondition(Department.NAME_NOT_NULL_CONDITION);
    assertTrue(condition.values().isEmpty());
    assertTrue(condition.columns().isEmpty());
  }

  @Test
  void keyConditionKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> keys(emptyList()));
  }

  @Test
  void combinationEmpty() {
    assertThrows(IllegalArgumentException.class, () -> Condition.combination(Conjunction.AND));
  }

  @Test
  void combinationEntityTypeMismatch() {
    assertThrows(IllegalArgumentException.class, () -> and(
            column(Employee.ID).equalTo(8),
            column(Department.NAME).equalTo("name")));
  }

  @Test
  void foreignKeyCondition() {
    Entity master = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, 2)
            .with(Master.CODE, 3)
            .build();
    Condition condition = foreignKey(Detail.MASTER_FK).equalTo(master);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.toString(ENTITIES.definition(Detail.TYPE)));
    Condition condition2 = foreignKey(Detail.MASTER_VIA_CODE_FK).equalTo(master);
    assertEquals("master_code = ?", condition2.toString(ENTITIES.definition(Detail.TYPE)));
  }

  @Test
  void combination() {
    Combination combination1 = and(
            column(Detail.STRING).equalTo("value"),
            column(Detail.INT).equalTo(666));
    EntityDefinition detailDefinition = ENTITIES.definition(Detail.TYPE);
    assertEquals("(string = ? and int = ?)", combination1.toString(detailDefinition));
    Combination combination2 = and(
            column(Detail.DOUBLE).equalTo(666.666),
            column(Detail.STRING).likeIgnoreCase("valu%e2"));
    Combination combination3 = or(combination1, combination2);
    assertEquals("((string = ? and int = ?) or (double = ? and upper(string) like upper(?)))",
            combination3.toString(detailDefinition));
  }

  @Test
  void columnConditionTest() {
    Condition critOne = column(Department.LOCATION).equalTo("New York");
    assertEquals("loc = ?", critOne.toString(ENTITIES.definition(Department.TYPE)));
    assertNotNull(critOne);
  }

  @Test
  void foreignKeyConditionNull() {
    EntityDefinition definition = ENTITIES.definition(Employee.TYPE);
    Condition condition = foreignKey(Employee.DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.toString(definition));

    condition = foreignKey(Employee.DEPARTMENT_FK).isNotNull();
    assertEquals("deptno is not null", condition.toString(definition));
  }

  @Test
  void foreignKeyConditionEntity() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();
    EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
    Condition condition = foreignKey(Employee.DEPARTMENT_FK).equalTo(department);
    assertEquals("deptno = ?", condition.toString(empDefinition));

    Entity department2 = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 11)
            .build();
    condition = foreignKey(Employee.DEPARTMENT_FK).in(asList(department, department2));
    assertEquals("deptno in (?, ?)", condition.toString(empDefinition));

    condition = foreignKey(Employee.DEPARTMENT_FK).notIn(asList(department, department2));
    assertEquals("deptno not in (?, ?)", condition.toString(empDefinition));
  }

  @Test
  void foreignKeyConditionEntityKey() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();
    EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
    Condition condition = foreignKey(Employee.DEPARTMENT_FK).equalTo(department);
    assertEquals("deptno = ?", condition.toString(empDefinition));
  }

  @Test
  void compositeForeignKey() {
    Entity master1 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, 2)
            .build();

    Entity master2 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 3)
            .with(Master.ID_2, 4)
            .build();

    EntityDefinition detailDefinition = ENTITIES.definition(Detail.TYPE);
    Condition condition = foreignKey(Detail.MASTER_FK).equalTo(master1);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.toString(detailDefinition));

    condition = foreignKey(Detail.MASTER_FK).notEqualTo(master1);
    assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.toString(detailDefinition));

    condition = foreignKey(Detail.MASTER_FK).in(singletonList(master1));
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.toString(detailDefinition));

    condition = foreignKey(Detail.MASTER_FK).in(asList(master1, master2));
    assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", condition.toString(detailDefinition));

    condition = foreignKey(Detail.MASTER_FK).notIn(asList(master1, master2));
    assertEquals("((master_id <> ? and master_id_2 <> ?) or (master_id <> ? and master_id_2 <> ?))", condition.toString(detailDefinition));

    condition = foreignKey(Detail.MASTER_FK).notIn(singletonList(master1));
    assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.toString(detailDefinition));
  }

  @Test
  void compositePrimaryKeyConditionWithNullValues() {
    Entity.Key masterKey = ENTITIES.keyBuilder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, null)
            .with(Master.CODE, 3)
            .build();
    key(masterKey);

    Entity.Key masterKey2 = ENTITIES.keyBuilder(Master.TYPE)
            .with(Master.ID_1, null)
            .with(Master.ID_2, null)
            .with(Master.CODE, 42)
            .build();

    keys(Arrays.asList(masterKey, masterKey2));
  }

  @Test
  void keyConditionCompositeKey() {
    Entity master1 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, 2)
            .build();

    Entity master2 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 3)
            .with(Master.ID_2, 4)
            .build();

    EntityDefinition masterDefinition = ENTITIES.definition(Master.TYPE);
    Condition condition = key(master1.primaryKey());
    assertEquals("(id = ? and id2 = ?)", condition.toString(masterDefinition));

    condition = keys(asList(master1.primaryKey(), master2.primaryKey()));
    assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", condition.toString(masterDefinition));
  }

  @Test
  void keyNullCondition() {
    assertThrows(NullPointerException.class, () ->
            foreignKey(Employee.DEPARTMENT_FK).in((Entity[]) null));
    assertThrows(NullPointerException.class, () ->
            foreignKey(Employee.DEPARTMENT_FK).in((Collection<Entity>) null));
    assertThrows(NullPointerException.class, () ->
            foreignKey(Employee.DEPARTMENT_FK).notIn((Entity[]) null));
    assertThrows(NullPointerException.class, () ->
            foreignKey(Employee.DEPARTMENT_FK).notIn((Collection<Entity>) null));

    EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
    Condition condition = foreignKey(Employee.DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.toString(empDefinition));

    condition = foreignKey(Employee.DEPARTMENT_FK).equalTo(null);
    assertEquals("deptno is null", condition.toString(empDefinition));

    condition = foreignKey(Employee.DEPARTMENT_FK).in(emptyList());
    assertEquals("deptno in ()", condition.toString(empDefinition));

    condition = foreignKey(Employee.DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.toString(empDefinition));

    condition = foreignKey(Employee.DEPARTMENT_FK).isNotNull();
    assertEquals("deptno is not null", condition.toString(empDefinition));

    condition = foreignKey(Employee.DEPARTMENT_FK).notEqualTo(null);
    assertEquals("deptno is not null", condition.toString(empDefinition));

    condition = foreignKey(Employee.DEPARTMENT_FK).notIn(emptyList());
    assertEquals("deptno not in ()", condition.toString(empDefinition));

    Entity master1 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, null)
            .with(Master.ID_2, null)
            .build();

    EntityDefinition detailDefinition = ENTITIES.definition(Detail.TYPE);
    condition = foreignKey(Detail.MASTER_FK).equalTo(master1);
    assertEquals("(master_id is null and master_id_2 is null)", condition.toString(detailDefinition));

    master1.put(Master.ID_2, 1);
    condition = foreignKey(Detail.MASTER_FK).equalTo(master1);
    assertEquals("(master_id is null and master_id_2 = ?)", condition.toString(detailDefinition));

    Entity dept = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 42)
            .build();

    condition = foreignKey(Employee.DEPARTMENT_FK).equalTo(dept);
    assertEquals("deptno = ?", condition.toString(empDefinition));
  }

  @Test
  void keyMismatch() {
    Entity master1 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, 2)
            .build();
    Entity detail = ENTITIES.builder(Detail.TYPE)
            .with(Detail.ID, 3L)
            .build();

    assertThrows(IllegalArgumentException.class, () -> keys(Arrays.asList(master1.primaryKey(), detail.primaryKey())));
  }

  @Test
  void conditionTest() {
    Entity entity = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();

    EntityDefinition deptDefinition = ENTITIES.definition(Department.TYPE);

    Condition condition = key(entity.primaryKey());
    assertDepartmentKeyCondition(condition, deptDefinition, "deptno = ?");

    condition = column(Department.NAME).notEqualTo("DEPT");
    assertDepartmentCondition(condition, deptDefinition, "dname <> ?", 1);

    condition = column(Department.NAME).notIn("DEPT", "DEPT2");
    assertDepartmentCondition(condition, deptDefinition, "dname not in (?, ?)", 2);

    condition = keys(singletonList(entity.primaryKey()));
    assertDepartmentKeyCondition(condition, deptDefinition, "deptno in (?)");

    condition = column(Department.NAME).notEqualTo("DEPT");
    assertDepartmentCondition(condition, deptDefinition, "dname <> ?", 1);
  }

  @Test
  void allCondition() {
    Condition condition = all(Department.TYPE);
    assertTrue(condition.values().isEmpty());
    assertTrue(condition.columns().isEmpty());
  }

  @Test
  void attributeConditionWithNonColumn() {
    EntityDefinition definition = ENTITIES.definition(Employee.TYPE);
    assertThrows(IllegalArgumentException.class, () ->
            column(Employee.DEPARTMENT_LOCATION).isNull().toString(definition));
  }

  @Test
  void conditionNullOrEmptyValues() {
    assertThrows(NullPointerException.class, () -> column(Department.NAME).in((String[]) null));
    assertThrows(NullPointerException.class, () -> column(Department.NAME).in((Collection<String>) null));

    assertThrows(NullPointerException.class, () -> column(Department.NAME).notIn((String[]) null));
    assertThrows(NullPointerException.class, () -> column(Department.NAME).notIn((Collection<String>) null));
  }

  @Test
  void whereClause() {
    EntityDefinition departmentDefinition = ENTITIES.definition(Department.TYPE);
    ColumnDefinition<?> columnDefinition = departmentDefinition.columnDefinition(Department.NAME);
    Condition condition = column(Department.NAME).equalTo("upper");
    assertEquals(columnDefinition.columnExpression() + " = ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).like("upper%");
    assertEquals(columnDefinition.columnExpression() + " like ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).equalTo("upper");
    assertEquals(columnDefinition.columnExpression() + " = ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).isNull();
    assertEquals(columnDefinition.columnExpression() + " is null", condition.toString(departmentDefinition));
    condition = column(Department.NAME).equalTo((String) null);
    assertEquals(columnDefinition.columnExpression() + " is null", condition.toString(departmentDefinition));
    condition = column(Department.NAME).in(emptyList());
    assertEquals(columnDefinition.columnExpression() + " in ()", condition.toString(departmentDefinition));

    condition = column(Department.NAME).notEqualTo("upper");
    assertEquals(columnDefinition.columnExpression() + " <> ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).notLike("upper%");
    assertEquals(columnDefinition.columnExpression() + " not like ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).notEqualTo("upper");
    assertEquals(columnDefinition.columnExpression() + " <> ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).isNotNull();
    assertEquals(columnDefinition.columnExpression() + " is not null", condition.toString(departmentDefinition));
    condition = column(Department.NAME).notEqualTo(null);
    assertEquals(columnDefinition.columnExpression() + " is not null", condition.toString(departmentDefinition));
    condition = column(Department.NAME).notIn(emptyList());
    assertEquals(columnDefinition.columnExpression() + " not in ()", condition.toString(departmentDefinition));

    condition = column(Department.NAME).greaterThan("upper");
    assertEquals(columnDefinition.columnExpression() + " > ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).greaterThanOrEqualTo("upper");
    assertEquals(columnDefinition.columnExpression() + " >= ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).lessThan("upper");
    assertEquals(columnDefinition.columnExpression() + " < ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).lessThanOrEqualTo("upper");
    assertEquals(columnDefinition.columnExpression() + " <= ?", condition.toString(departmentDefinition));

    condition = column(Department.NAME).betweenExclusive("upper", "lower");
    assertEquals("(" + columnDefinition.columnExpression() + " > ? and " + columnDefinition.columnExpression() + " < ?)", condition.toString(departmentDefinition));
    condition = column(Department.NAME).between("upper", "lower");
    assertEquals("(" + columnDefinition.columnExpression() + " >= ? and " + columnDefinition.columnExpression() + " <= ?)", condition.toString(departmentDefinition));

    condition = column(Department.NAME).notBetweenExclusive("upper", "lower");
    assertEquals("(" + columnDefinition.columnExpression() + " <= ? or " + columnDefinition.columnExpression() + " >= ?)", condition.toString(departmentDefinition));
    condition = column(Department.NAME).notBetween("upper", "lower");
    assertEquals("(" + columnDefinition.columnExpression() + " < ? or " + columnDefinition.columnExpression() + " > ?)", condition.toString(departmentDefinition));

    condition = column(Department.NAME).equalTo("upper");
    assertEquals(columnDefinition.columnExpression() + " = ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).like("%upper%");
    assertEquals(columnDefinition.columnExpression() + " like ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).notEqualTo("upper");
    assertEquals(columnDefinition.columnExpression() + " <> ?", condition.toString(departmentDefinition));
    condition = column(Department.NAME).notLike("%upper%");
    assertEquals(columnDefinition.columnExpression() + " not like ?", condition.toString(departmentDefinition));
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
    Condition condition1 = all(Department.TYPE);
    Condition condition2 = all(Department.TYPE);
    assertEquals(condition1, condition2);
    condition2 = all(Employee.TYPE);
    assertNotEquals(condition1, condition2);

    Entity.Key key1 = ENTITIES.primaryKey(Employee.TYPE, 1);
    Entity.Key key2 = ENTITIES.primaryKey(Employee.TYPE, 2);
    condition1 = key(key1);
    condition2 = key(key1);
    assertEquals(condition1, condition2);
    condition2 = key(key2);
    assertNotEquals(condition1, condition2);

    condition1 = foreignKey(Employee.DEPARTMENT_FK).isNull();
    condition2 = foreignKey(Employee.DEPARTMENT_FK).isNull();
    assertEquals(condition1, condition2);
    condition2 = foreignKey(Employee.DEPARTMENT_FK).isNotNull();
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.ID).equalTo(0);
    condition2 = column(Employee.ID).equalTo(0);
    assertEquals(condition1, condition2);
    condition2 = column(Employee.ID).equalTo(1);
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.NAME).equalTo("Luke");
    condition2 = column(Employee.NAME).equalTo("Luke");
    assertEquals(condition1, condition2);
    condition2 = column(Employee.NAME).equalToIgnoreCase("Luke");
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.ID).notEqualTo(0);
    condition2 = column(Employee.ID).notEqualTo(0);
    assertEquals(condition1, condition2);
    condition2 = column(Employee.ID).equalTo(0);
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.ID).notEqualTo(0);
    condition2 = column(Employee.ID).notEqualTo(0);
    assertEquals(condition1, condition2);
    condition2 = column(Employee.ID).notEqualTo(1);
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.ID).lessThan(0);
    condition2 = column(Employee.ID).lessThan(0);
    assertEquals(condition1, condition2);
    condition2 = column(Employee.ID).lessThan(1);
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.ID).lessThanOrEqualTo(0);
    condition2 = column(Employee.ID).lessThanOrEqualTo(0);
    assertEquals(condition1, condition2);
    condition2 = column(Employee.ID).lessThanOrEqualTo(1);
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.ID).greaterThan(0);
    condition2 = column(Employee.ID).greaterThan(0);
    assertEquals(condition1, condition2);
    condition2 = column(Employee.ID).greaterThan(1);
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.ID).greaterThanOrEqualTo(0);
    condition2 = column(Employee.ID).greaterThanOrEqualTo(0);
    assertEquals(condition1, condition2);
    condition2 = column(Employee.ID).greaterThanOrEqualTo(1);
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.ID).between(0, 1);
    condition2 = column(Employee.ID).between(0, 1);
    assertEquals(condition1, condition2);
    condition2 = column(Employee.ID).between(1, 0);
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.ID).betweenExclusive(0, 1);
    condition2 = column(Employee.ID).betweenExclusive(0, 1);
    assertEquals(condition1, condition2);
    condition2 = column(Employee.ID).betweenExclusive(1, 0);
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.ID).notBetween(0, 1);
    condition2 = column(Employee.ID).notBetween(0, 1);
    assertEquals(condition1, condition2);
    condition2 = column(Employee.ID).notBetween(1, 0);
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.ID).notBetweenExclusive(0, 1);
    condition2 = column(Employee.ID).notBetweenExclusive(0, 1);
    assertEquals(condition1, condition2);
    condition2 = column(Employee.ID).notBetweenExclusive(1, 0);
    assertNotEquals(condition1, condition2);

    condition1 = customCondition(Department.CONDITION,
            Collections.singletonList(Department.NAME), Collections.singletonList("Test"));
    condition2 = customCondition(Department.CONDITION,
            Collections.singletonList(Department.NAME), Collections.singletonList("Test"));
    assertEquals(condition1, condition2);

    condition1 = or(column(Employee.ID).equalTo(0),
            column(Employee.ID).equalTo(1));
    condition2 = or(column(Employee.ID).equalTo(0),
            column(Employee.ID).equalTo(1));
    assertEquals(condition1, condition2);
    condition2 = or(column(Employee.ID).equalTo(1),
            column(Employee.ID).equalTo(0));
    assertNotEquals(condition1, condition2);

    condition1 = or(column(Employee.ID).equalTo(0),
            column(Employee.NAME).equalTo("Luke"));
    condition2 = or(column(Employee.ID).equalTo(0),
            column(Employee.NAME).equalTo("Luke"));
    assertEquals(condition1, condition2);
    condition2 = or(column(Employee.ID).equalTo(0),
            column(Employee.NAME).equalTo("Lukas"));
    assertNotEquals(condition1, condition2);

    condition1 = column(Employee.NAME).equalTo("Luke");
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = column(Employee.NAME).greaterThanOrEqualTo("Luke");
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);

    condition1 = column(Employee.NAME).lessThanOrEqualTo("Luke");
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = column(Employee.NAME).greaterThanOrEqualTo("Luke");
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);

    condition1 = column(Employee.NAME).betweenExclusive("John", "Luke");
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = column(Employee.NAME).greaterThanOrEqualTo("Luke");
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);

    condition1 = column(Employee.NAME).notBetweenExclusive("John", "Luke");
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = column(Employee.NAME).lessThanOrEqualTo("Luke");
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);
  }
}
