/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.db.condition.Condition;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultPropertyConditionModel;
import org.jminor.framework.model.PropertyConditionModel;

import org.junit.Test;

import javax.swing.JTextField;

import static org.junit.Assert.assertEquals;

public class PropertySearchPanelTest {

  private static final Entities ENTITIES = new TestDomain();

  @Test
  public void createWithInitializedModel() {
    final PropertyConditionModel<Property.ColumnProperty> conditionModel =
            new DefaultPropertyConditionModel(new EntityConditions(ENTITIES),
                    ENTITIES.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME));
    conditionModel.setUpperBound("DALLAS");
    conditionModel.setConditionType(Condition.Type.LIKE);
    conditionModel.setEnabled(true);
    final PropertyConditionPanel searchPanel = new PropertyConditionPanel(conditionModel);
    assertEquals("DALLAS", ((JTextField) searchPanel.getUpperBoundField()).getText());
  }
}
