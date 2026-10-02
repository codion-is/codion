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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.javadoc;

import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.Condition;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.condition.Condition.*;

/**
 * The {@link Condition} javadoc snippets, each the region of the same name.
 */
final class ConditionSnippets {

	void usage(EntityConnection connection, Entities entities, Entity classical, Entity rock, Entity metal, Collection<Entity> artists) {
		// Simple conditions using column factory methods // @start region=usage
		List<Entity> theArtists = connection.select(
						Artist.NAME.like("The %"));

		List<Entity> classicalTracks = connection.select(
						Track.GENRE_FK.equalTo(classical));

		List<Entity> expensiveTracks = connection.select(
						Track.UNITPRICE.greaterThan(BigDecimal.valueOf(0.99)));

		// Complex conditions using logical combinations
		List<Entity> liveAlbums = connection.select(and(
						Album.ARTIST_FK.in(artists),
						Album.TITLE.likeIgnoreCase("%live%")));

		List<Entity> rockOrMetalTracks = connection.select(or(
						Track.GENRE_FK.equalTo(rock),
						Track.GENRE_FK.equalTo(metal)));

		// Key-based conditions
		Entity.Key artistKey = entities.primaryKey(Artist.TYPE, 1L);
		Entity artist = connection.selectSingle(key(artistKey));
		List<Entity> albumsByArtist = connection.select(
						Album.ARTIST_FK.equalTo(artist));

		// Multiple key conditions
		List<Entity.Key> trackKeys = entities.primaryKeys(Track.TYPE, 1L, 2L, 3L);
		List<Entity> tracks = connection.select(keys(trackKeys));

		// Complex nested conditions
		Condition greatestHits = and(
						Album.ARTIST_FK.in(artists),
						or(
										Album.TITLE.likeIgnoreCase("%greatest%"),
										Album.TITLE.likeIgnoreCase("%best%")));

		List<Entity> albums = connection.select(
						Select.where(greatestHits)
										.orderBy(ascending(Album.TITLE)));

		// All entities (no filtering)
		List<Entity> allArtists = connection.select(all(Artist.TYPE)); // @end
	}
}
