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
    final ColumnProperty property = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT)
            .getColumnProperty(TestDomain.DEPARTMENT_NAME);
    final ColumnConditionModel<Entity, ColumnProperty> conditionModel =
            new DefaultColumnConditionModel<>(property, property.getTypeClass(), Property.WILDCARD_CHARACTER.get(),
                    property.getFormat(), property.getDateTimeFormatPattern());
    conditionModel.setUpperBound("DALLAS");
    conditionModel.setOperator(Operator.LIKE);
    conditionModel.setEnabled(true);
    final PropertyConditionPanel searchPanel = new PropertyConditionPanel(conditionModel);
    assertEquals("DALLAS", ((JTextField) searchPanel.getUpperBoundField()).getText());
  }
}
