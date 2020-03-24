package org.jminor.framework.demos.world.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.demos.world.model.WorldAppModel;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;

import java.util.Locale;

public final class WorldAppPanel extends EntityApplicationPanel<WorldAppModel> {

  // tag::setupEntityPanelProviders[]
  @Override
  protected void setupEntityPanelProviders() {
    EntityPanelProvider countryPanelProvider = new EntityPanelProvider(World.T_COUNTRY);
    countryPanelProvider.setEditPanelClass(CountryEditPanel.class);
    countryPanelProvider.setTablePanelClass(CountryTablePanel.class);

    EntityPanelProvider cityPanelProvider = new EntityPanelProvider(World.T_CITY);
    cityPanelProvider.setEditPanelClass(CityEditPanel.class);

    EntityPanelProvider countryLanguagePanelProvider = new EntityPanelProvider(World.T_COUNTRYLANGUAGE);
    countryLanguagePanelProvider.setEditPanelClass(CountryLanguageEditPanel.class);

    countryPanelProvider.addDetailPanelProvider(cityPanelProvider);
    countryPanelProvider.addDetailPanelProvider(countryLanguagePanelProvider);

    EntityPanelProvider lookupPanelProvider = new EntityPanelProvider(World.T_LOOKUP)
            .setTablePanelClass(LookupTablePanel.class)
            .setRefreshOnInit(false);

    addEntityPanelProviders(countryPanelProvider, lookupPanelProvider);
  }
  // end::setupEntityPanelProviders[]

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
