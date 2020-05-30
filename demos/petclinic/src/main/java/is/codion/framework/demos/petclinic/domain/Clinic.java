/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.EntityAttribute;

import java.time.LocalDate;

import static is.codion.framework.domain.property.Properties.attribute;
import static is.codion.framework.domain.property.Properties.entityAttribute;
import static java.sql.Types.*;

public interface Clinic {

  String T_VET = "petclinic.vet";
  Attribute<Integer> VET_ID = attribute("id", INTEGER);
  Attribute<String> VET_FIRST_NAME = attribute("first_name", VARCHAR);
  Attribute<String> VET_LAST_NAME = attribute("last_name", VARCHAR);

  String T_SPECIALTY = "petclinic.specialty";
  Attribute<Integer> SPECIALTY_ID = attribute("id", INTEGER);
  Attribute<String> SPECIALTY_NAME = attribute("name", VARCHAR);

  String T_VET_SPECIALTY = "petclinic.vet_specialty";
  Attribute<Integer> VET_SPECIALTY_VET = attribute("vet", INTEGER);
  EntityAttribute VET_SPECIALTY_VET_FK = entityAttribute("vet_fk");
  Attribute<Integer> VET_SPECIALTY_SPECIALTY = attribute("specialty", INTEGER);
  EntityAttribute VET_SPECIALTY_SPECIALTY_FK = entityAttribute("specialty_fk");

  String T_PET_TYPE = "petclinic.pet_type";
  Attribute<Integer> PET_TYPE_ID = attribute("id", INTEGER);
  Attribute<String> PET_TYPE_NAME = attribute("name", VARCHAR);

  String T_OWNER = "petclinic.owner";
  Attribute<Integer> OWNER_ID = attribute("id", INTEGER);
  Attribute<String> OWNER_FIRST_NAME = attribute("first_name", VARCHAR);
  Attribute<String> OWNER_LAST_NAME = attribute("last_name", VARCHAR);
  Attribute<String> OWNER_ADDRESS = attribute("address", VARCHAR);
  Attribute<String> OWNER_CITY = attribute("city", VARCHAR);
  Attribute<String> OWNER_TELEPHONE = attribute("telephone", VARCHAR);

  String T_PET = "petclinic.pet";
  Attribute<Integer> PET_ID = attribute("id", INTEGER);
  Attribute<String> PET_NAME = attribute("name", VARCHAR);
  Attribute<LocalDate> PET_BIRTH_DATE = attribute("birth_date", DATE);
  Attribute<Integer> PET_PET_TYPE_ID = attribute("type_id", INTEGER);
  EntityAttribute PET_PET_TYPE_FK = entityAttribute("type_fk");
  Attribute<Integer> PET_OWNER_ID = attribute("owner_id", INTEGER);
  EntityAttribute PET_OWNER_FK = entityAttribute("owner_fk");

  String T_VISIT = "petclinic.visit";
  Attribute<Integer> VISIT_ID = attribute("id", INTEGER);
  Attribute<Integer> VISIT_PET_ID = attribute("pet_id", INTEGER);
  EntityAttribute VISIT_PET_FK = entityAttribute("pet_fk");
  Attribute<LocalDate> VISIT_DATE = attribute("date", DATE);
  Attribute<String> VISIT_DESCRIPTION = attribute("description", VARCHAR);
}
