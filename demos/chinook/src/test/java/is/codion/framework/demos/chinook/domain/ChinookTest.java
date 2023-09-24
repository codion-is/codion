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
package is.codion.framework.demos.chinook.domain;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChinookTest extends EntityTestUnit {

  public ChinookTest() {
    super(new ChinookImpl());
  }

  @Test
  void album() throws Exception {
    test(Album.TYPE);
  }

  @Test
  void artist() throws Exception {
    test(Artist.TYPE);
  }

  @Test
  void customer() throws Exception {
    test(Customer.TYPE);
  }

  @Test
  void employee() throws Exception {
    test(Employee.TYPE);
  }

  @Test
  void genre() throws Exception {
    test(Genre.TYPE);
  }

  @Test
  void invoce() throws Exception {
    test(Invoice.TYPE);
  }

  @Test
  void invoiceLine() throws Exception {
    test(InvoiceLine.TYPE);
  }

  @Test
  void mediaType() throws Exception {
    test(MediaType.TYPE);
  }

  @Test
  void playlist() throws Exception {
    test(Playlist.TYPE);
  }

  @Test
  void playlistTrack() throws Exception {
    test(PlaylistTrack.TYPE);
  }

  @Test
  void track() throws Exception {
    test(Track.TYPE);
  }

  @Test
  void randomPlaylist() throws Exception {
    EntityConnection connection = connection();
    connection.beginTransaction();
    try {
      Entity genre = connection.selectSingle(Genre.NAME.equalTo("Metal"));
      int noOfTracks = 10;
      String playlistName = "MetalPlaylistTest";
      RandomPlaylistParameters parameters = new RandomPlaylistParameters(playlistName, noOfTracks, singleton(genre));
      Entity playlist = connection.execute(Playlist.RANDOM_PLAYLIST, parameters);
      assertEquals(playlistName, playlist.get(Playlist.NAME));
      List<Entity> playlistTracks = connection.select(PlaylistTrack.PLAYLIST_FK.equalTo(playlist));
      assertEquals(noOfTracks, playlistTracks.size());
      playlistTracks.stream()
              .map(playlistTrack -> playlistTrack.get(PlaylistTrack.TRACK_FK))
              .forEach(track -> assertEquals(genre, track.get(Track.GENRE_FK)));
    }
    finally {
      connection.rollbackTransaction();
    }
  }
}
