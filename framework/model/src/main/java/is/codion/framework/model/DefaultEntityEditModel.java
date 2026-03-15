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

import is.codion.common.reactive.state.State;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.EntityEditor.PersistTask;
import is.codion.framework.model.EntityEditor.PersistTasks;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityEditModel} implementation
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 * @param <R> the {@link EntityEditor} type
 */
public class DefaultEntityEditModel<M extends EntityModel<M, E, T, R>, E extends EntityEditModel<M, E, T, R>,
				T extends EntityTableModel<M, E, T, R>, R extends EntityEditor> implements EntityEditModel<M, E, T, R> {

	private final R editor;
	private final DefaultPersistTasks tasks;
	private final DefaultSettings settings;

	/**
	 * Instantiates a new {@link DefaultEntityEditModel} based on the given editor
	 * @param editor the editor
	 */
	public DefaultEntityEditModel(R editor) {
		this.editor = requireNonNull(editor);
		this.settings = new DefaultSettings(editor.entityDefinition().readOnly());
		this.tasks = new DefaultPersistTasks();
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
	public final Settings settings() {
		return settings;
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

	@Override
	public final PersistTasks tasks() {
		return tasks;
	}

	@Override
	public final Entity insert() throws EntityValidationException {
		return tasks.insert().prepare().perform().handle();
	}

	@Override
	public final Collection<Entity> insert(Collection<Entity> entities) throws EntityValidationException {
		return tasks.insert(entities).prepare().perform().handle();
	}

	@Override
	public final Entity update() throws EntityValidationException {
		return tasks.update().prepare().perform().handle();
	}

	@Override
	public final Collection<Entity> update(Collection<Entity> entities) throws EntityValidationException {
		return tasks.update(entities).prepare().perform().handle();
	}

	@Override
	public final Entity delete() {
		return tasks.delete().prepare().perform().handle();
	}

	@Override
	public final Collection<Entity> delete(Collection<Entity> entities) {
		return tasks.delete(entities).prepare().perform().handle();
	}

	private final class DefaultPersistTasks implements PersistTasks {

		@Override
		public PersistTask<Entity> insert() throws EntityValidationException {
			settings.verifyInsertEnabled();

			return editor.tasks(connection()).insert();
		}

		@Override
		public PersistTask<Entity> insert(Entity entity) throws EntityValidationException {
			settings.verifyInsertEnabled();

			return editor.tasks(connection()).insert(entity);
		}

		@Override
		public PersistTask<Collection<Entity>> insert(Collection<Entity> entities) throws EntityValidationException {
			settings.verifyInsertEnabled();

			return editor.tasks(connection()).insert(entities);
		}

		@Override
		public PersistTask<Entity> update() throws EntityValidationException {
			settings.verifyUpdateEnabled(1);

			return editor.tasks(connection()).update();
		}

		@Override
		public PersistTask<Entity> update(Entity entity) throws EntityValidationException {
			settings.verifyUpdateEnabled(1);

			return editor.tasks(connection()).update(entity);
		}

		@Override
		public PersistTask<Collection<Entity>> update(Collection<Entity> entities) throws EntityValidationException {
			settings.verifyUpdateEnabled(requireNonNull(entities).size());

			return editor.tasks(connection()).update(entities);
		}

		@Override
		public PersistTask<Entity> delete() {
			settings.verifyDeleteEnabled();

			return editor.tasks(connection()).delete();
		}

		@Override
		public PersistTask<Entity> delete(Entity entity) {
			settings.verifyDeleteEnabled();

			return editor.tasks(connection()).delete(entity);
		}

		@Override
		public PersistTask<Collection<Entity>> delete(Collection<Entity> entities) {
			settings.verifyDeleteEnabled();

			return editor.tasks(connection()).delete(entities);
		}
	}

	private static final class DefaultSettings implements Settings {

		private final State readOnly;
		private final State insertEnabled = State.state(true);
		private final State updateEnabled = State.state(true);
		private final State updateMultipleEnabled = State.state(true);
		private final State deleteEnabled = State.state(true);

		private DefaultSettings(boolean readOnly) {
			this.readOnly = State.state(readOnly);
		}

		@Override
		public State readOnly() {
			return readOnly;
		}

		@Override
		public State insertEnabled() {
			return insertEnabled;
		}

		@Override
		public State updateEnabled() {
			return updateEnabled;
		}

		@Override
		public State updateMultipleEnabled() {
			return updateMultipleEnabled;
		}

		@Override
		public State deleteEnabled() {
			return deleteEnabled;
		}

		private void verifyInsertEnabled() {
			verifyNotReadOnly();
			if (!insertEnabled.is()) {
				throw new IllegalStateException("Inserting is not enabled!");
			}
		}

		private void verifyUpdateEnabled(int entityCount) {
			verifyNotReadOnly();
			if (!updateEnabled.is()) {
				throw new IllegalStateException("Updating is not enabled!");
			}
			if (entityCount > 1 && !updateMultipleEnabled.is()) {
				throw new IllegalStateException("Updating multiple entities is not enabled");
			}
		}

		private void verifyDeleteEnabled() {
			verifyNotReadOnly();
			if (!deleteEnabled.is()) {
				throw new IllegalStateException("Deleting is not enabled!");
			}
		}

		private void verifyNotReadOnly() {
			if (readOnly.is()) {
				throw new IllegalStateException("Edit model is read-only!");
			}
		}
	}
}