/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.model.table.SelectionModel;
import is.codion.common.properties.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ColorProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;

import java.util.Collection;
import java.util.List;

/**
 * Specifies a table model containing {@link Entity} instances.
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityTableModel}
 */
public interface EntityTableModel<E extends EntityEditModel> extends FilteredModel<Entity> {

  /**
   * Specifies whether the values of hidden columns are included in the underlying query<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> QUERY_HIDDEN_COLUMNS = Configuration.booleanValue("codion.client.queryHiddenColumns", true);

  /**
   * Specifies whether the table model sort order is used as a basis for the query order by clause.
   * Note that this only applies to column properties.
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> ORDER_QUERY_BY_SORT_ORDER = Configuration.booleanValue("codion.client.orderQueryBySortOrder", false);

  /**
   * Defines the actions a table model can perform when entities are inserted via the associated edit model
   */
  enum InsertAction {
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
    ADD_TOP_SORTED
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
   * @return the underlying domain entities
   */
  Entities entities();

  /**
   * @return the definition of the underlying entity
   */
  EntityDefinition entityDefinition();

  /**
   * Returns the {@link EntityEditModel} associated with this table model
   * @return the edit model associated with this table model
   */
  E editModel();

  /**
   * Sets {@code foreignKeyValues} as the search condition values for the given foreign key.
   * @param foreignKey the foreign key
   * @param foreignKeyValues the entities to use as condition values
   * @return true if the table search condition changed due to this call and a refresh is in order, false otherwise
   */
  boolean setForeignKeyConditionValues(ForeignKey foreignKey, Collection<Entity> foreignKeyValues);

  /**
   * For every entity in this table model, replaces the foreign key instance bearing the primary
   * key with the corresponding entity from {@code foreignKeyValues}, useful when property
   * values have been changed in the referenced entity that must be reflected in the table model.
   * @param foreignKey the foreign key
   * @param foreignKeyValues the foreign key entities
   */
  void replaceForeignKeyValues(ForeignKey foreignKey, Collection<Entity> foreignKeyValues);

  /**
   * Adds the given entities to the bottom of this table model.
   * It is recommended to only manually add entities directly to this table model after they have
   * been inserted into the underlying table since otherwise they will disappear during the next table model refresh.
   * @param entities the entities to add
   * @throws IllegalArgumentException in case the entities fail validation
   */
  void addEntities(Collection<Entity> entities);

  /**
   * Adds the given entities to the bottom of this table model and then, if sorting is enabled, sorts this table model.
   * It is recommended to only manually add entities directly to this table model after they have
   * been inserted into the underlying table since otherwise they will disappear during the next table model refresh.
   * @param entities the entities to add
   * @throws IllegalArgumentException in case the entities fail validation
   */
  void addEntitiesSorted(Collection<Entity> entities);

  /**
   * Adds the given entities to the top of this table model.
   * It is recommended to only manually add entities directly to this table model after they have
   * been inserted into the underlying table since otherwise they will disappear during the next table model refresh.
   * @param index the index at which to add
   * @param entities the entities to add
   * @throws IllegalArgumentException in case the entities fail validation
   */
  void addEntitiesAt(int index, Collection<Entity> entities);

  /**
   * Adds the given entities to the top of this table model and then, if sorting is enabled, sorts this table model.
   * It is recommended to only manually add entities directly to this table model after they have
   * been inserted into the underlying table since otherwise they will disappear during the next table model refresh.
   * @param index the index at which to add
   * @param entities the entities to add
   * @throws IllegalArgumentException in case the entities fail validation
   */
  void addEntitiesAtSorted(int index, Collection<Entity> entities);

  /**
   * Replaces the given entities in this table model
   * @param entities the entities to replace
   * @throws IllegalArgumentException in case the replacement entities fail validation
   */
  void replaceEntities(Collection<Entity> entities);

  /**
   * Refreshes the entities with the given keys by re-selecting them from the underlying database.
   * @param keys the keys of the entities to refresh
   */
  void refreshEntities(List<Key> keys);

  /**
   * @return the {@link EntityTableConditionModel} instance used by this table model
   */
  EntityTableConditionModel tableConditionModel();

  /**
   * @return true if this table model is editable
   */
  boolean isEditable();

  /**
   * @param editable true if this table model should be editable
   */
  void setEditable(boolean editable);

  /**
   * @return true if the underlying edit model allows deletion of records
   */
  boolean isDeleteEnabled();

  /**
   * @return true if the underlying edit model is read only
   */
  boolean isReadOnly();

  /**
   * @return true if the underlying edit model allows updating of records
   */
  boolean isUpdateEnabled();

  /**
   * @return true if multiple entities can be updated at a time
   */
  boolean isBatchUpdateEnabled();

  /**
   * @param batchUpdateEnabled true if this model should enable multiple entities to be updated at a time
   */
  void setBatchUpdateEnabled(boolean batchUpdateEnabled);

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
   * Returns the maximum number of records to fetch via the underlying query the next time
   * this table model is refreshed, a value of -1 means all records should be fetched
   * @return the fetch count
   */
  int getLimit();

  /**
   * Sets the maximum number of records to fetch via the underlying query the next time
   * this table model is refreshed, a value of -1 means all records should be fetched
   * @param limit the fetch count
   */
  void setLimit(int limit);

  /**
   * Returns whether the values of hidden columns are included when querying data
   * @return true if the values of hidden columns are included when querying data
   */
  boolean isQueryHiddenColumns();

  /**
   * @param queryHiddenColumns true if the values of hidden columns should be included when querying data
   * @see #QUERY_HIDDEN_COLUMNS
   */
  void setQueryHiddenColumns(boolean queryHiddenColumns);

  /**
   * Specifies whether the current sort order is used as a basis for the query order by clause.
   * Note that this only applies to column properties.
   * @return true if the current sort order should be used as a basis for the query order by clause
   */
  boolean isOrderQueryBySortOrder();

  /**
   * Specifies whether the current sort order is used as a basis for the query order by clause.
   * Note that this only applies to column properties.
   * @param orderQueryBySortOrder true if the current sort order should be used as a basis for the query order by clause
   */
  void setOrderQueryBySortOrder(boolean orderQueryBySortOrder);

  /**
   * Updates the given entities. If the entities are unmodified or the list is empty
   * this method returns silently.
   * @param entities the entities to update
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.model.CancelException in case the user cancels the operation
   * @throws is.codion.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @throws IllegalStateException in case this table model has no edit model or if the edit model does not allow updating
   * @see EntityValidator#validate(Entity)
   */
  void update(List<Entity> entities) throws ValidationException, DatabaseException;

  /**
   * Deletes the selected entities
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.model.CancelException in case the user cancels the operation
   * @throws IllegalStateException in case this table model has no edit model or if the edit model does not allow deleting
   */
  void deleteSelected() throws DatabaseException;

  /**
   * Returns a State controlling whether this table model should display all underlying entities
   * when no query condition has been set. Setting this value to 'true' prevents all records from
   * being fetched by accident, when no condition has been set, which is recommended for tables
   * with a large underlying dataset.
   * @return a State specifying whether this table model requires a query condition
   */
  State queryConditionRequiredState();

  /**
   * @return true if entities that are deleted via the associated edit model
   * should be automatically removed from this table model
   */
  boolean isRemoveDeletedEntities();

  /**
   * @param removeDeletedEntities true if entities that are deleted via the associated edit model
   * should be automatically removed from this table model
   */
  void setRemoveDeletedEntities(boolean removeDeletedEntities);

  /**
   * True if this table model reacts to entity edit events, by replacing foreign key values.
   * @return true if this combo box model listens edit events
   * @see EntityEditEvents
   */
  boolean isListenToEditEvents();

  /**
   * Set to true if this table model should react to entity edit events, by replacing foreign key values.
   * @param listenToEditEvents if true then this model listens to entity edit events
   * @see EntityEditEvents
   */
  void setListenToEditEvents(boolean listenToEditEvents);

  /**
   * @return the action performed when entities are inserted via the associated edit model
   */
  InsertAction getInsertAction();

  /**
   * @param insertAction the action to perform when entities are inserted via the associated edit model
   */
  void setInsertAction(InsertAction insertAction);

  /**
   * Finds entities according to the values in {@code keys}
   * @param keys the primary key values to use as condition
   * @return the entities having the primary key values as in {@code keys}
   */
  Collection<Entity> entitiesByKey(Collection<Key> keys);

  /**
   * Sets the selected entities according to the primary keys in {@code primaryKeys}
   * @param keys the primary keys of the entities to select
   */
  void selectEntitiesByKey(Collection<Key> keys);

  /**
   * @param primaryKey the primary key to search by
   * @return the entity with the given primary key from the table model, null if it's not found
   */
  Entity entityByKey(Key primaryKey);

  /**
   * @param primaryKey the primary key
   * @return the row index of the entity with the given primary key, -1 if not found
   */
  int indexOf(Key primaryKey);

  /**
   * Saves any user preferences. Note that if {@link EntityModel#USE_CLIENT_PREFERENCES} is set to 'false',
   * calling this method has no effect.
   */
  void savePreferences();

  /**
   * Arranges the column model so that only the given columns are visible and in the given order
   * @param attributes the column attributes
   */
  void setVisibleColumns(Attribute<?>... attributes);

  /**
   * Arranges the column model so that only the given columns are visible and in the given order
   * @param attributes the column attributes
   */
  void setVisibleColumns(List<Attribute<?>> attributes);

  /**
   * Refreshes the items in this table model, according to the underlying condition
   * @see #tableConditionModel()
   */
  void refresh();

  /**
   * Clears all items from this table model
   */
  void clear();

  /**
   * @return the number of visible rows in this table model
   */
  int getRowCount();

  /**
   * @return the {@link SelectionModel}
   */
  SelectionModel<Entity> selectionModel();

  /**
   * @return a StateObserver indicating if the search condition has changed since last refresh
   */
  StateObserver conditionChangedObserver();

  /**
   * @param listener notified when the selection changes in the underlying selection model
   */
  void addSelectionListener(EventListener listener);

  /**
   * Creates a new {@link ColumnPreferences} instance.
   * @param attribute the attribute
   * @param index the column index, -1 if not visible
   * @param width the column width
   * @return a new {@link ColumnPreferences} instance.
   */
  static ColumnPreferences columnPreferences(Attribute<?> attribute, int index, int width) {
    return new DefaultColumnPreferences(attribute, index, width);
  }

  /**
   * Represents preferences for an Attribute based table column.
   */
  interface ColumnPreferences {

    /**
     * The name of the root element identifying column preferences
     */
    String PREFERENCES_COLUMNS = "columns";

    /**
     * The key for the 'width' property
     */
    String PREFERENCES_COLUMN_WIDTH = "width";

    /**
     * The key for the 'index' property
     */
    String PREFERENCES_COLUMN_INDEX = "index";

    /**
     * @return the column attribute
     */
    Attribute<?> attribute();

    /**
     * @return the column index, -1 if not visible
     */
    int index();

    /**
     * @return true if this column is visible, false if hidden
     */
    boolean isVisible();

    /**
     * @return the column width in pixels
     */
    int width();
  }
}
