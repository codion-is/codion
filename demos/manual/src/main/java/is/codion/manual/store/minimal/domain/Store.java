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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.store.minimal.domain;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityFormatter;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Generator;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import java.util.function.Function;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;

// Extend the DomainModel class.
public class Store extends DomainModel {

	// Create a DomainType constant identifying the domain model.
	public static final DomainType DOMAIN = domainType(Store.class);

	// Create a namespace interface for the Customer entity.
	public interface Customer {
		// Use the DomainType and the table name to create an
		// EntityType constant identifying the entity.
		EntityType TYPE = DOMAIN.entityType("store.customer");

		// Use the EntityType to create typed Column constants for each column.
		Column<Long> ID = TYPE.longColumn("id");
		Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
		Column<String> LAST_NAME = TYPE.stringColumn("last_name");
		Column<String> EMAIL = TYPE.stringColumn("email");
		Column<Boolean> ACTIVE = TYPE.booleanColumn("active");
	}

	// Create a namespace interface for the Address entity.
	public interface Address {
		EntityType TYPE = DOMAIN.entityType("store.address");

		Column<Long> ID = TYPE.longColumn("id");
		Column<Long> CUSTOMER_ID = TYPE.longColumn("customer_id");
		Column<String> STREET = TYPE.stringColumn("street");
		Column<String> CITY = TYPE.stringColumn("city");

		// Use the EntityType to create a ForeignKey
		// constant for the foreign key relationship.
		ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
	}

	public Store() {
		super(DOMAIN);
		// Use the Customer.TYPE constant to define a new entity,
		// based on attributes defined using the Column constants.
		// This entity definition is then added to the domain model.
		add(Customer.TYPE.as(                   // returns EntityDefinition.Builder
										Customer.ID.as()
														.primaryKey()       // returns ColumnDefinition.Builder
														.generator(identity()),
										Customer.FIRST_NAME.as()
														.column()           // returns ColumnDefinition.Builder
														.caption("First name")
														.nullable(false)
														.maximumLength(40),
										Customer.LAST_NAME.as()
														.column()
														.caption("Last name")
														.nullable(false)
														.maximumLength(40),
										Customer.EMAIL.as()
														.column()
														.caption("Email")
														.maximumLength(100),
										Customer.ACTIVE.as()
														.column()
														.caption("Active")
														.nullable(false)
														.defaultValue(true))
						.formatter(EntityFormatter.builder()
										.value(Customer.LAST_NAME)
										.text(", ")
										.value(Customer.FIRST_NAME)
										.build())
						.caption("Customer")
						.build());

		// Use the Address.TYPE constant to define a new entity,
		// based on attributes defined using the Column and ForeignKey constants.
		// This entity definition is then added to the domain model.
		add(Address.TYPE.as(
										Address.ID.as()
														.primaryKey()
														.generator(identity()),
										Address.CUSTOMER_ID.as()
														.column()
														.nullable(false),
										Address.CUSTOMER_FK.as()
														.foreignKey()       // returns ForeignKeyDefinition.Builder
														.caption("Customer"),
										Address.STREET.as()
														.column()
														.caption("Street")
														.nullable(false)
														.maximumLength(100),
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
						.caption("Address")
						.build());
	}

	void addressExpanded() {
		Generator<Long> generator = Generator.identity();

		ColumnDefinition.Builder<Long, ?> id =
						Address.ID.as()
										.primaryKey()
										.generator(generator);

		ColumnDefinition.Builder<Long, ?> customerId =
						Address.CUSTOMER_ID.as()
										.column()
										.nullable(false);

		ForeignKeyDefinition.Builder customerFk =
						Address.CUSTOMER_FK.as()
										.foreignKey()
										.caption("Customer");

		ColumnDefinition.Builder<String, ?> street =
						Address.STREET.as()
										.column()
										.caption("Street")
										.nullable(false)
										.maximumLength(100);

		ColumnDefinition.Builder<String, ?> city =
						Address.CITY.as()
										.column()
										.caption("City")
										.nullable(false)
										.maximumLength(50);

		Function<Entity, String> formatter = EntityFormatter.builder()
						.value(Address.STREET)
						.text(", ")
						.value(Address.CITY)
						.build();

		EntityDefinition address =
						Address.TYPE.as(id, customerId, customerFk, street, city)
										.formatter(formatter)
										.caption("Address")
										.build();

		add(address);
	}
}
