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

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.Continent;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingDetailModelLink;
import is.codion.swing.framework.model.SwingEntityModel;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.util.Collection;

public final class ContinentModel extends SwingEntityModel {

  private final DefaultPieDataset<String> surfaceAreaDataset = new DefaultPieDataset<>();
  private final DefaultPieDataset<String> populationDataset = new DefaultPieDataset<>();
  private final DefaultPieDataset<String> gnpDataset = new DefaultPieDataset<>();
  private final DefaultCategoryDataset lifeExpectancyDataset = new DefaultCategoryDataset();

  ContinentModel(EntityConnectionProvider connectionProvider) {
    super(Continent.TYPE, connectionProvider);
    tableModel().refresher().addRefreshListener(this::refreshChartDatasets);
    CountryModel countryModel = new CountryModel(connectionProvider);
    addDetailModel(new CountryModelLink(countryModel)).active().set(true);
  }

  public PieDataset<String> populationDataset() {
    return populationDataset;
  }

  public PieDataset<String> surfaceAreaDataset() {
    return surfaceAreaDataset;
  }

  public PieDataset<String> gnpDataset() {
    return gnpDataset;
  }

  public CategoryDataset lifeExpectancyDataset() {
    return lifeExpectancyDataset;
  }

  private void refreshChartDatasets() {
    populationDataset.clear();
    surfaceAreaDataset.clear();
    gnpDataset.clear();
    lifeExpectancyDataset.clear();
    tableModel().items().forEach(continent -> {
      String contientName = continent.get(Continent.NAME);
      populationDataset.setValue(contientName, continent.get(Continent.POPULATION));
      surfaceAreaDataset.setValue(contientName, continent.get(Continent.SURFACE_AREA));
      gnpDataset.setValue(contientName, continent.get(Continent.GNP));
      lifeExpectancyDataset.addValue(continent.get(Continent.MIN_LIFE_EXPECTANCY), "Lowest", contientName);
      lifeExpectancyDataset.addValue(continent.get(Continent.MAX_LIFE_EXPECTANCY), "Highest", contientName);
    });
  }

  private static final class CountryModel extends SwingEntityModel {

    private CountryModel(EntityConnectionProvider connectionProvider) {
      super(Country.TYPE, connectionProvider);
      editModel().readOnly().set(true);
      ColumnConditionModel<?, ?> continentConditionModel =
              tableModel().conditionModel().conditionModel(Country.CONTINENT);
      continentConditionModel.automaticWildcard().set(AutomaticWildcard.NONE);
      continentConditionModel.caseSensitive().set(true);
    }
  }

  private static final class CountryModelLink extends SwingDetailModelLink {

    private CountryModelLink(SwingEntityModel detailModel) {
      super(detailModel);
    }

    @Override
    public void onSelection(Collection<Entity> selectedEntities) {
      Collection<String> continentNames = Entity.values(Continent.NAME, selectedEntities);
      if (detailModel().tableModel().conditionModel().setEqualConditionValues(Country.CONTINENT, continentNames)) {
        detailModel().tableModel().refresh();
      }
    }
  }
}
