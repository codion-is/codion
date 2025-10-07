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
package is.codion.manual.framework.domain;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.query.EntitySelectQuery;

import static is.codion.framework.domain.DomainType.domainType;

public final class CustomSelectQueries {

	static final DomainType DOMAIN = domainType(CustomSelectQueries.class);

	// tag::fromClause[]
	// Domain API
	interface Album {
		EntityType TYPE = DOMAIN.entityType("store.album");

		Column<Long> ID = TYPE.longColumn("id");
		Column<String> TITLE = TYPE.stringColumn("title");
		// Further attributes skipped for brevity
	}

	interface Track {
		EntityType TYPE = DOMAIN.entityType("store.track");

		Column<Long> ID = TYPE.longColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<Long> ALBUM_ID = TYPE.longColumn("album_id");
		Column<String> ALBUM_TITLE = TYPE.stringColumn("album_title");
		Column<String> ARTIST_NAME = TYPE.stringColumn("artist_name");

		ForeignKey ALBUM_FK = TYPE.foreignKey("album_fk", ALBUM_ID, Album.ID);
	}

	// Domain implementation
	static class StoreDomain extends DomainModel {
		StoreDomain() {
			super(DOMAIN);
			// add(Album.TYPE.define(....
			add(Track.TYPE.define(
											Track.ID.define()
															.primaryKey(),
											Track.NAME.define()
															.column()
															.caption("Name")
															// set the expression since the column 'NAME' is ambiguous
															.expression("track.name"),
											Track.ALBUM_ID.define()
															.column(),
											Track.ALBUM_FK.define()
															.foreignKey(),
											// These columns come from the joined tables,
											Track.ARTIST_NAME.define()
															.column()
															.caption("Artist")
															// set the expression since the column 'NAME' is ambiguous
															.expression("artist.name")
															.readOnly(true), // always mark denormalized values as read-only
											Track.ALBUM_TITLE.define()
															.column()
															.caption("Album")
															// No need for an expression, since 'TITLE' is unambiguous
															.readOnly(true)) // always mark denormalized values as read-only
							// Custom FROM clause to join album and artist tables
							.selectQuery(EntitySelectQuery.builder()
											.from("store.track " +
															"JOIN store.album ON track.album_id = album.id " +
															"JOIN store.artist ON album.artist_id = artist.id")
											.build())
							.build());
		}
	}
	// end::fromClause[]

	// tag::whereClause[]
	interface AvailableTrack {
		EntityType TYPE = DOMAIN.entityType("store.available_track");

		Column<Long> ID = TYPE.longColumn("id");
		Column<String> TITLE = TYPE.stringColumn("title");
	}

	static class AvailableTracksDomain extends DomainModel {
		AvailableTracksDomain() {
			super(DOMAIN);
			add(AvailableTrack.TYPE.define(
											AvailableTrack.ID.define()
															.primaryKey(),
											AvailableTrack.TITLE.define()
															.column()
															.caption("Title"))
							// Static WHERE clause filters to available tracks only
							.selectQuery(EntitySelectQuery.builder()
											.from("store.track")
											.where("available = true")
											.build())
							.readOnly(true)
							.build());
		}
	}
	// end::whereClause[]
}
