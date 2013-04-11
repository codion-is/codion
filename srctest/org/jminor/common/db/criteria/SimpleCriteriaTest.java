package org.jminor.common.db.criteria;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class SimpleCriteriaTest {

  @Test
  public void test() {
    final String crit = "id = 1";
    final SimpleCriteria<Object> criteria = new SimpleCriteria<Object>(crit);
    assertEquals(crit, criteria.getWhereClause());
    assertEquals(0, criteria.getValueKeys().size());
    assertEquals(0, criteria.getValues().size());
  }

  @Test (expected = IllegalArgumentException.class)
  public void nullCriteriaString() {
    new SimpleCriteria(null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void nullValues() {
    new SimpleCriteria<String>("some is null", null, Collections.<String>emptyList());
  }

  @Test (expected = IllegalArgumentException.class)
  public void nullKeys() {
    new SimpleCriteria<String>("some is null", Collections.emptyList(), null);
  }
}
