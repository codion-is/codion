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
    createTemporalFieldPanel(Pet.BIRTH_DATE);
    createForeignKeyComboBoxPanel(Pet.PET_TYPE_FK, () ->
            new PetTypeEditPanel(new SwingEntityEditModel(PetType.TYPE, editModel().connectionProvider())))
            .addButton(true);

    setLayout(gridLayout(2, 2));

    addInputPanel(Pet.OWNER_FK);
    addInputPanel(Pet.NAME);
    addInputPanel(Pet.BIRTH_DATE);
    addInputPanel(Pet.PET_TYPE_FK);
  }
}
