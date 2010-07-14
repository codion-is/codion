package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.Types;

public class AbstractSearchModelTest {

  @Test
  public void test() throws Exception {
    final AbstractSearchModel<String> model = new AbstractSearchModel<String>("test", Types.VARCHAR, "%") {
      @Override
      public boolean include(final Object object) {
        return false;
      }
    };
    assertEquals("test", model.getSearchKey());

    model.setCaseSensitive(true);
    assertTrue(model.isCaseSensitive());
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
