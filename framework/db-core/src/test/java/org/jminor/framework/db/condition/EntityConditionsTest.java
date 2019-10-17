/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.TestDomain;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Types;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.jminor.framework.db.condition.EntityConditions.*;
import static org.junit.jupiter.api.Assertions.*;

public class EntityConditionsTest {

  private static final TestDomain DOMAIN = new TestDomain();
  private static final EntityConditions ENTITY_CONDITIONS = using(DOMAIN);

  @Test
  public void test() {
    final Condition.Set set1 = Conditions.conditionSet(
            Conjunction.AND,
            propertyCondition(Properties.columnProperty("stringProperty", Types.VARCHAR), ConditionType.LIKE, "value"),
            propertyCondition(Properties.columnProperty("intProperty", Types.INTEGER), ConditionType.LIKE, 666)
    );
    final EntityCondition condition = condition("entityId", set1);
    assertEquals("(stringProperty = ? and intProperty = ?)", condition.getWhereClause());
    assertEquals(set1, condition.getCondition());
    final Condition.Set set2 = Conditions.conditionSet(
            Conjunction.AND,
            propertyCondition(Properties.columnProperty("doubleProperty", Types.DOUBLE), ConditionType.LIKE, 666.666),
            propertyCondition(Properties.columnProperty("stringProperty2", Types.VARCHAR), ConditionType.LIKE, false, "valu%e2")
    );
    final Condition.Set set3 = Conditions.conditionSet(Conjunction.OR, set1, set2);
    assertEquals("((stringProperty = ? and intProperty = ?) or (doubleProperty = ? and upper(stringProperty2) like upper(?)))",
            condition("entityId", set3).getWhereClause());
  }

  @Test
  public void conditionTest() {
    final Entity entity = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    EntityCondition condition = condition(entity.getKey());
    assertKeyCondition(condition);

    condition = condition(singletonList(entity.getKey()));
    assertKeyCondition(condition);

    condition = ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, ConditionType.NOT_LIKE, "DEPT");
    assertCondition(condition);
  }

  @Test
  public void selectConditionTest() {
    final Entity entity = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    EntitySelectCondition condition = selectCondition(entity.getKey());
    assertKeyCondition(condition);

    condition = selectCondition(singletonList(entity.getKey()));
    assertKeyCondition(condition);

    condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, ConditionType.NOT_LIKE, "DEPT");
    assertCondition(condition);

    final Condition critOne = ENTITY_CONDITIONS.propertyCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_LOCATION, ConditionType.LIKE, "New York");

    condition = selectCondition(TestDomain.T_DEPARTMENT, critOne).setOrderBy(
            Domain.orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertEquals(-1, condition.getFetchCount());

    condition = selectCondition(TestDomain.T_DEPARTMENT).setFetchCount(10);
    assertEquals(10, condition.getFetchCount());
  }

  @Test
  public void propertyConditionTest() {
    final Condition critOne = ENTITY_CONDITIONS.propertyCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_LOCATION, ConditionType.LIKE, true, "New York");
    assertEquals("loc = ?", critOne.getWhereClause());
    assertNotNull(critOne);
  }

  @Test
  public void foreignKeyConditionNull() {
    final Condition condition = ENTITY_CONDITIONS.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, (Entity.Key) null);
    assertEquals("deptno is null", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionEntity() {
    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    Condition condition = ENTITY_CONDITIONS.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, department);
    assertEquals("deptno = ?", condition.getWhereClause());

    final Entity department2 = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department2.put(TestDomain.DEPARTMENT_ID, 11);
    condition = ENTITY_CONDITIONS.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, asList(department, department2));
    assertEquals("(deptno in (?, ?))", condition.getWhereClause());

    condition = ENTITY_CONDITIONS.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.NOT_LIKE, asList(department, department2));
    assertEquals("(deptno not in (?, ?))", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionTest() {
    final Property.ForeignKeyProperty foreignKeyProperty = DOMAIN.getForeignKeyProperty(TestDomain.T_MASTER, TestDomain.MASTER_SUPER_FK);
    final Condition condition = foreignKeyCondition(foreignKeyProperty,
            ConditionType.LIKE, singletonList(null));
    assertEquals("super_id is null", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionEntityKey() {
    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final Condition condition = ENTITY_CONDITIONS.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, department.getKey());
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

    Condition condition = ENTITY_CONDITIONS.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE, master1);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.getWhereClause());

    condition = ENTITY_CONDITIONS.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.NOT_LIKE, master1);
    assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.getWhereClause());

    condition = ENTITY_CONDITIONS.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE,
            asList(master1, master2));
    assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", condition.getWhereClause());

    condition = ENTITY_CONDITIONS.foreignKeyCondition(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK, ConditionType.NOT_LIKE,
            asList(master1, master2));
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

    Condition condition = selectCondition(master1.getKey());
    assertEquals("(id = ? and id2 = ?)", condition.getWhereClause());

    condition = selectCondition(asList(master1.getKey(), master2.getKey()));
    assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", condition.getWhereClause());
  }

  @Test
  public void keyNullCondition() {
    Condition condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, singletonList(null));
    assertEquals("deptno is null", condition.getWhereClause());

    condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, emptyList());
    assertEquals("deptno is null", condition.getWhereClause());

    condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, null);
    assertEquals("deptno is null", condition.getWhereClause());

    final Entity.Key master1 = DOMAIN.key(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, null);
    master1.put(TestDomain.MASTER_ID_2, null);

    condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE, master1);
    assertEquals("(master_id is null and master_id_2 is null)", condition.getWhereClause());

    master1.put(TestDomain.MASTER_ID_2, 1);
    condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, ConditionType.LIKE, master1);
    assertEquals("(master_id is null and master_id_2 = ?)", condition.getWhereClause());

    final Entity.Key deptKey = DOMAIN.key(TestDomain.T_DEPARTMENT);
    deptKey.put(TestDomain.DEPARTMENT_ID, 42);

    condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, ConditionType.LIKE, deptKey);
    assertEquals("deptno = ?", condition.getWhereClause());
  }

  @Test
  public void selectConditionKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> selectCondition(emptyList()));
  }

  @Test
  public void stringConditionWithoutValue() {
    final String crit = "id = 1";
    final Condition condition = Conditions.stringCondition(crit);
    assertEquals(crit, condition.getWhereClause());
    assertEquals(0, condition.getProperties().size());
    assertEquals(0, condition.getValues().size());
  }

  @Test
  public void stringConditionWithValue() {
    final String crit = "id = ?";
    final Condition condition = Conditions.stringCondition(crit, singletonList(1),
            singletonList(DOMAIN.getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_ID)));
    assertEquals(crit, condition.getWhereClause());
    assertEquals(1, condition.getProperties().size());
    assertEquals(1, condition.getValues().size());
  }

  @Test
  public void stringConditionNullConditionString() {
    assertThrows(NullPointerException.class, () -> Conditions.stringCondition(null));
  }

  @Test
  public void stringConditionNullValues() {
    assertThrows(NullPointerException.class, () -> Conditions.stringCondition("some is null", null,
            emptyList()));
  }

  @Test
  public void stringConditionNullKeys() {
    assertThrows(NullPointerException.class, () -> Conditions.stringCondition("some is null", emptyList(), null));
  }

  @Test
  public void serialization() throws IOException, ClassNotFoundException {
    final Property.ColumnProperty id = DOMAIN.getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_ID);
    final Condition condition = Conditions.conditionSet(Conjunction.AND,
            Conditions.stringCondition("test", asList("val1", "val2"), asList(id, id)),
            Conditions.stringCondition("testing", asList("val1", "val2"), asList(id, id)));
    deserialize(serialize(condition));
  }

  @Test
  public void stringConditionTest() {
    final EntitySelectCondition condition = selectCondition(TestDomain.T_DEPARTMENT,
            Conditions.stringCondition("department name is not null"))
            .setOrderBy(Domain.orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getProperties().isEmpty());
    final Entity.OrderBy.OrderByProperty deptNameOrder = condition.getOrderBy().getOrderByProperties().get(0);
    assertEquals(deptNameOrder.getPropertyId(), TestDomain.DEPARTMENT_NAME);
    assertFalse(deptNameOrder.isDescending());
  }

  @Test
  public void selectAllCondition() {
    final EntitySelectCondition selectCondition = selectCondition(TestDomain.T_DEPARTMENT);
    assertTrue(selectCondition.getValues().isEmpty());
    assertTrue(selectCondition.getProperties().isEmpty());

    final EntityCondition condition = condition(TestDomain.T_DEPARTMENT);
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getProperties().isEmpty());
  }

  @Test
  public void selectConditionOrderBy() {
    final EntitySelectCondition condition = selectCondition(TestDomain.T_EMP)
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
    assertThrows(IllegalArgumentException.class, () -> selectCondition(TestDomain.T_EMP)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.EMP_NAME).descending(TestDomain.EMP_NAME)));
  }

  @Test
  public void propertyConditionWithNonColumnProperty() {
    assertThrows(IllegalArgumentException.class, () -> ENTITY_CONDITIONS.propertyCondition(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_LOCATION, ConditionType.LIKE, null));
  }

  @Test
  public void selectConditionOrderBySamePropertyId() {
    assertThrows(IllegalArgumentException.class, () -> selectCondition(TestDomain.T_EMP)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.EMP_DEPARTMENT).descending(TestDomain.EMP_DEPARTMENT)));
  }

  @Test
  public void selectConditionInvalidType() {
    assertThrows(IllegalArgumentException.class, () -> ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP, TestDomain.EMP_COMMISSION, ConditionType.LIKE, "test"));
  }

  private static void assertKeyCondition(final EntityCondition condition) {
    assertEquals(TestDomain.T_DEPARTMENT, condition.getEntityId());
    assertEquals("deptno = ?", condition.getWhereClause());
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getProperties().size());
    assertEquals(10, condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_ID, condition.getProperties().get(0).getPropertyId());
  }

  private static void assertCondition(final EntityCondition condition) {
    assertEquals(TestDomain.T_DEPARTMENT, condition.getEntityId());
    assertEquals("dname <> ?", condition.getWhereClause());
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getProperties().size());
    assertEquals("DEPT", condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_NAME, condition.getProperties().get(0).getPropertyId());
  }

  private static byte[] serialize(final Object obj) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ObjectOutputStream os = new ObjectOutputStream(out);
    os.writeObject(obj);

    return out.toByteArray();
  }

  private static Object deserialize(final byte[] data) throws IOException, ClassNotFoundException {
    return new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
  }
}
