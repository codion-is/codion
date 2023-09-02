package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.demos.world.model.CountryModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.TabbedPanelLayout;

final class CountryPanel extends EntityPanel {

  CountryPanel(CountryModel countryModel) {
    super(countryModel,
            new CountryEditPanel(countryModel.editModel()),
            new CountryTablePanel(countryModel.tableModel()),
            TabbedPanelLayout.builder()
                    .splitPaneResizeWeight(0.7)
                    .build());

    SwingEntityModel cityModel = countryModel.detailModel(City.TYPE);
    EntityPanel cityPanel = new EntityPanel(cityModel,
            new CityEditPanel(cityModel.tableModel()),
            new CityTablePanel(cityModel.tableModel()));

    SwingEntityModel countryLanguageModel = countryModel.detailModel(CountryLanguage.TYPE);
    EntityPanel countryLanguagePanel = new EntityPanel(countryLanguageModel,
            new CountryLanguageEditPanel(countryLanguageModel.editModel()),
            new CountryLanguageTablePanel(countryLanguageModel.tableModel()));

    addDetailPanels(cityPanel, countryLanguagePanel);
  }
}
