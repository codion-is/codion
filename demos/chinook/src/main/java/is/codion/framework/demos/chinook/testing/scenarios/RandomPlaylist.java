/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.loadtest.LoadTest.Scenario.Performer;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Genre;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.framework.domain.entity.Entity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.RANDOM;
import static java.util.Arrays.asList;

public final class RandomPlaylist implements Performer<EntityConnectionProvider> {

  private static final String PLAYLIST_NAME = "Random playlist";
  private static final Collection<String> GENRES =
          asList("Alternative", "Rock", "Metal", "Heavy Metal", "Pop");

  @Override
  public void perform(EntityConnectionProvider connectionProvider) throws Exception {
    EntityConnection connection = connectionProvider.connection();
    List<Entity> playlistGenres = connection.select(Genre.NAME.in(GENRES));
    RandomPlaylistParameters parameters = new RandomPlaylistParameters(PLAYLIST_NAME + " " + UUID.randomUUID(),
            RANDOM.nextInt(20) + 25, playlistGenres);
    Entity playlist = createPlaylist(connection, parameters);
    Collection<Entity> playlistTracks = connection.select(PlaylistTrack.PLAYLIST_FK.equalTo(playlist));
    Collection<Entity.Key> toDelete = Entity.primaryKeys(playlistTracks);
    toDelete.add(playlist.primaryKey());

    connection.delete(toDelete);
  }

  private static Entity createPlaylist(EntityConnection connection,
                                       RandomPlaylistParameters parameters) throws DatabaseException {
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
