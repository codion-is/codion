/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.Clinic.PetType;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

public final class PetTypeEditPanel extends EntityEditPanel {

  public PetTypeEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(PetType.NAME);

    createTextField(PetType.NAME).setColumns(12);

    setLayout(new GridLayout(1, 1, 5, 5));

    addPropertyPanel(PetType.NAME);
  }
}
