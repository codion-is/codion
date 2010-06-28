/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.FilteredTableModel;
import org.jminor.common.model.State;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.Color;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface EntityTableModel extends FilteredTableModel<Entity> {

  /**
   * @return the state used to determine if updating should be enabled
   * @see #isMultipleUpdateAllowed()
   */
  State stateAllowMultipleUpdate();

  /**
   * @return the state used to determine if deleting should be enabled
   * @see #isDeleteAllowed()
   * @see #setDeleteAllowed(boolean)
   */
  State stateAllowDelete();

  /**
   * @return the ID of the entity this table model represents
   */
  String getEntityID();

  /**
   * @return the EntityDbConnection provider
   */
  EntityDbProvider getDbProvider();

  /**
   * @param propertyID the ID of the property to sort by
   * @param status the sorting status, use TableSorter.DESCENDING, .NOT_SORTED, .ASCENDING
   */
  void setSortingStatus(final String propertyID, final int status);

  /**
   * Returns the edit model associated with this table model,
   * throws a RuntimeExcption in case no edit model has been associated with this table model
   * @return the edit model associated with this table model
   * @see #setEditModel(EntityEditModel)
   */
  EntityEditModel getEditModel();

  /**
   * Associates the given edit model with this table model, this enables delete/update
   * functionality via this table model as well as enabling it to
   * react to delete events in the edit model.
   * Throws a RuntimeException in case the edit model has been previously set
   * @param editModel the edit model to associate with this table model
   * @see #deleteSelected()
   * @see #update(java.util.List)
   */
  void setEditModel(EntityEditModel editModel);

  /**
   * @param detailModel if set to true then this table model will not run a query unless a query criteria has been specified
   * @see #setQueryCriteriaRequired(boolean)
   */
  void setDetailModel(boolean detailModel);

  /**
   * Filters this table model according the the given values by finding the first foreign key property
   * referencing the entity identified by <code>referencedEntityID</code> and setting <code>referenceEntities</code>
   * as the criteria values. If no foreign key property is found this method has no effect.
   * @param referencedEntityID the ID of the master entity
   * @param referenceEntities the entities to use as criteria values
   * @see #isDetailModel()
   */
  public void searchByForeignKeyValues(final String referencedEntityID, final List<Entity> referenceEntities);

  /**
   * Retrieves the entities identified by the given primary keys and adds them to this table model
   * @param primaryKeys the primary keys
   * @param atFront if true the entities are added to the front
   */
  void addEntitiesByPrimaryKeys(List<Entity.Key> primaryKeys, boolean atFront);

  /**
   * Replaces the given entities in this table model
   * @param entities the entities to replace
   */
  void replaceEntities(List<Entity> entities);

  /**
   * @return true if the underlying query should be configurable
   */
  boolean isQueryConfigurationAllowed();

  /**
   * Specifies whether or not the underlying query should be configurable
   * @param value the value
   */
  void setQueryConfigurationAllowed(boolean value);

  /**
   * @return the EntityTableSearcher instance used by this table model
   */
  EntityTableSearchModel getSearchModel();

  /**
   * @return true if this table model should not run a query unless a query criteria has been specified
   */
  boolean isDetailModel();

  /**
   * @param value true if this model should allow records to be deleted
   */
  void setDeleteAllowed(final boolean value);

  /**
   * @return true if this model should allow records to be deleted
   */
  boolean isDeleteAllowed();

  /**
   * @return true if this model is read only or if no edit model has been specified.
   * by default this returns the isReadOnly value of the underlying entity
   * @see #setEditModel(EntityEditModel)
   */
  boolean isReadOnly();

  /**
   * @return true if this model allows multiple entities to be updated at a time
   */
  boolean isMultipleUpdateAllowed();

  /**
   * Returns the PropertySummaryModel associated with the given property
   * @param property the property
   * @return the PropertySummaryModel for the given property
   */
  PropertySummaryModel getPropertySummaryModel(Property property);

  /**
   * Returns the property the column at the given index is based on
   * @param columnIndex the column index
   * @return the column property
   */
  Property getColumnProperty(int columnIndex);

  /**
   * @param row the row for which to retrieve the background color
   * @return the background color for this row, specified by the row entity
   * @see org.jminor.framework.domain.Entity.Proxy#getBackgroundColor(org.jminor.framework.domain.Entity)
   * @see org.jminor.framework.client.ui.EntityTableCellRenderer
   */
  Color getRowBackgroundColor(int row);

  /**
   * @return the underlying table column properties
   */
  List<Property> getTableColumnProperties();

  /**
   * @return a String describing the selected/filtered state of this table model
   */
  String getStatusMessage();

  /**
   * @return a Map containing all entities which depend on the selected entities,
   * where the keys are entityIDs and the value is an array of entities of that type
   */
  Map<String, List<Entity>> getSelectionDependencies();

  void update(List<Entity> entities) throws CancelException, ValidationException, DbException;

  void deleteSelected() throws CancelException, DbException;

  /**
   * @param value if set to true then all underlying entities are shown
   * when no criteria is applied, which can be problematic in the case of huge datasets.
   */
  void setQueryCriteriaRequired(boolean value);

  /**
   * Returns the PropertySummaryModel associated with the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property
   * @return the PropertySummaryModel for the given property ID
   */
  PropertySummaryModel getPropertySummaryModel(String propertyID);

  /**
   * Finds entities according to the values in <code>keys</code>
   * @param keys the primary key values to use as condition
   * @return the entities having the primary key values as in <code>keys</code>
   */
  List<Entity> getEntitiesByPrimaryKeys(List<Entity.Key> keys);

  /**
   * Sets the selected entities according to the primary keys in <code>primaryKeys</code>
   * @param keys the primary keys of the entities to select
   */
  void setSelectedByPrimaryKeys(List<Entity.Key> keys);

  /**
   * Finds entities according to the values of <code>propertyValues</code>
   * @param values the property values to use as condition mapped
   * to their respective propertyIDs
   * @return the entities having the exact same property values as in <code>properties</properties>
   */
  Collection<Entity> getEntitiesByPropertyValues(Map<String, Object> values);

  /**
   * Returns an Iterator which iterates through the selected entities
   * @return the iterator used when generating reports
   * @see #getReportDataSource()
   */
  Iterator<Entity> getSelectedEntitiesIterator();

  /**
   * Returns an initialized ReportDataWrapper instance, the default implementation returns null.
   * @return an initialized ReportDataWrapper
   * @see #getSelectedEntitiesIterator()
   */
  ReportDataWrapper getReportDataSource();

  /**
   * @param property the property for which to retrieve the values
   * @param selectedOnly if true only values from the selected entities are returned
   * @return the values of <code>property</code> from the entities in the table model
   */
  public Collection<Object> getValues(final Property property, final boolean selectedOnly);

  /**
   * @param primaryKey the primary key to search by
   * @return the entity with the given primary key from the table model, null if it's not found
   */
  Entity getEntityByPrimaryKey(Entity.Key primaryKey);
}
