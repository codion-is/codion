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

import is.codion.framework.domain.entity.Entity;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a link between two entity models.
 */
public sealed interface ModelLink permits DefaultModelLink, ForeignKeyModelLink {

	/**
	 * <p>Returns a new {@link Builder} instance.
	 * <p>Note that if the linked model contains a table model it is configured so that a query condition is required for it to show
	 * any data, via {@link EntityQueryModel#conditionRequired()}
	 * @param model the model to link
	 * @param <M> the {@link EntityModel} type
	 * @param <E> the {@link EntityEditModel} type
	 * @param <T> the {@link EntityTableModel} type
	 * @param <R> the {@link EntityEditor} type
	 * @param <B> the builder type
	 * @return a {@link Builder} instance
	 */
	static <M extends EntityModel<M, E, T, R>, E extends EntityEditModel<M, E, T, R>, T extends EntityTableModel<M, E, T, R>,
					B extends Builder<B>, R extends EntityEditor<R>> Builder<B> builder(M model) {
		return new DefaultModelLink.DefaultBuilder<>(model);
	}

	/**
	 * Builds a {@link ModelLink}
	 * @param <B> the builder type
	 */
	interface Builder<B extends Builder<B>> {

		/**
		 * Note that only active model links respond to parent model selection by default.
		 * @param onSelection called when the selection changes in the parent model
		 * @return this builder
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
		ModelLink build();
	}
}
