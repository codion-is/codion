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

import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * The {@link Entities} javadoc snippets, each the region of the same name.
 */
final class EntitiesSnippets {

	void usage() {
		Domain domain = new ChinookImpl(); // @start region=usage
		Entities entities = domain.entities();

		// Create entity instances
		Entity customer = entities.entity(Customer.TYPE)
						.with(Customer.FIRSTNAME, "John")
						.with(Customer.LASTNAME, "Doe")
						.with(Customer.EMAIL, "john@example.com")
						.build();

		// Create primary keys
		Entity.Key customerKey = entities.primaryKey(Customer.TYPE, 42L); // @end
	}

	void connection(EntityConnection connection) {
		Entities entities = connection.entities(); // @start region=connection @end
	}

	void entity(Entities entities) {
		// Build an entity with initial values // @start region=entity
		Entity customer = entities.entity(Customer.TYPE)
						.with(Customer.FIRSTNAME, "John")
						.with(Customer.LASTNAME, "Doe")
						.with(Customer.EMAIL, "john@example.com")
						.build();

		// Build with a foreign key reference
		Entity invoice = entities.entity(Invoice.TYPE)
						.with(Invoice.CUSTOMER_FK, customer)
						.with(Invoice.DATE, LocalDate.now())
						.build(); // @end
	}

	void primaryKey(EntityConnection connection, Entities entities) {
		// Create a single-value primary key // @start region=primaryKey
		Entity.Key customerKey = entities.primaryKey(Customer.TYPE, 42L);

		// Use the key to fetch an entity
		Entity customer = connection.select(customerKey);

		// Keys can be compared
		Entity.Key anotherKey = entities.primaryKey(Customer.TYPE, 42L);
		customerKey.equals(anotherKey); // true

		// Null values are allowed
		Entity.Key nullKey = entities.primaryKey(Customer.TYPE, null); // @end
	}

	void primaryKeys(EntityConnection connection, Entities entities) {
		// Create multiple keys at once // @start region=primaryKeys
		List<Entity.Key> customerKeys = entities.primaryKeys(Customer.TYPE, 1L, 2L, 3L, 4L, 5L);

		// Fetch multiple entities
		Collection<Entity> customers = connection.select(customerKeys); // @end
	}

	void primaryKeysCollection(Entities entities) {
		// Create multiple keys at once // @start region=primaryKeysCollection
		List<Entity.Key> customerKeys = entities.primaryKeys(Customer.TYPE, List.of(1L, 2L, 3L, 4L, 5L)); // @end
	}
}
