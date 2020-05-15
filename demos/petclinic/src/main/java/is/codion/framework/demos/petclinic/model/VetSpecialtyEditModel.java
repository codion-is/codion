/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petclinic.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.swing.framework.model.SwingEntityEditModel;

import static dev.codion.framework.demos.petclinic.domain.Clinic.*;

public final class VetSpecialtyEditModel extends SwingEntityEditModel {

  public VetSpecialtyEditModel(final EntityConnectionProvider connectionProvider) {
    super(T_VET_SPECIALTY, connectionProvider);
    setPersistValue(VET_SPECIALTY_VET_FK, false);
    setPersistValue(VET_SPECIALTY_SPECIALTY_FK, false);
  }

  @Override
  public boolean isEntityNew() {
    return getEntity().getOriginalKey().isNull();
  }
}
