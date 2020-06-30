/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.db.TestDomain;
import is.codion.framework.domain.entity.Entities;

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
            .selectCondition().setOrderBy(orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertEquals(-1, condition.getFetchCount());

    condition = Conditions.condition(TestDomain.T_DEPARTMENT).selectCondition().setFetchCount(10);
    assertEquals(10, condition.getFetchCount());
  }

  @Test
  public void customConditionTest() {
    final SelectCondition condition = Conditions.customCondition(TestDomain.DEPARTMENT_NAME_NOT_NULL_CONDITION_ID)
            .selectCondition().setOrderBy(orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertTrue(condition.getValues().isEmpty());
    assertTrue(condition.getAttributes().isEmpty());
  }

  @Test
  public void selectConditionOrderBySameAttribute() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.T_EMP).selectCondition()
            .setOrderBy(orderBy().ascending(TestDomain.EMP_DEPARTMENT).descending(TestDomain.EMP_DEPARTMENT)));
  }

  @Test
  public void updateConditionDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.condition(TestDomain.T_EMP).updateCondition()
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
}
