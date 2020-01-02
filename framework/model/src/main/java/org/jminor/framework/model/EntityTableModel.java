/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.event.EventListener;
import org.jminor.common.model.FilteredModel;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.model.table.SelectionModel;
import org.jminor.common.state.State;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

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
   * @return the ID of the entity this table model is based on
   */
  String getEntityId();

  /**
   * @return the connection provider used by this table model
   */
  EntityConnectionProvider getConnectionProvider();

  /**
   * @return the underlying domain model
   */
  Domain getDomain();

  /**
   * @return the definition of the underlying entity
   */
  EntityDefinition getEntityDefinition();

  /**
   * Returns the {@link EntityEditModel} associated with this table model
   * @return the edit model associated with this table model
   * @see #setEditModel(EntityEditModel)
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
   * @throws IllegalArgumentException in case the given {@link EntityEditModel} is not based on the same entityId as this {@link EntityTableModel}
   */
  void setEditModel(E editModel);

  /**
   * Sets {@code foreignKeyValues} as the search condition values for the given foreignKeyProperty
   * and refreshes this table model.
   * @param foreignKeyProperty the ID of the foreign key property
   * @param foreignKeyValues the entities to use as condition values
   */
  void setForeignKeyConditionValues(ForeignKeyProperty foreignKeyProperty, Collection<Entity> foreignKeyValues);

  /**
   * For every entity in this table model, replaces the foreign key instance bearing the primary
   * key with the corresponding entity from {@code foreignKeyValues}, useful when property
   * values have been changed in the referenced entity that must be reflected in the table model.
   * @param foreignKeyEntityId the entity ID of the foreign key values
   * @param foreignKeyValues the foreign key entities
   */
  void replaceForeignKeyValues(String foreignKeyEntityId, Collection<Entity> foreignKeyValues);

  /**
   * Adds the given entities to this table model, it is recommended to only manually add entities
   * directly to this table model after they have been inserted into the underlying table
   * since otherwise they will disappear during the next table model refresh.
   * @param entities the entities to add
   * @param atTop if true then items are added at the top of the table model, else at the bottom
   * @param sortAfterAdding if true and sorting is enabled then the model is sorted after adding
   * @see org.jminor.common.model.table.TableSortModel#isSortingEnabled()
   */
  void addEntities(List<Entity> entities, boolean atTop, boolean sortAfterAdding);

  /**
   * Replaces the given entities in this table model
   * @param entities the entities to replace
   */
  void replaceEntities(Collection<Entity> entities);

  /**
   * Refreshes the entities with the given keys by re-selecting them from the underlying database.
   * @param keys the keys of the entities to refresh
   */
  void refreshEntities(List<Entity.Key> keys);

  /**
   * @return the {@link EntityTableConditionModel} instance used by this table model
   */
  EntityTableConditionModel getConditionModel();

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
   * @return this {@link EntityTableModel} instance
   */
  EntityTableModel<E> setBatchUpdateEnabled(boolean batchUpdateEnabled);

  /**
   * Returns the {@link ColumnSummaryModel} associated with the property identified by {@code propertyId}
   * @param propertyId the ID of the property
   * @return the {@link ColumnSummaryModel} for the given property ID
   */
  ColumnSummaryModel getColumnSummaryModel(String propertyId);

  /**
   * @param row the row for which to retrieve the background color
   * @param property the property for which to retrieve the background color
   * @return an Object representing the background color for this row and property, specified by the row entity
   * @see EntityDefinition.Builder#setColorProvider(Entity.ColorProvider)
   */
  Object getPropertyBackgroundColor(int row, Property property);

  /**
   * @param propertyId the propertyId
   * @return the index of the column representing the given property
   */
  int getPropertyColumnIndex(String propertyId);

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
   * @return this table model
   */
  EntityTableModel<E> setFetchCount(int fetchCount);

  /**
   * Updates the given entities. If the entities are unmodified or the list is empty
   * this method returns silently.
   * @param entities the entities to update
   * @throws DatabaseException in case of a database exception
   * @throws org.jminor.common.model.CancelException in case the user cancels the operation
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @throws IllegalStateException in case this table model has no edit model or if the edit model does not allow updating
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection)
   */
  void update(List<Entity> entities) throws ValidationException, DatabaseException;

  /**
   * Deletes the selected entities
   * @throws DatabaseException in case of a database exception
   * @throws org.jminor.common.model.CancelException in case the user cancels the operation
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
   * @param value true if entities that are deleted via the associated edit model
   * should be automatically removed from this table model
   * @return this {@link EntityTableModel} instance
   */
  EntityTableModel<E> setRemoveEntitiesOnDelete(boolean value);

  /**
   * @return the action performed when entities are inserted via the associated edit model
   */
  InsertAction getInsertAction();

  /**
   * @param insertAction the action to perform when entities are inserted via the associated edit model
   * @return this EntityTableModel instance
   */
  EntityTableModel<E> setInsertAction(InsertAction insertAction);

  /**
   * Finds entities according to the values in {@code keys}
   * @param keys the primary key values to use as condition
   * @return the entities having the primary key values as in {@code keys}
   */
  Collection<Entity> getEntitiesByKey(Collection<Entity.Key> keys);

  /**
   * Sets the selected entities according to the primary keys in {@code primaryKeys}
   * @param keys the primary keys of the entities to select
   */
  void setSelectedByKey(Collection<Entity.Key> keys);

  /**
   * Returns an Iterator which iterates through the selected entities
   * @return the iterator used when generating reports
   */
  Iterator<Entity> getSelectedEntitiesIterator();

  /**
   * @param primaryKey the primary key to search by
   * @return the entity with the given primary key from the table model, null if it's not found
   */
  Entity getEntityByKey(Entity.Key primaryKey);

  /**
   * @param primaryKey the primary key
   * @return the row index of the entity with the given primary key, -1 if not found
   */
  int indexOf(Entity.Key primaryKey);

  /**
   * Saves any user preferences
   */
  void savePreferences();

  /**
   * Arranges the column model so that only the given columns are visible and in the given order
   * @param propertyIds the column identifiers
   */
  void setColumns(String... propertyIds);

  /**
   * @param delimiter the delimiter
   * @return the table data as a tab delimited string, with column names as a header
   */
  String getTableDataAsDelimitedString(char delimiter);

  /**
   * @return the items in this table model, visible and filtered
   */
  @Override
  List<Entity> getAllItems();

  /**
   * @return the number of visible rows in this table model
   */
  int getRowCount();

  /**
   * @return the {@link SelectionModel}
   */
  SelectionModel<Entity> getSelectionModel();

  /**
   * @param listener notified when the selection changes in the underlying selection model
   */
  void addSelectionChangedListener(EventListener listener);

  /**
   * @param value true if this table model should automatically refresh when foreign key condition values are set
   * @see #setForeignKeyConditionValues(ForeignKeyProperty, Collection)
   * @return this {@link EntityTableModel} instance
   */
  EntityTableModel setRefreshOnForeignKeyConditionValuesSet(boolean value);

  /**
   * @return true if this table model automatically refreshes when foreign key condition values are set
   * @see #setForeignKeyConditionValues(ForeignKeyProperty, Collection)
   */
  boolean isRefreshOnForeignKeyConditionValuesSet();
}
