/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petclinic.domain.Owner;
import is.codion.framework.demos.petclinic.domain.Pet;
import is.codion.framework.demos.petclinic.domain.PetType;
import is.codion.framework.demos.petclinic.domain.Specialty;
import is.codion.framework.demos.petclinic.domain.Vet;
import is.codion.framework.demos.petclinic.domain.VetSpecialty;
import is.codion.framework.demos.petclinic.domain.Visit;
import is.codion.framework.demos.petclinic.model.PetclinicAppModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.icons.Icons;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;
import is.codion.swing.framework.ui.icons.FrameworkIcons;
import is.codion.swing.plugin.ikonli.foundation.IkonliFoundationFrameworkIcons;
import is.codion.swing.plugin.ikonli.foundation.IkonliFoundationIcons;

import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;

public final class PetclinicAppPanel extends EntityApplicationPanel<PetclinicAppModel> {

  @Override
  protected PetclinicAppModel initializeApplicationModel(EntityConnectionProvider connectionProvider) {
    return new PetclinicAppModel(connectionProvider);
  }

  @Override
  protected List<EntityPanel> initializeEntityPanels(PetclinicAppModel applicationModel) {
    SwingEntityModel ownersModel = applicationModel.getEntityModel(Owner.TYPE);
    SwingEntityModel petsModel = ownersModel.getDetailModel(Pet.TYPE);
    SwingEntityModel visitsModel = petsModel.getDetailModel(Visit.TYPE);

    EntityPanel ownersPanel = new EntityPanel(ownersModel,
            new OwnerEditPanel(ownersModel.getEditModel()));
    EntityPanel petsPanel = new EntityPanel(petsModel,
            new PetEditPanel(petsModel.getEditModel()));
    EntityPanel visitsPanel = new EntityPanel(visitsModel,
            new VisitEditPanel(visitsModel.getEditModel()));

    ownersPanel.addDetailPanel(petsPanel);
    petsPanel.addDetailPanel(visitsPanel);

    SwingEntityModel vetsModel = applicationModel.getEntityModel(Vet.TYPE);
    SwingEntityModel vetSpecialtiesModel = vetsModel.getDetailModel(VetSpecialty.TYPE);

    EntityPanel vetsPanel = new EntityPanel(vetsModel,
            new VetEditPanel(vetsModel.getEditModel()));
    EntityPanel vetSpecialtiesPanel = new EntityPanel(vetSpecialtiesModel,
            new VetSpecialtyEditPanel(vetSpecialtiesModel.getEditModel()));

    vetsPanel.addDetailPanel(vetSpecialtiesPanel);

    ownersModel.refresh();
    vetsModel.refresh();

    return asList(ownersPanel, vetsPanel);
  }

  @Override
  protected void setupEntityPanelBuilders() {
    EntityPanelBuilder petTypePanelBuilder =
            new EntityPanelBuilder(PetType.TYPE)
                    .setEditPanelClass(PetTypeEditPanel.class)
                    .setCaption("Pet types");
    EntityPanelBuilder specialtiesPanelBuilder =
            new EntityPanelBuilder(Specialty.TYPE)
                    .setEditPanelClass(SpecialtyEditPanel.class)
                    .setCaption("Specialties");

    addSupportPanelBuilders(petTypePanelBuilder, specialtiesPanelBuilder);
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    Icons.ICONS_CLASSNAME.set(IkonliFoundationIcons.class.getName());
    FrameworkIcons.FRAMEWORK_ICONS_CLASSNAME.set(IkonliFoundationFrameworkIcons.class.getName());
    EntityEditModel.POST_EDIT_EVENTS.set(true);
    EntityEditPanel.USE_SAVE_CONTROL.set(false);
    EntityPanel.COMPACT_ENTITY_PANEL_LAYOUT.set(true);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(ReferentialIntegrityErrorHandling.DEPENDENCIES);
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(ColumnConditionModel.AutomaticWildcard.POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.petclinic.domain.impl.PetClinicImpl");
    new PetclinicAppPanel().startApplication("Petclinic", null, MaximizeFrame.NO,
            Windows.getScreenSizeRatio(0.6), Users.parseUser("scott:tiger"));
  }
}
