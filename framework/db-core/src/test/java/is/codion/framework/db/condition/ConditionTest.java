/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.db.TestDomain;
import is.codion.framework.db.TestDomain.Department;
import is.codion.framework.db.TestDomain.Detail;
import is.codion.framework.db.TestDomain.Employee;
import is.codion.framework.db.TestDomain.Master;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.property.ColumnProperty;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static is.codion.framework.db.condition.Condition.where;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void selectConditionKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> Condition.condition(emptyList()));
  }

  @Test
  void selectCondition() {
    SelectCondition condition = where(Department.LOCATION).equalTo("New York")
            .selectBuilder()
            .orderBy(OrderBy.ascending(Department.NAME))
            .build();
    assertEquals(-1, condition.limit());

    condition = Condition.condition(Department.TYPE).selectBuilder()
            .limit(10)
            .build();
    assertEquals(10, condition.limit());
  }

  @Test
  void customConditionTest() {
    SelectCondition condition = Condition.customCondition(Department.NAME_NOT_NULL_CONDITION_ID)
            .selectBuilder()
            .orderBy(OrderBy.ascending(Department.NAME))
            .build();
    assertTrue(condition.values().isEmpty());
    assertTrue(condition.attributes().isEmpty());
  }

  @Test
  void updateConditionDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Condition.condition(Employee.TYPE).updateBuilder()
            .set(Employee.COMMISSION, 123d)
            .set(Employee.COMMISSION, 123d));
  }

  @Test
  void combinationEmpty() {
    assertThrows(IllegalArgumentException.class, () -> Condition.combination(Conjunction.AND));
  }

  @Test
  void foreignKeyCondition() {
    Entity master = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, 2)
            .with(Master.CODE, 3)
            .build();
    Condition condition = where(Detail.MASTER_FK).equalTo(master);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.toString(ENTITIES.definition(Detail.TYPE)));
    Condition condition2 = where(Detail.MASTER_VIA_CODE_FK).equalTo(master);
    assertEquals("master_code = ?", condition2.toString(ENTITIES.definition(Detail.TYPE)));
  }

  @Test
  void compositePrimaryKeyConditionWithNullValues() {
    Key masterKey = ENTITIES.keyBuilder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, null)
            .with(Master.CODE, 3)
            .build();
    Condition.condition(masterKey);

    Key masterKey2 = ENTITIES.keyBuilder(Master.TYPE)
            .with(Master.ID_1, null)
            .with(Master.ID_2, null)
            .with(Master.CODE, 42)
            .build();

    Condition.condition(Arrays.asList(masterKey, masterKey2));
  }

  @Test
  void combination() {
    Condition.Combination combination1 = where(Detail.STRING).equalTo("value")
            .and(where(Detail.INT).equalTo(666));
    EntityDefinition detailDefinition = ENTITIES.definition(Detail.TYPE);
    assertEquals("(string = ? and int = ?)", combination1.toString(detailDefinition));
    Condition.Combination combination2 = where(Detail.DOUBLE).equalTo(666.666)
            .and(where(Detail.STRING).equalToIgnoreCase("valu%e2"));
    Condition.Combination combination3 = combination1.or(combination2);
    assertEquals("((string = ? and int = ?) or (double = ? and upper(string) like upper(?)))",
            combination3.toString(detailDefinition));
  }

  @Test
  void attributeConditionTest() {
    Condition critOne = where(Department.LOCATION).equalTo("New York");
    assertEquals("loc = ?", critOne.toString(ENTITIES.definition(Department.TYPE)));
    assertNotNull(critOne);
  }

  @Test
  void foreignKeyConditionNull() {
    EntityDefinition definition = ENTITIES.definition(Employee.TYPE);
    Condition condition = where(Employee.DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.toString(definition));

    condition = where(Employee.DEPARTMENT_FK).isNotNull();
    assertEquals("deptno is not null", condition.toString(definition));
  }

  @Test
  void foreignKeyConditionEntity() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();
    EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
    Condition condition = where(Employee.DEPARTMENT_FK).equalTo(department);
    assertEquals("deptno = ?", condition.toString(empDefinition));

    Entity department2 = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 11)
            .build();
    condition = where(Employee.DEPARTMENT_FK).equalTo(asList(department, department2));
    assertEquals("deptno in (?, ?)", condition.toString(empDefinition));

    condition = where(Employee.DEPARTMENT_FK).notEqualTo(asList(department, department2));
    assertEquals("deptno not in (?, ?)", condition.toString(empDefinition));
  }

  @Test
  void foreignKeyConditionEntityKey() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();
    EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
    Condition condition = where(Employee.DEPARTMENT_FK).equalTo(department);
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
    Condition condition = where(Detail.MASTER_FK).equalTo(master1);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.toString(detailDefinition));

    condition = where(Detail.MASTER_FK).notEqualTo(master1);
    assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.toString(detailDefinition));

    condition = where(Detail.MASTER_FK).equalTo(asList(master1, master2));
    assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", condition.toString(detailDefinition));

    condition = where(Detail.MASTER_FK).notEqualTo(asList(master1, master2));
    assertEquals("((master_id <> ? and master_id_2 <> ?) or (master_id <> ? and master_id_2 <> ?))", condition.toString(detailDefinition));
  }

  @Test
  void selectConditionCompositeKey() {
    Entity master1 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, 2)
            .build();

    Entity master2 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 3)
            .with(Master.ID_2, 4)
            .build();

    EntityDefinition masterDefinition = ENTITIES.definition(Master.TYPE);
    Condition condition = Condition.condition(master1.primaryKey());
    assertEquals("(id = ? and id2 = ?)", condition.toString(masterDefinition));

    condition = Condition.condition(asList(master1.primaryKey(), master2.primaryKey()));
    assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", condition.toString(masterDefinition));
  }

  @Test
  void keyNullCondition() {
    assertThrows(NullPointerException.class, () ->
            where(Employee.DEPARTMENT_FK).equalTo((Entity[]) null));
    assertThrows(NullPointerException.class, () ->
            where(Employee.DEPARTMENT_FK).equalTo((Collection<Entity>) null));
    assertThrows(NullPointerException.class, () ->
            where(Employee.DEPARTMENT_FK).notEqualTo((Entity[]) null));
    assertThrows(NullPointerException.class, () ->
            where(Employee.DEPARTMENT_FK).notEqualTo((Collection<Entity>) null));

    EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
    Condition condition = where(Employee.DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.toString(empDefinition));

    condition = where(Employee.DEPARTMENT_FK).equalTo((Entity) null);
    assertEquals("deptno is null", condition.toString(empDefinition));

    condition = where(Employee.DEPARTMENT_FK).equalTo(emptyList());
    assertEquals("deptno is null", condition.toString(empDefinition));

    condition = where(Employee.DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.toString(empDefinition));

    condition = where(Employee.DEPARTMENT_FK).isNotNull();
    assertEquals("deptno is not null", condition.toString(empDefinition));

    condition = where(Employee.DEPARTMENT_FK).notEqualTo((Entity) null);
    assertEquals("deptno is not null", condition.toString(empDefinition));

    condition = where(Employee.DEPARTMENT_FK).notEqualTo(emptyList());
    assertEquals("deptno is not null", condition.toString(empDefinition));

    Entity master1 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, null)
            .with(Master.ID_2, null)
            .build();

    EntityDefinition detailDefinition = ENTITIES.definition(Detail.TYPE);
    condition = where(Detail.MASTER_FK).equalTo(master1);
    assertEquals("(master_id is null and master_id_2 is null)", condition.toString(detailDefinition));

    master1.put(Master.ID_2, 1);
    condition = where(Detail.MASTER_FK).equalTo(master1);
    assertEquals("(master_id is null and master_id_2 = ?)", condition.toString(detailDefinition));

    Entity dept = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 42)
            .build();

    condition = where(Employee.DEPARTMENT_FK).equalTo(dept);
    assertEquals("deptno = ?", condition.toString(empDefinition));
  }

  @Test
  void conditionTest() {
    Entity entity = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();

    EntityDefinition deptDefinition = ENTITIES.definition(Department.TYPE);

    Condition condition = Condition.condition(entity.primaryKey());
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = Condition.condition(entity.primaryKey());
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = where(Department.NAME).notEqualTo("DEPT");
    assertDepartmentCondition(condition, deptDefinition);
  }

  @Test
  void selectConditionTest() {
    Entity entity = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();

    EntityDefinition deptDefinition = ENTITIES.definition(Department.TYPE);

    Condition condition = Condition.condition(entity.primaryKey());
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = Condition.condition(singletonList(entity.primaryKey()));
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = where(Department.NAME).notEqualTo("DEPT");
    assertDepartmentCondition(condition, deptDefinition);
  }

  @Test
  void selectAllCondition() {
    Condition selectCondition = Condition.condition(Department.TYPE);
    assertTrue(selectCondition.values().isEmpty());
    assertTrue(selectCondition.attributes().isEmpty());

    Condition condition = Condition.condition(Department.TYPE);
    assertTrue(condition.values().isEmpty());
    assertTrue(condition.attributes().isEmpty());
  }

  @Test
  void attributeConditionWithNonColumnProperty() {
    EntityDefinition definition = ENTITIES.definition(Employee.TYPE);
    assertThrows(IllegalArgumentException.class, () ->
            where(Employee.DEPARTMENT_LOCATION).isNull().toString(definition));
  }

  @Test
  void conditionNullOrEmptyValues() {
    assertThrows(NullPointerException.class, () -> where(Department.NAME).equalTo((String[]) null));
    assertThrows(NullPointerException.class, () -> where(Department.NAME).equalTo((Collection<String>) null));

    assertThrows(NullPointerException.class, () -> where(Department.NAME).notEqualTo((String[]) null));
    assertThrows(NullPointerException.class, () -> where(Department.NAME).notEqualTo((Collection<String>) null));
  }

  @Test
  void whereClause() {
    EntityDefinition departmentDefinition = ENTITIES.definition(Department.TYPE);
    ColumnProperty<?> property = (ColumnProperty<?>) departmentDefinition.property(Department.NAME);
    Condition condition = where(Department.NAME).equalTo("upper%");
    assertEquals(property.columnExpression() + " like ?", condition.toString(departmentDefinition));
    condition = where(Department.NAME).equalTo("upper");
    assertEquals(property.columnExpression() + " = ?", condition.toString(departmentDefinition));
    condition = where(Department.NAME).isNull();
    assertEquals(property.columnExpression() + " is null", condition.toString(departmentDefinition));
    condition = where(Department.NAME).equalTo((String) null);
    assertEquals(property.columnExpression() + " is null", condition.toString(departmentDefinition));
    condition = where(Department.NAME).equalTo(emptyList());
    assertEquals(property.columnExpression() + " is null", condition.toString(departmentDefinition));

    condition = where(Department.NAME).notEqualTo("upper%");
    assertEquals(property.columnExpression() + " not like ?", condition.toString(departmentDefinition));
    condition = where(Department.NAME).notEqualTo("upper");
    assertEquals(property.columnExpression() + " <> ?", condition.toString(departmentDefinition));
    condition = where(Department.NAME).isNotNull();
    assertEquals(property.columnExpression() + " is not null", condition.toString(departmentDefinition));
    condition = where(Department.NAME).notEqualTo((String) null);
    assertEquals(property.columnExpression() + " is not null", condition.toString(departmentDefinition));
    condition = where(Department.NAME).notEqualTo(emptyList());
    assertEquals(property.columnExpression() + " is not null", condition.toString(departmentDefinition));

    condition = where(Department.NAME).greaterThan("upper");
    assertEquals(property.columnExpression() + " > ?", condition.toString(departmentDefinition));
    condition = where(Department.NAME).greaterThanOrEqualTo("upper");
    assertEquals(property.columnExpression() + " >= ?", condition.toString(departmentDefinition));
    condition = where(Department.NAME).lessThan("upper");
    assertEquals(property.columnExpression() + " < ?", condition.toString(departmentDefinition));
    condition = where(Department.NAME).lessThanOrEqualTo("upper");
    assertEquals(property.columnExpression() + " <= ?", condition.toString(departmentDefinition));

    condition = where(Department.NAME).betweenExclusive("upper", "lower");
    assertEquals("(" + property.columnExpression() + " > ? and " + property.columnExpression() + " < ?)", condition.toString(departmentDefinition));
    condition = where(Department.NAME).between("upper", "lower");
    assertEquals("(" + property.columnExpression() + " >= ? and " + property.columnExpression() + " <= ?)", condition.toString(departmentDefinition));

    condition = where(Department.NAME).notBetweenExclusive("upper", "lower");
    assertEquals("(" + property.columnExpression() + " <= ? or " + property.columnExpression() + " >= ?)", condition.toString(departmentDefinition));
    condition = where(Department.NAME).notBetween("upper", "lower");
    assertEquals("(" + property.columnExpression() + " < ? or " + property.columnExpression() + " > ?)", condition.toString(departmentDefinition));

    condition = where(Department.NAME).equalTo("%upper%");
    assertEquals(property.columnExpression() + " like ?", condition.toString(departmentDefinition));
    condition = where(Department.NAME).notEqualTo("%upper%");
    assertEquals(property.columnExpression() + " not like ?", condition.toString(departmentDefinition));
  }

  @Test
  void equals() {
    Condition condition1 = Condition.condition(Department.TYPE);
    Condition condition2 = Condition.condition(Department.TYPE);
    assertEquals(condition1, condition2);
    condition2 = Condition.condition(Employee.TYPE);
    assertNotEquals(condition1, condition2);

    Key key1 = ENTITIES.primaryKey(Employee.TYPE, 1);
    Key key2 = ENTITIES.primaryKey(Employee.TYPE, 2);
    condition1 = Condition.condition(key1);
    condition2 = Condition.condition(key1);
    assertEquals(condition1, condition2);
    condition2 = Condition.condition(key2);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.DEPARTMENT_FK).isNull();
    condition2 = Condition.where(Employee.DEPARTMENT_FK).isNull();
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.DEPARTMENT_FK).isNotNull();
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).equalTo(0);
    condition2 = Condition.where(Employee.ID).equalTo(0);
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).equalTo(1);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.NAME).equalTo("Luke");
    condition2 = Condition.where(Employee.NAME).equalTo("Luke");
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.NAME).equalToIgnoreCase("Luke");
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).notEqualTo(0);
    condition2 = Condition.where(Employee.ID).notEqualTo(0);
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).equalTo(0);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).notEqualTo(0);
    condition2 = Condition.where(Employee.ID).notEqualTo(0);
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).notEqualTo(1);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).lessThan(0);
    condition2 = Condition.where(Employee.ID).lessThan(0);
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).lessThan(1);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).lessThanOrEqualTo(0);
    condition2 = Condition.where(Employee.ID).lessThanOrEqualTo(0);
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).lessThanOrEqualTo(1);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).greaterThan(0);
    condition2 = Condition.where(Employee.ID).greaterThan(0);
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).greaterThan(1);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).greaterThanOrEqualTo(0);
    condition2 = Condition.where(Employee.ID).greaterThanOrEqualTo(0);
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).greaterThanOrEqualTo(1);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).between(0, 1);
    condition2 = Condition.where(Employee.ID).between(0, 1);
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).between(1, 0);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).betweenExclusive(0, 1);
    condition2 = Condition.where(Employee.ID).betweenExclusive(0, 1);
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).betweenExclusive(1, 0);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).notBetween(0, 1);
    condition2 = Condition.where(Employee.ID).notBetween(0, 1);
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).notBetween(1, 0);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).notBetweenExclusive(0, 1);
    condition2 = Condition.where(Employee.ID).notBetweenExclusive(0, 1);
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).notBetweenExclusive(1, 0);
    assertNotEquals(condition1, condition2);

    condition1 = Condition.customCondition(Department.CONDITION_ID,
            Collections.singletonList(Department.NAME), Collections.singletonList("Test"));
    condition2 = Condition.customCondition(Department.CONDITION_ID,
            Collections.singletonList(Department.NAME), Collections.singletonList("Test"));
    assertEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).equalTo(0)
            .or(Condition.where(Employee.ID).equalTo(1));
    condition2 = Condition.where(Employee.ID).equalTo(0)
            .or(Condition.where(Employee.ID).equalTo(1));
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).equalTo(1)
            .or(Condition.where(Employee.ID).equalTo(0));
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.ID).equalTo(0)
            .or(Condition.where(Employee.NAME).equalTo("Luke"));
    condition2 = Condition.where(Employee.ID).equalTo(0)
            .or(Condition.where(Employee.NAME).equalTo("Luke"));
    assertEquals(condition1, condition2);
    condition2 = Condition.where(Employee.ID).equalTo(0)
            .or(Condition.where(Employee.NAME).equalTo("Lukas"));
    assertNotEquals(condition1, condition2);

    condition1 = Condition.where(Employee.NAME).equalTo("Luke", "John");
    condition2 = Condition.where(Employee.NAME).equalTo("Luke", "John");
    assertEquals(condition1.selectBuilder().build(), condition2.selectBuilder().build());
    assertEquals(condition1.selectBuilder()
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build(),
            condition2.selectBuilder()
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build());
    assertNotEquals(condition1.selectBuilder()
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build(),
            condition2.selectBuilder()
                    .build());

    assertEquals(condition1.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .build(),
            condition2.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .build());

    assertEquals(condition1.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .offset(10)
                    .build(),
            condition2.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .offset(10)
                    .build());

    assertNotEquals(condition1.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .build(),
            condition2.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .offset(10)
                    .build());

    assertNotEquals(condition1.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .build(),
            condition2.selectBuilder()
                    .selectAttributes(Employee.ID)
                    .build());

    condition1 = Condition.where(Employee.NAME).equalTo("Luke");
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = Condition.where(Employee.NAME).greaterThanOrEqualTo("Luke");
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);

    condition1 = Condition.where(Employee.NAME).lessThanOrEqualTo("Luke");
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = Condition.where(Employee.NAME).greaterThanOrEqualTo("Luke");
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);

    condition1 = Condition.where(Employee.NAME).betweenExclusive("John", "Luke");
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = Condition.where(Employee.NAME).greaterThanOrEqualTo("Luke");
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);

    condition1 = Condition.where(Employee.NAME).notBetweenExclusive("John", "Luke");
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = Condition.where(Employee.NAME).lessThanOrEqualTo("Luke");
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);
  }

  private static void assertDepartmentKeyCondition(Condition condition, EntityDefinition departmentDefinition) {
    assertEquals("deptno = ?", condition.toString(departmentDefinition));
    assertEquals(1, condition.values().size());
    assertEquals(1, condition.attributes().size());
    assertEquals(10, condition.values().get(0));
    assertEquals(Department.ID, condition.attributes().get(0));
  }

  private static void assertDepartmentCondition(Condition condition, EntityDefinition departmentDefinition) {
    assertEquals("dname <> ?", condition.toString(departmentDefinition));
    assertEquals(1, condition.values().size());
    assertEquals(1, condition.attributes().size());
    assertEquals("DEPT", condition.values().get(0));
    assertEquals(Department.NAME, condition.attributes().get(0));
  }
}
