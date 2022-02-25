/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.db.TestDomain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ColumnProperty;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static is.codion.framework.db.condition.Conditions.where;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionsTest {

  private static final Entities ENTITIES = new TestDomain().getEntities();

  @Test
  void selectConditionKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(emptyList()));
  }

  @Test
  void selectCondition() {
    SelectCondition condition = where(TestDomain.DEPARTMENT_LOCATION).equalTo("New York")
            .toSelectCondition().orderBy(orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertEquals(-1, condition.getLimit());

    condition = Conditions.condition(TestDomain.T_DEPARTMENT).toSelectCondition().limit(10);
    assertEquals(10, condition.getLimit());
  }

  @Test
  void customConditionTest() {
    SelectCondition condition = Conditions.customCondition(TestDomain.DEPARTMENT_NAME_NOT_NULL_CONDITION_ID)
            .toSelectCondition().orderBy(orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getAttributes().isEmpty());
  }

  @Test
  void selectConditionOrderBySameAttribute() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.T_EMP).toSelectCondition()
            .orderBy(orderBy().ascending(TestDomain.EMP_DEPARTMENT).descending(TestDomain.EMP_DEPARTMENT)));
  }

  @Test
  void updateConditionDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.T_EMP).toUpdateCondition()
            .set(TestDomain.EMP_COMMISSION, 123d)
            .set(TestDomain.EMP_COMMISSION, 123d));
  }

  @Test
  void combinationEmpty() {
    Condition.Combination combination = Conditions.combination(Conjunction.AND);
    assertEquals("", combination.getConditionString(ENTITIES.getDefinition(TestDomain.T_EMP)));

    combination = combination.and(Conditions.condition(TestDomain.T_EMP),
            where(TestDomain.EMP_ID).equalTo(1));
    assertEquals("(empno = ?)", combination.getConditionString(ENTITIES.getDefinition(TestDomain.T_EMP)));
  }

  @Test
  void foreignKeyCondition() {
    Entity master = ENTITIES.builder(TestDomain.T_MASTER)
            .with(TestDomain.MASTER_ID_1, 1)
            .with(TestDomain.MASTER_ID_2, 2)
            .with(TestDomain.MASTER_CODE, 3)
            .build();
    Condition condition = where(TestDomain.DETAIL_MASTER_FK).equalTo(master);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.getConditionString(ENTITIES.getDefinition(TestDomain.T_DETAIL)));
    Condition condition2 = where(TestDomain.DETAIL_MASTER_VIA_CODE_FK).equalTo(master);
    assertEquals("master_code = ?", condition2.getConditionString(ENTITIES.getDefinition(TestDomain.T_DETAIL)));
  }

  @Test
  void combination() {
    Condition.Combination combination1 = where(TestDomain.DETAIL_STRING).equalTo("value")
            .and(where(TestDomain.DETAIL_INT).equalTo(666));
    EntityDefinition detailDefinition = ENTITIES.getDefinition(TestDomain.T_DETAIL);
    assertEquals("(string = ? and int = ?)", combination1.getConditionString(detailDefinition));
    Condition.Combination combination2 = where(TestDomain.DETAIL_DOUBLE).equalTo(666.666)
            .and(where(TestDomain.DETAIL_STRING).equalTo("valu%e2").caseSensitive(false));
    Condition.Combination combination3 = combination1.or(combination2);
    assertEquals("((string = ? and int = ?) or (double = ? and upper(string) like upper(?)))",
            combination3.getConditionString(detailDefinition));
  }

  @Test
  void attributeConditionTest() {
    Condition critOne = where(TestDomain.DEPARTMENT_LOCATION).equalTo("New York");
    assertEquals("loc = ?", critOne.getConditionString(ENTITIES.getDefinition(TestDomain.T_DEPARTMENT)));
    assertNotNull(critOne);
  }

  @Test
  void foreignKeyConditionNull() {
    EntityDefinition definition = ENTITIES.getDefinition(TestDomain.T_EMP);
    Condition condition = where(TestDomain.EMP_DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.getConditionString(definition));

    condition = where(TestDomain.EMP_DEPARTMENT_FK).isNotNull();
    assertEquals("deptno is not null", condition.getConditionString(definition));
  }

  @Test
  void foreignKeyConditionEntity() {
    Entity department = ENTITIES.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 10)
            .build();
    EntityDefinition empDefinition = ENTITIES.getDefinition(TestDomain.T_EMP);
    Condition condition = where(TestDomain.EMP_DEPARTMENT_FK).equalTo(department);
    assertEquals("deptno = ?", condition.getConditionString(empDefinition));

    Entity department2 = ENTITIES.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 11)
            .build();
    condition = where(TestDomain.EMP_DEPARTMENT_FK).equalTo(asList(department, department2));
    assertEquals("deptno in (?, ?)", condition.getConditionString(empDefinition));

    condition = where(TestDomain.EMP_DEPARTMENT_FK).notEqualTo(asList(department, department2));
    assertEquals("deptno not in (?, ?)", condition.getConditionString(empDefinition));
  }

  @Test
  void foreignKeyConditionEntityKey() {
    Entity department = ENTITIES.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 10)
            .build();
    EntityDefinition empDefinition = ENTITIES.getDefinition(TestDomain.T_EMP);
    Condition condition = where(TestDomain.EMP_DEPARTMENT_FK).equalTo(department);
    assertEquals("deptno = ?", condition.getConditionString(empDefinition));
  }

  @Test
  void compositeForeignKey() {
    Entity master1 = ENTITIES.builder(TestDomain.T_MASTER)
            .with(TestDomain.MASTER_ID_1, 1)
            .with(TestDomain.MASTER_ID_2, 2)
            .build();

    Entity master2 = ENTITIES.builder(TestDomain.T_MASTER)
            .with(TestDomain.MASTER_ID_1, 3)
            .with(TestDomain.MASTER_ID_2, 4)
            .build();

    EntityDefinition detailDefinition = ENTITIES.getDefinition(TestDomain.T_DETAIL);
    Condition condition = where(TestDomain.DETAIL_MASTER_FK).equalTo(master1);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.getConditionString(detailDefinition));

    condition = where(TestDomain.DETAIL_MASTER_FK).notEqualTo(master1);
    assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.getConditionString(detailDefinition));

    condition = where(TestDomain.DETAIL_MASTER_FK).equalTo(asList(master1, master2));
    assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", condition.getConditionString(detailDefinition));

    condition = where(TestDomain.DETAIL_MASTER_FK).notEqualTo(asList(master1, master2));
    assertEquals("((master_id <> ? and master_id_2 <> ?) or (master_id <> ? and master_id_2 <> ?))", condition.getConditionString(detailDefinition));
  }

  @Test
  void selectConditionCompositeKey() {
    Entity master1 = ENTITIES.builder(TestDomain.T_MASTER)
            .with(TestDomain.MASTER_ID_1, 1)
            .with(TestDomain.MASTER_ID_2, 2)
            .build();

    Entity master2 = ENTITIES.builder(TestDomain.T_MASTER)
            .with(TestDomain.MASTER_ID_1, 3)
            .with(TestDomain.MASTER_ID_2, 4)
            .build();

    EntityDefinition masterDefinition = ENTITIES.getDefinition(TestDomain.T_MASTER);
    Condition condition = Conditions.condition(master1.getPrimaryKey());
    assertEquals("(id = ? and id2 = ?)", condition.getConditionString(masterDefinition));

    condition = Conditions.condition(asList(master1.getPrimaryKey(), master2.getPrimaryKey()));
    assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", condition.getConditionString(masterDefinition));
  }

  @Test
  void keyNullCondition() {
    assertThrows(NullPointerException.class, () ->
            where(TestDomain.EMP_DEPARTMENT_FK).equalTo((Entity[]) null));
    assertThrows(NullPointerException.class, () ->
            where(TestDomain.EMP_DEPARTMENT_FK).equalTo((Collection<Entity>) null));
    assertThrows(NullPointerException.class, () ->
            where(TestDomain.EMP_DEPARTMENT_FK).notEqualTo((Entity[]) null));
    assertThrows(NullPointerException.class, () ->
            where(TestDomain.EMP_DEPARTMENT_FK).notEqualTo((Collection<Entity>) null));

    EntityDefinition empDefinition = ENTITIES.getDefinition(TestDomain.T_EMP);
    Condition condition = where(TestDomain.EMP_DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.getConditionString(empDefinition));

    condition = where(TestDomain.EMP_DEPARTMENT_FK).equalTo((Entity) null);
    assertEquals("deptno is null", condition.getConditionString(empDefinition));

    condition = where(TestDomain.EMP_DEPARTMENT_FK).equalTo(emptyList());
    assertEquals("deptno is null", condition.getConditionString(empDefinition));

    condition = where(TestDomain.EMP_DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.getConditionString(empDefinition));

    condition = where(TestDomain.EMP_DEPARTMENT_FK).isNotNull();
    assertEquals("deptno is not null", condition.getConditionString(empDefinition));

    condition = where(TestDomain.EMP_DEPARTMENT_FK).notEqualTo((Entity) null);
    assertEquals("deptno is not null", condition.getConditionString(empDefinition));

    condition = where(TestDomain.EMP_DEPARTMENT_FK).notEqualTo(emptyList());
    assertEquals("deptno is not null", condition.getConditionString(empDefinition));

    Entity master1 = ENTITIES.builder(TestDomain.T_MASTER)
            .with(TestDomain.MASTER_ID_1, null)
            .with(TestDomain.MASTER_ID_2, null)
            .build();

    EntityDefinition detailDefinition = ENTITIES.getDefinition(TestDomain.T_DETAIL);
    condition = where(TestDomain.DETAIL_MASTER_FK).equalTo(master1);
    assertEquals("(master_id is null and master_id_2 is null)", condition.getConditionString(detailDefinition));

    master1.put(TestDomain.MASTER_ID_2, 1);
    condition = where(TestDomain.DETAIL_MASTER_FK).equalTo(master1);
    assertEquals("(master_id is null and master_id_2 = ?)", condition.getConditionString(detailDefinition));

    Entity dept = ENTITIES.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 42)
            .build();

    condition = where(TestDomain.EMP_DEPARTMENT_FK).equalTo(dept);
    assertEquals("deptno = ?", condition.getConditionString(empDefinition));
  }

  @Test
  void conditionTest() {
    Entity entity = ENTITIES.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 10)
            .build();

    EntityDefinition deptDefinition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);

    Condition condition = Conditions.condition(entity.getPrimaryKey());
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = Conditions.condition(entity.getPrimaryKey());
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = where(TestDomain.DEPARTMENT_NAME).notEqualTo("DEPT");
    assertDepartmentCondition(condition, deptDefinition);
  }

  @Test
  void selectConditionTest() {
    Entity entity = ENTITIES.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 10)
            .build();

    EntityDefinition deptDefinition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);

    Condition condition = Conditions.condition(entity.getPrimaryKey());
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = Conditions.condition(singletonList(entity.getPrimaryKey()));
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = where(TestDomain.DEPARTMENT_NAME).notEqualTo("DEPT");
    assertDepartmentCondition(condition, deptDefinition);
  }

  @Test
  void selectAllCondition() {
    Condition selectCondition = Conditions.condition(TestDomain.T_DEPARTMENT);
    assertTrue(selectCondition.getValues().isEmpty());
    assertTrue(selectCondition.getAttributes().isEmpty());

    Condition condition = Conditions.condition(TestDomain.T_DEPARTMENT);
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getAttributes().isEmpty());
  }

  @Test
  void selectConditionOrderByDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.T_EMP).toSelectCondition()
            .orderBy(orderBy().ascending(TestDomain.EMP_NAME).descending(TestDomain.EMP_NAME)));
  }

  @Test
  void attributeConditionWithNonColumnProperty() {
    EntityDefinition definition = ENTITIES.getDefinition(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () ->
            where(TestDomain.EMP_DEPARTMENT_LOCATION).isNull().getConditionString(definition));
  }

  @Test
  void conditionNullOrEmptyValues() {
    assertThrows(NullPointerException.class, () -> where(TestDomain.DEPARTMENT_NAME).equalTo((String[]) null));
    assertThrows(NullPointerException.class, () -> where(TestDomain.DEPARTMENT_NAME).equalTo((Collection<String>) null));

    assertThrows(NullPointerException.class, () -> where(TestDomain.DEPARTMENT_NAME).notEqualTo((String[]) null));
    assertThrows(NullPointerException.class, () -> where(TestDomain.DEPARTMENT_NAME).notEqualTo((Collection<String>) null));
  }

  @Test
  void whereClause() throws Exception {
    EntityDefinition departmentDefinition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);
    ColumnProperty<?> property = (ColumnProperty<?>) departmentDefinition.getProperty(TestDomain.DEPARTMENT_NAME);
    Condition condition = where(TestDomain.DEPARTMENT_NAME).equalTo("upper%");
    assertEquals(property.getColumnExpression() + " like ?", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).equalTo("upper");
    assertEquals(property.getColumnExpression() + " = ?", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).isNull();
    assertEquals(property.getColumnExpression() + " is null", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).equalTo((String) null);
    assertEquals(property.getColumnExpression() + " is null", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).equalTo(emptyList());
    assertEquals(property.getColumnExpression() + " is null", condition.getConditionString(departmentDefinition));

    condition = where(TestDomain.DEPARTMENT_NAME).notEqualTo("upper%");
    assertEquals(property.getColumnExpression() + " not like ?", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).notEqualTo("upper");
    assertEquals(property.getColumnExpression() + " <> ?", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).isNotNull();
    assertEquals(property.getColumnExpression() + " is not null", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).notEqualTo((String) null);
    assertEquals(property.getColumnExpression() + " is not null", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).notEqualTo(emptyList());
    assertEquals(property.getColumnExpression() + " is not null", condition.getConditionString(departmentDefinition));

    condition = where(TestDomain.DEPARTMENT_NAME).greaterThan("upper");
    assertEquals(property.getColumnExpression() + " > ?", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).greaterThanOrEqualTo("upper");
    assertEquals(property.getColumnExpression() + " >= ?", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).lessThan("upper");
    assertEquals(property.getColumnExpression() + " < ?", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).lessThanOrEqualTo("upper");
    assertEquals(property.getColumnExpression() + " <= ?", condition.getConditionString(departmentDefinition));

    condition = where(TestDomain.DEPARTMENT_NAME).betweenExclusive("upper", "lower");
    assertEquals("(" + property.getColumnExpression() + " > ? and " + property.getColumnExpression() + " < ?)", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).between("upper", "lower");
    assertEquals("(" + property.getColumnExpression() + " >= ? and " + property.getColumnExpression() + " <= ?)", condition.getConditionString(departmentDefinition));

    condition = where(TestDomain.DEPARTMENT_NAME).notBetweenExclusive("upper", "lower");
    assertEquals("(" + property.getColumnExpression() + " < ? or " + property.getColumnExpression() + " > ?)", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).notBetween("upper", "lower");
    assertEquals("(" + property.getColumnExpression() + " <= ? or " + property.getColumnExpression() + " >= ?)", condition.getConditionString(departmentDefinition));

    condition = where(TestDomain.DEPARTMENT_NAME).equalTo("%upper%");
    assertEquals(property.getColumnExpression() + " like ?", condition.getConditionString(departmentDefinition));
    condition = where(TestDomain.DEPARTMENT_NAME).notEqualTo("%upper%");
    assertEquals(property.getColumnExpression() + " not like ?", condition.getConditionString(departmentDefinition));
  }

  private static void assertDepartmentKeyCondition(Condition condition, EntityDefinition departmentDefinition) {
    assertEquals("deptno = ?", condition.getConditionString(departmentDefinition));
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getAttributes().size());
    assertEquals(10, condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_ID, condition.getAttributes().get(0));
  }

  private static void assertDepartmentCondition(Condition condition, EntityDefinition departmentDefinition) {
    assertEquals("dname <> ?", condition.getConditionString(departmentDefinition));
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getAttributes().size());
    assertEquals("DEPT", condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_NAME, condition.getAttributes().get(0));
  }
}
