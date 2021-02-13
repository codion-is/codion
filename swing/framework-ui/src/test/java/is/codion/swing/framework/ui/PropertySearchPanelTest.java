/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Property;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PropertySearchPanelTest {

  private static final Domain DOMAIN = new TestDomain();

  @Test
  public void createWithInitializedModel() {
    final EntityDefinition definition = DOMAIN.getEntities().getDefinition(TestDomain.T_DEPARTMENT);
    final ColumnProperty<String> property = definition.getColumnProperty(TestDomain.DEPARTMENT_NAME);
    final ColumnConditionModel<Entity, Attribute<String>, String> conditionModel =
            new DefaultColumnConditionModel<>(property.getAttribute(), property.getAttribute().getTypeClass(), Property.WILDCARD_CHARACTER.get(),
                    property.getFormat(), property.getDateTimeFormatPattern());
    conditionModel.setEqualValue("DALLAS");
    conditionModel.setOperator(Operator.EQUAL);
    AttributeConditionPanel<Attribute<String>, String> searchPanel = new AttributeConditionPanel<>(conditionModel, definition, TestDomain.DEPARTMENT_NAME);
    assertEquals("DALLAS", ((JTextField) searchPanel.getEqualField()).getText());

    conditionModel.setLowerBound("A");
    conditionModel.setUpperBound("D");
    conditionModel.setOperator(Operator.BETWEEN);
    searchPanel = new AttributeConditionPanel<>(conditionModel, definition, TestDomain.DEPARTMENT_NAME);
    assertEquals("A", ((JTextField) searchPanel.getLowerBoundField()).getText());
    assertEquals("D", ((JTextField) searchPanel.getUpperBoundField()).getText());
    assertEquals(Operator.BETWEEN, searchPanel.getOperatorComboBox().getSelectedItem());
  }
}
