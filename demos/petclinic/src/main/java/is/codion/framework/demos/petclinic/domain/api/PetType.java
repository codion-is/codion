/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.demos.petclinic.domain.api.PetclinicApi.DOMAIN;

public interface PetType extends Entity {
  EntityType TYPE = DOMAIN.entityType("petclinic.pet_type", PetType.class);

  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<String> NAME = TYPE.stringAttribute("name");
}
