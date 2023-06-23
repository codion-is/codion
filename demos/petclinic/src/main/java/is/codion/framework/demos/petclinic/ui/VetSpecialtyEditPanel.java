/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Specialty;
import is.codion.framework.demos.petclinic.domain.api.VetSpecialty;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.JPanel;

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.buttonPanel;

public final class VetSpecialtyEditPanel extends EntityEditPanel {

  public VetSpecialtyEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(VetSpecialty.VET_FK);

    createForeignKeyComboBox(VetSpecialty.VET_FK);
    EntityComboBox specialtyComboBox = createForeignKeyComboBox(VetSpecialty.SPECIALTY_FK).build();

    Control newSpecialtyControl = EntityPanel.builder(Specialty.TYPE)
            .editPanelClass(SpecialtyEditPanel.class)
            .createInsertControl(specialtyComboBox);
    JPanel specialtyPanel = borderLayoutPanel()
            .centerComponent(specialtyComboBox)
            .eastComponent(buttonPanel(newSpecialtyControl).build())
            .build();

    setLayout(Layouts.gridLayout(1, 2));

    addInputPanel(VetSpecialty.VET_FK);
    addInputPanel(VetSpecialty.SPECIALTY_FK, specialtyPanel);
  }
}
