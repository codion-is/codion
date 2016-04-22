/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.DefaultEntityTableCriteriaModel;
import org.jminor.framework.model.DefaultPropertyFilterModelProvider;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.EntityTableCriteriaModel;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.swing.common.model.table.AbstractFilteredTableModel;
import org.jminor.swing.common.model.table.AbstractTableSortModel;
import org.jminor.swing.common.model.table.FilteredTableColumnModel;
import org.jminor.swing.common.model.table.FilteredTableModel;
import org.jminor.swing.common.model.table.TableSortModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A TableModel implementation for displaying and working with entities.
 *
 * <pre>
 * String entityID = "some.entity";
 * String clientTypeID = "JavadocDemo";
 * User user = new User("scott", "tiger");
 *
 * EntityConnectionProvider connectionProvider = EntityConnectionProviders.createConnectionProvider(user, clientTypeID);
 *
 * EntityTableModel tableModel = new DefaultEntityTableModel(entityID, connectionProvider);
 *
 * EntityEditModel editModel = ...;
 *
 * tableModel.setEditModel(editModel);
 *
 * EntityTablePanel panel = new EntityTablePanel(model);
 * </pre>
 */
public class DefaultEntityTableModel extends AbstractFilteredTableModel<Entity, Property>
        implements EntityTableModel, FilteredTableModel<Entity, Property> {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityTableModel.class);

  private static final String PREFERENCES_COLUMNS = "columns";
  private static final String PREFERENCES_COLUMN_WIDTH = "width";
  private static final String PREFERENCES_COLUMN_VISIBLE = "visible";
  private static final String PREFERENCES_COLUMN_INDEX = "index";

  /**
   * The entity ID
   */
  private final String entityID;

  /**
   * The EntityConnection provider
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * The edit model to use when updating/deleting entities
   */
  private EntityEditModel editModel;

  /**
   * The criteria model
   */
  private final EntityTableCriteriaModel criteriaModel;

  /**
   * the maximum number of records to fetch via the underlying query, -1 meaning all records should be fetched
   */
  private int fetchCount = -1;

  /**
   * True if the underlying query should be configurable by the user
   */
  private boolean queryConfigurationAllowed = true;

  /**
   * If true then querying should be disabled if no criteria is specified
   */
  private boolean queryCriteriaRequired = false;

  /**
   * If true then items deleted via the edit model are removed from this table model
   */
  private boolean removeEntitiesOnDelete = true;

  /**
   * The action to perform when entities are inserted via the associated edit model
   */
  private InsertAction insertAction = InsertAction.ADD_TOP;

  /**
   * Indicates if multiple entities can be updated at a time
   */
  private boolean batchUpdateAllowed = true;

  /**
   * Instantiates a new DefaultEntityTableModel with default column and criteria models.
   * @param entityID the entity ID
   * @param connectionProvider the db provider
   */
  public DefaultEntityTableModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(entityID, connectionProvider, new DefaultEntityTableSortModel(entityID),
            new DefaultEntityTableCriteriaModel(entityID, connectionProvider,
                    new DefaultPropertyFilterModelProvider(), new SwingPropertyCriteriaModelProvider()));
  }

  /**
   * Instantiates a new DefaultEntityTableModel.
   * @param entityID the entity ID
   * @param connectionProvider the db provider
   * @param criteriaModel the criteria model
   * @param sortModel the sort model
   * @throws IllegalArgumentException if <code>criteriaModel</code> is null or the criteria model entityID
   * does not match the one supplied as parameter
   */
  public DefaultEntityTableModel(final String entityID, final EntityConnectionProvider connectionProvider,
                                 final TableSortModel<Entity, Property> sortModel, final EntityTableCriteriaModel criteriaModel) {
    super(sortModel, Util.rejectNullValue(criteriaModel, "criteriaModel").getPropertyFilterModels());
    if (!criteriaModel.getEntityID().equals(entityID)) {
      throw new IllegalArgumentException("Entity ID mismatch, criteriaModel: " + criteriaModel.getEntityID()
              + ", tableModel: " + entityID);
    }
    this.entityID = entityID;
    this.connectionProvider = connectionProvider;
    this.criteriaModel = criteriaModel;
    bindEventsInternal();
    applyPreferences();
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final void setEditModel(final EntityEditModel editModel) {
    Util.rejectNullValue(editModel, "editModel");
    if (this.editModel != null) {
      throw new IllegalStateException("Edit model has already been set for table model: " + this);
    }
    if (!editModel.getEntityID().equals(entityID)) {
      throw new IllegalArgumentException("Entity ID mismatch, editModel: " + editModel.getEntityID() + ", tableModel: " + entityID);
    }
    this.editModel = editModel;
    bindEditModelEventsInternal();
    bindEditModelEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean hasEditModel() {
    return this.editModel != null;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isQueryConfigurationAllowed() {
    return queryConfigurationAllowed;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel setQueryConfigurationAllowed(final boolean value) {
    this.queryConfigurationAllowed = value;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final int getFetchCount() {
    return fetchCount;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isQueryCriteriaRequired() {
    return queryCriteriaRequired;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel setQueryCriteriaRequired(final boolean value) {
    this.queryCriteriaRequired = value;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public InsertAction getInsertAction() {
    return insertAction;
  }

  /** {@inheritDoc} */
  @Override
  public EntityTableModel setInsertAction(final InsertAction insertAction) {
    Util.rejectNullValue(insertAction, "insertAction");
    this.insertAction = insertAction;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isRemoveEntitiesOnDelete() {
    return removeEntitiesOnDelete;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel setRemoveEntitiesOnDelete(final boolean value) {
    this.removeEntitiesOnDelete = value;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableCriteriaModel getCriteriaModel() {
    return criteriaModel;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel getEditModel() {
    if (editModel == null) {
      throw new IllegalStateException("No edit model has been set for table model: " + this);
    }
    return editModel;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isBatchUpdateAllowed() {
    return batchUpdateAllowed;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel setBatchUpdateAllowed(final boolean batchUpdateAllowed) {
    this.batchUpdateAllowed = batchUpdateAllowed;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isDeleteAllowed() {
    return editModel != null && editModel.isDeleteAllowed();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isUpdateAllowed() {
    return editModel != null && editModel.isUpdateAllowed();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isReadOnly() {
    return editModel == null || editModel.isReadOnly();
  }

  /** {@inheritDoc} */
  @Override
  public final int getPropertyColumnIndex(final String propertyID) {
    return getColumnModel().getColumnIndex(Entities.getProperty(getEntityID(), propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public final Object getValueAt(final int rowIndex, final int columnIndex) {
    final Property property = getColumnModel().getColumnIdentifier(columnIndex);
    final Entity entity = getItemAt(rowIndex);

    return getValue(entity, property);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isCellEditable(final int rowIndex, final int columnIndex) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final void setValueAt(final Object value, final int rowIndex, final int columnIndex) {
    throw new UnsupportedOperationException("setValueAt is not supported");
  }

  /** {@inheritDoc} */
  @Override
  public Color getPropertyBackgroundColor(final int row, final Property property) {
    return (Color) getItemAt(row).getBackgroundColor(property);
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getEntityByKey(final Entity.Key primaryKey) {
    for (final Entity entity : getVisibleItems()) {
      if (entity.getKey().equals(primaryKey)) {
        return entity;
      }
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final int indexOf(final Entity.Key primaryKey) {
    return indexOf(getEntityByKey(primaryKey));
  }

  /** {@inheritDoc} */
  @Override
  public final String getStatusMessage() {
    final int filteredItemCount = getFilteredItemCount();

    return Integer.toString(getRowCount()) + " (" + Integer.toString(getSelectionModel().getSelectionCount()) + " " +
            FrameworkMessages.get(FrameworkMessages.SELECTED) + (filteredItemCount > 0 ? ", " +
            filteredItemCount + " " + FrameworkMessages.get(FrameworkMessages.HIDDEN) + ")" : ")");
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntities(final List<Entity> entities, final boolean atTop) {
    addItems(entities, atTop);
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceEntities(final Collection<Entity> entities) {
    replaceEntitiesByKey(EntityUtil.mapToKey(entities));
  }

  /** {@inheritDoc} */
  @Override
  public void setForeignKeyCriteriaValues(final Property.ForeignKeyProperty foreignKeyProperty, final Collection<Entity> foreignKeyValues) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    if (criteriaModel.setCriteriaValues(foreignKeyProperty.getPropertyID(), foreignKeyValues)) {
      refresh();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> foreignKeyValues) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(this.entityID, foreignKeyEntityID);
    boolean changed = false;
    for (final Entity entity : getAllItems()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
        for (final Entity foreignKeyValue : foreignKeyValues) {
          final Entity currentForeignKeyValue = entity.getForeignKey(foreignKeyProperty.getPropertyID());
          if (currentForeignKeyValue != null && currentForeignKeyValue.equals(foreignKeyValue)) {
            currentForeignKeyValue.setAs(foreignKeyValue);
            changed = true;
          }
        }
      }
    }
    if (changed) {
      fireTableChanged(new TableModelEvent(this, 0, getRowCount() - 1));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedByKey(final Collection<Entity.Key> keys) {
    final List<Entity.Key> keyList = new ArrayList<>(keys);
    final List<Integer> indexes = new ArrayList<>();
    for (final Entity visibleEntity : getVisibleItems()) {
      final int index = keyList.indexOf(visibleEntity.getKey());
      if (index >= 0) {
        indexes.add(indexOf(visibleEntity));
        keyList.remove(index);
        if (keyList.isEmpty()) {
          break;
        }
      }
    }

    getSelectionModel().setSelectedIndexes(indexes);
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getEntitiesByKey(final Collection<Entity.Key> keys) {
    final List<Entity> entities = new ArrayList<>();
    for (final Entity entity : getAllItems()) {
      for (final Entity.Key key : keys) {
        if (entity.getKey().equals(key)) {
          entities.add(entity);
          break;
        }
      }
    }

    return entities;
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getEntitiesByPropertyValue(final Map<String, Object> values) {
    final List<Entity> entities = new ArrayList<>();
    for (final Entity entity : getAllItems()) {
      boolean equal = true;
      for (final Map.Entry<String, Object> entries : values.entrySet()) {
        final String propertyID = entries.getKey();
        if (!entity.get(propertyID).equals(entries.getValue())) {
          equal = false;
          break;
        }
      }
      if (equal) {
        entities.add(entity);
      }
    }

    return entities;
  }

  /** {@inheritDoc} */
  @Override
  public final void deleteSelected() throws DatabaseException {
    if (!isDeleteAllowed()) {
      throw new IllegalStateException("Deleting is not allowed via this table model");
    }
    editModel.delete(getSelectionModel().getSelectedItems());
  }

  /** {@inheritDoc} */
  @Override
  public final void update(final List<Entity> entities) throws ValidationException, DatabaseException {
    Util.rejectNullValue(entities, "entities");
    if (!isUpdateAllowed()) {
      throw new IllegalStateException("Updating is not allowed via this table model");
    }
    if (entities.size() > 1 && !batchUpdateAllowed) {
      throw new IllegalStateException("Batch update of entities is not allowed!");
    }
    editModel.update(entities);
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnSummaryModel getColumnSummaryModel(final String propertyID) {
    return getColumnSummaryModel(Entities.getProperty(entityID, propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public final Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectionModel().getSelectedItems().iterator();
  }

  /** {@inheritDoc} */
  @Override
  public void setColumns(final String... propertyIDs) {
    final List<Property> properties = Entities.getProperties(getEntityID(), propertyIDs);
    getColumnModel().setColumns(properties.toArray(new Property[properties.size()]));
  }

  /** {@inheritDoc} */
  @Override
  public final void savePreferences() {
    if (Configuration.getBooleanValue(Configuration.USE_CLIENT_PREFERENCES)) {
      try {
        Util.putUserPreference(getUserPreferencesKey(), createPreferences().toString());
      }
      catch (final Exception e) {
        LOG.error("Error while saving preferences", e);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final String getTableDataAsDelimitedString(final char delimiter) {
    final List<String> headerValues = new ArrayList<>();
    final List<Property> properties = new ArrayList<>();
    final Enumeration<TableColumn> columnEnumeration = getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final Property property = (Property) columnEnumeration.nextElement().getIdentifier();
      properties.add(property);
      headerValues.add(property.getCaption());
    }

    final String[][] header = {headerValues.toArray(new String[headerValues.size()])};

    return Util.getDelimitedString(header, EntityUtil.getStringValueArray(properties,
            getSelectionModel().isSelectionEmpty() ? getVisibleItems() : getSelectionModel().getSelectedItems()),
            String.valueOf(delimiter));
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectionChangedListener(final EventListener listener) {
    getSelectionModel().addSelectionChangedListener(listener);
  }

  @Override
  protected final ColumnSummaryModel.ColumnValueProvider createColumnValueProvider(final Property property) {
    return new DefaultColumnValueProvider(property, this, property.getFormat());
  }

  /** {@inheritDoc} */
  @Override
  protected final void doRefresh() {
    try {
      LOG.debug("{} refreshing", this);
      final List<Entity> queryResult = performQuery(criteriaModel.getTableCriteria());
      clear();
      addItems(queryResult, false);
      criteriaModel.rememberCurrentCriteriaState();
    }
    finally {
      LOG.debug("{} refreshing done", this);
    }
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed,
   * using the order by clause returned by {@link #getOrderByClause()}
   * @param criteria a criteria
   * @return entities selected from the database according the the query criteria.
   * @see EntityTableCriteriaModel#getTableCriteria()
   */
  protected List<Entity> performQuery(final Criteria<Property.ColumnProperty> criteria) {
    if (criteria == null && queryCriteriaRequired) {
      return new ArrayList<>();
    }

    try {
      return connectionProvider.getConnection().selectMany(EntityCriteriaUtil.selectCriteria(entityID, criteria,
              getOrderByClause(), fetchCount));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the value to display in a table cell for the given property of the given entity.
   * Note that this method is responsible for providing a "human readable" version of the value,
   * such as the caption for value list properties and string versions of foreign key values.
   * @param entity the entity
   * @param property the property
   * @return the value of the given property for the given entity for display
   * @throws IllegalArgumentException in case entity or property is null
   */
  protected Object getValue(final Entity entity, final Property property) {
    Util.rejectNullValue(entity, "entity");
    Util.rejectNullValue(property, "property");
    if (property instanceof Property.ValueListProperty || property instanceof Property.ForeignKeyProperty) {
      return entity.getAsString(property);
    }

    return entity.get(property);
  }

  /** {@inheritDoc} */
  @Override
  protected final String getSearchValueAt(final int rowIndex, final TableColumn column) {
    return getItemAt(rowIndex).getAsString((Property) column.getIdentifier());
  }

  /**
   * The order by clause to use when selecting the data for this model,
   * by default the order by clause defined for the underlying entity
   * @return the order by clause
   * @see Entities#getOrderByClause(String)
   */
  protected String getOrderByClause() {
    return Entities.getOrderByClause(entityID);
  }

  @SuppressWarnings({"UnusedDeclaration"})
  protected void handleDelete(final EntityEditModel.DeleteEvent event) {/*Provided for subclasses*/}

  /**
   * Override to bind events using the edit model, called after the edit model has been set
   */
  protected void bindEditModelEvents() {/*Provided for subclasses*/}

  /**
   * Returns the key used to identify user preferences for this table model, that is column positions, widths and such.
   * The default implementation is:
   * <pre>
   * {@code
   * return getClass().getSimpleName() + "-" + getEntityID();
   * }
   * </pre>
   * Override in case this key is not unique.
   * @return the key used to identify user preferences for this table model
   */
  protected String getUserPreferencesKey() {
    return getClass().getSimpleName() + "-" + getEntityID();
  }

  /**
   * Clears any user preferences saved for this table model
   */
  final void clearPreferences() {
    Util.removeUserPreference(getUserPreferencesKey());
  }

  private void bindEventsInternal() {
    getColumnModel().addColumnHiddenListener(new EventInfoListener<Property>() {
      @Override
      public void eventOccurred(final Property info) {
        handleColumnHidden(info);
      }
    });
    criteriaModel.addSimpleCriteriaListener(new EventListener() {
      @Override
      public void eventOccurred() {
        refresh();
      }
    });
  }

  private void bindEditModelEventsInternal() {
    editModel.addAfterInsertListener(new EventInfoListener<EntityEditModel.InsertEvent>() {
      @Override
      public void eventOccurred(final EntityEditModel.InsertEvent info) {
        handleInsert(info);
      }
    });
    editModel.addAfterUpdateListener(new EventInfoListener<EntityEditModel.UpdateEvent>() {
      @Override
      public void eventOccurred(final EntityEditModel.UpdateEvent info) {
        handleUpdate(info);
      }
    });
    editModel.addAfterDeleteListener(new EventInfoListener<EntityEditModel.DeleteEvent>() {
      @Override
      public void eventOccurred(final EntityEditModel.DeleteEvent info) {
        handleDeleteInternal(info);
      }
    });
    editModel.addAfterRefreshListener(new EventListener() {
      @Override
      public void eventOccurred() {
        refresh();
      }
    });
    editModel.addEntitySetListener(new EventInfoListener<Entity>() {
      @Override
      public void eventOccurred(final Entity info) {
        if (info == null && !getSelectionModel().isSelectionEmpty()) {
          getSelectionModel().clearSelection();
        }
      }
    });
    getSelectionModel().addSelectedIndexListener(new EventListener() {
      @Override
      public void eventOccurred() {
        final Entity itemToSelect = getSelectionModel().isSelectionEmpty() ? null : getSelectionModel().getSelectedItem();
        editModel.setEntity(itemToSelect);
      }
    });

    addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(final TableModelEvent e) {
        //if the selected record is being updated via the table model refresh the one in the edit model
        if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == getSelectionModel().getSelectedIndex()) {
          editModel.setEntity(getSelectionModel().getSelectedItem());
        }
      }
    });
  }

  private void handleInsert(final EntityEditModel.InsertEvent insertEvent) {
    getSelectionModel().clearSelection();
    if (!insertAction.equals(InsertAction.DO_NOTHING)) {
      addEntities(insertEvent.getInsertedEntities(), insertAction.equals(InsertAction.ADD_TOP));
    }
  }

  private void handleUpdate(final EntityEditModel.UpdateEvent updateEvent) {
    replaceEntitiesByKey(new HashMap<>(updateEvent.getUpdatedEntities()));
  }

  /**
   * Replace the entities identified by the Entity.Key map keys with their respective value
   * @param entityMap the entities to replace mapped to the corresponding primary key found in this table model
   */
  private void replaceEntitiesByKey(final Map<Entity.Key, Entity> entityMap) {
    for (final Entity entity : getAllItems()) {
      final Iterator<Map.Entry<Entity.Key, Entity>> mapIterator = entityMap.entrySet().iterator();
      while (mapIterator.hasNext()) {
        final Map.Entry<Entity.Key, Entity> entry = mapIterator.next();
        if (entity.getKey().equals(entry.getKey())) {
          mapIterator.remove();
          entity.setAs(entry.getValue());
          final int index = indexOf(entity);
          if (index >= 0) {
            fireTableRowsUpdated(index, index);
          }
        }
      }
      if (entityMap.isEmpty()) {
        break;
      }
    }
    filterContents();
  }

  private void handleDeleteInternal(final EntityEditModel.DeleteEvent deleteEvent) {
    if (removeEntitiesOnDelete) {
      removeItems(deleteEvent.getDeletedEntities());
    }
    handleDelete(deleteEvent);
  }

  private void handleColumnHidden(final Property property) {
    //disable the criteria model for the column to be hidden, to prevent confusion
    criteriaModel.setEnabled(property.getPropertyID(), false);
  }

  private org.json.JSONObject createPreferences() throws Exception {
    final org.json.JSONObject preferencesRoot = new org.json.JSONObject();
    preferencesRoot.put(PREFERENCES_COLUMNS, createColumnPreferences());

    return preferencesRoot;
  }

  private org.json.JSONObject createColumnPreferences() throws Exception {
    final org.json.JSONObject columnPreferencesRoot = new org.json.JSONObject();
    for (final TableColumn column : getColumnModel().getAllColumns()) {
      final Property property = (Property) column.getIdentifier();
      final org.json.JSONObject columnObject = new org.json.JSONObject();
      final boolean visible = getColumnModel().isColumnVisible(property);
      columnObject.put(PREFERENCES_COLUMN_WIDTH, column.getWidth());
      columnObject.put(PREFERENCES_COLUMN_VISIBLE, visible);
      columnObject.put(PREFERENCES_COLUMN_INDEX, visible ? getColumnModel().getColumnIndex(property) : -1);
      columnPreferencesRoot.put(property.getPropertyID(), columnObject);
    }

    return columnPreferencesRoot;
  }

  private void applyPreferences() {
    if (Configuration.getBooleanValue(Configuration.USE_CLIENT_PREFERENCES)) {
      final String preferencesString = Util.getUserPreference(getUserPreferencesKey(), "");
      try {
        if (preferencesString.length() > 0) {
          applyColumnPreferences(new org.json.JSONObject(preferencesString).getJSONObject(PREFERENCES_COLUMNS));
        }
      }
      catch (final Exception e) {
        LOG.error("Error while applying preferences: " + preferencesString, e);
      }
    }
  }

  private void applyColumnPreferences(final org.json.JSONObject preferences) {
    final FilteredTableColumnModel<Property> columnModel = getColumnModel();
    final List<TableColumn> allColumns = Collections.list(columnModel.getColumns());
    for (final TableColumn column : allColumns) {
      final Property property = (Property) column.getIdentifier();
      if (columnModel.containsColumn(property)) {
        try {
          final org.json.JSONObject columnPreferences = preferences.getJSONObject(property.getPropertyID());
          column.setPreferredWidth(columnPreferences.getInt(PREFERENCES_COLUMN_WIDTH));
          if (columnPreferences.getBoolean(PREFERENCES_COLUMN_VISIBLE)) {
            final int index = Math.min(columnModel.getColumnCount() - 1, columnPreferences.getInt(PREFERENCES_COLUMN_INDEX));
            columnModel.moveColumn(getColumnModel().getColumnIndex(column.getIdentifier()), index);
          }
          else {
            columnModel.setColumnVisible((Property) column.getIdentifier(), false);
          }
        }
        catch (final Exception e) {
          LOG.info("Property preferences not found: " + property, e);
        }
      }
    }
  }

  private static List<TableColumn> initializeColumns(final String entityID) {
    int modelIndex = 0;
    final List<Property> visibleProperties = Entities.getVisibleProperties(entityID);
    if (visibleProperties.isEmpty()) {
      throw new IllegalStateException("No visible properties defined for entity: " + entityID);
    }
    final List<TableColumn> columns = new ArrayList<>(visibleProperties.size());
    for (final Property property : visibleProperties) {
      final TableColumn column = new TableColumn(modelIndex++);
      column.setIdentifier(property);
      column.setHeaderValue(property.getCaption());
      if (property.getPreferredColumnWidth() > 0) {
        column.setPreferredWidth(property.getPreferredColumnWidth());
      }
      columns.add(column);
    }

    return columns;
  }

  /**
   * A default sort model implementation based on Entity
   */
  public static class DefaultEntityTableSortModel extends AbstractTableSortModel<Entity, Property> {

    /**
     * Instantiates a new DefaultEntityTableSortModel
     * @param entityID the entity ID
     */
    public DefaultEntityTableSortModel(final String entityID) {
      super(initializeColumns(entityID));
    }

    /** {@inheritDoc} */
    @Override
    public final Class getColumnClass(final Property property) {
      return property.getTypeClass();
    }

    /** {@inheritDoc} */
    @Override
    protected Comparator initializeColumnComparator(final Property property) {
      if (property instanceof Property.ForeignKeyProperty) {
        return Entities.getComparator(((Property.ForeignKeyProperty) property).getReferencedEntityID());
      }

      return super.initializeColumnComparator(property);
    }

    /** {@inheritDoc} */
    @Override
    protected final Comparable getComparable(final Entity entity, final Property property) {
      return (Comparable) entity.get(property);
    }
  }
}