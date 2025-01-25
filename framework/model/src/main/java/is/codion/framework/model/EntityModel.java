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

import is.codion.common.state.State;
import is.codion.common.value.ObservableValueSet;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Collection;

/**
 * Specifies a class responsible for, among other things, coordinating a {@link EntityEditModel} and an {@link EntityTableModel}.
 * @param <M> the type of {@link EntityModel} used for detail models
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public interface EntityModel<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

	/**
	 * @return the type of the entity this entity model is based on
	 */
	EntityType entityType();

	/**
	 * @return the connection provider used by this entity model
	 */
	EntityConnectionProvider connectionProvider();

	/**
	 * Do not cache or keep the connection returned by this method in a long living field,
	 * since it may become invalid and thereby unusable.
	 * @return the connection used by this entity model
	 */
	EntityConnection connection();

	/**
	 * @return the underlying domain entities
	 */
	Entities entities();

	/**
	 * @return the definition of the underlying entity
	 */
	EntityDefinition entityDefinition();

	/**
	 * @param <C> the edit model type
	 * @return the {@link EntityEditModel} instance used by this {@link EntityModel}
	 */
	<C extends E> C editModel();

	/**
	 * @param <C> the table model type
	 * @return the {@link EntityTableModel}
	 * @throws IllegalStateException in case no table model is available
	 */
	<C extends T> C tableModel();

	/**
	 * @return true if this {@link EntityModel} contains a {@link EntityTableModel}
	 */
	boolean containsTableModel();

	/**
	 * @return the detail models
	 */
	DetailModels<M, E, T> detailModels();

	/**
	 * Manages the detail models for a {@link EntityModel}
	 * @param <M> the type of {@link EntityModel} used for detail models
	 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityModel}
	 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
	 */
	interface DetailModels<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

		/**
		 * @return an unmodifiable collection containing the detail models this model contains
		 */
		Collection<M> get();

		/**
		 * @return detail models with an active link to this model, that is, those that should respond to master model events
		 * @see ModelLink#active()
		 */
		ObservableValueSet<M> active();

		/**
		 * <p>Adds the given detail models to this model, based on the first fitting foreign key.
		 * <p>A side effect if a detail model contains a table model is that it is configured so that a query condition is required for it to show
		 * any data, via {@link EntityQueryModel#conditionRequired()}.
		 * @param detailModels the detail models to add
		 * @throws IllegalArgumentException in case no foreign key exists between the entities involved
		 */
		void add(M... detailModels);

		/**
		 * <p>Adds the given detail model to this model, based on the first fitting foreign key.
		 * <p>A side effect if the detail model contains a table model is that it is configured so that a query condition is required for it to show
		 * any data, via {@link EntityQueryModel#conditionRequired()}.
		 * @param detailModel the detail model
		 * @throws IllegalArgumentException in case no foreign key exists between the entities involved
		 */
		void add(M detailModel);

		/**
		 * Adds the given detail model to this model, based on the given foreign key.
		 * @param detailModel the detail model
		 * @param foreignKey the foreign key to base the detail model link on
		 */
		void add(M detailModel, ForeignKey foreignKey);

		/**
		 * Adds the given detail model to this model.
		 * @param modelLink the {@link ModelLink} to add
		 * @throws IllegalArgumentException in case the model has already been added
		 */
		void add(ModelLink<M, E, T> modelLink);

		/**
		 * @param modelClass the detail model class
		 * @return true if this model contains a detail model of the given class
		 */
		boolean contains(Class<? extends M> modelClass);

		/**
		 * @param entityType the entityType
		 * @return true if this model contains a detail model for the given entityType
		 */
		boolean contains(EntityType entityType);

		/**
		 * @param detailModel the detail model
		 * @return true if this model contains the given detail model
		 */
		boolean contains(M detailModel);

		/**
		 * Returns the first detail model of the given type
		 * @param <C> the model type
		 * @param modelClass the type of the required {@link EntityModel}
		 * @return the detail model of type {@code entityModelClass}
		 * @throws IllegalArgumentException in case this model does not contain a detail model of the given type
		 */
		<C extends M> C get(Class<C> modelClass);

		/**
		 * Returns a detail model of the given type
		 * @param <C> the detail model type
		 * @param entityType the entityType of the required EntityModel
		 * @return the detail model of type {@code entityModelClass}
		 * @throws IllegalArgumentException in case this model does not contain a detail model for the entityType
		 */
		<C extends M> C get(EntityType entityType);

		/**
		 * @param detailModel the detail model
		 * @return the active State for the given detail model
		 * @throws IllegalArgumentException in case this model does not contain the given detail model
		 */
		State active(M detailModel);
	}
}
