/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Pet;
import is.codion.framework.demos.petclinic.domain.api.PetType;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class PetEditPanel extends EntityEditPanel {

  public PetEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Pet.NAME);

    createForeignKeyComboBox(Pet.OWNER_FK);
    createTextField(Pet.NAME);
    createForeignKeyComboBoxPanel(Pet.PET_TYPE_FK, this::createPetTypeEditPanel)
            .add(true);
    createTemporalFieldPanel(Pet.BIRTH_DATE);

    setLayout(gridLayout(2, 2));

    addInputPanel(Pet.OWNER_FK);
    addInputPanel(Pet.NAME);
    addInputPanel(Pet.PET_TYPE_FK);
    addInputPanel(Pet.BIRTH_DATE);
  }

  private PetTypeEditPanel createPetTypeEditPanel() {
    return new PetTypeEditPanel(new SwingEntityEditModel(PetType.TYPE, editModel().connectionProvider()));
  }
}
