/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class SimpleCriteriaTest {

  @Test
  public void withoutValue() {
    final String crit = "id = 1";
    final SimpleCriteria<Object> criteria = new SimpleCriteria<>(crit);
    assertEquals(crit, criteria.getWhereClause());
    assertEquals(0, criteria.getValueKeys().size());
    assertEquals(0, criteria.getValues().size());
  }

  @Test
  public void withValue() {
    final String crit = "id = ?";
    final SimpleCriteria criteria = new SimpleCriteria(crit, Collections.singletonList(1), Collections.singletonList("id"));
    assertEquals(crit, criteria.getWhereClause());
    assertEquals(1, criteria.getValueKeys().size());
    assertEquals(1, criteria.getValues().size());
  }

  @Test (expected = IllegalArgumentException.class)
  public void nullCriteriaString() {
    new SimpleCriteria(null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void nullValues() {
    new SimpleCriteria<>("some is null", null, Collections.<String>emptyList());
  }

  @Test (expected = IllegalArgumentException.class)
  public void nullKeys() {
    new SimpleCriteria<String>("some is null", Collections.emptyList(), null);
  }
}
