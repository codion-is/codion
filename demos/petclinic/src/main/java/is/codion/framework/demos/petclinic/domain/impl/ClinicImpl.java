/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.impl;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;

import java.sql.Types;

import static is.codion.framework.demos.petclinic.domain.Clinic.*;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;

public final class ClinicImpl extends Domain {

  public ClinicImpl() {
    vet();
    specialty();
    vetSpecialty();
    petType();
    owner();
    pet();
    visit();
  }

  private void vet() {
    define(T_VET,
            primaryKeyProperty(VET_ID, Types.INTEGER),
            columnProperty(VET_FIRST_NAME, Types.VARCHAR, "First name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(VET_LAST_NAME, Types.VARCHAR, "Last name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false))
            .keyGenerator(automatic(T_VET))
            .caption("Vets")
            .stringProvider(new StringProvider(VET_LAST_NAME)
                    .addText(", ").addValue(VET_FIRST_NAME))
            .orderBy(orderBy().ascending(VET_LAST_NAME, VET_FIRST_NAME))
            .smallDataset(true);
  }

  private void specialty() {
    define(T_SPECIALTY,
            primaryKeyProperty(SPECIALTY_ID, Types.INTEGER),
            columnProperty(SPECIALTY_NAME, Types.VARCHAR, "Name")
                    .searchProperty(true)
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(automatic(T_SPECIALTY))
            .caption("Specialties")
            .stringProvider(new StringProvider(SPECIALTY_NAME))
            .smallDataset(true);
  }

  private void vetSpecialty() {
    define(T_VET_SPECIALTY,
            foreignKeyProperty(VET_SPECIALTY_VET_FK, "Vet", T_VET,
                    columnProperty(VET_SPECIALTY_VET, Types.INTEGER)
                            .primaryKeyIndex(0))
                    .nullable(false),
            foreignKeyProperty(VET_SPECIALTY_SPECIALTY_FK, "Specialty", T_SPECIALTY,
                    primaryKeyProperty(VET_SPECIALTY_SPECIALTY, Types.INTEGER)
                            .primaryKeyIndex(1))
                    .nullable(false))
            .caption("Vet specialties")
            .stringProvider(new StringProvider(VET_SPECIALTY_VET_FK).addText(" - ")
                    .addValue(VET_SPECIALTY_SPECIALTY_FK));
  }

  private void petType() {
    define(T_PET_TYPE,
            primaryKeyProperty(PET_TYPE_ID, Types.INTEGER),
            columnProperty(PET_TYPE_NAME, Types.VARCHAR, "Name")
                    .searchProperty(true)
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(automatic(T_PET_TYPE))
            .caption("Pet types")
            .stringProvider(new StringProvider(PET_TYPE_NAME))
            .orderBy(orderBy().ascending(PET_TYPE_NAME))
            .smallDataset(true);
  }

  private void owner() {
    define(T_OWNER,
            primaryKeyProperty(OWNER_ID, Types.INTEGER),
            columnProperty(OWNER_FIRST_NAME, Types.VARCHAR, "First name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(OWNER_LAST_NAME, Types.VARCHAR, "Last name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(OWNER_ADDRESS, Types.VARCHAR, "Address")
                    .maximumLength(255),
            columnProperty(OWNER_CITY, Types.VARCHAR, "City")
                    .maximumLength(80),
            columnProperty(OWNER_TELEPHONE, Types.VARCHAR, "Telephone")
                    .maximumLength(20))
            .keyGenerator(automatic(T_OWNER))
            .caption("Owners")
            .stringProvider(new StringProvider(OWNER_LAST_NAME).addText(", ")
                    .addValue(OWNER_FIRST_NAME))
            .orderBy(orderBy().ascending(OWNER_LAST_NAME, OWNER_FIRST_NAME));
  }

  private void pet() {
    define(T_PET,
            primaryKeyProperty(PET_ID, Types.INTEGER),
            columnProperty(PET_NAME, Types.VARCHAR, "Name")
                    .searchProperty(true)
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(PET_BIRTH_DATE, Types.DATE, "Birth date"),
            foreignKeyProperty(PET_PET_TYPE_FK, "Pet type", T_PET_TYPE,
                    columnProperty(PET_PET_TYPE_ID, Types.INTEGER))
                    .nullable(false),
            foreignKeyProperty(PET_OWNER_FK, "Owner", T_OWNER,
                    columnProperty(PET_OWNER_ID, Types.INTEGER))
                    .nullable(false))
            .keyGenerator(automatic(T_PET))
            .caption("Pets")
            .stringProvider(new StringProvider(PET_NAME))
            .orderBy(orderBy().ascending(PET_NAME));
  }

  private void visit() {
    define(T_VISIT,
            primaryKeyProperty(VISIT_ID, Types.INTEGER),
            foreignKeyProperty(VISIT_PET_FK, "Pet", T_PET,
                    columnProperty(VISIT_PET_ID, Types.INTEGER))
                    .nullable(false),
            columnProperty(VISIT_DATE, Types.DATE, "Date")
                    .nullable(false),
            columnProperty(VISIT_DESCRIPTION, Types.VARCHAR, "Description")
                    .maximumLength(255))
            .keyGenerator(automatic(T_VISIT))
            .orderBy(orderBy().ascending(VISIT_PET_ID).descending(VISIT_DATE))
            .caption("Visits");
  }
}
