package org.jminor.framework.demos.world.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.swing.framework.model.SwingEntityModel;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.util.List;

public final class ContinentModel extends SwingEntityModel {

  private final DefaultPieDataset continentSurfaceAreaDataset = new DefaultPieDataset();
  private final DefaultPieDataset continentPopulationDataset = new DefaultPieDataset();
  private final DefaultPieDataset continentGNPDataset = new DefaultPieDataset();
  private final DefaultCategoryDataset lifeExpectancyDataset = new DefaultCategoryDataset();

  public ContinentModel(final EntityConnectionProvider connectionProvider) {
    super(World.T_CONTINENT, connectionProvider);
  }

  public PieDataset getPopulationChartDataset() {
    try {
      List<Entity> continents = getConnectionProvider()
              .getConnection().select(Conditions.entitySelectCondition(World.T_CONTINENT));
      continents.forEach(continent -> continentPopulationDataset.setValue(
              continent.getString(World.CONTINENT_NAME),
              continent.getLong(World.CONTINENT_POPULATION)));

      return continentPopulationDataset;
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  public PieDataset getSurfaceAreaChartDataset() {
    try {
      List<Entity> continents = getConnectionProvider()
                  .getConnection().select(Conditions.entitySelectCondition(World.T_CONTINENT));
      continents.forEach(continent -> continentSurfaceAreaDataset.setValue(
              continent.getString(World.CONTINENT_NAME),
              continent.getInteger(World.CONTINENT_SURFACE_AREA)));

      return continentSurfaceAreaDataset;
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  public PieDataset getGNPChartDataset() {
    try {
      List<Entity> continents = getConnectionProvider()
                  .getConnection().select(Conditions.entitySelectCondition(World.T_CONTINENT));
      continents.forEach(continent -> continentGNPDataset.setValue(
              continent.getString(World.CONTINENT_NAME),
              continent.getDouble(World.CONTINENT_GNP)));

      return continentGNPDataset;
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  public CategoryDataset getLifeExpectancyDataset() {
    try {
      List<Entity> continents = getConnectionProvider()
                  .getConnection().select(Conditions.entitySelectCondition(World.T_CONTINENT));
      continents.forEach(continent -> {
        lifeExpectancyDataset.addValue(
                continent.getDouble(World.CONTINENT_MIN_LIFE_EXPECTANCY),
                "Minimum",
                continent.getString(World.CONTINENT_NAME));
        lifeExpectancyDataset.addValue(
                continent.getDouble(World.CONTINENT_MAX_LIFE_EXPECTANCY),
                "Maximum",
                continent.getString(World.CONTINENT_NAME));
      });

    return lifeExpectancyDataset;
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
