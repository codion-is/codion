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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.manual;

import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.demos.chinook.domain.api.Chinook.Playlist;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.List;

final class ConditionDemo {

	private static void condition(EntityConnection connection) {
		// tag::condition[]
		Condition allArtistsCondition =
						Condition.all(Artist.TYPE);

		List<Entity> artists =
						connection.select(allArtistsCondition);
		// end::condition[]
	}

	private static void columnCondition(EntityConnection connection) {
		// tag::columnCondition[]
		Condition liveAlbums =
						Album.TITLE.likeIgnoreCase("%Live%");

		List<Entity> albums =
						connection.select(liveAlbums);
		// end::columnCondition[]
	}

	private static void foreignKeyCondition(EntityConnection connection) {
		// tag::foreignKeyCondition[]
		Entity metallica =
						connection.selectSingle(
										Artist.NAME.equalTo("Metallica"));

		Condition albums =
						Album.ARTIST_FK.equalTo(metallica);
		// end::foreignKeyCondition[]
	}

	private static void customCondition(EntityConnection connection) {
		// tag::custom[]
		List<Long> classicalPlaylistIds =
						List.of(42L, 43L);

		Condition noneClassical =
						Track.NOT_IN_PLAYLIST.get(
										Playlist.ID, classicalPlaylistIds);

		List<Entity> tracks =
						connection.select(noneClassical);
		// end::custom[]
	}

	private static void combinationCondition(EntityConnection connection,
																					 Condition liveAlbums, Condition metallicaAlbums) {
		// tag::combination[]
		Condition liveMetallicaAlbums =
						Condition.and(liveAlbums, metallicaAlbums);

		List<Entity> albums =
						connection.select(liveMetallicaAlbums);
		// end::combination[]
	}

	private static void select(EntityConnection connection, Condition liveMetallicaAlbums) {
		// tag::select[]
		Select selectLiveMetallicaAlbums =
						Select.where(liveMetallicaAlbums)
										.orderBy(OrderBy.descending(Album.NUMBER_OF_TRACKS))
										.build();

		List<Entity> albums =
						connection.select(selectLiveMetallicaAlbums);
		// end::select[]
	}

	private static void update(EntityConnection connection, Condition liveMetallicaAlbums) {
		// tag::update[]
		Update removeLiveMetallicaAlbumCovers =
						Update.where(liveMetallicaAlbums)
										.set(Album.COVER, null)
										.build();

		int updateCount =
						connection.update(removeLiveMetallicaAlbumCovers);
		// end::update[]
	}

	private static void count(EntityConnection connection) {
		// tag::count[]
		Count countAlbumsWithCover =
						Count.where(Album.COVER.isNotNull());

		int count = connection.count(countAlbumsWithCover);
		// end::count[]
	}
}
