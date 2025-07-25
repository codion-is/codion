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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.store.domain;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.test.DefaultEntityFactory;
import is.codion.framework.domain.test.DomainTest;
import is.codion.manual.store.domain.Store.Address;
import is.codion.manual.store.domain.Store.Customer;
import is.codion.manual.store.domain.Store.CustomerAddress;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

// tag::storeTest[]
public class StoreTest extends DomainTest {

	private static final Store DOMAIN = new Store();

	public StoreTest() {
		super(DOMAIN, StoreEntityFactory::new);
	}

	@Test
	public void customer() {
		test(Customer.TYPE);
	}

	@Test
	public void address() {
		test(Address.TYPE);
	}

	@Test
	public void customerAddress() {
		test(CustomerAddress.TYPE);
	}

	private static final class StoreEntityFactory extends DefaultEntityFactory {

		private StoreEntityFactory(EntityConnection connection) {
			super(connection);
		}

		@Override
		public Optional<Entity> entity(ForeignKey foreignKey) {
			// See if the currently running test requires an ADDRESS entity
			if (foreignKey.referencedType().equals(Address.TYPE)) {
				return Optional.of(connection().insertSelect(entities().entity(Address.TYPE)
								.with(Address.ID, 21L)
								.with(Address.STREET, "One Way")
								.with(Address.CITY, "Sin City")
								.build()));
			}

			return super.entity(foreignKey);
		}

		@Override
		public Entity entity(EntityType entityType) {
			if (entityType.equals(Address.TYPE)) {
				// Initialize an entity representing a record in the
				// STORE.ADDRESS table, to use for testing
				return entities().entity(Address.TYPE)
								.with(Address.ID, 42L)
								.with(Address.STREET, "Street")
								.with(Address.CITY, "City")
								.with(Address.VALID, true)
								.build();
			}
			else if (entityType.equals(Customer.TYPE)) {
				// Initialize an entity representing a record in the
				// STORE.CUSTOMER table, to use for testing
				return entities().entity(Customer.TYPE)
								.with(Customer.ID, UUID.randomUUID().toString())
								.with(Customer.FIRST_NAME, "Robert")
								.with(Customer.LAST_NAME, "Ford")
								.with(Customer.ACTIVE, true)
								.build();
			}
			else if (entityType.equals(CustomerAddress.TYPE)) {
				return entities().entity(CustomerAddress.TYPE)
								.with(CustomerAddress.CUSTOMER_FK, entity(CustomerAddress.CUSTOMER_FK).orElseThrow())
								.with(CustomerAddress.ADDRESS_FK, entity(CustomerAddress.ADDRESS_FK).orElseThrow())
								.build();
			}

			return super.entity(entityType);
		}

		@Override
		public void modify(Entity entity) {
			if (entity.type().equals(Address.TYPE)) {
				entity.set(Address.STREET, "New Street");
				entity.set(Address.CITY, "New City");
			}
			else if (entity.type().equals(Customer.TYPE)) {
				// It is sufficient to change the value of a
				// single property, but the more, the merrier
				entity.set(Customer.FIRST_NAME, "Jesse");
				entity.set(Customer.LAST_NAME, "James");
				entity.set(Customer.ACTIVE, false);
			}
		}
	}
}
// end::storeTest[]