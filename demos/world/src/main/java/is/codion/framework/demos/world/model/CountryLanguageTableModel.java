package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

public final class CountryLanguageTableModel extends SwingEntityTableModel {

  private final DefaultPieDataset<String> chartDataset = new DefaultPieDataset<>();

  CountryLanguageTableModel(final EntityConnectionProvider connectionProvider) {
    super(CountryLanguage.TYPE, connectionProvider);
    addRefreshSuccessfulListener(this::refreshChartDataset);
  }

  public PieDataset<String> getChartDataset() {
    return chartDataset;
  }

  private void refreshChartDataset() {
    chartDataset.clear();
    Entity.castTo(CountryLanguage.class, getVisibleItems())
            .forEach(language -> chartDataset.setValue(language.language(), language.noOfSpeakers()));
  }
}
