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

final class DefaultForeignKeyModelLink implements ForeignKeyModelLink {

	private final ForeignKey foreignKey;
	private final ModelLink modelLink;

	private final boolean clearValueOnEmptySelection;
	private final boolean clearConditionOnEmptySelection;
	private final boolean setValueOnInsert;
	private final boolean setConditionOnInsert;
	private final boolean refreshOnSelection;

	private DefaultForeignKeyModelLink(DefaultBuilder builder) {
		this.modelLink = ModelLink.builder(builder.model)
						.onSelection(builder.onSelection == null ? new OnSelection() : builder.onSelection)
						.onInsert(builder.onInsert == null ? new OnInsert() : builder.onInsert)
						.onUpdate(builder.onUpdate == null ? new OnUpdate() : builder.onUpdate)
						.onDelete(builder.onDelete == null ? new OnDelete() : builder.onDelete)
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
	public <M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> M model() {
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
				model().editModel().editor().value(foreignKey).set(foreignKeyValue);
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
				model().editModel().editor().value(foreignKey).set(foreignKeyValue);
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

	static final class DefaultBuilder implements Builder {

		private final EntityModel<?, ?, ?> model;
		private final ForeignKey foreignKey;

		private Consumer<Collection<Entity>> onSelection;
		private Consumer<Collection<Entity>> onInsert;
		private Consumer<Map<Entity.Key, Entity>> onUpdate;
		private Consumer<Collection<Entity>> onDelete;

		private boolean clearValueOnEmptySelection = CLEAR_VALUE_ON_EMPTY_SELECTION.getOrThrow();
		private boolean clearConditionOnEmptySelection = CLEAR_CONDITION_ON_EMPTY_SELECTION.getOrThrow();
		private boolean setValueOnInsert = SET_VALUE_ON_INSERT.getOrThrow();
		private boolean setConditionOnInsert = SET_CONDITION_ON_INSERT.getOrThrow();
		private boolean refreshOnSelection = REFRESH_ON_SELECTION.getOrThrow();
		private boolean active = false;

		DefaultBuilder(EntityModel<?, ?, ?> model, ForeignKey foreignKey) {
			this.model = requireNonNull(model);
			this.foreignKey = requireNonNull(foreignKey);
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
		public Builder setConditionOnInsert(boolean setConditionOnInsert) {
			this.setConditionOnInsert = setConditionOnInsert;
			return this;
		}

		@Override
		public Builder setValueOnInsert(boolean setValueOnInsert) {
			this.setValueOnInsert = setValueOnInsert;
			return this;
		}

		@Override
		public Builder refreshOnSelection(boolean refreshOnSelection) {
			this.refreshOnSelection = refreshOnSelection;
			return this;
		}

		@Override
		public Builder clearValueOnEmptySelection(boolean clearValueOnEmptySelection) {
			this.clearValueOnEmptySelection = clearValueOnEmptySelection;
			return this;
		}

		@Override
		public Builder clearConditionOnEmptySelection(boolean clearConditionOnEmptySelection) {
			this.clearConditionOnEmptySelection = clearConditionOnEmptySelection;
			return this;
		}

		@Override
		public Builder active(boolean active) {
			this.active = active;
			return this;
		}

		@Override
		public ForeignKeyModelLink build() {
			return new DefaultForeignKeyModelLink(this);
		}
	}
}
