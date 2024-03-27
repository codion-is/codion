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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.exception;

import is.codion.framework.domain.entity.attribute.Attribute;

/**
 * An exception used to indicate that a value associated with a key exceeds the allowed length.
 */
public class LengthValidationException extends ValidationException {

	/**
	 * Instantiates a new LengthValidationException
	 * @param attribute the attribute
	 * @param value the value that exceeds the allowed length
	 * @param message the message
	 */
	public LengthValidationException(Attribute<?> attribute, Object value, String message) {
		super(attribute, value, message);
	}
}
