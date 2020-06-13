/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.domain.entity.EntityType.entityType;

public interface VetSpecialty {
  EntityType TYPE = entityType("petclinic.vet_specialty");
  Attribute<Integer> VET = TYPE.integerAttribute("vet");
  Attribute<Entity> VET_FK = TYPE.entityAttribute("vet_fk");
  Attribute<Integer> SPECIALTY = TYPE.integerAttribute("specialty");
  Attribute<Entity> SPECIALTY_FK = TYPE.entityAttribute("specialty_fk");
}
