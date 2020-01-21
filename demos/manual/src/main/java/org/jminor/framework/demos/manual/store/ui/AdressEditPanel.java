/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.ui;

import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

public final class AdressEditPanel extends EntityEditPanel {

  public AdressEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(Store.ADDRESS_STREET);

    createTextField(Store.ADDRESS_STREET);
    createTextField(Store.ADDRESS_CITY);
    createCheckBox(Store.ADDRESS_VALID, null, false);

    setLayout(new GridLayout(3, 1, 5, 5));
    addPropertyPanel(Store.ADDRESS_STREET);
    addPropertyPanel(Store.ADDRESS_CITY);
    addPropertyPanel(Store.ADDRESS_VALID);
  }
}
