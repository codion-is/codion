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

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Optional;

/**
 * An attribute which value is derived from one or more source attributes.
 * <p>
 * Derived attributes compute their values from other attributes within the same entity
 * or from related entities. They provide calculated fields, formatting, concatenation,
 * and other derived values without being stored in the database.
 * <p>
 * Derived attributes are defined using value providers that receive source values
 * and compute the derived result:
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
 *
 *         // Derived attributes
 *         Attribute<String> FULL_NAME = TYPE.stringAttribute("full_name");
 *         Attribute<String> DISPLAY_NAME = TYPE.stringAttribute("display_name");
 *         Attribute<Integer> AGE = TYPE.integerAttribute("age");
 *         Attribute<String> INITIALS = TYPE.stringAttribute("initials");
 *     }
 *
 *     void defineCustomer() {
 *         Customer.TYPE.define(
 *                 Customer.FIRST_NAME.define()
 *                     .column(),
 *                 Customer.LAST_NAME.define()
 *                     .column(),
 *                 Customer.EMAIL.define()
 *                     .column(),
 *                 Customer.BIRTH_DATE.define()
 *                     .column(),
 *
 *                 // Simple concatenation
 *                 Customer.FULL_NAME.define()
 *                     .attribute()
 *                     .derived(Customer.FIRST_NAME, Customer.LAST_NAME)
 *                     .provider(values -> {
 *                         String first = values.get(Customer.FIRST_NAME);
 *                         String last = values.get(Customer.LAST_NAME);
 *                         return (first != null ? first : "") + " " + (last != null ? last : "");
 *                     }),
 *
 *                 // Complex formatting with multiple sources
 *                 Customer.DISPLAY_NAME.define()
 *                     .attribute()
 *                     .derived(Customer.FULL_NAME, Customer.EMAIL)
 *                     .provider(values -> {
 *                         String fullName = values.get(Customer.FULL_NAME);
 *                         String email = values.get(Customer.EMAIL);
 *                         return fullName + " (" + email + ")";
 *                     }),
 *
 *                 // Age calculation
 *                 Customer.AGE.define()
 *                     .attribute()
 *                     .derived(Customer.BIRTH_DATE)
 *                     .provider(values -> {
 *                         LocalDate birthDate = values.get(Customer.BIRTH_DATE);
 *                         return birthDate != null ?
 *                             Period.between(birthDate, LocalDate.now()).getYears() : null;
 *                     }),
 *
 *                 // Initials from names
 *                 Customer.INITIALS.define()
 *                     .attribute()
 *                     .derived(Customer.FIRST_NAME, Customer.LAST_NAME)
 *                     .provider(values -> {
 *                         String first = values.get(Customer.FIRST_NAME);
 *                         String last = values.get(Customer.LAST_NAME);
 *                         String firstInitial = first != null && !first.isEmpty() ?
 *                             first.substring(0, 1).toUpperCase() : "";
 *                         String lastInitial = last != null && !last.isEmpty() ?
 *                             last.substring(0, 1).toUpperCase() : "";
 *                         return firstInitial + lastInitial;
 *                     }))
 *             .build();
 *     }
 * }
 *
 * // Usage
 * Entity customer = entities.entity(Customer.TYPE)
 *     .with(Customer.FIRST_NAME, "John")
 *     .with(Customer.LAST_NAME, "Doe")
 *     .with(Customer.EMAIL, "john.doe@example.com")
 *     .with(Customer.BIRTH_DATE, LocalDate.of(1990, 5, 15))
 *     .build();
 *
 * // Derived values are computed automatically
 * String fullName = customer.get(Customer.FULL_NAME);       // "John Doe"
 * String displayName = customer.get(Customer.DISPLAY_NAME); // "John Doe (john.doe@example.com)"
 * Integer age = customer.get(Customer.AGE);                 // Calculated age
 * String initials = customer.get(Customer.INITIALS);       // "JD"
 *}
 * @param <T> the value type
 * @see Provider
 * @see SourceValues
 */
public interface DerivedAttribute<T> extends Attribute<T> {

	/**
	 * Provides the source values from which to derive the value.
	 */
	interface SourceValues {

		/**
		 * Returns the value associated with the given source attribute.
		 * @param attribute the source attribute which value to retrieve
		 * @param <T> the value type
		 * @return the value associated with the given source attribute
		 * @throws IllegalArgumentException in case the given attribute is not a source attribute
		 */
		@Nullable <T> T get(Attribute<T> attribute);

		/**
		 * Returns the source value associated with the given attribute or an empty {@link Optional}
		 * if the associated value is null or if the given attribute is not a source attribute
		 * @param attribute the attribute which value to retrieve
		 * @param <T> the value type
		 * @return the value associated with attribute
		 */
		<T> Optional<T> optional(Attribute<T> attribute);
	}

	/**
	 * Responsible for providing values derived from other values
	 * @param <T> the underlying type
	 */
	interface Provider<T> extends Serializable {

		/**
		 * @param values the source values, mapped to their respective attributes
		 * @return the derived value
		 */
		@Nullable T get(SourceValues values);
	}
}
