/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petclinic.domain.api.Owner;
import is.codion.framework.demos.petclinic.domain.api.Pet;
import is.codion.framework.demos.petclinic.domain.api.PetType;
import is.codion.framework.demos.petclinic.domain.api.Specialty;
import is.codion.framework.demos.petclinic.domain.api.Vet;
import is.codion.framework.demos.petclinic.domain.api.VetSpecialty;
import is.codion.framework.demos.petclinic.domain.api.Visit;
import is.codion.framework.demos.petclinic.model.PetclinicAppModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;

public final class PetclinicAppPanel extends EntityApplicationPanel<PetclinicAppModel> {

  public PetclinicAppPanel() {
    super("Petclinic");
  }

  @Override
  protected PetclinicAppModel createApplicationModel(EntityConnectionProvider connectionProvider) {
    return new PetclinicAppModel(connectionProvider);
  }

  @Override
  protected List<EntityPanel> createEntityPanels(PetclinicAppModel applicationModel) {
    SwingEntityModel ownersModel = applicationModel.entityModel(Owner.TYPE);
    SwingEntityModel petsModel = ownersModel.detailModel(Pet.TYPE);
    SwingEntityModel visitsModel = petsModel.detailModel(Visit.TYPE);

    EntityPanel ownersPanel = new EntityPanel(ownersModel,
            new OwnerEditPanel(ownersModel.editModel()));
    EntityPanel petsPanel = new EntityPanel(petsModel,
            new PetEditPanel(petsModel.editModel()));
    EntityPanel visitsPanel = new EntityPanel(visitsModel,
            new VisitEditPanel(visitsModel.editModel()));

    ownersPanel.addDetailPanel(petsPanel);
    petsPanel.addDetailPanel(visitsPanel);

    SwingEntityModel vetsModel = applicationModel.entityModel(Vet.TYPE);
    SwingEntityModel vetSpecialtiesModel = vetsModel.detailModel(VetSpecialty.TYPE);

    EntityPanel vetsPanel = new EntityPanel(vetsModel,
            new VetEditPanel(vetsModel.editModel()));
    EntityPanel vetSpecialtiesPanel = new EntityPanel(vetSpecialtiesModel,
            new VetSpecialtyEditPanel(vetSpecialtiesModel.editModel()));

    vetsPanel.addDetailPanel(vetSpecialtiesPanel);

    return asList(ownersPanel, vetsPanel);
  }

  @Override
  protected List<EntityPanel.Builder> createSupportEntityPanelBuilders(PetclinicAppModel applicationModel) {
    EntityPanel.Builder petTypePanelBuilder =
            EntityPanel.builder(PetType.TYPE)
                    .editPanelClass(PetTypeEditPanel.class)
                    .caption("Pet types");
    EntityPanel.Builder specialtiesPanelBuilder =
            EntityPanel.builder(Specialty.TYPE)
                    .editPanelClass(SpecialtyEditPanel.class)
                    .caption("Specialties");

    return asList(petTypePanelBuilder, specialtiesPanelBuilder);
  }

  public static void main(String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    EntityEditModel.POST_EDIT_EVENTS.set(true);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(ReferentialIntegrityErrorHandling.DEPENDENCIES);
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(AutomaticWildcard.POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.petclinic.domain.PetClinic");
    SwingUtilities.invokeLater(() -> new PetclinicAppPanel().starter()
            .frameSize(Windows.screenSizeRatio(0.6))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start());
  }
}
