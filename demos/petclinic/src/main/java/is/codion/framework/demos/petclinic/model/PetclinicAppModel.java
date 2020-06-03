/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import static is.codion.framework.demos.petclinic.domain.Clinic.*;

public final class PetclinicAppModel extends SwingEntityApplicationModel {

  public PetclinicAppModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels(connectionProvider);
  }

  private void setupEntityModels(final EntityConnectionProvider connectionProvider) {
    SwingEntityModel ownersModel = new SwingEntityModel(Owner.TYPE, connectionProvider);
    SwingEntityModel petsModel = new SwingEntityModel(Pet.TYPE, connectionProvider);
    SwingEntityModel visitModel = new SwingEntityModel(Visit.TYPE, connectionProvider);

    ownersModel.addDetailModel(petsModel);
    petsModel.addDetailModel(visitModel);

    addEntityModels(ownersModel);
  }
}
