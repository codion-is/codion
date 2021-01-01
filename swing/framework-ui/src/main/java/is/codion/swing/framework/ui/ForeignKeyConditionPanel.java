/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.combobox.Completion;
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
   * @param foreignKeyConditionModel the model to base this panel on
   */
  public ForeignKeyConditionPanel(final ForeignKeyConditionModel foreignKeyConditionModel) {
    super(foreignKeyConditionModel, ToggleAdvancedButton.NO,
            new ForeignKeyBoundFieldFactory(foreignKeyConditionModel), Operator.EQUAL, Operator.NOT_EQUAL);
  }

  private static final class ForeignKeyBoundFieldFactory implements BoundFieldFactory {

    private final ColumnConditionModel<Entity, ForeignKeyProperty, Entity> model;

    private ForeignKeyBoundFieldFactory(final ColumnConditionModel<Entity, ForeignKeyProperty, Entity> model) {
      this.model = model;
    }

    @Override
    public JComponent createEqualField() {
      return createForeignKeyField();
    }

    @Override
    public JComponent createUpperBoundField() {
      return null;
    }

    @Override
    public JComponent createLowerBoundField() {
      return null;
    }

    private JComponent createForeignKeyField() {
      if (model instanceof SwingForeignKeyConditionModel) {
        final SwingEntityComboBoxModel boxModel = ((SwingForeignKeyConditionModel) model).getEntityComboBoxModel();
        boxModel.refresh();

        return Completion.maximumMatch(new EntityComboBox(boxModel));
      }

      return TextFields.selectAllOnFocusGained(new EntityLookupField(((ForeignKeyConditionModel) model).getEntityLookupModel()));
    }
  }
}
