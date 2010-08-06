/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.model.Conjunction;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class CriteriaSetTest {

  @Test
  public void test() throws Exception {
    final CriteriaSet<Object> andSet = new CriteriaSet<Object>(Conjunction.AND,
            new Criteria(), new Criteria());
    assertEquals("AND criteria set should be working", "(criteria and criteria)", andSet.asString());
    assertEquals(2, andSet.getCriteriaCount());

    final CriteriaSet<Object> orSet = new CriteriaSet<Object>(Conjunction.OR,
            new Criteria(), new Criteria());
    assertEquals("OR criteria set should be working", "(criteria or criteria)", orSet.asString());

    final List<Object> values = orSet.getValues();
    assertEquals(2, values.size());
    final List<Object> keys = orSet.getValueKeys();
    assertEquals(2, keys.size());

    final CriteriaSet<Object> andOrAndSet = new CriteriaSet<Object>(Conjunction.AND, andSet, orSet);
    assertEquals("AND OR AND critera set should be working", "((criteria and criteria) and (criteria or criteria))", andOrAndSet.asString());

    final CriteriaSet<Object> andOrOrSet = new CriteriaSet<Object>(Conjunction.OR, andSet, orSet);
    assertEquals("AND OR OR critera set should be working", "((criteria and criteria) or (criteria or criteria))", andOrOrSet.asString());

    final CriteriaSet<Object> set = new CriteriaSet<Object>(Conjunction.OR);
    assertEquals(0, set.getCriteriaCount());
  }

  private static class Criteria implements org.jminor.common.db.criteria.Criteria {
    public String asString() {
      return "criteria";
    }

    public List<Object> getValues() {
      final List<Object> values =  new ArrayList<Object>();
      values.add(1);
      return values;
    }

    public List<?> getValueKeys() {
      final List<Object> keys =  new ArrayList<Object>();
      keys.add("key");
      return keys;
    }
  }
}
