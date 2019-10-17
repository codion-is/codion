/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.framework.db.TestDomain;
import org.jminor.framework.domain.Property;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ConditionsTest {

  private static final Condition.Set AND_SET = Conditions.conditionSet(Conjunction.AND, new TestCondition(), new TestCondition());
  private static final Condition.Set OR_SET = Conditions.conditionSet(Conjunction.OR, new TestCondition(), new TestCondition());
  private static final Condition.Set AND_OR_AND_SET = Conditions.conditionSet(Conjunction.AND, AND_SET, OR_SET);
  private static final Condition.Set AND_OR_OR_SET = Conditions.conditionSet(Conjunction.OR, AND_SET, OR_SET);

  @Test
  public void andSet() {
    assertEquals("(condition and condition)", AND_SET.getWhereClause(), "AND condition set should be working");
    assertEquals(2, AND_SET.getConditionCount());
  }

  @Test
  public void orSet() {
    assertEquals("(condition or condition)", OR_SET.getWhereClause(), "OR condition set should be working");
    assertEquals(2, OR_SET.getValues().size());
    assertEquals(2, OR_SET.getProperties().size());
  }

  @Test
  public void andOrAndSet() {
    assertEquals("((condition and condition) and (condition or condition))", AND_OR_AND_SET.getWhereClause(), "AND OR AND condition set should be working");
  }

  @Test
  public void andOrOrSet() {
    assertEquals("((condition and condition) or (condition or condition))", AND_OR_OR_SET.getWhereClause(), "AND OR OR condition set should be working");
  }

  @Test
  public void getConditionCount() {
    Condition.Set set = Conditions.conditionSet(Conjunction.OR);
    assertEquals(0, set.getConditionCount());
    assertEquals("", set.getWhereClause());

    set = Conditions.conditionSet(Conjunction.OR, new TestCondition());
    assertEquals(1, set.getConditionCount());

    set = Conditions.conditionSet(Conjunction.OR, new TestCondition(), new TestCondition(), null, new TestCondition());
    assertEquals(3, set.getConditionCount());
  }

  private static class TestCondition implements Condition {
    private final TestDomain domain = new TestDomain();
    @Override
    public String getWhereClause() {
      return "condition";
    }

    @Override
    public List getValues() {
      return singletonList(1);
    }

    @Override
    public List<Property.ColumnProperty> getProperties() {
      return singletonList(domain.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID));
    }
  }
}
