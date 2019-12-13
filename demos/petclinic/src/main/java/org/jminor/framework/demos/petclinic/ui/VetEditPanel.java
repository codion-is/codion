/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.ui;

import org.jminor.framework.demos.petclinic.domain.Petclinic;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

public final class VetEditPanel extends EntityEditPanel {

  public VetEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(Petclinic.VET_FIRST_NAME);

    createTextField(Petclinic.VET_FIRST_NAME).setColumns(12);
    createTextField(Petclinic.VET_LAST_NAME).setColumns(12);

    setLayout(new GridLayout(1, 2, 5, 5));

    addPropertyPanel(Petclinic.VET_FIRST_NAME);
    addPropertyPanel(Petclinic.VET_LAST_NAME);
  }
}
