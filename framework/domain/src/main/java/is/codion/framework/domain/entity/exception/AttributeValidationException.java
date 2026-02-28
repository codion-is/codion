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
package is.codion.framework.domain.entity.exception;

import is.codion.framework.domain.entity.attribute.Attribute;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Indicates that an attribute value is invalid.
 */
public final class AttributeValidationException extends Exception {

	private final Attribute<?> attribute;
	private final @Nullable Object value;

	/**
	 * @param attribute the attribute
	 * @param value the value
	 * @param message the message
	 */
	public AttributeValidationException(Attribute<?> attribute, @Nullable Object value, String message) {
		super(requireNonNull(message));
		this.attribute = requireNonNull(attribute);
		this.value = value;
	}

	/**
	 * @return the attribute
	 */
	public Attribute<?> attribute() {
		return attribute;
	}

	/**
	 * @return the value
	 */
	public @Nullable Object value() {
		return value;
	}
}
