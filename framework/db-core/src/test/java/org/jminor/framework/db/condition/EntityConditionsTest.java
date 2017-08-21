/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.framework.db.TestDomain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class EntityConditionsTest {

  private static final TestDomain entities = new TestDomain();
  private static final EntityConditions entityConditions = new EntityConditions(entities);

  @Test
  public void test() {
    final Condition.Set<Property.ColumnProperty> set1 = Conditions.conditionSet(
            Conjunction.AND,
            entityConditions.propertyCondition(Properties.columnProperty("stringProperty", Types.VARCHAR), Condition.Type.LIKE, "value"),
            entityConditions.propertyCondition(Properties.columnProperty("intProperty", Types.INTEGER), Condition.Type.LIKE, 666)
    );
    final EntityCondition condition = entityConditions.condition("entityID", set1);
    assertEquals("(stringProperty like ? and intProperty = ?)", condition.getWhereClause());
    assertEquals(set1, condition.getCondition());
    final Condition.Set<Property.ColumnProperty> set2 = Conditions.conditionSet(
            Conjunction.AND,
            entityConditions.propertyCondition(Properties.columnProperty("doubleProperty", Types.DOUBLE), Condition.Type.LIKE, 666.666),
            entityConditions.propertyCondition(Properties.columnProperty("stringProperty2", Types.VARCHAR), Condition.Type.LIKE, false, "value2")
    );
    final Condition.Set<Property.ColumnProperty> set3 = Conditions.conditionSet(Conjunction.OR, set1, set2);
    assertEquals("((stringProperty like ? and intProperty = ?) or (doubleProperty = ? and upper(stringProperty2) like upper(?)))",
            entityConditions.condition("entityID", set3).getWhereClause());
  }

  @Test
  public void condition() {
    final Entity entity = entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    EntityCondition condition = entityConditions.condition(entity.getKey());
    assertKeyCondition(condition);

    condition = entityConditions.condition(Collections.singletonList(entity.getKey()));
    assertKeyCondition(condition);

    condition = entityConditions.condition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, Condition.Type.NOT_LIKE, "DEPT");
    assertCondition(condition);
  }

  @Test
  public void selectCondition() {
    final Entity entity = entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    EntitySelectCondition condition = entityConditions.selectCondition(entity.getKey());
    assertKeyCondition(condition);

    condition = entityConditions.selectCondition(Collections.singletonList(entity.getKey()));
    assertKeyCondition(condition);

    condition = entityConditions.selectCondition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, Condition.Type.NOT_LIKE, "DEPT");
    assertCondition(condition);

    final Condition<Property.ColumnProperty> critOne = entityConditions.propertyCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_LOCATION, Condition.Type.LIKE, "New York");

    condition = entityConditions.selectCondition(TestDomain.T_DEPARTMENT, critOne, TestDomain.DEPARTMENT_NAME);
    assertEquals(-1, condition.getFetchCount());

    condition = entityConditions.selectCondition(TestDomain.T_DEPARTMENT, 10);
    assertEquals(10, condition.getFetchCount());
  }

  @Test
  public void propertyCondition() {
    final Condition<Property.ColumnProperty> critOne = entityConditions.propertyCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_LOCATION, Condition.Type.LIKE, true, "New York");
    assertEquals("loc like ?", critOne.getWhereClause());
    assertNotNull(critOne);
  }

  @Test
  public void foreignKeyConditionNull() {
    final Condition<Property.ColumnProperty> condition = entityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, (Entity.Key) null);
    assertEquals("deptno is null", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionEntity() {
    final Entity department = entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    Condition<Property.ColumnProperty> condition = entityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, department);
    assertEquals("deptno = ?", condition.getWhereClause());

    final Entity department2 = entities.entity(TestDomain.T_DEPARTMENT);
    department2.put(TestDomain.DEPARTMENT_ID, 11);
    condition = entityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, Arrays.asList(department, department2));
    assertEquals("(deptno in (?, ?))", condition.getWhereClause());

    condition = entityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.NOT_LIKE, Arrays.asList(department, department2));
    assertEquals("(deptno not in (?, ?))", condition.getWhereClause());
  }

  @Test
  public void foreignKeyCondition() {
    final Property.ForeignKeyProperty foreignKeyProperty = entities.getForeignKeyProperty(TestDomain.T_MASTER, TestDomain.MASTER_SUPER_FK);
    final Condition<Property.ColumnProperty> condition = entityConditions.foreignKeyCondition(foreignKeyProperty,
            Condition.Type.LIKE, Collections.singletonList(null));
    assertEquals("super_id is null", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionEntityKey() {
    final Entity department = entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final Condition<Property.ColumnProperty> condition = entityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, department.getKey());
    assertEquals("deptno = ?", condition.getWhereClause());
  }

  @Test
  public void compositeForeignKey() {
    final Entity master1 = entities.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, 1);
    master1.put(TestDomain.MASTER_ID_2, 2);

    final Entity master2 = entities.entity(TestDomain.T_MASTER);
    master2.put(TestDomain.MASTER_ID_1, 3);
    master2.put(TestDomain.MASTER_ID_2, 4);

    Condition<Property.ColumnProperty> condition = entityConditions.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, Condition.Type.LIKE, master1);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.getWhereClause());

    condition = entityConditions.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, Condition.Type.NOT_LIKE, master1);
    assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.getWhereClause());

    condition = entityConditions.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, Condition.Type.LIKE,
            Arrays.asList(master1, master2));
    assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", condition.getWhereClause());

    condition = entityConditions.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, Condition.Type.NOT_LIKE,
            Arrays.asList(master1, master2));
    assertEquals("((master_id <> ? and master_id_2 <> ?) or (master_id <> ? and master_id_2 <> ?))", condition.getWhereClause());
  }

  @Test
  public void selectConditionCompositeKey() {
    final Entity master1 = entities.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, 1);
    master1.put(TestDomain.MASTER_ID_2, 2);

    final Entity master2 = entities.entity(TestDomain.T_MASTER);
    master2.put(TestDomain.MASTER_ID_1, 3);
    master2.put(TestDomain.MASTER_ID_2, 4);

    Condition<Property.ColumnProperty> condition = entityConditions.selectCondition(master1.getKey());
    assertEquals("(id = ? and id2 = ?)", condition.getWhereClause());

    condition = entityConditions.selectCondition(Arrays.asList(master1.getKey(), master2.getKey()));
    assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", condition.getWhereClause());
  }

  @Test
  public void keyNullCondition() {
    Condition<Property.ColumnProperty> condition = entityConditions.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, Collections.singletonList(null));
    assertEquals("deptno is null", condition.getWhereClause());

    condition = entityConditions.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, Collections.emptyList());
    assertEquals("deptno is null", condition.getWhereClause());

    condition = entityConditions.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, null);
    assertEquals("deptno is null", condition.getWhereClause());

    final Entity.Key master1 = entities.key(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, null);
    master1.put(TestDomain.MASTER_ID_2, null);

    condition = entityConditions.selectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, Condition.Type.LIKE, master1);
    assertEquals("(master_id is null and master_id_2 is null)", condition.getWhereClause());

    master1.put(TestDomain.MASTER_ID_2, 1);
    condition = entityConditions.selectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, Condition.Type.LIKE, master1);
    assertEquals("(master_id is null and master_id_2 = ?)", condition.getWhereClause());

    final Entity.Key deptKey = entities.key(TestDomain.T_DEPARTMENT);
    deptKey.put(TestDomain.DEPARTMENT_ID, 42);

    condition = entityConditions.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, deptKey);
    assertEquals("deptno = ?", condition.getWhereClause());
  }

  @Test(expected = IllegalArgumentException.class)
  public void selectConditionKeyNoKeys() {
    entityConditions.selectCondition(Collections.emptyList());
  }

  @Test
  public void simpleCondition() {
    final EntitySelectCondition condition = entityConditions.selectCondition(TestDomain.T_DEPARTMENT,
            Conditions.stringCondition("department name is not null"), TestDomain.DEPARTMENT_NAME, -1);
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getColumns().isEmpty());
    assertEquals(condition.getOrderByClause(), TestDomain.DEPARTMENT_NAME);
  }

  @Test
  public void selectAllCondition() {
    final EntitySelectCondition selectCondition = entityConditions.selectCondition(TestDomain.T_DEPARTMENT);
    assertTrue(selectCondition.getValues().isEmpty());
    assertTrue(selectCondition.getColumns().isEmpty());

    final EntityCondition condition = entityConditions.condition(TestDomain.T_DEPARTMENT);
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getColumns().isEmpty());
  }

  @Test
  public void selectConditionOrderBy() {
    final EntitySelectCondition condition = entityConditions.selectCondition(TestDomain.T_EMP)
            .orderByAscending(TestDomain.EMP_DEPARTMENT).orderByDescending(TestDomain.EMP_ID);
    assertEquals("deptno, empno desc", condition.getOrderByClause());
  }

  @Test(expected = IllegalArgumentException.class)
  public void propertyConditionWithNonColumnProperty() {
    entityConditions.propertyCondition(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_LOCATION, Condition.Type.LIKE, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void selectConditionOrderBySamePropertyID() {
    entityConditions.selectCondition(TestDomain.T_EMP)
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
