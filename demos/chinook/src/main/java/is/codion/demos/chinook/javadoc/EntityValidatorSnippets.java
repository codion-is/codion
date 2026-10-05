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
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.exception.AttributeValidationException;
import is.codion.framework.domain.entity.exception.EntityValidationException;

import java.util.Optional;

/**
 * The {@link EntityValidator} javadoc snippets, each the region of the same name.
 */
final class EntityValidatorSnippets {

  void usage() {
    // Custom validator for Customer entity // @start region=usage
    class CustomerValidator implements EntityValidator {

      @Override
      public void validate(Entity customer, Attribute<?> attribute) throws AttributeValidationException {
        // Start with super.validate(), which performs null validation
        EntityValidator.super.validate(customer, attribute);
        // Validate email format
        if (attribute.equals(Customer.EMAIL)) {
          String email = customer.get(Customer.EMAIL);
          // Email is non-null, since super.validate() checks that
          if (!isValidEmail(email)) {
            throw new AttributeValidationException(Customer.EMAIL, email, "Invalid email format");
          }
        }
      }

      private static boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
      }
    }

    // Usage in domain definition
    Customer.TYPE.as()
            .attributes(
                    Customer.ID.as()
                            .primaryKey(),
                    Customer.EMAIL.as()
                            .column()
                            .caption("Email")
                            .nullable(false))
            .validator(new CustomerValidator())
            .build(); // @end
  }

  void nullable(Entities entities) {
    // Context-aware nullable validation // @start region=nullable
    class CustomerValidator implements EntityValidator {

      @Override
      public boolean nullable(Entity customer, Attribute<?> attribute) {
        // Normally nullable, but not for customers in the USA
        if (attribute.equals(Customer.STATE)) {
          return !"USA".equals(customer.get(Customer.COUNTRY));
        }

        // Use default nullable behavior for other attributes
        return EntityValidator.super.nullable(customer, attribute);
      }
    }

    // Usage during validation
    EntityValidator validator = new CustomerValidator();

    Entity customer = entities.entity(Customer.TYPE)
            .with(Customer.COUNTRY, "USA")
            .build(); // No state

    boolean nullable = validator.nullable(customer, Customer.STATE); // false // @end
  }

  void validate(EntityConnection connection, Entities entities) {
    // Validation during entity lifecycle // @start region=validate
    Entity customer = entities.entity(Customer.TYPE)
            .with(Customer.FIRSTNAME, "John")
            .with(Customer.LASTNAME, "Doe")
            .with(Customer.EMAIL, "invalid-email") // Invalid format
            .build();

    EntityValidator validator = entities.definition(Customer.TYPE).validator();

    try {
      validator.validate(customer);
      // Validation passed
      connection.insert(customer);
    }
    catch (EntityValidationException e) {
      // Handle validation error
      System.err.println("Validation failed " + e.getMessage());
    }

    // Check if entity is valid without throwing exception
    if (validator.valid(customer)) {
      connection.insert(customer);
    }
    else {
      // Handle invalid entity
    } // @end
  }

  void warning() {
    class TrackValidator implements EntityValidator { // @start region=warning

      private static final int HOUR_MS = 3_600_000;

      @Override
      public Optional<String> warning(Entity track, Attribute<?> attribute) {
        if (attribute.equals(Track.MILLISECONDS)) {
          Integer milliseconds = track.get(Track.MILLISECONDS);
          if (milliseconds != null && milliseconds > HOUR_MS) {
            return Optional.of("Unusually long for a track - is this a whole album?");
          }
        }

        return EntityValidator.super.warning(track, attribute);
      }
    } // @end
  }
}
