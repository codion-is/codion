/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.EventListener;
import org.jminor.common.TextUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.model.PreferencesUtil;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.model.table.SortingDirective;
import org.jminor.common.model.table.TableSortModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultEntityTableConditionModel;
import org.jminor.framework.model.DefaultPropertyFilterModelProvider;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.EntityModel;
import org.jminor.framework.model.EntityTableConditionModel;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.swing.common.model.table.AbstractFilteredTableModel;
import org.jminor.swing.common.model.table.AbstractTableSortModel;
import org.jminor.swing.common.model.table.SwingFilteredTableColumnModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.TableModelEvent;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * A TableModel implementation for displaying and working with entities.
 *
 * <pre>
 * String entityId = "some.entity";
 * String clientTypeId = "JavadocDemo";
 * User user = new User("scott", "tiger");
 *
 * EntityConnectionProvider connectionProvider = EntityConnectionProviders.createConnectionProvider(user, clientTypeId);
 *
 * SwingEntityTableModel tableModel = new SwingEntityTableModel(entityId, connectionProvider);
 *
 * SwingEntityEditModel editModel = ...;
 *
 * tableModel.setEditModel(editModel);
 *
 * EntityTablePanel panel = new EntityTablePanel(model);
 * </pre>
 */
public class SwingEntityTableModel extends AbstractFilteredTableModel<Entity, Property>
        implements EntityTableModel<SwingEntityEditModel> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(SwingEntityTableModel.class.getName(), Locale.getDefault());

  private static final Logger LOG = LoggerFactory.getLogger(SwingEntityTableModel.class);

  /**
   * The entity ID
   */
  private final String entityId;

  /**
   * The EntityConnection provider
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * The edit model to use when updating/deleting entities
   */
  private SwingEntityEditModel editModel;

  /**
   * The condition model
   */
  private final EntityTableConditionModel conditionModel;

  /**
   * The conditions instance
   */
  private final EntityConditions entityConditions;

  /**
   * the maximum number of records to fetch via the underlying query, -1 meaning all records should be fetched
   */
  private int fetchCount = -1;

  /**
   * True if the underlying query should be configurable by the user
   */
  private boolean queryConfigurationAllowed = true;

  /**
   * If true then querying should be disabled if no condition is specified
   */
  private boolean queryConditionRequired = false;

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
   * Instantiates a new DefaultEntityTableModel with default column and condition models.
   * @param entityId the entity ID
   * @param connectionProvider the db provider
   */
  public SwingEntityTableModel(final String entityId, final EntityConnectionProvider connectionProvider) {
    this(entityId, connectionProvider, new DefaultEntityTableSortModel(connectionProvider.getDomain(), entityId),
            new DefaultEntityTableConditionModel(entityId, connectionProvider,
                    new DefaultPropertyFilterModelProvider(), new SwingPropertyConditionModelProvider()));
  }

  /**
   * Instantiates a new DefaultEntityTableModel.
   * @param entityId the entity ID
   * @param connectionProvider the db provider
   * @param conditionModel the condition model
   * @param sortModel the sort model
   * @throws NullPointerException in case conditionModel is null
   * @throws IllegalArgumentException if {@code conditionModel} entityId does not match the one supplied as parameter
   */
  public SwingEntityTableModel(final String entityId, final EntityConnectionProvider connectionProvider,
                               final TableSortModel<Entity, Property, TableColumn> sortModel,
                               final EntityTableConditionModel conditionModel) {
    super(sortModel, Objects.requireNonNull(conditionModel, "conditionModel").getPropertyFilterModels());
    if (!conditionModel.getEntityId().equals(entityId)) {
      throw new IllegalArgumentException("Entity ID mismatch, conditionModel: " + conditionModel.getEntityId()
              + ", tableModel: " + entityId);
    }
    this.entityId = entityId;
    this.connectionProvider = connectionProvider;
    this.conditionModel = conditionModel;
    this.entityConditions = connectionProvider.getConditions();
    bindEventsInternal();
    applyPreferences();
  }

  /** {@inheritDoc} */
  @Override
  public Entities getDomain() {
    return connectionProvider.getDomain();
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final void setEditModel(final SwingEntityEditModel editModel) {
    Objects.requireNonNull(editModel, "editModel");
    if (this.editModel != null) {
      throw new IllegalStateException("Edit model has already been set for table model: " + this);
    }
    if (!editModel.getEntityId().equals(entityId)) {
      throw new IllegalArgumentException("Entity ID mismatch, editModel: " + editModel.getEntityId() + ", tableModel: " + entityId);
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
  public final SwingEntityTableModel setQueryConfigurationAllowed(final boolean value) {
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
  public final SwingEntityTableModel setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isQueryConditionRequired() {
    return queryConditionRequired;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityTableModel setQueryConditionRequired(final boolean value) {
    this.queryConditionRequired = value;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public InsertAction getInsertAction() {
    return insertAction;
  }

  /** {@inheritDoc} */
  @Override
  public SwingEntityTableModel setInsertAction(final InsertAction insertAction) {
    Objects.requireNonNull(insertAction, "insertAction");
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
  public final SwingEntityTableModel setRemoveEntitiesOnDelete(final boolean value) {
    this.removeEntitiesOnDelete = value;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableConditionModel getConditionModel() {
    return conditionModel;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityEditModel getEditModel() {
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
  public final SwingEntityTableModel setBatchUpdateAllowed(final boolean batchUpdateAllowed) {
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
  public final int getPropertyColumnIndex(final String propertyId) {
    return getColumnModel().getColumnIndex(getDomain().getProperty(getEntityId(), propertyId));
  }

  /**
   * Returns true if the cell at <code>rowIndex</code> and <code>modelColumnIndex</code>
   * is editable.  Otherwise, <code>setValueAt</code> on the cell will not change the value of that cell.
   * @param rowIndex the row whose value to be queried
   * @param modelColumnIndex the column whose value to be queried
   * @return true if the cell is editable
   * @see #setValueAt
   */
  @Override
  public boolean isCellEditable(final int rowIndex, final int modelColumnIndex) {
    return false;
  }

  /**
   * Returns the value for the cell at <code>modelColumnIndex</code> and <code>rowIndex</code>.   *
   * @param rowIndex the row whose value is to be queried
   * @param modelColumnIndex the column whose value is to be queried
   * @return the value Object at the specified cell
   */
  @Override
  public final Object getValueAt(final int rowIndex, final int modelColumnIndex) {
    final Property property = getColumnModel().getColumnIdentifier(modelColumnIndex);
    final Entity entity = getItemAt(rowIndex);

    return getValue(entity, property);
  }

  /**
   * Sets the value in the cell at <code>columnIndex</code> and
   * <code>rowIndex</code> to <code>aValue</code>.
   * @param value the new value
   * @param rowIndex the row whose value is to be changed
   * @param modelColumnIndex the model index of the column to be changed
   */
  @Override
  public void setValueAt(final Object value, final int rowIndex, final int modelColumnIndex) {
    throw new UnsupportedOperationException("setValueAt is not supported");
  }

  /**
   * A convenience method for setting the sorting directive for the given property
   * @param propertyId the property ID
   * @param directive the directive
   * @param addColumnToSort if true then the column is added to the sorting state
   * @see TableSortModel#setSortingDirective(Object, SortingDirective, boolean)
   */
  public void setSortingDirective(final String propertyId, final SortingDirective directive, final boolean addColumnToSort) {
    getSortModel().setSortingDirective(getDomain().getProperty(getEntityId(), propertyId), directive, addColumnToSort);
  }

  /** {@inheritDoc} */
  @Override
  public Color getPropertyBackgroundColor(final int row, final Property property) {
    return (Color) getItemAt(row).getBackgroundColor(property);
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getEntityByKey(final Entity.Key primaryKey) {
    return getVisibleItems().stream().filter(entity -> entity.getKey().equals(primaryKey)).findFirst().orElse(null);
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

    return getRowCount() + " (" + getSelectionModel().getSelectionCount() + " " +
            MESSAGES.getString("selected") + (filteredItemCount > 0 ? ", " +
            filteredItemCount + " " + MESSAGES.getString("hidden") + ")" : ")");
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntities(final List<Entity> entities, final boolean atTop) {
    addItems(entities, atTop);
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceEntities(final Collection<Entity> entities) {
    replaceEntitiesByKey(Entities.mapToKey(entities));
  }

  /** {@inheritDoc} */
  @Override
  public void setForeignKeyConditionValues(final Property.ForeignKeyProperty foreignKeyProperty, final Collection<Entity> foreignKeyValues) {
    Objects.requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    if (conditionModel.setConditionValues(foreignKeyProperty.getPropertyId(), foreignKeyValues)) {
      refresh();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceForeignKeyValues(final String foreignKeyEntityId, final Collection<Entity> foreignKeyValues) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = getDomain().getForeignKeyProperties(this.entityId, foreignKeyEntityId);
    boolean changed = false;
    for (final Entity entity : getAllItems()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
        for (final Entity foreignKeyValue : foreignKeyValues) {
          final Entity currentForeignKeyValue = entity.getForeignKey(foreignKeyProperty.getPropertyId());
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
    return getAllItems().stream().filter(entity -> keys.stream()
            .anyMatch(key -> entity.getKey().equals(key))).collect(Collectors.toList());
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
    Objects.requireNonNull(entities, "entities");
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
  public final ColumnSummaryModel getColumnSummaryModel(final String propertyId) {
    return getColumnSummaryModel(getDomain().getProperty(entityId, propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public final Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectionModel().getSelectedItems().iterator();
  }

  /** {@inheritDoc} */
  @Override
  public void setColumns(final String... propertyIds) {
    final List<Property> properties = getDomain().getProperties(getEntityId(), propertyIds);
    getColumnModel().setColumns(properties.toArray(new Property[0]));
  }

  /** {@inheritDoc} */
  @Override
  public final void savePreferences() {
    if (EntityModel.USE_CLIENT_PREFERENCES.get()) {
      try {
        PreferencesUtil.putUserPreference(getUserPreferencesKey(), createPreferences().toString());
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

    final String[][] header = {headerValues.toArray(new String[0])};

    return TextUtil.getDelimitedString(header, Entities.getStringValueArray(properties,
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
      final List<Entity> queryResult = performQuery();
      clear();
      addItems(queryResult, false);
      conditionModel.rememberCurrentConditionState();
    }
    finally {
      LOG.debug("{} refreshing done", this);
    }
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed,
   * using the order by clause returned by {@link #getOrderBy()}
   * @return entities selected from the database according the the query condition.
   * @see EntityTableConditionModel#getCondition()
   */
  protected List<Entity> performQuery() {
    if (!getConditionModel().isEnabled() && queryConditionRequired) {
      return Collections.emptyList();
    }

    try {
      return connectionProvider.getConnection().selectMany(entityConditions.selectCondition(entityId,
              getConditionModel().getCondition(), fetchCount).setOrderBy(getOrderBy()));
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
   * @throws NullPointerException in case entity or property is null
   */
  protected Object getValue(final Entity entity, final Property property) {
    Objects.requireNonNull(entity, "entity");
    Objects.requireNonNull(property, "property");
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
   * @see Entities#getOrderBy(String)
   */
  protected Entity.OrderBy getOrderBy() {
    return getDomain().getOrderBy(entityId);
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
   * return getClass().getSimpleName() + "-" + getEntityId();
   * }
   * </pre>
   * Override in case this key is not unique.
   * @return the key used to identify user preferences for this table model
   */
  protected String getUserPreferencesKey() {
    return getClass().getSimpleName() + "-" + getEntityId();
  }

  /**
   * Clears any user preferences saved for this table model
   */
  final void clearPreferences() {
    PreferencesUtil.removeUserPreference(getUserPreferencesKey());
  }

  private void bindEventsInternal() {
    getColumnModel().addColumnHiddenListener(this::handleColumnHidden);
    conditionModel.addSimpleConditionListener(this::refresh);
  }

  private void bindEditModelEventsInternal() {
    editModel.addAfterInsertListener(this::handleInsert);
    editModel.addAfterUpdateListener(this::handleUpdate);
    editModel.addAfterDeleteListener(this::handleDeleteInternal);
    editModel.addAfterRefreshListener(this::refresh);
    editModel.addEntitySetListener(info -> {
      if (info == null && !getSelectionModel().isSelectionEmpty()) {
        getSelectionModel().clearSelection();
      }
    });
    getSelectionModel().addSelectedIndexListener(selected -> {
      final Entity itemToSelect = getSelectionModel().isSelectionEmpty() ? null : getSelectionModel().getSelectedItem();
      editModel.setEntity(itemToSelect);
    });

    addTableModelListener(e -> {
      //if the selected record is being updated via the table model refresh the one in the edit model
      if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == getSelectionModel().getSelectedIndex()) {
        editModel.setEntity(getSelectionModel().getSelectedItem());
      }
    });
  }

  private void handleInsert(final EntityEditModel.InsertEvent insertEvent) {
    getSelectionModel().clearSelection();
    if (!insertAction.equals(InsertAction.DO_NOTHING)) {
      addEntities(insertEvent.getInsertedEntities().stream().filter(entity ->
              entity.getEntityId().equals(getEntityId())).collect(Collectors.toList()), insertAction.equals(InsertAction.ADD_TOP));
    }
  }

  private void handleUpdate(final EntityEditModel.UpdateEvent updateEvent) {
    replaceEntitiesByKey(new HashMap<>(updateEvent.getUpdatedEntities()));
  }

  /**
   * Replace the entities identified by the Entity.Key map keys with their respective value.
   * Note that this does not trigger {@link #filterContents()}, that must be done explicitly.
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
  }

  private void handleDeleteInternal(final EntityEditModel.DeleteEvent deleteEvent) {
    if (removeEntitiesOnDelete) {
      removeItems(deleteEvent.getDeletedEntities());
    }
    handleDelete(deleteEvent);
  }

  private void handleColumnHidden(final Property property) {
    //disable the condition model for the column to be hidden, to prevent confusion
    conditionModel.setEnabled(property.getPropertyId(), false);
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
      columnPreferencesRoot.put(property.getPropertyId(), columnObject);
    }

    return columnPreferencesRoot;
  }

  private void applyPreferences() {
    if (EntityModel.USE_CLIENT_PREFERENCES.get()) {
      final String preferencesString = PreferencesUtil.getUserPreference(getUserPreferencesKey(), "");
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
    final SwingFilteredTableColumnModel<Property> columnModel = getColumnModel();
    for (final TableColumn column : Collections.list(columnModel.getColumns())) {
      final Property property = (Property) column.getIdentifier();
      if (columnModel.containsColumn(property)) {
        try {
          final org.json.JSONObject columnPreferences = preferences.getJSONObject(property.getPropertyId());
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

  /**
   * A default sort model implementation based on Entity
   */
  public static class DefaultEntityTableSortModel extends AbstractTableSortModel<Entity, Property> {

    private final Entities domain;

    /**
     * Instantiates a new DefaultEntityTableSortModel
     * @param domain the underlying entities
     * @param entityId the entity ID
     */
    public DefaultEntityTableSortModel(final Entities domain, final String entityId) {
      super(initializeColumns(domain, entityId));
      this.domain = domain;
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
        return domain.getComparator(((Property.ForeignKeyProperty) property).getForeignEntityId());
      }

      return super.initializeColumnComparator(property);
    }

    /** {@inheritDoc} */
    @Override
    protected final Comparable getComparable(final Entity entity, final Property property) {
      return (Comparable) entity.get(property);
    }

    private static List<TableColumn> initializeColumns(final Entities domain, final String entityId) {
      int modelIndex = 0;
      final List<Property> visibleProperties = domain.getVisibleProperties(entityId);
      if (visibleProperties.isEmpty()) {
        throw new IllegalStateException("No visible properties defined for entity: " + entityId);
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
  }
}