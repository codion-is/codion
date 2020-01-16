/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.ui;

import org.jminor.swing.common.ui.Components;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;

import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.GridLayout;

import static org.jminor.framework.demos.petclinic.domain.Clinic.*;

public final class PetEditPanel extends EntityEditPanel {

  public PetEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(PET_NAME);

    createForeignKeyComboBox(PET_OWNER_FK);
    createTextField(PET_NAME).setColumns(12);
    createTextField(PET_BIRTH_DATE);
    final EntityComboBox petTypeBox = createForeignKeyComboBox(PET_PET_TYPE_FK);

    final Action newPetTypeAction = EntityEditPanel.createEditPanelAction(petTypeBox,
            new EntityPanelProvider(T_PET_TYPE)
                    .setEditPanelClass(PetTypeEditPanel.class));
    final JPanel petTypePanel = Components.createEastButtonPanel(petTypeBox, newPetTypeAction, false);

    setLayout(new GridLayout(2, 2, 5, 5));

    addPropertyPanel(PET_OWNER_FK);
    addPropertyPanel(PET_NAME);
    addPropertyPanel(PET_BIRTH_DATE);
    add(createPropertyPanel(PET_PET_TYPE_FK, petTypePanel));
  }
}
