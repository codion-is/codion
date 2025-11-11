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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.utilities.TypeReference;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column.ColumnDefiner;
import is.codion.framework.domain.entity.attribute.DefaultAttribute.DefaultAttributeDefiner;
import is.codion.framework.domain.entity.attribute.DefaultAttribute.DefaultType;
import is.codion.framework.domain.entity.attribute.DerivedAttributeDefinition.DenormalizedBuilder;
import is.codion.framework.domain.entity.attribute.DerivedAttributeDefinition.DerivedBuilder;
import is.codion.framework.domain.entity.attribute.ForeignKey.ForeignKeyDefiner;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Typed {@link Attribute} representing a named, typed property of an entity.
 * <p>
 * Attributes are the building blocks of entity definitions, representing columns, foreign keys,
 * derived values, or transient properties. Each attribute has a name, type, and is associated
 * with a specific entity type.
 * <p>
 * Note that attribute names are case-sensitive and Attributes are equal if their
 * names and entityTypes are equal, the valueClass does not factor into equality.
 * <p>
 * Attributes are typically created through entity type factory methods and then configured
 * using the {@link #define()} method to create attribute definitions:
 * {@snippet :
 * public class Store extends DefaultDomain {
 *
 *     interface Customer {
 *         EntityType TYPE = DOMAIN.entityType("store.customer");
 *
 *         // Typed attribute creation
 *         Column<Integer> ID = TYPE.integerColumn("id");
 *         Column<String> NAME = TYPE.stringColumn("name");
 *         Column<String> EMAIL = TYPE.stringColumn("email");
 *         Column<LocalDate> BIRTH_DATE = TYPE.localDateColumn("birth_date");
 *         Column<Boolean> ACTIVE = TYPE.booleanColumn("active");
 *
 *         // Transient attribute (not mapped to database)
 *         Attribute<String> DISPLAY_NAME = TYPE.stringAttribute("display_name");
 *
 *         // Custom typed attribute
 *         Attribute<CustomerStatus> STATUS = TYPE.attribute("status", CustomerStatus.class);
 *     }
 *
 *     void defineCustomer() {
 *         Customer.TYPE.define(
 *                 // Column attributes
 *                 Customer.ID.define()
 *                     .primaryKey(),
 *                 Customer.NAME.define()
 *                     .column()
 *                     .nullable(false)
 *                     .maximumLength(100),
 *                 Customer.EMAIL.define()
 *                     .column()
 *                     .nullable(false),
 *                 Customer.BIRTH_DATE.define()
 *                     .column(),
 *                 Customer.ACTIVE.define()
 *                     .column()
 *                     .defaultValue(true),
 *
 *                 // Derived attribute
 *                 Customer.DISPLAY_NAME.define()
 *                     .derived()
 *				             .from(Customer.NAME, Customer.EMAIL)
 *                     .value(source ->
 *                         source.get(Customer.NAME) + " (" + source.get(Customer.EMAIL) + ")"),
 *
 *                 // Custom typed attribute
 *                 Customer.STATUS.define()
 *                     .column()
 *                     .columnClass(String.class, CustomerStatus::valueOf))
 *             .build();
 *     }
 * }
 *
 * // Usage with entities
 * Entity customer = entities.entity(Customer.TYPE)
 *     .with(Customer.NAME, "John Doe")
 *     .with(Customer.EMAIL, "john@example.com")
 *     .with(Customer.ACTIVE, true)
 *     .build();
 *
 * // Type-safe value access
 * String name = customer.get(Customer.NAME);           // String
 * Boolean active = customer.get(Customer.ACTIVE);     // Boolean
 * LocalDate birthDate = customer.get(Customer.BIRTH_DATE); // LocalDate
 *
 * // Attribute type information
 * Class<String> nameType = Customer.NAME.type().valueClass(); // String.class
 * boolean isNumerical = Customer.ID.type().isNumerical();     // true
 * boolean isTemporal = Customer.BIRTH_DATE.type().isTemporal(); // true
 *}
 * @param <T> the attribute type
 * @see #define()
 * @see #type()
 * @see EntityType
 */
public sealed interface Attribute<T> permits Column, DefaultAttribute, ForeignKey {

	/**
	 * @return a {@link AttributeDefiner} for this attribute
	 */
	AttributeDefiner<T> define();

	/**
	 * @return the attribute type
	 */
	Type<T> type();

	/**
	 * @return the name of this attribute.
	 */
	String name();

	/**
	 * @return the entity type this Attribute is associated with
	 */
	EntityType entityType();

	/**
	 * Creates a new {@link Attribute}, associated with the given entityType.
	 * @param entityType the entityType owning this attribute
	 * @param name the attribute name
	 * @param typeReference the {@link TypeReference} representing the attribute value type
	 * @param <T> the attribute type
	 * @return a new {@link Attribute}
	 */
	static <T> Attribute<T> attribute(EntityType entityType, String name, TypeReference<T> typeReference) {
		return attribute(entityType, name, requireNonNull(typeReference).rawType());
	}

	/**
	 * Creates a new {@link Attribute}, associated with the given entityType.
	 * @param entityType the entityType owning this attribute
	 * @param name the attribute name
	 * @param valueClass the class representing the attribute value type
	 * @param <T> the attribute type
	 * @return a new {@link Attribute}
	 */
	static <T> Attribute<T> attribute(EntityType entityType, String name, Class<T> valueClass) {
		return new DefaultAttribute<>(name, valueClass, entityType);
	}

	/**
	 * Defines the data-type of an Attribute
	 * @param <T> the attribute data type
	 */
	sealed interface Type<T> permits DefaultType {

		/**
		 * @return the Class representing the attribute value
		 */
		Class<T> valueClass();

		/**
		 * @param value the value to validate
		 * @return the validated value
		 * @throws IllegalArgumentException in case {@code value} is of a type incompatible with this attribute
		 * @see #valueClass()
		 */
		@Nullable T validateType(@Nullable T value);

		/**
		 * @return true if this attribute represents a numerical value.
		 */
		boolean isNumeric();

		/**
		 * @return true if this attribute represents a {@link java.time.temporal.Temporal} value.
		 */
		boolean isTemporal();

		/**
		 * @return true if this attribute represents a {@link java.time.LocalDate} value.
		 */
		boolean isLocalDate();

		/**
		 * @return true if this attribute represents a {@link java.time.LocalDateTime} value.
		 */
		boolean isLocalDateTime();

		/**
		 * @return true if this attribute represents a {@link java.time.LocalTime} value.
		 */
		boolean isLocalTime();

		/**
		 * @return true if this attribute represents a {@link java.time.OffsetDateTime} value.
		 */
		boolean isOffsetDateTime();

		/**
		 * @return true if this attribute represents a {@link Character} value.
		 */
		boolean isCharacter();

		/**
		 * @return true if this attribute represents a {@link String} value.
		 */
		boolean isString();

		/**
		 * @return true if this attribute represents a {@link Long} value.
		 */
		boolean isLong();

		/**
		 * @return true if this attribute represents a {@link Integer} value.
		 */
		boolean isInteger();

		/**
		 * @return true if this attribute represents a {@link Short} value.
		 */
		boolean isShort();

		/**
		 * @return true if this attribute represents a {@link Double} value.
		 */
		boolean isDouble();

		/**
		 * @return true if this attribute represents a {@link java.math.BigDecimal} value.
		 */
		boolean isBigDecimal();

		/**
		 * @return true if this attribute represents a decimal number value.
		 */
		boolean isDecimal();

		/**
		 * @return true if this attribute represents a {@link Boolean} value.
		 */
		boolean isBoolean();

		/**
		 * @return true if this attribute represents a byte array value.
		 */
		boolean isByteArray();

		/**
		 * @return true if this attribute represents an enum value.
		 */
		boolean isEnum();

		/**
		 * @return true if this attribute represents a {@link Entity} value.
		 */
		boolean isEntity();
	}

	/**
	 * Provides {@link AttributeDefinition.Builder} instances.
	 * @param <T> the column type
	 */
	sealed interface AttributeDefiner<T> permits ColumnDefiner, DefaultAttributeDefiner, ForeignKeyDefiner {

		/**
		 * Creates a new {@link TransientAttributeDefinition.Builder} instance, which does not map to an underlying table column.
		 * @param <B> the builder type
		 * @return a new {@link TransientAttributeDefinition.Builder}
		 */
		<B extends TransientAttributeDefinition.Builder<T, B>> TransientAttributeDefinition.Builder<T, B> attribute();

		/**
		 * Instantiates a {@link DenormalizedBuilder.SourceAttributeStep} instance, for displaying a value from a referenced entity attribute.
		 * @param <B> the builder type
		 * @return a new {@link DenormalizedBuilder.SourceAttributeStep}
		 */
		<B extends DenormalizedBuilder<T, B>> DenormalizedBuilder.SourceAttributeStep<T, B> denormalized();

		/**
		 * Instantiates a {@link DerivedBuilder.SourceAttributesStep} instance, for building an attribute which
		 * value is derived from zero or more source attributes.
		 * @param <B> the builder type
		 * @return a new {@link DerivedBuilder.SourceAttributesStep}
		 */
		<B extends DerivedBuilder<T, B>> DerivedBuilder.SourceAttributesStep<T, B> derived();
	}
}
