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
package is.codion.manual.quickstart;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityFormatter;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.attribute.Column.Generator.automatic;
import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;

// tag::store[]
// tag::storeDomain[]
public class Store extends DomainModel {

	public static final DomainType DOMAIN = domainType(Store.class);

	public Store() {
		super(DOMAIN);
		add(customer(), address(), customerAddress());
	}
	// end::storeDomain[]

	// tag::customerApi[]
	public interface Customer {
		EntityType TYPE = DOMAIN.entityType("store.customer");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
		Column<String> LAST_NAME = TYPE.stringColumn("last_name");
	}
	// end::customerApi[]

	// tag::customerImpl[]
	EntityDefinition customer() {
		return Customer.TYPE.as(
										Customer.ID.as()
														.primaryKey()
														.generator(identity()),
										Customer.FIRST_NAME.as()
														.column()
														.caption("First name")
														.nullable(false)
														.maximumLength(40),
										Customer.LAST_NAME.as()
														.column()
														.caption("Last name")
														.nullable(false)
														.maximumLength(40))
						.formatter(EntityFormatter.builder()
										.value(Customer.LAST_NAME)
										.text(", ")
										.value(Customer.FIRST_NAME)
										.build())
						.build();
	}
	// end::customerImpl[]

	// tag::address[]
	public interface Address {
		EntityType TYPE = DOMAIN.entityType("store.address");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> STREET = TYPE.stringColumn("street");
		Column<String> CITY = TYPE.stringColumn("city");
	}

	EntityDefinition address() {
		return Address.TYPE.as(
										Address.ID.as()
														.primaryKey()
														.generator(automatic("store.address")),
										Address.STREET.as()
														.column()
														.caption("Street")
														.nullable(false)
														.maximumLength(120),
										Address.CITY.as()
														.column()
														.caption("City")
														.nullable(false)
														.maximumLength(50))
						.formatter(EntityFormatter.builder()
										.value(Address.STREET)
										.text(", ")
										.value(Address.CITY)
										.build())
						.build();
	}
	// end::address[]

	// tag::customerAddress[]
	public interface CustomerAddress {
		EntityType TYPE = DOMAIN.entityType("store.customer_address");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> CUSTOMER_ID = TYPE.integerColumn("customer_id");
		Column<Integer> ADDRESS_ID = TYPE.integerColumn("address_id");

		ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
		ForeignKey ADDRESS_FK = TYPE.foreignKey("address_fk", ADDRESS_ID, Address.ID);
	}

	EntityDefinition customerAddress() {
		return CustomerAddress.TYPE.as(
										CustomerAddress.ID.as()
														.primaryKey()
														.generator(automatic("store.customer_address")),
										CustomerAddress.CUSTOMER_ID.as()
														.column()
														.nullable(false),
										CustomerAddress.CUSTOMER_FK.as()
														.foreignKey()
														.caption("Customer"),
										CustomerAddress.ADDRESS_ID.as()
														.column()
														.nullable(false),
										CustomerAddress.ADDRESS_FK.as()
														.foreignKey()
														.caption("Address"))
						.caption("Customer address")
						.build();
	}
	// end::customerAddress[]
}
// end::store[]
