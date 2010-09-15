/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.FilteredTableModel;
import org.jminor.common.model.SortingDirective;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.Color;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Specifies a table model containing Entity objects
 */
public interface EntityTableModel extends FilteredTableModel<Entity, Property>, EntityDataProvider {

  /**
   * Returns the edit model associated with this table model,
   * throws a RuntimeExcption in case no edit model has been associated with this table model
   * @return the edit model associated with this table model
   * @see #setEditModel(EntityEditModel)
   */
  EntityEditModel getEditModel();

  /**
   * @return true if this table model contains a edit model
   */
  boolean hasEditModel();

  /**
   * Associates the given edit model with this table model, this enables delete/update
   * functionality via this table model as well as enabling it to
   * react to delete events in the edit model.
   * Throws a RuntimeException in case the edit model has been previously set
   * @param editModel the edit model to associate with this table model
   * @see #deleteSelected()
   * @see #update(java.util.List)
   */
  void setEditModel(final EntityEditModel editModel);

  /**
   * @param detailModel if set to true then this table model will not run a query unless a query criteria has been specified
   * @see #setQueryCriteriaRequired(boolean)
   * @return this table model instance
   */
  EntityTableModel setDetailModel(final boolean detailModel);

  /**
   * Refreshes this table model according the the given values by finding the first foreign key property
   * referencing the entity identified by <code>referencedEntityID</code> and setting <code>referenceEntities</code>
   * as the criteria values. If no foreign key property is found this method has no effect.
   * @param referencedEntityID the ID of the master entity
   * @param referenceEntities the entities to use as criteria values
   * @see #isDetailModel()
   */
  void setForeignKeySearchValues(final String referencedEntityID, final List<Entity> referenceEntities);

  /**
   * For every entity in this table model, replaces the foreign key instance bearing the primary
   * key with the corresponding entity from <code>foreignKeyValues</code>, useful when property
   * values have been changed in the referenced entity that must be reflected in the table model.
   * @param foreignKeyEntityID the entity ID of the foreign key values
   * @param newForeignKeyValues the foreign key entities
   */
  void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> newForeignKeyValues);

  /**
   * Retrieves the entities identified by the given primary keys and adds them to this table model
   * @param primaryKeys the primary keys
   * @param atFront if true the entities are added to the front
   */
  void addEntitiesByPrimaryKeys(final List<Entity.Key> primaryKeys, final boolean atFront);

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
   * @return this table model instance
   */
  EntityTableModel setQueryConfigurationAllowed(final boolean value);

  /**
   * @return the EntityTableSearcher instance used by this table model
   */
  EntityTableSearchModel getSearchModel();

  /**
   * @return true if this table model has a master model
   */
  boolean isDetailModel();

  /**
   * @return true if this model allows records to be deleted
   */
  boolean isDeleteAllowed();

  /**
   * @return true if this model is read only or if no edit model has been specified.
   * @see #setEditModel(EntityEditModel)
   */
  boolean isReadOnly();

  /**
   * @return true if this model allows records to be deleted
   */
  boolean isUpdateAllowed();

  /**
   * @return true if this model allows multiple entities to be updated at a time
   */
  boolean isBatchUpdateAllowed();

  /**
   * @param batchUpdateAllowed true if this model should allow multiple entities to be updated at a time
   * @return this EntityTableModel instance
   */
  EntityTableModel setBatchUpdateAllowed(final boolean batchUpdateAllowed);

  /**
   * Returns the PropertySummaryModel associated with the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property
   * @return the PropertySummaryModel for the given property ID
   */
  PropertySummaryModel getPropertySummaryModel(final String propertyID);

  /**
   * Returns the PropertySummaryModel associated with the given property
   * @param property the property
   * @return the PropertySummaryModel for the given property
   */
  PropertySummaryModel getPropertySummaryModel(final Property property);

  /**
   * @param row the row for which to retrieve the background color
   * @param columnProperty the column property for which to retrieve the background color
   * @return the background color for this row and property, specified by the row entity
   * @see org.jminor.framework.domain.EntityDefinition#setBackgroundColorProvider(org.jminor.framework.domain.Entity.BackgroundColorProvider)
   * @see org.jminor.framework.client.ui.EntityTableCellRenderer
   */
  Color getPropertyBackgroundColor(final int row, final Property columnProperty);

  /**
   * @return the underlying table column properties
   */
  List<Property> getTableColumnProperties();

  /**
   * @return a String describing the selected/filtered state of this table model
   */
  String getStatusMessage();

  /**
   * Returns the maximum number of records to fetch via the underlying query,
   * by default this returns -1, meaning all records should be fetched
   * @return the fetch count
   */
  int getFetchCount();

  /**
   * Sets the maximum number of records to fetch via the underlying query,
   * a value of -1 means all records should be fetched
   * @param fetchCount the fetch count
   * @return this table model
   */
  EntityTableModel setFetchCount(final int fetchCount);

  /**
   * @return a Map containing all entities which depend on the selected entities,
   * where the keys are entityIDs and the value is an array of entities of that type
   */
  Map<String, Collection<Entity>> getSelectionDependencies();

  /**
   * Updates the given Entities. If the entities are unmodified or the list is empty
   * this method returns silently.
   * @param entities the Entities to update
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection, int)
   */
  void update(final List<Entity> entities) throws CancelException, ValidationException, DbException;

  /**
   * Deletes the selected entities
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   */
  void deleteSelected() throws CancelException, DbException;

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
   * Finds entities according to the values in <code>keys</code>
   * @param keys the primary key values to use as condition
   * @return the entities having the primary key values as in <code>keys</code>
   */
  List<Entity> getEntitiesByPrimaryKeys(final List<Entity.Key> keys);

  /**
   * Sets the selected entities according to the primary keys in <code>primaryKeys</code>
   * @param keys the primary keys of the entities to select
   */
  void setSelectedByPrimaryKeys(final List<Entity.Key> keys);

  /**
   * Finds entities according to the values of <code>propertyValues</code>
   * @param values the property values to use as condition mapped
   * to their respective propertyIDs
   * @return the entities having the exact same property values as in <code>properties</properties>
   */
  Collection<Entity> getEntitiesByPropertyValues(final Map<String, Object> values);

  /**
   * Returns an Iterator which iterates through the selected entities
   * @return the iterator used when generating reports
   * @see #getReportDataSource()
   */
  Iterator<Entity> getSelectedEntitiesIterator();

  /**
   * @return a list containing the primary keys of the selected entities,
   * if none are selected an empty list is returned
   */
  List<Entity.Key> getPrimaryKeysOfSelectedEntities();

  /**
   * Returns an initialized ReportDataWrapper instance, the default implementation returns null.
   * @return an initialized ReportDataWrapper
   * @see #getSelectedEntitiesIterator()
   */
  ReportDataWrapper getReportDataSource();

  /**
   * Sets the report data source to use during report generation
   * @param reportDataSource the data source
   * @return this table model instance
   */
  EntityTableModel setReportDataSource(final ReportDataWrapper reportDataSource);

  /**
   * @param property the property for which to retrieve the values
   * @param selectedOnly if true only values from the selected entities are returned
   * @return the values of <code>property</code> from the entities in the table model
   */
  Collection<Object> getValues(final Property property, final boolean selectedOnly);

  /**
   * @param primaryKey the primary key to search by
   * @return the entity with the given primary key from the table model, null if it's not found
   */
  Entity getEntityByPrimaryKey(final Entity.Key primaryKey);

  /**
   * @param primaryKey the primary key
   * @return the row index of the entity with the given primary key, -1 if not found
   */
  int indexOf(final Entity.Key primaryKey);

  /**
   * @param propertyID the propertyID
   * @param directive the sorting directive
   */
  void setSortingDirective(final String propertyID, final SortingDirective directive);

  /**
   * @param propertyID the propertyID
   * @return the sorting directive assigned to the given property column
   */
  SortingDirective getSortingDirective(final String propertyID);

  /**
   * @return the state used to determine if batch updating of entities should be enabled
   * @see #isBatchUpdateAllowed()
   */
  StateObserver getBatchUpdateAllowedState();
}
