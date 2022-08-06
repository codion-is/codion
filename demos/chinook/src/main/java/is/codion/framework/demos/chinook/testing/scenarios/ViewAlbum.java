/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRow;

public final class ViewAlbum extends AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(ChinookApplicationModel application) throws Exception {
    SwingEntityModel artistModel = application.entityModel(Artist.TYPE);
    artistModel.tableModel().refresh();
    selectRandomRow(artistModel.tableModel());
    SwingEntityModel albumModel = artistModel.detailModel(Album.TYPE);
    selectRandomRow(albumModel.tableModel());
  }

  @Override
  public int getDefaultWeight() {
    return 10;
  }
}
