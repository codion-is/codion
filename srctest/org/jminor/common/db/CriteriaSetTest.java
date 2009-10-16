/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import junit.framework.TestCase;

public class CriteriaSetTest extends TestCase {

  public void test() throws Exception {
    final CriteriaSet andSet = new CriteriaSet(CriteriaSet.Conjunction.AND,
            new Criteria(), new Criteria());
    assertEquals("AND criteria set should be working", "(criteria and criteria)", andSet.asString());

    final CriteriaSet orSet = new CriteriaSet(CriteriaSet.Conjunction.OR,
            new Criteria(), new Criteria());
    assertEquals("OR criteria set should be working", "(criteria or criteria)", orSet.asString());

    final CriteriaSet andOrAndSet = new CriteriaSet(CriteriaSet.Conjunction.AND, andSet, orSet);
    assertEquals("AND OR AND critera set should be working", "((criteria and criteria) and (criteria or criteria))", andOrAndSet.asString());

    final CriteriaSet andOrOrSet = new CriteriaSet(CriteriaSet.Conjunction.OR, andSet, orSet);
    assertEquals("AND OR OR critera set should be working", "((criteria and criteria) or (criteria or criteria))", andOrOrSet.asString());
  }

  private static class Criteria implements org.jminor.common.db.Criteria {
    public String asString() {
      return "criteria";
    }
  }
}
