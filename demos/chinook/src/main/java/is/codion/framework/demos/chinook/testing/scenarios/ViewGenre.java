/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Genre;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRows;

public final class ViewGenre extends AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(final ChinookApplicationModel application) throws ScenarioException {
    try {
      final SwingEntityModel genreModel = application.getEntityModel(Genre.TYPE);
      genreModel.getTableModel().refresh();
      EntityLoadTestModel.selectRandomRow(genreModel.getTableModel());
      final SwingEntityModel trackModel = genreModel.getDetailModel(Track.TYPE);
      selectRandomRows(trackModel.getTableModel(), 2);
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
