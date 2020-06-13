/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petclinic.domain.VetSpecialty;
import is.codion.swing.framework.model.SwingEntityEditModel;

public final class VetSpecialtyEditModel extends SwingEntityEditModel {

  public VetSpecialtyEditModel(final EntityConnectionProvider connectionProvider) {
    super(VetSpecialty.TYPE, connectionProvider);
    setPersistValue(VetSpecialty.VET_FK, false);
    setPersistValue(VetSpecialty.SPECIALTY_FK, false);
  }

  @Override
  public boolean isEntityNew() {
    return getEntity().getOriginalKey().isNull();
  }
}
