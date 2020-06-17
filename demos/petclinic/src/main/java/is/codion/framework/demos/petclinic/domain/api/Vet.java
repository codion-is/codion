/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.demos.petclinic.domain.api.PetClinicApi.DOMAIN;

public interface Vet extends Entity {
  EntityType<Vet> TYPE = DOMAIN.entityType("petclinic.vet", Vet.class);
  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
  Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
}
