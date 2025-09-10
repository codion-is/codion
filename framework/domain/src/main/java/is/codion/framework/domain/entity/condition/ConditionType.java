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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Defines a custom condition type that can be used to create complex SQL WHERE clauses
 * that cannot be expressed using the standard {@link Condition} API.
 * <p>
 * Custom conditions are particularly useful for:
 * <ul>
 *   <li>Subqueries (EXISTS, IN, NOT IN)</li>
 *   <li>Complex joins or correlations</li>
 *   <li>Database-specific functions or syntax</li>
 *   <li>Conditions that require multiple columns or complex value transformations</li>
 * </ul>
 * <p>
 * A {@link ConditionType} is created on an {@link EntityType} and serves as a named identifier
 * for a custom condition. The actual SQL generation logic is provided via a {@link ConditionString}
 * when defining the entity.
 * <p>
 * {@snippet :
 * // Define a custom condition type for finding tracks not in a playlist
 * interface Track {
 *     EntityType TYPE = DOMAIN.entityType("music.track");
 *
 *     Column<Long> ID = TYPE.longColumn("id");
 *     Column<String> NAME = TYPE.stringColumn("name");
 *
 *     // Define a custom condition for complex subquery logic
 *     ConditionType NOT_IN_PLAYLIST = TYPE.conditionType("not_in_playlist");
 * }
 *
 * // In the entity definition, provide the SQL generation logic
 * EntityDefinition track() {
 *     return Track.TYPE.define(
 *             Track.ID.define()
 *                 .primaryKey(),
 *             Track.NAME.define()
 *                 .column())
 *         .condition(Track.NOT_IN_PLAYLIST, (columns, values) ->
 *             "track.id NOT IN (SELECT track_id FROM playlist_track WHERE playlist_id = ?)")
 *         .build();
 * }
 *
 * // Usage - find tracks not in a specific playlist
 * Long playlistId = 42L;
 * List<Entity> tracks = connection.select(
 *     Track.NOT_IN_PLAYLIST.get(Playlist.ID, playlistId));
 *}
 * <p>
 * Example with multiple columns:
 * {@snippet :
 * // Define a condition that uses multiple columns
 * ConditionType OVERLAPPING_DATES = TYPE.conditionType("overlapping_dates");
 *
 * // In entity definition
 * .condition(Event.OVERLAPPING_DATES, (columns, values) -> {
 *     return "((start_date <= ? AND end_date >= ?) OR " +
 *            "(start_date <= ? AND end_date >= ?) OR " +
 *            "(start_date >= ? AND end_date <= ?))";
 * })
 *
 * // Usage with multiple column/value pairs
 * List<Column<?>> columns = List.of(
 *     Event.START_DATE, Event.END_DATE,
 *     Event.START_DATE, Event.END_DATE,
 *     Event.START_DATE, Event.END_DATE);
 * List<Object> values = List.of(
 *     searchStart, searchStart,
 *     searchEnd, searchEnd,
 *     searchStart, searchEnd);
 *
 * List<Entity> overlapping = connection.select(
 *     Event.OVERLAPPING_DATES.get(columns, values));
 *}
 * <p>
 * Example with no columns (using only values):
 * {@snippet :
 * // Define a condition that doesn't need column references
 * ConditionType WITHIN_RADIUS = TYPE.conditionType("within_radius");
 *
 * // In entity definition - values are: latitude, longitude, radius
 * .condition(Location.WITHIN_RADIUS, (columns, values) -> {
 *     return "ST_Distance(coordinates, ST_MakePoint(?, ?)) <= ?";
 * })
 *
 * // Usage with just values
 * List<Entity> nearby = connection.select(
 *     Location.WITHIN_RADIUS.get(List.of(40.7128, -74.0060, 10.0)));
 *}
 * @see CustomCondition
 * @see ConditionString
 * @see EntityDefinition#condition(ConditionType)
 * @see EntityDefinition.Builder#condition(ConditionType, ConditionString)
 */
public sealed interface ConditionType permits DefaultConditionType {

	/**
	 * @return the entity type
	 */
	EntityType entityType();

	/**
	 * @return the name
	 */
	String name();

	/**
	 * Returns a {@link CustomCondition} based on the {@link ConditionString} associated with this {@link ConditionType}
	 * @return a {@link CustomCondition} instance
	 * @see EntityDefinition#condition(ConditionType)
	 */
	CustomCondition get();

	/**
	 * <p>Returns a {@link CustomCondition} based on the {@link ConditionString} associated with this {@link ConditionType}
	 * <p>This method assumes that the {@link ConditionString} is not based on any columns or has no need for them when creating the condition string.
	 * <p>Note that {@link ConditionString#get(List, List)} will receive an empty column list.
	 * @param values the values used by this condition
	 * @return a {@link CustomCondition} instance
	 * @see EntityDefinition#condition(ConditionType)
	 */
	CustomCondition get(List<?> values);

	/**
	 * Returns a {@link CustomCondition} based on the {@link ConditionString} associated with this {@link ConditionType}
	 * @param column the column representing the value used by this condition
	 * @param value the value used by this condition string
	 * @param <T> the column type
	 * @return a {@link CustomCondition} instance
	 * @see EntityDefinition#condition(ConditionType)
	 */
	<T> CustomCondition get(Column<T> column, @Nullable T value);

	/**
	 * Returns a {@link CustomCondition} based on the {@link ConditionString} associated with this {@link ConditionType}
	 * @param column the column representing the values used by this condition, assuming all the values are for the same column
	 * @param values the values used by this condition string
	 * @param <T> the column type
	 * @return a {@link CustomCondition} instance
	 * @see EntityDefinition#condition(ConditionType)
	 */
	<T> CustomCondition get(Column<T> column, List<T> values);

	/**
	 * Returns a {@link CustomCondition} based on the {@link ConditionString} associated with this {@link ConditionType}
	 * @param columns the columns representing the values used by this condition, in the same order as their respective values
	 * @param values the values used by this condition string in the same order as their respective columns
	 * @return a {@link CustomCondition} instance
	 * @throws IllegalArgumentException in case the number of columns does not match the number of values
	 * @see EntityDefinition#condition(ConditionType)
	 */
	CustomCondition get(List<Column<?>> columns, List<?> values);

	/**
	 * Instantiates a new {@link ConditionType} for the given entity type
	 * @param entityType the entityType
	 * @param name the name
	 * @return a new condition type
	 */
	static ConditionType conditionType(EntityType entityType, String name) {
		return new DefaultConditionType(entityType, name);
	}
}
