/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.table.ColumnConditionPanel;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.framework.model.SwingForeignKeyConditionModel;

import javax.swing.JComponent;
import java.util.Arrays;

/**
 * A column condition panel based on a foreign key.
 */
public final class ForeignKeyConditionPanel extends ColumnConditionPanel<ForeignKey, Entity> {

  /**
   * Instantiates a new ForeignKeyConditionPanel.
   * @param foreignKeyConditionModel the model to base this panel on
   */
  public ForeignKeyConditionPanel(final ForeignKeyConditionModel foreignKeyConditionModel) {
    super(foreignKeyConditionModel, ToggleAdvancedButton.NO,
            new ForeignKeyBoundFieldFactory(foreignKeyConditionModel), Arrays.asList(Operator.EQUAL, Operator.NOT_EQUAL));
  }

  private static final class ForeignKeyBoundFieldFactory implements BoundFieldFactory {

    private final ColumnConditionModel<ForeignKey, Entity> model;

    private ForeignKeyBoundFieldFactory(final ColumnConditionModel<ForeignKey, Entity> model) {
      this.model = model;
    }

    @Override
    public JComponent createEqualField() {
      return Components.setPreferredHeight(createForeignKeyField(), TextFields.getPreferredTextFieldHeight());
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
        return Completion.maximumMatch(new EntityComboBox(((SwingForeignKeyConditionModel) model).getEntityComboBoxModel()).refreshOnSetVisible());
      }

      return TextFields.selectAllOnFocusGained(new EntitySearchField(((ForeignKeyConditionModel) model).getEntitySearchModel()));
    }
  }
}
