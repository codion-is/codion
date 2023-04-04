package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.framework.demos.world.model.CountryLanguageTableModel;
import is.codion.framework.demos.world.model.CountryModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

final class CountryPanel extends EntityPanel {

  CountryPanel(CountryModel countryModel) {
    super(countryModel,
            new CountryEditPanel(countryModel.editModel()),
            new CountryTablePanel(countryModel.tableModel()));

    SwingEntityModel cityModel = countryModel.detailModel(City.TYPE);
    EntityPanel cityPanel = new EntityPanel(cityModel,
            new CityEditPanel((CityTableModel) cityModel.tableModel()),
            new CityTablePanel((CityTableModel) cityModel.tableModel()));

    SwingEntityModel countryLanguageModel = countryModel.detailModel(CountryLanguage.TYPE);
    EntityPanel countryLanguagePanel = new EntityPanel(countryLanguageModel,
            new CountryLanguageEditPanel(countryLanguageModel.editModel()),
            new CountryLanguageTablePanel((CountryLanguageTableModel) countryLanguageModel.tableModel()));

    addDetailPanels(cityPanel, countryLanguagePanel);
    setDetailSplitPanelResizeWeight(0.7);
  }
}
