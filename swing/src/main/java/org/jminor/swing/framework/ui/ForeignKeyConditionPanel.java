/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.db.condition.Condition;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.ForeignKeyConditionModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;
import org.jminor.swing.framework.model.SwingForeignKeyConditionModel;

import javax.swing.JComponent;

/**
 * A column condition panel based on foreign key properties.
 */
public final class ForeignKeyConditionPanel extends ColumnConditionPanel<Property.ForeignKeyProperty> {

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
            new ForeignKeyInputFieldProvider(model), Condition.Type.LIKE, Condition.Type.NOT_LIKE);
  }

  private static final class ForeignKeyInputFieldProvider implements InputFieldProvider {

    private final ColumnConditionModel<Property.ForeignKeyProperty> model;

    private ForeignKeyInputFieldProvider(final ColumnConditionModel<Property.ForeignKeyProperty> model) {
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
        final EntityComboBoxModel boxModel = ((SwingForeignKeyConditionModel) model).getEntityComboBoxModel();
        boxModel.refresh();
        final EntityComboBox field = new EntityComboBox(boxModel);
        MaximumMatch.enable(field);

        return field;
      }
      else {
        return UiUtil.selectAllOnFocusGained(new EntityLookupField(((ForeignKeyConditionModel) model).getEntityLookupModel()));
      }
    }
  }
}
