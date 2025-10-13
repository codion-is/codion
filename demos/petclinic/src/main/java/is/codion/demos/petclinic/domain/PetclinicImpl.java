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
package is.codion.demos.petclinic.domain;

import is.codion.demos.petclinic.domain.api.Owner;
import is.codion.demos.petclinic.domain.api.Owner.PhoneType;
import is.codion.demos.petclinic.domain.api.Pet;
import is.codion.demos.petclinic.domain.api.PetType;
import is.codion.demos.petclinic.domain.api.Petclinic;
import is.codion.demos.petclinic.domain.api.Specialty;
import is.codion.demos.petclinic.domain.api.Vet;
import is.codion.demos.petclinic.domain.api.VetSpecialty;
import is.codion.demos.petclinic.domain.api.Visit;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityFormatter;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Column.Converter;

import java.sql.Statement;

import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;

public final class PetclinicImpl extends DomainModel {

	public PetclinicImpl() {
		super(Petclinic.DOMAIN);
		add(vet(), specialty(), vetSpecialty(), petType(), owner(), pet(), visit());
	}

	private EntityDefinition vet() {
		return Vet.TYPE.define(
										Vet.ID.define()
														.primaryKey()
														.generator(identity()),
										Vet.FIRST_NAME.define()
														.column()
														.caption("First name")
														.searchable(true)
														.maximumLength(30)
														.nullable(false),
										Vet.LAST_NAME.define()
														.column()
														.caption("Last name")
														.searchable(true)
														.maximumLength(30)
														.nullable(false))
						.caption("Vets")
						.formatter(EntityFormatter.builder()
										.value(Vet.LAST_NAME)
										.text(", ")
										.value(Vet.FIRST_NAME)
										.build())
						.orderBy(ascending(Vet.LAST_NAME, Vet.FIRST_NAME))
						.smallDataset(true)
						.build();
	}

	private EntityDefinition specialty() {
		return Specialty.TYPE.define(
										Specialty.ID.define()
														.primaryKey()
														.generator(identity()),
										Specialty.NAME.define()
														.column()
														.caption("Name")
														.searchable(true)
														.maximumLength(80)
														.nullable(false))
						.caption("Specialties")
						.formatter(Specialty.NAME)
						.smallDataset(true)
						.build();
	}

	private EntityDefinition vetSpecialty() {
		return VetSpecialty.TYPE.define(
										VetSpecialty.VET.define()
														.primaryKey(0)
														.updatable(true),
										VetSpecialty.SPECIALTY.define()
														.primaryKey(1)
														.updatable(true),
										VetSpecialty.VET_FK.define()
														.foreignKey()
														.caption("Vet"),
										VetSpecialty.SPECIALTY_FK.define()
														.foreignKey()
														.caption("Specialty"))
						.caption("Vet specialties")
						.formatter(EntityFormatter.builder()
										.value(VetSpecialty.VET_FK)
										.text(" - ")
										.value(VetSpecialty.SPECIALTY_FK)
										.build())
						.build();
	}

	private EntityDefinition petType() {
		return PetType.TYPE.define(
										PetType.ID.define()
														.primaryKey()
														.generator(identity()),
										PetType.NAME.define()
														.column()
														.caption("Name")
														.searchable(true)
														.maximumLength(80)
														.nullable(false))
						.caption("Pet types")
						.formatter(PetType.NAME)
						.orderBy(ascending(PetType.NAME))
						.smallDataset(true)
						.build();
	}

	private EntityDefinition owner() {
		return Owner.TYPE.define(
										Owner.ID.define()
														.primaryKey()
														.generator(identity()),
										Owner.FIRST_NAME.define()
														.column()
														.caption("First name")
														.searchable(true)
														.maximumLength(30)
														.nullable(false),
										Owner.LAST_NAME.define()
														.column()
														.caption("Last name")
														.searchable(true)
														.maximumLength(30)
														.nullable(false),
										Owner.ADDRESS.define()
														.column()
														.caption("Address")
														.maximumLength(255),
										Owner.CITY.define()
														.column()
														.caption("City")
														.maximumLength(80),
										Owner.TELEPHONE.define()
														.column()
														.caption("Telephone")
														.maximumLength(20),
										Owner.PHONE_TYPE.define()
														.column()
														.caption("Phone type")
														.nullable(false)
														.defaultValue(PhoneType.MOBILE)
														.converter(String.class, new PhoneTypeConverter()))
						.caption("Owners")
						.formatter(EntityFormatter.builder()
										.value(Owner.LAST_NAME)
										.text(", ")
										.value(Owner.FIRST_NAME)
										.build())
						.orderBy(ascending(Owner.LAST_NAME, Owner.FIRST_NAME))
						.build();
	}

	private static final class PhoneTypeConverter implements Converter<PhoneType, String> {

		@Override
		public String toColumnValue(PhoneType value, Statement statement) {
			return value.name();
		}

		@Override
		public PhoneType fromColumnValue(String columnValue) {
			return PhoneType.valueOf(columnValue);
		}
	}

	private EntityDefinition pet() {
		return Pet.TYPE.define(
										Pet.ID.define()
														.primaryKey()
														.generator(identity()),
										Pet.NAME.define()
														.column()
														.caption("Name")
														.searchable(true)
														.maximumLength(30)
														.nullable(false),
										Pet.BIRTH_DATE.define()
														.column()
														.caption("Birth date")
														.nullable(false),
										Pet.PET_TYPE_ID.define()
														.column()
														.nullable(false),
										Pet.PET_TYPE_FK.define()
														.foreignKey()
														.caption("Pet type"),
										Pet.OWNER_ID.define()
														.column()
														.nullable(false),
										Pet.OWNER_FK.define()
														.foreignKey()
														.caption("Owner"))
						.caption("Pets")
						.formatter(Pet.NAME)
						.orderBy(ascending(Pet.NAME))
						.build();
	}

	private EntityDefinition visit() {
		return Visit.TYPE.define(
										Visit.ID.define()
														.primaryKey()
														.generator(identity()),
										Visit.PET_ID.define()
														.column()
														.nullable(false),
										Visit.PET_FK.define()
														.foreignKey()
														.caption("Pet"),
										Visit.VISIT_DATE.define()
														.column()
														.caption("Date")
														.nullable(false),
										Visit.VET_ID.define()
														.column()
														.nullable(false),
										Visit.VET_FK.define()
														.foreignKey()
														.caption("Vet"),
										Visit.DESCRIPTION.define()
														.column()
														.caption("Description")
														.maximumLength(255))
						.orderBy(OrderBy.builder()
										.ascending(Visit.PET_ID)
										.descending(Visit.VISIT_DATE)
										.build())
						.caption("Visits")
						.build();
	}
}
