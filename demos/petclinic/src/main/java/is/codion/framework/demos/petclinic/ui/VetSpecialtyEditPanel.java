/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Specialty;
import is.codion.framework.demos.petclinic.domain.api.VetSpecialty;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class VetSpecialtyEditPanel extends EntityEditPanel {

  public VetSpecialtyEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(VetSpecialty.VET_FK);

    createForeignKeyComboBox(VetSpecialty.VET_FK)
            .preferredWidth(200);
    createForeignKeyComboBoxPanel(VetSpecialty.SPECIALTY_FK, this::createSpecialtyEditPanel)
            .preferredWidth(200)
            .addButton(true);

    setLayout(gridLayout(2, 1));

    addInputPanel(VetSpecialty.VET_FK);
    addInputPanel(VetSpecialty.SPECIALTY_FK);
  }

  private SpecialtyEditPanel createSpecialtyEditPanel() {
    return new SpecialtyEditPanel(new SwingEntityEditModel(Specialty.TYPE, editModel().connectionProvider()));
  }
}
