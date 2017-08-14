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

import static org.junit.Assert.*;

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
  public void foreignKeyCondition() {
    final Property.ForeignKeyProperty foreignKeyProperty = Entities.getForeignKeyProperty(TestDomain.T_MASTER, TestDomain.MASTER_SUPER_FK);
    final Condition<Property.ColumnProperty> condition = EntityConditions.foreignKeyCondition(foreignKeyProperty,
            Condition.Type.LIKE, Collections.singletonList(null));
    assertEquals("super_id is null", condition.getWhereClause());
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
  public void compositeForeignKey() {
    final Entity master1 = Entities.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, 1);
    master1.put(TestDomain.MASTER_ID_2, 2);

    final Entity master2 = Entities.entity(TestDomain.T_MASTER);
    master2.put(TestDomain.MASTER_ID_1, 3);
    master2.put(TestDomain.MASTER_ID_2, 4);

    Condition<Property.ColumnProperty> condition = EntityConditions.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, Condition.Type.LIKE, master1);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.getWhereClause());

    condition = EntityConditions.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, Condition.Type.NOT_LIKE, master1);
    assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.getWhereClause());

    condition = EntityConditions.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, Condition.Type.LIKE,
            Arrays.asList(master1, master2));
    assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", condition.getWhereClause());

    condition = EntityConditions.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, Condition.Type.NOT_LIKE,
            Arrays.asList(master1, master2));
    assertEquals("((master_id <> ? and master_id_2 <> ?) or (master_id <> ? and master_id_2 <> ?))", condition.getWhereClause());
  }

  @Test
  public void selectConditionCompositeKey() {
    final Entity master1 = Entities.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, 1);
    master1.put(TestDomain.MASTER_ID_2, 2);

    final Entity master2 = Entities.entity(TestDomain.T_MASTER);
    master2.put(TestDomain.MASTER_ID_1, 3);
    master2.put(TestDomain.MASTER_ID_2, 4);

    Condition<Property.ColumnProperty> condition = EntityConditions.selectCondition(master1.getKey());
    assertEquals("(id = ? and id2 = ?)", condition.getWhereClause());

    condition = EntityConditions.selectCondition(Arrays.asList(master1.getKey(), master2.getKey()));
    assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", condition.getWhereClause());
  }

  @Test
  public void keyNullCondition() {
    Condition<Property.ColumnProperty> condition = EntityConditions.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, Collections.singletonList(null));
    assertEquals("deptno is null", condition.getWhereClause());

    condition = EntityConditions.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, Collections.emptyList());
    assertEquals("deptno is null", condition.getWhereClause());

    condition = EntityConditions.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, null);
    assertEquals("deptno is null", condition.getWhereClause());

    final Entity.Key master1 = Entities.key(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, null);
    master1.put(TestDomain.MASTER_ID_2, null);

    condition = EntityConditions.selectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, Condition.Type.LIKE, master1);
    assertEquals("(master_id is null and master_id_2 is null)", condition.getWhereClause());

    master1.put(TestDomain.MASTER_ID_2, 1);
    condition = EntityConditions.selectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, Condition.Type.LIKE, master1);
    assertEquals("(master_id is null and master_id_2 = ?)", condition.getWhereClause());

    final Entity.Key deptKey = Entities.key(TestDomain.T_DEPARTMENT);
    deptKey.put(TestDomain.DEPARTMENT_ID, 42);

    condition = EntityConditions.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, deptKey);
    assertEquals("deptno = ?", condition.getWhereClause());
  }

  @Test(expected = IllegalArgumentException.class)
  public void selectConditionKeyNoKeys() {
    EntityConditions.selectCondition(Collections.emptyList());
  }

  @Test
  public void simpleCondition() {
    final EntitySelectCondition condition = EntityConditions.selectCondition(TestDomain.T_DEPARTMENT,
            Conditions.stringCondition("department name is not null"), TestDomain.DEPARTMENT_NAME, -1);
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getColumns().isEmpty());
    assertEquals(condition.getOrderByClause(), TestDomain.DEPARTMENT_NAME);
  }

  @Test
  public void selectAllCondition() {
    final EntitySelectCondition selectCondition = EntityConditions.selectCondition(TestDomain.T_DEPARTMENT);
    assertTrue(selectCondition.getValues().isEmpty());
    assertTrue(selectCondition.getColumns().isEmpty());

    final EntityCondition condition = EntityConditions.condition(TestDomain.T_DEPARTMENT);
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getColumns().isEmpty());
  }

  @Test
  public void selectConditionOrderBy() {
    final EntitySelectCondition condition = EntityConditions.selectCondition(TestDomain.T_EMP)
            .orderByAscending(TestDomain.EMP_DEPARTMENT).orderByDescending(TestDomain.EMP_ID);
    assertEquals("deptno, empno desc", condition.getOrderByClause());
  }

  @Test(expected = IllegalArgumentException.class)
  public void propertyConditionWithNonColumnProperty() {
    EntityConditions.propertyCondition(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_LOCATION, Condition.Type.LIKE, null);
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
