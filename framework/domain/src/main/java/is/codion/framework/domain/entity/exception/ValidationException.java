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

import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 * Specifies that one or more attribute values are invalid.
 */
public final class ValidationException extends RuntimeException {

	private final Collection<InvalidAttribute> invalidAttributes;

	/**
	 * @param attribute the attribute
	 * @param value the invalid value
	 * @param message the message
	 */
	public ValidationException(Attribute<?> attribute, @Nullable Object value, String message) {
		this(singleton(InvalidAttribute.invalidAttribute(attribute, value, message)));
	}

	/**
	 * @param invalidAttributes the invalid attributes
	 * @throws IllegalArgumentException in case the invalid attributes are not all from the same entity
	 */
	public ValidationException(Collection<InvalidAttribute> invalidAttributes) {
		super(createMessage(invalidAttributes));
		Set<EntityType> entityTypes = invalidAttributes.stream()
						.map(invalidAttribute -> invalidAttribute.attribute().entityType())
						.collect(toSet());
		if (entityTypes.size() > 1) {
			throw new IllegalArgumentException("All invalid attributes must be from the same entity: " + entityTypes);
		}
		this.invalidAttributes = unmodifiableList(new ArrayList<>(requireNonNull(invalidAttributes)));
	}

	/**
	 * @return the invalid attributes
	 */
	public Collection<InvalidAttribute> invalidAttributes() {
		return invalidAttributes;
	}

	private static String createMessage(Collection<InvalidAttribute> invalidAttributes) {
		if (requireNonNull(invalidAttributes).isEmpty()) {
			throw new IllegalArgumentException("One or more invalid attributes must be specified");
		}

		return invalidAttributes.stream()
						.map(InvalidAttribute::message)
						.collect(joining("\n"));
	}

	/**
	 * Specifies an invalid attribute value
	 */
	public interface InvalidAttribute {

		/**
		 * @return the attribute
		 */
		Attribute<?> attribute();

		/**
		 * @return the invalid value
		 */
		@Nullable Object value();

		/**
		 * @return the message
		 */
		String message();

		/**
		 * @param attribute the attribute
		 * @param value the invalid value
		 * @param message the message
		 * @return a new {@link InvalidAttribute} instance
		 */
		static InvalidAttribute invalidAttribute(Attribute<?> attribute, @Nullable Object value, String message) {
			return new DefaultInvalidAttribute(attribute, value, message);
		}
	}

	private static final class DefaultInvalidAttribute implements InvalidAttribute {

		private final Attribute<?> attribute;
		private final @Nullable Object value;
		private final String message;

		private DefaultInvalidAttribute(Attribute<?> attribute, @Nullable Object value, String message) {
			this.attribute = requireNonNull(attribute);
			this.value = value;
			this.message = requireNonNull(message);
		}

		@Override
		public Attribute<?> attribute() {
			return attribute;
		}

		@Override
		public @Nullable Object value() {
			return value;
		}

		@Override
		public String message() {
			return message;
		}
	}
}
