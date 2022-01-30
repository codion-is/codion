package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.demos.world.model.CountryModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class CountryPanel extends EntityPanel {

  public CountryPanel(CountryModel countryModel) {
    super(countryModel,
            new CountryEditPanel(countryModel.getEditModel()),
            new CountryTablePanel(countryModel.getTableModel()));

    SwingEntityModel cityModel = countryModel.getDetailModel(City.TYPE);
    EntityPanel cityPanel = new EntityPanel(cityModel,
            new CityEditPanel(cityModel.getEditModel()),
            new CityTablePanel(cityModel.getTableModel()));

    SwingEntityModel countryLanguageModel = countryModel.getDetailModel(CountryLanguage.TYPE);
    EntityPanel countryLanguagePanel = new EntityPanel(countryLanguageModel,
            new CountryLanguageEditPanel(countryLanguageModel.getEditModel()));

    addDetailPanels(cityPanel, countryLanguagePanel);
    setDetailSplitPanelResizeWeight(0.7);
  }
}
