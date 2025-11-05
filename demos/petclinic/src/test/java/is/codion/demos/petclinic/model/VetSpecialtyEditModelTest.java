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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.petclinic.model;

import is.codion.common.utilities.user.User;
import is.codion.demos.petclinic.domain.PetclinicImpl;
import is.codion.demos.petclinic.domain.api.Specialty;
import is.codion.demos.petclinic.domain.api.Vet;
import is.codion.demos.petclinic.domain.api.VetSpecialty;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class VetSpecialtyEditModelTest {

	@Test
	void validation() {
		try (EntityConnectionProvider connectionProvider = createConnectionProvider()) {
			EntityConnection connection = connectionProvider.connection();
			VetSpecialtyEditModel model = new VetSpecialtyEditModel(connectionProvider);

			Entity linda = connection.selectSingle(Vet.FIRST_NAME.equalTo("Linda"));
			Entity surgery = connection.selectSingle(Specialty.NAME.equalTo("surgery"));

			// Test insert
			model.editor().value(VetSpecialty.VET_FK).set(linda);
			model.editor().value(VetSpecialty.SPECIALTY_FK).set(surgery);
			assertThrows(ValidationException.class, model::insert);

			// Test update
			List<Entity> specialties = connection.select(VetSpecialty.VET_FK.equalTo(linda));
			model.editor().clear();
			model.editor().set(specialties.get(0));
			model.editor().value(VetSpecialty.SPECIALTY).set(specialties.get(1).get(VetSpecialty.SPECIALTY));
			assertThrows(ValidationException.class, model::update);
		}
	}

	private static EntityConnectionProvider createConnectionProvider() {
		return LocalEntityConnectionProvider.builder()
						.domain(new PetclinicImpl())
						.user(User.parse("scott:tiger"))
						.build();
	}
}
