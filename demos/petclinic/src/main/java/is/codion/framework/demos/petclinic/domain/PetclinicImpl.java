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
import is.codion.framework.domain.entity.attribute.Column;

import java.sql.Statement;

import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.framework.domain.entity.OrderBy.ascending;

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
    add(Vet.TYPE.define(
            Vet.ID.primaryKey(),
            Vet.FIRST_NAME.column("First name")
                    .searchColumn(true)
                    .maximumLength(30)
                    .nullable(false),
            Vet.LAST_NAME.column("Last name")
                    .searchColumn(true)
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
    add(Specialty.TYPE.define(
            Specialty.ID.primaryKey(),
            Specialty.NAME.column("Name")
                    .searchColumn(true)
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(identity())
            .caption("Specialties")
            .stringFactory(Specialty.NAME)
            .smallDataset(true));
  }

  private void vetSpecialty() {
    add(VetSpecialty.TYPE.define(
            VetSpecialty.VET.primaryKey()
                    .primaryKeyIndex(0),
            VetSpecialty.SPECIALTY.column()
                    .primaryKeyIndex(1),
            VetSpecialty.VET_FK.foreignKey("Vet"),
            VetSpecialty.SPECIALTY_FK.foreignKey("Specialty"))
            .caption("Vet specialties")
            .stringFactory(StringFactory.builder()
                    .value(VetSpecialty.VET_FK)
                    .text(" - ")
                    .value(VetSpecialty.SPECIALTY_FK)
                    .build()));
  }

  private void petType() {
    add(PetType.TYPE.define(
            PetType.ID.primaryKey(),
            PetType.NAME.column("Name")
                    .searchColumn(true)
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(identity())
            .caption("Pet types")
            .stringFactory(PetType.NAME)
            .orderBy(ascending(PetType.NAME))
            .smallDataset(true));
  }

  private void owner() {
    add(Owner.TYPE.define(
            Owner.ID.primaryKey(),
            Owner.FIRST_NAME.column("First name")
                    .searchColumn(true)
                    .maximumLength(30)
                    .nullable(false),
            Owner.LAST_NAME.column("Last name")
                    .searchColumn(true)
                    .maximumLength(30)
                    .nullable(false),
            Owner.ADDRESS.column("Address")
                    .maximumLength(255),
            Owner.CITY.column("City")
                    .maximumLength(80),
            Owner.TELEPHONE.column("Telephone")
                    .maximumLength(20),
            Owner.PHONE_TYPE.column("Phone type")
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

  private static final class PhoneTypeValueConverter implements Column.ValueConverter<PhoneType, String> {

    @Override
    public String toColumnValue(PhoneType value, Statement statement) {
      return value.name();
    }

    @Override
    public PhoneType fromColumnValue(String columnValue) {
      return PhoneType.valueOf(columnValue);
    }
  }

  private void pet() {
    add(Pet.TYPE.define(
            Pet.ID.primaryKey(),
            Pet.NAME.column("Name")
                    .searchColumn(true)
                    .maximumLength(30)
                    .nullable(false),
            Pet.BIRTH_DATE.column("Birth date")
                    .nullable(false),
            Pet.PET_TYPE_ID.column()
                    .nullable(false),
            Pet.PET_TYPE_FK.foreignKey("Pet type"),
            Pet.OWNER_ID.column()
                    .nullable(false),
            Pet.OWNER_FK.foreignKey("Owner"))
            .keyGenerator(identity())
            .caption("Pets")
            .stringFactory(Pet.NAME)
            .orderBy(ascending(Pet.NAME)));
  }

  private void visit() {
    add(Visit.TYPE.define(
            Visit.ID.primaryKey(),
            Visit.PET_ID.column()
                    .nullable(false),
            Visit.PET_FK.foreignKey("Pet"),
            Visit.VISIT_DATE.column("Date")
                    .nullable(false),
            Visit.DESCRIPTION.column("Description")
                    .maximumLength(255))
            .keyGenerator(identity())
            .orderBy(OrderBy.builder()
                    .ascending(Visit.PET_ID)
                    .descending(Visit.VISIT_DATE)
                    .build())
            .caption("Visits"));
  }
}
