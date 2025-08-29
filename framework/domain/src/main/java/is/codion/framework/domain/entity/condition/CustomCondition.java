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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import java.util.List;

/**
 * A custom {@link Condition} based on a {@link ConditionProvider}.
 * <p>
 * Custom conditions are used to create query conditions that cannot be created with the
 * standard {@link Condition}, {@link ColumnConditionFactory} or {@link ForeignKeyConditionFactory} APIs.
 * They enable complex SQL constructs such as subqueries, JOINs, window functions, and
 * database-specific functionality.
 * <p>
 * A {@link ConditionType} is associated with a {@link ConditionProvider}, which is responsible
 * for creating the condition string via {@link ConditionProvider#toString(List, List)}.
 * The {@code ?} substitute character is replaced with condition values when the statement is prepared.
 * <p>
 * Custom conditions provide the flexibility to use any SQL construct while maintaining
 * type safety and parameter binding:
 * {@snippet :
 * // Define custom condition types in entity interface
 * interface Track {
 *     EntityType TYPE = DOMAIN.entityType("chinook.track");
 *
 *     // Custom condition with dynamic values
 *     ConditionType NOT_IN_PLAYLIST = TYPE.conditionType("notInPlaylist");
 *
 *     // Custom condition without values
 *     ConditionType EXCLUDED = TYPE.conditionType("excluded");
 *
 *     // Custom condition with complex SQL
 *     ConditionType TOP_TRACKS_BY_GENRE = TYPE.conditionType("topTracksByGenre");
 * }
 *
 * // Register custom conditions in entity definition
 * Track.TYPE.define(
 *         // ... attribute definitions
 *         )
 *     .condition(Track.NOT_IN_PLAYLIST, (columns, values) -> {
 *         // Dynamic subquery with parameter placeholders
 *         return new StringBuilder()
 *             .append("trackid NOT IN (")
 *             .append("    SELECT trackid FROM chinook.playlisttrack")
 *             .append("    WHERE playlistid IN (")
 *             .append(String.join(", ", Collections.nCopies(values.size(), "?")))
 *             .append("))")
 *             .toString();
 *     })
 *     .condition(Track.EXCLUDED, (columns, values) ->
 *         // Static condition without parameters
 *         "trackid NOT IN (SELECT trackid FROM chinook.excluded_tracks)")
 *     .condition(Track.TOP_TRACKS_BY_GENRE, (columns, values) ->
 *         // Complex condition with window functions
 *         """
 *         trackid IN (
 *             SELECT trackid FROM (
 *                 SELECT trackid,
 *                        ROW_NUMBER() OVER (PARTITION BY genreid ORDER BY playcount DESC) as rn
 *                 FROM chinook.track_stats
 *                 WHERE genreid = ?
 *             ) ranked
 *             WHERE rn <= ?
 *         )
 *         """)
 *     .build();
 *
 * // Usage examples
 *
 * // Custom condition with values - tracks not in specific playlists
 * List<Long> playlistIds = List.of(1L, 5L, 10L);
 * Condition notInPlaylists = Track.NOT_IN_PLAYLIST.get(Playlist.ID, playlistIds);
 * List<Entity> availableTracks = connection.select(notInPlaylists);
 *
 * // Custom condition without values - excluded tracks
 * Condition excludedCondition = Track.EXCLUDED.get();
 * List<Entity> nonExcludedTracks = connection.select(excludedCondition);
 *
 * // Complex custom condition with multiple parameters
 * Condition topRockTracks = Track.TOP_TRACKS_BY_GENRE.get(
 *     List.of(Genre.ID, Track.LIMIT),
 *     List.of(1, 10)); // Genre ID 1, top 10 tracks
 *
 * // Combine custom conditions with standard conditions
 * List<Entity> complexQuery = connection.select(and(
 *     Track.NAME.like("The%"),
 *     Track.NOT_IN_PLAYLIST.get(Playlist.ID, List.of(1L)),
 *     Track.UNIT_PRICE.greaterThan(new BigDecimal("0.99"))));
 *
 * // Use with Select builder for additional control
 * List<Entity> detailedQuery = connection.select(
 *     Select.where(Track.TOP_TRACKS_BY_GENRE.get(
 *             List.of(Genre.ID, Track.LIMIT), List.of(genreId, 5)))
 *         .attributes(Track.NAME, Track.ALBUM_FK, Track.UNIT_PRICE)
 *         .orderBy(descending(Track.UNIT_PRICE))
 *         .build());
 *}
 * @see ConditionType
 * @see ConditionProvider
 * @see #conditionType()
 */
public interface CustomCondition extends Condition {

	/**
	 * @return the condition type
	 */
	ConditionType conditionType();
}
