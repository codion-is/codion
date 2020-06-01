package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.World;
import is.codion.swing.framework.model.SwingEntityModel;

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

  public PieDataset getPopulationDataset() {
    return populationDataset;
  }

  public PieDataset getSurfaceAreaDataset() {
    return surfaceAreaDataset;
  }

  public PieDataset getGnpDataset() {
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
            continent.get(World.CONTINENT_CONTINENT),
            continent.get(World.CONTINENT_POPULATION));
      surfaceAreaDataset.setValue(
            continent.get(World.CONTINENT_CONTINENT),
            continent.get(World.CONTINENT_SURFACE_AREA));
      gnpDataset.setValue(
            continent.get(World.CONTINENT_CONTINENT),
            continent.get(World.CONTINENT_GNP));
      lifeExpectancyDataset.addValue(
              continent.get(World.CONTINENT_MIN_LIFE_EXPECTANCY),
              "Lowest", continent.get(World.CONTINENT_CONTINENT));
      lifeExpectancyDataset.addValue(
              continent.get(World.CONTINENT_MAX_LIFE_EXPECTANCY),
              "Highest",continent.get(World.CONTINENT_CONTINENT));
    });
  }
}
