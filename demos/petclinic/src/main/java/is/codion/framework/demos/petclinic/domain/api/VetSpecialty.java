/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import static is.codion.framework.demos.petclinic.domain.api.PetClinicApi.DOMAIN;

public interface VetSpecialty extends Entity {
  EntityType TYPE = DOMAIN.entityType("petclinic.vet_specialty", VetSpecialty.class);

  Attribute<Integer> VET = TYPE.integerAttribute("vet");
  Attribute<Integer> SPECIALTY = TYPE.integerAttribute("specialty");

  ForeignKey VET_FK = TYPE.foreignKey("vet_fk", VET, Vet.ID);
  ForeignKey SPECIALTY_FK = TYPE.foreignKey("specialty_fk", SPECIALTY, Specialty.ID);
}
