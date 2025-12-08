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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityValidator;

import java.io.Serializable;

/**
 * Validates the value for an {@link Attribute}.
 * <p>Note that this validator is only called for non-null values, use {@link EntityValidator} for null validation.
 * @param <T> the value type
 * @see ValueAttributeDefinition.Builder#validator(AttributeValidator)
 * @see EntityDefinition.Builder#validator(EntityValidator)
 */
public interface AttributeValidator<T> extends Serializable {

	/**
	 * Validates the given value. This method is only called for non-null values.
	 * @param value the value to validate
	 * @throws IllegalArgumentException in case the value is invalid
	 */
	void validate(T value);
}
