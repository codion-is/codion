/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petclinic.domain.api.Owner;
import is.codion.framework.demos.petclinic.domain.api.Pet;
import is.codion.framework.demos.petclinic.domain.api.Visit;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

public final class PetclinicAppModel extends SwingEntityApplicationModel {

  public PetclinicAppModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels(connectionProvider);
  }

  private void setupEntityModels(EntityConnectionProvider connectionProvider) {
    SwingEntityModel ownersModel = new SwingEntityModel(Owner.TYPE, connectionProvider);
    SwingEntityModel petsModel = new SwingEntityModel(Pet.TYPE, connectionProvider);
    petsModel.editModel().initializeComboBoxModels(Pet.OWNER_FK, Pet.PET_TYPE_FK);
    SwingEntityModel visitModel = new SwingEntityModel(Visit.TYPE, connectionProvider);
    visitModel.editModel().initializeComboBoxModels(Visit.PET_FK);

    ownersModel.addDetailModel(petsModel);
    petsModel.addDetailModel(visitModel);

    ownersModel.tableModel().refresh();

    addEntityModel(ownersModel);
  }
}
