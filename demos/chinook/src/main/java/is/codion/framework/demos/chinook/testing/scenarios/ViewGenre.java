package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

public final class ViewGenre extends EntityLoadTestModel.AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(final ChinookApplicationModel application) throws ScenarioException {
    try {
      final SwingEntityModel genreModel = application.getEntityModel(Chinook.Genre.TYPE);
      genreModel.getTableModel().refresh();
      EntityLoadTestModel.selectRandomRow(genreModel.getTableModel());
      final SwingEntityModel trackModel = genreModel.getDetailModel(Chinook.Track.TYPE);
      EntityLoadTestModel.selectRandomRows(trackModel.getTableModel(), 2);
      genreModel.getConnectionProvider().getConnection().selectDependencies(trackModel.getTableModel().getSelectionModel().getSelectedItems());
    }
    catch (final Exception e) {
      throw new ScenarioException(e);
    }
  }

  @Override
  public int getDefaultWeight() {
    return 10;
  }
}
