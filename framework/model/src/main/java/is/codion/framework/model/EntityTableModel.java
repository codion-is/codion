/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.model.Refreshable;
import is.codion.common.model.table.SelectionModel;
import is.codion.common.state.State;
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
import java.util.Iterator;
import java.util.List;

/**
 * Specifies a table model containing {@link Entity} objects
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityTableModel}
 */
public interface EntityTableModel<E extends EntityEditModel> extends FilteredModel<Entity>, Refreshable {

  String PREFERENCES_COLUMNS = "columns";
  String PREFERENCES_COLUMN_WIDTH = "width";
  String PREFERENCES_COLUMN_VISIBLE = "visible";
  String PREFERENCES_COLUMN_INDEX = "index";

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
  EntityType<?> getEntityType();

  /**
   * @return the connection provider used by this table model
   */
  EntityConnectionProvider getConnectionProvider();

  /**
   * @return the underlying domain entities
   */
  Entities getEntities();

  /**
   * @return the definition of the underlying entity
   */
  EntityDefinition getEntityDefinition();

  /**
   * Returns the {@link EntityEditModel} associated with this table model
   * @return the edit model associated with this table model
   * @see #setEditModel(EntityEditModel)
   * @see #hasEditModel()
   * @throws IllegalStateException in case no edit model has been associated with this table model
   */
  E getEditModel();

  /**
   * @return true if this {@link EntityTableModel} contains a {@link EntityEditModel}
   */
  boolean hasEditModel();

  /**
   * Associates the given {@link EntityEditModel} with this {@link EntityTableModel}, this enables delete/update
   * functionality via this table model as well as enabling it to
   * react to delete events in the edit model.
   * Throws a RuntimeException in case the edit model has been previously set
   * @param editModel the edit model to associate with this table model
   * @see #deleteSelected()
   * @see #update(java.util.List)
   * @throws IllegalStateException in case an {@link EntityEditModel} has already been associated with this {@link EntityTableModel}
   * @throws IllegalArgumentException in case the given {@link EntityEditModel} is not based on the same entityType as this {@link EntityTableModel}
   */
  void setEditModel(E editModel);

  /**
   * Sets {@code foreignKeyValues} as the search condition values for the given foreign key
   * and refreshes this table model.
   * @param foreignKey the foreign key
   * @param foreignKeyValues the entities to use as condition values
   */
  void setForeignKeyConditionValues(ForeignKey foreignKey, Collection<Entity> foreignKeyValues);

  /**
   * For every entity in this table model, replaces the foreign key instance bearing the primary
   * key with the corresponding entity from {@code foreignKeyValues}, useful when property
   * values have been changed in the referenced entity that must be reflected in the table model.
   * @param foreignKeyEntityType the entityType of the foreign key values
   * @param foreignKeyValues the foreign key entities
   */
  void replaceForeignKeyValues(EntityType<?> foreignKeyEntityType, Collection<Entity> foreignKeyValues);

  /**
   * Adds the given entities to the bottom of this table model.
   * It is recommended to only manually add entities directly to this table model after they have
   * been inserted into the underlying table since otherwise they will disappear during the next table model refresh.
   * @param entities the entities to add
   */
  void addEntities(List<Entity> entities);

  /**
   * Adds the given entities to the bottom of this table model and then, if sorting is enabled, sorts this table model.
   * It is recommended to only manually add entities directly to this table model after they have
   * been inserted into the underlying table since otherwise they will disappear during the next table model refresh.
   * @param entities the entities to add
   */
  void addEntitiesSorted(List<Entity> entities);

  /**
   * Adds the given entities to the top of this table model.
   * It is recommended to only manually add entities directly to this table model after they have
   * been inserted into the underlying table since otherwise they will disappear during the next table model refresh.
   * @param index the index at which to add
   * @param entities the entities to add
   */
  void addEntitiesAt(int index, List<Entity> entities);

  /**
   * Adds the given entities to the top of this table model and then, if sorting is enabled, sorts this table model.
   * It is recommended to only manually add entities directly to this table model after they have
   * been inserted into the underlying table since otherwise they will disappear during the next table model refresh.
   * @param index the index at which to add
   * @param entities the entities to add
   * @see is.codion.common.model.table.TableSortModel#isSortingEnabled()
   */
  void addEntitiesAtSorted(int index, List<Entity> entities);

  /**
   * Replaces the given entities in this table model
   * @param entities the entities to replace
   */
  void replaceEntities(List<Entity> entities);

  /**
   * Refreshes the entities with the given keys by re-selecting them from the underlying database.
   * @param keys the keys of the entities to refresh
   */
  void refreshEntities(List<Key> keys);

  /**
   * @return the {@link EntityTableConditionModel} instance used by this table model
   */
  EntityTableConditionModel getTableConditionModel();

  /**
   * @return true if this table model is editable
   */
  boolean isEditable();

  /**
   * @param editable true if this table model should be editable
   */
  void setEditable(boolean editable);

  /**
   * @return true if this model has an edit model and that edit model allows deletion of records
   * @see #hasEditModel()
   * @see #setEditModel(EntityEditModel)
   */
  boolean isDeleteEnabled();

  /**
   * @return true if this model has no {@link EntityEditModel} or if that edit model is read only
   * @see #hasEditModel()
   * @see #setEditModel(EntityEditModel)
   */
  boolean isReadOnly();

  /**
   * @return true if this model has an edit model and that edit model allows updating of records
   * @see #hasEditModel()
   * @see #setEditModel(EntityEditModel)
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
   * @see EntityDefinition.Builder#colorProvider(ColorProvider)
   */
  Object getBackgroundColor(int row, Attribute<?> attribute);

  /**
   * Returns the maximum number of records to fetch via the underlying query the next time
   * this table model is refreshed, a value of -1 means all records should be fetched
   * @return the fetch count
   */
  int getFetchCount();

  /**
   * Sets the maximum number of records to fetch via the underlying query the next time
   * this table model is refreshed, a value of -1 means all records should be fetched
   * @param fetchCount the fetch count
   */
  void setFetchCount(int fetchCount);

  /**
   * Returns the query row count limit, a value of -1 means no limit.
   * @return the query row count limit
   */
  int getQueryRowCountLimit();

  /**
   * Sets the query row count limit, a value of -1 means no limit.
   * @param queryRowCountLimit the query row count limit
   */
  void setQueryRowCountLimit(int queryRowCountLimit);

  /**
   * Updates the given entities. If the entities are unmodified or the list is empty
   * this method returns silently.
   * @param entities the entities to update
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.model.CancelException in case the user cancels the operation
   * @throws is.codion.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @throws IllegalStateException in case this table model has no edit model or if the edit model does not allow updating
   * @see EntityValidator#validate(Collection, EntityDefinition)
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
  State getQueryConditionRequiredState();

  /**
   * @return true if entities that are deleted via the associated edit model
   * should be automatically removed from this table model
   */
  boolean isRemoveEntitiesOnDelete();

  /**
   * @param removeEntitiesOnDelete true if entities that are deleted via the associated edit model
   * should be automatically removed from this table model
   */
  void setRemoveEntitiesOnDelete(boolean removeEntitiesOnDelete);

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
  Collection<Entity> getEntitiesByKey(Collection<Key> keys);

  /**
   * Sets the selected entities according to the primary keys in {@code primaryKeys}
   * @param keys the primary keys of the entities to select
   */
  void setSelectedByKey(Collection<Key> keys);

  /**
   * Returns an Iterator which iterates through the selected entities
   * @return the iterator used when generating reports
   */
  Iterator<Entity> getSelectedEntitiesIterator();

  /**
   * @param primaryKey the primary key to search by
   * @return the entity with the given primary key from the table model, null if it's not found
   */
  Entity getEntityByKey(Key primaryKey);

  /**
   * @param primaryKey the primary key
   * @return the row index of the entity with the given primary key, -1 if not found
   */
  int indexOf(Key primaryKey);

  /**
   * Saves any user preferences
   */
  void savePreferences();

  /**
   * Arranges the column model so that only the given columns are visible and in the given order
   * @param attributes the column attributes
   */
  void setColumns(Attribute<?>... attributes);

  /**
   * @param delimiter the delimiter
   * @return the table data as a tab delimited string, with column names as a header
   */
  String getTableDataAsDelimitedString(char delimiter);

  /**
   * @return the items in this table model, visible and filtered
   */
  @Override
  List<Entity> getItems();

  /**
   * @return the number of visible rows in this table model
   */
  int getRowCount();

  /**
   * @return the {@link SelectionModel}
   */
  SelectionModel<Entity> getSelectionModel();

  /**
   * @param listener listener notified when a edit model has been set.
   */
  void addEditModelSetListener(EventDataListener<E> listener);

  /**
   * @param listener notified when the selection changes in the underlying selection model
   */
  void addSelectionChangedListener(EventListener listener);

  /**
   * @param listener notified each time this model is refreshed.
   */
  void addRefreshListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeRefreshListener(EventListener listener);

  /**
   * @param refreshOnForeignKeyConditionValuesSet true if this table model should automatically refresh when foreign key condition values are set
   * @see #setForeignKeyConditionValues(ForeignKey, Collection)
   */
  void setRefreshOnForeignKeyConditionValuesSet(boolean refreshOnForeignKeyConditionValuesSet);

  /**
   * @return true if this table model automatically refreshes when foreign key condition values are set
   * @see #setForeignKeyConditionValues(ForeignKey, Collection)
   */
  boolean isRefreshOnForeignKeyConditionValuesSet();
}
