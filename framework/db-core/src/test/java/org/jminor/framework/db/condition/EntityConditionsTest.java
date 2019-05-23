/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
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
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class EntityConditionsTest {

  private static final TestDomain DOMAIN = new TestDomain();
  private static final EntityConditions entityConditions = new EntityConditions(DOMAIN);

  @Test
  public void test() {
    final Condition.Set<Property.ColumnProperty> set1 = Conditions.conditionSet(
            Conjunction.AND,
            entityConditions.propertyCondition(Properties.columnProperty("stringProperty", Types.VARCHAR), Condition.Type.LIKE, "value"),
            entityConditions.propertyCondition(Properties.columnProperty("intProperty", Types.INTEGER), Condition.Type.LIKE, 666)
    );
    final EntityCondition condition = entityConditions.condition("entityId", set1);
    assertEquals("(stringProperty = ? and intProperty = ?)", condition.getWhereClause());
    assertEquals(set1, condition.getCondition());
    final Condition.Set<Property.ColumnProperty> set2 = Conditions.conditionSet(
            Conjunction.AND,
            entityConditions.propertyCondition(Properties.columnProperty("doubleProperty", Types.DOUBLE), Condition.Type.LIKE, 666.666),
            entityConditions.propertyCondition(Properties.columnProperty("stringProperty2", Types.VARCHAR), Condition.Type.LIKE, false, "valu%e2")
    );
    final Condition.Set<Property.ColumnProperty> set3 = Conditions.conditionSet(Conjunction.OR, set1, set2);
    assertEquals("((stringProperty = ? and intProperty = ?) or (doubleProperty = ? and upper(stringProperty2) like upper(?)))",
            entityConditions.condition("entityId", set3).getWhereClause());
  }

  @Test
  public void condition() {
    final Entity entity = DOMAIN.entity(TestDomain.T_DEPARTMENT);
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
    final Entity entity = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    EntitySelectCondition condition = entityConditions.selectCondition(entity.getKey());
    assertKeyCondition(condition);

    condition = entityConditions.selectCondition(Collections.singletonList(entity.getKey()));
    assertKeyCondition(condition);

    condition = entityConditions.selectCondition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, Condition.Type.NOT_LIKE, "DEPT");
    assertCondition(condition);

    final Condition<Property.ColumnProperty> critOne = entityConditions.propertyCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_LOCATION, Condition.Type.LIKE, "New York");

    condition = entityConditions.selectCondition(TestDomain.T_DEPARTMENT, critOne).setOrderBy(
            Domain.orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertEquals(-1, condition.getFetchCount());

    condition = entityConditions.selectCondition(TestDomain.T_DEPARTMENT, 10);
    assertEquals(10, condition.getFetchCount());
  }

  @Test
  public void propertyCondition() {
    final Condition<Property.ColumnProperty> critOne = entityConditions.propertyCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_LOCATION, Condition.Type.LIKE, true, "New York");
    assertEquals("loc = ?", critOne.getWhereClause());
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
    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    Condition<Property.ColumnProperty> condition = entityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, department);
    assertEquals("deptno = ?", condition.getWhereClause());

    final Entity department2 = DOMAIN.entity(TestDomain.T_DEPARTMENT);
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
    final Property.ForeignKeyProperty foreignKeyProperty = DOMAIN.getForeignKeyProperty(TestDomain.T_MASTER, TestDomain.MASTER_SUPER_FK);
    final Condition<Property.ColumnProperty> condition = entityConditions.foreignKeyCondition(foreignKeyProperty,
            Condition.Type.LIKE, Collections.singletonList(null));
    assertEquals("super_id is null", condition.getWhereClause());
  }

  @Test
  public void foreignKeyConditionEntityKey() {
    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final Condition<Property.ColumnProperty> condition = entityConditions.foreignKeyCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, department.getKey());
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
    final Entity master1 = DOMAIN.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, 1);
    master1.put(TestDomain.MASTER_ID_2, 2);

    final Entity master2 = DOMAIN.entity(TestDomain.T_MASTER);
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

    final Entity.Key master1 = DOMAIN.key(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, null);
    master1.put(TestDomain.MASTER_ID_2, null);

    condition = entityConditions.selectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, Condition.Type.LIKE, master1);
    assertEquals("(master_id is null and master_id_2 is null)", condition.getWhereClause());

    master1.put(TestDomain.MASTER_ID_2, 1);
    condition = entityConditions.selectCondition(TestDomain.T_DETAIL,
            TestDomain.DETAIL_MASTER_FK, Condition.Type.LIKE, master1);
    assertEquals("(master_id is null and master_id_2 = ?)", condition.getWhereClause());

    final Entity.Key deptKey = DOMAIN.key(TestDomain.T_DEPARTMENT);
    deptKey.put(TestDomain.DEPARTMENT_ID, 42);

    condition = entityConditions.selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, deptKey);
    assertEquals("deptno = ?", condition.getWhereClause());
  }

  @Test
  public void selectConditionKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> entityConditions.selectCondition(Collections.emptyList()));
  }

  @Test
  public void stringConditionWithoutValue() {
    final String crit = "id = 1";
    final Condition<Property.ColumnProperty> condition = entityConditions.stringCondition(crit);
    assertEquals(crit, condition.getWhereClause());
    assertEquals(0, condition.getColumns().size());
    assertEquals(0, condition.getValues().size());
  }

  @Test
  public void stringConditionWithValue() {
    final String crit = "id = ?";
    final Condition<Property.ColumnProperty> condition = entityConditions.stringCondition(crit, Collections.singletonList(1),
            Collections.singletonList(DOMAIN.getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_ID)));
    assertEquals(crit, condition.getWhereClause());
    assertEquals(1, condition.getColumns().size());
    assertEquals(1, condition.getValues().size());
  }

  @Test
  public void stringConditionNullConditionString() {
    assertThrows(NullPointerException.class, () -> entityConditions.stringCondition(null));
  }

  @Test
  public void stringConditionNullValues() {
    assertThrows(NullPointerException.class, () -> entityConditions.stringCondition("some is null", null,
            Collections.emptyList()));
  }

  @Test
  public void stringConditionNullKeys() {
    assertThrows(NullPointerException.class, () -> entityConditions.stringCondition("some is null", Collections.emptyList(), null));
  }

  @Test
  public void serialization() throws IOException, ClassNotFoundException {
    final Property.ColumnProperty id = DOMAIN.getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_ID);
    final Condition<Property.ColumnProperty> condition = Conditions.conditionSet(Conjunction.AND,
            entityConditions.stringCondition("test", Arrays.asList("val1", "val2"), Arrays.asList(id, id)),
            entityConditions.stringCondition("testing", Arrays.asList("val1", "val2"), Arrays.asList(id, id)));
    deserialize(serialize(condition));
  }

  @Test
  public void stringCondition() {
    final EntitySelectCondition condition = entityConditions.selectCondition(TestDomain.T_DEPARTMENT,
            entityConditions.stringCondition("department name is not null"), -1)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getColumns().isEmpty());
    final Entity.OrderBy.OrderByProperty deptNameOrder = condition.getOrderBy().getOrderByProperties().get(0);
    assertEquals(deptNameOrder.getPropertyId(), TestDomain.DEPARTMENT_NAME);
    assertFalse(deptNameOrder.isDescending());
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
    assertThrows(IllegalArgumentException.class, () -> entityConditions.selectCondition(TestDomain.T_EMP)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.EMP_NAME).descending(TestDomain.EMP_NAME)));
  }

  @Test
  public void propertyConditionWithNonColumnProperty() {
    assertThrows(IllegalArgumentException.class, () -> entityConditions.propertyCondition(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_LOCATION, Condition.Type.LIKE, null));
  }

  @Test
  public void selectConditionOrderBySamePropertyId() {
    assertThrows(IllegalArgumentException.class, () -> entityConditions.selectCondition(TestDomain.T_EMP)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.EMP_DEPARTMENT).descending(TestDomain.EMP_DEPARTMENT)));
  }

  @Test
  public void selectConditionInvalidType() {
    assertThrows(IllegalArgumentException.class, () -> entityConditions.selectCondition(TestDomain.T_EMP, TestDomain.EMP_COMMISSION, Condition.Type.LIKE, "test"));
  }

  private void assertKeyCondition(final EntityCondition condition) {
    assertEquals(TestDomain.T_DEPARTMENT, condition.getEntityId());
    assertEquals("deptno = ?", condition.getWhereClause());
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getColumns().size());
    final Object val = condition.getValues().get(0);
    assertEquals(10, condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_ID, condition.getColumns().get(0).getPropertyId());
  }

  private void assertCondition(final EntityCondition condition) {
    assertEquals(TestDomain.T_DEPARTMENT, condition.getEntityId());
    assertEquals("dname <> ?", condition.getWhereClause());
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getColumns().size());
    assertEquals("DEPT", condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_NAME, condition.getColumns().get(0).getPropertyId());
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
