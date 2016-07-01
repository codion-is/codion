/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.model.ConditionType;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.DefaultPropertyConditionModel;
import org.jminor.framework.model.PropertyConditionModel;

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
    final PropertyConditionModel<Property.ColumnProperty> conditionModel =
            new DefaultPropertyConditionModel(Entities.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME));
    conditionModel.setUpperBound("DALLAS");
    conditionModel.setSearchType(ConditionType.LIKE);
    conditionModel.setEnabled(true);
    final PropertyConditionPanel searchPanel = new PropertyConditionPanel(conditionModel);
    assertEquals("DALLAS", ((JTextField) searchPanel.getUpperBoundField()).getText());
  }
}
