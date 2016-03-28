/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.swing.common.model.table.FilteredTableModel;
import org.jminor.swing.common.model.table.SortingDirective;

import java.awt.Color;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Specifies a table model containing {@link Entity} objects
 */
public interface EntityTableModel extends FilteredTableModel<Entity, Property>, EntityDataProvider {

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
    ADD_BOTTOM
  }

  /**
   * Returns the {@link EntityEditModel} associated with this table model
   * @return the edit model associated with this table model
   * @see #setEditModel(EntityEditModel)
   * @throws IllegalStateException in case no edit model has been associated with this table model
   */
  EntityEditModel getEditModel();

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
   * @throws IllegalArgumentException in case the given {@link EntityEditModel} is not based on the same entityID as this {@link EntityTableModel}
   */
  void setEditModel(final EntityEditModel editModel);

  /**
   * Sets <code>foreignKeyValues</code> as the search criteria values for the given foreignKeyProperty
   * and refreshes this table model.
   * @param foreignKeyProperty the ID of the foreign key property
   * @param foreignKeyValues the entities to use as criteria values
   */
  void setForeignKeyCriteriaValues(final Property.ForeignKeyProperty foreignKeyProperty, final Collection<Entity> foreignKeyValues);

  /**
   * For every entity in this table model, replaces the foreign key instance bearing the primary
   * key with the corresponding entity from <code>foreignKeyValues</code>, useful when property
   * values have been changed in the referenced entity that must be reflected in the table model.
   * @param foreignKeyEntityID the entity ID of the foreign key values
   * @param foreignKeyValues the foreign key entities
   */
  void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> foreignKeyValues);

  /**
   * Adds the given entities to this table model, it is recommended to only add entities
   * directly to this table model after they have been inserted into the underlying table
   * since otherwise they will disappear during the next table model refresh.
   * @param entities the entities to add
   * @param atTop if true the entities are added to the top of this table model, otherwise at the bottom
   */
  void addEntities(final List<Entity> entities, final boolean atTop);

  /**
   * Replaces the given entities in this table model
   * @param entities the entities to replace
   */
  void replaceEntities(final Collection<Entity> entities);

  /**
   * @return true if the underlying query should be configurable
   */
  boolean isQueryConfigurationAllowed();

  /**
   * Specifies whether or not the underlying query should be configurable
   * @param value the value
   * @return this {@link EntityTableModel} instance
   */
  EntityTableModel setQueryConfigurationAllowed(final boolean value);

  /**
   * @return the {@link EntityTableCriteriaModel} instance used by this table model
   */
  EntityTableCriteriaModel getCriteriaModel();

  /**
   * @return true if this model has an edit model and that edit model allows deletion of records
   * @see #hasEditModel()
   * @see #setEditModel(EntityEditModel)
   */
  boolean isDeleteAllowed();

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
  boolean isUpdateAllowed();

  /**
   * @return true if this model allows multiple entities to be updated at a time
   */
  boolean isBatchUpdateAllowed();

  /**
   * @param batchUpdateAllowed true if this model should allow multiple entities to be updated at a time
   * @return this {@link EntityTableModel} instance
   */
  EntityTableModel setBatchUpdateAllowed(final boolean batchUpdateAllowed);

  /**
   * Returns the {@link ColumnSummaryModel} associated with the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property
   * @return the {@link ColumnSummaryModel} for the given property ID
   */
  ColumnSummaryModel getColumnSummaryModel(final String propertyID);

  /**
   * @param row the row for which to retrieve the background color
   * @param columnProperty the column property for which to retrieve the background color
   * @return the background color for this row and property, specified by the row entity
   * @see org.jminor.framework.domain.Entity.Definition#setBackgroundColorProvider(org.jminor.framework.domain.Entity.BackgroundColorProvider)
   * @see org.jminor.swing.framework.ui.EntityTableCellRenderers
   */
  Color getPropertyBackgroundColor(final int row, final Property columnProperty);

  /**
   * @param propertyID the propertyID
   * @return the index of the column representing the given property
   */
  int getPropertyColumnIndex(final String propertyID);

  /**
   * @return a String describing the selected/filtered state of this table model
   */
  String getStatusMessage();

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
  EntityTableModel setFetchCount(final int fetchCount);

  /**
   * Updates the given entities. If the entities are unmodified or the list is empty
   * this method returns silently.
   * @param entities the Entities to update
   * @throws DatabaseException in case of a database exception
   * @throws org.jminor.common.model.CancelException in case the user cancels the operation
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @throws IllegalStateException in case this table model has no edit model or if the edit model does not allow updating
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection)
   */
  void update(final List<Entity> entities) throws ValidationException, DatabaseException;

  /**
   * Deletes the selected entities
   * @throws DatabaseException in case of a database exception
   * @throws org.jminor.common.model.CancelException in case the user cancels the operation
   * @throws IllegalStateException in case this table model has no edit model or if the edit model does not allow deleting
   */
  void deleteSelected() throws DatabaseException;

  /**
   * @return whether to show all underlying entities when no criteria is applied.
   */
  boolean isQueryCriteriaRequired();

  /**
   * @param value if set to true then all underlying entities are shown
   * when no criteria is applied, which can be problematic in the case of huge datasets.
   * @return this table model instance
   */
  EntityTableModel setQueryCriteriaRequired(final boolean value);

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
  EntityTableModel setRemoveEntitiesOnDelete(final boolean value);

  /**
   * @return the action performed when entities are inserted via the associated edit model
   */
  InsertAction getInsertAction();

  /**
   * @param insertAction the action to perform when entities are inserted via the associated edit model
   * @return this EntityTableModel instance
   */
  EntityTableModel setInsertAction(final InsertAction insertAction);

  /**
   * Finds entities according to the values in <code>keys</code>
   * @param keys the primary key values to use as condition
   * @return the entities having the primary key values as in <code>keys</code>
   */
  Collection<Entity> getEntitiesByKey(final Collection<Entity.Key> keys);

  /**
   * Sets the selected entities according to the primary keys in <code>primaryKeys</code>
   * @param keys the primary keys of the entities to select
   */
  void setSelectedByKey(final Collection<Entity.Key> keys);

  /**
   * Finds entities according to the values of propertyValues
   * @param values the property values to use as condition mapped
   * to their respective propertyIDs
   * @return the entities having the exact same property values as in properties
   */
  Collection<Entity> getEntitiesByPropertyValue(final Map<String, Object> values);

  /**
   * Returns an Iterator which iterates through the selected entities
   * @return the iterator used when generating reports
   */
  Iterator<Entity> getSelectedEntitiesIterator();

  /**
   * @param primaryKey the primary key to search by
   * @return the entity with the given primary key from the table model, null if it's not found
   */
  Entity getEntityByKey(final Entity.Key primaryKey);

  /**
   * @param primaryKey the primary key
   * @return the row index of the entity with the given primary key, -1 if not found
   */
  int indexOf(final Entity.Key primaryKey);

  /**
   * @param propertyID the propertyID
   * @param directive the sorting directive
   * @param addColumnToSort if false then the sorting state is cleared, otherwise
   * this column is added to the sorted column set according to <code>getSortingPriority()</code>
   */
  void setSortingDirective(final String propertyID, final SortingDirective directive,
                           final boolean addColumnToSort);

  /**
   * @param propertyID the propertyID
   * @return the sorting directive assigned to the given property column
   */
  SortingDirective getSortingDirective(final String propertyID);

  /**
   * Saves any user preferences
   */
  void savePreferences();

  /**
   * Arranges the column model so that only the given columns are visible and in the given order
   * @param propertyIDs the column identifiers
   */
  void setColumns(final String... propertyIDs);

  /**
   * @param delimiter the delimiter
   * @return the table data as a tab delimited string, with column names as a header
   */
  String getTableDataAsDelimitedString(final char delimiter);
}
