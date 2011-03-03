package org.jminor.common.db.criteria;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class SimpleCriteriaTest {

  @Test
  public void test() {
    final String crit = "id = 1";
    final SimpleCriteria<Object> criteria = new SimpleCriteria<Object>(crit, new ArrayList<Object>(), new ArrayList<Object>());
    assertEquals(crit, criteria.asString());
    assertEquals(0, criteria.getValueKeys().size());
    assertEquals(0, criteria.getValues().size());
    new SimpleCriteria("hello");
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
