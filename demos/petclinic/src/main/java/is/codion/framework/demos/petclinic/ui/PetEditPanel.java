/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.swing.common.ui.Components;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;

import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.GridLayout;

import static is.codion.framework.demos.petclinic.domain.Clinic.Pet;
import static is.codion.framework.demos.petclinic.domain.Clinic.PetType;

public final class PetEditPanel extends EntityEditPanel {

  public PetEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Pet.NAME);

    createForeignKeyComboBox(Pet.OWNER_FK);
    createTextField(Pet.NAME).setColumns(12);
    createTextField(Pet.BIRTH_DATE);
    final EntityComboBox petTypeBox = createForeignKeyComboBox(Pet.PET_TYPE_FK);

    final Action newPetTypeAction = new EntityPanelBuilder(PetType.TYPE)
            .setEditPanelClass(PetTypeEditPanel.class)
            .createEditPanelAction(petTypeBox);
    final JPanel petTypePanel = Components.createEastButtonPanel(petTypeBox, newPetTypeAction);

    setLayout(new GridLayout(2, 2, 5, 5));

    addInputPanel(Pet.OWNER_FK);
    addInputPanel(Pet.NAME);
    addInputPanel(Pet.BIRTH_DATE);
    add(createInputPanel(Pet.PET_TYPE_FK, petTypePanel));
  }
}
