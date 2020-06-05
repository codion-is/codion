/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static is.codion.framework.demos.petclinic.domain.Clinic.Owner;

public final class OwnerEditPanel extends EntityEditPanel {

  public OwnerEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Owner.FIRST_NAME);

    createTextField(Owner.FIRST_NAME).setColumns(12);
    createTextField(Owner.LAST_NAME).setColumns(12);
    createTextField(Owner.ADDRESS).setColumns(12);
    createTextField(Owner.CITY).setColumns(12);
    createTextField(Owner.TELEPHONE).setColumns(12);

    setLayout(new GridLayout(3, 2, 5, 5));

    addInputPanel(Owner.FIRST_NAME);
    addInputPanel(Owner.LAST_NAME);
    addInputPanel(Owner.ADDRESS);
    addInputPanel(Owner.CITY);
    addInputPanel(Owner.TELEPHONE);
  }
}
