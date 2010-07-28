package org.jminor.common.db.criteria;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.ArrayList;

public class SimpleCriteriaTest {

  @Test
  public void test() {
    final String crit = "id = 1";
    final SimpleCriteria<Object> criteria = new SimpleCriteria<Object>(crit, new ArrayList<Object>(), new ArrayList<Object>());
    assertEquals(crit, criteria.asString());
    assertEquals(0, criteria.getValueKeys().size());
    assertEquals(0, criteria.getValues().size());
  }
}
