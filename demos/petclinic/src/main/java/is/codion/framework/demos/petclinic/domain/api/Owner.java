/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import static is.codion.framework.demos.petclinic.domain.api.Petclinic.DOMAIN;

public interface Owner {
  EntityType TYPE = DOMAIN.entityType("petclinic.owner");

  Column<Integer> ID = TYPE.integerColumn("id");
  Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
  Column<String> LAST_NAME = TYPE.stringColumn("last_name");
  Column<String> ADDRESS = TYPE.stringColumn("address");
  Column<String> CITY = TYPE.stringColumn("city");
  Column<String> TELEPHONE = TYPE.stringColumn("telephone");
  Column<PhoneType> PHONE_TYPE = TYPE.column("phone_type", PhoneType.class);

  enum PhoneType {
    MOBILE, HOME, WORK
  }
}
