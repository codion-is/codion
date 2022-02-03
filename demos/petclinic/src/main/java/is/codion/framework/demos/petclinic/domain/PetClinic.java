/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.demos.petclinic.domain.api.Owner;
import is.codion.framework.demos.petclinic.domain.api.Pet;
import is.codion.framework.demos.petclinic.domain.api.PetClinicApi;
import is.codion.framework.demos.petclinic.domain.api.PetType;
import is.codion.framework.demos.petclinic.domain.api.Specialty;
import is.codion.framework.demos.petclinic.domain.api.Vet;
import is.codion.framework.demos.petclinic.domain.api.VetSpecialty;
import is.codion.framework.demos.petclinic.domain.api.Visit;
import is.codion.framework.domain.DefaultDomain;

import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
import static is.codion.framework.domain.property.Properties.*;

public final class PetClinic extends DefaultDomain {

  public PetClinic() {
    super(PetClinicApi.DOMAIN);
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
                    .searchProperty()
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(Vet.LAST_NAME, "Last name")
                    .searchProperty()
                    .maximumLength(30)
                    .nullable(false))
            .keyGenerator(identity())
            .caption("Vets")
            .stringFactory(stringFactory(Vet.LAST_NAME)
                    .text(", ").value(Vet.FIRST_NAME))
            .orderBy(orderBy().ascending(Vet.LAST_NAME, Vet.FIRST_NAME))
            .smallDataset();
  }

  private void specialty() {
    define(Specialty.TYPE,
            primaryKeyProperty(Specialty.ID),
            columnProperty(Specialty.NAME, "Name")
                    .searchProperty()
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(identity())
            .caption("Specialties")
            .stringFactory(stringFactory(Specialty.NAME))
            .smallDataset();
  }

  private void vetSpecialty() {
    define(VetSpecialty.TYPE,
            columnProperty(VetSpecialty.VET)
                    .primaryKeyIndex(0),
            columnProperty(VetSpecialty.SPECIALTY)
                    .primaryKeyIndex(1),
            foreignKeyProperty(VetSpecialty.VET_FK, "Vet"),
            foreignKeyProperty(VetSpecialty.SPECIALTY_FK, "Specialty"))
            .caption("Vet specialties")
            .stringFactory(stringFactory(VetSpecialty.VET_FK).text(" - ")
                    .value(VetSpecialty.SPECIALTY_FK));
  }

  private void petType() {
    define(PetType.TYPE,
            primaryKeyProperty(PetType.ID),
            columnProperty(PetType.NAME, "Name")
                    .searchProperty()
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(identity())
            .caption("Pet types")
            .stringFactory(stringFactory(PetType.NAME))
            .orderBy(orderBy().ascending(PetType.NAME))
            .smallDataset();
  }

  private void owner() {
    define(Owner.TYPE,
            primaryKeyProperty(Owner.ID),
            columnProperty(Owner.FIRST_NAME, "First name")
                    .searchProperty()
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(Owner.LAST_NAME, "Last name")
                    .searchProperty()
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(Owner.ADDRESS, "Address")
                    .maximumLength(255),
            columnProperty(Owner.CITY, "City")
                    .maximumLength(80),
            columnProperty(Owner.TELEPHONE, "Telephone")
                    .maximumLength(20))
            .keyGenerator(identity())
            .caption("Owners")
            .stringFactory(stringFactory(Owner.LAST_NAME).text(", ")
                    .value(Owner.FIRST_NAME))
            .orderBy(orderBy().ascending(Owner.LAST_NAME, Owner.FIRST_NAME));
  }

  private void pet() {
    define(Pet.TYPE,
            primaryKeyProperty(Pet.ID),
            columnProperty(Pet.NAME, "Name")
                    .searchProperty()
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(Pet.BIRTH_DATE, "Birth date")
                    .nullable(false),
            columnProperty(Pet.PET_TYPE_ID)
                    .nullable(false),
            foreignKeyProperty(Pet.PET_TYPE_FK, "Pet type"),
            columnProperty(Pet.OWNER_ID)
                    .nullable(false),
            foreignKeyProperty(Pet.OWNER_FK, "Owner"))
            .keyGenerator(identity())
            .caption("Pets")
            .stringFactory(stringFactory(Pet.NAME))
            .orderBy(orderBy().ascending(Pet.NAME));
  }

  private void visit() {
    define(Visit.TYPE,
            primaryKeyProperty(Visit.ID),
            columnProperty(Visit.PET_ID)
                    .nullable(false),
            foreignKeyProperty(Visit.PET_FK, "Pet"),
            columnProperty(Visit.DATE, "Date")
                    .nullable(false),
            columnProperty(Visit.DESCRIPTION, "Description")
                    .maximumLength(255))
            .keyGenerator(identity())
            .orderBy(orderBy().ascending(Visit.PET_ID).descending(Visit.DATE))
            .caption("Visits");
  }
}
