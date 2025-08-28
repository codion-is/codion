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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.common.user.User;
import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static is.codion.framework.db.EntityConnection.Update.where;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class AlbumModelTest {

	private static final String MASTER_OF_PUPPETS = "Master Of Puppets";

	@Test
	void albumRefreshedWhenTrackRatingIsUpdated() {
		try (EntityConnectionProvider connectionProvider = createConnectionProvider()) {
			EntityConnection connection = connectionProvider.connection();
			connection.startTransaction();

			// Initialize all the tracks with an inital rating of 8
			Entity masterOfPuppets = connection.selectSingle(Album.TITLE.equalTo(MASTER_OF_PUPPETS));
			connection.update(where(Track.ALBUM_FK.equalTo(masterOfPuppets))
							.set(Track.RATING, 8)
							.build());
			// Re-select the album to get the updated rating, which is the average of the track ratings
			masterOfPuppets = connection.selectSingle(Album.TITLE.equalTo(MASTER_OF_PUPPETS));
			assertEquals(8, masterOfPuppets.get(Album.RATING));

			// Create our AlbumModel and configure the query condition
			// to populate it with only Master Of Puppets
			AlbumModel albumModel = new AlbumModel(connectionProvider);
			SwingEntityTableModel albumTableModel = albumModel.tableModel();
			albumTableModel.queryModel().condition().get(Album.TITLE).set().equalTo(MASTER_OF_PUPPETS);
			albumTableModel.items().refresh();
			assertEquals(1, albumTableModel.items().count());

			List<Entity> modifiedTracks = connection.select(Track.ALBUM_FK.equalTo(masterOfPuppets)).stream()
							.peek(track -> track.set(Track.RATING, 10))
							.toList();

			// Update the tracks using the edit model
			albumModel.detailModels().get(Track.TYPE).editModel().update(modifiedTracks);

			// Which should trigger the refresh of the album in the Album model
			// now with the new rating as the average of the track ratings
			assertEquals(10, albumTableModel.items().included().get(0).get(Album.RATING));

			connection.rollbackTransaction();
		}
	}

	private static EntityConnectionProvider createConnectionProvider() {
		return LocalEntityConnectionProvider.builder()
						.domain(new ChinookImpl())
						.user(User.parse("scott:tiger"))
						.build();
	}
}
