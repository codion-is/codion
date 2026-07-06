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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A basic TypeReference implementation, capturing a generic type via an anonymous subclass:
 * {@snippet :
 * TypeReference<List<String>> reference = new TypeReference<>() {};
 *}
 * <p>The captured type {@code T} must itself be a parameterized type; for plain classes use the
 * available {@code Class<T>} based overloads instead. A non-parameterized {@code T}
 * (e.g. {@code new TypeReference<String>() {}}) throws {@link IllegalArgumentException}.
 * @param <T> the type represented by this type reference.
 */
public abstract class TypeReference<T> {

	private final Type type;

	/**
	 * Instantiates a new {@link TypeReference}
	 * @throws IllegalArgumentException in case the subclass is raw (no type argument) or {@code T} is not a parameterized type
	 */
	protected TypeReference() {
		Type superClass = getClass().getGenericSuperclass();
		if (!(superClass instanceof ParameterizedType)) {
			throw new IllegalArgumentException("TypeReference must be created with a type argument, e.g. new TypeReference<List<String>>() {}");
		}
		type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
		if (!(type instanceof ParameterizedType)) {
			throw new IllegalArgumentException("Not a parameterized type");
		}
	}

	/**
	 * @return the type
	 */
	public final Type type() {
		return type;
	}

	/**
	 * @return the generic class type
	 */
	public final Class<T> rawType() {
		return (Class<T>) ((ParameterizedType) type).getRawType();
	}

	@Override
	public final int hashCode() {
		return type.hashCode();
	}

	@Override
	public final boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof TypeReference)) {
			return false;
		}
		TypeReference<?> other = (TypeReference<?>) object;
		return type.equals(other.type);
	}

	@Override
	public final String toString() {
		return type.getTypeName();
	}
}