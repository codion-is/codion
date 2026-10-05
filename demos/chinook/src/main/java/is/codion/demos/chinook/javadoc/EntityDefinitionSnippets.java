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

import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityFormatter;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;

import static is.codion.demos.chinook.domain.api.Chinook.DOMAIN;
import static is.codion.framework.domain.entity.OrderBy.ascending;

/**
 * The {@link EntityDefinition} javadoc snippets, each the region of the same name.
 * <p>{@link Genre} mirrors a part of the Chinook domain API, for the usage region to declare.
 */
final class EntityDefinitionSnippets {

  // Define entity types // @start region=usage
  interface Genre {
    EntityType TYPE = DOMAIN.entityType("chinook.genre");

    Column<Long> ID = TYPE.longColumn("id");
    Column<String> NAME = TYPE.stringColumn("name");
  }

  // Define the entity structure
  EntityDefinition genre() {
    return Genre.TYPE.as()
            .attributes(
                    Genre.ID.as()
                            .primaryKey(),
                    Genre.NAME.as()
                            .column()
                            .caption("Name")
                            .nullable(false)
                            .maximumLength(120))
            .caption("Genres")
            .orderBy(ascending(Genre.NAME))
            .formatter(Genre.NAME)
            .smallDataset(true)
            .build();
  } // @end

  void formatter(Entities entities) {
    // Define custom string representation // @start region=formatter
    Customer.TYPE.as()
            .attributes(
                    Customer.ID.as()
                            .primaryKey(),
                    Customer.FIRSTNAME.as()
                            .column()
                            .caption("First name"),
                    Customer.LASTNAME.as()
                            .column()
                            .caption("Last name"),
                    Customer.EMAIL.as()
                            .column()
                            .caption("Email"))
            .formatter(customer ->
                    customer.get(Customer.LASTNAME) + ", " +
                            customer.get(Customer.FIRSTNAME) +
                            " (" + customer.get(Customer.EMAIL) + ")")
            .build();

    // Usage
    Entity customer = entities.entity(Customer.TYPE)
            .with(Customer.FIRSTNAME, "John")
            .with(Customer.LASTNAME, "Doe")
            .with(Customer.EMAIL, "john@example.com")
            .build();

    System.out.println(customer); // "Doe, John (john@example.com)" // @end
  }

  void attributes() {
    EntityDefinition definition = Customer.TYPE.as() // @start region=attributes
            .attributes(
                    Customer.ID.as()
                            .primaryKey(),
                    Customer.LASTNAME.as()
                            .column()
                            .caption("Last name")
                            .nullable(false)
                            .maximumLength(20),
                    Customer.FIRSTNAME.as()
                            .column()
                            .caption("First name")
                            .nullable(false)
                            .maximumLength(40),
                    Customer.EMAIL.as()
                            .column()
                            .caption("Email")
                            .nullable(false)
                            .maximumLength(60),
                    Customer.COMPANY.as()
                            .column()
                            .caption("Company")
                            .maximumLength(80))
            .caption("Customer")
            .description("Customer information")
            .orderBy(ascending(Customer.LASTNAME, Customer.FIRSTNAME))
            .formatter(customer ->
                    customer.get(Customer.LASTNAME) + " (" + customer.get(Customer.EMAIL) + ")")
            .build(); // @end
  }

  void formatterAttribute(EntityDefinition.Builder builder, Attribute<?> attribute) {
    builder.formatter(EntityFormatter.builder() // @start region=formatterAttribute
            .value(attribute)
            .build()); // @end
  }
}
