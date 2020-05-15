/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static is.codion.framework.demos.petclinic.domain.Clinic.VET_SPECIALTY_SPECIALTY_FK;
import static is.codion.framework.demos.petclinic.domain.Clinic.VET_SPECIALTY_VET_FK;

public final class VetSpecialtyEditPanel extends EntityEditPanel {

  public VetSpecialtyEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(VET_SPECIALTY_VET_FK);

    createForeignKeyComboBox(VET_SPECIALTY_VET_FK);
    createForeignKeyComboBox(VET_SPECIALTY_SPECIALTY_FK);

    setLayout(new GridLayout(1, 2, 5, 5));

    addPropertyPanel(VET_SPECIALTY_VET_FK);
    addPropertyPanel(VET_SPECIALTY_SPECIALTY_FK);
  }
}
