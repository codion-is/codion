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

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.FilterModel;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Collection;

/**
 * Specifies a table model containing {@link Entity} instances.
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityTableModel}
 */
public interface EntityTableModel<E extends EntityEditModel> extends FilterModel<Entity> {

	/**
	 * Specifies the default action a table model takes when entities are inserted via its edit model.
	 * <ul>
	 * <li>Value type: {@link OnInsert}
	 * <li>Default value: {@link OnInsert#ADD_TOP}
	 * </ul>
	 */
	PropertyValue<OnInsert> ON_INSERT = Configuration.enumValue(EntityTableModel.class.getName() + ".onInsert", OnInsert.class, OnInsert.ADD_TOP);

	/**
	 * Specifies whether table models handle entity edit events, by replacing updated entities
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see #handleEditEvents()
	 * @see EntityEditModel#POST_EDIT_EVENTS
	 */
	PropertyValue<Boolean> HANDLE_EDIT_EVENTS = Configuration.booleanValue(EntityTableModel.class.getName() + ".handleEditEvents", true);

	/**
	 * Defines the actions a table model can perform when entities are inserted via the associated edit model
	 */
	enum OnInsert {
		/**
		 * This table model does nothing when entities are inserted via the associated edit model
		 */
		DO_NOTHING,
		/**
		 * The entities inserted via the associated edit model are added as the topmost rows in the model
		 */
		ADD_TOP,
		/**
		 * The entities inserted via the associated edit model are added as the bottommost rows in the model
		 */
		ADD_BOTTOM,
		/**
		 * The entities inserted via the associated edit model are added as the topmost rows in the model,
		 * if sorting is enabled then sorting is performed
		 */
		ADD_TOP_SORTED,
		/**
		 * The entities inserted via the associated edit model are added as the bottommost rows in the model,
		 * if sorting is enabled then sorting is performed
		 */
		ADD_BOTTOM_SORTED
	}

	/**
	 * @return the type of the entity this table model is based on
	 */
	EntityType entityType();

	/**
	 * @return the connection provider used by this table model
	 */
	EntityConnectionProvider connectionProvider();

	/**
	 * Do not cache or keep the connection returned by this method in a long living field,
	 * since it may become invalid and thereby unusable.
	 * @return the connection used by this table model
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
	 * Returns the {@link EntityEditModel} associated with this table model
	 * @return the edit model associated with this table model
	 */
	<C extends E> C editModel();

	/**
	 * For every entity in this table model, replaces the foreign key instance bearing the primary
	 * key with the corresponding entity from {@code foreignKeyValues}, useful when attribute
	 * values have been changed in the referenced entity that must be reflected in the table model.
	 * @param foreignKey the foreign key
	 * @param foreignKeyValues the foreign key entities
	 */
	void replace(ForeignKey foreignKey, Collection<Entity> foreignKeyValues);

	/**
	 * Replaces the given entities in this table model
	 * @param entities the entities to replace
	 */
	void replace(Collection<Entity> entities);

	/**
	 * Refreshes the entities with the given keys by re-selecting them from the underlying database.
	 * @param keys the keys of the entities to refresh
	 */
	void refresh(Collection<Entity.Key> keys);

	/**
	 * @return the {@link State} controlling whether this table model is editable
	 */
	State editable();

	/**
	 * Deletes the selected entities
	 * @return the deleted entities
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.model.CancelException in case the user cancels the operation
	 * @throws IllegalStateException in case this table model has no edit model or if the edit model does not allow deleting
	 */
	Collection<Entity> deleteSelected();

	/**
	 * @return the {@link State} controlling whether this table model handles entity edit events, by replacing updated entities
	 * @see EntityEditEvents
	 */
	State handleEditEvents();

	/**
	 * @return the {@link Value} controlling the action to perform when entities are inserted via the associated edit model
	 * @see #ON_INSERT
	 */
	Value<OnInsert> onInsert();

	/**
	 * @return the {@link State} controlling whether entities that are deleted via the associated edit model
	 * should be automatically removed from this table model
	 */
	State removeDeleted();

	/**
	 * Selects entities according to the primary keys in {@code primaryKeys}
	 * @param keys the primary keys of the entities to select
	 */
	void select(Collection<Entity.Key> keys);

	/**
	 * @return the {@link MultiSelection} instance
	 */
	MultiSelection<Entity> selection();

	/**
	 * @return the underlying query model
	 */
	EntityQueryModel queryModel();
}
