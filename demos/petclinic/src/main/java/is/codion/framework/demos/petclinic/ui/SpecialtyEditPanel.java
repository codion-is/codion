/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Specialty;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

public final class SpecialtyEditPanel extends EntityEditPanel {

  public SpecialtyEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Specialty.NAME);

    textFieldBuilder(Specialty.NAME).columns(12).build();

    setLayout(Layouts.gridLayout(1, 1));

    addInputPanel(Specialty.NAME);
  }
}
