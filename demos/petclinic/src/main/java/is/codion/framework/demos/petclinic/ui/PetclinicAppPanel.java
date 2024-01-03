/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.demos.petclinic.domain.api.Owner;
import is.codion.framework.demos.petclinic.domain.api.Pet;
import is.codion.framework.demos.petclinic.domain.api.PetType;
import is.codion.framework.demos.petclinic.domain.api.Petclinic;
import is.codion.framework.demos.petclinic.domain.api.Specialty;
import is.codion.framework.demos.petclinic.domain.api.Vet;
import is.codion.framework.demos.petclinic.domain.api.VetSpecialty;
import is.codion.framework.demos.petclinic.domain.api.Visit;
import is.codion.framework.demos.petclinic.model.PetclinicAppModel;
import is.codion.framework.demos.petclinic.model.VetSpecialtyEditModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public final class PetclinicAppPanel extends EntityApplicationPanel<PetclinicAppModel> {

  private static final String DEFAULT_FLAT_LOOK_AND_FEEL = "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme";

  public PetclinicAppPanel(PetclinicAppModel appModel) {
    super(appModel);
  }

  @Override
  protected List<EntityPanel> createEntityPanels() {
    SwingEntityModel ownersModel = applicationModel().entityModel(Owner.TYPE);
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

    return singletonList(ownersPanel);
  }

  @Override
  protected List<EntityPanel.Builder> createSupportEntityPanelBuilders() {
    EntityPanel.Builder petTypePanelBuilder =
            EntityPanel.builder(PetType.TYPE)
                    .editPanelClass(PetTypeEditPanel.class)
                    .caption("Pet types");
    EntityPanel.Builder specialtyPanelBuilder =
            EntityPanel.builder(Specialty.TYPE)
                    .editPanelClass(SpecialtyEditPanel.class)
                    .caption("Specialties");

    SwingEntityModel.Builder vetSpecialtyModelBuilder =
            SwingEntityModel.builder(VetSpecialty.TYPE)
                    .editModelClass(VetSpecialtyEditModel.class);
    SwingEntityModel.Builder vetModelBuilder =
            SwingEntityModel.builder(Vet.TYPE)
                    .detailModelBuilder(vetSpecialtyModelBuilder);

    EntityPanel.Builder vetSpecialtyPanelBuilder =
            EntityPanel.builder(vetSpecialtyModelBuilder)
                    .editPanelClass(VetSpecialtyEditPanel.class)
                    .caption("Specialty");
    EntityPanel.Builder vetPanelBuilder =
            EntityPanel.builder(vetModelBuilder)
                    .editPanelClass(VetEditPanel.class)
                    .detailPanelBuilder(vetSpecialtyPanelBuilder)
                    .caption("Vets");

    return asList(petTypePanelBuilder, specialtyPanelBuilder, vetPanelBuilder);
  }

  public static void main(String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    Arrays.stream(FlatAllIJThemes.INFOS)
            .forEach(LookAndFeelProvider::addLookAndFeelProvider);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING
            .set(ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES);
    EntityApplicationPanel.builder(PetclinicAppModel.class, PetclinicAppPanel.class)
            .applicationName("Petclinic")
            .domainType(Petclinic.DOMAIN)
            .defaultLookAndFeelClassName(DEFAULT_FLAT_LOOK_AND_FEEL)
            .frameSize(Windows.screenSizeRatio(0.6))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start();
  }
}
