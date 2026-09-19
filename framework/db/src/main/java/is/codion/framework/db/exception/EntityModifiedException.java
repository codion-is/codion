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

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

/**
 * An exception indicating that an entity, being updated, has been modified or deleted since it was loaded.
 * <p>The message, describing the modification, is put together when read, in the language of the reader,
 * see {@link #getMessage()} and {@link #message(Locale)}.
 */
public final class EntityModifiedException extends UpdateEntityException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final Entity entity;
	private final @Nullable Entity modified;
	private final Collection<Column<?>> columns;

	/**
	 * Instantiates a new EntityModifiedException
	 * @param entity the entity being updated
	 * @param modified the current (modified) version of the entity, null if it has been deleted
	 * @param columns the modified columns, an empty collection in case the entity has been deleted
	 */
	public EntityModifiedException(Entity entity, @Nullable Entity modified, Collection<Column<?>> columns) {
		// the message in the language of the one throwing, for a reader not putting the message together itself
		super(message(requireNonNull(entity), modified, requireNonNull(columns), Locale.getDefault()));
		this.entity = entity;
		this.modified = modified;
		this.columns = unmodifiableSet(new HashSet<>(columns));
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

	@Override
	public @Nullable String message(Locale locale) {
		try {
			return message(entity, modified, columns, requireNonNull(locale));
		}
		catch (RuntimeException e) {
			// reading a message must never throw
			return super.message(locale);
		}
	}

	private static String message(Entity entity, @Nullable Entity modified, Collection<Column<?>> columns, Locale locale) {
		String recordModified = Messages.message(EntityModifiedException.class, "record_modified", locale);
		if (modified == null) {
			Entity original = entity.copy().mutable();
			original.revert();

			return recordModified + ", " + original + " " + Messages.message(EntityModifiedException.class, "has_been_deleted", locale);
		}
		StringBuilder builder = new StringBuilder(recordModified).append(": ").append(entity.type());
		for (Column<?> column : columns) {
			builder.append("\n").append(column)
							.append(": ").append(entity.original(column))
							.append(" -> ").append(modified.get(column));
		}

		return builder.toString();
	}
}
