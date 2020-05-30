/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.EntityAttribute;

import java.time.LocalDate;

import static is.codion.framework.domain.property.Attributes.*;

public interface Clinic {

  String T_VET = "petclinic.vet";
  Attribute<Integer> VET_ID = integerAttribute("id");
  Attribute<String> VET_FIRST_NAME = stringAttribute("first_name");
  Attribute<String> VET_LAST_NAME = stringAttribute("last_name");

  String T_SPECIALTY = "petclinic.specialty";
  Attribute<Integer> SPECIALTY_ID = integerAttribute("id");
  Attribute<String> SPECIALTY_NAME = stringAttribute("name");

  String T_VET_SPECIALTY = "petclinic.vet_specialty";
  Attribute<Integer> VET_SPECIALTY_VET = integerAttribute("vet");
  EntityAttribute VET_SPECIALTY_VET_FK = entityAttribute("vet_fk");
  Attribute<Integer> VET_SPECIALTY_SPECIALTY = integerAttribute("specialty");
  EntityAttribute VET_SPECIALTY_SPECIALTY_FK = entityAttribute("specialty_fk");

  String T_PET_TYPE = "petclinic.pet_type";
  Attribute<Integer> PET_TYPE_ID = integerAttribute("id");
  Attribute<String> PET_TYPE_NAME = stringAttribute("name");

  String T_OWNER = "petclinic.owner";
  Attribute<Integer> OWNER_ID = integerAttribute("id");
  Attribute<String> OWNER_FIRST_NAME = stringAttribute("first_name");
  Attribute<String> OWNER_LAST_NAME = stringAttribute("last_name");
  Attribute<String> OWNER_ADDRESS = stringAttribute("address");
  Attribute<String> OWNER_CITY = stringAttribute("city");
  Attribute<String> OWNER_TELEPHONE = stringAttribute("telephone");

  String T_PET = "petclinic.pet";
  Attribute<Integer> PET_ID = integerAttribute("id");
  Attribute<String> PET_NAME = stringAttribute("name");
  Attribute<LocalDate> PET_BIRTH_DATE = localDateAttribute("birth_date");
  Attribute<Integer> PET_PET_TYPE_ID = integerAttribute("type_id");
  EntityAttribute PET_PET_TYPE_FK = entityAttribute("type_fk");
  Attribute<Integer> PET_OWNER_ID = integerAttribute("owner_id");
  EntityAttribute PET_OWNER_FK = entityAttribute("owner_fk");

  String T_VISIT = "petclinic.visit";
  Attribute<Integer> VISIT_ID = integerAttribute("id");
  Attribute<Integer> VISIT_PET_ID = integerAttribute("pet_id");
  EntityAttribute VISIT_PET_FK = entityAttribute("pet_fk");
  Attribute<LocalDate> VISIT_DATE = localDateAttribute("date");
  Attribute<String> VISIT_DESCRIPTION = stringAttribute("description");
}
