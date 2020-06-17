package is.codion.framework.demos.world.ui;

import is.codion.common.model.CancelException;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Continent;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.demos.world.domain.api.World.Lookup;
import is.codion.framework.demos.world.model.CountryCustomModel;
import is.codion.framework.demos.world.model.CountryModel;
import is.codion.framework.demos.world.model.WorldAppModel;
import is.codion.swing.common.ui.icons.Icons;
import is.codion.swing.framework.model.SwingEntityModelBuilder;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;
import is.codion.swing.framework.ui.icons.FrameworkIcons;
import is.codion.swing.plugin.ikonli.foundation.IkonliFoundationFrameworkIcons;
import is.codion.swing.plugin.ikonli.foundation.IkonliFoundationIcons;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Locale;

public final class WorldAppPanel extends EntityApplicationPanel<WorldAppModel> {

  // tag::setupEntityPanelBuilders[]
  @Override
  protected void setupEntityPanelBuilders() {
    final SwingEntityModelBuilder countryModelBuilder = new SwingEntityModelBuilder(Country.TYPE);
    countryModelBuilder.setModelClass(CountryModel.class);
    EntityPanelBuilder countryPanelBuilder = new EntityPanelBuilder(countryModelBuilder) {
      @Override
      protected void configurePanel(final EntityPanel entityPanel) {
        entityPanel.setDetailSplitPanelResizeWeight(0.7);
      }
    };
    countryPanelBuilder.setEditPanelClass(CountryEditPanel.class);
    countryPanelBuilder.setTablePanelClass(CountryTablePanel.class);

    final SwingEntityModelBuilder countryCustomModelBuilder = new SwingEntityModelBuilder(Country.TYPE);
    countryCustomModelBuilder.setModelClass(CountryCustomModel.class);
    EntityPanelBuilder countryCustomPanelBuilder = new EntityPanelBuilder(countryCustomModelBuilder)
            .setPanelClass(CountryCustomPanel.class)
            .setCaption("Custom Country");

    EntityPanelBuilder cityPanelBuilder = new EntityPanelBuilder(City.TYPE);
    cityPanelBuilder.setEditPanelClass(CityEditPanel.class);

    EntityPanelBuilder countryLanguagePanelBuilder = new EntityPanelBuilder(CountryLanguage.TYPE);
    countryLanguagePanelBuilder.setEditPanelClass(CountryLanguageEditPanel.class);

    countryPanelBuilder.addDetailPanelBuilder(cityPanelBuilder);
    countryPanelBuilder.addDetailPanelBuilder(countryLanguagePanelBuilder);

    EntityPanelBuilder continentPanelBuilder = new EntityPanelBuilder(Continent.TYPE)
            .setPanelClass(ContinentPanel.class);
    EntityPanelBuilder lookupPanelBuilder = new EntityPanelBuilder(Lookup.TYPE)
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
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.world.domain.WorldImpl");
    new WorldAppPanel().startApplication("World", null, MaximizeFrame.NO,
            new Dimension(1024, 720), Users.parseUser("scott:tiger"));
  }
}
