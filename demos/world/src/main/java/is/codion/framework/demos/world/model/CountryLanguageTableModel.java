/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

public final class CountryLanguageTableModel extends SwingEntityTableModel {

  private final DefaultPieDataset<String> chartDataset = new DefaultPieDataset<>();

  CountryLanguageTableModel(EntityConnectionProvider connectionProvider) {
    super(CountryLanguage.TYPE, connectionProvider);
    editModel().initializeComboBoxModels(CountryLanguage.COUNTRY_FK);
    refresher().addRefreshListener(this::refreshChartDataset);
  }

  public PieDataset<String> chartDataset() {
    return chartDataset;
  }

  private void refreshChartDataset() {
    chartDataset.clear();
    visibleItems().forEach(countryLanguage ->
            chartDataset.setValue(countryLanguage.get(CountryLanguage.LANGUAGE),
                    countryLanguage.get(CountryLanguage.NO_OF_SPEAKERS)));
  }
}
