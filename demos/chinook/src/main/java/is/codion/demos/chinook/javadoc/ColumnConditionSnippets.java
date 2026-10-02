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
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.ColumnCondition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static is.codion.framework.domain.entity.condition.Condition.and;

/**
 * The {@link ColumnCondition} javadoc snippets, each the region of the same name.
 */
final class ColumnConditionSnippets {

	void usage(EntityConnection connection) {
		// Equality conditions // @start region=usage
		List<Entity> teenSpirit = connection.select(
						Track.NAME.equalTo("Smells Like Teen Spirit"));

		List<Entity> noCompany = connection.select(
						Customer.COMPANY.equalTo(null)); // Becomes "IS NULL"

		// String pattern matching
		List<Entity> theArtists = connection.select(
						Artist.NAME.like("The %"));

		List<Entity> liveAlbums = connection.select(
						Album.TITLE.likeIgnoreCase("%live%"));

		// Comparison conditions
		List<Entity> expensiveTracks = connection.select(
						Track.UNITPRICE.greaterThan(BigDecimal.valueOf(0.99)));

		List<Entity> recentTracks = connection.select(
						Track.INSERT_TIME.greaterThanOrEqualTo(
										LocalDateTime.now().minusDays(30)));

		// Range conditions
		List<Entity> mediumPricedTracks = connection.select(
						Track.UNITPRICE.between(
										BigDecimal.valueOf(0.50),
										BigDecimal.valueOf(1.50)));

		// Collection-based conditions
		List<String> artists = List.of("AC/DC", "Metallica", "Iron Maiden");
		List<Entity> metalTracks = connection.select(
						Track.ARTIST_NAME.inIgnoreCase(artists));

		List<Long> excludedIds = List.of(1L, 5L, 10L);
		List<Entity> filteredArtists = connection.select(
						Artist.ID.notIn(excludedIds));

		// Null checks
		List<Entity> tracksWithComposer = connection.select(
						Track.COMPOSER.isNotNull());

		List<Entity> tracksWithoutComposer = connection.select(
						Track.COMPOSER.isNull());

		// Case-insensitive operations
		List<Entity> zeppelinTracks = connection.select(
						Track.ARTIST_NAME.equalToIgnoreCase("led zeppelin"));

		// Complex combinations with logical operators
		List<Entity> tracks = connection.select(and(
						Track.UNITPRICE.greaterThan(BigDecimal.valueOf(0.99)),
						Track.ARTIST_NAME.inIgnoreCase("AC/DC", "Metallica"),
						Track.MILLISECONDS.lessThan(300_000))); // Less than 5 minutes // @end
	}
}
