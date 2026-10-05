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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
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
 *   <li><strong>{@link is.codion.framework.domain.entity.condition.ColumnConditions}</strong> -
 *       Conditions based on column values (equality, comparison, patterns, nullity)</li>
 *   <li><strong>{@link is.codion.framework.domain.entity.condition.ForeignKeyConditions}</strong> -
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
 * {@snippet class = "is.codion.demos.chinook.javadoc.ConditionPackageSnippets" region = "usage" :
 * // Column conditions - created directly from Column attributes // @start region=usage
 * Condition nameStartsWithA = Customer.LASTNAME.like("A%");
 * Condition fromUSA = Customer.COUNTRY.equalTo("USA");
 * Condition hasEmail = Customer.EMAIL.isNotNull();
 *
 * // Foreign key conditions
 * Entity peacock = connection.selectSingle(Employee.LASTNAME.equalTo("Peacock"));
 * Condition supportedByPeacock = Customer.SUPPORTREP_FK.equalTo(peacock);
 *
 * // Combining conditions
 * Condition condition = and(
 *         nameStartsWithA,
 *         fromUSA,
 *         hasEmail,
 *         supportedByPeacock);
 *
 * // Using conditions in queries
 * List<Entity> customers = connection.select(condition); // @end}
 *
 * <h3>Column Condition Examples</h3>
 * {@snippet class = "is.codion.demos.chinook.javadoc.ConditionPackageSnippets" region = "columnConditions" :
 * // Equality // @start region=columnConditions
 * Condition teenSpirit = Track.NAME.equalTo("Smells Like Teen Spirit");
 * Condition rated = Track.RATING.equalTo(5);
 *
 * // Comparison
 * Condition longTracks = Track.MILLISECONDS.greaterThan(180_000);
 * Condition totals = Invoice.TOTAL.between(BigDecimal.valueOf(10), BigDecimal.valueOf(100));
 *
 * // Pattern matching
 * Condition theArtists = Artist.NAME.like("The %");
 * Condition zeppelin = Artist.NAME.likeIgnoreCase("%zeppelin%");
 *
 * // Nullity
 * Condition noPhone = Customer.PHONE.isNull();
 * Condition hasEmail = Customer.EMAIL.isNotNull();
 *
 * // Multiple values
 * Condition genres = Track.GENRE_ID.in(1L, 2L, 3L);
 * Condition ratings = Album.RATING.notIn(1, 2, 3); // @end}
 *
 * <h3>Foreign Key Condition Examples</h3>
 * {@snippet class = "is.codion.demos.chinook.javadoc.ConditionPackageSnippets" region = "foreignKeyConditions" :
 * // Single entity reference // @start region=foreignKeyConditions
 * Entity metal = connection.selectSingle(Genre.NAME.equalTo("Metal"));
 * Condition metalTracks = Track.GENRE_FK.equalTo(metal);
 *
 * // Multiple entity references
 * List<Entity> artists = connection.select(Artist.NAME.like("A%"));
 * Condition byArtists = Album.ARTIST_FK.in(artists);
 *
 * // Null foreign key
 * Condition noGenre = Track.GENRE_FK.isNull(); // @end}
 *
 * <h3>Custom Conditions</h3>
 * <p>For complex queries that cannot be expressed with standard operators,
 * use {@link is.codion.framework.domain.entity.condition.ConditionType} to define
 * custom SQL conditions:
 * <p>
 * {@snippet class = "is.codion.demos.chinook.javadoc.ConditionTypeSnippets" region = "notInPlaylist" :
 * // Define a custom condition type for finding tracks not in a playlist // @start region=notInPlaylist
 * interface Track {
 *   EntityType TYPE = DOMAIN.entityType("chinook.track");
 *
 *   Column<Long> ID = TYPE.longColumn("id");
 *   Column<String> NAME = TYPE.stringColumn("name");
 *
 *   // Define a custom condition for complex subquery logic
 *   ConditionType NOT_IN_PLAYLIST = TYPE.conditionType("not_in_playlist");
 * }
 *
 * // In the entity definition, provide the SQL generation logic
 * EntityDefinition track() {
 *   return Track.TYPE.as()
 *           .attributes(
 *                   Track.ID.as()
 *                           .primaryKey(),
 *                   Track.NAME.as()
 *                           .column()
 *                           .caption("Name"))
 *           .condition(Track.NOT_IN_PLAYLIST, (columns, values) ->
 *                   "track.id NOT IN (SELECT track_id FROM chinook.playlisttrack WHERE playlist_id = ?)")
 *           .build();
 * }
 *
 * // Usage - find tracks not in a specific playlist
 * List<Entity> tracks(EntityConnection connection, Long playlistId) {
 *   return connection.select(Track.NOT_IN_PLAYLIST.get(Playlist.ID, playlistId));
 * } // @end}
 *
 * <h3>Condition Combinations</h3>
 * {@snippet class = "is.codion.demos.chinook.javadoc.ConditionPackageSnippets" region = "combinations" :
 * // AND combination // @start region=combinations
 * Condition longExpensiveTracks = and(
 *         Track.MILLISECONDS.greaterThan(300_000),
 *         Track.UNITPRICE.greaterThan(BigDecimal.valueOf(0.99)));
 *
 * // OR combination
 * Condition popularTracks = or(
 *         Track.RATING.greaterThanOrEqualTo(8),
 *         Track.PLAY_COUNT.greaterThan(100));
 *
 * // Complex nesting
 * Condition condition = and(
 *         longExpensiveTracks,
 *         popularTracks,
 *         Track.COMPOSER.isNotNull()); // @end}
 *
 * <h3>Advanced Features</h3>
 *
 * <h4>Case Sensitivity</h4>
 * {@snippet class = "is.codion.demos.chinook.javadoc.ConditionPackageSnippets" region = "caseSensitivity" :
 * // Case-insensitive operations // @start region=caseSensitivity
 * Condition zeppelin = Artist.NAME.equalToIgnoreCase("led zeppelin");
 * Condition loveAlbums = Album.TITLE.likeIgnoreCase("%love%"); // @end}
 *
 * <h4>All Condition</h4>
 * {@snippet class = "is.codion.demos.chinook.javadoc.ConditionPackageSnippets" region = "all" :
 * // Select all rows (no WHERE clause) // @start region=all
 * List<Entity> customers = connection.select(all(Customer.TYPE));
 *
 * // Useful for conditional filtering
 * Condition condition = searchText.isEmpty() ?
 *         all(Track.TYPE) :
 *         Track.NAME.like("%" + searchText + "%"); // @end}
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
 * @see is.codion.framework.domain.entity.condition.ColumnConditions
 * @see is.codion.framework.domain.entity.condition.ForeignKeyConditions
 * @see is.codion.framework.domain.entity.condition.CustomCondition
 * @see is.codion.framework.domain.entity.condition.ConditionType
 */
@org.jspecify.annotations.NullMarked
package is.codion.framework.domain.entity.condition;