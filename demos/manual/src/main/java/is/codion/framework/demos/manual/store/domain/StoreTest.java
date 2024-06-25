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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.store.domain;

import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.domain.Store.CustomerAddress;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.test.DefaultEntityFactory;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

// tag::storeTest[]
public class StoreTest extends EntityTestUnit {

	private static final Store DOMAIN = new Store();

	public StoreTest() {
		super(DOMAIN, new StoreEntityFactory());
	}

	@Test
	public void customer() throws Exception {
		test(Customer.TYPE);
	}

	@Test
	public void address() throws Exception {
		test(Address.TYPE);
	}

	@Test
	public void customerAddress() throws Exception {
		test(CustomerAddress.TYPE);
	}

	private static final class StoreEntityFactory extends DefaultEntityFactory {

		private StoreEntityFactory() {
			super(DOMAIN.entities());
		}

		@Override
		public Entity foreignKeyEntity(ForeignKey foreignKey,
																	 Map<ForeignKey, Entity> foreignKeyEntities) {
			//see if the currently running test requires an ADDRESS entity
			if (foreignKey.referencedType().equals(Address.TYPE)) {
				return entities().builder(Address.TYPE)
								.with(Address.ID, 21L)
								.with(Address.STREET, "One Way")
								.with(Address.CITY, "Sin City")
								.build();
			}

			return super.foreignKeyEntity(foreignKey, foreignKeyEntities);
		}

		@Override
		public Entity entity(EntityType entityType,
												 Map<ForeignKey, Entity> foreignKeyEntities) {
			if (entityType.equals(Address.TYPE)) {
				//Initialize an entity representing the table STORE.ADDRESS,
				//which can be used for the testing
				return entities().builder(Address.TYPE)
								.with(Address.ID, 42L)
								.with(Address.STREET, "Street")
								.with(Address.CITY, "City")
								.build();
			}
			else if (entityType.equals(Customer.TYPE)) {
				//Initialize an entity representing the table STORE.CUSTOMER,
				//which can be used for the testing
				return entities().builder(Customer.TYPE)
								.with(Customer.ID, UUID.randomUUID().toString())
								.with(Customer.FIRST_NAME, "Robert")
								.with(Customer.LAST_NAME, "Ford")
								.with(Customer.ACTIVE, true)
								.build();
			}
			else if (entityType.equals(CustomerAddress.TYPE)) {
				return entities().builder(CustomerAddress.TYPE)
								.with(CustomerAddress.CUSTOMER_FK, foreignKeyEntities.get(Customer.TYPE))
								.with(CustomerAddress.ADDRESS_FK, foreignKeyEntities.get(Address.TYPE))
								.build();
			}

			return super.entity(entityType, foreignKeyEntities);
		}

		@Override
		public void modify(Entity entity, Map<ForeignKey, Entity> foreignKeyEntities) {
			if (entity.entityType().equals(Address.TYPE)) {
				entity.put(Address.STREET, "New Street");
				entity.put(Address.CITY, "New City");
			}
			else if (entity.entityType().equals(Customer.TYPE)) {
				//It is sufficient to change the value of a single property, but the more, the merrier
				entity.put(Customer.FIRST_NAME, "Jesse");
				entity.put(Customer.LAST_NAME, "James");
				entity.put(Customer.ACTIVE, false);
			}
		}
	}
}
// end::storeTest[]