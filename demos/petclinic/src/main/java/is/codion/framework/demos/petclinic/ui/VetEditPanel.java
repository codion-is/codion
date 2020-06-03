/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.Clinic.Vet;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

public final class VetEditPanel extends EntityEditPanel {

  public VetEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Vet.FIRST_NAME);

    createTextField(Vet.FIRST_NAME).setColumns(12);
    createTextField(Vet.LAST_NAME).setColumns(12);

    setLayout(new GridLayout(1, 2, 5, 5));

    addPropertyPanel(Vet.FIRST_NAME);
    addPropertyPanel(Vet.LAST_NAME);
  }
}
