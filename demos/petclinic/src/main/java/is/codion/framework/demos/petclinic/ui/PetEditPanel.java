/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Pet;
import is.codion.framework.demos.petclinic.domain.api.PetType;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;

import javax.swing.Action;
import javax.swing.JPanel;

public final class PetEditPanel extends EntityEditPanel {

  public PetEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Pet.NAME);

    createForeignKeyComboBox(Pet.OWNER_FK);
    createTextField(Pet.NAME).setColumns(12);
    createTextField(Pet.BIRTH_DATE);
    EntityComboBox petTypeBox = createForeignKeyComboBox(Pet.PET_TYPE_FK);

    Action newPetTypeAction = new EntityPanelBuilder(PetType.TYPE)
            .setEditPanelClass(PetTypeEditPanel.class)
            .createEditPanelAction(petTypeBox);
    JPanel petTypePanel = Components.createEastButtonPanel(petTypeBox, newPetTypeAction);

    setLayout(Layouts.gridLayout(2, 2));

    addInputPanel(Pet.OWNER_FK);
    addInputPanel(Pet.NAME);
    addInputPanel(Pet.BIRTH_DATE);
    add(createInputPanel(Pet.PET_TYPE_FK, petTypePanel));
  }
}
