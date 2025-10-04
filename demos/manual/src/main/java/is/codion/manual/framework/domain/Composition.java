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
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.manual.framework.domain.Composition.Orders.Customer;
import is.codion.manual.framework.domain.Composition.Products.Product;

import static is.codion.framework.domain.DomainType.domainType;

public class Composition {
	// tag::composition[]
	// Base domain with product catalog
	static class Products extends DomainModel {

		static final DomainType DOMAIN = domainType("products");

		interface Product {
			EntityType TYPE = DOMAIN.entityType("products.product");

			Column<Integer> ID = TYPE.integerColumn("id");
			Column<String> NAME = TYPE.stringColumn("name");
		}

		public Products() {
			super(DOMAIN);
			add(product());
		}

		EntityDefinition product() {
			return Product.TYPE.define(
											Product.ID.define()
															.primaryKey(),
											Product.NAME.define()
															.column())
							.build();
		}
	}

	// Orders domain composes Products and adds customer/order entities
	static class Orders extends DomainModel {

		static final DomainType DOMAIN = domainType("orders");

		interface Customer {
			EntityType TYPE = DOMAIN.entityType("orders.customer");

			Column<Integer> ID = TYPE.integerColumn("id");
			Column<String> NAME = TYPE.stringColumn("first_name");
		}

		interface Order {
			EntityType TYPE = DOMAIN.entityType("orders.order");

			Column<Integer> ID = TYPE.integerColumn("id");
			Column<Integer> CUSTOMER_ID = TYPE.integerColumn("customer_id");
			Column<Integer> PRODUCT_ID = TYPE.integerColumn("product_id");

			ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
			// Foreign key referencing composed Products domain
			ForeignKey PRODUCT_FK = TYPE.foreignKey("product_fk", PRODUCT_ID, Product.ID);
		}

		public Orders() {
			super(DOMAIN);
			// Include entire Products domain
			add(new Products());
			add(customer(), order());
		}

		EntityDefinition customer() {
			return Customer.TYPE.define(
											Customer.ID.define()
															.primaryKey(),
											Customer.NAME.define()
															.column())
							.build();
		}

		EntityDefinition order() {
			return Order.TYPE.define(
											Order.ID.define()
															.primaryKey(),
											Order.CUSTOMER_ID.define()
															.column(),
											Order.CUSTOMER_FK.define()
															.foreignKey(),
											Order.PRODUCT_ID.define()
															.column(),
											Order.PRODUCT_FK.define()
															.foreignKey())
							.build();
		}
	}

	// Store domain composes Orders (which transitively includes Products)
	static class Store extends DomainModel {

		static final DomainType DOMAIN = domainType("store");

		interface Employee {
			EntityType TYPE = DOMAIN.entityType("store.employee");

			Column<Integer> ID = TYPE.integerColumn("id");
			Column<String> NAME = TYPE.stringColumn("first_name");
		}

		public Store() {
			super(DOMAIN);
			// Includes Orders domain (and transitively Products)
			add(new Orders());
			add(employee());
		}

		EntityDefinition employee() {
			return Employee.TYPE.define(
											Employee.ID.define()
															.primaryKey(),
											Employee.NAME.define()
															.column())
							.build();
		}
	}
	// end::composition[]

	// tag::selectiveComposition[]
	// Website domain selectively includes entities and functionality from other domains
	static class StoreWebSite extends DomainModel {

		static final DomainType DOMAIN = domainType("website");

		public StoreWebSite() {
			super(DOMAIN);
			Entities orderEntities = new Orders().entities();
			// Selectively add specific entities from Orders domain
			add(orderEntities.definition(Product.TYPE));
			add(orderEntities.definition(Customer.TYPE));
			// Include only reports from Products domain
			addReports(new Products());
			// Include only functions from Store domain
			addFunctions(new Store());
			// Include only procedures from Orders domain
			addProcedures(new Orders());
		}
	}
	// end::selectiveComposition[]
}
