/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.ui;

import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static org.jminor.framework.demos.petclinic.domain.Petclinic.VET_FIRST_NAME;
import static org.jminor.framework.demos.petclinic.domain.Petclinic.VET_LAST_NAME;

public final class VetEditPanel extends EntityEditPanel {

  public VetEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(VET_FIRST_NAME);

    createTextField(VET_FIRST_NAME).setColumns(12);
    createTextField(VET_LAST_NAME).setColumns(12);

    setLayout(new GridLayout(1, 2, 5, 5));

    addPropertyPanel(VET_FIRST_NAME);
    addPropertyPanel(VET_LAST_NAME);
  }
}
