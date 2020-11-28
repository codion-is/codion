/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.db.TestDomain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.Test;

import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionsTest {

  private static final Entities ENTITIES = new TestDomain().getEntities();

  @Test
  public void selectConditionKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(emptyList()));
  }

  @Test
  public void condition() {
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
    final AttributeCondition<Entity> condition = Conditions.condition(TestDomain.DETAIL_MASTER_FK).equalTo(master);

    //not expanded
    assertThrows(IllegalArgumentException.class, () -> condition.getWhereClause(ENTITIES.getDefinition(TestDomain.T_DETAIL)));

    WhereCondition whereCondition = Conditions.whereCondition(condition, ENTITIES.getDefinition(TestDomain.T_DETAIL));
    assertEquals("(master_id = ? and master_id_2 = ?)", whereCondition.getWhereClause());

    final AttributeCondition<Entity> condition2 = Conditions.condition(TestDomain.DETAIL_MASTER_VIA_CODE_FK).equalTo(master);
    whereCondition = Conditions.whereCondition(condition2, ENTITIES.getDefinition(TestDomain.T_DETAIL));
    assertEquals("master_code = ?", whereCondition.getWhereClause());
  }
}
