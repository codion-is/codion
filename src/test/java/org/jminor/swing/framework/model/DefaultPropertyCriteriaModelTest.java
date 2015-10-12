/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.model.SearchType;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DefaultPropertyCriteriaModelTest {

  @Test
  public void propertyCriteriaModel() throws Exception {
    TestDomain.init();
    final Property.ColumnProperty property = (Property.ColumnProperty) Entities.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME);
    final PropertyCriteriaModel model = new DefaultPropertyCriteriaModel(property);
    assertEquals(property, model.getColumnIdentifier());
    model.setSearchType(SearchType.LIKE);
    assertFalse(model.isLowerBoundRequired());
    model.setUpperBound("upper");
    assertEquals(property.getPropertyID() + " like ?", model.getCriteria().getWhereClause());
    model.setSearchType(SearchType.NOT_LIKE);
    assertEquals(property.getPropertyID() + " not like ?", model.getCriteria().getWhereClause());
    model.setSearchType(SearchType.GREATER_THAN);
    assertEquals(property.getPropertyID() + " >= ?", model.getCriteria().getWhereClause());
    model.setSearchType(SearchType.LESS_THAN);
    assertEquals(property.getPropertyID() + " <= ?", model.getCriteria().getWhereClause());

    model.setSearchType(SearchType.WITHIN_RANGE);
    assertTrue(model.isLowerBoundRequired());
    model.setLowerBound("lower");
    List values = model.getCriteria().getValues();
    assertTrue(values.contains("upper"));
    assertTrue(values.contains("lower"));
    assertEquals("(" + property.getPropertyID() + " >= ? and " + property.getPropertyID() + " <= ?)", model.getCriteria().getWhereClause());

    model.setSearchType(SearchType.LIKE);
    model.setAutomaticWildcard(true);
    values = model.getCriteria().getValues();
    assertTrue(values.contains("%upper%"));
    assertEquals(property.getPropertyID() + " like ?", model.getCriteria().getWhereClause());
    model.setSearchType(SearchType.NOT_LIKE);
    assertEquals(property.getPropertyID() + " not like ?", model.getCriteria().getWhereClause());
  }
}
