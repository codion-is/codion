/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petclinic.ui;

import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static dev.codion.framework.demos.petclinic.domain.Clinic.*;

public final class OwnerEditPanel extends EntityEditPanel {

  public OwnerEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(OWNER_FIRST_NAME);

    createTextField(OWNER_FIRST_NAME).setColumns(12);
    createTextField(OWNER_LAST_NAME).setColumns(12);
    createTextField(OWNER_ADDRESS).setColumns(12);
    createTextField(OWNER_CITY).setColumns(12);
    createTextField(OWNER_TELEPHONE).setColumns(12);

    setLayout(new GridLayout(3, 2, 5, 5));

    addPropertyPanel(OWNER_FIRST_NAME);
    addPropertyPanel(OWNER_LAST_NAME);
    addPropertyPanel(OWNER_ADDRESS);
    addPropertyPanel(OWNER_CITY);
    addPropertyPanel(OWNER_TELEPHONE);
  }
}
