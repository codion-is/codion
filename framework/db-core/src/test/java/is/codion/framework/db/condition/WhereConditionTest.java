/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.db.TestDomain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.ColumnProperty;

import org.junit.jupiter.api.Test;

import static is.codion.framework.db.condition.Conditions.*;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class WhereConditionTest {

  private static final Entities ENTITIES = new TestDomain().getEntities();

  @Test
  public void test() {
    final Condition.Combination combination1 = Conditions.condition(TestDomain.DETAIL_STRING, Operator.EQUALS, "value")
            .and(Conditions.condition(TestDomain.DETAIL_INT, Operator.EQUALS, 666));
    final EntityDefinition detailDefinition = ENTITIES.getDefinition(TestDomain.T_DETAIL);
    final WhereCondition condition = whereCondition(combination1, detailDefinition);
    assertEquals("(string = ? and int = ?)", condition.getWhereClause());
    final Condition.Combination combination2 = Conditions.condition(TestDomain.DETAIL_DOUBLE, Operator.EQUALS, 666.666)
            .and(Conditions.condition(TestDomain.DETAIL_STRING, Operator.EQUALS, "valu%e2").setCaseSensitive(false));
    final Condition.Combination combination3 = combination1.or(combination2);
    assertEquals("((string = ? and int = ?) or (double = ? and upper(string) like upper(?)))",
            whereCondition(combination3, detailDefinition).getWhereClause());
  }

  @Test
  public void attributeConditionTest() {
    final WhereCondition critOne = whereCondition(Conditions.condition(TestDomain.DEPARTMENT_LOCATION,
            Operator.EQUALS, "New York"), ENTITIES.getDefinition(TestDomain.T_DEPARTMENT));
    assertEquals("loc = ?", critOne.getWhereClause());
    assertNotNull(critOne);
  }

  @Test
  public void foreignKeyConditionNull() {
    final EntityDefinition definition = ENTITIES.getDefinition(TestDomain.T_EMP);
    WhereCondition condition = whereCondition(condition(TestDomain.EMP_DEPARTMENT_FK, NullCheck.IS_NULL), definition);
    assertEquals("deptno is null", condition.getWhereClause());

    condition = whereCondition(condition(TestDomain.EMP_DEPARTMENT_FK, NullCheck.IS_NOT_NULL), definition);
    assertEquals("deptno is not null", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionEntity() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final EntityDefinition empDefinition = ENTITIES.getDefinition(TestDomain.T_EMP);
    WhereCondition condition = whereCondition(condition(TestDomain.EMP_DEPARTMENT_FK, Operator.EQUALS, department), empDefinition);
    assertEquals("deptno = ?", condition.getWhereClause());

    final Entity department2 = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department2.put(TestDomain.DEPARTMENT_ID, 11);
    condition = whereCondition(condition(TestDomain.EMP_DEPARTMENT_FK, Operator.EQUALS, asList(department, department2)), empDefinition);
    assertEquals("deptno in (?, ?)", condition.getWhereClause());

    condition = whereCondition(condition(TestDomain.EMP_DEPARTMENT_FK, Operator.NOT_EQUALS, asList(department, department2)), empDefinition);
    assertEquals("deptno not in (?, ?)", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionEntityKey() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final EntityDefinition empDefinition = ENTITIES.getDefinition(TestDomain.T_EMP);
    final WhereCondition condition = whereCondition(condition(
            TestDomain.EMP_DEPARTMENT_FK, Operator.EQUALS, department.getKey()), empDefinition);
    assertEquals("deptno = ?", condition.getWhereClause());
  }

  @Test
  public void compositeForeignKey() {
    final Entity master1 = ENTITIES.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, 1);
    master1.put(TestDomain.MASTER_ID_2, 2);

    final Entity master2 = ENTITIES.entity(TestDomain.T_MASTER);
    master2.put(TestDomain.MASTER_ID_1, 3);
    master2.put(TestDomain.MASTER_ID_2, 4);

    final EntityDefinition detailDefinition = ENTITIES.getDefinition(TestDomain.T_DETAIL);
    WhereCondition condition = whereCondition(condition(TestDomain.DETAIL_MASTER_FK, Operator.EQUALS, master1), detailDefinition);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.getWhereClause());

    condition = whereCondition(condition(TestDomain.DETAIL_MASTER_FK, Operator.NOT_EQUALS, master1), detailDefinition);
    assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.getWhereClause());

    condition = whereCondition(condition(TestDomain.DETAIL_MASTER_FK, Operator.EQUALS, asList(master1, master2)), detailDefinition);
    assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", condition.getWhereClause());

    condition = whereCondition(condition(TestDomain.DETAIL_MASTER_FK, Operator.NOT_EQUALS, asList(master1, master2)), detailDefinition);
    assertEquals("((master_id <> ? and master_id_2 <> ?) or (master_id <> ? and master_id_2 <> ?))", condition.getWhereClause());
  }

  @Test
  public void selectConditionCompositeKey() {
    final Entity master1 = ENTITIES.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, 1);
    master1.put(TestDomain.MASTER_ID_2, 2);

    final Entity master2 = ENTITIES.entity(TestDomain.T_MASTER);
    master2.put(TestDomain.MASTER_ID_1, 3);
    master2.put(TestDomain.MASTER_ID_2, 4);

    final EntityDefinition masterDefinition = ENTITIES.getDefinition(TestDomain.T_MASTER);
    WhereCondition condition = whereCondition(selectCondition(master1.getKey()), masterDefinition);
    assertEquals("(id = ? and id2 = ?)", condition.getWhereClause());

    condition = whereCondition(selectCondition(asList(master1.getKey(), master2.getKey())), masterDefinition);
    assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", condition.getWhereClause());
  }

  @Test
  public void keyNullCondition() {
    final EntityDefinition empDefinition = ENTITIES.getDefinition(TestDomain.T_EMP);
    WhereCondition condition = whereCondition(selectCondition(TestDomain.EMP_DEPARTMENT_FK, NullCheck.IS_NULL), empDefinition);
    assertEquals("deptno is null", condition.getWhereClause());

    condition = whereCondition(selectCondition(TestDomain.EMP_DEPARTMENT_FK, NullCheck.IS_NULL), empDefinition);
    assertEquals("deptno is null", condition.getWhereClause());

    condition = whereCondition(selectCondition(TestDomain.EMP_DEPARTMENT_FK, NullCheck.IS_NOT_NULL), empDefinition);
    assertEquals("deptno is not null", condition.getWhereClause());

    final Key master1Key = ENTITIES.key(TestDomain.T_MASTER);
    master1Key.put(TestDomain.MASTER_ID_1, null);
    master1Key.put(TestDomain.MASTER_ID_2, null);

    final EntityDefinition detailDefinition = ENTITIES.getDefinition(TestDomain.T_DETAIL);
    condition = whereCondition(selectCondition(TestDomain.DETAIL_MASTER_FK, Operator.EQUALS, master1Key), detailDefinition);
    assertEquals("(master_id is null and master_id_2 is null)",
            condition.getWhereClause());

    master1Key.put(TestDomain.MASTER_ID_2, 1);
    condition = whereCondition(selectCondition(TestDomain.DETAIL_MASTER_FK, Operator.EQUALS, master1Key), detailDefinition);
    assertEquals("(master_id is null and master_id_2 = ?)",
            condition.getWhereClause());

    final Key deptKey = ENTITIES.key(TestDomain.T_DEPARTMENT, 42);

    condition = whereCondition(selectCondition(
            TestDomain.EMP_DEPARTMENT_FK, Operator.EQUALS, deptKey), empDefinition);
    assertEquals("deptno = ?", condition.getWhereClause());
  }

  @Test
  public void conditionTest() {
    final Entity entity = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    final EntityDefinition deptDefinition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);

    WhereCondition condition = whereCondition(Conditions.condition(entity.getKey()), deptDefinition);
    assertKeyCondition(condition);

    condition = whereCondition(Conditions.condition(entity.getKey()), deptDefinition);
    assertKeyCondition(condition);

    condition = whereCondition(condition(TestDomain.DEPARTMENT_NAME, Operator.NOT_EQUALS, "DEPT"), deptDefinition);
    assertCondition(condition);
  }

  @Test
  public void selectConditionTest() {
    final Entity entity = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    final EntityDefinition deptDefinition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);

    WhereCondition condition = whereCondition(Conditions.selectCondition(entity.getKey()), deptDefinition);
    assertKeyCondition(condition);

    condition = whereCondition(Conditions.selectCondition(singletonList(entity.getKey())), deptDefinition);
    assertKeyCondition(condition);

    condition = whereCondition(Conditions.selectCondition(TestDomain.DEPARTMENT_NAME, Operator.NOT_EQUALS, "DEPT"), deptDefinition);
    assertCondition(condition);
  }

  @Test
  public void customConditionTest() {
    final EntityDefinition departmentDefinition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);
    final WhereCondition condition = whereCondition(selectCondition(
            Conditions.customCondition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_CONDITION_ID))
            .setOrderBy(orderBy().ascending(TestDomain.DEPARTMENT_NAME)), departmentDefinition);

    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getColumnProperties().isEmpty());
  }

  @Test
  public void selectAllCondition() {
    final EntityDefinition departmentDefinition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);
    final WhereCondition selectCondition = whereCondition(Conditions.selectCondition(TestDomain.T_DEPARTMENT), departmentDefinition);
    assertTrue(selectCondition.getValues().isEmpty());
    assertTrue(selectCondition.getColumnProperties().isEmpty());

    final WhereCondition condition = whereCondition(Conditions.condition(TestDomain.T_DEPARTMENT), departmentDefinition);
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getColumnProperties().isEmpty());
  }

  @Test
  public void selectConditionOrderByDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.selectCondition(TestDomain.T_EMP)
            .setOrderBy(orderBy().ascending(TestDomain.EMP_NAME).descending(TestDomain.EMP_NAME)));
  }

  @Test
  public void attributeConditionWithNonColumnProperty() {
    final EntityDefinition definition = ENTITIES.getDefinition(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () ->
            whereCondition(condition(TestDomain.EMP_DEPARTMENT_LOCATION, NullCheck.IS_NULL), definition).getWhereClause());
  }

  @Test
  public void whereClause() throws Exception {
    final EntityDefinition departmentDefinition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);
    final ColumnProperty<?> property = (ColumnProperty<?>) departmentDefinition.getProperty(TestDomain.DEPARTMENT_NAME);
    WhereCondition condition = whereCondition(condition(TestDomain.DEPARTMENT_NAME, Operator.EQUALS, "upper%"), departmentDefinition);
    assertEquals(property.getColumnName() + " like ?", condition.getWhereClause());
    condition = whereCondition(condition(TestDomain.DEPARTMENT_NAME, Operator.EQUALS, "upper"), departmentDefinition);
    assertEquals(property.getColumnName() + " = ?", condition.getWhereClause());
    condition = whereCondition(condition(TestDomain.DEPARTMENT_NAME, Operator.NOT_EQUALS, "upper%"), departmentDefinition);
    assertEquals(property.getColumnName() + " not like ?", condition.getWhereClause());
    condition = whereCondition(condition(TestDomain.DEPARTMENT_NAME, Operator.NOT_EQUALS, "upper"), departmentDefinition);
    assertEquals(property.getColumnName() + " <> ?", condition.getWhereClause());
    condition = whereCondition(condition(TestDomain.DEPARTMENT_NAME, Operator.GREATER_THAN, "upper"), departmentDefinition);
    assertEquals(property.getColumnName() + " >= ?", condition.getWhereClause());
    condition = whereCondition(condition(TestDomain.DEPARTMENT_NAME, Operator.LESS_THAN, "upper"), departmentDefinition);
    assertEquals(property.getColumnName() + " <= ?", condition.getWhereClause());

    condition = whereCondition(condition(TestDomain.DEPARTMENT_NAME, Operator.WITHIN_RANGE, "upper", "lower"), departmentDefinition);
    assertEquals("(" + property.getColumnName() + " >= ? and " + property.getColumnName() + " <= ?)", condition.getWhereClause());

    condition = whereCondition(condition(TestDomain.DEPARTMENT_NAME, Operator.EQUALS, "%upper%"), departmentDefinition);
    assertEquals(property.getColumnName() + " like ?", condition.getWhereClause());
    condition = whereCondition(condition(TestDomain.DEPARTMENT_NAME, Operator.NOT_EQUALS, "%upper%"), departmentDefinition);
    assertEquals(property.getColumnName() + " not like ?", condition.getWhereClause());
  }

  private static void assertKeyCondition(final WhereCondition condition) {
    assertEquals("deptno = ?", condition.getWhereClause());
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getColumnProperties().size());
    assertEquals(10, condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_ID, condition.getColumnProperties().get(0).getAttribute());
  }

  private static void assertCondition(final WhereCondition condition) {
    assertEquals("dname <> ?", condition.getWhereClause());
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getColumnProperties().size());
    assertEquals("DEPT", condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_NAME, condition.getColumnProperties().get(0).getAttribute());
  }
}
