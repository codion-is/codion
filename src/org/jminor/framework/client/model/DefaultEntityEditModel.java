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
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;
import org.jminor.common.model.valuemap.ValueChangeMap;
import org.jminor.common.model.valuemap.AbstractValueChangeMapEditModel;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.RangeValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

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
 * A class for editing a Entity instance, providing property change events and combobox models.
 */
public class DefaultEntityEditModel extends AbstractValueChangeMapEditModel<String, Object> implements EntityEditModel {

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

  /**
   * Instantiates a new EntityEditModel based on the entity identified by <code>entityID</code>.
   * @param entityID the ID of the entity to base this EntityEditModel on
   * @param dbProvider the EntityDbProvider instance
   */
  public DefaultEntityEditModel(final String entityID, final EntityDbProvider dbProvider) {
    super(new Entity(entityID));
    Util.rejectNullValue(entityID);
    Util.rejectNullValue(dbProvider);
    if (!Configuration.getBooleanValue(Configuration.ALL_PANELS_ACTIVE)) {
      ACTIVE_STATE_GROUP.addState(stActive);
    }
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    setValueMap(null);
    bindEventsInternal();
    bindEvents();
  }

  public boolean isReadOnly() {
    return EntityRepository.isReadOnly(entityID);
  }

  public boolean isInsertAllowed() {
    return stAllowInsert.isActive();
  }

  public void setInsertAllowed(final boolean value) {
    stAllowInsert.setActive(value);
  }

  public State stateAllowInsert() {
    return stAllowInsert.getLinkedState();
  }

  public boolean isUpdateAllowed() {
    return stAllowUpdate.isActive();
  }

  public void setUpdateAllowed(final boolean value) {
    stAllowUpdate.setActive(value);
  }

  public State stateAllowUpdate() {
    return stAllowUpdate.getLinkedState();
  }

  public boolean isDeleteAllowed() {
    return stAllowDelete.isActive();
  }

  public void setDeleteAllowed(final boolean value) {
    stAllowDelete.setActive(value);
  }

  public State stateAllowDelete() {
    return stAllowDelete.getLinkedState();
  }

  public State stateEntityNull() {
    return stEntityNull.getLinkedState();
  }

  public State stateActive() {
    return stActive.getLinkedState();
  }

  public void setActive(boolean active) {
    stActive.setActive(active);
  }

  public void setEntity(final Entity entity) {
    setValueMap(entity);
  }

  public String getEntityID() {
    return entityID;
  }

  public EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  public boolean isEntityNew() {
    final Entity.Key key = ((Entity) getValueMap()).getPrimaryKey();
    final Entity.Key originalkey = ((Entity) getValueMap()).getOriginalPrimaryKey();
    return key.isNull() || originalkey.isNull();
  }

  /**
   * Returns the value associated with the given propertyID assuming it
   * is an Entity instance
   * @param foreignKeyPropertyID the ID of the property
   * @return the value assuming it is an Entity
   * @throws ClassCastException in case the value was not an Entity
   */
  public Entity getEntityValue(final String foreignKeyPropertyID) {
    return (Entity) getValue(foreignKeyPropertyID);
  }

  public Entity getEntityCopy() {
    return getEntityCopy(true);
  }

  /**
   * @param includePrimaryKeyValues if false then the primary key values are excluded
   * @return a deep copy of the active entity
   * @see org.jminor.framework.domain.Entity#getCopy()
   */
  public Entity getEntityCopy(final boolean includePrimaryKeyValues) {
    final Entity copy = (Entity) getEntity().getCopy();
    if (!includePrimaryKeyValues) {
      copy.getPrimaryKey().clear();
    }

    return copy;
  }

  public boolean isEntityModified() {
    return stateModified().isActive();
  }

  public final void insert() throws CancelException, DbException, ValidationException {
    insert(Arrays.asList(getEntityCopy(!EntityRepository.isPrimaryKeyAutoGenerated(entityID))));
  }

  /**
   * Performs an insert on the given entities, returns silently on recieving an empty list
   * @param entities the entities to insert
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws ValidationException in case validation fails
   * @see #evtBeforeInsert
   * @see #evtAfterInsert
   * @see #validateEntities(java.util.List, int)
   */
  public final void insert(final List<Entity> entities) throws CancelException, DbException, ValidationException {
    Util.rejectNullValue(entities);
    if (entities.size() == 0) {
      return;
    }
    if (isReadOnly()) {
      throw new RuntimeException("This is a read-only model, inserting is not allowed!");
    }
    if (!isInsertAllowed()) {
      throw new RuntimeException("This model does not allow inserting!");
    }

    LOG.debug(toString() + " - insert " + Util.getListContentsAsString(entities, false));

    evtBeforeInsert.fire();
    validateEntities(entities, EntityEditModel.INSERT);

    final List<Entity.Key> primaryKeys = Entity.Key.copy(doInsert(entities));
    evtAfterInsert.fire(new InsertEvent(this, primaryKeys));
  }

  public final void update() throws CancelException, DbException, ValidationException {
    update(Arrays.asList(getEntityCopy()));
  }

  public final void update(final List<Entity> entities) throws DbException, CancelException, ValidationException {
    Util.rejectNullValue(entities);
    if (entities.size() == 0) {
      return;
    }
    if (isReadOnly()) {
      throw new RuntimeException("This is a read-only model, updating is not allowed!");
    }
    if (!isUpdateAllowed()) {
      throw new RuntimeException("This model does not allow updating!");
    }

    LOG.debug(toString() + " - update " + Util.getListContentsAsString(entities, false));

    final List<Entity> modifiedEntities = EntityUtil.getModifiedEntities(entities);
    if (modifiedEntities.size() == 0) {
      return;
    }

    evtBeforeUpdate.fire();
    validateEntities(modifiedEntities, EntityEditModel.UPDATE);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);

    evtAfterUpdate.fire(new UpdateEvent(this, updatedEntities, Entity.isPrimaryKeyModified(modifiedEntities)));
  }

  public final void delete() throws DbException, CancelException {
    delete(Arrays.asList(getEntityCopy()));
  }

  public final void delete(final List<Entity> entities) throws DbException, CancelException {
    Util.rejectNullValue(entities);
    if (entities.size() == 0) {
      return;
    }
    if (isReadOnly()) {
      throw new RuntimeException("This is a read-only model, deleting is not allowed!");
    }
    if (!isDeleteAllowed()) {
      throw new RuntimeException("This model does not allow deleting!");
    }

    LOG.debug(toString() + " - delete " + Util.getListContentsAsString(entities, false));

    evtBeforeDelete.fire();

    doDelete(entities);

    evtAfterDelete.fire(new DeleteEvent(this, entities));
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public void validateEntities(final List<Entity> entities, final int action) throws ValidationException {
    Util.rejectNullValue(entities);
    for (final Entity entity : entities) {
      for (final Property property : EntityRepository.getProperties(entity.getEntityID()).values()) {
        validate(entity, property.getPropertyID(), action);
      }
    }
  }

  public final void validate(final String key, final int action) throws ValidationException {
    Util.rejectNullValue(key);
    validate(getEntity(), key, action);
  }

  public final void validate(final ValueChangeMap<String, Object> valueMap, final String key, final int action) throws ValidationException {
    validate((Entity) valueMap, key, action);
  }

  /**
   * Validates the given property in the given entity
   * @param entity the entity to validate
   * @param propertyID the ID of the property to validate
   * @param action the action requiring validation
   * @throws ValidationException in case the validation fails
   * @see org.jminor.framework.domain.Property#setNullable(boolean)
   * @see org.jminor.framework.Configuration#PERFORM_NULL_VALIDATION
   */
  public void validate(final Entity entity, final String propertyID, final int action) throws ValidationException {
    Util.rejectNullValue(entity);
    Util.rejectNullValue(propertyID);
    final Property property = entity.getProperty(propertyID);
    if (Configuration.getBooleanValue(Configuration.PERFORM_NULL_VALIDATION)) {
      performNullValidation(entity, property, action);
    }
    if (property.isNumerical()) {
      performRangeValidation(entity, property);
    }
  }

  /**
   * Returns true if the given property accepts a null value, by default this
   * method simply returns <code>property.isNullable()</code>
   * @param key the property ID
   * @return true if the property accepts a null value
   */
  public boolean isNullable(final String key) {
    Util.rejectNullValue(key);
    return isNullable(getEntity(), key);
  }

  /**
   * Refreshes any data aware control models found in the edit model,
   * such as combo box models.
   */
  public void refresh() {
    try {
      evtRefreshStarted.fire();
      refreshComboBoxModels();
    }
    finally {
      evtRefreshDone.fire();
    }
  }

  public void clear() {
    clearComboBoxModels();
  }

  public void refreshComboBoxModels() {
    for (final ComboBoxModel comboBoxModel : propertyComboBoxModels.values()) {
      if (comboBoxModel instanceof Refreshable) {
        ((Refreshable) comboBoxModel).refresh();
      }
    }
  }

  public void clearComboBoxModels() {
    for (final ComboBoxModel comboBoxModel : propertyComboBoxModels.values()) {
      if (comboBoxModel instanceof Refreshable) {
        ((Refreshable) comboBoxModel).clear();
      }
    }
  }

  /**
   * @param property the property for which to get the ComboBoxModel
   * @return a PropertyComboBoxModel representing <code>property</code>
   * @throws RuntimeException if no combo box has been initialized for the given property
   */
  public PropertyComboBoxModel getPropertyComboBoxModel(final Property property) {
    Util.rejectNullValue(property);
    final PropertyComboBoxModel comboBoxModel = (PropertyComboBoxModel) propertyComboBoxModels.get(property);
    if (comboBoxModel == null) {
      throw new RuntimeException("No PropertyComboBoxModel has been initialized for property: " + property);
    }

    return comboBoxModel;
  }

  public PropertyComboBoxModel initializePropertyComboBoxModel(final Property property, final Event refreshEvent,
                                                               final String nullValueString) {
    Util.rejectNullValue(property);
    PropertyComboBoxModel comboBoxModel = (PropertyComboBoxModel) propertyComboBoxModels.get(property);
    if (comboBoxModel == null) {
      comboBoxModel = createPropertyComboBoxModel(property, refreshEvent == null ? evtEntitiesChanged : refreshEvent, nullValueString);
      setComboBoxModel(property, comboBoxModel);
      comboBoxModel.refresh();
    }

    return comboBoxModel;
  }

  /**
   * @param property the property for which to get the ComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires
   * @param nullValueString the value to use for representing the null item at the top of the list,
   * if this value is null then no such item is included
   * @return a new PropertyComboBoxModel based on the given property
   */
  public PropertyComboBoxModel createPropertyComboBoxModel(final Property property, final Event refreshEvent,
                                                           final String nullValueString) {
    return new PropertyComboBoxModel(entityID, dbProvider, property, nullValueString, refreshEvent);
  }

  public EntityComboBoxModel getEntityComboBoxModel(final String propertyID) {
    Util.rejectNullValue(propertyID);
    final Property property = EntityRepository.getProperty(entityID, propertyID);
    if (!(property instanceof Property.ForeignKeyProperty)) {
      throw new IllegalArgumentException("EntityComboBoxModels are only available for Property.ForeignKeyProperty");
    }

    return getEntityComboBoxModel((Property.ForeignKeyProperty) property);
  }

  public EntityComboBoxModel getEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty);
    final EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null) {
      throw new RuntimeException("No EntityComboBoxModel has been initialized for property: " + foreignKeyProperty);
    }

    return comboBoxModel;
  }

  public EntityComboBoxModel initializeEntityComboBoxModel(final String propertyID) {
    Util.rejectNullValue(propertyID);
    final Property property = EntityRepository.getProperty(entityID, propertyID);
    if (!(property instanceof Property.ForeignKeyProperty)) {
      throw new IllegalArgumentException("EntityComboBoxModels are only available for Property.ForeignKeyProperty");
    }

    return initializeEntityComboBoxModel((Property.ForeignKeyProperty) property);
  }

  public EntityComboBoxModel initializeEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty);
    EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null) {
      comboBoxModel = createEntityComboBoxModel(foreignKeyProperty);
      setComboBoxModel(foreignKeyProperty, comboBoxModel);
    }

    return comboBoxModel;
  }

  public boolean containsComboBoxModel(final String propertyID) {
    return containsComboBoxModel(EntityRepository.getProperty(entityID, propertyID));
  }

  public boolean containsComboBoxModel(final Property property) {
    Util.rejectNullValue(property);
    return propertyComboBoxModels.containsKey(property);
  }

  public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty);
    final EntityComboBoxModel model = new DefaultEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), dbProvider);
    model.setNullValueString(isNullable(getEntity(), foreignKeyProperty.getPropertyID()) ?
            (String) Configuration.getValue(Configuration.DEFAULT_COMBO_BOX_NULL_VALUE_ITEM) : null);

    return model;
  }

  public EntityLookupModel createEntityLookupModel(final String entityID, final List<Property> lookupProperties,
                                                   final Criteria additionalSearchCriteria) {
    final EntityLookupModel model = new DefaultEntityLookupModel(entityID, dbProvider, lookupProperties);
    model.setAdditionalLookupCriteria(additionalSearchCriteria);

    return model;
  }

  /**
   * Clears the edit model and sets the default state.
   * @see #getDefaultValueMap()
   */
  public void clearValues() {
    setValueMap(null);
  }

  public final Entity getDefaultValueMap() {
    final Entity defaultEntity = new Entity(entityID);
    final Collection<Property> databaseProperties = EntityRepository.getDatabaseProperties(entityID);
    for (final Property property : databaseProperties) {
      if (!property.hasParentProperty() && !property.isDenormalized()) {//these are set via their respective parent properties
        defaultEntity.setValue(property, getDefaultValue(property));
      }
    }

    return defaultEntity;
  }

  /**
   * Initializes a value provider for the given property, used for adding lookup
   * functionality to input fields for example.
   * @param property the property
   * @return a value provider for the given property
   */
  public ValueCollectionProvider<Object> getValueProvider(final Property property) {
    return new PropertyValueProvider(dbProvider, entityID, property.getPropertyID());
  }

  /**
   * Returns the default value for the given property, used when initializing a new
   * default entity for this edit model. This does not apply to denormalized properties
   * (Property.DenormalizedProperty) nor properties that are wrapped in foreign key properties
   * (Property.ForeignKeyProperty)
   * If the default value of a property should be the last value used <code>persistValueOnClear</code>
   * should be overridden so that it returns <code>true</code> for that property.
   * @param property the property
   * @return the default value for the property
   * @see Property#setDefaultValue(Object)
   * @see #persistValueOnClear(org.jminor.framework.domain.Property)
   */
  public Object getDefaultValue(final Property property) {
    return persistValueOnClear(property) ? getValue(property.getPropertyID()) : property.getDefaultValue();
  }

  public Event eventAfterDelete() {
    return evtAfterDelete;
  }

  public Event eventAfterInsert() {
    return evtAfterInsert;
  }

  public Event eventAfterUpdate() {
    return evtAfterUpdate;
  }

  public Event eventBeforeDelete() {
    return evtBeforeDelete;
  }

  public Event eventBeforeInsert() {
    return evtBeforeInsert;
  }

  public Event eventBeforeUpdate() {
    return evtBeforeUpdate;
  }

  public Event eventEntitiesChanged() {
    return evtEntitiesChanged;
  }

  public Event eventRefreshStarted() {
    return evtRefreshStarted;
  }

  public Event eventRefreshDone() {
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

  protected void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException {
    if (entity.isValueNull(property.getPropertyID())) {
      return;
    }

    final Double value = property.isDouble() ? (Double) entity.getValue(property.getPropertyID())
            : (Integer) entity.getValue(property.getPropertyID());
    if (value < (property.getMin() == null ? Double.NEGATIVE_INFINITY : property.getMin())) {
      throw new RangeValidationException(property.getPropertyID(), value, "'" + property + "' " +
              FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_SMALL) + " " + property.getMin());
    }
    if (value > (property.getMax() == null ? Double.POSITIVE_INFINITY : property.getMax())) {
      throw new RangeValidationException(property.getPropertyID(), value, "'" + property + "' " +
              FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_LARGE) + " " + property.getMax());
    }
  }

  protected void performNullValidation(final Entity entity, final Property property, final int action) throws NullValidationException {
    if (!isNullable(entity, property.getPropertyID()) && entity.isValueNull(property.getPropertyID())) {
      if (action == EntityEditModel.INSERT) {
        if (!property.columnHasDefaultValue() || (property instanceof Property.PrimaryKeyProperty &&
                !EntityRepository.isPrimaryKeyAutoGenerated(entityID))) {
          throw new NullValidationException(property.getPropertyID(),
                  FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_IS_REQUIRED) + ": " + property);
        }
      }
      else {
        throw new NullValidationException(property.getPropertyID(),
                FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_IS_REQUIRED) + ": " + property);

      }
    }
  }

  /**
   * Returns true if the given property accepts a null value for the given entity,
   * by default this method simply returns <code>property.isNullable()</code>
   * @param valueMap the entity being validated
   * @param key the property ID
   * @return true if the property accepts a null value
   */
  public boolean isNullable(final ValueChangeMap<String, Object> valueMap, final String key) {
    return EntityRepository.getProperty(entityID, key).isNullable();
  }

  /**
   * Returns true if the last available value for this property should be used when initializing
   * a default entity for this EntityModel.
   * Override for selective reset of field values when the model is cleared.
   * For Property.ForeignKeyProperty values this method by default returns the value of the
   * property <code>Configuration.PERSIST_ENTITY_REFERENCE_VALUES</code>.
   * @param property the property
   * @return true if the given entity field value should be reset when the model is cleared
   * @see org.jminor.framework.Configuration#PERSIST_FOREIGN_KEY_VALUES
   */
  protected boolean persistValueOnClear(final Property property) {
    return property instanceof Property.ForeignKeyProperty
            && Configuration.getBooleanValue(Configuration.PERSIST_FOREIGN_KEY_VALUES);
  }

  /**
   * @return the Entity instance being edited
   */
  protected Entity getEntity() {
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
          LOG.trace(msg);
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
}