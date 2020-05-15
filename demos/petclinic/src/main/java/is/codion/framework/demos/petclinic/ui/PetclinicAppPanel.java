/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petclinic.model.PetclinicAppModel;
import is.codion.framework.demos.petclinic.model.VetSpecialtyEditModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityModelBuilder;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import java.util.List;
import java.util.Locale;

import static is.codion.framework.demos.petclinic.domain.Clinic.*;
import static java.util.Collections.singletonList;

public final class PetclinicAppPanel extends EntityApplicationPanel<PetclinicAppModel> {

  @Override
  protected PetclinicAppModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
    return new PetclinicAppModel(connectionProvider);
  }

  @Override
  protected List<EntityPanel> initializeEntityPanels(final PetclinicAppModel applicationModel) {
    SwingEntityModel ownersModel = applicationModel.getEntityModel(T_OWNER);
    SwingEntityModel petsModel = ownersModel.getDetailModel(T_PET);
    SwingEntityModel visitsModel = petsModel.getDetailModel(T_VISIT);

    EntityPanel ownersPanel = new EntityPanel(ownersModel,
            new OwnerEditPanel(ownersModel.getEditModel()));
    EntityPanel petsPanel = new EntityPanel(petsModel,
            new PetEditPanel(petsModel.getEditModel()));
    EntityPanel visitsPanel = new EntityPanel(visitsModel,
            new VisitEditPanel(visitsModel.getEditModel()));

    ownersPanel.addDetailPanel(petsPanel);
    petsPanel.addDetailPanel(visitsPanel);

    ownersModel.refresh();

    return singletonList(ownersPanel);
  }

  @Override
  protected void setupEntityPanelBuilders() {
    EntityPanelBuilder petTypePanelBuilder =
            new EntityPanelBuilder(T_PET_TYPE)
                    .setEditPanelClass(PetTypeEditPanel.class)
                    .setCaption("Pet types");
    EntityPanelBuilder specialtiesPanelBuilder =
            new EntityPanelBuilder(T_SPECIALTY)
                    .setEditPanelClass(SpecialtyEditPanel.class)
                    .setCaption("Specialties");

    EntityPanelBuilder vetsPanelBuilder =
            new EntityPanelBuilder(T_VET)
                    .setEditPanelClass(VetEditPanel.class)
                    .setCaption("Vets");
    SwingEntityModelBuilder vetSpecialtyModelBuilder =
            new SwingEntityModelBuilder(T_VET_SPECIALTY)
                    .setEditModelClass(VetSpecialtyEditModel.class);
    EntityPanelBuilder vetSpecialtiesPanelBuilder =
            new EntityPanelBuilder(vetSpecialtyModelBuilder)
                    .setEditPanelClass(VetSpecialtyEditPanel.class)
                    .setCaption("Specialties");
    vetsPanelBuilder.addDetailPanelBuilder(vetSpecialtiesPanelBuilder);

    addSupportPanelBuilders(petTypePanelBuilder, specialtiesPanelBuilder, vetsPanelBuilder);
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    EntityEditModel.POST_EDIT_EVENTS.set(true);
    EntityEditPanel.USE_SAVE_CONTROL.set(false);
    EntityPanel.COMPACT_ENTITY_PANEL_LAYOUT.set(true);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(ReferentialIntegrityErrorHandling.DEPENDENCIES);
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(ColumnConditionModel.AutomaticWildcard.POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.petclinic.domain.impl.ClinicImpl");
    new PetclinicAppPanel().startApplication("Petclinic", null, MaximizeFrame.NO,
            Windows.getScreenSizeRatio(0.6), Users.parseUser("scott:tiger"));
  }
}
