/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import static is.codion.framework.demos.petclinic.domain.api.Petclinic.DOMAIN;

public interface Vet {
  EntityType TYPE = DOMAIN.entityType("petclinic.vet");

  Column<Integer> ID = TYPE.integerColumn("id");
  Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
  Column<String> LAST_NAME = TYPE.stringColumn("last_name");
}
