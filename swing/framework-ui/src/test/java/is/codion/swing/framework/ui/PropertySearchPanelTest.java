/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Property;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PropertySearchPanelTest {

  private static final Domain DOMAIN = new TestDomain();

  @Test
  public void createWithInitializedModel() {
    final ColumnProperty<String> property = DOMAIN.getEntities().getDefinition(TestDomain.T_DEPARTMENT)
            .getColumnProperty(TestDomain.DEPARTMENT_NAME);
    final ColumnConditionModel<Entity, ColumnProperty<String>, String> conditionModel =
            new DefaultColumnConditionModel<>(property, property.getAttribute().getTypeClass(), Property.WILDCARD_CHARACTER.get(),
                    property.getFormat(), property.getDateTimeFormatPattern());
    conditionModel.setEqualValue("DALLAS");
    conditionModel.setOperator(Operator.EQUAL);
    PropertyConditionPanel<String> searchPanel = new PropertyConditionPanel<>(conditionModel);
    assertEquals("DALLAS", ((JTextField) searchPanel.getEqualField()).getText());

    conditionModel.setLowerBound("A");
    conditionModel.setUpperBound("D");
    conditionModel.setOperator(Operator.BETWEEN);
    searchPanel = new PropertyConditionPanel<>(conditionModel);
    assertEquals("A", ((JTextField) searchPanel.getLowerBoundField()).getText());
    assertEquals("D", ((JTextField) searchPanel.getUpperBoundField()).getText());
    assertEquals(Operator.BETWEEN, searchPanel.getOperatorComboBox().getSelectedItem());
  }
}
