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

import java.io.Serializable;
import java.util.Optional;

/**
 * An attribute which value is derived from one or more attributes.
 * @param <T> the value type
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
		<T> T get(Attribute<T> attribute);

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
		T get(SourceValues values);
	}
}
