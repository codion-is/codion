/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import static org.jminor.framework.demos.petclinic.domain.Clinic.*;

public final class VetSpecialtyEditModel extends SwingEntityEditModel {

  public VetSpecialtyEditModel(final EntityConnectionProvider connectionProvider) {
    super(T_VET_SPECIALTY, connectionProvider);
    setValuePersistent(VET_SPECIALTY_VET_FK, false);
    setValuePersistent(VET_SPECIALTY_SPECIALTY_FK, false);
  }

  @Override
  public boolean isEntityNew() {
    return getEntity().getOriginalKey().isNull();
  }
}
