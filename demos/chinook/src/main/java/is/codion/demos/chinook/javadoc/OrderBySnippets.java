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

import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;

import java.util.List;

/**
 * The {@link OrderBy} javadoc snippets, each the region of the same name.
 */
final class OrderBySnippets {

  void usage(EntityConnection connection) {
    // Default ordering for the entity // @start region=usage
    Customer.TYPE.as()
            .attributes(
                    Customer.LASTNAME.as()
                            .column()
                            .caption("Last name"),
                    Customer.FIRSTNAME.as()
                            .column()
                            .caption("First name"))
            .orderBy(OrderBy.builder()
                    .ascending(Customer.LASTNAME, Customer.FIRSTNAME)
                    .build())
            .build();

    // Query usage examples
    // Simple ascending sort
    List<Entity> customers = connection.select(
            Select.all(Customer.TYPE)
                    .orderBy(OrderBy.ascending(Customer.LASTNAME)));

    // Multiple columns, mixed directions
    List<Entity> tracksByRatingAndName = connection.select(
            Select.all(Track.TYPE)
                    .orderBy(OrderBy.builder()
                            .descending(Track.RATING) // Highest rated first
                            .ascendingIgnoreCase(Track.NAME) // Case-insensitive names
                            .build()));

    // With null handling
    List<Entity> tracksByComposer = connection.select(
            Select.all(Track.TYPE)
                    .orderBy(OrderBy.builder()
                            .ascending(OrderBy.NullOrder.NULLS_LAST, Track.COMPOSER)
                            .build())); // @end
  }

  void builder(EntityConnection connection, boolean byRating, boolean byPlayCount) {
    // Complex ordering with multiple columns and options // @start region=builder
    OrderBy order = OrderBy.builder()
            .descending(Track.RATING) // Highest rated first
            .ascending(Track.ARTIST_NAME) // Then by artist
            .descending(OrderBy.NullOrder.NULLS_LAST, Track.COMPOSER) // Then by composer (nulls last)
            .ascendingIgnoreCase(Track.NAME) // Finally by name (case-insensitive)
            .build();

    // Use in query
    List<Entity> tracks = connection.select(
            Select.all(Track.TYPE)
                    .orderBy(order));

    // Builder pattern allows conditional ordering
    OrderBy.Builder builder = OrderBy.builder();
    if (byRating) {
      builder.descending(Track.RATING);
    }
    builder.ascending(Track.NAME);
    if (byPlayCount) {
      builder.descending(Track.PLAY_COUNT);
    }
    OrderBy dynamicOrder = builder.build(); // @end
  }

  void ascending(EntityConnection connection) {
    // Single column ascending // @start region=ascending
    OrderBy byName = OrderBy.ascending(Artist.NAME);

    // Multiple columns ascending
    OrderBy byLastAndFirstName = OrderBy.ascending(Customer.LASTNAME, Customer.FIRSTNAME);

    // Usage in queries
    List<Entity> customers = connection.select(
            Select.all(Customer.TYPE)
                    .orderBy(OrderBy.ascending(Customer.LASTNAME)));

    // Usage in entity definition as default ordering
    Customer.TYPE.as()
            .attributes(
                    Customer.LASTNAME.as()
                            .column()
                            .caption("Last name"),
                    Customer.FIRSTNAME.as()
                            .column()
                            .caption("First name"))
            .orderBy(OrderBy.ascending(Customer.LASTNAME, Customer.FIRSTNAME))
            .build(); // @end
  }

  void descending(EntityConnection connection, Entity customer) {
    // Single column descending // @start region=descending
    OrderBy byDateDescending = OrderBy.descending(Invoice.DATE);

    // Multiple columns descending
    OrderBy byRatingAndPlayCount = OrderBy.descending(Track.RATING, Track.PLAY_COUNT);

    // Usage - most recent invoices first
    List<Entity> recentInvoices = connection.select(
            Select.all(Invoice.TYPE)
                    .orderBy(OrderBy.descending(Invoice.DATE)));

    // Combine with conditions
    List<Entity> recentCustomerInvoices = connection.select(
            Select.where(Invoice.CUSTOMER_FK.equalTo(customer))
                    .orderBy(OrderBy.descending(Invoice.DATE))); // @end
  }
}
