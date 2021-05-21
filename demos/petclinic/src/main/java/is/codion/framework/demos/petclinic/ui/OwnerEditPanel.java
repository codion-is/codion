/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Owner;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

public final class OwnerEditPanel extends EntityEditPanel {

  public OwnerEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Owner.FIRST_NAME);

    textFieldBuilder(Owner.FIRST_NAME).columns(12).build();
    textFieldBuilder(Owner.LAST_NAME).columns(12).build();
    textFieldBuilder(Owner.ADDRESS).columns(12).build();
    textFieldBuilder(Owner.CITY).columns(12).build();
    textFieldBuilder(Owner.TELEPHONE).columns(12).build();

    setLayout(Layouts.gridLayout(3, 2));

    addInputPanel(Owner.FIRST_NAME);
    addInputPanel(Owner.LAST_NAME);
    addInputPanel(Owner.ADDRESS);
    addInputPanel(Owner.CITY);
    addInputPanel(Owner.TELEPHONE);
  }
}
