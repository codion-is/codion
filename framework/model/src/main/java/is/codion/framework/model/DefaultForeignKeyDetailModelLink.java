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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link ForeignKeyDetailModelLink} implementation.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 */
public class DefaultForeignKeyDetailModelLink<M extends DefaultEntityModel<M, E, T>, E extends AbstractEntityEditModel,
				T extends EntityTableModel<E>> extends DefaultDetailModelLink<M, E, T> implements ForeignKeyDetailModelLink<M, E, T> {

	private final ForeignKey foreignKey;
	private final State clearForeignKeyValueOnEmptySelection = State.state(CLEAR_FOREIGN_KEY_VALUE_ON_EMPTY_SELECTION.getOrThrow());
	private final State clearForeignKeyConditionOnEmptySelection = State.state(CLEAR_FOREIGN_KEY_CONDITION_ON_EMPTY_SELECTION.getOrThrow());
	private final State setForeignKeyValueOnInsert = State.state(SET_FOREIGN_KEY_VALUE_ON_INSERT.getOrThrow());
	private final State setForeignKeyConditionOnInsert = State.state(SET_FOREIGN_KEY_CONDITION_ON_INSERT.getOrThrow());
	private final State refreshOnSelection = State.state(REFRESH_ON_SELECTION.getOrThrow());

	/**
	 * @param detailModel the detail model
	 * @param foreignKey the foreign key to base this link on
	 */
	public DefaultForeignKeyDetailModelLink(M detailModel, ForeignKey foreignKey) {
		super(detailModel);
		this.foreignKey = requireNonNull(foreignKey);
		if (detailModel.containsTableModel()) {
			detailModel.tableModel().queryModel().conditions().persist().add(foreignKey);
		}
	}

	@Override
	public final ForeignKey foreignKey() {
		return foreignKey;
	}

	@Override
	public final State setForeignKeyConditionOnInsert() {
		return setForeignKeyConditionOnInsert;
	}

	@Override
	public final State setForeignKeyValueOnInsert() {
		return setForeignKeyValueOnInsert;
	}

	@Override
	public final State refreshOnSelection() {
		return refreshOnSelection;
	}

	@Override
	public final State clearForeignKeyValueOnEmptySelection() {
		return clearForeignKeyValueOnEmptySelection;
	}

	@Override
	public final State clearForeignKeyConditionOnEmptySelection() {
		return clearForeignKeyConditionOnEmptySelection;
	}

	@Override
	public void onSelection(Collection<Entity> selectedEntities) {
		if (detailModel().containsTableModel() &&
						setForeignKeyConditionOnSelection(selectedEntities) && refreshOnSelection.get()) {
			detailModel().tableModel().items().refresh(items -> setForeignKeyValueOnSelection(selectedEntities));
		}
		else {
			setForeignKeyValueOnSelection(selectedEntities);
		}
	}

	@Override
	public void onInsert(Collection<Entity> insertedEntities) {
		Collection<Entity> entities = ofReferencedType(insertedEntities);
		if (!entities.isEmpty()) {
			detailModel().editModel().add(foreignKey, entities);
			Entity insertedEntity = entities.iterator().next();
			setForeignKeyValueOnInsert(insertedEntity);
			if (detailModel().containsTableModel() && setForeignKeyConditionOnInsert(insertedEntity)) {
				detailModel().tableModel().items().refresh();
			}
		}
	}

	@Override
	public void onUpdate(Map<Entity.Key, Entity> updatedEntities) {
		Collection<Entity> entities = ofReferencedType(updatedEntities.values());
		detailModel().editModel().replace(foreignKey, entities);
		if (detailModel().containsTableModel() &&
						// These will be replaced by the table model if it handles edit events
						detailModel().tableModel().handleEditEvents().not().get()) {
			detailModel().tableModel().replace(foreignKey, entities);
		}
	}

	@Override
	public void onDelete(Collection<Entity> deletedEntities) {
		detailModel().editModel().remove(foreignKey, ofReferencedType(deletedEntities));
	}

	private Collection<Entity> ofReferencedType(Collection<Entity> entities) {
		return entities.stream()
						.filter(entity -> entity.entityType().equals(foreignKey.referencedType()))
						.collect(toList());
	}

	private void setForeignKeyValueOnSelection(Collection<Entity> entities) {
		Entity foreignKeyValue = entities.isEmpty() ? null : entities.iterator().next();
		if (detailModel().editModel().editor().exists().not().get()
						&& (foreignKeyValue != null || clearForeignKeyValueOnEmptySelection.get())) {
			detailModel().editModel().value(foreignKey).set(foreignKeyValue);
		}
	}

	private void setForeignKeyValueOnInsert(Entity foreignKeyValue) {
		if (detailModel().editModel().editor().exists().not().get() && setForeignKeyValueOnInsert.get()) {
			detailModel().editModel().value(foreignKey).set(foreignKeyValue);
		}
	}

	private boolean setForeignKeyConditionOnSelection(Collection<Entity> selectedEntities) {
		if (!selectedEntities.isEmpty() || clearForeignKeyConditionOnEmptySelection.get()) {
			return detailModel().tableModel().queryModel().conditions()
							.get(foreignKey).set().in(selectedEntities);
		}

		return false;
	}

	private boolean setForeignKeyConditionOnInsert(Entity insertedEntity) {
		if (setForeignKeyConditionOnInsert.get()) {
			return detailModel().tableModel().queryModel().conditions()
							.get(foreignKey).set().in(insertedEntity);
		}

		return false;
	}
}
