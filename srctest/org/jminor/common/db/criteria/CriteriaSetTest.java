/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.model.Conjunction;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class CriteriaSetTest {

  @Test
  public void test() throws Exception {
    final CriteriaSet<Object> andSet = new CriteriaSet<Object>(Conjunction.AND, new TestCriteria(), new TestCriteria());
    assertEquals("AND criteria set should be working", "(criteria and criteria)", andSet.getWhereClause());
    assertEquals(2, andSet.getCriteriaCount());

    final CriteriaSet<Object> orSet = new CriteriaSet<Object>(Conjunction.OR, new TestCriteria(), new TestCriteria());
    assertEquals("OR criteria set should be working", "(criteria or criteria)", orSet.getWhereClause());

    final List<Object> values = orSet.getValues();
    assertEquals(2, values.size());
    final List<Object> keys = orSet.getValueKeys();
    assertEquals(2, keys.size());

    final CriteriaSet<Object> andOrAndSet = new CriteriaSet<>(Conjunction.AND, andSet, orSet);
    assertEquals("AND OR AND criteria set should be working", "((criteria and criteria) and (criteria or criteria))", andOrAndSet.getWhereClause());

    final CriteriaSet<Object> andOrOrSet = new CriteriaSet<>(Conjunction.OR, andSet, orSet);
    assertEquals("AND OR OR criteria set should be working", "((criteria and criteria) or (criteria or criteria))", andOrOrSet.getWhereClause());

    CriteriaSet<Object> set = new CriteriaSet<>(Conjunction.OR);
    assertEquals(0, set.getCriteriaCount());

    set = new CriteriaSet<Object>(Conjunction.OR, new TestCriteria(), null, null);
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
    public List<Object> getValues() {
      final List<Object> values =  new ArrayList<>();
      values.add(1);
      return values;
    }

    @Override
    public List<?> getValueKeys() {
      final List<Object> keys =  new ArrayList<>();
      keys.add("key");
      return keys;
    }
  }
}
