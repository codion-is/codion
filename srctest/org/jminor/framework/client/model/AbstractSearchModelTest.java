package org.jminor.framework.client.model;

import org.jminor.common.model.SearchType;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.*;
import org.junit.Test;

public class AbstractSearchModelTest {

  @Test
  public void test() throws Exception {
    new EmpDept();
    final Property property = EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME);
    final AbstractSearchModel model = new AbstractSearchModel(property) {
      @Override
      public boolean include(final Object object) {
        return false;
      }
    };
    assertEquals(property.getPropertyID(), model.getPropertyID());
    assertEquals(property.getCaption(), model.getCaption());
    assertEquals(property.getValueClass(), model.getProperty().getValueClass());

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
