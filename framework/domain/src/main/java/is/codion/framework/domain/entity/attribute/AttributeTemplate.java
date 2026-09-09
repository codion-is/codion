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
package is.codion.framework.domain.entity.attribute;

/**
 * Specifies a reusable attribute configuration.
 * See {@link ColumnTemplate} for template usage examples.
 * @param <T> the attribute type
 * @see Attribute#as(AttributeTemplate)
 * @see ColumnTemplate
 */
@FunctionalInterface
public interface AttributeTemplate<T> {

	/**
	 * Applies this template to the given attribute
	 * @param attribute the attribute
	 * @return a {@link AttributeDefinition.Builder} for the given attribute
	 */
	AttributeDefinition.Builder<T, ?> apply(Attribute<T> attribute);
}
