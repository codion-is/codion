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

import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.demos.chinook.domain.api.Chinook.Playlist;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.ConditionType;

import java.util.List;

import static is.codion.demos.chinook.domain.api.Chinook.DOMAIN;

/**
 * The {@link ConditionType} javadoc snippets, each the region of the same name.
 * <p>Each region declares its entity, mirroring a part of the Chinook domain API, in a holder class of its own.
 */
final class ConditionTypeSnippets {

	static final class NotInPlaylist {

		// Define a custom condition type for finding tracks not in a playlist // @start region=notInPlaylist
		interface Track {
			EntityType TYPE = DOMAIN.entityType("chinook.track");

			Column<Long> ID = TYPE.longColumn("id");
			Column<String> NAME = TYPE.stringColumn("name");

			// Define a custom condition for complex subquery logic
			ConditionType NOT_IN_PLAYLIST = TYPE.conditionType("not_in_playlist");
		}

		// In the entity definition, provide the SQL generation logic
		EntityDefinition track() {
			return Track.TYPE.as()
							.attributes(
											Track.ID.as()
															.primaryKey(),
											Track.NAME.as()
															.column()
															.caption("Name"))
							.condition(Track.NOT_IN_PLAYLIST, (columns, values) ->
											"track.id NOT IN (SELECT track_id FROM chinook.playlisttrack WHERE playlist_id = ?)")
							.build();
		}

		// Usage - find tracks not in a specific playlist
		List<Entity> tracks(EntityConnection connection, Long playlistId) {
			return connection.select(Track.NOT_IN_PLAYLIST.get(Playlist.ID, playlistId));
		} // @end
	}

	static final class MultipleColumns {

		interface Invoice { // @start region=multipleColumns
			EntityType TYPE = DOMAIN.entityType("chinook.invoice");

			Column<Long> ID = TYPE.longColumn("id");

			// Define a condition that uses multiple columns
			ConditionType CONTAINS_TRACK = TYPE.conditionType("contains_track");
		}

		// In the entity definition
		EntityDefinition invoice() {
			return Invoice.TYPE.as()
							.attributes(
											Invoice.ID.as()
															.primaryKey())
							.condition(Invoice.CONTAINS_TRACK, (columns, values) ->
											"invoice.id IN (SELECT invoice_id FROM chinook.invoiceline WHERE track_id = ? AND quantity >= ?)")
							.build();
		}

		// Usage - a column for each value, used when binding it
		List<Entity> invoices(EntityConnection connection, Long trackId) {
			return connection.select(Invoice.CONTAINS_TRACK.get(
							List.of(InvoiceLine.TRACK_ID, InvoiceLine.QUANTITY),
							List.of(trackId, 2)));
		} // @end
	}

	static final class NoValues {

		interface Track { // @start region=noValues
			EntityType TYPE = DOMAIN.entityType("chinook.track");

			Column<Long> ID = TYPE.longColumn("id");

			// Define a condition without columns or values
			ConditionType NOT_PURCHASED = TYPE.conditionType("not_purchased");
		}

		// In the entity definition
		EntityDefinition track() {
			return Track.TYPE.as()
							.attributes(
											Track.ID.as()
															.primaryKey())
							.condition(Track.NOT_PURCHASED, (columns, values) ->
											"track.id NOT IN (SELECT track_id FROM chinook.invoiceline)")
							.build();
		}

		// Usage
		List<Entity> tracks(EntityConnection connection) {
			return connection.select(Track.NOT_PURCHASED.get());
		} // @end
	}
}
