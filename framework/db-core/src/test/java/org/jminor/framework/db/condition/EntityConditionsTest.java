/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.framework.db.TestDomain;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EntityConditionsTest {

  @BeforeClass
  public static void init() {
    TestDomain.init();
  }

  @Test
  public void condition() {
    final Entity entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    EntityCondition condition = EntityConditions.condition(entity.getKey());
    assertKeyCondition(condition);

    condition = EntityConditions.condition(Collections.singletonList(entity.getKey()));
    assertKeyCondition(condition);

    condition = EntityConditions.condition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, Condition.Type.NOT_LIKE, "DEPT");
    assertCondition(condition);
  }

  @Test
  public void selectCondition() {
    final Entity entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    EntitySelectCondition condition = EntityConditions.selectCondition(entity.getKey());
    assertKeyCondition(condition);

    condition = EntityConditions.selectCondition(Collections.singletonList(entity.getKey()));
    assertKeyCondition(condition);

    condition = EntityConditions.selectCondition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, Condition.Type.NOT_LIKE, "DEPT");
    assertCondition(condition);

    final Condition<Property.ColumnProperty> critOne = EntityConditions.propertyCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_LOCATION, Condition.Type.LIKE, "New York");

    condition = EntityConditions.selectCondition(TestDomain.T_DEPARTMENT, critOne, TestDomain.DEPARTMENT_NAME);
    assertEquals(-1, condition.getFetchCount());

    condition = EntityConditions.selectCondition(TestDomain.T_DEPARTMENT, 10);
    assertEquals(10, condition.getFetchCount());
  }

  @Test
  public void propertyCondition() {
    final Condition<Property.ColumnProperty> critOne = EntityConditions.propertyCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_LOCATION, Condition.Type.LIKE, true, "New York");
    assertEquals("loc like ?", critOne.getWhereClause());
    assertNotNull(critOne);
  }

  @Test
  public void foreignKeyConditionNull() {
    final Condition<Property.ColumnProperty> condition = EntityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, (Entity.Key) null);
    assertEquals("deptno is null", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionEntity() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    Condition<Property.ColumnProperty> condition = EntityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, department);
    assertEquals("deptno = ?", condition.getWhereClause());

    final Entity department2 = Entities.entity(TestDomain.T_DEPARTMENT);
    department2.put(TestDomain.DEPARTMENT_ID, 11);
    condition = EntityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, Arrays.asList(department, department2));
    assertEquals("(deptno in (?, ?))", condition.getWhereClause());

    condition = EntityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.NOT_LIKE, Arrays.asList(department, department2));
    assertEquals("(deptno not in (?, ?))", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionEntityKey() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final Condition<Property.ColumnProperty> condition = EntityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, department.getKey());
    assertEquals("deptno = ?", condition.getWhereClause());
  }

  @Test
  public void simpleCondition() {
    final EntitySelectCondition condition = EntityConditions.selectCondition(TestDomain.T_DEPARTMENT,
            Conditions.stringCondition("department name is not null"), TestDomain.DEPARTMENT_NAME, -1);
    assertEquals(0, condition.getValues().size());
    assertEquals(0, condition.getColumns().size());
    assertEquals(condition.getOrderByClause(), TestDomain.DEPARTMENT_NAME);
  }

  @Test
  public void selectConditionOrderBy() {
    final EntitySelectCondition condition = EntityConditions.selectCondition(TestDomain.T_EMP)
            .orderByAscending(TestDomain.EMP_DEPARTMENT).orderByDescending(TestDomain.EMP_ID);
    assertEquals("deptno, empno desc", condition.getOrderByClause());
  }

  @Test(expected = IllegalArgumentException.class)
  public void selectConditionOrderBySamePropertyID() {
    EntityConditions.selectCondition(TestDomain.T_EMP)
            .orderByAscending(TestDomain.EMP_DEPARTMENT).orderByDescending(TestDomain.EMP_DEPARTMENT);
  }

  private void assertKeyCondition(final EntityCondition condition) {
    assertEquals(TestDomain.T_DEPARTMENT, condition.getEntityID());
    assertEquals("deptno = ?", condition.getWhereClause());
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getColumns().size());
    final Object val = condition.getValues().get(0);
    assertEquals(10, condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_ID, condition.getColumns().get(0).getPropertyID());
  }

  private void assertCondition(final EntityCondition condition) {
    assertEquals(TestDomain.T_DEPARTMENT, condition.getEntityID());
    assertEquals("dname not like ?", condition.getWhereClause());
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getColumns().size());
    assertEquals("DEPT", condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_NAME, condition.getColumns().get(0).getPropertyID());
  }
}
