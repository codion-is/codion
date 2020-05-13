/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petclinic.ui;

import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static dev.codion.framework.demos.petclinic.domain.Clinic.SPECIALTY_NAME;

public final class SpecialtyEditPanel extends EntityEditPanel {

  public SpecialtyEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(SPECIALTY_NAME);

    createTextField(SPECIALTY_NAME).setColumns(12);

    setLayout(new GridLayout(1, 1, 5, 5));

    addPropertyPanel(SPECIALTY_NAME);
  }
}
