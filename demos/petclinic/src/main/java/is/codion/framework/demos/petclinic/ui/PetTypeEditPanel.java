/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petclinic.ui;

import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static dev.codion.framework.demos.petclinic.domain.Clinic.PET_TYPE_NAME;

public final class PetTypeEditPanel extends EntityEditPanel {

  public PetTypeEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(PET_TYPE_NAME);

    createTextField(PET_TYPE_NAME).setColumns(12);

    setLayout(new GridLayout(1, 1, 5, 5));

    addPropertyPanel(PET_TYPE_NAME);
  }
}
