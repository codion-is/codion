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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
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
 * {@snippet class = "is.codion.demos.chinook.javadoc.DerivedAttributeDefinitionSnippets" region = "usage" :
 * interface Employee { // @start region=usage
 *   EntityType TYPE = DOMAIN.entityType("chinook.employee");
 *
 *   Column<Long> ID = TYPE.longColumn("id");
 *   Column<String> FIRSTNAME = TYPE.stringColumn("firstname");
 *   Column<String> LASTNAME = TYPE.stringColumn("lastname");
 *   Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
 *
 *   // Derived attributes
 *   Attribute<String> NAME = TYPE.stringAttribute("name");
 *   Attribute<Integer> YEARS_EMPLOYED = TYPE.integerAttribute("years_employed");
 * }
 *
 * EntityDefinition employee() {
 *   return Employee.TYPE.as()
 *           .attributes(
 *                   Employee.ID.as()
 *                           .primaryKey(),
 *                   Employee.FIRSTNAME.as()
 *                           .column()
 *                           .caption("First name"),
 *                   Employee.LASTNAME.as()
 *                           .column()
 *                           .caption("Last name"),
 *                   Employee.HIREDATE.as()
 *                           .column()
 *                           .caption("Hire date"),
 *
 *                   // Simple derived attribute (cached by default)
 *                   Employee.NAME.as()
 *                           .derived()
 *                           .from(Employee.FIRSTNAME, Employee.LASTNAME)
 *                           .with(values -> values.optional(Employee.FIRSTNAME).orElse("") + " " +
 *                                   values.optional(Employee.LASTNAME).orElse(""))
 *                           .caption("Name"),
 *
 *                   // Time-dependent derived attribute (not cached)
 *                   Employee.YEARS_EMPLOYED.as()
 *                           .derived()
 *                           .from(Employee.HIREDATE)
 *                           .with(values -> values.optional(Employee.HIREDATE)
 *                                   .map(hireDate -> Period.between(hireDate, LocalDate.now()).getYears())
 *                                   .orElse(null))
 *                           .cached(false) // Changes over time
 *                           .caption("Years employed"))
 *           .build();
 * }
 *
 * void employees(Entities entities) {
 *   // Usage
 *   Entity employee = entities.entity(Employee.TYPE)
 *           .with(Employee.FIRSTNAME, "Jane")
 *           .with(Employee.LASTNAME, "Peacock")
 *           .with(Employee.HIREDATE, LocalDate.of(2002, 4, 1))
 *           .build();
 *
 *   // Derived values are computed automatically
 *   String name = employee.get(Employee.NAME);                     // "Jane Peacock" (cached)
 *   Integer yearsEmployed = employee.get(Employee.YEARS_EMPLOYED); // Computed on each access
 *
 *   // Modifying source attributes updates derived values
 *   employee.set(Employee.LASTNAME, "Park");
 *   String newName = employee.get(Employee.NAME);                  // "Jane Park"
 * } // @end}
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
	 * Note that cached attribute values are included when an entity is serialized.
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
		 * Note that cached attribute values are included when an entity is serialized.
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
