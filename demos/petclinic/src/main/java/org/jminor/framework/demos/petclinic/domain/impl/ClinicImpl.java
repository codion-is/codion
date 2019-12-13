/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.domain.impl;

import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.StringProvider;

import java.sql.Types;

import static org.jminor.framework.demos.petclinic.domain.Clinic.*;
import static org.jminor.framework.domain.KeyGenerators.automatic;
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
                    .setMaxLength(30)
                    .setNullable(false),
            columnProperty(VET_LAST_NAME, Types.VARCHAR, "Last name")
                    .setMaxLength(30)
                    .setNullable(false))
            .setKeyGenerator(automatic(T_VET))
            .setCaption("Vets")
            .setSearchPropertyIds(VET_FIRST_NAME, VET_LAST_NAME)
            .setStringProvider(new StringProvider(VET_LAST_NAME)
                    .addText(", ").addValue(VET_FIRST_NAME))
            .setOrderBy(orderBy().ascending(VET_LAST_NAME, VET_FIRST_NAME))
            .setSmallDataset(true);
  }

  private void specialty() {
    define(T_SPECIALTY,
            primaryKeyProperty(SPECIALTY_ID),
            columnProperty(SPECIALTY_NAME, Types.VARCHAR, "Name")
                    .setMaxLength(80)
                    .setNullable(false))
            .setKeyGenerator(automatic(T_SPECIALTY))
            .setCaption("Specialties")
            .setSearchPropertyIds(SPECIALTY_NAME)
            .setStringProvider(new StringProvider(SPECIALTY_NAME))
            .setSmallDataset(true);
  }

  private void vetSpecialty() {
    define(T_VET_SPECIALTY,
            foreignKeyProperty(VET_SPECIALTY_VET_FK, "Vet", T_VET,
                    columnProperty(VET_SPECIALTY_VET)
                            .setPrimaryKeyIndex(0))
                    .setNullable(false),
            foreignKeyProperty(VET_SPECIALTY_SPECIALTY_FK, "Specialty", T_SPECIALTY,
                    primaryKeyProperty(VET_SPECIALTY_SPECIALTY)
                            .setPrimaryKeyIndex(1))
                    .setNullable(false))
            .setCaption("Vet specialties")
            .setStringProvider(new StringProvider(VET_SPECIALTY_VET_FK).addText(" - ")
                    .addValue(VET_SPECIALTY_SPECIALTY_FK));
  }

  private void petType() {
    define(T_PET_TYPE,
            primaryKeyProperty(PET_TYPE_ID),
            columnProperty(PET_TYPE_NAME, Types.VARCHAR, "Name")
                    .setMaxLength(80)
                    .setNullable(false))
            .setKeyGenerator(automatic(T_PET_TYPE))
            .setCaption("Pet types")
            .setSearchPropertyIds(PET_TYPE_NAME)
            .setStringProvider(new StringProvider(PET_TYPE_NAME))
            .setOrderBy(orderBy().ascending(PET_TYPE_NAME))
            .setSmallDataset(true);
  }

  private void owner() {
    define(T_OWNER,
            primaryKeyProperty(OWNER_ID),
            columnProperty(OWNER_FIRST_NAME, Types.VARCHAR, "First name")
                    .setMaxLength(30)
                    .setNullable(false),
            columnProperty(OWNER_LAST_NAME, Types.VARCHAR, "Last name")
                    .setMaxLength(30)
                    .setNullable(false),
            columnProperty(OWNER_ADDRESS, Types.VARCHAR, "Address")
                    .setMaxLength(255),
            columnProperty(OWNER_CITY, Types.VARCHAR, "City")
                    .setMax(80),
            columnProperty(OWNER_TELEPHONE, Types.VARCHAR, "Telephone")
                    .setMaxLength(20))
            .setKeyGenerator(automatic(T_OWNER))
            .setCaption("Owners")
            .setSearchPropertyIds(OWNER_FIRST_NAME, OWNER_LAST_NAME)
            .setStringProvider(new StringProvider(OWNER_LAST_NAME).addText(", ")
                    .addValue(OWNER_FIRST_NAME))
            .setOrderBy(orderBy().ascending(OWNER_LAST_NAME, OWNER_FIRST_NAME));
  }

  private void pet() {
    define(T_PET,
            primaryKeyProperty(PET_ID),
            columnProperty(PET_NAME, Types.VARCHAR, "Name")
                    .setMaxLength(30)
                    .setNullable(false),
            columnProperty(PET_BIRTH_DATE, Types.DATE, "Birth date"),
            foreignKeyProperty(PET_PET_TYPE_FK, "Pet type", T_PET_TYPE,
                    columnProperty(PET_PET_TYPE_ID))
                    .setNullable(false),
            foreignKeyProperty(PET_OWNER_FK, "Owner", T_OWNER,
                    columnProperty(PET_OWNER_ID))
                    .setNullable(false))
            .setKeyGenerator(automatic(T_PET))
            .setCaption("Pets")
            .setSearchPropertyIds(PET_NAME)
            .setStringProvider(new StringProvider(PET_NAME))
            .setOrderBy(orderBy().ascending(PET_NAME));
  }

  private void visit() {
    define(T_VISIT,
            primaryKeyProperty(VISIT_ID),
            foreignKeyProperty(VISIT_PET_FK, "Pet", T_PET,
                    columnProperty(VISIT_PET_ID))
                    .setNullable(false),
            columnProperty(VISIT_DATE, Types.DATE, "Date")
                    .setNullable(false),
            columnProperty(VISIT_DESCRIPTION, Types.VARCHAR, "Description")
                    .setMaxLength(255))
            .setKeyGenerator(automatic(T_VISIT))
            .setOrderBy(orderBy().ascending(VISIT_PET_ID).descending(VISIT_DATE))
            .setCaption("Visits");
  }
}
