package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.Continent;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

public final class ContinentModel extends SwingEntityModel {

  private final DefaultPieDataset<String> surfaceAreaDataset = new DefaultPieDataset<>();
  private final DefaultPieDataset<String> populationDataset = new DefaultPieDataset<>();
  private final DefaultPieDataset<String> gnpDataset = new DefaultPieDataset<>();
  private final DefaultCategoryDataset lifeExpectancyDataset = new DefaultCategoryDataset();

  ContinentModel(EntityConnectionProvider connectionProvider) {
    super(Continent.TYPE, connectionProvider);
    getTableModel().addRefreshListener(this::refreshChartDatasets);
  }

  public PieDataset<String> getPopulationDataset() {
    return populationDataset;
  }

  public PieDataset<String> getSurfaceAreaDataset() {
    return surfaceAreaDataset;
  }

  public PieDataset<String> getGnpDataset() {
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
    Entity.castTo(Continent.class, getTableModel().getItems()).forEach(continent -> {
      populationDataset.setValue(continent.name(), continent.population());
      surfaceAreaDataset.setValue(continent.name(), continent.surfaceArea());
      gnpDataset.setValue(continent.name(), continent.gnp());
      lifeExpectancyDataset.addValue(continent.minLifeExpectancy(), "Lowest", continent.name());
      lifeExpectancyDataset.addValue( continent.maxLifeExpectancy(), "Highest", continent.name());
    });
  }
}
