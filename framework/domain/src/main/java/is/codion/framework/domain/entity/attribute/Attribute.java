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

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Typed {@link Attribute}.
 * Note that attribute names are case-sensitive and Attributes are equal if their
 * names and entityTypes are equal, the valueClass does not factor into equality.
 * @param <T> the attribute type
 */
public interface Attribute<T> {

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
	 * Defines the data type of an Attribute
	 * @param <T> the attribute data type
	 */
	interface Type<T> {

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
		boolean isNumerical();

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
	interface AttributeDefiner<T> {

		/**
		 * Creates a new {@link TransientAttributeDefinition.Builder} instance, which does not map to an underlying table column.
		 * @param <B> the builder type
		 * @return a new {@link TransientAttributeDefinition.Builder}
		 */
		<B extends TransientAttributeDefinition.Builder<T, B>> TransientAttributeDefinition.Builder<T, B> attribute();

		/**
		 * Instantiates a {@link DerivedAttributeDefinition.Builder} instance, for displaying a value from a referenced entity attribute.
		 * @param <B> the builder type
		 * @param entityAttribute the entity attribute from which this attribute gets its value
		 * @param denormalizedAttribute the attribute from the referenced entity, from which this attribute gets its value
		 * @return a new {@link DerivedAttributeDefinition.Builder}
		 */
		<B extends DerivedAttributeDefinition.Builder<T, B>> DerivedAttributeDefinition.Builder<T, B> denormalized(Attribute<Entity> entityAttribute,
																																																							 Attribute<T> denormalizedAttribute);

		/**
		 * Instantiates a {@link DerivedAttributeDefinition.Builder} instance, which value is derived from one or more source attributes.
		 * @param valueProvider a {@link DerivedAttribute.Provider} instance responsible for deriving the value
		 * @param sourceAttributes the attributes from which this attribute derives its value
		 * @param <B> the builder type
		 * @return a new {@link DerivedAttributeDefinition.Builder}
		 * @throws IllegalArgumentException in case no source attributes are specified
		 */
		<B extends DerivedAttributeDefinition.Builder<T, B>> DerivedAttributeDefinition.Builder<T, B> derived(DerivedAttribute.Provider<T> valueProvider,
																																																					Attribute<?>... sourceAttributes);
	}
}
