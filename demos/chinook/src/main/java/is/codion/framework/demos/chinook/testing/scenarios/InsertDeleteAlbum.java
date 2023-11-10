/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.domain.Chinook.Genre;
import is.codion.framework.demos.chinook.domain.Chinook.MediaType;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.randomArtistId;
import static is.codion.framework.domain.entity.condition.Condition.all;
import static java.util.Arrays.asList;

public final class InsertDeleteAlbum extends AbstractUsageScenario<EntityConnectionProvider> {

  private static final Random RANDOM = new Random();
  private static final Collection<String> GENRES =
          asList("Classical", "Easy Listening", "Jazz", "Latin", "Reggae", "Soundtrack");

  @Override
  protected void perform(EntityConnectionProvider connectionProvider) throws Exception {
    EntityConnection connection = connectionProvider.connection();
    Entity artist = connection.selectSingle(Artist.ID.equalTo(randomArtistId()));
    Entity album = connectionProvider.entities().builder(Album.TYPE)
            .with(Album.ARTIST_FK, artist)
            .with(Album.TITLE, "Title")
            .build();
    album = connection.insertSelect(album);
    List<Entity> genres = connection.select(Genre.NAME.in(GENRES));
    List<Entity> mediaTypes = connection.select(all(MediaType.TYPE));
    Collection<Entity> tracks = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      Entity track = connectionProvider.entities().builder(Track.TYPE)
              .with(Track.ALBUM_FK, album)
              .with(Track.NAME, "Track " + i)
              .with(Track.BYTES, 10000000)
              .with(Track.COMPOSER, "Composer")
              .with(Track.MILLISECONDS, 1000000)
              .with(Track.UNITPRICE, BigDecimal.valueOf(2))
              .with(Track.GENRE_FK, genres.get(RANDOM.nextInt(genres.size())))
              .with(Track.MEDIATYPE_FK, mediaTypes.get(RANDOM.nextInt(mediaTypes.size())))
              .build();
      tracks.add(track);
    }
    tracks = connection.insertSelect(tracks);
    Collection<Entity.Key> toDelete = new ArrayList<>(Entity.primaryKeys(tracks));
    toDelete.add(album.primaryKey());
    connection.delete(toDelete);
  }

  @Override
  public int defaultWeight() {
    return 3;
  }
}
