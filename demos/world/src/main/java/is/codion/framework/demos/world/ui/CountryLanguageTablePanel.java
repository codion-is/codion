package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.CountryLanguageTableModel;

final class CountryLanguageTablePanel extends ChartTablePanel {

  CountryLanguageTablePanel(CountryLanguageTableModel tableModel) {
    super(tableModel, tableModel.chartDataset(), "Languages");
  }
}
