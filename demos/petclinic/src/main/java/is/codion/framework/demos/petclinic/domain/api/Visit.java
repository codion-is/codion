/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKeyAttribute;

import java.time.LocalDate;

import static is.codion.framework.demos.petclinic.domain.api.PetClinicApi.DOMAIN;

public interface Visit extends Entity {
  EntityType<Visit> TYPE = DOMAIN.entityType("petclinic.visit", Visit.class);
  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<Integer> PET_ID = TYPE.integerAttribute("pet_id");
  ForeignKeyAttribute PET_FK = TYPE.foreignKey("pet_fk", Visit.PET_ID, Pet.ID);
  Attribute<LocalDate> DATE = TYPE.localDateAttribute("date");
  Attribute<String> DESCRIPTION = TYPE.stringAttribute("description");
}
