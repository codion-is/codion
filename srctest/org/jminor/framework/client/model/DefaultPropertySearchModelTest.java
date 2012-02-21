/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.SearchType;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DefaultPropertySearchModelTest {

  @Test
  public void propertySearchModel() throws Exception {
    EmpDept.init();
    final Property.ColumnProperty property = (Property.ColumnProperty) Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME);
    final PropertySearchModel model = new DefaultPropertySearchModel(property);
    assertEquals(property, model.getColumnIdentifier());
    model.setSearchType(SearchType.LIKE);
    assertFalse(model.isLowerBoundRequired());
    model.setUpperBound("upper");
    assertEquals(property.getPropertyID() + " like ?", model.getCriteria().asString());
    model.setSearchType(SearchType.NOT_LIKE);
    assertEquals(property.getPropertyID() + " not like ?", model.getCriteria().asString());
    model.setSearchType(SearchType.GREATER_THAN);
    assertEquals(property.getPropertyID() + " >= ?", model.getCriteria().asString());
    model.setSearchType(SearchType.LESS_THAN);
    assertEquals(property.getPropertyID() + " <= ?", model.getCriteria().asString());

    model.setSearchType(SearchType.WITHIN_RANGE);
    assertTrue(model.isLowerBoundRequired());
    model.setLowerBound("lower");
    List values = model.getCriteria().getValues();
    assertTrue(values.contains("upper"));
    assertTrue(values.contains("lower"));
    assertEquals("(" + property.getPropertyID() + " >= ? and " + property.getPropertyID() + " <= ?)", model.getCriteria().asString());

    model.setSearchType(SearchType.LIKE);
    model.setAutomaticWildcard(true);
    values = model.getCriteria().getValues();
    assertTrue(values.contains("%upper%"));
    assertEquals(property.getPropertyID() + " like ?", model.getCriteria().asString());
    model.setSearchType(SearchType.NOT_LIKE);
    assertEquals(property.getPropertyID() + " not like ?", model.getCriteria().asString());
  }
}
