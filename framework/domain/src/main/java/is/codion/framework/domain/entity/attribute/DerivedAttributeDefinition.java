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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.DefaultDerivedAttributeDefinition.DefaultDenormalizedAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultDerivedAttributeDefinition.DefaultDerivedAttributeDefinitionBuilder;

import java.util.List;

/**
 * A definition for attributes which value is derived from the values of one or more attributes.
 * <p>
 * DerivedAttributeDefinition extends {@link ValueAttributeDefinition} and configures attributes
 * that compute their values from other attributes within the same entity or from related entities.
 * These attributes provide calculated fields, formatting, aggregation, and other computed values.
 * <p>
 * Derived attributes can be cached for performance or computed on-demand:
 * {@snippet :
 * public class Store extends DefaultDomain {
 *
 *     interface Customer {
 *         EntityType TYPE = DOMAIN.entityType("store.customer");
 *
 *         Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
 *         Column<String> LAST_NAME = TYPE.stringColumn("last_name");
 *         Column<String> EMAIL = TYPE.stringColumn("email");
 *         Column<LocalDate> BIRTH_DATE = TYPE.localDateColumn("birth_date");
 *         Column<String> PHONE = TYPE.stringColumn("phone");
 *
 *         // Derived attributes
 *         Attribute<String> FULL_NAME = TYPE.stringAttribute("full_name");
 *         Attribute<String> CONTACT_INFO = TYPE.stringAttribute("contact_info");
 *         Attribute<Integer> AGE = TYPE.integerAttribute("age");
 *         Attribute<String> NAME_UPPER = TYPE.stringAttribute("name_upper");
 *     }
 *
 *     void defineCustomer() {
 *         Customer.TYPE.as(
 *                 Customer.FIRST_NAME.as()
 *                     .column(),
 *                 Customer.LAST_NAME.as()
 *                     .column(),
 *                 Customer.EMAIL.as()
 *                     .column(),
 *                 Customer.BIRTH_DATE.as()
 *                     .column(),
 *                 Customer.PHONE.as()
 *                     .column(),
 *
 *                 // Simple derived attribute (cached by default)
 *                 Customer.FULL_NAME.as()
 *                     .derived()
 *                     .from(Customer.FIRST_NAME, Customer.LAST_NAME)
 *                     .with(values -> {
 *                         String first = values.get(Customer.FIRST_NAME);
 *                         String last = values.get(Customer.LAST_NAME);
 *                         if (first == null && last == null) {
 *                             return null;
 *                         }
 *                         return ((first != null ? first : "") + " " +
 *                                 (last != null ? last : "")).trim();
 *                     })
 *                     .caption("Full Name"),
 *
 *                 // Multi-source derived attribute with caching disabled
 *                 Customer.CONTACT_INFO.as()
 *                     .derived()
 *                     .from(Customer.FULL_NAME, Customer.EMAIL, Customer.PHONE)
 *                     .with(values -> {
 *                         String name = values.get(Customer.FULL_NAME);
 *                         String email = values.get(Customer.EMAIL);
 *                         String phone = values.get(Customer.PHONE);
 *
 *                         StringBuilder contact = new StringBuilder();
 *                         if (name != null) contact.append(name);
 *                         if (email != null) {
 *                             if (contact.length() > 0) contact.append(" - ");
 *                             contact.append(email);
 *                         }
 *                         if (phone != null) {
 *                             if (contact.length() > 0) contact.append(" - ");
 *                             contact.append(phone);
 *                         }
 *                         return contact.toString();
 *                     })
 *                     .cached(false) // Compute on each access
 *                     .caption("Contact Information"),
 *
 *                 // Time-dependent derived attribute (not cached)
 *                 Customer.AGE.as()
 *                     .derived()
 * 			               .from(Customer.BIRTH_DATE)
 *                     .with(values -> {
 *                         LocalDate birthDate = values.get(Customer.BIRTH_DATE);
 *                         return birthDate != null ?
 *                             Period.between(birthDate, LocalDate.now()).getYears() : null;
 *                     })
 *                     .cached(false) // Age changes over time
 *                     .caption("Age"),
 *
 *                 // Formatting derived attribute
 *                 Customer.NAME_UPPER.as()
 *                     .derived()
 * 		                 .from(Customer.FULL_NAME)
 *                     .with(values -> {
 *                         String fullName = values.get(Customer.FULL_NAME);
 *                         return fullName != null ? fullName.toUpperCase() : null;
 *                     })
 *                     .caption("Name (Uppercase)"))
 *             .build();
 *     }
 * }
 *
 * // Usage examples
 * Entity customer = entities.entity(Customer.TYPE)
 *     .with(Customer.FIRST_NAME, "John")
 *     .with(Customer.LAST_NAME, "Doe")
 *     .with(Customer.EMAIL, "john.doe@example.com")
 *     .with(Customer.BIRTH_DATE, LocalDate.of(1990, 5, 15))
 *     .build();
 *
 * // Derived values are computed automatically
 * String fullName = customer.get(Customer.FULL_NAME);         // "John Doe" (cached)
 * String contactInfo = customer.get(Customer.CONTACT_INFO);   // Computed each time
 * Integer age = customer.get(Customer.AGE);                   // Current age
 * String nameUpper = customer.get(Customer.NAME_UPPER);       // "JOHN DOE"
 *
 * // Modifying source attributes updates derived values
 * customer.set(Customer.FIRST_NAME, "Jane");
 * String newFullName = customer.get(Customer.FULL_NAME);      // "Jane Doe"
 *}
 * @param <T> the underlying type
 * @see DerivedValue
 * @see #attributes()
 * @see #cached()
 */
public sealed interface DerivedAttributeDefinition<T> extends AttributeDefinition<T> permits DefaultDerivedAttributeDefinition {

	/**
	 * @return the source attributes this attribute derives from.
	 */
	List<Attribute<?>> attributes();

	/**
	 * @return the derived value
	 */
	DerivedValue<T> value();

	/**
	 * Note that cached attributes are included when an entity is serialized.
	 * @return true if the value of this derived attribute is cached, false if computed on each access
	 */
	boolean cached();

	/**
	 * Builds a derived AttributeDefinition instance
	 * @param <T> the attribute value type
	 */
	sealed interface DerivedBuilder<T, B extends DerivedBuilder<T, B>>
					extends AttributeDefinition.Builder<T, B>
					permits DefaultDerivedAttributeDefinitionBuilder {

		/**
		 * Default true unless no source attributes are specified or this is a denormalized attribute.
		 * Note that cached attributes are included when an entity is serialized.
		 * @param cached true if the value of this derived attribute should be cached, false if it should be computed on each access
		 * @return this builder instance
		 * @throws IllegalArgumentException in case this is a denormalized attribute
		 */
		DerivedBuilder<T, B> cached(boolean cached);

		/**
		 * The first step in building a {@link DerivedAttributeDefinition}
		 * @param <T> the attribute value type
		 * @param <B> the builder type
		 */
		interface DerivedFromStep<T, B extends DerivedBuilder<T, B>> {

			/**
			 * @param attributes the attributes to derive the value from
			 * @return a {@link DerivedWithStep} instance
			 */
			DerivedWithStep<T, B> from(Attribute<?>... attributes);
		}

		/**
		 * The second step in building a {@link DerivedAttributeDefinition}
		 * @param <T> the attribute value type
		 * @param <B> the builder type
		 */
		interface DerivedWithStep<T, B extends DerivedBuilder<T, B>> {

			/**
			 * @param value a {@link DerivedValue} instance responsible for providing the derived value
			 * @return a {@link DerivedBuilder} instance
			 */
			DerivedBuilder<T, B> with(DerivedValue<T> value);
		}
	}

	/**
	 * Builds a derived AttributeDefinition instance
	 * @param <T> the attribute value type
	 */
	sealed interface DenormalizedBuilder<T, B extends DenormalizedBuilder<T, B>>
					extends AttributeDefinition.Builder<T, B>
					permits DefaultDenormalizedAttributeDefinitionBuilder {

		/**
		 * The first step in building a denormalized attribute
		 * @param <T> the attribute value type
		 * @param <B> the builder type
		 */
		interface DenormalizedFromStep<T, B extends DenormalizedBuilder<T, B>> {

			/**
			 * @param source the source attribute to denormalize from
			 * @return a {@link DenormalizedUsingStep} instance
			 */
			DenormalizedUsingStep<T, B> from(Attribute<Entity> source);
		}

		/**
		 * The second step in building a denormalized attribute
		 * @param <T> the attribute value type
		 * @param <B> the builder type
		 */
		interface DenormalizedUsingStep<T, B extends DenormalizedBuilder<T, B>> {

			/**
			 * @param denormalized the denormalized attribute
			 * @return a {@link DenormalizedBuilder} instance
			 */
			DenormalizedBuilder<T, B> using(Attribute<T> denormalized);
		}
	}
}
