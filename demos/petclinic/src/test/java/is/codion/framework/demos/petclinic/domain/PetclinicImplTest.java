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
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.demos.petclinic.domain.api.Owner;
import is.codion.framework.demos.petclinic.domain.api.Pet;
import is.codion.framework.demos.petclinic.domain.api.PetType;
import is.codion.framework.demos.petclinic.domain.api.Specialty;
import is.codion.framework.demos.petclinic.domain.api.Vet;
import is.codion.framework.demos.petclinic.domain.api.VetSpecialty;
import is.codion.framework.demos.petclinic.domain.api.Visit;
import is.codion.framework.domain.test.DomainTest;

import org.junit.jupiter.api.Test;

public final class PetclinicImplTest extends DomainTest {

	public PetclinicImplTest() {
		super(new PetclinicImpl());
	}

	@Test
	void vet() {
		test(Vet.TYPE);
	}

	@Test
	void specialty() {
		test(Specialty.TYPE);
	}

	@Test
	void vetSpecialty() {
		test(VetSpecialty.TYPE);
	}

	@Test
	void petType() {
		test(PetType.TYPE);
	}

	@Test
	void owner() {
		test(Owner.TYPE);
	}

	@Test
	void pet() {
		test(Pet.TYPE);
	}

	@Test
	void visit() {
		test(Visit.TYPE);
	}
}
