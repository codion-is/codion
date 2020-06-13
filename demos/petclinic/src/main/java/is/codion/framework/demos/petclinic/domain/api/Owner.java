/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.domain.entity.EntityType.entityType;

public interface Owner {
  EntityType TYPE = entityType("petclinic.owner");
  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
  Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
  Attribute<String> ADDRESS = TYPE.stringAttribute("address");
  Attribute<String> CITY = TYPE.stringAttribute("city");
  Attribute<String> TELEPHONE = TYPE.stringAttribute("telephone");
}
