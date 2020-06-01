/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.domain.entity.Identity;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.EntityAttribute;

import java.time.LocalDate;

import static is.codion.framework.domain.entity.Identity.identity;
import static is.codion.framework.domain.property.Attributes.*;

public interface Clinic {

  Identity T_VET = identity("petclinic.vet");
  Attribute<Integer> VET_ID = integerAttribute("id", T_VET);
  Attribute<String> VET_FIRST_NAME = stringAttribute("first_name", T_VET);
  Attribute<String> VET_LAST_NAME = stringAttribute("last_name", T_VET);

  Identity T_SPECIALTY = identity("petclinic.specialty");
  Attribute<Integer> SPECIALTY_ID = integerAttribute("id", T_SPECIALTY);
  Attribute<String> SPECIALTY_NAME = stringAttribute("name", T_SPECIALTY);

  Identity T_VET_SPECIALTY = identity("petclinic.vet_specialty");
  Attribute<Integer> VET_SPECIALTY_VET = integerAttribute("vet", T_VET_SPECIALTY);
  EntityAttribute VET_SPECIALTY_VET_FK = entityAttribute("vet_fk", T_VET_SPECIALTY);
  Attribute<Integer> VET_SPECIALTY_SPECIALTY = integerAttribute("specialty", T_VET_SPECIALTY);
  EntityAttribute VET_SPECIALTY_SPECIALTY_FK = entityAttribute("specialty_fk", T_VET_SPECIALTY);

  Identity T_PET_TYPE = identity("petclinic.pet_type");
  Attribute<Integer> PET_TYPE_ID = integerAttribute("id", T_PET_TYPE);
  Attribute<String> PET_TYPE_NAME = stringAttribute("name", T_PET_TYPE);

  Identity T_OWNER = identity("petclinic.owner");
  Attribute<Integer> OWNER_ID = integerAttribute("id", T_OWNER);
  Attribute<String> OWNER_FIRST_NAME = stringAttribute("first_name", T_OWNER);
  Attribute<String> OWNER_LAST_NAME = stringAttribute("last_name", T_OWNER);
  Attribute<String> OWNER_ADDRESS = stringAttribute("address", T_OWNER);
  Attribute<String> OWNER_CITY = stringAttribute("city", T_OWNER);
  Attribute<String> OWNER_TELEPHONE = stringAttribute("telephone", T_OWNER);

  Identity T_PET = identity("petclinic.pet");
  Attribute<Integer> PET_ID = integerAttribute("id", T_PET);
  Attribute<String> PET_NAME = stringAttribute("name", T_PET);
  Attribute<LocalDate> PET_BIRTH_DATE = localDateAttribute("birth_date", T_PET);
  Attribute<Integer> PET_PET_TYPE_ID = integerAttribute("type_id", T_PET);
  EntityAttribute PET_PET_TYPE_FK = entityAttribute("type_fk", T_PET);
  Attribute<Integer> PET_OWNER_ID = integerAttribute("owner_id", T_PET);
  EntityAttribute PET_OWNER_FK = entityAttribute("owner_fk", T_PET);

  Identity T_VISIT = identity("petclinic.visit");
  Attribute<Integer> VISIT_ID = integerAttribute("id", T_VISIT);
  Attribute<Integer> VISIT_PET_ID = integerAttribute("pet_id", T_VISIT);
  EntityAttribute VISIT_PET_FK = entityAttribute("pet_fk", T_VISIT);
  Attribute<LocalDate> VISIT_DATE = localDateAttribute("date", T_VISIT);
  Attribute<String> VISIT_DESCRIPTION = stringAttribute("description", T_VISIT);
}
