/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Attribute;

import java.time.LocalDate;

import static is.codion.framework.domain.property.Properties.attribute;

public interface Clinic {

  String T_VET = "petclinic.vet";
  Attribute<Integer> VET_ID = attribute("id");
  Attribute<String> VET_FIRST_NAME = attribute("first_name");
  Attribute<String> VET_LAST_NAME = attribute("last_name");

  String T_SPECIALTY = "petclinic.specialty";
  Attribute<Integer> SPECIALTY_ID = attribute("id");
  Attribute<String> SPECIALTY_NAME = attribute("name");

  String T_VET_SPECIALTY = "petclinic.vet_specialty";
  Attribute<Integer> VET_SPECIALTY_VET = attribute("vet");
  Attribute<Entity> VET_SPECIALTY_VET_FK = attribute("vet_fk");
  Attribute<Integer> VET_SPECIALTY_SPECIALTY = attribute("specialty");
  Attribute<Entity> VET_SPECIALTY_SPECIALTY_FK = attribute("specialty_fk");

  String T_PET_TYPE = "petclinic.pet_type";
  Attribute<Integer> PET_TYPE_ID = attribute("id");
  Attribute<String> PET_TYPE_NAME = attribute("name");

  String T_OWNER = "petclinic.owner";
  Attribute<Integer> OWNER_ID = attribute("id");
  Attribute<String> OWNER_FIRST_NAME = attribute("first_name");
  Attribute<String> OWNER_LAST_NAME = attribute("last_name");
  Attribute<String> OWNER_ADDRESS = attribute("address");
  Attribute<String> OWNER_CITY = attribute("city");
  Attribute<String> OWNER_TELEPHONE = attribute("telephone");

  String T_PET = "petclinic.pet";
  Attribute<Integer> PET_ID = attribute("id");
  Attribute<String> PET_NAME = attribute("name");
  Attribute<String> PET_BIRTH_DATE = attribute("birth_date");
  Attribute<Integer> PET_PET_TYPE_ID = attribute("type_id");
  Attribute<Entity> PET_PET_TYPE_FK = attribute("type_fk");
  Attribute<Integer> PET_OWNER_ID = attribute("owner_id");
  Attribute<Entity> PET_OWNER_FK = attribute("owner_fk");

  String T_VISIT = "petclinic.visit";
  Attribute<Integer> VISIT_ID = attribute("id");
  Attribute<Integer> VISIT_PET_ID = attribute("pet_id");
  Attribute<Entity> VISIT_PET_FK = attribute("pet_fk");
  Attribute<LocalDate> VISIT_DATE = attribute("date");
  Attribute<String> VISIT_DESCRIPTION = attribute("description");
}
