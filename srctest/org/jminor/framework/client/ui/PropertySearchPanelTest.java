/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchType;
import org.jminor.framework.client.model.DefaultPropertySearchModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;

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
    final PropertySearchModel<Property.ColumnProperty> searchModel =
            new DefaultPropertySearchModel(Entities.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME));
    searchModel.setUpperBound("DALLAS");
    searchModel.setSearchType(SearchType.LIKE);
    searchModel.setEnabled(true);
    final PropertySearchPanel searchPanel = new PropertySearchPanel(searchModel);
    assertEquals("DALLAS", ((JTextField) searchPanel.getUpperBoundField()).getText());
  }
}
