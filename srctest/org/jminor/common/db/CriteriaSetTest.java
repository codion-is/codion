/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import junit.framework.TestCase;

public class CriteriaSetTest extends TestCase {

  public void test() throws Exception {
    final CriteriaSet andSet = new CriteriaSet(CriteriaSet.Conjunction.AND,
            new Criteria(), new Criteria());
    assertEquals("AND criteria set should be working", "(criteria and criteria)", andSet.toString());

    final CriteriaSet orSet = new CriteriaSet(CriteriaSet.Conjunction.OR,
            new Criteria(), new Criteria());
    assertEquals("OR criteria set should be working", "(criteria or criteria)", orSet.toString());

    final CriteriaSet andOrAndSet = new CriteriaSet(CriteriaSet.Conjunction.AND, andSet, orSet);
    assertEquals("AND OR AND critera set should be working", "((criteria and criteria) and (criteria or criteria))", andOrAndSet.toString());

    final CriteriaSet andOrOrSet = new CriteriaSet(CriteriaSet.Conjunction.OR, andSet, orSet);
    assertEquals("AND OR OR critera set should be working", "((criteria and criteria) or (criteria or criteria))", andOrOrSet.toString());
  }

  private static class Criteria implements org.jminor.common.db.Criteria {
    @Override
    public String toString() {
      return "criteria";
    }
  }
}
