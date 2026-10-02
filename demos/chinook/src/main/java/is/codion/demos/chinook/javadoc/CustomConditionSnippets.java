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

import is.codion.demos.chinook.domain.api.Chinook.Playlist;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.domain.entity.condition.ConditionType;
import is.codion.framework.domain.entity.condition.CustomCondition;

import java.util.Collections;
import java.util.List;

import static is.codion.demos.chinook.domain.api.Chinook.DOMAIN;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.condition.Condition.and;

/**
 * The {@link CustomCondition} javadoc snippets, each the region of the same name.
 * <p>The usage region declares its entity, mirroring a part of the Chinook domain API.
 */
final class CustomConditionSnippets {

	// Define custom condition types in the entity interface // @start region=usage
	interface Track {
		EntityType TYPE = DOMAIN.entityType("chinook.track");

		Column<Long> ID = TYPE.longColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");

		// Custom condition with any number of values
		ConditionType NOT_IN_PLAYLISTS = TYPE.conditionType("not_in_playlists");
	}

	// Register custom conditions in the entity definition
	EntityDefinition track() {
		return Track.TYPE.as()
						.attributes(
										Track.ID.as()
														.primaryKey(),
										Track.NAME.as()
														.column()
														.caption("Name"))
						.condition(Track.NOT_IN_PLAYLISTS, (columns, values) ->
										// A parameter placeholder for each value
										"track.id NOT IN (SELECT track_id FROM chinook.playlisttrack WHERE playlist_id IN (" +
														String.join(", ", Collections.nCopies(values.size(), "?")) + "))")
						.build();
	}

	List<Entity> tracks(EntityConnection connection) {
		// Tracks not in specific playlists
		List<Entity> notInPlaylists = connection.select(
						Track.NOT_IN_PLAYLISTS.get(Playlist.ID, List.of(1L, 5L, 10L)));

		// Combine custom conditions with standard conditions
		Condition condition = and(
						Track.NAME.like("The%"),
						Track.NOT_IN_PLAYLISTS.get(Playlist.ID, List.of(1L)));

		// Use a Select for additional control
		return connection.select(
						Select.where(condition)
										.attributes(Track.NAME)
										.orderBy(ascending(Track.NAME)));
	} // @end
}
