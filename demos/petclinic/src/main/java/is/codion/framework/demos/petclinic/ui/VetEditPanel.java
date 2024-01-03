/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Vet;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class VetEditPanel extends EntityEditPanel {

  public VetEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Vet.FIRST_NAME);

    createTextField(Vet.FIRST_NAME);
    createTextField(Vet.LAST_NAME);

    setLayout(gridLayout(2, 1));

    addInputPanel(Vet.FIRST_NAME);
    addInputPanel(Vet.LAST_NAME);
  }
}
