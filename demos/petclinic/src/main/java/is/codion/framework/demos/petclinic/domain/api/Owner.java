/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.demos.petclinic.domain.api.Petclinic.DOMAIN;

public interface Owner extends Entity {
  EntityType TYPE = DOMAIN.entityType("petclinic.owner", Owner.class);

  Attribute<Integer> ID = TYPE.integerAttribute("id");
  Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
  Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
  Attribute<String> ADDRESS = TYPE.stringAttribute("address");
  Attribute<String> CITY = TYPE.stringAttribute("city");
  Attribute<String> TELEPHONE = TYPE.stringAttribute("telephone");
  Attribute<PhoneType> PHONE_TYPE = TYPE.attribute("phone_type", PhoneType.class);

  enum PhoneType {
    MOBILE, HOME, WORK
  }
}
