/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import static is.codion.framework.demos.petclinic.domain.api.Petclinic.DOMAIN;

public interface VetSpecialty {
  EntityType TYPE = DOMAIN.entityType("petclinic.vet_specialty");

  Column<Integer> VET = TYPE.integerColumn("vet");
  Column<Integer> SPECIALTY = TYPE.integerColumn("specialty");

  ForeignKey VET_FK = TYPE.foreignKey("vet_fk", VET, Vet.ID);
  ForeignKey SPECIALTY_FK = TYPE.foreignKey("specialty_fk", SPECIALTY, Specialty.ID);
}
