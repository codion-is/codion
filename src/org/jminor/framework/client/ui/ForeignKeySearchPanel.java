/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.table.ColumnSearchModel;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.table.ColumnSearchPanel;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.ForeignKeySearchModel;
import org.jminor.framework.domain.Property;

import javax.swing.JComponent;

/**
 * A column search panel based on foreign key properties.
 */
public final class ForeignKeySearchPanel extends ColumnSearchPanel<Property.ForeignKeyProperty> {

  /**
   * Instantiates a new ForeignKeySearchModel.
   * @param model the model to base this panel on
   */
  public ForeignKeySearchPanel(final ForeignKeySearchModel model) {
    this(model, true, false);
  }

  /**
   * Instantiates a new ForeignKeySearchModel.
   * @param model the model to base this panel on
   * @param includeToggleSearchEnabledButton if true a toggle button for enabling/disabling is included
   * @param includeToggleAdvancedSearchButton if true an advanced toggle button is included
   */
  public ForeignKeySearchPanel(final ForeignKeySearchModel model, final boolean includeToggleSearchEnabledButton,
                               final boolean includeToggleAdvancedSearchButton) {
    super(model, includeToggleSearchEnabledButton, includeToggleAdvancedSearchButton,
            new ForeignKeyInputFieldProvider(model), SearchType.LIKE, SearchType.NOT_LIKE);
  }

  private static final class ForeignKeyInputFieldProvider implements InputFieldProvider<Property.ForeignKeyProperty> {

    private final ColumnSearchModel<Property.ForeignKeyProperty> model;

    private ForeignKeyInputFieldProvider(final ColumnSearchModel<Property.ForeignKeyProperty> model) {
      this.model = model;
    }

    /** {@inheritDoc} */
    @Override
    public ColumnSearchModel<Property.ForeignKeyProperty> getSearchModel() {
      return model;
    }

    /** {@inheritDoc} */
    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (isUpperBound) {
        return initializeForeignKeyField();
      }

      return null;
    }

    private JComponent initializeForeignKeyField() {
      final EntityComboBoxModel boxModel = ((ForeignKeySearchModel) model).getEntityComboBoxModel();
      if (boxModel != null) {
        boxModel.refresh();
        final EntityComboBox field = new EntityComboBox(boxModel);
        MaximumMatch.enable(field);

        return field;
      }
      else {
        return new EntityLookupField(((ForeignKeySearchModel) model).getEntityLookupModel());
      }
    }
  }
}
