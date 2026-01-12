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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.petclinic.model;

import is.codion.demos.petclinic.domain.api.VetSpecialty;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.framework.model.SwingEntityEditModel;

import static is.codion.framework.db.EntityConnection.Count.where;
import static is.codion.framework.domain.entity.condition.Condition.and;

public final class VetSpecialtyEditModel extends SwingEntityEditModel {

	public VetSpecialtyEditModel(EntityConnectionProvider connectionProvider) {
		super(VetSpecialty.TYPE, connectionProvider);
		editor().validator().set(new VetSpecialtyValidator());
		initializeComboBoxModels(VetSpecialty.VET_FK, VetSpecialty.SPECIALTY_FK);
		editor().value(VetSpecialty.VET_FK).persist().set(false);
		editor().value(VetSpecialty.SPECIALTY_FK).persist().set(false);
	}

	private final class VetSpecialtyValidator implements EntityValidator {

		@Override
		public void validate(Entity entity) {
			// Perform the standard validation in order to
			// assert that all required values are present
			EntityValidator.super.validate(entity);
			int rowCount = connection().count(where(and(
							VetSpecialty.SPECIALTY.equalTo(entity.get(VetSpecialty.SPECIALTY)),
							VetSpecialty.VET.equalTo(entity.get(VetSpecialty.VET)))));
			if (rowCount > 0) {
				throw new ValidationException(VetSpecialty.SPECIALTY_FK,
								entity.get(VetSpecialty.SPECIALTY_FK), "Vet/specialty combination already exists");
			}
		}
	}
}
