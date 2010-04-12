/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.framework.db.criteria.CriteriaUtil;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class CriteriaSetTest {

  private static final Database database = DatabaseProvider.createInstance();
  private static final CriteriaValueProvider valueProvider = CriteriaUtil.getCriteriaValueProvider();

  @Test
  public void test() throws Exception {
    final CriteriaSet andSet = new CriteriaSet(CriteriaSet.Conjunction.AND,
            new Criteria(), new Criteria());
    assertEquals("AND criteria set should be working", "(criteria and criteria)", andSet.asString(database, valueProvider));

    final CriteriaSet orSet = new CriteriaSet(CriteriaSet.Conjunction.OR,
            new Criteria(), new Criteria());
    assertEquals("OR criteria set should be working", "(criteria or criteria)", orSet.asString(database, valueProvider));

    final CriteriaSet andOrAndSet = new CriteriaSet(CriteriaSet.Conjunction.AND, andSet, orSet);
    assertEquals("AND OR AND critera set should be working", "((criteria and criteria) and (criteria or criteria))", andOrAndSet.asString(database, valueProvider));

    final CriteriaSet andOrOrSet = new CriteriaSet(CriteriaSet.Conjunction.OR, andSet, orSet);
    assertEquals("AND OR OR critera set should be working", "((criteria and criteria) or (criteria or criteria))", andOrOrSet.asString(database, valueProvider));
  }

  private static class Criteria implements org.jminor.common.db.criteria.Criteria {
    public String asString(final Database database, final CriteriaValueProvider valueProvider) {
      return "criteria";
    }
  }
}
