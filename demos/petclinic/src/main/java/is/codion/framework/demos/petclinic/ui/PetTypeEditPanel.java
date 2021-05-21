/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.PetType;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

public final class PetTypeEditPanel extends EntityEditPanel {

  public PetTypeEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(PetType.NAME);

    createTextField(PetType.NAME).columns(12);

    setLayout(Layouts.gridLayout(1, 1));

    addInputPanel(PetType.NAME);
  }
}
