/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.time.LocalDate;

import static is.codion.framework.demos.petclinic.domain.api.PetclinicApi.DOMAIN;

public interface Visit extends Entity {
  EntityType TYPE = DOMAIN.entityType("petclinic.visit", Visit.class);

  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<Integer> PET_ID = TYPE.integerAttribute("pet_id");
  Attribute<LocalDate> DATE = TYPE.localDateAttribute("\"date\"");
  Attribute<String> DESCRIPTION = TYPE.stringAttribute("description");

  ForeignKey PET_FK = TYPE.foreignKey("pet_fk", PET_ID, Pet.ID);
}
