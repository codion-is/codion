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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.state.State;
import is.codion.framework.domain.entity.Entity;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultModelLink implements ModelLink {

	private final EntityModel<?, ?, ?> model;
	private final State active;

	private final Consumer<Collection<Entity>> onSelection;
	private final Consumer<Collection<Entity>> onInsert;
	private final Consumer<Map<Entity.Key, Entity>> onUpdate;
	private final Consumer<Collection<Entity>> onDelete;

	private DefaultModelLink(DefaultBuilder builder) {
		this.model = builder.model;
		this.active = State.state(builder.active);
		this.onSelection = builder.onSelection;
		this.onInsert = builder.onInsert;
		this.onUpdate = builder.onUpdate;
		this.onDelete = builder.onDelete;
		if (model.containsTableModel()) {
			model.tableModel().queryModel().conditionRequired().set(true);
		}
	}

	@Override
	public <M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> M model() {
		return (M) model;
	}

	@Override
	public State active() {
		return active;
	}

	@Override
	public void onSelection(Collection<Entity> selectedEntities) {
		onSelection.accept(selectedEntities);
	}

	@Override
	public void onInsert(Collection<Entity> insertedEntities) {
		onInsert.accept(insertedEntities);
	}

	@Override
	public void onUpdate(Map<Entity.Key, Entity> updatedEntities) {
		onUpdate.accept(updatedEntities);
	}

	@Override
	public void onDelete(Collection<Entity> deletedEntities) {
		onDelete.accept(deletedEntities);
	}

	static class DefaultBuilder implements ModelLink.Builder {

		private static final Consumer<?> EMPTY_CONSUMER = new EmptyConsumer<>();

		private final EntityModel<?, ?, ?> model;

		private Consumer<Collection<Entity>> onSelection = (Consumer<Collection<Entity>>) EMPTY_CONSUMER;
		private Consumer<Collection<Entity>> onInsert = (Consumer<Collection<Entity>>) EMPTY_CONSUMER;
		private Consumer<Map<Entity.Key, Entity>> onUpdate = (Consumer<Map<Entity.Key, Entity>>) EMPTY_CONSUMER;
		private Consumer<Collection<Entity>> onDelete = (Consumer<Collection<Entity>>) EMPTY_CONSUMER;
		private boolean active = false;

		DefaultBuilder(EntityModel<?, ?, ?> model) {
			this.model = requireNonNull(model);
		}

		@Override
		public Builder onSelection(Consumer<Collection<Entity>> onSelection) {
			this.onSelection = requireNonNull(onSelection);
			return this;
		}

		@Override
		public Builder onInsert(Consumer<Collection<Entity>> onInsert) {
			this.onInsert = requireNonNull(onInsert);
			return this;
		}

		@Override
		public Builder onUpdate(Consumer<Map<Entity.Key, Entity>> onUpdate) {
			this.onUpdate = requireNonNull(onUpdate);
			return this;
		}

		@Override
		public Builder onDelete(Consumer<Collection<Entity>> onDelete) {
			this.onDelete = requireNonNull(onDelete);
			return this;
		}

		@Override
		public Builder active(boolean active) {
			this.active = active;
			return this;
		}

		@Override
		public ModelLink build() {
			return new DefaultModelLink(this);
		}
	}

	private static final class EmptyConsumer<T> implements Consumer<T> {

		@Override
		public void accept(T result) {}
	}
}
