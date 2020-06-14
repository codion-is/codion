/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.time.LocalDate;

import static is.codion.framework.demos.petclinic.domain.api.PetClinicApi.DOMAIN;

public interface Visit {
  EntityType TYPE = DOMAIN.entityType("petclinic.visit");
  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<Integer> PET_ID = TYPE.integerAttribute("pet_id");
  Attribute<Entity> PET_FK = TYPE.entityAttribute("pet_fk");
  Attribute<LocalDate> DATE = TYPE.localDateAttribute("date");
  Attribute<String> DESCRIPTION = TYPE.stringAttribute("description");
}
