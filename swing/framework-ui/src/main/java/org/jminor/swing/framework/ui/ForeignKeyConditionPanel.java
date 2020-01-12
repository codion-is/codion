/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.db.ConditionType;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.model.ForeignKeyConditionModel;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;
import org.jminor.swing.common.ui.textfield.TextFields;
import org.jminor.swing.framework.model.SwingEntityComboBoxModel;
import org.jminor.swing.framework.model.SwingForeignKeyConditionModel;

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
    this(model, true, false);
  }

  /**
   * Instantiates a new ForeignKeyConditionPanel.
   * @param model the model to base this panel on
   * @param includeToggleEnabledButton if true a toggle button for enabling/disabling is included
   * @param includeToggleAdvancedConditionButton if true an advanced toggle button is included
   */
  public ForeignKeyConditionPanel(final ForeignKeyConditionModel model, final boolean includeToggleEnabledButton,
                                  final boolean includeToggleAdvancedConditionButton) {
    super(model, includeToggleEnabledButton, includeToggleAdvancedConditionButton,
            new ForeignKeyInputFieldProvider(model), ConditionType.LIKE, ConditionType.NOT_LIKE);
  }

  private static final class ForeignKeyInputFieldProvider implements InputFieldProvider {

    private final ColumnConditionModel<Entity, ForeignKeyProperty> model;

    private ForeignKeyInputFieldProvider(final ColumnConditionModel<Entity, ForeignKeyProperty> model) {
      this.model = model;
    }

    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (isUpperBound) {
        return initializeForeignKeyField();
      }

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
