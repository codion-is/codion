package org.jminor.framework.demos.world.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.demos.world.model.CountryCustomModel;
import org.jminor.framework.demos.world.model.CountryModel;
import org.jminor.framework.demos.world.model.WorldAppModel;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.framework.model.SwingEntityModelBuilder;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityPanelBuilder;

import java.util.Locale;

public final class WorldAppPanel extends EntityApplicationPanel<WorldAppModel> {

  // tag::setupEntityPanelBuilders[]
  @Override
  protected void setupEntityPanelBuilders() {
    final SwingEntityModelBuilder countryModelBuilder = new SwingEntityModelBuilder(World.T_COUNTRY);
    countryModelBuilder.setModelClass(CountryModel.class);
    EntityPanelBuilder countryPanelBuilder = new EntityPanelBuilder(countryModelBuilder);
    countryPanelBuilder.setEditPanelClass(CountryEditPanel.class);
    countryPanelBuilder.setTablePanelClass(CountryTablePanel.class);

    final SwingEntityModelBuilder countryCustomModelBuilder = new SwingEntityModelBuilder(World.T_COUNTRY);
    countryCustomModelBuilder.setModelClass(CountryCustomModel.class);
    EntityPanelBuilder customCountryPanelBuilder = new EntityPanelBuilder(countryCustomModelBuilder)
            .setPanelClass(CountryCustomPanel.class)
            .setCaption("Custom Country");

    EntityPanelBuilder cityPanelBuilder = new EntityPanelBuilder(World.T_CITY);
    cityPanelBuilder.setEditPanelClass(CityEditPanel.class);

    EntityPanelBuilder countryLanguagePanelBuilder = new EntityPanelBuilder(World.T_COUNTRYLANGUAGE);
    countryLanguagePanelBuilder.setEditPanelClass(CountryLanguageEditPanel.class);

    countryPanelBuilder.addDetailPanelBuilder(cityPanelBuilder);
    countryPanelBuilder.addDetailPanelBuilder(countryLanguagePanelBuilder);

    EntityPanelBuilder continentPanelBuilder = new EntityPanelBuilder(World.T_CONTINENT)
            .setPanelClass(ContinentPanel.class);
    EntityPanelBuilder lookupPanelBuilder = new EntityPanelBuilder(World.T_LOOKUP)
            .setTablePanelClass(LookupTablePanel.class)
            .setRefreshOnInit(false);

    addEntityPanelBuilders(countryPanelBuilder, customCountryPanelBuilder, continentPanelBuilder, lookupPanelBuilder);
  }
  // end::setupEntityPanelBuilders[]

  @Override
  protected WorldAppModel initializeApplicationModel(EntityConnectionProvider connectionProvider) {
    return new WorldAppModel(connectionProvider);
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.world.domain.World");
    new WorldAppPanel().startApplication("World", null, false,
            Windows.getScreenSizeRatio(0.8), Users.parseUser("scott:tiger"));
  }
}
