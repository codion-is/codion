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

import java.util.List;

/**
 * A custom {@link Condition} based on a {@link ConditionString}.
 * <p>
 * Custom conditions are used to create query conditions that cannot be created with the
 * standard {@link Condition}, {@link ColumnConditions} or {@link ForeignKeyConditions} APIs.
 * They enable complex SQL constructs such as subqueries, JOINs, window functions, and
 * database-specific functionality.
 * <p>
 * A {@link ConditionType} is associated with a {@link ConditionString}, which is responsible
 * for creating the condition string via {@link ConditionString#get(List, List)}.
 * The {@code ?} substitute character is replaced with condition values when the statement is prepared.
 * <p>
 * Custom conditions provide the flexibility to use any SQL construct while maintaining
 * type safety and parameter binding:
 * {@snippet class = is.codion.demos.chinook.javadoc.CustomConditionSnippets region = usage :
 * // Define custom condition types in the entity interface
 * interface Track {
 * 	EntityType TYPE = DOMAIN.entityType("chinook.track");
 *
 * 	Column<Long> ID = TYPE.longColumn("id");
 * 	Column<String> NAME = TYPE.stringColumn("name");
 *
 * 	// Custom condition with any number of values
 * 	ConditionType NOT_IN_PLAYLISTS = TYPE.conditionType("not_in_playlists");
 * }
 *
 * // Register custom conditions in the entity definition
 * EntityDefinition track() {
 * 	return Track.TYPE.as()
 * 					.attributes(
 * 									Track.ID.as()
 * 													.primaryKey(),
 * 									Track.NAME.as()
 * 													.column()
 * 													.caption("Name"))
 * 					.condition(Track.NOT_IN_PLAYLISTS, (columns, values) ->
 * 									// A parameter placeholder for each value
 * 									"track.id NOT IN (SELECT track_id FROM chinook.playlisttrack WHERE playlist_id IN (" +
 * 													String.join(", ", Collections.nCopies(values.size(), "?")) + "))")
 * 					.build();
 * }
 *
 * List<Entity> tracks(EntityConnection connection) {
 * 	// Tracks not in specific playlists
 * 	List<Entity> notInPlaylists = connection.select(
 * 					Track.NOT_IN_PLAYLISTS.get(Playlist.ID, List.of(1L, 5L, 10L)));
 *
 * 	// Combine custom conditions with standard conditions
 * 	Condition condition = and(
 * 					Track.NAME.like("The%"),
 * 					Track.NOT_IN_PLAYLISTS.get(Playlist.ID, List.of(1L)));
 *
 * 	// Use a Select for additional control
 * 	return connection.select(
 * 					Select.where(condition)
 * 									.attributes(Track.NAME)
 * 									.orderBy(ascending(Track.NAME)));
 * }}
 * @see ConditionType
 * @see ConditionString
 * @see #conditionType()
 */
public sealed interface CustomCondition extends Condition permits DefaultCustomCondition {

	/**
	 * @return the condition type
	 */
	ConditionType conditionType();
}
