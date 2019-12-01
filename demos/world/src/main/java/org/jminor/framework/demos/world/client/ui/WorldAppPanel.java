/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.client.ui;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.beans.ui.CityEditPanel;
import org.jminor.framework.demos.world.beans.ui.CountryEditPanel;
import org.jminor.framework.demos.world.beans.ui.CountryLanguageEditPanel;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.textfield.NumberField;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;
import org.jminor.swing.framework.ui.EntityTablePanel;

import java.util.Locale;

public final class WorldAppPanel extends EntityApplicationPanel<WorldAppPanel.WorldAppModel> {

  // tag::setupEntityPanelProviders[]
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

    final EntityPanelProvider lookupPanelProvider = new EntityPanelProvider(World.T_LOOKUP) {
      @Override
      protected void configureTablePanel(final EntityTablePanel tablePanel) {
        tablePanel.setConditionPanelVisible(true);
      }
    }.setRefreshOnInit(false);

    addEntityPanelProviders(countryPanelProvider, lookupPanelProvider);
  }
  // end::setupEntityPanelProviders[]

  @Override
  protected WorldAppModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
    return new WorldAppModel(connectionProvider);
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityPanel.COMPACT_ENTITY_PANEL_LAYOUT.set(true);
    NumberField.DISABLE_GROUPING.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.world.domain.World");
    new WorldAppPanel().startApplication("World", null, false, UiUtil.getScreenSizeRatio(0.8),
            new User("scott", "tiger".toCharArray()));
  }

  public static final class WorldAppModel extends SwingEntityApplicationModel {

    private WorldAppModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }
  }
}
