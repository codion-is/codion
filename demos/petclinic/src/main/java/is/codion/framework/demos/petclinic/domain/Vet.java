/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.domain.entity.EntityType.entityType;

public interface Vet {
  EntityType TYPE = entityType("petclinic.vet");
  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
  Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
}
