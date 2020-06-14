/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.demos.petclinic.domain.api.Clinic;
import is.codion.framework.demos.petclinic.domain.api.Owner;
import is.codion.framework.demos.petclinic.domain.api.Pet;
import is.codion.framework.demos.petclinic.domain.api.PetType;
import is.codion.framework.demos.petclinic.domain.api.Specialty;
import is.codion.framework.demos.petclinic.domain.api.Vet;
import is.codion.framework.demos.petclinic.domain.api.VetSpecialty;
import is.codion.framework.demos.petclinic.domain.api.Visit;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;

import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;

public final class PetClinic extends Domain {

  public PetClinic() {
    super(Clinic.DOMAIN);
    vet();
    specialty();
    vetSpecialty();
    petType();
    owner();
    pet();
    visit();
  }

  private void vet() {
    define(Vet.TYPE,
            primaryKeyProperty(Vet.ID),
            columnProperty(Vet.FIRST_NAME, "First name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(Vet.LAST_NAME, "Last name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false))
            .keyGenerator(automatic(Vet.TYPE.getName()))
            .caption("Vets")
            .stringProvider(new StringProvider(Vet.LAST_NAME)
                    .addText(", ").addValue(Vet.FIRST_NAME))
            .orderBy(orderBy().ascending(Vet.LAST_NAME, Vet.FIRST_NAME))
            .smallDataset(true);
  }

  private void specialty() {
    define(Specialty.TYPE,
            primaryKeyProperty(Specialty.ID),
            columnProperty(Specialty.NAME, "Name")
                    .searchProperty(true)
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(automatic(Specialty.TYPE.getName()))
            .caption("Specialties")
            .stringProvider(new StringProvider(Specialty.NAME))
            .smallDataset(true);
  }

  private void vetSpecialty() {
    define(VetSpecialty.TYPE,
            foreignKeyProperty(VetSpecialty.VET_FK, "Vet", Vet.TYPE,
                    columnProperty(VetSpecialty.VET)
                            .primaryKeyIndex(0))
                    .nullable(false),
            foreignKeyProperty(VetSpecialty.SPECIALTY_FK, "Specialty", Specialty.TYPE,
                    columnProperty(VetSpecialty.SPECIALTY)
                            .primaryKeyIndex(1))
                    .nullable(false))
            .caption("Vet specialties")
            .stringProvider(new StringProvider(VetSpecialty.VET_FK).addText(" - ")
                    .addValue(VetSpecialty.SPECIALTY_FK));
  }

  private void petType() {
    define(PetType.TYPE,
            primaryKeyProperty(PetType.ID),
            columnProperty(PetType.NAME, "Name")
                    .searchProperty(true)
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(automatic(PetType.TYPE.getName()))
            .caption("Pet types")
            .stringProvider(new StringProvider(PetType.NAME))
            .orderBy(orderBy().ascending(PetType.NAME))
            .smallDataset(true);
  }

  private void owner() {
    define(Owner.TYPE,
            primaryKeyProperty(Owner.ID),
            columnProperty(Owner.FIRST_NAME, "First name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(Owner.LAST_NAME, "Last name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(Owner.ADDRESS, "Address")
                    .maximumLength(255),
            columnProperty(Owner.CITY, "City")
                    .maximumLength(80),
            columnProperty(Owner.TELEPHONE, "Telephone")
                    .maximumLength(20))
            .keyGenerator(automatic(Owner.TYPE.getName()))
            .caption("Owners")
            .stringProvider(new StringProvider(Owner.LAST_NAME).addText(", ")
                    .addValue(Owner.FIRST_NAME))
            .orderBy(orderBy().ascending(Owner.LAST_NAME, Owner.FIRST_NAME));
  }

  private void pet() {
    define(Pet.TYPE,
            primaryKeyProperty(Pet.ID),
            columnProperty(Pet.NAME, "Name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(Pet.BIRTH_DATE, "Birth date")
                    .nullable(false),
            foreignKeyProperty(Pet.PET_TYPE_FK, "Pet type", PetType.TYPE,
                    columnProperty(Pet.PET_TYPE_ID))
                    .nullable(false),
            foreignKeyProperty(Pet.OWNER_FK, "Owner", Owner.TYPE,
                    columnProperty(Pet.OWNER_ID))
                    .nullable(false))
            .keyGenerator(automatic(Pet.TYPE.getName()))
            .caption("Pets")
            .stringProvider(new StringProvider(Pet.NAME))
            .orderBy(orderBy().ascending(Pet.NAME));
  }

  private void visit() {
    define(Visit.TYPE,
            primaryKeyProperty(Visit.ID),
            foreignKeyProperty(Visit.PET_FK, "Pet", Pet.TYPE,
                    columnProperty(Visit.PET_ID))
                    .nullable(false),
            columnProperty(Visit.DATE, "Date")
                    .nullable(false),
            columnProperty(Visit.DESCRIPTION, "Description")
                    .maximumLength(255))
            .keyGenerator(automatic(Visit.TYPE.getName()))
            .orderBy(orderBy().ascending(Visit.PET_ID).descending(Visit.DATE))
            .caption("Visits");
  }
}
