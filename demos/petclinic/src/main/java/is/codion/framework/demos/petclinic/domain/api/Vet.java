/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.demos.petclinic.domain.api.Petclinic.DOMAIN;

public interface Vet {
  EntityType TYPE = DOMAIN.entityType("petclinic.vet");

  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
  Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
}
