/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petclinic.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.swing.framework.model.SwingEntityApplicationModel;
import dev.codion.swing.framework.model.SwingEntityModel;

import static dev.codion.framework.demos.petclinic.domain.Clinic.*;

public final class PetclinicAppModel extends SwingEntityApplicationModel {

  public PetclinicAppModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels(connectionProvider);
  }

  private void setupEntityModels(final EntityConnectionProvider connectionProvider) {
    SwingEntityModel ownersModel = new SwingEntityModel(T_OWNER, connectionProvider);
    SwingEntityModel petsModel = new SwingEntityModel(T_PET, connectionProvider);
    SwingEntityModel visitModel = new SwingEntityModel(T_VISIT, connectionProvider);

    ownersModel.addDetailModel(petsModel);
    petsModel.addDetailModel(visitModel);

    addEntityModels(ownersModel);
  }
}
