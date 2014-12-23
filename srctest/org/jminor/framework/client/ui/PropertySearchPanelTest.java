/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchType;
import org.jminor.framework.client.model.DefaultPropertySearchModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;

import org.junit.Before;
import org.junit.Test;

import javax.swing.JTextField;

import static org.junit.Assert.assertEquals;

public class PropertySearchPanelTest {

  @Before
  public void setUp() {
    EmpDept.init();
  }

  @Test
  public void createWithInitializedModel() {
    final PropertySearchModel<Property.ColumnProperty> searchModel =
            new DefaultPropertySearchModel(Entities.getColumnProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME));
    searchModel.setUpperBound("DALLAS");
    searchModel.setSearchType(SearchType.LIKE);
    searchModel.setEnabled(true);
    final PropertySearchPanel searchPanel = new PropertySearchPanel(searchModel);
    assertEquals("DALLAS", ((JTextField) searchPanel.getUpperBoundField()).getText());
  }
}
