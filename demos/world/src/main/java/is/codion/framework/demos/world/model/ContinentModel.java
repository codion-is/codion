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
    addDetailModel(new CountryModelLink(countryModel)).setActive(true);
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
    Entity.castTo(Continent.class, tableModel().items()).forEach(continent -> {
      populationDataset.setValue(continent.name(), continent.population());
      surfaceAreaDataset.setValue(continent.name(), continent.surfaceArea());
      gnpDataset.setValue(continent.name(), continent.gnp());
      lifeExpectancyDataset.addValue(continent.minLifeExpectancy(), "Lowest", continent.name());
      lifeExpectancyDataset.addValue(continent.maxLifeExpectancy(), "Highest", continent.name());
    });
  }

  private static final class CountryModel extends SwingEntityModel {

    private CountryModel(EntityConnectionProvider connectionProvider) {
      super(Country.TYPE, connectionProvider);
      editModel().setReadOnly(true);
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
