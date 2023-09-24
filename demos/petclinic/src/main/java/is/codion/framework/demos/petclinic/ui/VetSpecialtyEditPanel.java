/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Specialty;
import is.codion.framework.demos.petclinic.domain.api.VetSpecialty;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.component.EntityComboBox;

import javax.swing.JPanel;

import static is.codion.swing.common.ui.component.button.ButtonPanelBuilder.createEastButtonPanel;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class VetSpecialtyEditPanel extends EntityEditPanel {

  public VetSpecialtyEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(VetSpecialty.VET_FK);

    createForeignKeyComboBox(VetSpecialty.VET_FK)
            .preferredWidth(200);
    EntityComboBox specialtyComboBox =
            createForeignKeyComboBox(VetSpecialty.SPECIALTY_FK)
                    .preferredWidth(200)
                    .build();

    Control newSpecialtyControl = createInsertControl(specialtyComboBox, () ->
            new SpecialtyEditPanel(new SwingEntityEditModel(Specialty.TYPE, editModel().connectionProvider())));
    JPanel specialtyPanel = createEastButtonPanel(specialtyComboBox, newSpecialtyControl);

    setLayout(gridLayout(2, 1));

    addInputPanel(VetSpecialty.VET_FK);
    addInputPanel(VetSpecialty.SPECIALTY_FK, specialtyPanel);
  }
}
