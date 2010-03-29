/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.DbException;
import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.client.model.exception.ValidationException;
import org.jminor.framework.db.criteria.CriteriaUtil;
import org.jminor.framework.db.exception.EntityModifiedException;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.apache.log4j.Logger;

import javax.swing.ComboBoxModel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for editing a Entity instance, providing property change events and combobox models
 */
public class EntityEditModel {

  protected static final Logger log = Util.getLogger(EntityEditModel.class);

  /**
   * Code for the insert action, used during validation
   */
  public static final int INSERT = 1;

  /**
   * Code for the update action, used during validation
   */
  public static final int UPDATE = 2;

  /**
   * Code for an unknown action, used during validation
   */
  public static final int UNKNOWN = 3;

  private final Event evtBeforeInsert = new Event();
  private final Event evtAfterInsert = new Event();
  private final Event evtBeforeUpdate = new Event();
  private final Event evtAfterUpdate = new Event();
  private final Event evtBeforeDelete = new Event();
  private final Event evtAfterDelete = new Event();
  private final Event evtEntityChanged = new Event();
  private final Event evtEntitiesChanged = new Event();
  private final Event evtModelCleared = new Event();

  private final State stEntityNull = new State(true);
  private final State stAllowInsert = new State(true);
  private final State stAllowUpdate = new State(true);
  private final State stAllowDelete = new State(true);

  /**
   * The entity used for editing
   */
  protected final Entity entity;

  /**
   * The EntityDbProvider instance to use when populating combo boxes and such
   */
  private final EntityDbProvider dbProvider;

  /**
   * Holds the ComboBoxModels used by this EntityModel, those that implement Refreshable
   * are refreshed when refreshComboBoxModels() is called
   * @see org.jminor.common.model.Refreshable
   */
  private final Map<Property, ComboBoxModel> propertyComboBoxModels = new HashMap<Property, ComboBoxModel>();

  /**
   * Holds events signaling property changes made to the active entity via the ui
   */
  private final Map<Property, Event> propertyValueSetEventMap = new HashMap<Property, Event>();

  /**
   * Holds events signaling property changes made to the active entity, via the model or ui
   */
  private final Map<Property, Event> propertyChangeEventMap = new HashMap<Property, Event>();

  /**
   * Instantiates a new EntityEditModel based on the entity identified by <code>entityID</code>.   *
   * @param entityID the ID of the entity to base this EntityEditModel on
   * @param dbProvider the EntityDbProvider instance used when populating ComboBoxModels
   */
  public EntityEditModel(final String entityID, final EntityDbProvider dbProvider) {
    if (entityID == null)
      throw new IllegalArgumentException("entityID is null");
    if (dbProvider == null)
      throw new IllegalArgumentException("dbProvider is null");
    this.dbProvider = dbProvider;
    this.entity = new Entity(entityID);
    this.entity.setAs(getDefaultEntity());
    bindEventsInternal();
    bindEvents();
  }

  /**
   * @return true if this model is read only,
   * by default this returns the isReadOnly value of the underlying entity
   */
  public boolean isReadOnly() {
    return EntityRepository.isReadOnly(getEntityID());
  }

  /**
   * @return true if this model allows multiple entities to be updated at a time
   */
  public boolean isMultipleUpdateAllowed() {
    return true;
  }

  /**
   * @return true if this model should allow records to be inserted
   */
  public boolean isInsertAllowed() {
    return stAllowInsert.isActive();
  }

  /**
   * @param value true if this model should allow inserts
   */
  public void setInsertAllowed(final boolean value) {
    stAllowInsert.setActive(value);
  }

  /**
   * @return the state used to determine if inserting should be enabled
   * @see #isInsertAllowed()
   * @see #setInsertAllowed(boolean)
   */
  public State stateAllowInsert() {
    return stAllowInsert;
  }

  /**
   * @return true if this model should allow records to be updated
   */
  public boolean isUpdateAllowed() {
    return stAllowUpdate.isActive();
  }

  /**
   * @param value true if this model should allow records to be updated
   */
  public void setUpdateAllowed(final boolean value) {
    stAllowUpdate.setActive(value);
  }

  /**
   * @return the state used to determine if updating should be enabled
   * @see #isUpdateAllowed()
   * @see #setUpdateAllowed(boolean)
   */
  public State stateAllowUpdate() {
    return stAllowUpdate;
  }

  /**
   * @return true if this model should allow records to be deleted
   */
  public boolean isDeleteAllowed() {
    return stAllowDelete.isActive();
  }

  /**
   * @param value true if this model should allow records to be deleted
   */
  public void setDeleteAllowed(final boolean value) {
    stAllowDelete.setActive(value);
  }

  /**
   * @return the state used to determine if deleting should be enabled
   * @see #isDeleteAllowed()
   * @see #setDeleteAllowed(boolean)
   */
  public State stateAllowDelete() {
    return stAllowDelete;
  }

  /**
   * @return the ID of the entity this EntityEditModel is based on
   */
  public String getEntityID() {
    return entity.getEntityID();
  }

  /**
   * @return the EntityDbProvider instance used by this EntityEditModel
   */
  public EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /**
   * @return a State indicating whether or not the active entity is null
   */
  public State getEntityNullState() {
    return stEntityNull.getLinkedState();
  }

  /**
   * @return true if the active entity is null
   * @see org.jminor.framework.domain.Entity#isNull()
   */
  public boolean isEntityNull() {
    return entity.isNull();
  }

  /**
   * Clears the model by setting the active entity to null
   * @see #evtModelCleared
   */
  public final void clear() {
    setEntity(null);
    evtModelCleared.fire();
  }

  /**
   * @return a deep copy of the active entity
   * @see org.jminor.framework.domain.Entity#getCopy()
   */
  public Entity getEntityCopy() {
    return getEntityCopy(true);
  }

  /**
   * @param includePrimaryKeyValues if true then the primary key values are include
   * @return a deep copy of the active entity
   * @see org.jminor.framework.domain.Entity#getCopy()
   */
  public Entity getEntityCopy(final boolean includePrimaryKeyValues) {
    final Entity copy = entity.getCopy();
    if (!includePrimaryKeyValues) {
      for (final Property.PrimaryKeyProperty property : EntityRepository.getPrimaryKeyProperties(copy.getEntityID()))
        copy.setValue(property, null, false);
    }

    return copy;
  }

  /**
   * @return the state which indicates the modified state of the active entity
   * @see org.jminor.framework.domain.Entity#getModifiedState()
   */
  public State getEntityModifiedState() {
    return entity.getModifiedState();
  }

  /**
   * @return true if the active entity has been modified
   * @see org.jminor.framework.domain.Entity#isModified()
   */
  public boolean isEntityModified() {
    return getEntityModifiedState().isActive();
  }

  /**
   * @return an Event which fires when the underlying entities have changed, via insert, update or delete
   */
  public Event getEntityChangedEvent() {
    return evtEntityChanged;
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the event
   * @return an Event object which fires when the value of property <code>propertyID</code> is changed via
   * the <code>setValue()</code> methods
   * @see #setValue(String, Object)
   * @see #setValue(org.jminor.framework.domain.Property, Object)
   */
  public Event getPropertyValueSetEvent(final String propertyID) {
    return getPropertyValueSetEvent(EntityRepository.getProperty(getEntityID(), propertyID));
  }

  /**
   * @param property the property for which to retrieve the event
   * @return an Event object which fires when the value of <code>property</code> is changed via
   * the <code>setValue()</code> methods
   * @see #setValue(String, Object)
   * @see #setValue(org.jminor.framework.domain.Property, Object)
   */
  public Event getPropertyValueSetEvent(final Property property) {
    if (!propertyValueSetEventMap.containsKey(property))
      propertyValueSetEventMap.put(property, new Event());

    return propertyValueSetEventMap.get(property);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the event
   * @return an Event object which fires when the value of the property identified by <code>propertyID</code> changes
   */
  public Event getPropertyChangeEvent(final String propertyID) {
    return getPropertyChangeEvent(EntityRepository.getProperty(getEntityID(), propertyID));
  }

  /**
   * @param property the property for which to retrieve the event
   * @return an Event object which fires when the value of <code>property</code> changes
   */
  public Event getPropertyChangeEvent(final Property property) {
    if (!propertyChangeEventMap.containsKey(property))
      propertyChangeEventMap.put(property, new Event());

    return propertyChangeEventMap.get(property);
  }

  /**
   * Performs a insert on the active entity
   * @throws org.jminor.common.db.DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws ValidationException in case validation fails
   * @see #validateEntities(java.util.List, int)
   */
  public final void insert() throws CancelException, DbException, ValidationException {
    insert(Arrays.asList(getEntityCopy(!EntityRepository.isPrimaryKeyAutoGenerated(getEntityID()))));
  }

  /**
   * Performs an insert on the given entities
   * @param entities the entities to insert
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws ValidationException in case validation fails
   * @see #evtBeforeInsert
   * @see #evtAfterInsert
   * @see #validateEntities(java.util.List, int)
   */
  public final void insert(final List<Entity> entities) throws CancelException, DbException, ValidationException {
    if (isReadOnly())
      throw new RuntimeException("This is a read-only model, inserting is not allowed!");
    if (!isInsertAllowed())
      throw new RuntimeException("This model does not allow inserting!");

    log.debug(toString() + " - insert " + Util.getListContentsAsString(entities, false));

    evtBeforeInsert.fire();
    validateEntities(entities, INSERT);

    final List<Entity.Key> primaryKeys = Entity.Key.copyEntityKeys(doInsert(entities));
    evtAfterInsert.fire(new InsertEvent(this, primaryKeys));
  }

  /**
   * Performs a update on the active entity
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws org.jminor.framework.db.exception.EntityModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @see #validateEntities(java.util.List, int)
   */
  public final void update() throws CancelException, DbException, EntityModifiedException, ValidationException {
    update(Arrays.asList(getEntityCopy()));
  }

  /**
   * Updates the given Entities. If the entities are unmodified this method returns silently.
   * @param entities the Entities to update
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws EntityModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @see #evtBeforeUpdate
   * @see #evtAfterUpdate
   * @see #validateEntities(java.util.List, int)
   */
  public final void update(final List<Entity> entities) throws DbException, EntityModifiedException, CancelException, ValidationException {
    if (isReadOnly())
      throw new RuntimeException("This is a read-only model, updating is not allowed!");
    if (!isMultipleUpdateAllowed() && entities.size() > 1)
      throw new RuntimeException("Update of multiple entities is not allowed!");
    if (!isUpdateAllowed())
      throw new RuntimeException("This model does not allow updating!");

    log.debug(toString() + " - update " + Util.getListContentsAsString(entities, false));

    final List<Entity> modifiedEntities = EntityUtil.getModifiedEntities(entities);
    if (modifiedEntities.size() == 0)
      return;

    evtBeforeUpdate.fire();
    validateEntities(modifiedEntities, UPDATE);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);

    evtAfterUpdate.fire(new UpdateEvent(this, updatedEntities, Entity.isPrimaryKeyModified(modifiedEntities)));
  }

  /**
   * Deletes the active entity
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @see #evtBeforeDelete
   * @see #evtAfterDelete
   */
  public final void delete() throws DbException, CancelException {
    delete(Arrays.asList(getEntityCopy()));
  }

  /**
   * Deletes the given entities
   * @param entities the entities to delete
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @see #evtBeforeDelete
   * @see #evtAfterDelete
   */
  public final void delete(final List<Entity> entities) throws DbException, CancelException {
    if (isReadOnly())
      throw new RuntimeException("This is a read-only model, deleting is not allowed!");
    if (!isDeleteAllowed())
      throw new RuntimeException("This model does not allow deleting!");

    log.debug(toString() + " - delete " + Util.getListContentsAsString(entities, false));

    evtBeforeDelete.fire();

    doDelete(entities);

    evtAfterDelete.fire(new DeleteEvent(this, entities));
  }

  /**
   * Returns true if the given value is valid for the given property, using the <code>validate</code> method
   * @param property the property
   * @return true if the value is valid
   * @see #validate(org.jminor.framework.domain.Entity,org.jminor.framework.domain.Property,int)
   * @see #validate(org.jminor.framework.domain.Property,int)
   */
  public final boolean isValid(final Property property) {
    try {
      validate(property, isEntityNull() ? INSERT : UPDATE);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  /**
   * Validates the given Entity objects.
   * @param entities the entities to validate
   * @param action describes the action requiring validation,
   * EntityEditModel.INSERT, EntityEditModel.UPDATE or EntityEditModel.UNKNOWN
   * @throws ValidationException in case the validation fails
   * @see EntityEditModel#validate(org.jminor.framework.domain.Entity,org.jminor.framework.domain.Property,int)
   * @see EntityEditModel#validate(org.jminor.framework.domain.Property,int)
   * @see #INSERT
   * @see #UPDATE
   * @see #UNKNOWN
   */
  @SuppressWarnings({"UnusedDeclaration"})
  public void validateEntities(final List<Entity> entities, final int action) throws ValidationException {
    for (final Entity entity : entities) {
      for (final Property property : EntityRepository.getProperties(entity.getEntityID()).values()) {
        validate(entity, property, action);
      }
    }
  }

  /**
   * Checks if the value of the given property is valid, throws a ValidationException if not,
   * this default implementation performs a null value validation if the corresponding configuration parameter is set
   * @param property the property
   * @param action describes the action requiring validation,
   * EntityEditModel.INSERT, EntityEditModel.UPDATE or EntityEditModel.UNKNOWN
   * @throws ValidationException if the given value is not valid for the given property
   * @see Property#setNullable(boolean)
   * @see Configuration#PERFORM_NULL_VALIDATION
   */
  public void validate(final Property property, final int action) throws ValidationException {
    validate(entity, property, action);
  }

  /**
   * Checks if the value of the given property is valid, throws a ValidationException if not,
   * this default implementation performs a null value validation if the corresponding configuration parameter is set
   * @param entity the entity to validate
   * @param property the property
   * @param action describes the action requiring validation,
   * EntityEditModel.INSERT, EntityEditModel.UPDATE or EntityEditModel.UNKNOWN
   * @throws ValidationException if the given value is not valid for the given property
   * @see Property#setNullable(boolean)
   * @see Configuration#PERFORM_NULL_VALIDATION
   */
  public void validate(final Entity entity, final Property property, final int action) throws ValidationException {
    if (Configuration.getBooleanValue(Configuration.PERFORM_NULL_VALIDATION)) {
      if (!isPropertyNullable(entity, property) && entity.isValueNull(property.getPropertyID())) {
        if (action == UPDATE)
          throw new ValidationException(property, null,
                  FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_IS_REQUIRED) + ": " + property);
        else if (action == INSERT) {
          if (!property.columnHasDefaultValue() || (property instanceof Property.PrimaryKeyProperty &&
                  !EntityRepository.isPrimaryKeyAutoGenerated(getEntityID())))
            throw new ValidationException(property, null,
                    FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_IS_REQUIRED) + ": " + property);
        }
      }
    }
  }

  /**
   * Refreshes the Refreshable ComboBoxModels associated with this EntityModel
   * @see org.jminor.common.model.Refreshable
   */
  public void refreshComboBoxModels() {
    for (final ComboBoxModel comboBoxModel : propertyComboBoxModels.values())
      if (comboBoxModel instanceof Refreshable)
        ((Refreshable) comboBoxModel).refresh();
  }

  /**
   * @param property the property for which to get the ComboBoxModel
   * @return a PropertyComboBoxModel representing <code>property</code>
   * @throws RuntimeException if no combo box has been initialized for the given property
   */
  public PropertyComboBoxModel getPropertyComboBoxModel(final Property property) {
    final PropertyComboBoxModel comboBoxModel = (PropertyComboBoxModel) propertyComboBoxModels.get(property);
    if (comboBoxModel == null)
      throw new RuntimeException("No PropertyComboBoxModel has been initialized for property: " + property);

    return comboBoxModel;
  }

  /**
   * @param property the property for which to get the ComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires,
   * if none is specified EntityModel.eventEntitiesChanged is used
   * @param nullValue the value to use for representing the null item at the top of the list,
   * if this value is null then no such item is included
   * @return a PropertyComboBoxModel representing <code>property</code>, if no combo box model
   * has been initialized for the given property, a new one is created and associated with
   * the property, to be returned the next time this method is called
   */
  public PropertyComboBoxModel initializePropertyComboBoxModel(final Property property, final Event refreshEvent,
                                                               final String nullValue) {
    PropertyComboBoxModel comboBoxModel = (PropertyComboBoxModel) propertyComboBoxModels.get(property);
    if (comboBoxModel == null) {
      setComboBoxModel(property, comboBoxModel = createPropertyComboBoxModel(property,
              refreshEvent == null ? evtEntitiesChanged : refreshEvent, nullValue));
      comboBoxModel.refresh();
    }

    return comboBoxModel;
  }

  /**
   * @param property the property for which to get the ComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires,
   * if none is specified EntityModel.eventEntitiesChanged is used
   * @param nullValue the value to use for representing the null item at the top of the list,
   * if this value is null then no such item is included
   * @return a new PropertyComboBoxModel based on the given property
   */
  public PropertyComboBoxModel createPropertyComboBoxModel(final Property property, final Event refreshEvent,
                                                           final String nullValue) {
    return new PropertyComboBoxModel(getEntityID(), property, getDbProvider(), nullValue, refreshEvent);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel for the property identified by <code>propertyID</code>,
   * if no combo box model is associated with the property a new one is initialized, and associated
   * with the given property
   * @throws RuntimeException if no combo box has been initialized for the given property
   */
  public EntityComboBoxModel getEntityComboBoxModel(final String propertyID) {
    final Property property = EntityRepository.getProperty(getEntityID(), propertyID);
    if (!(property instanceof Property.ForeignKeyProperty))
      throw new IllegalArgumentException("EntityComboBoxModels are only available for Property.ForeignKeyProperty");

    return getEntityComboBoxModel((Property.ForeignKeyProperty) property);
  }

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel associated with the <code>property</code>
   * @throws RuntimeException if no combo box has been initialized for the given property
   */
  public EntityComboBoxModel getEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    final EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null)
      throw new RuntimeException("No EntityComboBoxModel has been initialized for property: " + foreignKeyProperty);

    return comboBoxModel;
  }

  /**
   * @param propertyID the ID of the foreign key property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel for the <code>property</code>,
   * if no combo box model is associated with the property a new one is initialized, and associated
   * with the given property
   */
  public EntityComboBoxModel initializeEntityComboBoxModel(final String propertyID) {
    final Property property = EntityRepository.getProperty(getEntityID(), propertyID);
    if (!(property instanceof Property.ForeignKeyProperty))
      throw new IllegalArgumentException("EntityComboBoxModels are only available for Property.ForeignKeyProperty");

    return initializeEntityComboBoxModel((Property.ForeignKeyProperty) property);
  }

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel for the <code>property</code>,
   * if no combo box model is associated with the property a new one is initialized, and associated
   * with the given property
   */
  public EntityComboBoxModel initializeEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null)
      setComboBoxModel(foreignKeyProperty, comboBoxModel = createEntityComboBoxModel(foreignKeyProperty));

    return comboBoxModel;
  }

  /**
   * Returns true if this edit model contains a ComboBoxModel for the given property
   * @param propertyID the ID of the property
   * @return true if a ComboBoxModel has been initialized for the given property
   */
  public boolean containsComboBoxModel(final String propertyID) {
    return containsComboBoxModel(EntityRepository.getProperty(getEntityID(), propertyID));
  }

  /**
   * Returns true if this edit model contains a ComboBoxModel for the given property
   * @param property the property
   * @return true if a ComboBoxModel has been initialized for the given property
   */
  public boolean containsComboBoxModel(final Property property) {
    return propertyComboBoxModels.containsKey(property);
  }

  /**
   * Sets the active entity, that is, the entity to be edited
   * @param entity the entity to set as active, if null then the default entity value is set as active
   * @see #evtEntityChanged
   * @see #getDefaultEntity()
   */
  public final void setEntity(final Entity entity) {
    if (entity != null && this.entity.propertyValuesEqual(entity))
      return;

    this.entity.setAs(entity == null ? getDefaultEntity() : entity);
    stEntityNull.setActive(this.entity.isNull());
    evtEntityChanged.fire();
  }

  /**
   * Sets the value of the property with name <code>propertyID</code> in the active entity to <code>value</code>,
   * basic type validation is performed.
   * @param propertyID the ID of the property to update
   * @param value the new value
   */
  public void setValue(final String propertyID, final Object value) {
    setValue(EntityRepository.getProperty(getEntityID(), propertyID), value);
  }

  /**
   * Sets the value of <code>property</code> in the active entity to <code>value</code>
   * @param property the property to update
   * @param value the new value
   */
  public void setValue(final Property property, final Object value) {
    final Object oldValue = getValue(property);
    final Object newValue = doSetValue(property, value, true);

    if (!Util.equal(newValue, oldValue))
      notifyPropertyValueSet(new Property.Event(this, getEntityID(), property, newValue, oldValue, false, false));
  }

  /**
   * @param propertyID the property identifier
   * @return true if the value of the given property is null
   */
  public boolean isValueNull(final String propertyID) {
    return entity.isValueNull(propertyID);
  }

  /**
   * @param property the property for which to retrieve the value
   * @return the value associated with <code>property</code>
   */
  public Object getValue(final Property property) {
    return getValue(property.getPropertyID());
  }

  /**
   * @param propertyID the id of the property for which to retrieve the value
   * @return the value associated with the property identified by <code>propertyID</code>
   */
  public Object getValue(final String propertyID) {
    return entity.getValue(propertyID);
  }

  /**
   * @param propertyID the id of the property for which to retrieve the value
   * @return the value associated with the property identified by <code>propertyID</code>
   */
  public Entity getEntityValue(final String propertyID) {
    return entity.getEntityValue(propertyID);
  }

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the value
   * @return the value associated with <code>property</code>
   */
  public Entity getEntityValue(final Property.ForeignKeyProperty foreignKeyProperty) {
    return getEntityValue(foreignKeyProperty.getPropertyID());
  }

  /**
   * Creates a default EntityComboBoxModel for the given property, override to provide
   * specific EntityComboBoxModels (filtered for example) for properties.
   * This method is called when creating a EntityComboBoxModel for entity properties, both
   * for the edit fields used when editing a single record and the edit field used
   * when updating multiple records.
   * This default implementation returns a sorted EntityComboBoxModel with the default nullValueItem
   * if the underlying property is nullable
   * @param foreignKeyProperty the foreign key property for which to create a EntityComboBoxModel
   * @return a EntityComboBoxModel for the given property
   * @see Configuration#DEFAULT_COMBO_BOX_NULL_VALUE_ITEM
   * @see org.jminor.framework.domain.Property#isNullable()
   */
  public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    return new EntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), getDbProvider(), false,
            isPropertyNullable(entity, foreignKeyProperty) ?
                    (String) Configuration.getValue(Configuration.DEFAULT_COMBO_BOX_NULL_VALUE_ITEM) : null, true);
  }

  /**
   * Creates a EntityLookupModel for the given entityID
   * @param entityID the ID of the entity
   * @param additionalSearchCriteria an additional search criteria applied when performing the lookup
   * @param lookupProperties the properties involved in the lookup
   * @return a EntityLookupModel
   */
  public EntityLookupModel createEntityLookupModel(final String entityID, final Criteria additionalSearchCriteria,
                                                   final List<Property> lookupProperties) {
    return new EntityLookupModel(entityID, getDbProvider(), additionalSearchCriteria, lookupProperties);
  }

  /**
   * If this method is overridden then calling super.getDefaultEntity() would be proper
   * @return the default entity for this EntityModel, it is set as active when no item is selected
   * @see #getDefaultValue(org.jminor.framework.domain.Property)
   */
  public Entity getDefaultEntity() {
    final Entity defaultEntity = new Entity(getEntityID());
    for (final Property property : EntityRepository.getDatabaseProperties(getEntityID()))
      if (!property.hasParentProperty() && !(property instanceof Property.DenormalizedProperty))//these are set via their respective parent properties
        defaultEntity.setValue(property, getDefaultValue(property), true);

    return defaultEntity;
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
    return persistValueOnClear(property) ? getValue(property) : property.getDefaultValue();
  }

  /**
   * @return an Event fired after a successful delete
   */
  public Event eventAfterDelete() {
    return evtAfterDelete;
  }

  /**
   * @return an Event fired after a successful insert
   */
  public Event eventAfterInsert() {
    return evtAfterInsert;
  }

  /**
   * @return an Event fired after a successful update
   */
  public Event eventAfterUpdate() {
    return evtAfterUpdate;
  }

  /**
   * @return an Event fired before a delete
   */
  public Event eventBeforeDelete() {
    return evtBeforeDelete;
  }

  /**
   * @return an Event fired before a insert
   */
  public Event eventBeforeInsert() {
    return evtBeforeInsert;
  }

  /**
   * @return an Event fired before a update
   */
  public Event eventBeforeUpdate() {
    return evtBeforeUpdate;
  }

  /**
   * @return an Event fired when the underlying table has undergone changes,
   * such as insert, update or delete
   */
  public Event eventEntitiesChanged() {
    return evtEntitiesChanged;
  }

  /**
   * @return an Event fired when the active entity has been changed
   */
  public Event eventEntityChanged() {
    return evtEntityChanged;
  }

  /**
   * @return an Event fired when the model has been cleared
   */
  public Event eventModelCleared() {
    return evtModelCleared;
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
      return getDbProvider().getEntityDb().insert(entities);
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
      return getDbProvider().getEntityDb().update(entities);
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
      getDbProvider().getEntityDb().delete(CriteriaUtil.criteria((EntityUtil.getPrimaryKeys(entities))));
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns true if the given property accepts a null value, by default this
   * method simply returns <code>property.isNullable()</code>
   * @param entity the entity being validated
   * @param property the property
   * @return true if the property accepts a null value
   */
  protected boolean isPropertyNullable(final Entity entity, final Property property) {
    return property.isNullable();
  }

  /**
   * Sets the value in the underlying entity
   * @param property the property for which to set the value
   * @param value the value
   * @param validateType if true then type validation should be performed
   * @return the value that was just set
   */
  protected Object doSetValue(final Property property, final Object value, final boolean validateType) {
    entity.setValue(property.getPropertyID(), value, validateType);

    return value;
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
   * Override to add event bindings
   */
  protected void bindEvents() {}

  private void bindEventsInternal() {
    evtAfterDelete.addListener(evtEntitiesChanged);
    evtAfterInsert.addListener(evtEntitiesChanged);
    evtAfterUpdate.addListener(evtEntitiesChanged);
    entity.addPropertyListener(new Property.Listener() {
      @Override
      protected void propertyChanged(final Property.Event event) {
        final Event propertyEvent = propertyChangeEventMap.get(event.getProperty());
        if (propertyEvent != null)
          propertyEvent.fire(event);
      }
    });
    if (Configuration.getBooleanValue(Configuration.PROPERTY_DEBUG_OUTPUT)) {
      entity.addPropertyListener(new Property.Listener() {
        @Override
        protected void propertyChanged(final Property.Event event) {
          final String msg = getPropertyChangeDebugString(event);
          System.out.println(msg);
          log.trace(msg);
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
    if (propertyComboBoxModels.containsKey(property))
      throw new RuntimeException("ComboBoxModel already associated with property: " + property);

    propertyComboBoxModels.put(property, model);
  }

  private void notifyPropertyValueSet(final Property.Event event) {
    if (Configuration.getBooleanValue(Configuration.PROPERTY_DEBUG_OUTPUT)) {
      final String msg = getPropertyChangeDebugString(event);
      System.out.println(msg);
      log.trace(msg);
    }
    getPropertyValueSetEvent(event.getProperty()).fire(event);
  }

  private static String getPropertyChangeDebugString(final Property.Event event) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (event.getSource() instanceof Entity)
      stringBuilder.append("[entity] ");
    else
      stringBuilder.append(event.isModelChange() ? "[model] " : "[ui] ");
    stringBuilder.append(event.getEntityID()).append(" : ").append(event.getProperty()).append(
            event.getProperty().hasParentProperty() ? " [fk]" : "").append("; ");
    if (!event.isInitialization()) {
      if (event.getOldValue() != null)
        stringBuilder.append(event.getOldValue().getClass().getSimpleName()).append(" ");
      stringBuilder.append(getValueString(event.getProperty(), event.getOldValue()));
    }
    if (!event.isInitialization())
      stringBuilder.append(" -> ");
    if (event.getNewValue() != null)
      stringBuilder.append(event.getNewValue().getClass().getSimpleName()).append(" ");
    stringBuilder.append(getValueString(event.getProperty(), event.getNewValue()));

    return stringBuilder.toString();
  }

  /**
   * @param property the property
   * @param value the value
   * @return a string representing the given property value for debug output
   */
  private static String getValueString(final Property property, final Object value) {
    final boolean valueIsNull = Entity.isValueNull(property.getPropertyType(), value);
    final StringBuilder stringBuilder = new StringBuilder("[").append(valueIsNull
            ? (value == null ? "null" : "null value") : value).append("]");
    if (value instanceof Entity)
      stringBuilder.append(" PK{").append(((Entity)value).getPrimaryKey()).append("}");

    return stringBuilder.toString();
  }
}