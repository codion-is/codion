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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
/**
 * Provides a type-safe condition API for building SQL WHERE clauses programmatically.
 *
 * <h2>Overview</h2>
 * <p>The condition framework enables type-safe query construction through a fluent API that
 * mirrors SQL operators while leveraging Java's type system for compile-time safety.
 * Conditions are the primary mechanism for filtering data when querying entities.
 *
 * <h2>Core Concepts</h2>
 *
 * <h3>Condition Types</h3>
 * <ul>
 *   <li><strong>{@link is.codion.framework.domain.entity.condition.ColumnConditionFactory}</strong> -
 *       Conditions based on column values (equality, comparison, patterns, nullity)</li>
 *   <li><strong>{@link is.codion.framework.domain.entity.condition.ForeignKeyConditionFactory}</strong> -
 *       Conditions based on foreign key relationships</li>
 *   <li><strong>{@link is.codion.framework.domain.entity.condition.CustomCondition}</strong> -
 *       Complex conditions that cannot be expressed with standard operators</li>
 *   <li><strong>Combination Conditions</strong> - AND/OR combinations of other conditions</li>
 *   <li><strong>All Condition</strong> - Represents no filtering (SELECT all rows)</li>
 * </ul>
 *
 * <h3>Basic Usage</h3>
 * <p>Note: {@link is.codion.framework.domain.entity.attribute.Column} and
 * {@link is.codion.framework.domain.entity.attribute.ForeignKey} implement their respective
 * condition factory interfaces, allowing you to create conditions directly from attributes.
 * {@snippet :
 * // Column conditions - created directly from Column attributes
 * Condition nameStartsWithA = Customer.NAME.like("A%");
 * Condition ageOver18 = Customer.AGE.greaterThan(18);
 * Condition hasEmail = Customer.EMAIL.isNotNull();
 *
 * // Foreign key conditions
 * Entity usa = connection.selectSingle(Country.CODE.equalTo("US"));
 * Condition fromUSA = Customer.COUNTRY_FK.equalTo(usa);
 *
 * // Combining conditions
 * Condition complexCondition = and(
 *     nameStartsWithA,
 *     ageOver18,
 *     hasEmail,
 *     fromUSA
 * );
 *
 * // Using conditions in queries
 * List<Entity> customers = connection.select(complexCondition);
 *}
 *
 * <h3>Column Condition Examples</h3>
 * {@snippet :
 * // Equality
 * Track.NAME.equalTo("Yesterday")
 * Track.RATING.equalTo(5)
 *
 * // Comparison
 * Track.DURATION.greaterThan(180)
 * Invoice.TOTAL.between(10.0, 100.0)
 *
 * // Pattern matching
 * Artist.NAME.like("The %")
 * Artist.NAME.likeIgnoreCase("%beatles%")
 *
 * // Nullity
 * Customer.PHONE.isNull()
 * Customer.EMAIL.isNotNull()
 *
 * // Multiple values
 * Track.GENRE_ID.in(1, 2, 3)
 * Album.YEAR.notIn(1990, 1991, 1992)
 *}
 *
 * <h3>Foreign Key Condition Examples</h3>
 * {@snippet :
 * // Single entity reference
 * Entity metalGenre = connection.selectSingle(Genre.NAME.equalTo("Metal"));
 * Condition metalTracks = Track.GENRE_FK.equalTo(metalGenre);
 *
 * // Multiple entity references
 * List<Entity> selectedArtists = connection.select(Artist.NAME.like("A%"));
 * Condition bySelectedArtists = Album.ARTIST_FK.in(selectedArtists);
 *
 * // Null foreign key (orphaned records)
 * Condition noAlbum = Track.ALBUM_FK.isNull();
 *}
 *
 * <h3>Custom Conditions</h3>
 * <p>For complex queries that cannot be expressed with standard operators,
 * use {@link is.codion.framework.domain.entity.condition.ConditionType} to define
 * custom SQL conditions:
 * <p>
 * {@snippet :
 * // Define in entity interface
 * ConditionType NOT_IN_PLAYLIST = TYPE.conditionType("not_in_playlist");
 *
 * // Implement in entity definition
 * .condition(Track.NOT_IN_PLAYLIST, (columns, values) ->
 *     "track.id NOT IN (SELECT track_id FROM playlist_track WHERE playlist_id = ?)")
 *
 * // Use in queries
 * List<Entity> tracks = connection.select(
 *     Track.NOT_IN_PLAYLIST.get(Playlist.ID, playlistId));
 *}
 *
 * <h3>Condition Combinations</h3>
 * {@snippet :
 * // AND combination
 * Condition activeCustomers = and(
 *     Customer.ACTIVE.equalTo(true),
 *     Customer.LAST_ORDER_DATE.greaterThan(oneYearAgo)
 * );
 *
 * // OR combination
 * Condition importantCustomers = or(
 *     Customer.TOTAL_PURCHASES.greaterThan(10000),
 *     Customer.VIP.equalTo(true)
 * );
 *
 * // Complex nesting
 * Condition targetCustomers = and(
 *     activeCustomers,
 *     importantCustomers,
 *     Customer.EMAIL.isNotNull()
 * );
 *}
 *
 * <h3>Advanced Features</h3>
 *
 * <h4>Case Sensitivity</h4>
 * {@snippet :
 * // Case-insensitive operations
 * Artist.NAME.equalToIgnoreCase("the beatles")
 * Album.TITLE.likeIgnoreCase("%love%")
 *}
 *
 * <h4>All Condition</h4>
 * {@snippet :
 * // Select all rows (no WHERE clause)
 * List<Entity> allCustomers = connection.select(all(Customer.TYPE));
 *
 * // Useful for conditional filtering
 * Condition filter = searchTerm.isEmpty() ?
 *     all(Product.TYPE) :
 *     Product.NAME.like("%" + searchTerm + "%");
 *}
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Use column-specific methods for type safety (equalTo, greaterThan, etc.)</li>
 *   <li>Prefer foreign key conditions over joining on ID columns</li>
 *   <li>Use custom conditions for complex SQL that doesn't fit the standard API</li>
 *   <li>Combine conditions logically to build readable queries</li>
 *   <li>Leverage case-insensitive operations when appropriate</li>
 * </ul>
 * @see is.codion.framework.domain.entity.condition.Condition
 * @see is.codion.framework.domain.entity.condition.ColumnCondition
 * @see is.codion.framework.domain.entity.condition.ColumnConditionFactory
 * @see is.codion.framework.domain.entity.condition.ForeignKeyConditionFactory
 * @see is.codion.framework.domain.entity.condition.CustomCondition
 * @see is.codion.framework.domain.entity.condition.ConditionType
 */
@org.jspecify.annotations.NullMarked
package is.codion.framework.domain.entity.condition;