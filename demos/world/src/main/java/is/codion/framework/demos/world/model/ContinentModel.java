package is.codion.framework.demos.world.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.Continent;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingDetailModelHandler;
import is.codion.swing.framework.model.SwingEntityModel;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.util.Collection;
import java.util.List;

public final class ContinentModel extends SwingEntityModel {

  private final DefaultPieDataset<String> surfaceAreaDataset = new DefaultPieDataset<>();
  private final DefaultPieDataset<String> populationDataset = new DefaultPieDataset<>();
  private final DefaultPieDataset<String> gnpDataset = new DefaultPieDataset<>();
  private final DefaultCategoryDataset lifeExpectancyDataset = new DefaultCategoryDataset();

  ContinentModel(EntityConnectionProvider connectionProvider) {
    super(Continent.TYPE, connectionProvider);
    tableModel().addRefreshListener(this::refreshChartDatasets);
    CountryModel countryModel = new CountryModel(connectionProvider);
    addDetailModel(new CountryModelHandler(countryModel)).setActive(true);
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
              tableModel().tableConditionModel().conditionModel(Country.CONTINENT);
      continentConditionModel.automaticWildcardValue().set(AutomaticWildcard.NONE);
      continentConditionModel.caseSensitiveState().set(true);
    }
  }

  private static final class CountryModelHandler extends SwingDetailModelHandler {

    private CountryModelHandler(SwingEntityModel detailModel) {
      super(detailModel);
    }

    @Override
    public void onSelection(List<Entity> selectedEntities) {
      Collection<String> continentNames = Entity.get(Continent.NAME, selectedEntities);
      if (detailModel().tableModel().tableConditionModel().setEqualConditionValues(Country.CONTINENT, continentNames)) {
        detailModel().tableModel().refresh();
      }
    }
  }
}
