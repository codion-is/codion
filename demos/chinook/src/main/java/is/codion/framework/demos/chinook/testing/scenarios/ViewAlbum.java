/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.model.ChinookAppModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRow;

public final class ViewAlbum extends AbstractEntityUsageScenario<ChinookAppModel> {

  @Override
  protected void perform(ChinookAppModel application) throws Exception {
    SwingEntityModel artistModel = application.entityModel(Artist.TYPE);
    artistModel.tableModel().refresh();
    selectRandomRow(artistModel.tableModel());
    SwingEntityModel albumModel = artistModel.detailModel(Album.TYPE);
    selectRandomRow(albumModel.tableModel());
  }

  @Override
  public int defaultWeight() {
    return 10;
  }
}
