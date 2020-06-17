/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.demos.petclinic.domain.api.PetClinicApi.DOMAIN;

public interface VetSpecialty extends Entity {
  EntityType<VetSpecialty> TYPE = DOMAIN.entityType("petclinic.vet_specialty", VetSpecialty.class);
  Attribute<Integer> VET = TYPE.integerAttribute("vet");
  Attribute<Entity> VET_FK = TYPE.entityAttribute("vet_fk");
  Attribute<Integer> SPECIALTY = TYPE.integerAttribute("specialty");
  Attribute<Entity> SPECIALTY_FK = TYPE.entityAttribute("specialty_fk");
}
