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
import is.codion.framework.domain.entity.attribute.Column.Converter;

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
            Vet.ID.define()
                    .primaryKey(),
            Vet.FIRST_NAME.define()
                    .column()
                    .caption("First name")
                    .searchColumn(true)
                    .maximumLength(30)
                    .nullable(false),
            Vet.LAST_NAME.define()
                    .column()
                    .caption("Last name")
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
            Specialty.ID.define()
                    .primaryKey(),
            Specialty.NAME.define()
                    .column()
                    .caption("Name")
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
            .stringFactory(StringFactory.builder()
                    .value(VetSpecialty.VET_FK)
                    .text(" - ")
                    .value(VetSpecialty.SPECIALTY_FK)
                    .build())
            .exists(new VetSpecialty.Exists()));
  }

  private void petType() {
    add(PetType.TYPE.define(
            PetType.ID.define()
                    .primaryKey(),
            PetType.NAME.define()
                    .column()
                    .caption("Name")
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
            Owner.ID.define()
                    .primaryKey(),
            Owner.FIRST_NAME.define()
                    .column()
                    .caption("First name")
                    .searchColumn(true)
                    .maximumLength(30)
                    .nullable(false),
            Owner.LAST_NAME.define()
                    .column()
                    .caption("Last name")
                    .searchColumn(true)
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
                    .columnClass(String.class, new PhoneTypeConverter()))
            .keyGenerator(identity())
            .caption("Owners")
            .stringFactory(StringFactory.builder()
                    .value(Owner.LAST_NAME)
                    .text(", ")
                    .value(Owner.FIRST_NAME)
                    .build())
            .orderBy(ascending(Owner.LAST_NAME, Owner.FIRST_NAME)));
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

  private void pet() {
    add(Pet.TYPE.define(
            Pet.ID.define()
                    .primaryKey(),
            Pet.NAME.define()
                    .column()
                    .caption("Name")
                    .searchColumn(true)
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
            .keyGenerator(identity())
            .caption("Pets")
            .stringFactory(Pet.NAME)
            .orderBy(ascending(Pet.NAME)));
  }

  private void visit() {
    add(Visit.TYPE.define(
            Visit.ID.define()
                    .primaryKey(),
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
            Visit.DESCRIPTION.define()
                    .column()
                    .caption("Description")
                    .maximumLength(255))
            .keyGenerator(identity())
            .orderBy(OrderBy.builder()
                    .ascending(Visit.PET_ID)
                    .descending(Visit.VISIT_DATE)
                    .build())
            .caption("Visits"));
  }
}
