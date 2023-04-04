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
    addRefreshListener(this::refreshChartDataset);
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
