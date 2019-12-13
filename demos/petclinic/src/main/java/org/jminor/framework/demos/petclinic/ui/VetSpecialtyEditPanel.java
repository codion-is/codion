/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.ui;

import org.jminor.framework.demos.petclinic.domain.Petclinic;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

public final class VetSpecialtyEditPanel extends EntityEditPanel {

  public VetSpecialtyEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(Petclinic.VET_SPECIALTY_VET_FK);

    createForeignKeyComboBox(Petclinic.VET_SPECIALTY_VET_FK);
    createForeignKeyComboBox(Petclinic.VET_SPECIALTY_SPECIALTY_FK);

    setLayout(new GridLayout(1, 2, 5, 5));

    addPropertyPanel(Petclinic.VET_SPECIALTY_VET_FK);
    addPropertyPanel(Petclinic.VET_SPECIALTY_SPECIALTY_FK);
  }
}
