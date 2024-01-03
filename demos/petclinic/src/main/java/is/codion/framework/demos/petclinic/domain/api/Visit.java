/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.time.LocalDate;

import static is.codion.framework.demos.petclinic.domain.api.Petclinic.DOMAIN;

public interface Visit {
  EntityType TYPE = DOMAIN.entityType("petclinic.visit");

  Column<Integer> ID = TYPE.integerColumn("id");
  Column<Integer> PET_ID = TYPE.integerColumn("pet_id");
  Column<LocalDate> VISIT_DATE = TYPE.localDateColumn("visit_date");
  Column<String> DESCRIPTION = TYPE.stringColumn("description");

  ForeignKey PET_FK = TYPE.foreignKey("pet_fk", PET_ID, Pet.ID);
}
