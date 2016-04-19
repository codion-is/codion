/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.model.SearchType;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.DefaultPropertyCriteriaModel;
import org.jminor.framework.model.PropertyCriteriaModel;

import org.junit.Before;
import org.junit.Test;

import javax.swing.JTextField;

import static org.junit.Assert.assertEquals;

public class PropertySearchPanelTest {

  @Before
  public void setUp() {
    TestDomain.init();
  }

  @Test
  public void createWithInitializedModel() {
    final PropertyCriteriaModel<Property.ColumnProperty> criteriaModel =
            new DefaultPropertyCriteriaModel(Entities.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME));
    criteriaModel.setUpperBound("DALLAS");
    criteriaModel.setSearchType(SearchType.LIKE);
    criteriaModel.setEnabled(true);
    final PropertyCriteriaPanel searchPanel = new PropertyCriteriaPanel(criteriaModel);
    assertEquals("DALLAS", ((JTextField) searchPanel.getUpperBoundField()).getText());
  }
}
