/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petclinic.domain.api.VetSpecialty;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.framework.model.SwingEntityEditModel;

import static is.codion.framework.db.condition.Conditions.condition;

public final class VetSpecialtyEditModel extends SwingEntityEditModel {

  public VetSpecialtyEditModel(EntityConnectionProvider connectionProvider) {
    super(VetSpecialty.TYPE, connectionProvider);
    setPersistValue(VetSpecialty.VET_FK, false);
    setPersistValue(VetSpecialty.SPECIALTY_FK, false);
  }

  @Override
  public boolean isEntityNew() {
    return getEntity().getOriginalPrimaryKey().isNull();
  }

  @Override
  public void validate(Entity entity) throws ValidationException {
    super.validate(entity);
    try {
      int rowCount = getConnectionProvider().getConnection().rowCount(
              condition(VetSpecialty.SPECIALTY).equalTo(entity.get(VetSpecialty.SPECIALTY))
                      .and(condition(VetSpecialty.VET).equalTo(entity.get(VetSpecialty.VET))));
      if (rowCount > 0) {
        throw new ValidationException(VetSpecialty.SPECIALTY_FK,
                entity.get(VetSpecialty.SPECIALTY_FK), "Vet/specialty combination already exists");
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
