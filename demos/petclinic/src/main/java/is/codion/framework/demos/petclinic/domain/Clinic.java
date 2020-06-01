/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;

import java.time.LocalDate;

import static is.codion.framework.domain.entity.Entities.entityIdentity;

public interface Clinic {

  Entity.Identity T_VET = entityIdentity("petclinic.vet");
  Attribute<Integer> VET_ID = T_VET.integerAttribute("id");
  Attribute<String> VET_FIRST_NAME = T_VET.stringAttribute("first_name");
  Attribute<String> VET_LAST_NAME = T_VET.stringAttribute("last_name");

  Entity.Identity T_SPECIALTY = entityIdentity("petclinic.specialty");
  Attribute<Integer> SPECIALTY_ID = T_SPECIALTY.integerAttribute("id");
  Attribute<String> SPECIALTY_NAME = T_SPECIALTY.stringAttribute("name");

  Entity.Identity T_VET_SPECIALTY = entityIdentity("petclinic.vet_specialty");
  Attribute<Integer> VET_SPECIALTY_VET = T_VET_SPECIALTY.integerAttribute("vet");
  Attribute<Entity> VET_SPECIALTY_VET_FK = T_VET_SPECIALTY.entityAttribute("vet_fk");
  Attribute<Integer> VET_SPECIALTY_SPECIALTY = T_VET_SPECIALTY.integerAttribute("specialty");
  Attribute<Entity> VET_SPECIALTY_SPECIALTY_FK = T_VET_SPECIALTY.entityAttribute("specialty_fk");

  Entity.Identity T_PET_TYPE = entityIdentity("petclinic.pet_type");
  Attribute<Integer> PET_TYPE_ID = T_PET_TYPE.integerAttribute("id");
  Attribute<String> PET_TYPE_NAME = T_PET_TYPE.stringAttribute("name");

  Entity.Identity T_OWNER = entityIdentity("petclinic.owner");
  Attribute<Integer> OWNER_ID = T_OWNER.integerAttribute("id");
  Attribute<String> OWNER_FIRST_NAME = T_OWNER.stringAttribute("first_name");
  Attribute<String> OWNER_LAST_NAME = T_OWNER.stringAttribute("last_name");
  Attribute<String> OWNER_ADDRESS = T_OWNER.stringAttribute("address");
  Attribute<String> OWNER_CITY = T_OWNER.stringAttribute("city");
  Attribute<String> OWNER_TELEPHONE = T_OWNER.stringAttribute("telephone");

  Entity.Identity T_PET = entityIdentity("petclinic.pet");
  Attribute<Integer> PET_ID = T_PET.integerAttribute("id");
  Attribute<String> PET_NAME = T_PET.stringAttribute("name");
  Attribute<LocalDate> PET_BIRTH_DATE = T_PET.localDateAttribute("birth_date");
  Attribute<Integer> PET_PET_TYPE_ID = T_PET.integerAttribute("type_id");
  Attribute<Entity> PET_PET_TYPE_FK = T_PET.entityAttribute("type_fk");
  Attribute<Integer> PET_OWNER_ID = T_PET.integerAttribute("owner_id");
  Attribute<Entity> PET_OWNER_FK = T_PET.entityAttribute("owner_fk");

  Entity.Identity T_VISIT = entityIdentity("petclinic.visit");
  Attribute<Integer> VISIT_ID = T_VISIT.integerAttribute("id");
  Attribute<Integer> VISIT_PET_ID = T_VISIT.integerAttribute("pet_id");
  Attribute<Entity> VISIT_PET_FK = T_VISIT.entityAttribute("pet_fk");
  Attribute<LocalDate> VISIT_DATE = T_VISIT.localDateAttribute("date");
  Attribute<String> VISIT_DESCRIPTION = T_VISIT.stringAttribute("description");
}
