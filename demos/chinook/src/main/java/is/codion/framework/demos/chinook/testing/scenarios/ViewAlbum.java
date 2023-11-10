/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;

import java.util.List;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.randomArtistId;

public final class ViewAlbum extends AbstractUsageScenario<EntityConnectionProvider> {

  @Override
  protected void perform(EntityConnectionProvider connectionProvider) throws DatabaseException {
    EntityConnection connection = connectionProvider.connection();
    Entity artist = connection.selectSingle(Artist.ID.equalTo(randomArtistId()));
    List<Entity> albums = connection.select(where(Album.ARTIST_FK.equalTo(artist))
            .limit(1)
            .build());
    if (!albums.isEmpty()) {
      connection.select(Chinook.Track.ALBUM_FK.equalTo(albums.get(0)));
    }
  }

  @Override
  public int defaultWeight() {
    return 10;
  }
}
