/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.ForeignKeyCriteriaModel;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.table.ColumnCriteriaPanel;
import org.jminor.swing.framework.model.SwingForeignKeyCriteriaModel;

import javax.swing.JComponent;

/**
 * A column criteria panel based on foreign key properties.
 */
public final class ForeignKeyCriteriaPanel extends ColumnCriteriaPanel<Property.ForeignKeyProperty> {

  /**
   * Instantiates a new ForeignKeyCriteriaPanel.
   * @param model the model to base this panel on
   */
  public ForeignKeyCriteriaPanel(final ForeignKeyCriteriaModel model) {
    this(model, true, false);
  }

  /**
   * Instantiates a new ForeignKeyCriteriaPanel.
   * @param model the model to base this panel on
   * @param includeToggleEnabledButton if true a toggle button for enabling/disabling is included
   * @param includeToggleAdvancedCriteriaButton if true an advanced toggle button is included
   */
  public ForeignKeyCriteriaPanel(final ForeignKeyCriteriaModel model, final boolean includeToggleEnabledButton,
                                 final boolean includeToggleAdvancedCriteriaButton) {
    super(model, includeToggleEnabledButton, includeToggleAdvancedCriteriaButton,
            new ForeignKeyInputFieldProvider(model), SearchType.LIKE, SearchType.NOT_LIKE);
  }

  private static final class ForeignKeyInputFieldProvider implements InputFieldProvider {

    private final ColumnCriteriaModel<Property.ForeignKeyProperty> model;

    private ForeignKeyInputFieldProvider(final ColumnCriteriaModel<Property.ForeignKeyProperty> model) {
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
      if (model instanceof SwingForeignKeyCriteriaModel) {
        final EntityComboBoxModel boxModel = ((SwingForeignKeyCriteriaModel) model).getEntityComboBoxModel();
        boxModel.refresh();
        final EntityComboBox field = new EntityComboBox(boxModel);
        MaximumMatch.enable(field);

        return field;
      }
      else {
        return new EntityLookupField(((ForeignKeyCriteriaModel) model).getEntityLookupModel());
      }
    }
  }
}
