/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.time.LocalDate;

import static is.codion.framework.demos.petclinic.domain.api.Petclinic.DOMAIN;

public interface Visit {
  EntityType TYPE = DOMAIN.entityType("petclinic.visit");

  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<Integer> PET_ID = TYPE.integerAttribute("pet_id");
  Attribute<LocalDate> VISIT_DATE = TYPE.localDateAttribute("visit_date");
  Attribute<String> DESCRIPTION = TYPE.stringAttribute("description");

  ForeignKey PET_FK = TYPE.foreignKey("pet_fk", PET_ID, Pet.ID);
}
