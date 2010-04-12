/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.CriteriaValueProvider;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.SearchType;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.CriteriaUtil;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * User: Bjorn Darri
 * Date: 29.7.2009
 * Time: 18:07:24
 */
public class PropertySearchModelTest {

  private static final Database database = DatabaseProvider.createInstance();
  private static final CriteriaValueProvider valueProvider = CriteriaUtil.getCriteriaValueProvider();

  static {
    new EmpDept();
  }

  @Test
  public void propertySearchModel() throws Exception {
    final Property property = EntityRepository.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME);
    final PropertySearchModel model = new PropertySearchModel(property);
    assertEquals(property, model.getProperty());
    model.setSearchType(SearchType.LIKE);
    model.setUpperBound("upper");
    assertEquals(property.getPropertyID() + " = '" + "upper'", model.getPropertyCriteria().asString(database, valueProvider));
    model.setSearchType(SearchType.NOT_LIKE);
    assertEquals(property.getPropertyID() + " <> '" + "upper'", model.getPropertyCriteria().asString(database, valueProvider));
    model.setSearchType(SearchType.AT_MOST);
    assertEquals(property.getPropertyID() + " >= '" + "upper'", model.getPropertyCriteria().asString(database, valueProvider));
    model.setSearchType(SearchType.AT_LEAST);
    assertEquals(property.getPropertyID() + " <= '" + "upper'", model.getPropertyCriteria().asString(database, valueProvider));

    model.setSearchType(SearchType.WITHIN_RANGE);
    model.setLowerBound("lower");
    assertEquals("(" + property.getPropertyID() + " >= '" + "lower' and "
            + property.getPropertyID() + " <= '" + "upper')", model.getPropertyCriteria().asString(database, valueProvider));

    model.setSearchType(SearchType.LIKE);
    model.setAutomaticWildcard(true);
    final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
    assertEquals(property.getPropertyID() + " like '" + wildcard + "upper" + wildcard + "'", model.getPropertyCriteria().asString(database, valueProvider));
    model.setSearchType(SearchType.NOT_LIKE);
    assertEquals(property.getPropertyID() + " not like '" + wildcard + "upper" + wildcard + "'", model.getPropertyCriteria().asString(database, valueProvider));
  }
}
