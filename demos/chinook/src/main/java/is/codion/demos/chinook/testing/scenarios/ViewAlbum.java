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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.testing.scenarios;

import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.tools.loadtest.LoadTest.Scenario.Performer;

import java.util.List;

import static is.codion.demos.chinook.testing.scenarios.LoadTestUtil.randomArtistId;
import static is.codion.framework.db.EntityConnection.Select.where;

public final class ViewAlbum implements Performer<EntityConnectionProvider> {

	@Override
	public void perform(EntityConnectionProvider connectionProvider) {
		EntityConnection connection = connectionProvider.connection();
		Entity artist = connection.selectSingle(Artist.ID.equalTo(randomArtistId()));
		List<Entity> albums = connection.select(where(Album.ARTIST_FK.equalTo(artist))
						.limit(1)
						.build());
		if (!albums.isEmpty()) {
			connection.select(Chinook.Track.ALBUM_FK.equalTo(albums.get(0)));
		}
	}
}
