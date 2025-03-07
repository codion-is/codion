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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.petclinic.ui;

import is.codion.demos.petclinic.domain.api.Pet;
import is.codion.demos.petclinic.domain.api.PetType;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class PetEditPanel extends EntityEditPanel {

	public PetEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}

	@Override
	protected void initializeUI() {
		focus().initial().set(Pet.NAME);

		createComboBox(Pet.OWNER_FK);
		createTextField(Pet.NAME);
		createComboBoxPanel(Pet.PET_TYPE_FK, this::createPetTypeEditPanel)
						.includeAddButton(true);
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
