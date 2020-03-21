/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.domain.impl;

import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.StringProvider;

import java.sql.Types;

import static org.jminor.framework.demos.petclinic.domain.Clinic.*;
import static org.jminor.framework.domain.entity.KeyGenerators.automatic;
import static org.jminor.framework.domain.property.Properties.*;

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
            primaryKeyProperty(VET_ID),
            columnProperty(VET_FIRST_NAME, Types.VARCHAR, "First name")
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(VET_LAST_NAME, Types.VARCHAR, "Last name")
                    .maximumLength(30)
                    .nullable(false))
            .keyGenerator(automatic(T_VET))
            .caption("Vets")
            .searchPropertyIds(VET_FIRST_NAME, VET_LAST_NAME)
            .stringProvider(new StringProvider(VET_LAST_NAME)
                    .addText(", ").addValue(VET_FIRST_NAME))
            .orderBy(orderBy().ascending(VET_LAST_NAME, VET_FIRST_NAME))
            .smallDataset(true);
  }

  private void specialty() {
    define(T_SPECIALTY,
            primaryKeyProperty(SPECIALTY_ID),
            columnProperty(SPECIALTY_NAME, Types.VARCHAR, "Name")
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(automatic(T_SPECIALTY))
            .caption("Specialties")
            .searchPropertyIds(SPECIALTY_NAME)
            .stringProvider(new StringProvider(SPECIALTY_NAME))
            .smallDataset(true);
  }

  private void vetSpecialty() {
    define(T_VET_SPECIALTY,
            foreignKeyProperty(VET_SPECIALTY_VET_FK, "Vet", T_VET,
                    columnProperty(VET_SPECIALTY_VET)
                            .primaryKeyIndex(0))
                    .nullable(false),
            foreignKeyProperty(VET_SPECIALTY_SPECIALTY_FK, "Specialty", T_SPECIALTY,
                    primaryKeyProperty(VET_SPECIALTY_SPECIALTY)
                            .primaryKeyIndex(1))
                    .nullable(false))
            .caption("Vet specialties")
            .stringProvider(new StringProvider(VET_SPECIALTY_VET_FK).addText(" - ")
                    .addValue(VET_SPECIALTY_SPECIALTY_FK));
  }

  private void petType() {
    define(T_PET_TYPE,
            primaryKeyProperty(PET_TYPE_ID),
            columnProperty(PET_TYPE_NAME, Types.VARCHAR, "Name")
                    .maximumLength(80)
                    .nullable(false))
            .keyGenerator(automatic(T_PET_TYPE))
            .caption("Pet types")
            .searchPropertyIds(PET_TYPE_NAME)
            .stringProvider(new StringProvider(PET_TYPE_NAME))
            .orderBy(orderBy().ascending(PET_TYPE_NAME))
            .smallDataset(true);
  }

  private void owner() {
    define(T_OWNER,
            primaryKeyProperty(OWNER_ID),
            columnProperty(OWNER_FIRST_NAME, Types.VARCHAR, "First name")
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(OWNER_LAST_NAME, Types.VARCHAR, "Last name")
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(OWNER_ADDRESS, Types.VARCHAR, "Address")
                    .maximumLength(255),
            columnProperty(OWNER_CITY, Types.VARCHAR, "City")
                    .maximumValue(80),
            columnProperty(OWNER_TELEPHONE, Types.VARCHAR, "Telephone")
                    .maximumLength(20))
            .keyGenerator(automatic(T_OWNER))
            .caption("Owners")
            .searchPropertyIds(OWNER_FIRST_NAME, OWNER_LAST_NAME)
            .stringProvider(new StringProvider(OWNER_LAST_NAME).addText(", ")
                    .addValue(OWNER_FIRST_NAME))
            .orderBy(orderBy().ascending(OWNER_LAST_NAME, OWNER_FIRST_NAME));
  }

  private void pet() {
    define(T_PET,
            primaryKeyProperty(PET_ID),
            columnProperty(PET_NAME, Types.VARCHAR, "Name")
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(PET_BIRTH_DATE, Types.DATE, "Birth date"),
            foreignKeyProperty(PET_PET_TYPE_FK, "Pet type", T_PET_TYPE,
                    columnProperty(PET_PET_TYPE_ID))
                    .nullable(false),
            foreignKeyProperty(PET_OWNER_FK, "Owner", T_OWNER,
                    columnProperty(PET_OWNER_ID))
                    .nullable(false))
            .keyGenerator(automatic(T_PET))
            .caption("Pets")
            .searchPropertyIds(PET_NAME)
            .stringProvider(new StringProvider(PET_NAME))
            .orderBy(orderBy().ascending(PET_NAME));
  }

  private void visit() {
    define(T_VISIT,
            primaryKeyProperty(VISIT_ID),
            foreignKeyProperty(VISIT_PET_FK, "Pet", T_PET,
                    columnProperty(VISIT_PET_ID))
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
