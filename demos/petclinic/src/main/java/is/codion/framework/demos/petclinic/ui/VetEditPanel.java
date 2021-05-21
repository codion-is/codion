/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Vet;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

public final class VetEditPanel extends EntityEditPanel {

  public VetEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Vet.FIRST_NAME);

    textFieldBuilder(Vet.FIRST_NAME).columns(12).build();
    textFieldBuilder(Vet.LAST_NAME).columns(12).build();

    setLayout(Layouts.gridLayout(1, 2));

    addInputPanel(Vet.FIRST_NAME);
    addInputPanel(Vet.LAST_NAME);
  }
}
