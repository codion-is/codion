package is.codion.framework.demos.world.ui;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Continent;
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
  protected void setupEntityPanelBuilders(final WorldAppModel applicationModel) {
    SwingEntityModel countryModel = applicationModel.getEntityModel(CountryModel.class);
    SwingEntityModel countryOverviewModel = applicationModel.getEntityModel(CountryOverviewModel.class);
    SwingEntityModel cityModel = countryModel.getDetailModel(City.TYPE);
    SwingEntityModel countryLanguageModel = countryModel.getDetailModel(CountryLanguage.TYPE);
    SwingEntityModel continentModel = applicationModel.getEntityModel(Continent.TYPE);
    SwingEntityModel lookupModel = applicationModel.getEntityModel(Lookup.TYPE);

    EntityPanel.Builder countryPanelBuilder = EntityPanel.builder(countryModel)
            .panelInitializer(entityPanel -> entityPanel.setDetailSplitPanelResizeWeight(0.7));
    countryPanelBuilder.editPanelClass(CountryEditPanel.class);

    EntityPanel.Builder countryCustomPanelBuilder = EntityPanel.builder(countryOverviewModel)
            .panelClass(CountryOverviewPanel.class)
            .caption("Country Overview");

    EntityPanel.Builder cityPanelBuilder = EntityPanel.builder(cityModel)
            .editPanelClass(CityEditPanel.class)
            .tablePanelClass(CityTablePanel.class);

    EntityPanel.Builder countryLanguagePanelBuilder = EntityPanel.builder(countryLanguageModel)
            .editPanelClass(CountryLanguageEditPanel.class);

    countryPanelBuilder.detailPanelBuilder(cityPanelBuilder);
    countryPanelBuilder.detailPanelBuilder(countryLanguagePanelBuilder);

    EntityPanel.Builder continentPanelBuilder = EntityPanel.builder(continentModel)
            .panelClass(ContinentPanel.class);
    EntityPanel.Builder lookupPanelBuilder = EntityPanel.builder(lookupModel)
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
