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

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 * Indicates an invalid entity, either due to one or more invalid attribute exceptions,
 * accessible via {@link #exceptions()} or a general validation failure.
 */
public final class EntityValidationException extends Exception {

	@Serial
	private static final long serialVersionUID = 1;

	private final Collection<AttributeValidationException> exceptions;

	/**
	 * @param message the message
	 */
	public EntityValidationException(String message) {
		super(requireNonNull(message));
		this.exceptions = emptyList();
	}

	/**
	 * @param exceptions the invalid attribute exceptions
	 * @throws IllegalArgumentException in case {@code exceptions} is empty, or the attribute exceptions are not all from the same entity
	 */
	public EntityValidationException(Collection<AttributeValidationException> exceptions) {
		super(createMessage(exceptions));
		Set<EntityType> entityTypes = exceptions.stream()
						.map(exception -> exception.attribute().entityType())
						.collect(toSet());
		if (entityTypes.size() > 1) {
			throw new IllegalArgumentException("All invalid attributes must be from the same entity: " + entityTypes);
		}
		this.exceptions = unmodifiableList(new ArrayList<>(requireNonNull(exceptions)));
	}

	/**
	 * @return the attribute validation exceptions, may be empty in case of a general validation exception
	 */
	public Collection<AttributeValidationException> exceptions() {
		return exceptions;
	}

	private static String createMessage(Collection<AttributeValidationException> exceptions) {
		if (requireNonNull(exceptions).isEmpty()) {
			throw new IllegalArgumentException("One or more attribute validation exceptions must be specified");
		}

		return exceptions.stream()
						.map(Exception::getMessage)
						.collect(joining("\n"));
	}
}
