/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.ui;

import org.jminor.framework.demos.petclinic.domain.Petclinic;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

public final class SpecialtyEditPanel extends EntityEditPanel {

  public SpecialtyEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(Petclinic.SPECIALTY_NAME);

    createTextField(Petclinic.SPECIALTY_NAME).setColumns(12);

    setLayout(new GridLayout(1, 1, 5, 5));

    addPropertyPanel(Petclinic.SPECIALTY_NAME);
  }
}
