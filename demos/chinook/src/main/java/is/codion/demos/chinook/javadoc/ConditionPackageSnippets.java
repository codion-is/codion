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
import is.codion.demos.chinook.domain.api.Chinook.Employee;
import is.codion.demos.chinook.domain.api.Chinook.Genre;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.Condition;

import java.math.BigDecimal;
import java.util.List;

import static is.codion.framework.domain.entity.condition.Condition.*;

/**
 * The {@code is.codion.framework.domain.entity.condition} package javadoc snippets, each the region of the same name.
 */
final class ConditionPackageSnippets {

	void usage(EntityConnection connection) {
		// Column conditions - created directly from Column attributes // @start region=usage
		Condition nameStartsWithA = Customer.LASTNAME.like("A%");
		Condition fromUSA = Customer.COUNTRY.equalTo("USA");
		Condition hasEmail = Customer.EMAIL.isNotNull();

		// Foreign key conditions
		Entity peacock = connection.selectSingle(Employee.LASTNAME.equalTo("Peacock"));
		Condition supportedByPeacock = Customer.SUPPORTREP_FK.equalTo(peacock);

		// Combining conditions
		Condition condition = and(
						nameStartsWithA,
						fromUSA,
						hasEmail,
						supportedByPeacock);

		// Using conditions in queries
		List<Entity> customers = connection.select(condition); // @end
	}

	void columnConditions() {
		// Equality // @start region=columnConditions
		Condition teenSpirit = Track.NAME.equalTo("Smells Like Teen Spirit");
		Condition rated = Track.RATING.equalTo(5);

		// Comparison
		Condition longTracks = Track.MILLISECONDS.greaterThan(180_000);
		Condition totals = Invoice.TOTAL.between(BigDecimal.valueOf(10), BigDecimal.valueOf(100));

		// Pattern matching
		Condition theArtists = Artist.NAME.like("The %");
		Condition zeppelin = Artist.NAME.likeIgnoreCase("%zeppelin%");

		// Nullity
		Condition noPhone = Customer.PHONE.isNull();
		Condition hasEmail = Customer.EMAIL.isNotNull();

		// Multiple values
		Condition genres = Track.GENRE_ID.in(1L, 2L, 3L);
		Condition ratings = Album.RATING.notIn(1, 2, 3); // @end
	}

	void foreignKeyConditions(EntityConnection connection) {
		// Single entity reference // @start region=foreignKeyConditions
		Entity metal = connection.selectSingle(Genre.NAME.equalTo("Metal"));
		Condition metalTracks = Track.GENRE_FK.equalTo(metal);

		// Multiple entity references
		List<Entity> artists = connection.select(Artist.NAME.like("A%"));
		Condition byArtists = Album.ARTIST_FK.in(artists);

		// Null foreign key
		Condition noGenre = Track.GENRE_FK.isNull(); // @end
	}

	void combinations() {
		// AND combination // @start region=combinations
		Condition longExpensiveTracks = and(
						Track.MILLISECONDS.greaterThan(300_000),
						Track.UNITPRICE.greaterThan(BigDecimal.valueOf(0.99)));

		// OR combination
		Condition popularTracks = or(
						Track.RATING.greaterThanOrEqualTo(8),
						Track.PLAY_COUNT.greaterThan(100));

		// Complex nesting
		Condition condition = and(
						longExpensiveTracks,
						popularTracks,
						Track.COMPOSER.isNotNull()); // @end
	}

	void caseSensitivity() {
		// Case-insensitive operations // @start region=caseSensitivity
		Condition zeppelin = Artist.NAME.equalToIgnoreCase("led zeppelin");
		Condition loveAlbums = Album.TITLE.likeIgnoreCase("%love%"); // @end
	}

	void allCondition(EntityConnection connection, String searchText) {
		// Select all rows (no WHERE clause) // @start region=all
		List<Entity> customers = connection.select(all(Customer.TYPE));

		// Useful for conditional filtering
		Condition condition = searchText.isEmpty() ?
						all(Track.TYPE) :
						Track.NAME.like("%" + searchText + "%"); // @end
	}
}
