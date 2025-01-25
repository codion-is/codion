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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A central application model class.
 * @param <E> the type of {@link EntityEditModel}
 * @param <T> the type of {@link EntityTableModel}
 */
public class DefaultEntityApplicationModel<E extends EntityEditModel, T extends EntityTableModel<E>> implements EntityApplicationModel<E, T> {

	private final EntityConnectionProvider connectionProvider;
	private final Version version;
	private final DefaultEntityModels models = new DefaultEntityModels();

	/**
	 * Instantiates a new DefaultEntityApplicationModel
	 * @param connectionProvider the EntityConnectionProvider instance
	 * @throws NullPointerException in case connectionProvider is null
	 */
	public DefaultEntityApplicationModel(EntityConnectionProvider connectionProvider) {
		this(connectionProvider, null);
	}

	/**
	 * Instantiates a new DefaultEntityApplicationModel
	 * @param connectionProvider the EntityConnectionProvider instance
	 * @param version the application version
	 * @throws NullPointerException in case connectionProvider is null
	 */
	public DefaultEntityApplicationModel(EntityConnectionProvider connectionProvider, Version version) {
		this.connectionProvider = requireNonNull(connectionProvider);
		this.version = version;
	}

	@Override
	public final User user() {
		return connectionProvider.connection().user();
	}

	@Override
	public final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	@Override
	public final EntityConnection connection() {
		return connectionProvider.connection();
	}

	@Override
	public final Optional<Version> version() {
		return Optional.ofNullable(version);
	}

	@Override
	public final Entities entities() {
		return connectionProvider.entities();
	}

	@Override
	public final EntityModels<E, T> entityModels() {
		return models;
	}

	@Override
	public final void refresh() {
		for (EntityModel<E, T> entityModel : models.entityModels) {
			if (entityModel.containsTableModel()) {
				entityModel.tableModel().items().refresh();
			}
		}
	}

	private final class DefaultEntityModels implements EntityModels<E, T> {

		private final List<EntityModel<E, T>> entityModels = new ArrayList<>();

		@Override
		public void add(EntityModel<E, T>... entityModels) {
			for (EntityModel<E, T> entityModel : requireNonNull(entityModels)) {
				add(entityModel);
			}
		}

		@Override
		public void add(EntityModel<E, T> entityModel) {
			if (this.entityModels.contains(requireNonNull(entityModel))) {
				throw new IllegalArgumentException("Entity model " + entityModel + " has already been added");
			}
			this.entityModels.add(entityModel);
		}

		@Override
		public boolean contains(Class<? extends EntityModel<E, T>> modelClass) {
			return entityModels.stream()
							.anyMatch(entityModel -> entityModel.getClass().equals(modelClass));
		}

		@Override
		public boolean contains(EntityType entityType) {
			return entityModels.stream()
							.anyMatch(entityModel -> entityModel.entityType().equals(entityType));
		}

		@Override
		public boolean contains(EntityModel<E, T> entityModel) {
			return entityModels.contains(entityModel);
		}

		@Override
		public List<EntityModel<E, T>> get() {
			return Collections.unmodifiableList(entityModels);
		}

		@Override
		public <C extends EntityModel<E, T>> C get(Class<C> modelClass) {
			for (EntityModel<E, T> model : entityModels) {
				if (model.getClass().equals(modelClass)) {
					return (C) model;
				}
			}

			throw new IllegalArgumentException("EntityModel of type: " + modelClass + " not found");
		}

		@Override
		public <C extends EntityModel<E, T>> C get(EntityType entityType) {
			for (EntityModel<E, T> entityModel : entityModels) {
				if (entityModel.entityType().equals(entityType)) {
					return (C) entityModel;
				}
			}

			throw new IllegalArgumentException("EntityModel for type " + entityType + " not  found in model: " + DefaultEntityApplicationModel.this);
		}
	}
}
