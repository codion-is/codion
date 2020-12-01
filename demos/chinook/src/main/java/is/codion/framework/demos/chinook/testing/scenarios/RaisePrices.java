package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.demos.chinook.model.TrackTableModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import java.math.BigDecimal;

public final class RaisePrices extends EntityLoadTestModel.AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(final ChinookApplicationModel application) throws ScenarioException {
    try {
      final SwingEntityModel artistModel = application.getEntityModel(Chinook.Artist.TYPE);
      artistModel.getTableModel().refresh();
      EntityLoadTestModel.selectRandomRows(artistModel.getTableModel(), 2);
      final SwingEntityModel albumModel = artistModel.getDetailModel(Chinook.Album.TYPE);
      EntityLoadTestModel.selectRandomRows(albumModel.getTableModel(), 0.5);
      final TrackTableModel trackTableModel =
              (TrackTableModel) albumModel.getDetailModel(Chinook.Track.TYPE).getTableModel();
      EntityLoadTestModel.selectRandomRows(trackTableModel, 4);
      trackTableModel.raisePriceOfSelected(BigDecimal.valueOf(0.01));
    }
    catch (final Exception e) {
      throw new ScenarioException(e);
    }
  }
}
