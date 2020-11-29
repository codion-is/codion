/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKeyAttribute;

import static is.codion.framework.demos.petclinic.domain.api.PetClinicApi.DOMAIN;

public interface VetSpecialty extends Entity {
  EntityType<VetSpecialty> TYPE = DOMAIN.entityType("petclinic.vet_specialty", VetSpecialty.class);
  Attribute<Integer> VET = TYPE.integerAttribute("vet");
  ForeignKeyAttribute VET_FK = TYPE.foreignKey("vet_fk", VetSpecialty.VET, Vet.ID);
  Attribute<Integer> SPECIALTY = TYPE.integerAttribute("specialty");
  ForeignKeyAttribute SPECIALTY_FK = TYPE.foreignKey("specialty_fk", VetSpecialty.SPECIALTY, Specialty.ID);
}
