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
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultForeignKeyModelLink<M extends EntityModel<M, E, T>, E extends EntityEditModel,
				T extends EntityTableModel<E>> implements ForeignKeyModelLink<M, E, T> {

	private final ForeignKey foreignKey;
	private final ModelLink<M, E, T> modelLink;

	private final boolean clearValueOnEmptySelection;
	private final boolean clearConditionOnEmptySelection;
	private final boolean setValueOnInsert;
	private final boolean setConditionOnInsert;
	private final boolean refreshOnSelection;

	private DefaultForeignKeyModelLink(DefaultBuilder<M, E, T, ?> builder) {
		this.modelLink = ModelLink.builder(builder.model)
						.onSelection(builder.onSelection == null ? new OnSelection() : builder.onSelection)
						.onInsert(builder.onInsert == null ? new OnInsert() : builder.onInsert)
						.onUpdate(builder.onUpdate)
						.onDelete(builder.onDelete)
						.active(builder.active)
						.build();
		this.foreignKey = builder.foreignKey;
		this.clearValueOnEmptySelection = builder.clearValueOnEmptySelection;
		this.clearConditionOnEmptySelection = builder.clearConditionOnEmptySelection;
		this.setValueOnInsert = builder.setValueOnInsert;
		this.setConditionOnInsert = builder.setConditionOnInsert;
		this.refreshOnSelection = builder.refreshOnSelection;
		if (modelLink.model().containsTableModel()) {
			modelLink.model().tableModel().query().condition().persist().add(foreignKey);
		}
	}

	@Override
	public ForeignKey foreignKey() {
		return foreignKey;
	}

	@Override
	public M model() {
		return modelLink.model();
	}

	@Override
	public State active() {
		return modelLink.active();
	}

	@Override
	public void onSelection(Collection<Entity> selectedEntities) {
		modelLink.onSelection(selectedEntities);
	}

	@Override
	public void onInsert(Collection<Entity> insertedEntities) {
		modelLink.onInsert(insertedEntities);
	}

	@Override
	public void onUpdate(Map<Entity, Entity> updatedEntities) {
		modelLink.onUpdate(updatedEntities);
	}

	@Override
	public void onDelete(Collection<Entity> deletedEntities) {
		modelLink.onDelete(deletedEntities);
	}

	private final class OnSelection implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> selectedEntities) {
			if (model().containsTableModel() &&
							setConditionOnSelection(selectedEntities) && refreshOnSelection) {
				model().tableModel().items().refresh(result -> setValueOnSelection(selectedEntities));
			}
			else {
				setValueOnSelection(selectedEntities);
			}
		}

		private boolean setConditionOnSelection(Collection<Entity> selection) {
			if (!selection.isEmpty()) {
				return model().tableModel().query().condition()
								.get(foreignKey).set().in(selection);
			}
			if (clearConditionOnEmptySelection) {
				model().tableModel().query().condition()
								.get(foreignKey).set().in(selection);

				// Always refresh if the selection is empty, since
				// clearing an already empty condition does not change it
				return true;
			}

			return false;
		}

		private void setValueOnSelection(Collection<Entity> entities) {
			Entity foreignKeyValue = entities.isEmpty() ? null : entities.iterator().next();
			if (model().editModel().editor().exists().not().is()
							&& (foreignKeyValue != null || clearValueOnEmptySelection)) {
				model().editModel().editor().value(foreignKey).set(foreignKeyValue);
			}
		}
	}

	private final class OnInsert implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> insertedEntities) {
			Collection<Entity> entities = ofReferencedType(insertedEntities);
			if (!entities.isEmpty()) {
				Entity insertedEntity = entities.iterator().next();
				setValueOnInsert(insertedEntity);
				if (model().containsTableModel() && setConditionOnInsert(insertedEntity)) {
					model().tableModel().items().refresh();
				}
			}
		}

		private void setValueOnInsert(Entity foreignKeyValue) {
			if (model().editModel().editor().exists().not().is() && setValueOnInsert) {
				model().editModel().editor().value(foreignKey).set(foreignKeyValue);
			}
		}

		private boolean setConditionOnInsert(Entity insertedEntity) {
			if (setConditionOnInsert) {
				return model().tableModel().query().condition()
								.get(foreignKey).set().in(insertedEntity);
			}

			return false;
		}

		private Collection<Entity> ofReferencedType(Collection<Entity> entities) {
			return entities.stream()
							.filter(entity -> entity.type().equals(foreignKey.referencedType()))
							.collect(toList());
		}
	}

	static final class DefaultBuilder<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>,
					B extends ForeignKeyModelLink.Builder<M, E, T, B>> implements Builder<M, E, T, B> {

		private static final Consumer<?> EMPTY_CONSUMER = new EmptyConsumer<>();

		private final M model;
		private final ForeignKey foreignKey;

		private @Nullable Consumer<Collection<Entity>> onSelection;
		private @Nullable Consumer<Collection<Entity>> onInsert;
		private Consumer<Map<Entity, Entity>> onUpdate = (Consumer<Map<Entity, Entity>>) EMPTY_CONSUMER;
		private Consumer<Collection<Entity>> onDelete = (Consumer<Collection<Entity>>) EMPTY_CONSUMER;

		private boolean clearValueOnEmptySelection = CLEAR_VALUE_ON_EMPTY_SELECTION.getOrThrow();
		private boolean clearConditionOnEmptySelection = CLEAR_CONDITION_ON_EMPTY_SELECTION.getOrThrow();
		private boolean setValueOnInsert = SET_VALUE_ON_INSERT.getOrThrow();
		private boolean setConditionOnInsert = SET_CONDITION_ON_INSERT.getOrThrow();
		private boolean refreshOnSelection = REFRESH_ON_SELECTION.getOrThrow();
		private boolean active = false;

		DefaultBuilder(M model, ForeignKey foreignKey) {
			this.model = requireNonNull(model);
			this.foreignKey = requireNonNull(foreignKey);
		}

		@Override
		public B onSelection(Consumer<Collection<Entity>> onSelection) {
			this.onSelection = requireNonNull(onSelection);
			return (B) this;
		}

		@Override
		public B onInsert(Consumer<Collection<Entity>> onInsert) {
			this.onInsert = requireNonNull(onInsert);
			return (B) this;
		}

		@Override
		public B onUpdate(Consumer<Map<Entity, Entity>> onUpdate) {
			this.onUpdate = requireNonNull(onUpdate);
			return (B) this;
		}

		@Override
		public B onDelete(Consumer<Collection<Entity>> onDelete) {
			this.onDelete = requireNonNull(onDelete);
			return (B) this;
		}

		@Override
		public B setConditionOnInsert(boolean setConditionOnInsert) {
			this.setConditionOnInsert = setConditionOnInsert;
			return (B) this;
		}

		@Override
		public B setValueOnInsert(boolean setValueOnInsert) {
			this.setValueOnInsert = setValueOnInsert;
			return (B) this;
		}

		@Override
		public B refreshOnSelection(boolean refreshOnSelection) {
			this.refreshOnSelection = refreshOnSelection;
			return (B) this;
		}

		@Override
		public B clearValueOnEmptySelection(boolean clearValueOnEmptySelection) {
			this.clearValueOnEmptySelection = clearValueOnEmptySelection;
			return (B) this;
		}

		@Override
		public B clearConditionOnEmptySelection(boolean clearConditionOnEmptySelection) {
			this.clearConditionOnEmptySelection = clearConditionOnEmptySelection;
			return (B) this;
		}

		@Override
		public B active(boolean active) {
			this.active = active;
			return (B) this;
		}

		@Override
		public ForeignKeyModelLink<M, E, T> build() {
			return new DefaultForeignKeyModelLink<>(this);
		}
	}

	private static final class EmptyConsumer<T> implements Consumer<T> {

		@Override
		public void accept(T result) {}
	}
}
