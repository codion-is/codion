package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.Types;

public class DefaultSearchModelTest {

  @Test
  public void test() throws Exception {
    final DefaultSearchModel<String> model = new DefaultSearchModel<String>("test", Types.VARCHAR, "%");
    assertEquals("test", model.getSearchKey());
    
    model.setAutomaticWildcard(true);
    assertTrue(model.isAutomaticWildcard());

    model.setLocked(true);
    assertTrue(model.stateLocked().isActive());
    try {
      model.setUpperBound("test");
      fail("Should not be able to set upper bound in a locked search model");
    }
    catch (IllegalStateException e) {}
    try {
      model.setLowerBound("test");
      fail("Should not be able to set lower bound in a locked search model");
    }
    catch (IllegalStateException e) {}
    try {
      model.setSearchEnabled(true);
      fail("Should not be able to set search enabled in a locked search model");
    }
    catch (IllegalStateException e) {}
    try {
      model.setSearchType(SearchType.NOT_LIKE);
      fail("Should not be able to set search type in a locked search model");
    }
    catch (IllegalStateException e) {}
  }
}
