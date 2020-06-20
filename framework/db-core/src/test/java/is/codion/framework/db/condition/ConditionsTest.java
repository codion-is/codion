/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.db.TestDomain;

import org.junit.jupiter.api.Test;

import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionsTest {

  @Test
  public void selectConditionKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.selectCondition(emptyList()));
  }

  @Test
  public void test() {
    final Condition critOne = Conditions.attributeCondition(TestDomain.DEPARTMENT_LOCATION, Operator.EQUAL_TO, "New York");

    EntitySelectCondition condition = Conditions.selectCondition(TestDomain.T_DEPARTMENT, critOne).setOrderBy(
            orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertEquals(-1, condition.getFetchCount());

    condition = Conditions.selectCondition(TestDomain.T_DEPARTMENT).setFetchCount(10);
    assertEquals(10, condition.getFetchCount());
  }

  @Test
  public void customConditionTest() {
    final EntitySelectCondition condition = Conditions.selectCondition(TestDomain.T_DEPARTMENT,
            Conditions.customCondition(TestDomain.DEPARTMENT_NAME_NOT_NULL_CONDITION_ID))
            .setOrderBy(orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertTrue(condition.getCondition().getValues().isEmpty());
    assertTrue(condition.getCondition().getAttributes().isEmpty());
  }

  @Test
  public void selectConditionOrderBySameAttribute() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.selectCondition(TestDomain.T_EMP)
            .setOrderBy(orderBy().ascending(TestDomain.EMP_DEPARTMENT).descending(TestDomain.EMP_DEPARTMENT)));
  }

  @Test
  public void updateConditionDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Conditions.updateCondition(TestDomain.T_EMP)
            .set(TestDomain.EMP_COMMISSION, 123d)
            .set(TestDomain.EMP_COMMISSION, 123d));
  }
}
