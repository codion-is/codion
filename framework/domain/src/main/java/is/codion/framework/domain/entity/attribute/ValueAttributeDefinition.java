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

import is.codion.common.utilities.item.Item;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.AbstractValueAttributeDefinition.AbstractValueAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static is.codion.common.utilities.Configuration.booleanValue;

/**
 * Defines an Attribute that holds concrete values (String, Integer, BigDecimal, etc.)
 * requiring type-specific validation, formatting, and constraints.
 * <p>
 * Contrast with {@link ForeignKeyDefinition} which holds Entity references and delegates
 * formatting to the referenced entity's stringFactory.
 * @param <T> the value type
 * @see AttributeDefinition
 * @see ColumnDefinition
 * @see DerivedAttributeDefinition
 * @see TransientAttributeDefinition
 */
public sealed interface ValueAttributeDefinition<T> extends AttributeDefinition<T>
				permits AbstractValueAttributeDefinition, ColumnDefinition, TransientAttributeDefinition {

	/**
	 * Specifies whether String values should be trimmed by default
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see String#trim()
	 */
	PropertyValue<Boolean> TRIM_STRINGS = booleanValue("codion.domain.trimStrings", true);

	/**
	 * @return true if null is a valid value for this attribute
	 */
	boolean nullable();

	/**
	 * @return the maximum allowed value for this attribute, an empty Optional if none is defined,
	 * only applicable to numerical attributes
	 */
	Optional<Number> maximum();

	/**
	 * @return the minimum allowed value for this attribute, an empty Optional if none is defined,
	 * only applicable to numerical attributes
	 */
	Optional<Number> minimum();

	/**
	 * @return the maximum length of this attribute value, -1 is returned if the maximum length is undefined,
	 * this only applies to String (varchar) based attributes
	 */
	int maximumLength();

	/**
	 * @return if string values should be trimmed, this applies to String (varchar) based attributes
	 */
	boolean trim();

	/**
	 * Validates the given value against the valid items for this attribute.
	 * Always returns true if this is not an item based attribute.
	 * @param value the value to validate
	 * @return true if the given value is a valid item for this attribute
	 * @see #items()
	 */
	boolean validItem(@Nullable T value);

	/**
	 * Returns the valid items for this attribute or an empty list in
	 * case this is not an item based attribute
	 * @return an unmodifiable view of the valid items for this attribute
	 */
	List<Item<T>> items();

	/**
	 * @return true if a default value has been set for this attribute
	 */
	boolean hasDefaultValue();

	/**
	 * @return the default value for this attribute, if no default value has been set null is returned
	 * @see #hasDefaultValue()
	 */
	@Nullable T defaultValue();

	/**
	 * Validates the value of this attribute as found in the given entity.
	 * <p>Note: When validating non-nullable attributes during entity insertion
	 * (when the entity does not exist), null values are allowed for:
	 * <ul>
	 * <li>Columns with default values - the database will provide the default value
	 * <li>Generated primary key columns - the database will generate the key value
	 * </ul>
	 * @param entity the entity containing the value to validate
	 * @param nullable true if null values are allowed in this validation context,
	 * false if null should trigger a {@link NullValidationException}
	 * @throws ValidationException in case of an invalid value
	 * @see EntityDefinition.Builder#validator(EntityValidator)
	 * @see Builder#validator(AttributeValidator)
	 * @see AttributeValidator
	 */
	void validate(Entity entity, boolean nullable);

	/**
	 * Builds a ValueAttributeDefinition instance
	 * @param <T> the value type
	 * @param <B> the builder type
	 */
	sealed interface Builder<T, B extends Builder<T, B>> extends AttributeDefinition.Builder<T, B>
					permits AbstractValueAttributeDefinitionBuilder, ColumnDefinition.Builder, TransientAttributeDefinition.Builder {

		/**
		 * Specifies whether this attribute is nullable. Note that this will not prevent
		 * the value from being set to null, only prevent successful validation of the entity.
		 * @param nullable specifies whether null is a valid value for this attribute
		 * @return this builder instance
		 */
		B nullable(boolean nullable);

		/**
		 * Only applicable to numerical attributes
		 * @param minimum the minimum allowed value for this attribute
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a numerical attribute
		 */
		B minimum(Number minimum);

		/**
		 * Only applicable to numerical attributes
		 * @param maximum the maximum allowed value for this attribute
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a numerical attribute
		 */
		B maximum(Number maximum);

		/**
		 * Only applicable to numerical attributes
		 * @param minimum the minimum allowed value for this attribute
		 * @param maximum the maximum allowed value for this attribute
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a numerical attribute
		 */
		B range(Number minimum, Number maximum);

		/**
		 * Sets the maximum length of this attribute value, this applies to String (varchar) based attributes
		 * @param maximumLength the maximum length
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a String attribute
		 */
		B maximumLength(int maximumLength);

		/**
		 * Define a validator for this attribute, this validator is called by
		 * the {@link EntityValidator} and is only called for non-null values.
		 * @param validator a {@link AttributeValidator} to use for this attribute
		 * @return this builder instance
		 */
		B validator(AttributeValidator<T> validator);

		/**
		 * Specifies whether string values should be trimmed, this applies to String (varchar) based attributes.
		 * @param trim true if strings values should be trimmed
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a String attribute
		 * @see String#trim()
		 * @see ValueAttributeDefinition#TRIM_STRINGS
		 */
		B trim(boolean trim);

		/**
		 * Note that by default items are sorted by to their caption, not their value.
		 * Use {@link #comparator(java.util.Comparator)} to set a custom comparator.
		 * @param items the {@link Item}s representing all the valid values for this attribute
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the valid item list contains duplicate values
		 */
		B items(List<Item<T>> items);

		/**
		 * Sets the default value for this attribute, overrides the underlying column default value, if any
		 * @param defaultValue the value to use as default
		 * @return this builder instance
		 */
		B defaultValue(T defaultValue);

		/**
		 * Sets the default value supplier, use in case of dynamic default values.
		 * @param supplier the default value supplier
		 * @return this builder instance
		 */
		B defaultValue(ValueSupplier<T> supplier);

		@Override
		ValueAttributeDefinition<T> build();
	}
}
