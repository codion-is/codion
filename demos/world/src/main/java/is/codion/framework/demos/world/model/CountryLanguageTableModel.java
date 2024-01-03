/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.domain.entity.Entity;
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
    Entity.castTo(CountryLanguage.class, visibleItems()).forEach(language ->
            chartDataset.setValue(language.language(), language.noOfSpeakers()));
  }
}
