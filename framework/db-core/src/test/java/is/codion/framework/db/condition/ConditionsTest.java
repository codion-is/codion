/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.db.TestDomain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ColumnProperty;

import org.junit.jupiter.api.Test;

import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionsTest {

  private static final Entities ENTITIES = new TestDomain().getEntities();

  @Test
  public void selectConditionKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(emptyList()));
  }

  @Test
  public void selectCondition() {
    SelectCondition condition = Conditions.condition(TestDomain.DEPARTMENT_LOCATION).equalTo("New York")
            .select().orderBy(orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertEquals(-1, condition.getFetchCount());

    condition = Conditions.condition(TestDomain.T_DEPARTMENT).select().fetchCount(10);
    assertEquals(10, condition.getFetchCount());
  }

  @Test
  public void customConditionTest() {
    final SelectCondition condition = Conditions.customCondition(TestDomain.DEPARTMENT_NAME_NOT_NULL_CONDITION_ID)
            .select().orderBy(orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getAttributes().isEmpty());
  }

  @Test
  public void selectConditionOrderBySameAttribute() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.T_EMP).select()
            .orderBy(orderBy().ascending(TestDomain.EMP_DEPARTMENT).descending(TestDomain.EMP_DEPARTMENT)));
  }

  @Test
  public void updateConditionDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.T_EMP).update()
            .set(TestDomain.EMP_COMMISSION, 123d)
            .set(TestDomain.EMP_COMMISSION, 123d));
  }

  @Test
  public void combinationEmpty() {
    final Condition.Combination combination = Conditions.combination(Conjunction.AND,
            Conditions.condition(TestDomain.T_EMP),
            Conditions.condition(TestDomain.EMP_ID).equalTo(1));
    assertEquals("(empno = ?)", combination.getWhereClause(ENTITIES.getDefinition(TestDomain.T_EMP)));
  }

  @Test
  public void foreignKeyCondition() {
    final Entity master = ENTITIES.entity(TestDomain.T_MASTER);
    master.put(TestDomain.MASTER_ID_1, 1);
    master.put(TestDomain.MASTER_ID_2, 2);
    master.put(TestDomain.MASTER_CODE, 3);
    final Condition condition = Conditions.condition(TestDomain.DETAIL_MASTER_FK).equalTo(master);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.getWhereClause(ENTITIES.getDefinition(TestDomain.T_DETAIL)));
    final Condition condition2 = Conditions.condition(TestDomain.DETAIL_MASTER_VIA_CODE_FK).equalTo(master);
    assertEquals("master_code = ?", condition2.getWhereClause(ENTITIES.getDefinition(TestDomain.T_DETAIL)));
  }

  @Test
  public void combination() {
    final Condition.Combination combination1 = Conditions.condition(TestDomain.DETAIL_STRING).equalTo("value")
            .and(Conditions.condition(TestDomain.DETAIL_INT).equalTo(666));
    final EntityDefinition detailDefinition = ENTITIES.getDefinition(TestDomain.T_DETAIL);
    assertEquals("(string = ? and int = ?)", combination1.getWhereClause(detailDefinition));
    final Condition.Combination combination2 = Conditions.condition(TestDomain.DETAIL_DOUBLE).equalTo(666.666)
            .and(Conditions.condition(TestDomain.DETAIL_STRING).equalTo("valu%e2").caseSensitive(false));
    final Condition.Combination combination3 = combination1.or(combination2);
    assertEquals("((string = ? and int = ?) or (double = ? and upper(string) like upper(?)))",
            combination3.getWhereClause(detailDefinition));
  }

  @Test
  public void attributeConditionTest() {
    final Condition critOne = Conditions.condition(TestDomain.DEPARTMENT_LOCATION).equalTo("New York");
    assertEquals("loc = ?", critOne.getWhereClause(ENTITIES.getDefinition(TestDomain.T_DEPARTMENT)));
    assertNotNull(critOne);
  }

  @Test
  public void foreignKeyConditionNull() {
    final EntityDefinition definition = ENTITIES.getDefinition(TestDomain.T_EMP);
    Condition condition = Conditions.condition(TestDomain.EMP_DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.getWhereClause(definition));

    condition = Conditions.condition(TestDomain.EMP_DEPARTMENT_FK).isNotNull();
    assertEquals("deptno is not null", condition.getWhereClause(definition));
  }

  @Test
  public void foreignKeyConditionEntity() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final EntityDefinition empDefinition = ENTITIES.getDefinition(TestDomain.T_EMP);
    Condition condition = Conditions.condition(TestDomain.EMP_DEPARTMENT_FK).equalTo(department);
    assertEquals("deptno = ?", condition.getWhereClause(empDefinition));

    final Entity department2 = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department2.put(TestDomain.DEPARTMENT_ID, 11);
    condition = Conditions.condition(TestDomain.EMP_DEPARTMENT_FK).equalTo(asList(department, department2));
    assertEquals("deptno in (?, ?)", condition.getWhereClause(empDefinition));

    condition = Conditions.condition(TestDomain.EMP_DEPARTMENT_FK).notEqualTo(asList(department, department2));
    assertEquals("deptno not in (?, ?)", condition.getWhereClause(empDefinition));
  }

  @Test
  public void foreignKeyConditionEntityKey() {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final EntityDefinition empDefinition = ENTITIES.getDefinition(TestDomain.T_EMP);
    final Condition condition = Conditions.condition(TestDomain.EMP_DEPARTMENT_FK).equalTo(department);
    assertEquals("deptno = ?", condition.getWhereClause(empDefinition));
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
    Condition condition = Conditions.condition(TestDomain.DETAIL_MASTER_FK).equalTo(master1);
    assertEquals("(master_id = ? and master_id_2 = ?)", condition.getWhereClause(detailDefinition));

    condition = Conditions.condition(TestDomain.DETAIL_MASTER_FK).notEqualTo(master1);
    assertEquals("(master_id <> ? and master_id_2 <> ?)", condition.getWhereClause(detailDefinition));

    condition = Conditions.condition(TestDomain.DETAIL_MASTER_FK).equalTo(asList(master1, master2));
    assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", condition.getWhereClause(detailDefinition));

    condition = Conditions.condition(TestDomain.DETAIL_MASTER_FK).notEqualTo(asList(master1, master2));
    assertEquals("((master_id <> ? and master_id_2 <> ?) or (master_id <> ? and master_id_2 <> ?))", condition.getWhereClause(detailDefinition));
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
    Condition condition = Conditions.condition(master1.getPrimaryKey());
    assertEquals("(id = ? and id2 = ?)", condition.getWhereClause(masterDefinition));

    condition = Conditions.condition(asList(master1.getPrimaryKey(), master2.getPrimaryKey()));
    assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", condition.getWhereClause(masterDefinition));
  }

  @Test
  public void keyNullCondition() {
    final EntityDefinition empDefinition = ENTITIES.getDefinition(TestDomain.T_EMP);
    Condition condition = Conditions.condition(TestDomain.EMP_DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.getWhereClause(empDefinition));

    condition = Conditions.condition(TestDomain.EMP_DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", condition.getWhereClause(empDefinition));

    condition = Conditions.condition(TestDomain.EMP_DEPARTMENT_FK).isNotNull();
    assertEquals("deptno is not null", condition.getWhereClause(empDefinition));

    final Entity master1 = ENTITIES.entity(TestDomain.T_MASTER);
    master1.put(TestDomain.MASTER_ID_1, null);
    master1.put(TestDomain.MASTER_ID_2, null);

    final EntityDefinition detailDefinition = ENTITIES.getDefinition(TestDomain.T_DETAIL);
    condition = Conditions.condition(TestDomain.DETAIL_MASTER_FK).equalTo(master1);
    assertEquals("(master_id is null and master_id_2 is null)", condition.getWhereClause(detailDefinition));

    master1.put(TestDomain.MASTER_ID_2, 1);
    condition = Conditions.condition(TestDomain.DETAIL_MASTER_FK).equalTo(master1);
    assertEquals("(master_id is null and master_id_2 = ?)", condition.getWhereClause(detailDefinition));

    final Entity dept = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 42);

    condition = Conditions.condition(TestDomain.EMP_DEPARTMENT_FK).equalTo(dept);
    assertEquals("deptno = ?", condition.getWhereClause(empDefinition));
  }

  @Test
  public void conditionTest() {
    final Entity entity = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    final EntityDefinition deptDefinition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);

    Condition condition = Conditions.condition(entity.getPrimaryKey());
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = Conditions.condition(entity.getPrimaryKey());
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).notEqualTo("DEPT");
    assertDepartmentCondition(condition, deptDefinition);
  }

  @Test
  public void selectConditionTest() {
    final Entity entity = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    final EntityDefinition deptDefinition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);

    Condition condition = Conditions.condition(entity.getPrimaryKey());
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = Conditions.condition(singletonList(entity.getPrimaryKey()));
    assertDepartmentKeyCondition(condition, deptDefinition);

    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).notEqualTo("DEPT");
    assertDepartmentCondition(condition, deptDefinition);
  }

  @Test
  public void selectAllCondition() {
    final Condition selectCondition = Conditions.condition(TestDomain.T_DEPARTMENT);
    assertTrue(selectCondition.getValues().isEmpty());
    assertTrue(selectCondition.getAttributes().isEmpty());

    final Condition condition = Conditions.condition(TestDomain.T_DEPARTMENT);
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getAttributes().isEmpty());
  }

  @Test
  public void selectConditionOrderByDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.T_EMP).select()
            .orderBy(orderBy().ascending(TestDomain.EMP_NAME).descending(TestDomain.EMP_NAME)));
  }

  @Test
  public void attributeConditionWithNonColumnProperty() {
    final EntityDefinition definition = ENTITIES.getDefinition(TestDomain.T_EMP);
    assertThrows(IllegalArgumentException.class, () ->
            Conditions.condition(TestDomain.EMP_DEPARTMENT_LOCATION).isNull().getWhereClause(definition));
  }

  @Test
  public void conditionNullOrEmptyValues() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.DEPARTMENT_NAME).equalTo((String) null));
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.DEPARTMENT_NAME).equalTo((String[]) null));
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.DEPARTMENT_NAME).equalTo(emptyList()));

    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.DEPARTMENT_NAME).notEqualTo((String) null));
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.DEPARTMENT_NAME).notEqualTo((String[]) null));
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.DEPARTMENT_NAME).notEqualTo(emptyList()));
  }

  @Test
  public void whereClause() throws Exception {
    final EntityDefinition departmentDefinition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);
    final ColumnProperty<?> property = (ColumnProperty<?>) departmentDefinition.getProperty(TestDomain.DEPARTMENT_NAME);
    Condition condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).equalTo("upper%");
    assertEquals(property.getColumnName() + " like ?", condition.getWhereClause(departmentDefinition));
    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).equalTo("upper");
    assertEquals(property.getColumnName() + " = ?", condition.getWhereClause(departmentDefinition));
    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).isNull();
    assertEquals(property.getColumnName() + " is null", condition.getWhereClause(departmentDefinition));

    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).notEqualTo("upper%");
    assertEquals(property.getColumnName() + " not like ?", condition.getWhereClause(departmentDefinition));
    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).notEqualTo("upper");
    assertEquals(property.getColumnName() + " <> ?", condition.getWhereClause(departmentDefinition));
    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).isNotNull();
    assertEquals(property.getColumnName() + " is not null", condition.getWhereClause(departmentDefinition));

    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).greaterThan("upper");
    assertEquals(property.getColumnName() + " > ?", condition.getWhereClause(departmentDefinition));
    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).greaterThanOrEqualTo("upper");
    assertEquals(property.getColumnName() + " >= ?", condition.getWhereClause(departmentDefinition));
    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).lessThan("upper");
    assertEquals(property.getColumnName() + " < ?", condition.getWhereClause(departmentDefinition));
    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).lessThanOrEqualTo("upper");
    assertEquals(property.getColumnName() + " <= ?", condition.getWhereClause(departmentDefinition));

    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).betweenExclusive("upper", "lower");
    assertEquals("(" + property.getColumnName() + " > ? and " + property.getColumnName() + " < ?)", condition.getWhereClause(departmentDefinition));
    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).between("upper", "lower");
    assertEquals("(" + property.getColumnName() + " >= ? and " + property.getColumnName() + " <= ?)", condition.getWhereClause(departmentDefinition));

    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).notBetweenExclusive("upper", "lower");
    assertEquals("(" + property.getColumnName() + " < ? or " + property.getColumnName() + " > ?)", condition.getWhereClause(departmentDefinition));
    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).notBetween("upper", "lower");
    assertEquals("(" + property.getColumnName() + " <= ? or " + property.getColumnName() + " >= ?)", condition.getWhereClause(departmentDefinition));

    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).equalTo("%upper%");
    assertEquals(property.getColumnName() + " like ?", condition.getWhereClause(departmentDefinition));
    condition = Conditions.condition(TestDomain.DEPARTMENT_NAME).notEqualTo("%upper%");
    assertEquals(property.getColumnName() + " not like ?", condition.getWhereClause(departmentDefinition));
  }

  private static void assertDepartmentKeyCondition(final Condition condition, final EntityDefinition departmentDefinition) {
    assertEquals("deptno = ?", condition.getWhereClause(departmentDefinition));
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getAttributes().size());
    assertEquals(10, condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_ID, condition.getAttributes().get(0));
  }

  private static void assertDepartmentCondition(final Condition condition, final EntityDefinition departmentDefinition) {
    assertEquals("dname <> ?", condition.getWhereClause(departmentDefinition));
    assertEquals(1, condition.getValues().size());
    assertEquals(1, condition.getAttributes().size());
    assertEquals("DEPT", condition.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_NAME, condition.getAttributes().get(0));
  }
}
