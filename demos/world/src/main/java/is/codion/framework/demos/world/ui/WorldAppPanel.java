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
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;

public final class WorldAppPanel extends EntityApplicationPanel<WorldAppModel> {

  public WorldAppPanel() {
    super("World");
  }

  // tag::initializeEntityPanels[]
  @Override
  protected List<EntityPanel> initializeEntityPanels(WorldAppModel applicationModel) {
    SwingEntityModel countryModel = applicationModel.getEntityModel(CountryModel.class);
    SwingEntityModel countryOverviewModel = applicationModel.getEntityModel(CountryOverviewModel.class);
    SwingEntityModel cityModel = countryModel.getDetailModel(City.TYPE);
    SwingEntityModel countryLanguageModel = countryModel.getDetailModel(CountryLanguage.TYPE);
    SwingEntityModel continentModel = applicationModel.getEntityModel(Continent.TYPE);
    SwingEntityModel lookupModel = applicationModel.getEntityModel(Lookup.TYPE);

    EntityPanel countryPanel = new EntityPanel(countryModel,
            new CountryEditPanel(countryModel.getEditModel()),
            new CountryTablePanel(countryModel.getTableModel()));
    countryPanel.setDetailSplitPanelResizeWeight(0.7);
    countryModel.refresh();

    EntityPanel countryOverviewPanel = new CountryOverviewPanel(countryOverviewModel);
    countryOverviewPanel.setCaption("Country Overview");
    countryOverviewModel.refresh();

    EntityPanel cityPanel = new EntityPanel(cityModel,
            new CityEditPanel(cityModel.getEditModel()),
            new CityTablePanel(cityModel.getTableModel()));

    EntityPanel countryLanguagePanel = new EntityPanel(countryLanguageModel,
            new CountryLanguageEditPanel(countryLanguageModel.getEditModel()));

    countryPanel.addDetailPanels(cityPanel, countryLanguagePanel);

    EntityPanel continentPanel = new ContinentPanel(continentModel);
    continentModel.refresh();

    EntityPanel lookupPanel = new EntityPanel(lookupModel, new LookupTablePanel(lookupModel.getTableModel()));

    return asList(countryPanel, countryOverviewPanel, continentPanel, lookupPanel);
  }
  // end::initializeEntityPanels[]

  @Override
  protected WorldAppModel initializeApplicationModel(EntityConnectionProvider connectionProvider) {
    return new WorldAppModel(connectionProvider);
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    UIManager.put("Table.alternateRowColor", new Color(215, 215, 215));
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(ReferentialIntegrityErrorHandling.DEPENDENCIES);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.world.domain.WorldImpl");
    SwingUtilities.invokeLater(() -> new WorldAppPanel().starter()
            .frameSize(new Dimension(1024, 720))
            .defaultLoginUser(User.parseUser("scott:tiger"))
            .start());
  }
}
