/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.table.ColumnConditionPanel;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.framework.model.SwingForeignKeyConditionModel;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * A column condition panel based on a foreign key.
 */
public final class ForeignKeyConditionPanel extends ColumnConditionPanel<Entity, ForeignKey, Entity> {

  /**
   * Instantiates a new ForeignKeyConditionPanel.
   * @param foreignKeyConditionModel the model to base this panel on
   */
  public ForeignKeyConditionPanel(final ForeignKeyConditionModel foreignKeyConditionModel) {
    super(foreignKeyConditionModel, ToggleAdvancedButton.NO,
            new ForeignKeyBoundFieldFactory(foreignKeyConditionModel), Operator.EQUAL, Operator.NOT_EQUAL);
  }

  private static final class ForeignKeyBoundFieldFactory implements BoundFieldFactory {

    private final ColumnConditionModel<Entity, ForeignKey, Entity> model;

    private ForeignKeyBoundFieldFactory(final ColumnConditionModel<Entity, ForeignKey, Entity> model) {
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
        final EntityComboBox comboBox = new EntityComboBox(((SwingForeignKeyConditionModel) model).getEntityComboBoxModel());
        new RefreshOnVisible(comboBox);

        return Completion.maximumMatch(comboBox);
      }

      return TextFields.selectAllOnFocusGained(new EntityLookupField(((ForeignKeyConditionModel) model).getEntityLookupModel()));
    }

    private static final class RefreshOnVisible implements AncestorListener {

      private final EntityComboBox comboBox;

      private RefreshOnVisible(final EntityComboBox comboBox) {
        this.comboBox = comboBox;
        this.comboBox.addAncestorListener(this);
      }

      @Override
      public void ancestorAdded(final AncestorEvent event) {
        comboBox.getModel().refresh();
        comboBox.removeAncestorListener(this);
      }

      @Override
      public void ancestorRemoved(final AncestorEvent event) {}

      @Override
      public void ancestorMoved(final AncestorEvent event) {}
    }
  }
}
