/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist;
import is.codion.framework.demos.chinook.domain.Chinook.RandomPlaylistParameters;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.util.Collections;

public final class PlaylistTableModel extends SwingEntityTableModel {

  public PlaylistTableModel(final EntityConnectionProvider connectionProvider) {
    super(Playlist.TYPE, connectionProvider);
  }

  public void createRandomPlaylist(final RandomPlaylistParameters parameters) throws DatabaseException {
    final Entity randomPlaylist = getConnectionProvider().getConnection()
            .executeFunction(Playlist.RANDOM_PLAYLIST, parameters);
    addEntitiesAt(0, Collections.singletonList(randomPlaylist));
    getSelectionModel().setSelectedItem(randomPlaylist);
  }
}
