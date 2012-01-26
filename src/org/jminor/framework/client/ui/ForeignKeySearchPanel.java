/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.SearchType;
import org.jminor.common.ui.ColumnSearchPanel;
import org.jminor.common.ui.combobox.MaximumMatch;
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
    this(model, false);
  }

  /**
   * Instantiates a new ForeignKeySearchModel.
   * @param model the model to base this panel on
   * @param includeToggleAdvancedSearchButton if true an advanced toggle button is included
   */
  public ForeignKeySearchPanel(final ForeignKeySearchModel model, final boolean includeToggleAdvancedSearchButton) {
    super(model, true, includeToggleAdvancedSearchButton, new ForeignKeyInputFieldProvider(model), SearchType.LIKE, SearchType.NOT_LIKE);
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
      return initializeForeignKeyField();
    }

    private JComponent initializeForeignKeyField() {
      final EntityComboBoxModel boxModel = ((ForeignKeySearchModel) model).getEntityComboBoxModel();
      if (boxModel != null) {
        final EntityComboBox field = new EntityComboBox(boxModel);
        MaximumMatch.enable(field);

        return field;
      }
      else {
        final EntityLookupField field = new EntityLookupField(((ForeignKeySearchModel) model).getEntityLookupModel());
        field.getModel().refreshSearchText();

        return field;
      }
    }
  }
}
