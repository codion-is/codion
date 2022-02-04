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
            new CountryEditPanel(countryModel.getEditModel()),
            new CountryTablePanel(countryModel.getTableModel()));

    SwingEntityModel cityModel = countryModel.getDetailModel(City.TYPE);
    EntityPanel cityPanel = new EntityPanel(cityModel,
            new CityEditPanel(cityModel.getEditModel(), (CityTableModel) cityModel.getTableModel()),
            new CityTablePanel((CityTableModel) cityModel.getTableModel()));

    SwingEntityModel countryLanguageModel = countryModel.getDetailModel(CountryLanguage.TYPE);
    EntityPanel countryLanguagePanel = new EntityPanel(countryLanguageModel,
            new CountryLanguageEditPanel(countryLanguageModel.getEditModel()),
            new CountryLanguageTablePanel((CountryLanguageTableModel) countryLanguageModel.getTableModel()));

    addDetailPanels(cityPanel, countryLanguagePanel);
    setDetailSplitPanelResizeWeight(0.7);
  }
}
