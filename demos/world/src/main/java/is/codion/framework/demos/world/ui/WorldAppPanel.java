/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.ui;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.demos.world.domain.api.World;
import is.codion.framework.demos.world.domain.api.World.Continent;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.Lookup;
import is.codion.framework.demos.world.model.CountryModel;
import is.codion.framework.demos.world.model.WorldAppModel;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.SwingConstants;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;

public final class WorldAppPanel extends EntityApplicationPanel<WorldAppModel> {

  private static final String DEFAULT_FLAT_LOOK_AND_FEEL = "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme";

  public WorldAppPanel(WorldAppModel applicationModel) {
    super(applicationModel);
    FrameworkIcons.instance().add(Foundation.MAP, Foundation.PAGE_EXPORT, Foundation.PAGE_ADD, Foundation.CHECK);
  }

  // tag::initializeEntityPanels[]
  @Override
  protected List<EntityPanel> createEntityPanels() {
    CountryModel countryModel = applicationModel().entityModel(Country.TYPE);
    CountryPanel countryPanel = new CountryPanel(countryModel);

    SwingEntityModel continentModel = applicationModel().entityModel(Continent.TYPE);
    ContinentPanel continentPanel = new ContinentPanel(continentModel);

    SwingEntityModel lookupModel = applicationModel().entityModel(Lookup.TYPE);
    EntityPanel lookupPanel = new EntityPanel(lookupModel,
            new LookupTablePanel(lookupModel.tableModel()));

    return asList(countryPanel, continentPanel, lookupPanel);
  }
  // end::initializeEntityPanels[]

  public static void main(String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    Arrays.stream(FlatAllIJThemes.INFOS)
            .forEach(LookAndFeelProvider::addLookAndFeelProvider);
    EntityPanel.TOOLBAR_CONTROLS.set(true);
    FilteredTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING
            .set(ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES);
    EntityApplicationPanel.builder(WorldAppModel.class, WorldAppPanel.class)
            .applicationName("World")
            .domainType(World.DOMAIN)
            .applicationVersion(WorldAppModel.VERSION)
            .defaultLookAndFeelClassName(DEFAULT_FLAT_LOOK_AND_FEEL)
            .defaultLoginUser(User.parse("scott:tiger"))
            .defaultLookAndFeelClassName(FlatDarkLaf.class.getName())
            .start();
  }
}
