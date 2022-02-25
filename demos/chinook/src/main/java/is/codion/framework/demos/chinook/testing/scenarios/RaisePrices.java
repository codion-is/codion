/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.demos.chinook.model.TrackTableModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.math.BigDecimal;

import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRows;

public final class RaisePrices extends AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(final ChinookApplicationModel application) throws Exception {
    SwingEntityModel artistModel = application.getEntityModel(Artist.TYPE);
    artistModel.getTableModel().refresh();
    selectRandomRows(artistModel.getTableModel(), 2);
    SwingEntityModel albumModel = artistModel.getDetailModel(Album.TYPE);
    selectRandomRows(albumModel.getTableModel(), 0.5);
    TrackTableModel trackTableModel =
            (TrackTableModel) albumModel.getDetailModel(Track.TYPE).getTableModel();
    selectRandomRows(trackTableModel, 4);
    trackTableModel.raisePriceOfSelected(BigDecimal.valueOf(0.01));
  }
}
