/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.DefaultValueChangeMapEditModel;
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.common.model.valuemap.ValueProvider;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.EntityValidator;
import org.jminor.framework.domain.Property;

import org.apache.log4j.Logger;

import javax.swing.ComboBoxModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A default EntityEditModel implementation
 */
public class DefaultEntityEditModel extends DefaultValueChangeMapEditModel<String, Object> implements EntityEditModel {

  protected static final Logger LOG = Util.getLogger(DefaultEntityEditModel.class);

  private final Event evtBeforeInsert = new Event();
  private final Event evtAfterInsert = new Event();
  private final Event evtBeforeUpdate = new Event();
  private final Event evtAfterUpdate = new Event();
  private final Event evtBeforeDelete = new Event();
  private final Event evtAfterDelete = new Event();
  private final Event evtEntitiesChanged = new Event();
  private final Event evtRefreshStarted = new Event();
  private final Event evtRefreshDone = new Event();

  private final State stEntityNull = new State(true);
  private final State stAllowInsert = new State(true);
  private final State stAllowUpdate = new State(true);
  private final State stAllowDelete = new State(true);

  /**
   * Indicates whether the model is active and ready to receive input
   */
  private final State stActive = new State(Configuration.getBooleanValue(Configuration.ALL_PANELS_ACTIVE));

  /**
   * The ID of the entity this edit model is based on
   */
  private final String entityID;

  /**
   * The EntityDbProvider instance to use
   */
  private final EntityDbProvider dbProvider;

  /**
   * Holds the ComboBoxModels used by this EntityModel, those that implement Refreshable
   * are refreshed when refreshComboBoxModels() is called
   * @see org.jminor.common.model.Refreshable
   */
  private final Map<Property, ComboBoxModel> propertyComboBoxModels = new HashMap<Property, ComboBoxModel>();

  /**
   * The mechanism for restricting a single active EntityEditModel at a time
   */
  private static final State.StateGroup ACTIVE_STATE_GROUP = new State.StateGroup();

  private boolean readOnly;

  /**
   * Instantiates a new EntityEditModel based on the entity identified by <code>entityID</code>.
   * @param entityID the ID of the entity to base this EntityEditModel on
   * @param dbProvider the EntityDbProvider instance
   */
  public DefaultEntityEditModel(final String entityID, final EntityDbProvider dbProvider) {
    this(entityID, dbProvider, new DefaultEntityValidator(entityID));
  }

  /**
   * Instantiates a new EntityEditModel based on the entity identified by <code>entityID</code>.
   * @param entityID the ID of the entity to base this EntityEditModel on
   * @param dbProvider the EntityDbProvider instance
   * @param validator the validator to use
   */
  public DefaultEntityEditModel(final String entityID, final EntityDbProvider dbProvider, final EntityValidator validator) {
    super(Entities.entityInstance(entityID), validator);
    Util.rejectNullValue(dbProvider, "dbProvider");
    if (!Configuration.getBooleanValue(Configuration.ALL_PANELS_ACTIVE)) {
      ACTIVE_STATE_GROUP.addState(stActive);
    }
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.readOnly = EntityRepository.isReadOnly(entityID);
    setValueMap(null);
    bindEventsInternal();
    bindEvents();
  }

  public Object getDefaultValue(final Property property) {
    return persistValueOnClear(property) ? getValue(property.getPropertyID()) : property.getDefaultValue();
  }

  public boolean persistValueOnClear(final Property property) {
    return property instanceof Property.ForeignKeyProperty
            && Configuration.getBooleanValue(Configuration.PERSIST_FOREIGN_KEY_VALUES);
  }

  public PropertyComboBoxModel createPropertyComboBoxModel(final Property.ColumnProperty property, final Event refreshEvent,
                                                           final String nullValueString) {
    return new DefaultPropertyComboBoxModel(entityID, dbProvider, property, nullValueString, refreshEvent);
  }

  public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    final EntityComboBoxModel model = new DefaultEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), dbProvider);
    model.setNullValueString(getValidator().isNullable(getEntity(), foreignKeyProperty.getPropertyID()) ?
            (String) Configuration.getValue(Configuration.DEFAULT_COMBO_BOX_NULL_VALUE_ITEM) : null);

    return model;
  }

  public EntityLookupModel createEntityLookupModel(final String entityID,
                                                   final List<Property.ColumnProperty> lookupProperties,
                                                   final Criteria additionalSearchCriteria) {
    final EntityLookupModel model = new DefaultEntityLookupModel(entityID, dbProvider, lookupProperties);
    model.setAdditionalLookupCriteria(additionalSearchCriteria);

    return model;
  }

  public final boolean isReadOnly() {
    return readOnly;
  }

  public final EntityEditModel setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
    return this;
  }

  public final boolean isInsertAllowed() {
    return stAllowInsert.isActive();
  }

  public final EntityEditModel setInsertAllowed(final boolean value) {
    stAllowInsert.setActive(value);
    return this;
  }

  public final State stateAllowInsert() {
    return stAllowInsert.getLinkedState();
  }

  public final boolean isUpdateAllowed() {
    return stAllowUpdate.isActive();
  }

  public final EntityEditModel setUpdateAllowed(final boolean value) {
    stAllowUpdate.setActive(value);
    return this;
  }

  public final State stateAllowUpdate() {
    return stAllowUpdate.getLinkedState();
  }

  public final boolean isDeleteAllowed() {
    return stAllowDelete.isActive();
  }

  public final EntityEditModel setDeleteAllowed(final boolean value) {
    stAllowDelete.setActive(value);
    return this;
  }

  public final State stateAllowDelete() {
    return stAllowDelete.getLinkedState();
  }

  public final State stateEntityNull() {
    return stEntityNull.getLinkedState();
  }

  public final State stateActive() {
    return stActive.getLinkedState();
  }

  public final void setActive(final boolean active) {
    stActive.setActive(active);
  }

  public final void setEntity(final Entity entity) {
    setValueMap(entity);
  }

  public final String getEntityID() {
    return entityID;
  }

  public final EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  public final Entity getForeignKeyValue(final String foreignKeyPropertyID) {
    return (Entity) getValue(foreignKeyPropertyID);
  }

  public final Entity getEntityCopy() {
    return getEntityCopy(true);
  }

  public final Entity getEntityCopy(final boolean includePrimaryKeyValues) {
    final Entity copy = (Entity) getEntity().getCopy();
    if (!includePrimaryKeyValues) {
      copy.getPrimaryKey().clear();
    }

    return copy;
  }

  public final boolean isEntityNew() {
    final Entity.Key key = ((Entity) getValueMap()).getPrimaryKey();
    final Entity.Key originalkey = ((Entity) getValueMap()).getOriginalPrimaryKey();
    return key.isNull() || originalkey.isNull();
  }

  public final void insert() throws CancelException, DbException, ValidationException {
    insert(Arrays.asList(getEntityCopy(!EntityRepository.isPrimaryKeyAutoGenerated(entityID))));
  }

  public final void insert(final List<Entity> entities) throws CancelException, DbException, ValidationException {
    Util.rejectNullValue(entities, "entities");
    if (entities.isEmpty()) {
      return;
    }
    if (readOnly) {
      throw new RuntimeException("This is a read-only model, inserting is not allowed!");
    }
    if (!isInsertAllowed()) {
      throw new RuntimeException("This model does not allow inserting!");
    }

    LOG.debug(toString() + " - insert " + Util.getCollectionContentsAsString(entities, false));

    evtBeforeInsert.fire();
    ((EntityValidator) getValidator()).validate(entities, EntityValidator.INSERT);

    final List<Entity.Key> primaryKeys = EntityUtil.copy(doInsert(entities));
    evtAfterInsert.fire(new InsertEvent(this, primaryKeys));
  }

  public final void update() throws CancelException, DbException, ValidationException {
    update(Arrays.asList(getEntityCopy()));
  }

  public final void update(final List<Entity> entities) throws DbException, CancelException, ValidationException {
    Util.rejectNullValue(entities, "entities");
    if (entities.isEmpty()) {
      return;
    }
    if (readOnly) {
      throw new RuntimeException("This is a read-only model, updating is not allowed!");
    }
    if (!isUpdateAllowed()) {
      throw new RuntimeException("This model does not allow updating!");
    }

    LOG.debug(toString() + " - update " + Util.getCollectionContentsAsString(entities, false));

    final List<Entity> modifiedEntities = EntityUtil.getModifiedEntities(entities);
    if (modifiedEntities.isEmpty()) {
      return;
    }

    evtBeforeUpdate.fire();
    ((EntityValidator) getValidator()).validate(modifiedEntities, EntityValidator.UPDATE);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);
    final int index = updatedEntities.indexOf(getEntity());
    if (index >= 0) {
      setEntity(updatedEntities.get(index));
    }

    evtAfterUpdate.fire(new UpdateEvent(this, updatedEntities, EntityUtil.isPrimaryKeyModified(modifiedEntities)));
  }

  public final void delete() throws DbException, CancelException {
    delete(Arrays.asList(getEntityCopy()));
  }

  public final void delete(final List<Entity> entities) throws DbException, CancelException {
    Util.rejectNullValue(entities, "entities");
    if (entities.isEmpty()) {
      return;
    }
    if (readOnly) {
      throw new RuntimeException("This is a read-only model, deleting is not allowed!");
    }
    if (!isDeleteAllowed()) {
      throw new RuntimeException("This model does not allow deleting!");
    }

    LOG.debug(toString() + " - delete " + Util.getCollectionContentsAsString(entities, false));

    evtBeforeDelete.fire();

    doDelete(entities);

    evtAfterDelete.fire(new DeleteEvent(this, entities));
  }

  @Override
  public final void refresh() {
    try {
      evtRefreshStarted.fire();
      refreshComboBoxModels();
    }
    finally {
      evtRefreshDone.fire();
    }
  }

  @Override
  public final void clear() {
    clearComboBoxModels();
  }

  public final void refreshComboBoxModels() {
    for (final ComboBoxModel comboBoxModel : propertyComboBoxModels.values()) {
      if (comboBoxModel instanceof Refreshable) {
        ((Refreshable) comboBoxModel).refresh();
      }
    }
  }

  public final void clearComboBoxModels() {
    for (final ComboBoxModel comboBoxModel : propertyComboBoxModels.values()) {
      if (comboBoxModel instanceof Refreshable) {
        ((Refreshable) comboBoxModel).clear();
      }
    }
  }

  public final PropertyComboBoxModel getPropertyComboBoxModel(final Property.ColumnProperty property) {
    Util.rejectNullValue(property, "property");
    final PropertyComboBoxModel comboBoxModel = (PropertyComboBoxModel) propertyComboBoxModels.get(property);
    if (comboBoxModel == null) {
      throw new RuntimeException("No PropertyComboBoxModel has been initialized for property: " + property);
    }

    return comboBoxModel;
  }

  public final PropertyComboBoxModel initializePropertyComboBoxModel(final Property.ColumnProperty property, final Event refreshEvent,
                                                                     final String nullValueString) {
    Util.rejectNullValue(property, "property");
    PropertyComboBoxModel comboBoxModel = (PropertyComboBoxModel) propertyComboBoxModels.get(property);
    if (comboBoxModel == null) {
      comboBoxModel = createPropertyComboBoxModel(property, refreshEvent == null ? evtEntitiesChanged : refreshEvent, nullValueString);
      setComboBoxModel(property, comboBoxModel);
      comboBoxModel.refresh();
    }

    return comboBoxModel;
  }

  public final EntityComboBoxModel getEntityComboBoxModel(final String propertyID) {
    Util.rejectNullValue(propertyID, "propertyID");
    final Property property = EntityRepository.getProperty(entityID, propertyID);
    if (!(property instanceof Property.ForeignKeyProperty)) {
      throw new IllegalArgumentException("EntityComboBoxModels are only available for Property.ForeignKeyProperty");
    }

    return getEntityComboBoxModel((Property.ForeignKeyProperty) property);
  }

  public final EntityComboBoxModel getEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    final EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null) {
      throw new RuntimeException("No EntityComboBoxModel has been initialized for property: " + foreignKeyProperty);
    }

    return comboBoxModel;
  }

  public final EntityComboBoxModel initializeEntityComboBoxModel(final String propertyID) {
    Util.rejectNullValue(propertyID, "propertyID");
    final Property property = EntityRepository.getProperty(entityID, propertyID);
    if (!(property instanceof Property.ForeignKeyProperty)) {
      throw new IllegalArgumentException("EntityComboBoxModels are only available for Property.ForeignKeyProperty");
    }

    return initializeEntityComboBoxModel((Property.ForeignKeyProperty) property);
  }

  public final EntityComboBoxModel initializeEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null) {
      comboBoxModel = createEntityComboBoxModel(foreignKeyProperty);
      setComboBoxModel(foreignKeyProperty, comboBoxModel);
    }

    return comboBoxModel;
  }

  public final boolean containsComboBoxModel(final String propertyID) {
    return containsComboBoxModel(EntityRepository.getProperty(entityID, propertyID));
  }

  public final boolean containsComboBoxModel(final Property property) {
    Util.rejectNullValue(property, "property");
    return propertyComboBoxModels.containsKey(property);
  }

  @Override
  public final Entity getDefaultValueMap() {
    final Entity defaultEntity = Entities.entityInstance(entityID);
    final Collection<Property.ColumnProperty> databaseProperties = EntityRepository.getColumnProperties(entityID);
    for (final Property.ColumnProperty property : databaseProperties) {
      if (!property.hasParentProperty() && !property.isDenormalized()) {//these are set via their respective parent properties
        defaultEntity.setValue(property, getDefaultValue(property));
      }
    }

    return defaultEntity;
  }

  public final ValueCollectionProvider<Object> getValueProvider(final Property property) {
    return new PropertyValueProvider(dbProvider, entityID, property.getPropertyID());
  }

  public final Event eventAfterDelete() {
    return evtAfterDelete;
  }

  public final Event eventAfterInsert() {
    return evtAfterInsert;
  }

  public final Event eventAfterUpdate() {
    return evtAfterUpdate;
  }

  public final Event eventBeforeDelete() {
    return evtBeforeDelete;
  }

  public final Event eventBeforeInsert() {
    return evtBeforeInsert;
  }

  public final Event eventBeforeUpdate() {
    return evtBeforeUpdate;
  }

  public final Event eventEntitiesChanged() {
    return evtEntitiesChanged;
  }

  public final Event eventRefreshStarted() {
    return evtRefreshStarted;
  }

  public final Event eventRefreshDone() {
    return evtRefreshDone;
  }

  /**
   * Inserts the given entities from the database
   * @param entities the entities to insert
   * @return a list containing the primary keys of the inserted entities
   * @throws DbException in case of a database exception
   * @throws CancelException in case the operation is canceled
   */
  protected List<Entity.Key> doInsert(final List<Entity> entities) throws DbException, CancelException {
    try {
      return dbProvider.getEntityDb().insert(entities);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Updates the given entities in the database
   * @param entities the entities to update
   * @return a list containing the updated entities
   * @throws DbException in case of a database exception
   * @throws CancelException in case the operation is cancelled
   */
  protected List<Entity> doUpdate(final List<Entity> entities) throws DbException, CancelException {
    try {
      return dbProvider.getEntityDb().update(entities);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Deletes the given entities from the database
   * @param entities the entities to delete
   * @throws DbException in case of a database exception
   * @throws CancelException in case the operation is canceled
   */
  protected void doDelete(final List<Entity> entities) throws DbException, CancelException {
    try {
      dbProvider.getEntityDb().delete(EntityUtil.getPrimaryKeys(entities));
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the Entity instance being edited
   */
  protected final Entity getEntity() {
    return (Entity) getValueMap();
  }

  /**
   * Override to add event bindings
   */
  protected void bindEvents() {}

  private void bindEventsInternal() {
    evtAfterDelete.addListener(evtEntitiesChanged);
    evtAfterInsert.addListener(evtEntitiesChanged);
    evtAfterUpdate.addListener(evtEntitiesChanged);
    eventValueMapSet().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        stEntityNull.setActive(getEntity().isNull());
      }
    });
    if (Configuration.getBooleanValue(Configuration.PROPERTY_DEBUG_OUTPUT)) {
      getEntity().addValueListener(new ValueChangeListener<String, Object>() {
        @Override
        protected void valueChanged(final ValueChangeEvent<String, Object> event) {
          final String msg = getValueChangeDebugString(event);
          System.out.println(msg);
          LOG.debug(msg);
        }
      });
    }
  }

  /**
   * Sets the ComboBoxModel to be associated with the given property
   * @param property the property
   * @param model the ComboBoxModel
   * @throws RuntimeException in case the ComboBoxModel has already been set for this property
   */
  private void setComboBoxModel(final Property property, final ComboBoxModel model) {
    if (propertyComboBoxModels.containsKey(property)) {
      throw new RuntimeException("ComboBoxModel already associated with property: " + property);
    }

    propertyComboBoxModels.put(property, model);
  }

  private static String getValueChangeDebugString(final ValueChangeEvent<String, Object> event) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (event.getSource() instanceof Entity) {
      stringBuilder.append("[entity] ");
    }
    else {
      stringBuilder.append(event.isModelChange() ? "[model] " : "[ui] ");
    }
    final Entity valueOwner = (Entity) event.getValueOwner();
    final Property property = EntityRepository.getProperty(valueOwner.getEntityID(), event.getKey());
    stringBuilder.append(valueOwner.getEntityID()).append(" : ").append(property).append(
            property.hasParentProperty() ? " [fk]" : "").append("; ");
    if (!event.isInitialization()) {
      if (!event.isOldValueNull()) {
        stringBuilder.append(event.getOldValue().getClass().getSimpleName()).append(" ");
      }
      stringBuilder.append(getValueString(event.getOldValue()));
    }
    if (!event.isInitialization()) {
      stringBuilder.append(" -> ");
    }
    if (!event.isNewValueNull()) {
      stringBuilder.append(event.getNewValue().getClass().getSimpleName()).append(" ");
    }
    stringBuilder.append(getValueString(event.getNewValue()));

    return stringBuilder.toString();
  }

  /**
   * @param value the value
   * @return a string representing the given property value for debug output
   */
  private static String getValueString(final Object value) {
    final StringBuilder stringBuilder = new StringBuilder("[").append(value == null ? "null value" : value).append("]");
    if (value instanceof Entity) {
      stringBuilder.append(" PK{").append(((Entity) value).getPrimaryKey()).append("}");
    }

    return stringBuilder.toString();
  }

  static class PropertyValueProvider implements ValueCollectionProvider<Object> {

    private final EntityDbProvider dbProvider;
    private final String entityID;
    private final String propertyID;

    PropertyValueProvider(final EntityDbProvider dbProvider, final String entityID, final String propertyID) {
      this.dbProvider = dbProvider;
      this.entityID = entityID;
      this.propertyID = propertyID;
    }

    public String getEntityID() {
      return entityID;
    }

    public String getPropertyID() {
      return propertyID;
    }

    public EntityDbProvider getDbProvider() {
      return dbProvider;
    }

    public Collection<Object> getValues() {
      try {
        return dbProvider.getEntityDb().selectPropertyValues(entityID, propertyID, true);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public static ValueProvider<Property, Object> getValueProvider(final Entity entity) {
      return new ValueProvider<Property, Object>() {
        public Object getValue(final Property key) {
          return entity.getValue(key);
        }
      };
    }
  }
}