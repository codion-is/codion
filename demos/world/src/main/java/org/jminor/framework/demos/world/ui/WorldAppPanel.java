package org.jminor.framework.demos.world.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.demos.world.model.CountryCustomModel;
import org.jminor.framework.demos.world.model.CountryModel;
import org.jminor.framework.demos.world.model.WorldAppModel;
import org.jminor.swing.common.ui.icons.Icons;
import org.jminor.swing.framework.model.SwingEntityModelBuilder;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityPanelBuilder;
import org.jminor.swing.framework.ui.icons.FrameworkIcons;
import org.jminor.swing.plugin.ikonli.foundation.IkonliFoundationFrameworkIcons;
import org.jminor.swing.plugin.ikonli.foundation.IkonliFoundationIcons;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
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
    EntityPanelBuilder countryCustomPanelBuilder = new EntityPanelBuilder(countryCustomModelBuilder)
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

    addEntityPanelBuilders(countryPanelBuilder, countryCustomPanelBuilder, continentPanelBuilder, lookupPanelBuilder);
  }
  // end::setupEntityPanelBuilders[]

  @Override
  protected WorldAppModel initializeApplicationModel(EntityConnectionProvider connectionProvider) {
    return new WorldAppModel(connectionProvider);
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    UIManager.put("Table.alternateRowColor", new Color(215, 215, 215));
    Icons.ICONS_CLASSNAME.set(IkonliFoundationIcons.class.getName());
    FrameworkIcons.FRAMEWORK_ICONS_CLASSNAME.set(IkonliFoundationFrameworkIcons.class.getName());
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.world.domain.World");
    new WorldAppPanel().startApplication("World", null, MaximizeFrame.NO,
            new Dimension(1024, 720), Users.parseUser("scott:tiger"));
  }
}
