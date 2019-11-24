/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.TestDomain;
import org.jminor.framework.domain.Domain;

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.jminor.framework.db.condition.Conditions.entitySelectCondition;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionsTest {

  @Test
  public void selectConditionKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> entitySelectCondition(emptyList()));
  }

  @Test
  public void test() {
    final Condition critOne = Conditions.propertyCondition(TestDomain.DEPARTMENT_LOCATION, ConditionType.LIKE, "New York");

    EntitySelectCondition condition = Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT, critOne).setOrderBy(
            Domain.orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertEquals(-1, condition.getFetchCount());

    condition = Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT).setFetchCount(10);
    assertEquals(10, condition.getFetchCount());
  }

  @Test
  public void customConditionTest() {
    final EntitySelectCondition condition = Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT,
            Conditions.customCondition(TestDomain.DEPARTMENT_NAME_NOT_NULL_CONDITION_ID))
            .setOrderBy(Domain.orderBy().ascending(TestDomain.DEPARTMENT_NAME));
    assertTrue(condition.getCondition().getValues().isEmpty());
    assertTrue(condition.getCondition().getPropertyIds().isEmpty());
  }

  @Test
  public void selectConditionOrderBySamePropertyId() {
    assertThrows(IllegalArgumentException.class, () -> entitySelectCondition(TestDomain.T_EMP)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.EMP_DEPARTMENT).descending(TestDomain.EMP_DEPARTMENT)));
  }
}
