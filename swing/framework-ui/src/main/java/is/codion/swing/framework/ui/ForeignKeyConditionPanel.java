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
 * A column condition panel based on a foreign key.
 */
public final class ForeignKeyConditionPanel extends ColumnConditionPanel<Entity, ForeignKeyProperty, Entity> {

  /**
   * Instantiates a new ForeignKeyConditionPanel.
   * @param model the model to base this panel on
   */
  public ForeignKeyConditionPanel(final ForeignKeyConditionModel model) {
    super(model, ToggleAdvancedButton.NO, new ForeignKeyBoundFieldProvider(model), Operator.EQUAL, Operator.NOT_EQUAL);
  }

  private static final class ForeignKeyBoundFieldProvider implements BoundFieldProvider {

    private final ColumnConditionModel<Entity, ForeignKeyProperty, Entity> model;

    private ForeignKeyBoundFieldProvider(final ColumnConditionModel<Entity, ForeignKeyProperty, Entity> model) {
      this.model = model;
    }

    @Override
    public JComponent initializeEqualValueField() {
      return initializeForeignKeyField();
    }

    @Override
    public JComponent initializeUpperBoundField() {
      return null;
    }

    @Override
    public JComponent initializeLowerBoundField() {
      return null;
    }

    private JComponent initializeForeignKeyField() {
      if (model instanceof SwingForeignKeyConditionModel) {
        final SwingEntityComboBoxModel boxModel = ((SwingForeignKeyConditionModel) model).getEntityComboBoxModel();
        boxModel.refresh();

        return MaximumMatch.enable(new EntityComboBox(boxModel));
      }

      return TextFields.selectAllOnFocusGained(new EntityLookupField(((ForeignKeyConditionModel) model).getEntityLookupModel()));
    }
  }
}
