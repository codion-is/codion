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
import is.codion.framework.domain.entity.attribute.AbstractValueAttributeDefinition.AbstractValueAttributeDefinitionBuilder;

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
	 * Builds a ValueAttributeDefinition instance
	 * @param <T> the value type
	 * @param <B> the builder type
	 */
	sealed interface Builder<T, B extends Builder<T, B>> extends AttributeDefinition.Builder<T, B>
					permits AbstractValueAttributeDefinitionBuilder, ColumnDefinition.Builder, TransientAttributeDefinition.Builder {

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

		@Override
		ValueAttributeDefinition<T> build();
	}
}
