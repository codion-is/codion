/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.Vet;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

public final class VetEditPanel extends EntityEditPanel {

  public VetEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Vet.FIRST_NAME);

    createTextField(Vet.FIRST_NAME).setColumns(12);
    createTextField(Vet.LAST_NAME).setColumns(12);

    setLayout(Layouts.gridLayout(1, 2));

    addInputPanel(Vet.FIRST_NAME);
    addInputPanel(Vet.LAST_NAME);
  }
}
