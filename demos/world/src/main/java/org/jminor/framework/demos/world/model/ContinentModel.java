package org.jminor.framework.demos.world.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.swing.framework.model.SwingEntityModel;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

public final class ContinentModel extends SwingEntityModel {

  private final DefaultPieDataset surfaceAreaDataset = new DefaultPieDataset();
  private final DefaultPieDataset populationDataset = new DefaultPieDataset();
  private final DefaultPieDataset gnpDataset = new DefaultPieDataset();
  private final DefaultCategoryDataset lifeExpectancyDataset = new DefaultCategoryDataset();

  public ContinentModel(EntityConnectionProvider connectionProvider) {
    super(World.T_CONTINENT, connectionProvider);
    getTableModel().addRefreshListener(this::refreshChartDatasets);
  }

  public PieDataset getPopulationChartDataset() {
    return populationDataset;
  }

  public PieDataset getSurfaceAreaChartDataset() {
    return surfaceAreaDataset;
  }

  public PieDataset getGNPChartDataset() {
    return gnpDataset;
  }

  public CategoryDataset getLifeExpectancyDataset() {
    return lifeExpectancyDataset;
  }

  private void refreshChartDatasets() {
    populationDataset.clear();
    surfaceAreaDataset.clear();
    gnpDataset.clear();
    lifeExpectancyDataset.clear();
    getTableModel().getItems().forEach(continent -> {
      populationDataset.setValue(
            continent.getString(World.CONTINENT_NAME),
            continent.getLong(World.CONTINENT_POPULATION));
      surfaceAreaDataset.setValue(
            continent.getString(World.CONTINENT_NAME),
            continent.getInteger(World.CONTINENT_SURFACE_AREA));
      gnpDataset.setValue(
            continent.getString(World.CONTINENT_NAME),
            continent.getDouble(World.CONTINENT_GNP));
      lifeExpectancyDataset.addValue(
              continent.getDouble(World.CONTINENT_MIN_LIFE_EXPECTANCY),
              "Minimum", continent.getString(World.CONTINENT_NAME));
      lifeExpectancyDataset.addValue(
              continent.getDouble(World.CONTINENT_MAX_LIFE_EXPECTANCY),
              "Maximum",continent.getString(World.CONTINENT_NAME));
    });
  }
}
