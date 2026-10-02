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
import is.codion.demos.chinook.domain.api.Chinook.Genre;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.ForeignKeyConditions;

import java.util.List;

import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.condition.Condition.and;
import static is.codion.framework.domain.entity.condition.Condition.or;

/**
 * The {@link ForeignKeyConditions} javadoc snippets, each the region of the same name.
 */
final class ForeignKeyConditionsSnippets {

	void usage(EntityConnection connection, Entity excludedArtist, Entity rock) {
		// Find albums by specific artist // @start region=usage
		Entity aliceInChains = connection.selectSingle(
						Artist.NAME.equalTo("Alice In Chains"));

		List<Entity> aliceInChainsAlbums = connection.select(
						Album.ARTIST_FK.equalTo(aliceInChains));

		// Find tracks by genre
		Entity metal = connection.selectSingle(
						Genre.NAME.equalToIgnoreCase("metal"));

		List<Entity> metalTracks = connection.select(
						Track.GENRE_FK.equalTo(metal));

		// Find albums by multiple artists
		List<Entity> theArtists = connection.select(
						Artist.NAME.like("The %"));

		List<Entity> albumsByTheArtists = connection.select(
						Album.ARTIST_FK.in(theArtists));

		// Find tracks without a genre
		List<Entity> uncategorizedTracks = connection.select(
						Track.GENRE_FK.isNull());

		// Find tracks with a genre
		List<Entity> categorizedTracks = connection.select(
						Track.GENRE_FK.isNotNull());

		// Complex query with foreign key conditions
		List<Entity> albums = connection.select(and(
						Album.ARTIST_FK.in(theArtists),
						Album.ARTIST_FK.notEqualTo(excludedArtist)));

		// Using with Select for more control
		List<Entity> rockTracks = connection.select(
						Select.where(Track.GENRE_FK.equalTo(rock))
										.attributes(Track.NAME, Track.ALBUM_FK)
										.orderBy(ascending(Track.NAME)));

		// Foreign key conditions with null handling
		List<Entity> metalOrUncategorized = connection.select(or(
						Track.GENRE_FK.equalTo(metal),
						Track.GENRE_FK.isNull())); // @end
	}
}
