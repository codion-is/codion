/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import static is.codion.framework.demos.petclinic.domain.api.Petclinic.DOMAIN;

public interface PetType {
  EntityType TYPE = DOMAIN.entityType("petclinic.pet_type");

  Column<Integer> ID = TYPE.integerColumn("id");
  Column<String> NAME = TYPE.stringColumn("name");
}
