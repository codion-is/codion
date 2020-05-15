/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static is.codion.framework.demos.petclinic.domain.Clinic.PET_TYPE_NAME;

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
