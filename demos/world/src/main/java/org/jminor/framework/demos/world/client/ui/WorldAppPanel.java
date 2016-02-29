/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.beans.ui.CityEditPanel;
import org.jminor.framework.demos.world.beans.ui.CountryEditPanel;
import org.jminor.framework.demos.world.beans.ui.CountryLanguageEditPanel;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.framework.model.DefaultEntityApplicationModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;

import java.util.Locale;

public final class WorldAppPanel extends EntityApplicationPanel<WorldAppPanel.WorldAppModel> {

  @Override
  protected void setupEntityPanelProviders() {
    final EntityPanelProvider countryPanelProvider = new EntityPanelProvider(World.T_COUNTRY);
    countryPanelProvider.setEditPanelClass(CountryEditPanel.class);
    final EntityPanelProvider cityPanelProvider = new EntityPanelProvider(World.T_CITY);
    cityPanelProvider.setEditPanelClass(CityEditPanel.class);
    final EntityPanelProvider countryLanguagePanelProvider = new EntityPanelProvider(World.T_COUNTRYLANGUAGE);
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
    Configuration.setValue(Configuration.TOOLBAR_BUTTONS, true);
    Configuration.setValue(Configuration.COMPACT_ENTITY_PANEL_LAYOUT, true);
    Configuration.setValue(Configuration.USE_OPTIMISTIC_LOCKING, true);
    new WorldAppPanel().startApplication("World", null, false, UiUtil.getScreenSizeRatio(0.8), new User("scott", "tiger"));
  }

  public static final class WorldAppModel extends DefaultEntityApplicationModel {

    private WorldAppModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }

    @Override
    protected void loadDomainModel() {
      new World();
    }
  }
}
