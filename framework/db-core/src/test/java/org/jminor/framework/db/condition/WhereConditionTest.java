/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.TestDomain;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.jminor.framework.db.condition.Conditions.*;
import static org.junit.jupiter.api.Assertions.*;

public final class WhereConditionTest {

  private static final TestDomain DOMAIN = new TestDomain();

  @Test
  public void test() {
    final Condition.Set set1 = Conditions.conditionSet(
            Conjunction.AND,
            Conditions.propertyCondition(TestDomain.DETAIL_STRING, ConditionType.LIKE, "value"),
            Conditions.propertyCondition(TestDomain.DETAIL_INT, ConditionType.LIKE, 666)
    );
    final Entity.Definition detailDefinition = DOMAIN.getDefinition(TestDomain.T_DETAIL);
    final WhereCondition condition = whereCondition(entityCondition(TestDomain.T_DETAIL, set1), detailDefinition);
    assertEquals("(string = ? and int = ?)", condition.getWhereClause());
    final Condition.Set set2 = Conditions.conditionSet(
            Conjunction.AND,
            Conditions.propertyCondition(TestDomain.DETAIL_DOUBLE, ConditionType.LIKE, 666.666),
            Conditions.propertyCondition(TestDomain.DETAIL_STRING, ConditionType.LIKE, "valu%e2").setCaseSensitive(false)
    );
    final Condition.Set set3 = Conditions.conditionSet(Conjunction.OR, set1, set2);
    assertEquals("((string = ? and int = ?) or (double = ? and upper(string) like upper(?)))",
            whereCondition(entityCondition(TestDomain.T_DETAIL, set3), detailDefinition).getWhereClause());
  }

  @Test
  public void propertyConditionTest() {
    final WhereCondition critOne = whereCondition(entityCondition(TestDomain.T_DEPARTMENT,
            Conditions.propertyCondition(TestDomain.DEPARTMENT_LOCATION, ConditionType.LIKE, "New York")), DOMAIN.getDefinition(TestDomain.T_DEPARTMENT));
    assertEquals("loc = ?", critOne.getWhereClause());
    assertNotNull(critOne);
  }

  @Test
  public void foreignKeyConditionNull() {
    final Entity.Definition definition = DOMAIN.getDefinition(TestDomain.T_EMP);
    final WhereCondition condition = whereCondition(entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, (Entity.Key) null), definition);
    assertEquals("deptno is null", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionEntity() {
    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final Entity.Definition empDefinition = DOMAIN.getDefinition(TestDomain.T_EMP);
    WhereCondition condition = whereCondition(entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, department), empDefinition);
    assertEquals("deptno = ?", condition.getWhereClause());

    final Entity department2 = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department2.put(TestDomain.DEPARTMENT_ID, 11);
    condition = whereCondition(entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, asList(department, department2)), empDefinition);
    assertEquals("(deptno in (?, ?))", condition.getWhereClause());

    condition = whereCondition(entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.NOT_LIKE, asList(department, department2)), empDefinition);
    assertEquals("(deptno not in (?, ?))", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionEntityKey() {
    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final Entity.Definition empDefinition = DOMAIN.getDefinition(TestDomain.T_EMP);
    final WhereCondition condition = whereCondition(entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, department.getKey()), empDefinition);
    assertEquals("deptno = ?", condition.getWhereClause());
  }

  @Test
  public void compositeForeignKey() {
    final Entity master1 = DOMAIN.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, 1);
    master1.put(TestDomain.MASTER_ID_2, 2);

    final Entity master2 = DOMAIN.entity(TestDomain.T_MASTER);
    master2.put(TestDomain.MASTER_ID_1, 3);
    master2.put(TestDomain.MASTER_ID_2, 4);

    final Entity.Definition detailDefinition = DOMAIN.getDefinition(TestDomain.T_DETAIL);
    WhereCondition condition = whereCondition(entityCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE, master1), detailDefinition);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.getWhereClause());

    condition = whereCondition(entityCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.NOT_LIKE, master1), detailDefinition);
    assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.getWhereClause());

    condition = whereCondition(entityCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE, asList(master1, master2)), detailDefinition);
    assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", condition.getWhereClause());

    condition = whereCondition(entityCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.NOT_LIKE, asList(master1, master2)), detailDefinition);
    assertEquals("((master_id <> ? and master_id_2 <> ?) or (master_id <> ? and master_id_2 <> ?))", condition.getWhereClause());
  }

  @Test
  public void selectConditionCompositeKey() {
    final Entity master1 = DOMAIN.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, 1);
    master1.put(TestDomain.MASTER_ID_2, 2);

    final Entity master2 = DOMAIN.entity(TestDomain.T_MASTER);
    master2.put(TestDomain.MASTER_ID_1, 3);
    master2.put(TestDomain.MASTER_ID_2, 4);

    final Entity.Definition masterDefinition = DOMAIN.getDefinition(TestDomain.T_MASTER);
    WhereCondition condition = whereCondition(entitySelectCondition(master1.getKey()), masterDefinition);
    assertEquals("(id = ? and id2 = ?)", condition.getWhereClause());

    condition = whereCondition(entitySelectCondition(asList(master1.getKey(), master2.getKey())), masterDefinition);
    assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", condition.getWhereClause());
  }

  @Test
  public void keyNullCondition() {
    final Entity.Definition empDefinition = DOMAIN.getDefinition(TestDomain.T_EMP);
    WhereCondition condition = whereCondition(entitySelectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, singletonList(null)), empDefinition);
    assertEquals("deptno is null", condition.getWhereClause());

    condition = whereCondition(entitySelectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, null), empDefinition);
    assertEquals("deptno is null", condition.getWhereClause());

    final Entity.Key master1 = DOMAIN.key(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, null);
    master1.put(TestDomain.MASTER_ID_2, null);

    final Entity.Definition detailDefinition = DOMAIN.getDefinition(TestDomain.T_DETAIL);
    condition = whereCondition(entitySelectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE, master1), detailDefinition);
    assertEquals("(master_id is null and master_id_2 is null)",
            condition.getWhereClause());

    master1.put(TestDomain.MASTER_ID_2, 1);
    condition = whereCondition(entitySelectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE, master1), detailDefinition);
    assertEquals("(master_id is null and master_id_2 = ?)",
            condition.getWhereClause());

    final Entity.Key deptKey = DOMAIN.key(TestDomain.T_DEPARTMENT);
    deptKey.put(TestDomain.DEPARTMENT_ID, 42);

    condition = whereCondition(entitySelectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, deptKey), empDefinition);
    assertEquals("deptno = ?", condition.getWhereClause());
  }

  @Test
  public void conditionTest() {
    final Entity entity = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    final Entity.Definition deptDefinition = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT);

    WhereCondition condition = whereCondition(entityCondition(entity.getKey()), deptDefinition);
    assertKeyCondition(condition);

    condition = whereCondition(entityCondition(singletonList(entity.getKey())), deptDefinition);
    assertKeyCondition(condition);

    condition = whereCondition(entityCondition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, ConditionType.NOT_LIKE, "DEPT"), deptDefinition);
    assertCondition(condition);
  }

  @Test
  public void selectConditionTest() {
    final Entity entity = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    final Entity.Definition deptDefinition = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT);

    WhereCondition condition = whereCondition(Conditions.entitySelectCondition(entity.getKey()), deptDefinition);
    assertKeyCondition(condition);

    condition = whereCondition(Conditions.entitySelectCondition(singletonList(entity.getKey())), deptDefinition);
    assertKeyCondition(condition);

    condition = whereCondition(Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, ConditionType.NOT_LIKE, "DEPT"), deptDefinition);
    assertCondition(condition);
  }

  @Test
  public void customConditionTest() {
    final Entity.Definition departmentDefinition = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT);
    final WhereCondition condition = whereCondition(entitySelectCondition(TestDomain.T_DEPARTMENT,
            Conditions.customCondition(TestDomain.DEPARTMENT_CONDITION_ID))
            .setOrderBy(Domain.orderBy().ascending(TestDomain.DEPARTMENT_NAME)), departmentDefinition);

    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getColumnProperties().isEmpty());
  }

  @Test
  public void selectAllCondition() {
    final Entity.Definition departmentDefinition = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT);
    final WhereCondition selectCondition = whereCondition(Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT), departmentDefinition);
    assertTrue(selectCondition.getValues().isEmpty());
    assertTrue(selectCondition.getColumnProperties().isEmpty());

    final WhereCondition condition = whereCondition(Conditions.entityCondition(TestDomain.T_DEPARTMENT), departmentDefinition);
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getColumnProperties().isEmpty());
  }

  @Test
  public void selectConditionOrderByDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.entitySelectCondition(TestDomain.T_EMP)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.EMP_NAME).descending(TestDomain.EMP_NAME)));
  }

  @Test
  public void propertyConditionWithNonColumnProperty() {
    final Entity.Definition definition = DOMAIN.getDefinition(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () -> whereCondition(entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_LOCATION, ConditionType.LIKE, null), definition)
            .getWhereClause());
  }

  @Test
  public void selectConditionInvalidType() {
    final Entity.Definition definition = DOMAIN.getDefinition(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () -> whereCondition(entitySelectCondition(TestDomain.T_EMP,
            TestDomain.EMP_COMMISSION, ConditionType.LIKE, "test"), definition)
            .getWhereClause());
  }

  @Test
  public void propertyConditionModel() throws Exception {
    final Entity.Definition departmentDefinition = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT);
    final ColumnProperty property = (ColumnProperty) departmentDefinition.getProperty(TestDomain.DEPARTMENT_NAME);
    WhereCondition condition = whereCondition(entityCondition(TestDomain.T_DEPARTMENT,
            propertyCondition(TestDomain.DEPARTMENT_NAME, ConditionType.LIKE, "upper%")), departmentDefinition);
    assertEquals(property.getPropertyId() + " like ?", condition.getWhereClause());
    condition = whereCondition(entityCondition(TestDomain.T_DEPARTMENT,
            propertyCondition(TestDomain.DEPARTMENT_NAME, ConditionType.LIKE, "upper")), departmentDefinition);
    assertEquals(property.getPropertyId() + " = ?", condition.getWhereClause());
    condition = whereCondition(entityCondition(TestDomain.T_DEPARTMENT,
            propertyCondition(TestDomain.DEPARTMENT_NAME, ConditionType.NOT_LIKE, "upper%")), departmentDefinition);
    assertEquals(property.getPropertyId() + " not like ?", condition.getWhereClause());
    condition = whereCondition(entityCondition(TestDomain.T_DEPARTMENT,
            propertyCondition(TestDomain.DEPARTMENT_NAME, ConditionType.NOT_LIKE, "upper")), departmentDefinition);
    assertEquals(property.getPropertyId() + " <> ?", condition.getWhereClause());
    condition = whereCondition(entityCondition(TestDomain.T_DEPARTMENT,
            propertyCondition(TestDomain.DEPARTMENT_NAME, ConditionType.GREATER_THAN, "upper")), departmentDefinition);
    assertEquals(property.getPropertyId() + " >= ?", condition.getWhereClause());
    condition = whereCondition(entityCondition(TestDomain.T_DEPARTMENT,
            propertyCondition(TestDomain.DEPARTMENT_NAME, ConditionType.LESS_THAN, "upper")), departmentDefinition);
    assertEquals(property.getPropertyId() + " <= ?", condition.getWhereClause());

    condition = whereCondition(entityCondition(TestDomain.T_DEPARTMENT,
            propertyCondition(TestDomain.DEPARTMENT_NAME, ConditionType.WITHIN_RANGE, asList("upper", "lower"))), departmentDefinition);
    assertEquals("(" + property.getPropertyId() + " >= ? and " + property.getPropertyId() + " <= ?)", condition.getWhereClause());

    condition = whereCondition(entityCondition(TestDomain.T_DEPARTMENT,
            propertyCondition(TestDomain.DEPARTMENT_NAME, ConditionType.LIKE, "%upper%")), departmentDefinition);
    assertEquals(property.getPropertyId() + " like ?", condition.getWhereClause());
    condition = whereCondition(entityCondition(TestDomain.T_DEPARTMENT,
            propertyCondition(TestDomain.DEPARTMENT_NAME, ConditionType.NOT_LIKE, "%upper%")), departmentDefinition);
    assertEquals(property.getPropertyId() + " not like ?", condition.getWhereClause());
  }

  private static void assertKeyCondition(final WhereCondition condition) {
    assertEquals("deptno = ?", condition.getWhereClause());
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getColumnProperties().size());
    assertEquals(10, condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_ID, condition.getColumnProperties().get(0).getPropertyId());
  }

  private static void assertCondition(final WhereCondition condition) {
    assertEquals("dname <> ?", condition.getWhereClause());
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getColumnProperties().size());
    assertEquals("DEPT", condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_NAME, condition.getColumnProperties().get(0).getPropertyId());
  }
}
