/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.SearchType;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.List;

/**
 * User: Bjorn Darri
 * Date: 29.7.2009
 * Time: 18:07:24
 */
public class PropertySearchModelTest {

  static {
    new EmpDept();
  }

  @Test
  public void propertySearchModel() throws Exception {
    final Property property = EntityRepository.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME);
    final PropertySearchModel model = new PropertySearchModel(property);
    assertEquals(property, model.getSearchKey());
    model.setSearchType(SearchType.LIKE);
    model.setUpperBound("upper");
    assertEquals(property.getPropertyID() + " like ?", model.getPropertyCriteria().asString());
    model.setSearchType(SearchType.NOT_LIKE);
    assertEquals(property.getPropertyID() + " not like ?", model.getPropertyCriteria().asString());
    model.setSearchType(SearchType.AT_MOST);
    assertEquals(property.getPropertyID() + " >= ?", model.getPropertyCriteria().asString());
    model.setSearchType(SearchType.AT_LEAST);
    assertEquals(property.getPropertyID() + " <= ?", model.getPropertyCriteria().asString());

    model.setSearchType(SearchType.WITHIN_RANGE);
    model.setLowerBound("lower");
    List<Object> values = model.getPropertyCriteria().getValues();
    assertTrue(values.contains("upper"));
    assertTrue(values.contains("lower"));
    assertEquals("(" + property.getPropertyID() + " >= ? and " + property.getPropertyID() + " <= ?)", model.getPropertyCriteria().asString());

    model.setSearchType(SearchType.LIKE);
    model.setAutomaticWildcard(true);
    values = model.getPropertyCriteria().getValues();
    assertTrue(values.contains("%upper%"));
    assertEquals(property.getPropertyID() + " like ?", model.getPropertyCriteria().asString());
    model.setSearchType(SearchType.NOT_LIKE);
    assertEquals(property.getPropertyID() + " not like ?", model.getPropertyCriteria().asString());
  }
}
