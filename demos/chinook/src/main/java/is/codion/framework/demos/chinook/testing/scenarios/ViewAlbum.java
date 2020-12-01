package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

public final class ViewAlbum extends EntityLoadTestModel.AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(final ChinookApplicationModel application) throws ScenarioException {
    try {
      final SwingEntityModel artistModel = application.getEntityModel(Chinook.Artist.TYPE);
      artistModel.getTableModel().refresh();
      EntityLoadTestModel.selectRandomRow(artistModel.getTableModel());
      final SwingEntityModel albumModel = artistModel.getDetailModel(Chinook.Album.TYPE);
      EntityLoadTestModel.selectRandomRow(albumModel.getTableModel());
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
