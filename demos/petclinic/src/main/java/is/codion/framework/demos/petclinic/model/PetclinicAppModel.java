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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.petclinic.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petclinic.domain.api.Owner;
import is.codion.framework.demos.petclinic.domain.api.Pet;
import is.codion.framework.demos.petclinic.domain.api.Visit;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

public final class PetclinicAppModel extends SwingEntityApplicationModel {

	public PetclinicAppModel(EntityConnectionProvider connectionProvider) {
		super(connectionProvider);
		setupEntityModels(connectionProvider);
	}

	private void setupEntityModels(EntityConnectionProvider connectionProvider) {
		SwingEntityModel ownersModel = new SwingEntityModel(Owner.TYPE, connectionProvider);
		SwingEntityModel petsModel = new SwingEntityModel(Pet.TYPE, connectionProvider);
		petsModel.editModel().initializeComboBoxModels(Pet.OWNER_FK, Pet.PET_TYPE_FK);
		SwingEntityModel visitModel = new SwingEntityModel(Visit.TYPE, connectionProvider);
		visitModel.editModel().initializeComboBoxModels(Visit.PET_FK);

		ownersModel.addDetailModel(petsModel);
		petsModel.addDetailModel(visitModel);

		ownersModel.tableModel().refresh();

		addEntityModel(ownersModel);
	}
}
