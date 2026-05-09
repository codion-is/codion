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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityEditModel} implementation
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 * @param <R> the {@link EntityEditor} type
 */
public class DefaultEntityEditModel<M extends EntityModel<M, E, T, R>, E extends EntityEditModel<M, E, T, R>,
				T extends EntityTableModel<M, E, T, R>, R extends EntityEditor<R>> implements EntityEditModel<M, E, T, R> {

	private final R editor;

	/**
	 * Instantiates a new {@link DefaultEntityEditModel} based on the given editor
	 * @param editor the editor
	 */
	public DefaultEntityEditModel(R editor) {
		this.editor = requireNonNull(editor);
	}

	@Override
	public final Entities entities() {
		return editor.entities();
	}

	@Override
	public final EntityDefinition entityDefinition() {
		return editor.entityDefinition();
	}

	@Override
	public final String toString() {
		return getClass() + ", " + entityType();
	}

	@Override
	public final EntityType entityType() {
		return editor.entityDefinition().type();
	}

	@Override
	public final EntityConnectionProvider connectionProvider() {
		return editor.connectionProvider();
	}

	@Override
	public final EntityConnection connection() {
		return editor.connectionProvider().connection();
	}

	@Override
	public final R editor() {
		return editor;
	}
}