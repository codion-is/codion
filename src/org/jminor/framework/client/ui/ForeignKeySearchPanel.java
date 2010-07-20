/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

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
import java.text.Format;

/**
 * User: Björn Darri
 * Date: 19.7.2010
 * Time: 14:15:36
 */
public class ForeignKeySearchPanel extends AbstractSearchPanel<Property.ForeignKeyProperty> {

  public ForeignKeySearchPanel(final ForeignKeySearchModel model) {
    this(model, false, false);
  }

  public ForeignKeySearchPanel(final ForeignKeySearchModel model, final boolean includeActivateBtn,
                               final boolean includeToggleAdvBtn) {
    super(model, includeActivateBtn, includeToggleAdvBtn);
    model.initialize();
  }

  @Override
  protected boolean isLowerBoundFieldRequired(final Property.ForeignKeyProperty property) {
    return false;
  }

  @Override
  protected boolean searchTypeAllowed(final SearchType searchType) {
    return searchType == SearchType.LIKE || searchType == SearchType.NOT_LIKE;
  }

  @Override
  protected JComponent getInputField(final boolean isUpperBound) {
    final JComponent field = initEntityField();
    field.setToolTipText(isUpperBound ? "a" : "b");

    return field;
  }

  protected Format getInputFormat() {
    return null;
  }

  private JComponent initEntityField() {
    final EntityComboBoxModel boxModel = ((ForeignKeySearchModel) getModel()).getEntityComboBoxModel();
    if (boxModel != null) {
      final EntityComboBox field = new EntityComboBox(boxModel);
      MaximumMatch.enable(field);

      return field;
    }
    else {
      final EntityLookupField field = new EntityLookupField(((ForeignKeySearchModel) getModel()).getEntityLookupModel());
      field.setEnterAction(getEnableAction());
      field.getModel().refreshSearchText();

      return field;
    }
  }

  private Action getEnableAction() {
    return new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        getModel().setSearchEnabled(!getModel().isSearchEnabled());
      }
    };
  }
}
