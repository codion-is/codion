/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchModel;
import org.jminor.common.model.SearchType;
import org.jminor.common.ui.AbstractSearchPanel;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.ForeignKeySearchModel;
import org.jminor.framework.domain.Property;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;

/**
 * User: Björn Darri
 * Date: 19.7.2010
 * Time: 14:15:36
 */
public final class ForeignKeySearchPanel extends AbstractSearchPanel<Property.ForeignKeyProperty> {

  public ForeignKeySearchPanel(final ForeignKeySearchModel model) {
    this(model, false);
  }

  public ForeignKeySearchPanel(final ForeignKeySearchModel model, final boolean includeToggleAdvBtn) {
    super(model, true, includeToggleAdvBtn, new ForeignKeyInputFieldProvider(model));
  }

  @Override
  protected boolean isLowerBoundFieldRequired(final Property.ForeignKeyProperty searchKey) {
    return false;
  }

  @Override
  protected boolean searchTypeAllowed(final SearchType searchType) {
    return searchType == SearchType.LIKE || searchType == SearchType.NOT_LIKE;
  }

  private static final class ForeignKeyInputFieldProvider implements InputFieldProvider<Property.ForeignKeyProperty> {

    private final SearchModel<Property.ForeignKeyProperty> model;

    private ForeignKeyInputFieldProvider(final SearchModel<Property.ForeignKeyProperty> model) {
      this.model = model;
    }

    public SearchModel<Property.ForeignKeyProperty> getSearchModel() {
      return model;
    }

    public JComponent getInputField(final boolean isUpperBound) {
      final JComponent field = initEntityField();
      field.setToolTipText(isUpperBound ? "a" : "b");

      return field;
    }

    private JComponent initEntityField() {
      final EntityComboBoxModel boxModel = ((ForeignKeySearchModel) model).getEntityComboBoxModel();
      if (boxModel != null) {
        final EntityComboBox field = new EntityComboBox(boxModel);
        MaximumMatch.enable(field);

        return field;
      }
      else {
        final EntityLookupField field = new EntityLookupField(((ForeignKeySearchModel) model).getEntityLookupModel());
        field.setEnterAction(getEnableAction());
        field.getModel().refreshSearchText();

        return field;
      }
    }

    private Action getEnableAction() {
      return new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
          model.setSearchEnabled(!model.isSearchEnabled());
        }
      };
    }
  }
}
