/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.demos.petclinic.domain.api.Owner;
import is.codion.framework.demos.petclinic.domain.api.Owner.PhoneType;
import is.codion.framework.demos.petclinic.domain.api.Pet;
import is.codion.framework.demos.petclinic.domain.api.PetType;
import is.codion.framework.demos.petclinic.domain.api.Petclinic;
import is.codion.framework.demos.petclinic.domain.api.Specialty;
import is.codion.framework.demos.petclinic.domain.api.Vet;
import is.codion.framework.demos.petclinic.domain.api.VetSpecialty;
import is.codion.framework.demos.petclinic.domain.api.Visit;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.property.ColumnProperty.ValueConverter;

import java.sql.Statement;

import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.property.Property.*;

public final class PetclinicImpl extends DefaultDomain {

  public PetclinicImpl() {
    super(Petclinic.DOMAIN);
    vet();
    specialty();
    vetSpecialty();
    petType();
    owner();
    pet();
    visit();
  }

  private void vet() {
    add(definition(
            primaryKeyProperty(Vet.ID),
            columnProperty(Vet.FIRST_NAME, "First name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(Vet.LAST_NAME, "Last name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false))
            .keyGenerator(identity())
            .caption("Vets")
            .stringFactory(StringFactory.builder()
                    .value(Vet.LAST_NAME)
                    .text(", ")
                    .value(Vet.FIRST_NAME)
                    .build())
            .orderBy(ascending(Vet.LAST_NAME, Vet.FIRST_NAME))
            .smallDataset(true));
  }

  private void specialty() {
    add(definition(
            primaryKeyProperty(Specialty.ID),
            columnProperty(Specialty.NAME, "Name")
                    .searchProperty(true)
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(identity())
            .caption("Specialties")
            .stringFactory(Specialty.NAME)
            .smallDataset(true));
  }

  private void vetSpecialty() {
    add(definition(
            columnProperty(VetSpecialty.VET)
                    .primaryKeyIndex(0),
            columnProperty(VetSpecialty.SPECIALTY)
                    .primaryKeyIndex(1),
            foreignKeyProperty(VetSpecialty.VET_FK, "Vet"),
            foreignKeyProperty(VetSpecialty.SPECIALTY_FK, "Specialty"))
            .caption("Vet specialties")
            .stringFactory(StringFactory.builder()
                    .value(VetSpecialty.VET_FK)
                    .text(" - ")
                    .value(VetSpecialty.SPECIALTY_FK)
                    .build()));
  }

  private void petType() {
    add(definition(
            primaryKeyProperty(PetType.ID),
            columnProperty(PetType.NAME, "Name")
                    .searchProperty(true)
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(identity())
            .caption("Pet types")
            .stringFactory(PetType.NAME)
            .orderBy(ascending(PetType.NAME))
            .smallDataset(true));
  }

  private void owner() {
    add(definition(
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
                    .maximumLength(20),
            columnProperty(Owner.PHONE_TYPE, "Phone type")
                    .columnClass(String.class, new PhoneTypeValueConverter()))
            .keyGenerator(identity())
            .caption("Owners")
            .stringFactory(StringFactory.builder()
                    .value(Owner.LAST_NAME)
                    .text(", ")
                    .value(Owner.FIRST_NAME)
                    .build())
            .orderBy(ascending(Owner.LAST_NAME, Owner.FIRST_NAME)));
  }

  private void pet() {
    add(definition(
            primaryKeyProperty(Pet.ID),
            columnProperty(Pet.NAME, "Name")
                    .searchProperty(true)
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
            .stringFactory(Pet.NAME)
            .orderBy(ascending(Pet.NAME)));
  }

  private void visit() {
    add(definition(
            primaryKeyProperty(Visit.ID),
            columnProperty(Visit.PET_ID)
                    .nullable(false),
            foreignKeyProperty(Visit.PET_FK, "Pet"),
            columnProperty(Visit.DATE, "Date")
                    .nullable(false),
            columnProperty(Visit.DESCRIPTION, "Description")
                    .maximumLength(255))
            .keyGenerator(identity())
            .orderBy(OrderBy.builder()
                    .ascending(Visit.PET_ID)
                    .descending(Visit.DATE)
                    .build())
            .caption("Visits"));
  }

  private static final class PhoneTypeValueConverter implements ValueConverter<PhoneType, String> {

    @Override
    public String toColumnValue(PhoneType value, Statement statement) {
      return value.name();
    }

    @Override
    public PhoneType fromColumnValue(String columnValue) {
      return PhoneType.valueOf(columnValue);
    }
  }
}
