/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.model.Conjunction;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class CriteriaSetTest {

  private static final CriteriaSet AND_SET = new CriteriaSet(Conjunction.AND, new TestCriteria(), new TestCriteria());
  private static final CriteriaSet<Object> OR_SET = new CriteriaSet<Object>(Conjunction.OR, new TestCriteria(), new TestCriteria());
  private static final CriteriaSet<Object> AND_OR_AND_SET = new CriteriaSet<>(Conjunction.AND, AND_SET, OR_SET);
  private static final CriteriaSet<Object> AND_OR_OR_SET = new CriteriaSet<>(Conjunction.OR, AND_SET, OR_SET);

  @Test
  public void andSet() {
    assertEquals("AND criteria set should be working", "(criteria and criteria)", AND_SET.getWhereClause());
    assertEquals(2, AND_SET.getCriteriaCount());
  }

  @Test
  public void orSet() {
    assertEquals("OR criteria set should be working", "(criteria or criteria)", OR_SET.getWhereClause());
    assertEquals(2, OR_SET.getValues().size());
    assertEquals(2, OR_SET.getValueKeys().size());
  }

  @Test
  public void andOrAndSet() {
    assertEquals("AND OR AND criteria set should be working", "((criteria and criteria) and (criteria or criteria))", AND_OR_AND_SET.getWhereClause());
  }

  @Test
  public void andOrOrSet() {
    assertEquals("AND OR OR criteria set should be working", "((criteria and criteria) or (criteria or criteria))", AND_OR_OR_SET.getWhereClause());
  }

  @Test
  public void getCriteriaCount() {
    CriteriaSet<Object> set = new CriteriaSet<>(Conjunction.OR);
    assertEquals(0, set.getCriteriaCount());
    assertEquals("", set.getWhereClause());

    set = new CriteriaSet<Object>(Conjunction.OR, new TestCriteria());
    assertEquals(1, set.getCriteriaCount());

    set = new CriteriaSet<Object>(Conjunction.OR, new TestCriteria(), new TestCriteria(), null, new TestCriteria());
    assertEquals(3, set.getCriteriaCount());
  }

  private static class TestCriteria implements Criteria {
    @Override
    public String getWhereClause() {
      return "criteria";
    }

    @Override
    public List<?> getValues() {
      return Collections.singletonList(1);
    }

    @Override
    public List<?> getValueKeys() {
      return Collections.singletonList("key");
    }
  }
}
