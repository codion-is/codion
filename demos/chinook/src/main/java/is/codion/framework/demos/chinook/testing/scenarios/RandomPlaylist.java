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
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Genre;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.framework.demos.chinook.model.ChinookAppModel;
import is.codion.framework.demos.chinook.model.PlaylistTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static java.util.Arrays.asList;

public final class RandomPlaylist extends AbstractEntityUsageScenario<ChinookAppModel> {

  private static final Random RANDOM = new Random();
  private static final String PLAYLIST_NAME = "Random playlist";
  private static final Collection<String> GENRES =
          asList("Alternative", "Rock", "Metal", "Heavy Metal", "Pop");

  @Override
  protected void perform(ChinookAppModel application) throws Exception {
    SwingEntityModel playlistModel = application.entityModel(Playlist.TYPE);
    PlaylistTableModel playlistTableModel = playlistModel.tableModel();
    playlistTableModel.refresh();
    List<Entity> playlistGenres = application.connectionProvider().connection()
            .select(Genre.NAME.in(GENRES));
    playlistTableModel.createRandomPlaylist(new RandomPlaylistParameters(PLAYLIST_NAME + " " + UUID.randomUUID(),
            RANDOM.nextInt(20) + 25, playlistGenres));
    SwingEntityTableModel playlistTrackTableModel = playlistModel.detailModel(PlaylistTrack.TYPE).tableModel();
    playlistTrackTableModel.selectionModel().selectAll();
    playlistTrackTableModel.deleteSelected();
    playlistTableModel.deleteSelected();
  }
}
