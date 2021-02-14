package is.codion.framework.demos.world.ui;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Continent;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.demos.world.domain.api.World.Lookup;
import is.codion.framework.demos.world.model.CountryModel;
import is.codion.framework.demos.world.model.CountryOverviewModel;
import is.codion.framework.demos.world.model.WorldAppModel;
import is.codion.swing.common.ui.icons.Icons;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;
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
    SwingEntityModel.Builder countryModelBuilder = SwingEntityModel.builder(Country.TYPE);
    countryModelBuilder.modelClass(CountryModel.class);
    EntityPanel.Builder countryPanelBuilder = EntityPanel.builder(countryModelBuilder)
            .panelInitializer(entityPanel -> entityPanel.setDetailSplitPanelResizeWeight(0.7));
    countryPanelBuilder.editPanelClass(CountryEditPanel.class);

    SwingEntityModel.Builder countryOverviewModelBuilder = SwingEntityModel.builder(Country.TYPE);
    countryOverviewModelBuilder.modelClass(CountryOverviewModel.class);
    EntityPanel.Builder countryCustomPanelBuilder = EntityPanel.builder(countryOverviewModelBuilder)
            .panelClass(CountryOverviewPanel.class)
            .caption("Country Overview");

    EntityPanel.Builder cityPanelBuilder = EntityPanel.builder(City.TYPE)
            .editPanelClass(CityEditPanel.class)
            .tablePanelClass(CityTablePanel.class);

    EntityPanel.Builder countryLanguagePanelBuilder = EntityPanel.builder(CountryLanguage.TYPE)
            .editPanelClass(CountryLanguageEditPanel.class);

    countryPanelBuilder.detailPanelBuilder(cityPanelBuilder);
    countryPanelBuilder.detailPanelBuilder(countryLanguagePanelBuilder);

    EntityPanel.Builder continentPanelBuilder = EntityPanel.builder(Continent.TYPE)
            .panelClass(ContinentPanel.class);
    EntityPanel.Builder lookupPanelBuilder = EntityPanel.builder(Lookup.TYPE)
            .tablePanelClass(LookupTablePanel.class)
            .refreshOnInit(false);

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
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(ReferentialIntegrityErrorHandling.DEPENDENCIES);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.world.domain.WorldImpl");
    new WorldAppPanel().startApplication("World", null, MaximizeFrame.NO,
            new Dimension(1024, 720), User.parseUser("scott:tiger"));
  }
}
