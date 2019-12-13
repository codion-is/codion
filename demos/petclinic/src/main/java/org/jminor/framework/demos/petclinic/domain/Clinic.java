/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.domain;

public interface Clinic {

  String T_VET = "petclinic.vet";
  String VET_ID = "id";
  String VET_FIRST_NAME = "first_name";
  String VET_LAST_NAME = "last_name";

  String T_SPECIALTY = "petclinic.specialty";
  String SPECIALTY_ID = "id";
  String SPECIALTY_NAME = "name";

  String T_VET_SPECIALTY = "petclinic.vet_specialty";
  String VET_SPECIALTY_VET = "vet";
  String VET_SPECIALTY_VET_FK = "vet_fk";
  String VET_SPECIALTY_SPECIALTY = "specialty";
  String VET_SPECIALTY_SPECIALTY_FK = "specialty_fk";

  String T_PET_TYPE = "petclinic.pet_type";
  String PET_TYPE_ID = "id";
  String PET_TYPE_NAME = "name";

  String T_OWNER = "petclinic.owner";
  String OWNER_ID = "id";
  String OWNER_FIRST_NAME = "first_name";
  String OWNER_LAST_NAME = "last_name";
  String OWNER_ADDRESS = "address";
  String OWNER_CITY = "city";
  String OWNER_TELEPHONE = "telephone";

  String T_PET = "petclinic.pet";
  String PET_ID = "id";
  String PET_NAME = "name";
  String PET_BIRTH_DATE = "birth_date";
  String PET_PET_TYPE_ID = "type_id";
  String PET_PET_TYPE_FK = "type_fk";
  String PET_OWNER_ID = "owner_id";
  String PET_OWNER_FK = "owner_fk";

  String T_VISIT = "petclinic.visit";
  String VISIT_ID = "id";
  String VISIT_PET_ID = "pet_id";
  String VISIT_PET_FK = "pet_fk";
  String VISIT_DATE = "date";
  String VISIT_DESCRIPTION = "description";
}
