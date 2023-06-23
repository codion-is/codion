/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Genre;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.model.ChinookAppModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;
import is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel;

import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel.selectRandomRows;

public final class ViewGenre extends AbstractEntityUsageScenario<ChinookAppModel> {

  @Override
  protected void perform(ChinookAppModel application) throws Exception {
    SwingEntityModel genreModel = application.entityModel(Genre.TYPE);
    genreModel.tableModel().refresh();
    EntityLoadTestModel.selectRandomRow(genreModel.tableModel());
    SwingEntityModel trackModel = genreModel.detailModel(Track.TYPE);
    selectRandomRows(trackModel.tableModel(), 2);
    genreModel.connectionProvider().connection().selectDependencies(trackModel.tableModel().selectionModel().getSelectedItems());
  }

  @Override
  public int defaultWeight() {
    return 10;
  }
}
