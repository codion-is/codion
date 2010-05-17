/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class CriteriaSetTest {

  private static final Database DATABASE = DatabaseProvider.createInstance();

  @Test
  public void test() throws Exception {
    final CriteriaSet<Object> andSet = new CriteriaSet<Object>(CriteriaSet.Conjunction.AND,
            new Criteria(), new Criteria());
    assertEquals("AND criteria set should be working", "(criteria and criteria)", andSet.asString(DATABASE, null));

    final CriteriaSet<Object> orSet = new CriteriaSet<Object>(CriteriaSet.Conjunction.OR,
            new Criteria(), new Criteria());
    assertEquals("OR criteria set should be working", "(criteria or criteria)", orSet.asString(DATABASE, null));

    final CriteriaSet<Object> andOrAndSet = new CriteriaSet<Object>(CriteriaSet.Conjunction.AND, andSet, orSet);
    assertEquals("AND OR AND critera set should be working", "((criteria and criteria) and (criteria or criteria))", andOrAndSet.asString(DATABASE, null));

    final CriteriaSet<Object> andOrOrSet = new CriteriaSet<Object>(CriteriaSet.Conjunction.OR, andSet, orSet);
    assertEquals("AND OR OR critera set should be working", "((criteria and criteria) or (criteria or criteria))", andOrOrSet.asString(DATABASE, null));
  }

  private static class Criteria implements org.jminor.common.db.criteria.Criteria {
    public String asString(final Database database, final ValueProvider valueProvider) {
      return "criteria";
    }

    public List<?> getValues() {
      return null;
    }

    public List<?> getValueKeys() {
      return null;
    }
  }
}
