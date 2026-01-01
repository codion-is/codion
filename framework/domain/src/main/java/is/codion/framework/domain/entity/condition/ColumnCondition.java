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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.utilities.Operator;
import is.codion.framework.domain.entity.attribute.Column;

/**
 * A condition based on a single {@link Column}.
 * <p>
 * ColumnConditions provide type-safe filtering for column values using various operators
 * such as equality, comparison, pattern matching, and range operations. String conditions
 * support case-sensitive and case-insensitive variants.
 * <p>
 * Column conditions are typically created through the Column.Factory interface, which
 * all Column instances implement:
 * {@snippet :
 * // Equality conditions
 * List<Entity> metalTracks = connection.select(
 *     Track.GENRE_FK.equalTo(metalGenre));
 *
 * List<Entity> unpriced = connection.select(
 *     Track.UNIT_PRICE.equalTo(null)); // Becomes "IS NULL"
 *
 * // String pattern matching
 * List<Entity> theArtists = connection.select(
 *     Artist.NAME.like("The %"));
 *
 * List<Entity> liveAlbums = connection.select(
 *     Album.TITLE.likeIgnoreCase("%live%"));
 *
 * // Comparison conditions
 * List<Entity> expensiveTracks = connection.select(
 *     Track.UNIT_PRICE.greaterThan(new BigDecimal("0.99")));
 *
 * List<Entity> recentTracks = connection.select(
 *     Track.CREATED_DATE.greaterThanOrEqualTo(
 *         LocalDateTime.now().minusDays(30)));
 *
 * // Range conditions
 * List<Entity> mediumPricedTracks = connection.select(
 *     Track.UNIT_PRICE.between(
 *         new BigDecimal("0.50"),
 *         new BigDecimal("1.50")));
 *
 * // Collection-based conditions
 * List<String> genres = List.of("Rock", "Metal", "Blues");
 * List<Entity> rockishTracks = connection.select(
 *     Track.GENRE_NAME.inIgnoreCase(genres));
 *
 * List<Integer> excludedIds = List.of(1, 5, 10);
 * List<Entity> filteredArtists = connection.select(
 *     Artist.ID.notIn(excludedIds));
 *
 * // Null checks
 * List<Entity> tracksWithPrice = connection.select(
 *     Track.UNIT_PRICE.isNotNull());
 *
 * List<Entity> tracksWithoutComposer = connection.select(
 *     Track.COMPOSER.isNull());
 *
 * // Case-insensitive operations
 * List<Entity> beatlesAlbums = connection.select(
 *     Album.ARTIST_NAME.equalToIgnoreCase("the beatles"));
 *
 * // Complex combinations with logical operators
 * List<Entity> complexQuery = connection.select(and(
 *     Track.UNIT_PRICE.greaterThan(new BigDecimal("0.99")),
 *     Track.GENRE_NAME.inIgnoreCase("Rock", "Metal"),
 *     Track.DURATION.lessThan(300000))); // Less than 5 minutes
 *}
 * @param <T> the attribute type
 * @see ColumnConditions
 * @see Column
 * @see Operator
 */
public sealed interface ColumnCondition<T> extends Condition permits AbstractColumnCondition {

	/**
	 * @return the attribute
	 */
	Column<T> column();

	/**
	 * @return the condition operator
	 */
	Operator operator();

	/**
	 * @return true in case of wildcard comparison, meaning EQUAL should be treated as LIKE
	 */
	boolean wildcard();

	/**
	 * @return true if this condition is case-sensitive, only applies to String based conditions
	 */
	boolean caseSensitive();
}
