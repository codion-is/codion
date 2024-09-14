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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.FilterModel;
import is.codion.common.model.table.TableSelectionModel;
import is.codion.common.observer.Observer;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.ColorProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Collection;
import java.util.Optional;

/**
 * Specifies a table model containing {@link Entity} instances.
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityTableModel}
 */
public interface EntityTableModel<E extends EntityEditModel> extends FilterModel<Entity> {

	/**
	 * Specifies the default action a table model takes when entities are inserted via its edit model.
	 * <li>Value type: {@link OnInsert}
	 * <li>Default value: {@link OnInsert#ADD_TOP}
	 */
	PropertyValue<OnInsert> ON_INSERT = Configuration.enumValue(EntityTableModel.class.getName() + ".onInsert", OnInsert.class, OnInsert.ADD_TOP);

	/**
	 * Specifies whether table models handle entity edit events, by replacing updated entities
	 * <li>Value type: Boolean
	 * <li>Default value: true
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
	 * @return the {@link EntityTableConditionModel} instance used by this table model
	 */
	EntityTableConditionModel conditionModel();

	/**
	 * @return the {@link State} controlling whether this table model is editable
	 */
	State editable();

	/**
	 * @param row the row for which to retrieve the background color
	 * @param attribute the attribute for which to retrieve the background color
	 * @return an Object representing the background color for this row and attribute, specified by the row entity
	 * @see EntityDefinition.Builder#backgroundColorProvider(ColorProvider)
	 */
	Object backgroundColor(int row, Attribute<?> attribute);

	/**
	 * @param row the row for which to retrieve the foreground color
	 * @param attribute the attribute for which to retrieve the foreground color
	 * @return an Object representing the foreground color for this row and attribute, specified by the row entity
	 * @see EntityDefinition.Builder#foregroundColorProvider(ColorProvider)
	 */
	Object foregroundColor(int row, Attribute<?> attribute);

	/**
	 * Returns the ValueSet controlling which attributes are included when selecting entities to populate this model.
	 * Note that an empty ValueSet indicates that the default select attributes should be used.
	 * @return the ValueSet controlling the selected attributes
	 */
	ValueSet<Attribute<?>> attributes();

	/**
	 * Returns the Value controlling the maximum number of rows to fetch via the underlying query the next time
	 * this table model is refreshed, a null value means all rows should be fetched
	 * @return the {@link Value} controlling the limit
	 */
	Value<Integer> limit();

	/**
	 * Controls the order by clause to use when selecting the data for this model.
	 * Setting this value to null reverts back to the default order by
	 * for the underlying entity, if one has been specified
	 * @return the {@link Value} controlling the order by clause
	 * @see EntityDefinition#orderBy()
	 */
	Value<OrderBy> orderBy();

	/**
	 * Deletes the selected entities
	 * @return the deleted entities
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.model.CancelException in case the user cancels the operation
	 * @throws IllegalStateException in case this table model has no edit model or if the edit model does not allow deleting
	 */
	Collection<Entity> deleteSelected() throws DatabaseException;

	/**
	 * Returns a State controlling whether this table model should display all underlying entities
	 * when no query condition has been set. Setting this value to 'true' prevents all rows from
	 * being fetched by accident, when no condition has been set, which is recommended for tables
	 * with a large underlying dataset.
	 * @return a State specifying whether this table model requires a query condition
	 */
	State conditionRequired();

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
	 * Finds entities in this table model according to the values in {@code keys}
	 * @param keys the primary key values to use as condition
	 * @return the entities from this table model having the primary key values as in {@code keys}
	 */
	Collection<Entity> find(Collection<Entity.Key> keys);

	/**
	 * Selects entities according to the primary keys in {@code primaryKeys}
	 * @param keys the primary keys of the entities to select
	 */
	void select(Collection<Entity.Key> keys);

	/**
	 * Finds the entity in this table model having the given primary key
	 * @param primaryKey the primary key to search by
	 * @return the entity with the given primary key from the table model, an empty Optional if not found
	 */
	Optional<Entity> find(Entity.Key primaryKey);

	/**
	 * @param primaryKey the primary key
	 * @return the row index of the entity with the given primary key, -1 if not found
	 */
	int indexOf(Entity.Key primaryKey);

	/**
	 * Refreshes the items in this table model, according to the underlying condition
	 * @see #conditionModel()
	 */
	void refresh();

	/**
	 * Clears all items from this table model
	 */
	void clear();

	/**
	 * @return the number of visible rows in this table model
	 */
	int rowCount();

	/**
	 * @return the {@link TableSelectionModel}
	 */
	TableSelectionModel<Entity> selectionModel();

	/**
	 * @return a {@link StateObserver} indicating if the search condition has changed since last refresh
	 */
	StateObserver conditionChanged();

	/**
	 * @return an observer notified when the selection changes in the underlying selection model
	 */
	Observer<?> selectionChanged();
}
