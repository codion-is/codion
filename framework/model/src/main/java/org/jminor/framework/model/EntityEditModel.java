/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Configuration;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.ValueChange;
import org.jminor.common.db.valuemap.ValueCollectionProvider;
import org.jminor.common.db.valuemap.ValueMap;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.model.Refreshable;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.value.PropertyValue;
import org.jminor.common.value.Value;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Specifies a class for editing {@link Entity} instances.
 */
public interface EntityEditModel extends Refreshable {

  /**
   * Specifies whether foreign key values should persist when the UI is cleared or be reset to null<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> PERSIST_FOREIGN_KEY_VALUES = Configuration.booleanValue("jminor.client.persistForeignKeyValues", true);

  /**
   * Indicates whether the application should ask for confirmation when exiting if some data is unsaved<br>
   * and whether it should warn when unsaved data is about to be lost due to selection changes f.ex.
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> WARN_ABOUT_UNSAVED_DATA = Configuration.booleanValue("jminor.client.warnAboutUnsavedData", false);

  /**
   * Specifies the value used by default to represent a null value in combo box models.
   * Using the value null indicates that no null value item should be used.<br>
   * Value type: String<br>
   * Default value: -
   */
  PropertyValue<String> COMBO_BOX_NULL_VALUE_ITEM = Configuration.stringValue("jminor.client.comboBoxNullValueItem", "-");

  /**
   * Specifies whether edit models post their insert, update and delete events to the {@link EntityEditEvents}<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> POST_EDIT_EVENTS = Configuration.booleanValue("jminor.client.editModelPostEditEvents", false);

  /**
   * @return the ID of the entity this edit model is based on
   */
  String getEntityId();

  /**
   * @return the connection provider used by this edit model
   */
  EntityConnectionProvider getConnectionProvider();

  /**
   * @return an Entity instance populated with default values for all properties
   * @see #getDefaultValue(Property)
   */
  Entity getDefaultEntity();

  /**
   * Copies the values from the given {@link Entity} into the underlying
   * {@link Entity} being edited by this edit model. If {@code entity}
   * is null then the entity being edited is populated with default values
   * @param entity the entity
   */
  void setEntity(Entity entity);

  /**
   * Refreshes the active Entity from the database, discarding all changes.
   * If the active Entity is new then calling this method has no effect.
   */
  void refreshEntity();

  /**
   * @return a deep copy of the active entity
   * @see org.jminor.framework.domain.Domain#copyEntity(Entity)
   */
  Entity getEntityCopy();

  /**
   * @param includePrimaryKeyValues if false then the primary key values are excluded
   * @return a deep copy of the active entity
   * @see org.jminor.framework.domain.Domain#copyEntity(Entity)
   */
  Entity getEntityCopy(boolean includePrimaryKeyValues);

  /**
   * Returns true if the active entity is new or false if it represents a row already persisted.
   * By default an entity is new if either its primary key or the original primary key are null.
   * It is not recommended to base the result of this function on a database query since it is called frequently,
   * as in, every time a property value changes.
   * @return true if the active entity is new, that is, does not represent a persistent row
   * @see #getPrimaryKeyNullObserver
   * @see org.jminor.framework.domain.Entity#isKeyNull()
   */
  boolean isEntityNew();

  /**
   * Returns true if an entity is selected and a value has been modified or if the entity is new
   * and one or more non-default values have been entered
   * @return true if this edit model contains unsaved data
   * @see EntityEditModel#WARN_ABOUT_UNSAVED_DATA
   */
  boolean containsUnsavedData();

  /**
   * @param propertyId the ID of the property
   * @return true if the value of the given property is null
   */
  boolean isNull(String propertyId);

  /**
   * @param propertyId the ID of the property
   * @return true if the value of the given property is not null
   */
  boolean isNotNull(String propertyId);

  /**
   * @param property the property
   * @return true if this value is allowed to be null in the underlying entity
   */
  boolean isNullable(Property property);

  /**
   * Sets the given value in the underlying value map
   * @param propertyId the ID of the property to associate the given value with
   * @param value the value to associate with the given property
   */
  void put(String propertyId, Object value);

  /**
   * Sets the given value in the underlying value map
   * @param property the property to associate the given value with
   * @param value the value to associate with the given property
   */
  void put(Property property, Object value);

  /**
   * Removes the given value from the underlying value map
   * @param propertyId the ID of the property
   * @return the value, if any
   */
  Object remove(String propertyId);

  /**
   * Removes the given value from the map
   * @param property the property associated with the value to remove
   * @return the value, if any
   */
  Object remove(Property property);

  /**
   * Returns the value associated with the given property
   * @param propertyId the ID of the property
   * @return the value associated with the given property
   */
  Object get(String propertyId);

  /**
   * Returns the value associated with the given property in the underlying value map
   * @param property the property of the value to retrieve
   * @return the value associated with the given property
   */
  Object get(Property property);

  /**
   * Returns the value associated with the given propertyId assuming it
   * is an {@link Entity} instance
   * @param foreignKeyPropertyId the ID of the property
   * @return the value assuming it is an {@link Entity}
   * @throws ClassCastException in case the value was not an {@link Entity}
   */
  Entity getForeignKey(String foreignKeyPropertyId);

  /**
   * For every field referencing the given foreign key values, replaces that foreign key instance with
   * the corresponding entity from {@code foreignKeyValues}, useful when property
   * values have been changed in the referenced entity that must be reflected in the edit model.
   * @param foreignKeyEntityId the entity ID of the foreign key values
   * @param foreignKeyValues the new foreign key entities
   */
  void replaceForeignKeyValues(String foreignKeyEntityId, Collection<Entity> foreignKeyValues);

  /**
   * Initializes a value provider for the given property, useful for adding lookup
   * functionality to input fields for example.
   * @param property the property
   * @return a value provider for the given property
   */
  ValueCollectionProvider getValueProvider(Property property);

  /**
   * Instantiates a new Value based on the value identified by {@code propertyId} in this edit model
   * @param propertyId the property id
   * @param <V> the value type
   * @return a Value based on the given edit model value
   */
  <V> Value<V> value(String propertyId);

  /**
   * @return the underlying domain model
   */
  Domain getDomain();

  /**
   * @return the definition of the underlying entity
   */
  EntityDefinition getEntityDefinition();

  /**
   * @return true if this model is read only, that is if insert, update and delete are not enabled
   * @see #isInsertEnabled()
   * @see #isUpdateEnabled()
   * @see #isDeleteEnabled()
   */
  boolean isReadOnly();

  /**
   * Makes this model read-only by disabling insert, update and delete
   * @param readOnly the read only status
   * @return this edit model instance
   * @see #setInsertEnabled(boolean)
   * @see #setUpdateEnabled(boolean)
   * @see #setDeleteEnabled(boolean)
   */
  EntityEditModel setReadOnly(boolean readOnly);

  /**
   * @return true if this model warns about unsaved data
   * @see #WARN_ABOUT_UNSAVED_DATA
   */
  boolean isWarnAboutUnsavedData();

  /**
   * @param warnAboutUnsavedData if true then this model warns about unsaved data
   * @return this edit model instance
   * @see #WARN_ABOUT_UNSAVED_DATA
   */
  EntityEditModel setWarnAboutUnsavedData(boolean warnAboutUnsavedData);

  /**
   * @return true if this model should enable records to be inserted
   */
  boolean isInsertEnabled();

  /**
   * @param insertEnabled true if this model should enable inserts
   * @return this edit model instance
   */
  EntityEditModel setInsertEnabled(boolean insertEnabled);

  /**
   * @return true if this model should enable records to be updated
   */
  boolean isUpdateEnabled();

  /**
   * @param updateEnabled true if this model should enable records to be updated
   * @return this edit model instance
   */
  EntityEditModel setUpdateEnabled(boolean updateEnabled);

  /**
   * @return true if this model should allow records to be deleted
   */
  boolean isDeleteEnabled();

  /**
   * @param deleteEnabled true if this model should enable records to be deleted
   * @return this edit model instance
   */
  EntityEditModel setDeleteEnabled(boolean deleteEnabled);

  /**
   * Returns true if this edit model posts its insert, update and delete events on the
   * {@link EntityEditEvents} event bus
   * @return true if insert, update and delete events are posted on the edit event bus
   */
  boolean isPostEditEvents();

  /**
   * Set to true if this edit model should post its insert, update and delete
   * events on the {@link EntityEditEvents} event bus
   * @param postEditEvents true if edit events should be posted
   * @return this edit model instance
   */
  EntityEditModel setPostEditEvents(boolean postEditEvents);

  /**
   * Creates a {@link EntityLookupModel} for looking up entities referenced by the given foreign key property,
   * using the search properties defined for that entity type, or if none are defined all string based searchable
   * properties in that entity.
   * @param foreignKeyProperty the foreign key property for which to create a {@link EntityLookupModel}
   * @return a {@link EntityLookupModel} for looking up entities of the type referenced by the given foreign key property,
   * @throws IllegalStateException in case no searchable properties can be found for the entity type referenced by the
   * given foreign key property
   */
  EntityLookupModel createForeignKeyLookupModel(ForeignKeyProperty foreignKeyProperty);

  /**
   * Returns true if this edit model contains a {@link EntityLookupModel} for the given foreign key property
   * @param foreignKeyPropertyId the ID of the property
   * @return true if a {@link EntityLookupModel} has been initialized for the given foreign key property
   */
  boolean containsLookupModel(String foreignKeyPropertyId);

  /**
   * @param foreignKeyPropertyId the ID of the property for which to retrieve the {@link EntityLookupModel}
   * @return the {@link EntityLookupModel} associated with the {@code property}, if no lookup model
   * has been initialized for the given property, a new one is created, associated with the property and returned.
   */
  EntityLookupModel getForeignKeyLookupModel(String foreignKeyPropertyId);

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the {@link EntityLookupModel}
   * @return the {@link EntityLookupModel} associated with the {@code property}, if no lookup model
   * has been initialized for the given property, a new one is created, associated with the property and returned.
   */
  EntityLookupModel getForeignKeyLookupModel(ForeignKeyProperty foreignKeyProperty);

  /**
   * Returns the default value for the given property, used when initializing a new default entity for this edit model.
   * This method is only called for properties that are non-denormalized and are not part of a foreign key.
   * If the default value of a property should be the last value used, call {@link #setValuePersistent(String, boolean)}
   * with {@code true} for the given property or override {@link #isValuePersistent} so that it
   * returns {@code true} for that property in case the value should persist.
   * @param property the property
   * @return the default value for the property
   * @see Property.Builder#setDefaultValue(Object)
   * @see #setValuePersistent(String, boolean)
   * @see #isValuePersistent(Property)
   */
  Object getDefaultValue(Property property);

  /**
   * Returns true if the last available value for this property should be used when initializing
   * a default entity.
   * Override for selective reset of field values when the model is cleared.
   * For foreign key property values this method by default returns the value of the
   * property {@link EntityEditModel#PERSIST_FOREIGN_KEY_VALUES}.
   * @param property the property
   * @return true if the given field value should be reset when the model is cleared
   * @see EntityEditModel#PERSIST_FOREIGN_KEY_VALUES
   */
  boolean isValuePersistent(Property property);

  /**
   * Returns true if values based on this property should be available for lookup via this EditModel.
   * This means displaying all the distinct property values to the user, allowing her to select one.
   * @param property the property
   * @return true if value lookup should be enabled for this property
   */
  boolean isLookupEnabled(Property property);

  /**
   * @param propertyId the property ID
   * @param persistValueOnClear true if this model should persist the value of the given property on clear
   * @return this edit model instance
   * @see EntityEditModel#PERSIST_FOREIGN_KEY_VALUES
   */
  EntityEditModel setValuePersistent(String propertyId, boolean persistValueOnClear);

  /**
   * Performs a insert on the active entity, sets the primary key values of the active entity
   * according to the primary key of the inserted entity
   * @return the inserted entity
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws ValidationException in case validation fails
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection)
   */
  Entity insert() throws DatabaseException, ValidationException;

  /**
   * Performs an insert on the given entities, returns silently on receiving an empty list
   * @param entities the entities to insert
   * @return a list containing the inserted entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws ValidationException in case validation fails
   * @see #addBeforeInsertListener(EventDataListener)
   * @see #addAfterInsertListener(EventDataListener)
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection)
   */
  List<Entity> insert(List<Entity> entities) throws DatabaseException, ValidationException;

  /**
   * Performs a update on the active entity
   * @return the updated entity
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @throws org.jminor.common.db.exception.UpdateException in case the active entity is not modified
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection)
   */
  Entity update() throws DatabaseException, ValidationException;

  /**
   * Updates the given entities. If the entities are unmodified or the list is empty
   * this method returns silently.
   * @param entities the entities to update
   * @return the updated entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @see #addBeforeUpdateListener(EventDataListener)
   * @see #addAfterUpdateListener(EventDataListener)
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection)
   */
  List<Entity> update(List<Entity> entities) throws DatabaseException, ValidationException;

  /**
   * Deletes the active entity
   * @return the deleted entity
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @see #addBeforeDeleteListener(EventDataListener)
   * @see #addAfterDeleteListener(EventDataListener)
   */
  Entity delete() throws DatabaseException;

  /**
   * Deletes the given entities, returns silently on receiving an empty list
   * @param entities the entities to delete
   * @return the deleted entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @see #addBeforeDeleteListener(EventDataListener)
   * @see #addAfterDeleteListener(EventDataListener)
   */
  List<Entity> delete(List<Entity> entities) throws DatabaseException;

  /**
   * @return true if the underlying Entity is modified
   * @see #getModifiedObserver()
   */
  boolean isModified();

  /**
   * Adds the inserted entities to all foreign key models based on that entity type
   * @param values the values
   */
  void addForeignKeyValues(List<Entity> values);

  /**
   * Removes the deleted entities from all foreign key models based on that entity type
   * todo set foreign key values referencing the deleted entity to null
   * @param values the values
   */
  void removeForeignKeyValues(List<Entity> values);

  /**
   * Sets the values in the given list as the values for the respective foreign keys, uses the first
   * value found for each entity type in case of multiple entities of that type
   * @param values the entities
   */
  void setForeignKeyValues(List<Entity> values);

  /**
   * @return the validator
   */
  Entity.Validator getValidator();

  /**
   * Checks if the value associated with the given property is valid, throws a ValidationException if not
   * @param property the property the value is associated with
   * @throws ValidationException if the given value is not valid for the given property
   */
  void validate(Property property) throws ValidationException;

  /**
   * Validates the current state of the entity
   * @throws ValidationException in case the entity is invalid
   */
  void validate() throws ValidationException;

  /**
   * Validates the current state of the given entity
   * @param entity the entity to validate
   * @throws ValidationException in case the entity is invalid
   */
  void validate(Entity entity) throws ValidationException;

  /**
   * Validates the given entities
   * @param entities the entities to validate
   * @throws ValidationException on finding the first invalid entity
   */
  void validate(Collection<Entity> entities) throws ValidationException;

  /**
   * Returns true if the value associated with the given key is valid, using the {@code validate} method
   * @param key the key the value is associated with
   * @return true if the value is valid
   * @see #validate(Property)
   * @see ValueMap.Validator#validate(ValueMap)
   */
  boolean isValid(Property key);

  /**
   * @return true if the underlying value map contains only valid values
   * @see #getValidObserver()
   */
  boolean isValid();

    /**
   * @return a StateObserver indicating the valid status of the underlying value map
   * @see #getValidator()
   * @see #isValid()
   */
  StateObserver getValidObserver();

  /**
   * Returns a StateObserver responsible for indicating when and if any values in the underlying Entity are modified.
   * @return a StateObserver indicating the modified state of this edit model
   * @see #isModified()
   * @see ValueMap#getModifiedObserver()
   */
  StateObserver getModifiedObserver();

  /**
   * @return an observer indicating whether or not the active entity is new
   * @see #isEntityNew()
   */
  StateObserver getEntityNewObserver();

  /**
   * @return the state used to determine if deleting should be enabled
   * @see #isDeleteEnabled()
   * @see #setDeleteEnabled(boolean)
   */
  StateObserver getDeleteEnabledObserver();

  /**
   * @return a {@link StateObserver} indicating whether or not the primary key of the active entity is null
   */
  StateObserver getPrimaryKeyNullObserver();

  /**
   * @return the {@link StateObserver} used to determine if updating should be enabled
   * @see #isUpdateEnabled()
   * @see #setUpdateEnabled(boolean)
   */
  StateObserver getUpdateEnabledObserver();

  /**
   * @return the {@link StateObserver} used to determine if inserting should be enabled
   * @see #isInsertEnabled()
   * @see #setInsertEnabled(boolean)
   */
  StateObserver getInsertEnabledObserver();

  /**
   * @param propertyId the ID of the property for which to retrieve the event
   * @return an EventObserver notified when the value of the given property changes
   */
  EventObserver<ValueChange<Property, Object>> getValueObserver(String propertyId);

  /**
   * Adds a listener notified each time the value associated with the given property is edited via
   * {@link #put(Property, Object)} or {@link #remove(Property)}, note that this event is only fired
   * when the value actually changes.
   * @param propertyId the ID of the property for which to monitor value edits
   * @param listener a listener notified each time the value of the given property is edited via this model
   */
  void addValueEditListener(String propertyId, EventDataListener<ValueChange<Property, Object>> listener);

  /**
   * @param propertyId the propertyId
   * @param listener the listener to remove
   */
  void removeValueEditListener(String propertyId, EventDataListener listener);

  /**
   * Adds a listener notified each time the value associated with the given key changes, either
   * via editing or when the active entity is set.
   * @param propertyId the ID of the property for which to monitor value changes
   * @param listener a listener notified each time the value of the property identified by {@code propertyId} changes
   * @see #setEntity(Entity)
   */
  void addValueListener(String propertyId, EventDataListener<ValueChange<Property, Object>> listener);

  /**
   * @param propertyId the ID of the property for which to remove the listener
   * @param listener the listener to remove
   */
  void removeValueListener(String propertyId, EventDataListener listener);

  /**
   * @param listener a listener notified each time the entity is set
   * @see #setEntity(Entity)
   */
  void addEntitySetListener(EventDataListener<Entity> listener);

  /**
   * @param listener the listener to remove
   */
  void removeEntitySetListener(EventDataListener listener);

  /**
   * @param listener a listener to be notified before an insert is performed
   */
  void addBeforeInsertListener(EventDataListener<List<Entity>> listener);

  /**
   * @param listener a listener to remove
   */
  void removeBeforeInsertListener(EventDataListener listener);

  /**
   * @param listener a listener to be notified each time a insert has been performed
   */
  void addAfterInsertListener(EventDataListener<List<Entity>> listener);

  /**
   * @param listener a listener to remove
   */
  void removeAfterInsertListener(EventDataListener listener);

  /**
   * @param listener a listener to be notified before an update is performed
   */
  void addBeforeUpdateListener(EventDataListener<Map<Entity.Key, Entity>> listener);

  /**
   * @param listener a listener to remove
   */
  void removeBeforeUpdateListener(EventDataListener listener);

  /**
   * @param listener a listener to be notified each time an update has been performed,
   * with the updated entities, mapped to their respective original primary keys, that is,
   * the primary keys before the update was performed
   */
  void addAfterUpdateListener(EventDataListener<Map<Entity.Key, Entity>> listener);

  /**
   * @param listener a listener to remove
   */
  void removeAfterUpdateListener(EventDataListener listener);

  /**
   * @param listener a listener to be notified before a delete is performed
   */
  void addBeforeDeleteListener(EventDataListener<List<Entity>> listener);

  /**
   * @param listener a listener to remove
   */
  void removeBeforeDeleteListener(EventDataListener listener);

  /**
   * @param listener a listener to be notified each time a delete has been performed
   */
  void addAfterDeleteListener(EventDataListener<List<Entity>> listener);

  /**
   * @param listener a listener to remove
   */
  void removeAfterDeleteListener(EventDataListener listener);

  /**
   * @param listener a listener to be notified before a refresh is performed
   */
  void addBeforeRefreshListener(EventListener listener);

  /**
   * @param listener a listener to remove
   */
  void removeBeforeRefreshListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time a refresh has been performed
   */
  void addAfterRefreshListener(EventListener listener);

  /**
   * @param listener a listener to remove
   */
  void removeAfterRefreshListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time a entity is modified via this model,
   * updated, inserted or deleted
   */
  void addEntitiesChangedListener(EventListener listener);

  /**
   * @param listener a listener to remove
   */
  void removeEntitiesChangedListener(EventListener listener);

  /**
   * @param listener a listener notified each time the active entity is about to be set
   */
  void addConfirmSetEntityObserver(EventDataListener<State> listener);

  /**
   * @param listener a listener to remove
   */
  void removeConfirmSetEntityObserver(EventDataListener listener);
}
