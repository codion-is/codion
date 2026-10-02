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

import is.codion.common.db.database.Database;
import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Generator;
import is.codion.framework.domain.entity.attribute.ColumnTemplate;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static is.codion.demos.chinook.domain.api.Chinook.DOMAIN;
import static is.codion.framework.domain.entity.condition.Condition.and;

/**
 * The {@link Column} javadoc snippets, each the region of the same name.
 * <p>{@link Track} mirrors a part of the Chinook domain API, for the usage region to declare.
 */
final class ColumnSnippets {

	private static final ColumnTemplate<String> REQUIRED_SEARCHABLE =
					column -> column.as()
									.column()
									.nullable(false)
									.searchable(true);

	interface Track { // @start region=usage
		EntityType TYPE = DOMAIN.entityType("chinook.track");

		// Column definitions
		Column<Long> ID = TYPE.longColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> COMPOSER = TYPE.stringColumn("composer");
		Column<Integer> MILLISECONDS = TYPE.integerColumn("milliseconds");
		Column<Integer> RATING = TYPE.integerColumn("rating");
		Column<BigDecimal> UNITPRICE = TYPE.bigDecimalColumn("unitprice");
	}

	EntityDefinition track() {
		return Track.TYPE.as()
						.attributes(
										Track.ID.as()
														.primaryKey()
														.generator(Generator.identity()),
										Track.NAME.as()
														.column()
														.caption("Name")
														.nullable(false)
														.maximumLength(200),
										Track.COMPOSER.as()
														.column()
														.caption("Composer")
														.maximumLength(220),
										Track.MILLISECONDS.as()
														.column()
														.caption("Duration")
														.nullable(false),
										Track.RATING.as()
														.column()
														.caption("Rating")
														.nullable(false)
														.defaultValue(5)
														.range(1, 10),
										Track.UNITPRICE.as()
														.column()
														.caption("Price")
														.nullable(false)
														.minimum(0)
														.fractionDigits(2))
						.build();
	} // @end

	void conditions(EntityConnection connection, Collection<Entity> artists) {
		// Query condition usage (inherited from ColumnConditions) // @start region=conditions
		List<Entity> bachTracks = connection.select(
						Track.COMPOSER.equalTo("Johann Sebastian Bach"));

		List<Entity> loveSongs = connection.select(
						Track.NAME.likeIgnoreCase("%love%"));

		List<Entity> longTracks = connection.select(
						Track.MILLISECONDS.greaterThanOrEqualTo(600_000));

		List<Entity> expensiveTracks = connection.select(
						Track.UNITPRICE.greaterThan(BigDecimal.valueOf(0.99)));

		// Complex conditions
		List<Entity> liveAlbums = connection.select(and(
						Album.ARTIST_FK.in(artists),
						Album.TITLE.likeIgnoreCase("%live%"))); // @end
	}

	void template() {
		Customer.LASTNAME.as(REQUIRED_SEARCHABLE) // @start region=template
						.maximumLength(20); // @end
	}

	void generator() {
		class UUIDGenerator implements Generator<String> { // @start region=generator

			@Override
			public void beforeInsert(Entity entity, Column<String> column, Database database, Connection connection) {
				// Only generate if not already set
				if (!entity.present(column)) {
					entity.set(column, UUID.randomUUID().toString());
				}
			}
		} // @end
	}

	void queried(EntityConnection connection, Entities entities, Entity customer) {
		// Custom query-based key generation, such as a function call // @start region=queried
		Invoice.TYPE.as()
						.attributes(
										Invoice.ID.as()
														.primaryKey()
														.generator(Generator.queried("SELECT chinook.next_invoice_id()")))
						.build();

		// Usage - the key is fetched before insert
		Entity invoice = entities.entity(Invoice.TYPE)
						.with(Invoice.CUSTOMER_FK, customer)
						.with(Invoice.DATE, LocalDate.now())
						.build();

		Entity.Key key = connection.insert(invoice);
		Long generatedId = key.get(Invoice.ID); // @end
	}

	void identity(EntityConnection connection, Entities entities) {
		// SQL Server, MySQL auto-increment, or similar // @start region=identity
		Customer.TYPE.as()
						.attributes(
										Customer.ID.as()
														.primaryKey()
														.generator(Generator.identity()))
						.build();

		// Usage - the database generates the key on insert
		Entity customer = entities.entity(Customer.TYPE)
						.with(Customer.FIRSTNAME, "John")
						.with(Customer.LASTNAME, "Doe")
						.with(Customer.EMAIL, "john@example.com")
						.build();

		Entity.Key key = connection.insert(customer);
		Long generatedId = key.get(Customer.ID); // @end
	}
}
