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

public interface Pet extends Entity {
  EntityType TYPE = DOMAIN.entityType("petclinic.pet", Pet.class);

  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<String> NAME = TYPE.stringAttribute("name");
  Attribute<LocalDate> BIRTH_DATE = TYPE.localDateAttribute("birth_date");
  Attribute<Integer> PET_TYPE_ID = TYPE.integerAttribute("type_id");
  Attribute<Integer> OWNER_ID = TYPE.integerAttribute("owner_id");

  ForeignKey PET_TYPE_FK = TYPE.foreignKey("type_fk", PET_TYPE_ID, PetType.ID);
  ForeignKey OWNER_FK = TYPE.foreignKey("owner_fk", OWNER_ID, Owner.ID);
}
