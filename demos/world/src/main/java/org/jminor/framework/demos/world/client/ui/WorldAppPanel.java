/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.client.ui;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.beans.ui.CityEditPanel;
import org.jminor.framework.demos.world.beans.ui.CountryEditPanel;
import org.jminor.framework.demos.world.beans.ui.CountryLanguageEditPanel;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.domain.Entities;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;

import java.util.Locale;

public final class WorldAppPanel extends EntityApplicationPanel<WorldAppPanel.WorldAppModel> {

  @Override
  protected void setupEntityPanelProviders() {
    final Entities entities = getModel().getEntities();
    final EntityPanelProvider countryPanelProvider = new EntityPanelProvider(World.T_COUNTRY, entities.getCaption(World.T_COUNTRY));
    countryPanelProvider.setEditPanelClass(CountryEditPanel.class);
    final EntityPanelProvider cityPanelProvider = new EntityPanelProvider(World.T_CITY, entities.getCaption(World.T_CITY));
    cityPanelProvider.setEditPanelClass(CityEditPanel.class);
    final EntityPanelProvider countryLanguagePanelProvider = new EntityPanelProvider(World.T_COUNTRYLANGUAGE, entities.getCaption(World.T_COUNTRYLANGUAGE));
    countryLanguagePanelProvider.setEditPanelClass(CountryLanguageEditPanel.class);
    countryPanelProvider.addDetailPanelProvider(cityPanelProvider);
    countryPanelProvider.addDetailPanelProvider(countryLanguagePanelProvider);

    addEntityPanelProvider(countryPanelProvider);
  }

  @Override
  protected WorldAppModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
    return new WorldAppModel(connectionProvider);
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityPanel.COMPACT_ENTITY_PANEL_LAYOUT.set(true);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.world.domain.World");
    new WorldAppPanel().startApplication("World", null, false, UiUtil.getScreenSizeRatio(0.8), new User("scott", "tiger"));
  }

  public static final class WorldAppModel extends SwingEntityApplicationModel {

    private WorldAppModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }
  }
}
