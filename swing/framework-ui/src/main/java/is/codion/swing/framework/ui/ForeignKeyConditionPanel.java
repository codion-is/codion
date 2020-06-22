/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.combobox.MaximumMatch;
import is.codion.swing.common.ui.table.ColumnConditionPanel;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingForeignKeyConditionModel;

import javax.swing.JComponent;

/**
 * A column condition panel based on foreign key properties.
 */
public final class ForeignKeyConditionPanel extends ColumnConditionPanel<Entity, ForeignKeyProperty> {

  /**
   * Instantiates a new ForeignKeyConditionPanel.
   * @param model the model to base this panel on
   */
  public ForeignKeyConditionPanel(final ForeignKeyConditionModel model) {
    super(model, ToggleAdvancedButton.NO, new ForeignKeyBoundFieldProvider(model), Operator.EQUALS, Operator.NOT_EQUALS);
  }

  private static final class ForeignKeyBoundFieldProvider implements BoundFieldProvider {

    private final ColumnConditionModel<Entity, ForeignKeyProperty> model;

    private ForeignKeyBoundFieldProvider(final ColumnConditionModel<Entity, ForeignKeyProperty> model) {
      this.model = model;
    }

    @Override
    public JComponent initializeUpperBoundField() {
      return initializeForeignKeyField();
    }

    @Override
    public JComponent initializeLowerBoundField() {
      return null;
    }

    private JComponent initializeForeignKeyField() {
      if (model instanceof SwingForeignKeyConditionModel) {
        final SwingEntityComboBoxModel boxModel = ((SwingForeignKeyConditionModel) model).getEntityComboBoxModel();
        boxModel.refresh();
        final EntityComboBox field = new EntityComboBox(boxModel);
        MaximumMatch.enable(field);

        return field;
      }

      return TextFields.selectAllOnFocusGained(new EntityLookupField(((ForeignKeyConditionModel) model).getEntityLookupModel()));
    }
  }
}
