/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Playlist;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.demos.chinook.model.PlaylistTableModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.util.Random;

public final class RandomPlaylist extends AbstractEntityUsageScenario<ChinookApplicationModel> {

  private static final Random RANDOM = new Random();
  private static final String PLAYLIST_NAME = "Random playlist";

  @Override
  protected void perform(ChinookApplicationModel application) throws Exception {
    SwingEntityModel playlistModel = application.entityModel(Playlist.TYPE);
    PlaylistTableModel playlistTableModel = (PlaylistTableModel) playlistModel.tableModel();
    playlistTableModel.refresh();
    playlistTableModel.createRandomPlaylist(new RandomPlaylistParameters(PLAYLIST_NAME + " " + System.currentTimeMillis(),
            RANDOM.nextInt(100) + 25));
    SwingEntityTableModel playlistTrackTableModel = playlistModel.detailModel(PlaylistTrack.TYPE).tableModel();
    playlistTrackTableModel.selectionModel().selectAll();
    playlistTrackTableModel.deleteSelected();
    playlistTableModel.deleteSelected();
  }
}
