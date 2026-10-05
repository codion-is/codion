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
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Entity.Key;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static is.codion.demos.chinook.domain.api.Chinook.*;

/**
 * The {@link Entity} javadoc snippets, each the region of the same name.
 */
final class EntitySnippets {

  void usage(Entities entities) {
    // Creating and working with entities // @start region=usage
    Entity customer = entities.entity(Customer.TYPE)
            .with(Customer.FIRSTNAME, "John")
            .with(Customer.LASTNAME, "Doe")
            .with(Customer.EMAIL, "john@example.com")
            .build();

    // Accessing values
    String lastName = customer.get(Customer.LASTNAME);
    Optional<String> email = customer.optional(Customer.EMAIL);

    // Modifying values (if mutable)
    customer.set(Customer.EMAIL, "newemail@example.com");
    boolean modified = customer.modified(Customer.EMAIL); // true

    // Reverting changes
    customer.revert(Customer.EMAIL); // back to "john@example.com" // @end
  }

  void foreignKeys(EntityConnection connection) {
    // Working with foreign keys // @start region=foreignKeys
    Entity invoice = connection.selectSingle(Invoice.ID.equalTo(123L));

    // Access the referenced entity (automatically loaded if configured)
    Entity customer = invoice.get(Invoice.CUSTOMER_FK);

    // Access foreign key attributes directly
    String lastName = invoice.get(Invoice.CUSTOMER_FK).get(Customer.LASTNAME);

    // Get the foreign key value
    Key customerKey = invoice.key(Invoice.CUSTOMER_FK); // @end
  }

  void entity(EntityConnection connection) {
    Entity invoice = connection.selectSingle(Invoice.ID.equalTo(42L)); // @start region=entity

    // Get the customer entity - may be fully loaded or just contain the key
    Entity customer = invoice.entity(Invoice.CUSTOMER_FK);

    if (customer != null) {
      // This is always available - the foreign key value
      Long customerId = customer.get(Customer.ID);

      // This may return null if customer wasn't loaded
      // and the foreign key entity doesn't contain Customer.LASTNAME
      String lastName = customer.get(Customer.LASTNAME);
    } // @end
  }

  void modified(Entities entities) {
    Entity customer = entities.entity(Customer.TYPE) // @start region=modified
            .with(Customer.FIRSTNAME, "John")
            .with(Customer.EMAIL, "john@example.com")
            .build();

    customer.modified(Customer.FIRSTNAME); // false

    customer.set(Customer.FIRSTNAME, "Jane");
    customer.modified(Customer.FIRSTNAME); // true

    customer.save();
    customer.modified(Customer.FIRSTNAME); // false // @end
  }

  void valuesEqual(Entities entities) {
    Entity customer1 = entities.entity(Customer.TYPE) // @start region=valuesEqual
            .with(Customer.ID, 42L)
            .with(Customer.LASTNAME, "Doe")
            .build();

    Entity customer2 = entities.entity(Customer.TYPE)
            .with(Customer.ID, 42L)
            .with(Customer.LASTNAME, "Doe")
            .with(Customer.EMAIL, "john@example.com") // present in customer2 only
            .build();

    customer1.equals(customer2);      // true, the primary keys are equal
    customer1.valuesEqual(customer2); // false, EMAIL is present in customer2 only
    customer2.valuesEqual(customer1); // false, symmetric // @end
  }

  void valuesEqualAttributes(Entity customer1, Entity customer2) {
    customer1.valuesEqual(customer2, List.of(Customer.ID, Customer.LASTNAME)); // true, ID and LASTNAME are equal // @start region=valuesEqualAttributes
    customer1.valuesEqual(customer2, List.of(Customer.ID, Customer.EMAIL));    // false, EMAIL is present in customer2 only // @end
  }

  void copy(Entities entities) {
    Entity customer = entities.entity(Customer.TYPE) // @start region=copy
            .with(Customer.ID, 42L)
            .with(Customer.LASTNAME, "Doe")
            .with(Customer.EMAIL, "john@example.com")
            .build();

    // Create a mutable copy
    Entity mutableCopy = customer.copy().mutable();
    mutableCopy.set(Customer.EMAIL, "new@example.com");

    // Original remains unchanged
    customer.get(Customer.EMAIL); // "john@example.com"
    mutableCopy.get(Customer.EMAIL); // "new@example.com"

    // Create a builder initialized with entity values
    Entity newCustomer = customer.copy().builder()
            .with(Customer.ID, 43L) // Different ID
            .with(Customer.PHONE, "555-1234") // Additional field
            .build(); // @end
  }

  void builder() {
    Domain domain = new ChinookImpl(); // @start region=builder

    Entities entities = domain.entities();

    Entity customer = entities.entity(Customer.TYPE)
            .with(Customer.FIRSTNAME, "John")
            .with(Customer.LASTNAME, "Doe")
            .build(); // @end
  }

  void groupByValue(EntityConnection connection, Entity album) {
    List<Entity> tracks = connection.select(Track.ALBUM_FK.equalTo(album)); // @start region=groupByValue

    // Group the tracks by composer
    LinkedHashMap<String, List<Entity>> tracksByComposer =
            Entity.groupByValue(Track.COMPOSER, tracks);

    // Process the tracks by composer
    tracksByComposer.forEach((composer, composerTracks) ->
            System.out.println("Composer: " + composer + ", tracks: " + composerTracks.size()));

    // Tracks without a composer are grouped under null
    List<Entity> noComposerTracks = tracksByComposer.get(null); // @end
  }
}
