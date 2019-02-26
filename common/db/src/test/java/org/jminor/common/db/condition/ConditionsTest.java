/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.Column;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ConditionsTest {

  private static final Condition.Set<TestColumn> AND_SET = Conditions.conditionSet(Conjunction.AND, new TestCondition(), new TestCondition());
  private static final Condition.Set<TestColumn> OR_SET = Conditions.conditionSet(Conjunction.OR, new TestCondition(), new TestCondition());
  private static final Condition.Set<TestColumn> AND_OR_AND_SET = Conditions.conditionSet(Conjunction.AND, AND_SET, OR_SET);
  private static final Condition.Set<TestColumn> AND_OR_OR_SET = Conditions.conditionSet(Conjunction.OR, AND_SET, OR_SET);

  @Test
  public void andSet() {
    assertEquals("(condition and condition)", AND_SET.getWhereClause(), "AND condition set should be working");
    assertEquals(2, AND_SET.getConditionCount());
  }

  @Test
  public void orSet() {
    assertEquals("(condition or condition)", OR_SET.getWhereClause(), "OR condition set should be working");
    assertEquals(2, OR_SET.getValues().size());
    assertEquals(2, OR_SET.getColumns().size());
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
    Condition.Set<TestColumn> set = Conditions.conditionSet(Conjunction.OR);
    assertEquals(0, set.getConditionCount());
    assertEquals("", set.getWhereClause());

    set = Conditions.conditionSet(Conjunction.OR, new TestCondition());
    assertEquals(1, set.getConditionCount());

    set = Conditions.conditionSet(Conjunction.OR, new TestCondition(), new TestCondition(), null, new TestCondition());
    assertEquals(3, set.getConditionCount());
  }

  private static class TestCondition implements Condition<TestColumn> {
    private final TestColumn testColumn = new TestColumn();
    @Override
    public String getWhereClause() {
      return "condition";
    }

    @Override
    public List<?> getValues() {
      return Collections.singletonList(1);
    }

    @Override
    public List<TestColumn> getColumns() {
      return Collections.singletonList(testColumn);
    }
  }

  private static final class TestColumn implements Column, Serializable {
    @Override
    public String getColumnName() {return null;}
    @Override
    public int getType() {return 0;}
    @Override
    public boolean isUpdatable() {return false;}
    @Override
    public boolean isSearchable() {return false;}
    @Override
    public String getCaption() {return null;}
    @Override
    public String getDescription() {return null;}
    @Override
    public Class<?> getTypeClass() {return null;}
    @Override
    public void validateType(final Object value) {}
  };
}
