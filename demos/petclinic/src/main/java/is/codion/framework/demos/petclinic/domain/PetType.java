/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.domain.entity.EntityType.entityType;

public interface PetType {
  EntityType TYPE = entityType("petclinic.pet_type");
  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<String> NAME = TYPE.stringAttribute("name");
}
