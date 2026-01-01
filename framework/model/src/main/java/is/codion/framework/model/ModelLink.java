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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.state.State;
import is.codion.framework.domain.entity.Entity;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a link between two entity models.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 * @see #onSelection(Collection)
 * @see #onInsert(Collection)
 * @see #onUpdate(Map)
 * @see #onDelete(Collection)
 */
public interface ModelLink<M extends EntityModel<M, E, T>, E extends EntityEditModel,
				T extends EntityTableModel<E>> {

	/**
	 * @return the linked model
	 */
	M model();

	/**
	 * <p>Controls the active status of this link.
	 * <p>Active model links respond to the parent model selection via {@link #onSelection(Collection)}.
	 * @return the {@link State} controlling the active status of this model link
	 * @see #onSelection(Collection)
	 */
	State active();

	/**
	 * <p>Called when the selection changes in the parent model or when this link is activated.
	 * <p>Note that only active links are required to handle parent model selection.
	 * @param selectedEntities the selected entities
	 * @see #active()
	 */
	void onSelection(Collection<Entity> selectedEntities);

	/**
	 * Called when insert is performed in the parent model, regardless of entity type.
	 * @param insertedEntities the inserted entities
	 */
	void onInsert(Collection<Entity> insertedEntities);

	/**
	 * Called when an update is performed in the parent model, regardless of entity type.
	 * @param updatedEntities the updated entities, mapped to their state before the update
	 */
	void onUpdate(Map<Entity, Entity> updatedEntities);

	/**
	 * Called when delete is performed in the parent model, regardless of entity type.
	 * @param deletedEntities the deleted entities
	 */
	void onDelete(Collection<Entity> deletedEntities);

	/**
	 * <p>Returns a new {@link Builder} instance.
	 * <p>Note that if the linked model contains a table model it is configured so that a query condition is required for it to show
	 * any data, via {@link EntityQueryModel#conditionRequired()}
	 * @param model the model to link
	 * @param <M> the {@link EntityModel} type
	 * @param <E> the {@link EntityEditModel} type
	 * @param <T> the {@link EntityTableModel} type
	 * @param <B> the builder type
	 * @return a {@link Builder} instance
	 */
	static <M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>,
					B extends Builder<M, E, T, B>> Builder<M, E, T, B> builder(M model) {
		return new DefaultModelLink.DefaultBuilder<>(model);
	}

	/**
	 * Builds a {@link ModelLink}
	 * @param <M> the {@link EntityModel} type
	 * @param <E> the {@link EntityEditModel} type
	 * @param <T> the {@link EntityTableModel} type
	 * @param <B> the builder type
	 */
	interface Builder<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>,
					B extends Builder<M, E, T, B>> {

		/**
		 * Note that only active model links respond to parent model selection by default.
		 * @param onSelection called when the selection changes in the parent model
		 * @return this builder
		 * @see #active()
		 */
		B onSelection(Consumer<Collection<Entity>> onSelection);

		/**
		 * @param onInsert called when an insert is performed in the parent model
		 * @return this builder
		 */
		B onInsert(Consumer<Collection<Entity>> onInsert);

		/**
		 * @param onUpdate called when an update is performed in the parent model
		 * @return this builder
		 */
		B onUpdate(Consumer<Map<Entity, Entity>> onUpdate);

		/**
		 * @param onDelete called when delete is performed in the parent model
		 * @return this builder
		 */
		B onDelete(Consumer<Collection<Entity>> onDelete);

		/**
		 * @param active the initial active state of this link
		 * @return this builder
		 */
		B active(boolean active);

		/**
		 * @return a {@link ModelLink}
		 */
		ModelLink<M, E, T> build();
	}
}
