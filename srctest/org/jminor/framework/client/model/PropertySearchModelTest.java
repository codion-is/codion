package org.jminor.framework.client.model;

import org.jminor.common.model.SearchType;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.demos.empdept.model.EmpDept;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;

import junit.framework.TestCase;

/**
 * User: Björn Darri
 * Date: 29.7.2009
 * Time: 18:07:24
 */
public class PropertySearchModelTest extends TestCase {

  static {
    new EmpDept();
  }

  public void testPropertySearchModel() throws Exception {
    final Property property = EntityRepository.get().getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME);
    final PropertySearchModel model = new PropertySearchModel(property);
    assertEquals(property, model.getProperty());
    model.setSearchType(SearchType.LIKE);
    model.setUpperBound("upper");
    assertEquals(property.propertyID + " = '" + "upper'", model.getPropertyCriteria().toString());
    model.setSearchType(SearchType.NOT_LIKE);
    assertEquals(property.propertyID + " <> '" + "upper'", model.getPropertyCriteria().toString());
    model.setSearchType(SearchType.MIN);
    assertEquals(property.propertyID + " >= '" + "upper'", model.getPropertyCriteria().toString());
    model.setSearchType(SearchType.MAX);
    assertEquals(property.propertyID + " <= '" + "upper'", model.getPropertyCriteria().toString());

    model.setSearchType(SearchType.INSIDE);
    model.setLowerBound("lower");
    assertEquals("(" + property.propertyID + " >= '" + "lower' and "
            + property.propertyID + " <= '" + "upper')", model.getPropertyCriteria().toString());

    model.setSearchType(SearchType.LIKE);
    model.setAutomaticWildcard(true);
    final String wildcard = (String) FrameworkSettings.get().getProperty(FrameworkSettings.WILDCARD_CHARACTER);
    assertEquals(property.propertyID + " like '" + wildcard + "upper" + wildcard + "'", model.getPropertyCriteria().toString());
    model.setSearchType(SearchType.NOT_LIKE);
    assertEquals(property.propertyID + " not like '" + wildcard + "upper" + wildcard + "'", model.getPropertyCriteria().toString());
  }
}
