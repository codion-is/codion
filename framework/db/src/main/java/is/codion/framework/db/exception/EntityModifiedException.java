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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.exception;

import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

/**
 * An exception indicating that an entity, being updated, has been modified or deleted since it was loaded.
 */
public final class EntityModifiedException extends UpdateEntityException {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityModifiedException.class, getBundle(EntityModifiedException.class.getName()));

	private final Entity entity;
	private final @Nullable Entity modified;
	private final Collection<Column<?>> columns;

	/**
	 * Instantiates a new EntityModifiedException with a default message, describing the modification
	 * @param entity the entity being updated
	 * @param modified the current (modified) version of the entity, null if it has been deleted
	 * @param columns the modified columns, an empty collection in case the entity has been deleted
	 */
	public EntityModifiedException(Entity entity, @Nullable Entity modified, Collection<Column<?>> columns) {
		this(entity, modified, columns, message(requireNonNull(entity), modified, requireNonNull(columns)));
	}

	/**
	 * Instantiates a new EntityModifiedException
	 * @param entity the entity being updated
	 * @param modified the current (modified) version of the entity, null if it has been deleted
	 * @param columns the modified columns, an empty collection in case the entity has been deleted
	 * @param message a message describing the modification
	 */
	public EntityModifiedException(Entity entity, @Nullable Entity modified, Collection<Column<?>> columns, String message) {
		super(message);
		this.entity = requireNonNull(entity);
		this.modified = modified;
		this.columns = unmodifiableSet(new HashSet<>(requireNonNull(columns)));
	}

	/**
	 * @return the entity being updated
	 */
	public Entity entity() {
		return entity;
	}

	/**
	 * @return the current (modified) version of the entity, an empty Optional if it has been deleted
	 */
	public Optional<Entity> modified() {
		return Optional.ofNullable(modified);
	}

	/**
	 * @return the modified columns, or an empty collection in case the entity has been deleted
	 */
	public Collection<Column<?>> columns() {
		return columns;
	}

	private static String message(Entity entity, @Nullable Entity modified, Collection<Column<?>> columns) {
		if (modified == null) {
			Entity original = entity.copy().mutable();
			original.revert();

			return MESSAGES.getString("record_modified") + ", " + original + " " + MESSAGES.getString("has_been_deleted");
		}
		StringBuilder builder = new StringBuilder(MESSAGES.getString("record_modified")).append(": ").append(entity.type());
		for (Column<?> column : columns) {
			builder.append("\n").append(column)
							.append(": ").append(entity.original(column))
							.append(" -> ").append(modified.get(column));
		}

		return builder.toString();
	}
}
