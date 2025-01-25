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
import is.codion.framework.domain.entity.attribute.ForeignKey;

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

	private DefaultForeignKeyModelLink(DefaultBuilder<M, E, T> builder) {
		this.modelLink = ModelLink.builder(builder.model)
						.onSelection(new OnSelection())
						.onInsert(new OnInsert())
						.onUpdate(new OnUpdate())
						.onDelete(new OnDelete())
						.active(builder.active)
						.build();
		this.foreignKey = builder.foreignKey;
		this.clearValueOnEmptySelection = builder.clearValueOnEmptySelection;
		this.clearConditionOnEmptySelection = builder.clearConditionOnEmptySelection;
		this.setValueOnInsert = builder.setValueOnInsert;
		this.setConditionOnInsert = builder.setConditionOnInsert;
		this.refreshOnSelection = builder.refreshOnSelection;
		if (modelLink.model().containsTableModel()) {
			modelLink.model().tableModel().queryModel().conditions().persist().add(foreignKey);
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
	public void onUpdate(Map<Entity.Key, Entity> updatedEntities) {
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

		private boolean setConditionOnSelection(Collection<Entity> selectedEntities) {
			if (!selectedEntities.isEmpty() || clearConditionOnEmptySelection) {
				return model().tableModel().queryModel().conditions()
								.get(foreignKey).set().in(selectedEntities);
			}

			return false;
		}

		private void setValueOnSelection(Collection<Entity> entities) {
			Entity foreignKeyValue = entities.isEmpty() ? null : entities.iterator().next();
			if (model().editModel().editor().exists().not().get()
							&& (foreignKeyValue != null || clearValueOnEmptySelection)) {
				model().editModel().value(foreignKey).set(foreignKeyValue);
			}
		}
	}

	private final class OnInsert implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> insertedEntities) {
			Collection<Entity> entities = ofReferencedType(insertedEntities);
			if (!entities.isEmpty()) {
				model().editModel().add(foreignKey, entities);
				Entity insertedEntity = entities.iterator().next();
				setValueOnInsert(insertedEntity);
				if (model().containsTableModel() && setConditionOnInsert(insertedEntity)) {
					model().tableModel().items().refresh();
				}
			}
		}

		private void setValueOnInsert(Entity foreignKeyValue) {
			if (model().editModel().editor().exists().not().get() && setValueOnInsert) {
				model().editModel().value(foreignKey).set(foreignKeyValue);
			}
		}

		private boolean setConditionOnInsert(Entity insertedEntity) {
			if (setConditionOnInsert) {
				return model().tableModel().queryModel().conditions()
								.get(foreignKey).set().in(insertedEntity);
			}

			return false;
		}
	}

	private final class OnUpdate implements Consumer<Map<Entity.Key, Entity>> {

		@Override
		public void accept(Map<Entity.Key, Entity> updatedEntities) {
			Collection<Entity> entities = ofReferencedType(updatedEntities.values());
			model().editModel().replace(foreignKey, entities);
			if (model().containsTableModel() &&
							// These will be replaced by the table model if it handles edit events
							model().tableModel().handleEditEvents().not().get()) {
				model().tableModel().replace(foreignKey, entities);
			}
		}
	}

	private final class OnDelete implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> deletedEntities) {
			model().editModel().remove(foreignKey, ofReferencedType(deletedEntities));
		}
	}

	private Collection<Entity> ofReferencedType(Collection<Entity> entities) {
		return entities.stream()
						.filter(entity -> entity.type().equals(foreignKey.referencedType()))
						.collect(toList());
	}

	static final class DefaultBuilder<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> implements Builder<M, E, T> {

		private final M model;
		private final ForeignKey foreignKey;

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
		public Builder<M, E, T> setConditionOnInsert(boolean setConditionOnInsert) {
			this.setConditionOnInsert = setConditionOnInsert;
			return this;
		}

		@Override
		public Builder<M, E, T> setValueOnInsert(boolean setValueOnInsert) {
			this.setValueOnInsert = setValueOnInsert;
			return this;
		}

		@Override
		public Builder<M, E, T> refreshOnSelection(boolean refreshOnSelection) {
			this.refreshOnSelection = refreshOnSelection;
			return this;
		}

		@Override
		public Builder<M, E, T> clearValueOnEmptySelection(boolean clearValueOnEmptySelection) {
			this.clearValueOnEmptySelection = clearValueOnEmptySelection;
			return this;
		}

		@Override
		public Builder<M, E, T> clearConditionOnEmptySelection(boolean clearConditionOnEmptySelection) {
			this.clearConditionOnEmptySelection = clearConditionOnEmptySelection;
			return this;
		}

		@Override
		public Builder<M, E, T> active(boolean active) {
			this.active = active;
			return this;
		}

		@Override
		public ForeignKeyModelLink<M, E, T> build() {
			return new DefaultForeignKeyModelLink<>(this);
		}
	}
}
