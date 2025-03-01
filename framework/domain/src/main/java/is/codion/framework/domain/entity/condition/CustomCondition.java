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

import is.codion.framework.domain.entity.attribute.Column;

import java.util.List;

/**
 * <p>A custom {@link Condition} based on a {@link ConditionProvider}.
 * <p>Custom conditions are used to create query conditions that can not be created with the
 * {@link Condition}, {@link ColumnCondition} or {@link ForeignKeyCondition} APIs, for example
 * conditions using JOINs or native DBMS functionality.
 * <p>A {@link ConditionType} is associated with a {@link ConditionProvider}, which is responsible
 * for creating the condition string via {@link ConditionProvider#toString(List, List)}.
  * {@snippet :
 * // Custom condition with values
 * Track.TYPE.define(
 * // ...
 * ).condition(Track.NOT_IN_PLAYLIST, (columns, values) ->
 *         new StringBuilder("""
 *                 trackid NOT IN (
 *                     SELECT trackid
 *                     FROM chinook.playlisttrack
 *                     WHERE playlistid IN ("""
 *                  )
 *                 .append(String.join(", ",
 *                         Collections.nCopies(values.size(), "?")))
 *                 .append(")\n")
 *                 .append(")")
 *                 .toString());
 *
 * Condition condition =
 *         Track.NOT_IN_PLAYLIST.get(Playlist.ID, List.of(42L, 43L));
 *
 * // Custom condition without values
 * Track.TYPE.define(
 * // ...
 * ).condition(Track.EXCLUDED, (columns, values) ->
 *         "trackid not in (select trackid from chinook.excluded_tracks)");
 *
 * Condition condition = Track.EXCLUDED.get();
 *
 * List<Entity> tracks = connection.select(
 *         Condition.and(Track.NAME.like("The%"), condition));
 * }
 * <p>The ? substitute character is replaced with the condition values when when the statement is prepared.
 * That relies on the {@code columns} List for the value data type, and assumes it contains the {@link Column} associated
 * with each value at the same index. If the {@code columns} List is empty, no value substitution is performed.
 */
public interface CustomCondition extends Condition {

	/**
	 * @return the condition type
	 */
	ConditionType conditionType();
}
