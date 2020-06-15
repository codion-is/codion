/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.time.LocalDate;

import static is.codion.framework.demos.petclinic.domain.api.PetClinicApi.DOMAIN;

public interface Pet extends Entity {
  EntityType<Pet> TYPE = DOMAIN.entityType("petclinic.pet", Pet.class);
  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<String> NAME = TYPE.stringAttribute("name");
  Attribute<LocalDate> BIRTH_DATE = TYPE.localDateAttribute("birth_date");
  Attribute<Integer> PET_TYPE_ID = TYPE.integerAttribute("type_id");
  Attribute<Entity> PET_TYPE_FK = TYPE.entityAttribute("type_fk");
  Attribute<Integer> OWNER_ID = TYPE.integerAttribute("owner_id");
  Attribute<Entity> OWNER_FK = TYPE.entityAttribute("owner_fk");
}
