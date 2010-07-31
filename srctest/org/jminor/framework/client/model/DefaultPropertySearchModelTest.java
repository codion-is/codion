/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.SearchType;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.List;

/**
 * User: Bjorn Darri<br>
 * Date: 29.7.2009<br>
 * Time: 18:07:24
 */
public class DefaultPropertySearchModelTest {

  static {
    new EmpDept();
  }

  @Test
  public void propertySearchModel() throws Exception {
    final Property.ColumnProperty property = (Property.ColumnProperty) Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME);
    final PropertySearchModel model = new DefaultPropertySearchModel(property);
    assertEquals(property, model.getSearchKey());
    model.setSearchType(SearchType.LIKE);
    model.setUpperBound("upper");
    assertEquals(property.getPropertyID() + " like ?", model.getCriteria().asString());
    model.setSearchType(SearchType.NOT_LIKE);
    assertEquals(property.getPropertyID() + " not like ?", model.getCriteria().asString());
    model.setSearchType(SearchType.AT_MOST);
    assertEquals(property.getPropertyID() + " >= ?", model.getCriteria().asString());
    model.setSearchType(SearchType.AT_LEAST);
    assertEquals(property.getPropertyID() + " <= ?", model.getCriteria().asString());

    model.setSearchType(SearchType.WITHIN_RANGE);
    model.setLowerBound("lower");
    List<Object> values = model.getCriteria().getValues();
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
