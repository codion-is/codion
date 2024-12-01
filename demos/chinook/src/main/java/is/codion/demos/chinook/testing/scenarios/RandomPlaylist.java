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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.testing.scenarios;

import is.codion.demos.chinook.domain.api.Chinook.Genre;
import is.codion.demos.chinook.domain.api.Chinook.Playlist;
import is.codion.demos.chinook.domain.api.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.demos.chinook.domain.api.Chinook.PlaylistTrack;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.tools.loadtest.LoadTest.Scenario.Performer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static is.codion.demos.chinook.testing.scenarios.LoadTestUtil.RANDOM;
import static is.codion.framework.db.EntityConnection.transaction;
import static java.util.Arrays.asList;

public final class RandomPlaylist implements Performer<EntityConnectionProvider> {

	private static final String PLAYLIST_NAME = "Random playlist";
	private static final Collection<String> GENRES =
					asList("Alternative", "Rock", "Metal", "Heavy Metal", "Pop");

	@Override
	public void perform(EntityConnectionProvider connectionProvider) {
		EntityConnection connection = connectionProvider.connection();
		List<Entity> playlistGenres = connection.select(Genre.NAME.in(GENRES));
		RandomPlaylistParameters parameters = new RandomPlaylistParameters(PLAYLIST_NAME + " " + UUID.randomUUID(),
						RANDOM.nextInt(20) + 25, playlistGenres);
		Entity playlist = transaction(connection, () -> connection.execute(Playlist.RANDOM_PLAYLIST, parameters));
		Collection<Entity> playlistTracks = connection.select(PlaylistTrack.PLAYLIST_FK.equalTo(playlist));
		Collection<Entity.Key> toDelete = Entity.primaryKeys(playlistTracks);
		toDelete.add(playlist.primaryKey());

		connection.delete(toDelete);
	}
}
