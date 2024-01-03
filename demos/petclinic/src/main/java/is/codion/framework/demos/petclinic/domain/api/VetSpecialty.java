/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.io.Serializable;
import java.util.function.Predicate;

import static is.codion.framework.demos.petclinic.domain.api.Petclinic.DOMAIN;

public interface VetSpecialty {
  EntityType TYPE = DOMAIN.entityType("petclinic.vet_specialty");

  Column<Integer> VET = TYPE.integerColumn("vet");
  Column<Integer> SPECIALTY = TYPE.integerColumn("specialty");

  ForeignKey VET_FK = TYPE.foreignKey("vet_fk", VET, Vet.ID);
  ForeignKey SPECIALTY_FK = TYPE.foreignKey("specialty_fk", SPECIALTY, Specialty.ID);

  final class Exists implements Predicate<Entity>, Serializable {

    private static final long serialVersionUID = 1;

    @Override
    public boolean test(Entity entity) {
      return entity.originalPrimaryKey().isNotNull();
    }
  }
}
