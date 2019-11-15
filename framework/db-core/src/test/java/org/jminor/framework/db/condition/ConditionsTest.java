/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.TestDomain;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.jminor.framework.db.condition.Conditions.entityCondition;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionsTest {

  private static final TestDomain DOMAIN = new TestDomain();

  @Test
  public void test() {
    final Condition.Set set1 = Conditions.conditionSet(
            Conjunction.AND,
            Conditions.propertyCondition(TestDomain.DETAIL_STRING, ConditionType.LIKE, "value"),
            Conditions.propertyCondition(TestDomain.DETAIL_INT, ConditionType.LIKE, 666)
    );
    final EntityCondition condition = entityCondition(TestDomain.T_DETAIL, set1);
    final Entity.Definition detailDefinition = DOMAIN.getDefinition(TestDomain.T_DETAIL);
    assertEquals("(string = ? and int = ?)", condition.getWhereClause(detailDefinition));
    assertEquals(set1, condition.getCondition(detailDefinition));
    final Condition.Set set2 = Conditions.conditionSet(
            Conjunction.AND,
            Conditions.propertyCondition(TestDomain.DETAIL_DOUBLE, ConditionType.LIKE, 666.666),
            Conditions.propertyCondition(TestDomain.DETAIL_STRING, ConditionType.LIKE, "valu%e2", false)
    );
    final Condition.Set set3 = Conditions.conditionSet(Conjunction.OR, set1, set2);
    assertEquals("((string = ? and int = ?) or (double = ? and upper(string) like upper(?)))",
            entityCondition(TestDomain.T_DETAIL, set3).getWhereClause(detailDefinition));
  }

  @Test
  public void conditionTest() {
    final Entity entity = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    EntityCondition condition = entityCondition(entity.getKey());
    assertKeyCondition(condition);

    condition = entityCondition(singletonList(entity.getKey()));
    assertKeyCondition(condition);

    condition = entityCondition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, ConditionType.NOT_LIKE, "DEPT");
    assertCondition(condition);
  }

  @Test
  public void selectConditionTest() {
    final Entity entity = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    EntitySelectCondition condition = Conditions.entitySelectCondition(entity.getKey());
    assertKeyCondition(condition);

    condition = Conditions.entitySelectCondition(singletonList(entity.getKey()));
    assertKeyCondition(condition);

    condition = Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, ConditionType.NOT_LIKE, "DEPT");
    assertCondition(condition);

    final Condition critOne = Conditions.propertyCondition(TestDomain.DEPARTMENT_LOCATION, ConditionType.LIKE, "New York");

    condition = Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT, critOne).setOrderBy(
            Domain.orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertEquals(-1, condition.getFetchCount());

    condition = Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT).setFetchCount(10);
    assertEquals(10, condition.getFetchCount());
  }

  @Test
  public void propertyConditionTest() {
    final EntityCondition critOne = entityCondition(TestDomain.T_DEPARTMENT,
            Conditions.propertyCondition(TestDomain.DEPARTMENT_LOCATION, ConditionType.LIKE, "New York", true));
    assertEquals("loc = ?", critOne.getWhereClause(DOMAIN.getDefinition(TestDomain.T_DEPARTMENT)));
    assertNotNull(critOne);
  }

  @Test
  public void foreignKeyConditionNull() {
    final EntityCondition condition = entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, (Entity.Key) null);
    assertEquals("deptno is null", condition.getWhereClause(DOMAIN.getDefinition(TestDomain.T_EMP)));
  }

  @Test
  public void foreignKeyConditionEntity() {
    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    EntityCondition condition = entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, department);
    final Entity.Definition empDefinition = DOMAIN.getDefinition(TestDomain.T_EMP);
    assertEquals("deptno = ?", condition.getWhereClause(empDefinition));

    final Entity department2 = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department2.put(TestDomain.DEPARTMENT_ID, 11);
    condition = entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, asList(department, department2));
    assertEquals("(deptno in (?, ?))", condition.getWhereClause(empDefinition));

    condition = entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.NOT_LIKE, asList(department, department2));
    assertEquals("(deptno not in (?, ?))", condition.getWhereClause(empDefinition));
  }

  @Test
  public void foreignKeyConditionEntityKey() {
    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final EntityCondition condition = entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, department.getKey());
    assertEquals("deptno = ?", condition.getWhereClause(DOMAIN.getDefinition(TestDomain.T_EMP)));
  }

  @Test
  public void compositeForeignKey() {
    final Entity master1 = DOMAIN.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, 1);
    master1.put(TestDomain.MASTER_ID_2, 2);

    final Entity master2 = DOMAIN.entity(TestDomain.T_MASTER);
    master2.put(TestDomain.MASTER_ID_1, 3);
    master2.put(TestDomain.MASTER_ID_2, 4);

    EntityCondition condition = entityCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE, master1);
    final Entity.Definition detailDefinition = DOMAIN.getDefinition(TestDomain.T_DETAIL);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.getWhereClause(detailDefinition));

    condition = entityCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.NOT_LIKE, master1);
    assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.getWhereClause(detailDefinition));

    condition = entityCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE, asList(master1, master2));
    assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", condition.getWhereClause(detailDefinition));

    condition = entityCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.NOT_LIKE, asList(master1, master2));
    assertEquals("((master_id <> ? and master_id_2 <> ?) or (master_id <> ? and master_id_2 <> ?))", condition.getWhereClause(detailDefinition));
  }

  @Test
  public void selectConditionCompositeKey() {
    final Entity master1 = DOMAIN.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, 1);
    master1.put(TestDomain.MASTER_ID_2, 2);

    final Entity master2 = DOMAIN.entity(TestDomain.T_MASTER);
    master2.put(TestDomain.MASTER_ID_1, 3);
    master2.put(TestDomain.MASTER_ID_2, 4);

    EntityCondition condition = Conditions.entitySelectCondition(master1.getKey());
    final Entity.Definition masterDefinition = DOMAIN.getDefinition(TestDomain.T_MASTER);
    assertEquals("(id = ? and id2 = ?)", condition.getWhereClause(masterDefinition));

    condition = Conditions.entitySelectCondition(asList(master1.getKey(), master2.getKey()));
    assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", condition.getWhereClause(masterDefinition));
  }

  @Test
  public void keyNullCondition() {
    EntityCondition condition = Conditions.entitySelectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, singletonList(null));
    final Entity.Definition empDefinition = DOMAIN.getDefinition(TestDomain.T_EMP);
    assertEquals("deptno is null", condition.getWhereClause(empDefinition));

    condition = Conditions.entitySelectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, null);
    assertEquals("deptno is null", condition.getWhereClause(empDefinition));

    final Entity.Key master1 = DOMAIN.key(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, null);
    master1.put(TestDomain.MASTER_ID_2, null);

    condition = Conditions.entitySelectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE, master1);
    final Entity.Definition detailDefinition = DOMAIN.getDefinition(TestDomain.T_DETAIL);
    assertEquals("(master_id is null and master_id_2 is null)",
            condition.getWhereClause(detailDefinition));

    master1.put(TestDomain.MASTER_ID_2, 1);
    condition = Conditions.entitySelectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE, master1);
    assertEquals("(master_id is null and master_id_2 = ?)",
            condition.getWhereClause(detailDefinition));

    final Entity.Key deptKey = DOMAIN.key(TestDomain.T_DEPARTMENT);
    deptKey.put(TestDomain.DEPARTMENT_ID, 42);

    condition = Conditions.entitySelectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, deptKey);
    assertEquals("deptno = ?", condition.getWhereClause(empDefinition));
  }

  @Test
  public void selectConditionKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.entitySelectCondition(emptyList()));
  }

  @Test
  public void customConditionTest() {
    final EntitySelectCondition condition = Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT,
            Conditions.customCondition(TestDomain.DEPARTMENT_NAME_NOT_NULL_CONDITION_ID))
            .setOrderBy(Domain.orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    final Entity.Definition departmentDefinition = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT);
    assertTrue(condition.getCondition(departmentDefinition).getValues().isEmpty());
    assertTrue(condition.getCondition(departmentDefinition).getPropertyIds().isEmpty());
    final Entity.OrderBy.OrderByProperty deptNameOrder = condition.getOrderBy().getOrderByProperties().get(0);
    assertEquals(deptNameOrder.getPropertyId(), TestDomain.DEPARTMENT_NAME);
    assertFalse(deptNameOrder.isDescending());
  }

  @Test
  public void selectAllCondition() {
    final EntitySelectCondition selectCondition = Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT);
    final Entity.Definition departmentDefinition = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT);
    assertTrue(selectCondition.getCondition(departmentDefinition).getValues().isEmpty());
    assertTrue(selectCondition.getCondition(departmentDefinition).getPropertyIds().isEmpty());

    final EntityCondition condition = entityCondition(TestDomain.T_DEPARTMENT);
    assertTrue(condition.getCondition(departmentDefinition).getValues().isEmpty());
    assertTrue(condition.getCondition(departmentDefinition).getPropertyIds().isEmpty());
  }

  @Test
  public void selectConditionOrderBy() {
    final EntitySelectCondition condition = Conditions.entitySelectCondition(TestDomain.T_EMP)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.EMP_DEPARTMENT).descending(TestDomain.EMP_ID));
    final Entity.OrderBy.OrderByProperty deptOrder = condition.getOrderBy().getOrderByProperties().get(0);
    assertEquals(deptOrder.getPropertyId(), TestDomain.EMP_DEPARTMENT);
    assertFalse(deptOrder.isDescending());
    final Entity.OrderBy.OrderByProperty empOrder = condition.getOrderBy().getOrderByProperties().get(1);
    assertEquals(empOrder.getPropertyId(), TestDomain.EMP_ID);
    assertTrue(empOrder.isDescending());
  }

  @Test
  public void selectConditionOrderByDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.entitySelectCondition(TestDomain.T_EMP)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.EMP_NAME).descending(TestDomain.EMP_NAME)));
  }

  @Test
  public void propertyConditionWithNonColumnProperty() {
    assertThrows(IllegalArgumentException.class, () -> entityCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_LOCATION, ConditionType.LIKE, null)
            .getWhereClause(DOMAIN.getDefinition(TestDomain.T_EMP)));
  }

  @Test
  public void selectConditionOrderBySamePropertyId() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.entitySelectCondition(TestDomain.T_EMP)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.EMP_DEPARTMENT).descending(TestDomain.EMP_DEPARTMENT)));
  }

  @Test
  public void selectConditionInvalidType() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.entitySelectCondition(TestDomain.T_EMP,
            TestDomain.EMP_COMMISSION, ConditionType.LIKE, "test")
            .getWhereClause(DOMAIN.getDefinition(TestDomain.T_EMP)));
  }

  private static void assertKeyCondition(final EntityCondition condition) {
    final Entity.Definition departmentDefinition = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT);
    assertEquals(TestDomain.T_DEPARTMENT, condition.getEntityId());
    assertEquals("deptno = ?", condition.getWhereClause(departmentDefinition));
    assertEquals(1, condition.getCondition(departmentDefinition).getValues().size());
    assertEquals(1, condition.getCondition(departmentDefinition).getPropertyIds().size());
    assertEquals(10, condition.getCondition(departmentDefinition).getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_ID, condition.getCondition(departmentDefinition).getPropertyIds().get(0));
  }

  private static void assertCondition(final EntityCondition condition) {
    final Entity.Definition departmentDefinition = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT);
    assertEquals(TestDomain.T_DEPARTMENT, condition.getEntityId());
    assertEquals("dname <> ?", condition.getWhereClause(departmentDefinition));
    assertEquals(1, condition.getCondition(departmentDefinition).getValues().size());
    assertEquals(1, condition.getCondition(departmentDefinition).getPropertyIds().size());
    assertEquals("DEPT", condition.getCondition(departmentDefinition).getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_NAME, condition.getCondition(departmentDefinition).getPropertyIds().get(0));
  }
}
