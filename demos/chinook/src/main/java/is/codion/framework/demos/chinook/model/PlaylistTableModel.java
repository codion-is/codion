/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.util.Collections;

public final class PlaylistTableModel extends SwingEntityTableModel {

  public PlaylistTableModel(EntityConnectionProvider connectionProvider) {
    super(Playlist.TYPE, connectionProvider);
  }

  public void createRandomPlaylist(RandomPlaylistParameters parameters) throws DatabaseException {
    Entity randomPlaylist = createPlaylist(parameters);
    addItemsAt(0, Collections.singletonList(randomPlaylist));
    selectionModel().setSelectedItem(randomPlaylist);
  }

  private Entity createPlaylist(RandomPlaylistParameters parameters) throws DatabaseException {
    EntityConnection connection = connectionProvider().connection();
    connection.beginTransaction();
    try {
      Entity randomPlaylist = connection.execute(Playlist.RANDOM_PLAYLIST, parameters);
      connection.commitTransaction();

      return randomPlaylist;
    }
    catch (DatabaseException e) {
      connection.rollbackTransaction();
      throw e;
    }
    catch (Exception e) {
      connection.rollbackTransaction();
      throw new RuntimeException(e);
    }
  }
}
