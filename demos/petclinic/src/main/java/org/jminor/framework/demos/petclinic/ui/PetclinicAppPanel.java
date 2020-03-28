/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.petclinic.model.PetclinicAppModel;
import org.jminor.framework.demos.petclinic.model.VetSpecialtyEditModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityModelProvider;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityPanelBuilder;
import org.jminor.swing.framework.ui.EntityTablePanel;

import java.util.List;
import java.util.Locale;

import static java.util.Collections.singletonList;
import static org.jminor.framework.demos.petclinic.domain.Clinic.*;

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
    EntityPanelBuilder petTypePanelProvider =
            new EntityPanelBuilder(T_PET_TYPE)
                    .setEditPanelClass(PetTypeEditPanel.class)
                    .setCaption("Pet types");
    EntityPanelBuilder specialtiesPanelProvider =
            new EntityPanelBuilder(T_SPECIALTY)
                    .setEditPanelClass(SpecialtyEditPanel.class)
                    .setCaption("Specialties");

    EntityPanelBuilder vetsPanelProvider =
            new EntityPanelBuilder(T_VET)
                    .setEditPanelClass(VetEditPanel.class)
                    .setCaption("Vets");
    SwingEntityModelProvider vetSpecialtyModelProvider =
            new SwingEntityModelProvider(T_VET_SPECIALTY)
                    .setEditModelClass(VetSpecialtyEditModel.class);
    EntityPanelBuilder vetSpecialtiesPanelProvider =
            new EntityPanelBuilder(vetSpecialtyModelProvider)
                    .setEditPanelClass(VetSpecialtyEditPanel.class)
                    .setCaption("Specialties");
    vetsPanelProvider.addDetailPanelBuilder(vetSpecialtiesPanelProvider);

    addSupportPanelBuilders(petTypePanelProvider, specialtiesPanelProvider, vetsPanelProvider);
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    EntityEditModel.POST_EDIT_EVENTS.set(true);
    EntityEditPanel.USE_SAVE_CONTROL.set(false);
    EntityPanel.COMPACT_ENTITY_PANEL_LAYOUT.set(true);
    EntityTablePanel.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(EntityTablePanel.ReferentialIntegrityErrorHandling.DEPENDENCIES);
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(ColumnConditionModel.AutomaticWildcard.POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.petclinic.domain.impl.ClinicImpl");
    new PetclinicAppPanel().startApplication("Petclinic", null, false,
            Windows.getScreenSizeRatio(0.6), Users.parseUser("scott:tiger"));
  }
}
