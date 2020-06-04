/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.Refreshable;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.PropertyValue;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

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
  PropertyValue<Boolean> PERSIST_FOREIGN_KEY_VALUES = Configuration.booleanValue("codion.client.persistForeignKeyValues", true);

  /**
   * Indicates whether the application should ask for confirmation when exiting if some data is unsaved<br>
   * and whether it should warn when unsaved data is about to be lost, i.e. due to selection changes.
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> WARN_ABOUT_UNSAVED_DATA = Configuration.booleanValue("codion.client.warnAboutUnsavedData", false);

  /**
   * Specifies the value used by default to represent a null value in combo box models.
   * Using the value null indicates that no null value item should be used.<br>
   * Value type: String<br>
   * Default value: -
   */
  PropertyValue<String> COMBO_BOX_NULL_VALUE_ITEM = Configuration.stringValue("codion.client.comboBoxNullValueItem", "-");

  /**
   * Specifies whether edit models post their insert, update and delete events to the {@link EntityEditEvents}<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> POST_EDIT_EVENTS = Configuration.booleanValue("codion.client.editModelPostEditEvents", false);

  /**
   * @return the type of the entity this edit model is based on
   */
  EntityType getEntityType();

  /**
   * @return the connection provider used by this edit model
   */
  EntityConnectionProvider getConnectionProvider();

  /**
   * @return an Entity instance populated with default values for all properties
   * @see #getDefaultValue(Attribute)
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
   * @see is.codion.framework.domain.entity.Entities#copyEntity(Entity)
   */
  Entity getEntityCopy();

  /**
   * Returns true if the active entity is new or false if it represents a row already persisted.
   * By default an entity is new if either its primary key or the original primary key are null.
   * It is not recommended to base the result of this function on a database query since it is called frequently,
   * as in, every time a property value changes.
   * @return true if the active entity is new, that is, does not represent a persistent row
   * @see #getPrimaryKeyNullObserver
   * @see Entity.Key#isNull()
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
   * @param attribute the attribute
   * @return true if the value of the given property is null
   */
  boolean isNull(Attribute<?> attribute);

  /**
   * @param attribute the attribute
   * @return true if the value of the given property is not null
   */
  boolean isNotNull(Attribute<?> attribute);

  /**
   * @param property the property
   * @return true if this value is allowed to be null in the underlying entity
   */
  boolean isNullable(Property<?> property);

  /**
   * Sets the given value in the underlying Entity
   * @param attribute the attribute to associate the given value with
   * @param value the value to associate with the given property
   * @param <T> the value type
   */
  <T> void put(Attribute<T> attribute, T value);

  /**
   * Removes the given value from the underlying Entity
   * @param attribute the attribute
   * @param <T> the value type
   * @return the value, if any
   */
  <T> T remove(Attribute<T> attribute);

  /**
   * Returns the value associated with the given property
   * @param attribute the attribute
   * @param <T> the value type
   * @return the value associated with the given property
   */
  <T> T get(Attribute<T> attribute);

  /**
   * Returns the value associated with the given attribute assuming it
   * is an {@link Entity} instance
   * @param foreignKeyAttribute the attribute
   * @return the value assuming it is an {@link Entity}
   * @throws ClassCastException in case the value was not an {@link Entity}
   */
  Entity getForeignKey(Attribute<Entity> foreignKeyAttribute);

  /**
   * Instantiates a new Value based on {@code attribute} in this edit model
   * @param attribute the attribute
   * @param <V> the value type
   * @return a Value based on the given edit model value
   */
  <V> Value<V> value(Attribute<V> attribute);

  /**
   * @return the underlying domain entities
   */
  Entities getEntities();

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
   * @see #setInsertEnabled(boolean)
   * @see #setUpdateEnabled(boolean)
   * @see #setDeleteEnabled(boolean)
   */
  void setReadOnly(boolean readOnly);

  /**
   * @return true if this model warns about unsaved data
   * @see #WARN_ABOUT_UNSAVED_DATA
   */
  boolean isWarnAboutUnsavedData();

  /**
   * @param warnAboutUnsavedData if true then this model warns about unsaved data
   * @see #WARN_ABOUT_UNSAVED_DATA
   */
  void setWarnAboutUnsavedData(boolean warnAboutUnsavedData);

  /**
   * @return true if this model should enable records to be inserted
   */
  boolean isInsertEnabled();

  /**
   * @param insertEnabled true if this model should enable inserts
   */
  void setInsertEnabled(boolean insertEnabled);

  /**
   * @return true if this model should enable records to be updated
   */
  boolean isUpdateEnabled();

  /**
   * @param updateEnabled true if this model should enable records to be updated
   */
  void setUpdateEnabled(boolean updateEnabled);

  /**
   * @return true if this model should allow records to be deleted
   */
  boolean isDeleteEnabled();

  /**
   * @param deleteEnabled true if this model should enable records to be deleted
   */
  void setDeleteEnabled(boolean deleteEnabled);

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
   */
  void setPostEditEvents(boolean postEditEvents);

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
   * @param foreignKeyAttribute the attribute
   * @return true if a {@link EntityLookupModel} has been initialized for the given foreign key property
   */
  boolean containsLookupModel(Attribute<Entity> foreignKeyAttribute);

  /**
   * @param foreignKeyAttribute the attribute for which to retrieve the {@link EntityLookupModel}
   * @return the {@link EntityLookupModel} associated with the {@code property}, if no lookup model
   * has been initialized for the given property, a new one is created, associated with the attribute and returned.
   */
  EntityLookupModel getForeignKeyLookupModel(Attribute<Entity> foreignKeyAttribute);

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the {@link EntityLookupModel}
   * @return the {@link EntityLookupModel} associated with the {@code property}, if no lookup model
   * has been initialized for the given property, a new one is created, associated with the attribute and returned.
   */
  EntityLookupModel getForeignKeyLookupModel(ForeignKeyProperty foreignKeyProperty);

  /**
   * Returns the default value for the given property, used when initializing a new default entity for this edit model.
   * This method is only called for properties that are non-denormalized and are not part of a foreign key.
   * If the default value of a property should be the last value used, call {@link #setPersistValue(Attribute, boolean)}
   * with {@code true} for the given property or override {@link #isPersistValue} so that it
   * returns {@code true} for that property in case the value should persist.
   * @param property the property
   * @param <T> the value type
   * @return the default value for the property
   * @see Property.Builder#defaultValue(Object)
   * @see #setPersistValue(Attribute, boolean)
   * @see #isPersistValue(Property)
   */
  <T> T getDefaultValue(Attribute<T> property);

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
  boolean isPersistValue(Property<?> property);

  /**
   * @param attribute the attribute
   * @param persistValue true if this model should persist the value of the given property on clear
   * @see EntityEditModel#PERSIST_FOREIGN_KEY_VALUES
   */
  void setPersistValue(Attribute<?> attribute, boolean persistValue);

  /**
   * Performs a insert on the active entity, sets the primary key values of the active entity
   * according to the primary key of the inserted entity
   * @return the inserted entity
   * @throws is.codion.common.db.exception.DatabaseException in case of a database exception
   * @throws ValidationException in case validation fails
   * @see EntityValidator#validate(Collection, EntityDefinition)
   */
  Entity insert() throws DatabaseException, ValidationException;

  /**
   * Performs an insert on the given entities, returns silently on receiving an empty list.
   * @param entities the entities to insert
   * @return a list containing the inserted entities
   * @throws is.codion.common.db.exception.DatabaseException in case of a database exception
   * @throws ValidationException in case validation fails
   * @see #addBeforeInsertListener(EventDataListener)
   * @see #addAfterInsertListener(EventDataListener)
   * @see EntityValidator#validate(Collection, EntityDefinition)
   */
  List<Entity> insert(List<Entity> entities) throws DatabaseException, ValidationException;

  /**
   * Performs a update on the active entity
   * @return the updated entity
   * @throws is.codion.common.db.exception.DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @throws is.codion.common.db.exception.UpdateException in case the active entity is not modified
   * @see EntityValidator#validate(Collection, EntityDefinition)
   */
  Entity update() throws DatabaseException, ValidationException;

  /**
   * Updates the given entities. If the entities are unmodified or the list is empty this method returns silently.
   * @param entities the entities to update
   * @return the updated entities
   * @throws is.codion.common.db.exception.DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @see #addBeforeUpdateListener(EventDataListener)
   * @see #addAfterUpdateListener(EventDataListener)
   * @see EntityValidator#validate(Collection, EntityDefinition)
   */
  List<Entity> update(List<Entity> entities) throws DatabaseException, ValidationException;

  /**
   * Deletes the active entity
   * @return the deleted entity
   * @throws is.codion.common.db.exception.DatabaseException in case of a database exception
   * @see #addBeforeDeleteListener(EventDataListener)
   * @see #addAfterDeleteListener(EventDataListener)
   */
  Entity delete() throws DatabaseException;

  /**
   * Deletes the given entities, returns silently on receiving an empty list
   * @param entities the entities to delete
   * @return the deleted entities
   * @throws is.codion.common.db.exception.DatabaseException in case of a database exception
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
   * @param entities the values
   */
  void addForeignKeyValues(List<Entity> entities);

  /**
   * Removes the given entities from all foreign key models based on that entity type and clears any foreign
   * key values referencing them.
   * @param entities the values
   */
  void removeForeignKeyValues(List<Entity> entities);

  /**
   * For every field referencing the given foreign key values, replaces that foreign key instance with
   * the corresponding entity from {@code values}, useful when property
   * values have been changed in the referenced entity that must be reflected in the edit model.
   * @param entities the foreign key entities
   */
  void replaceForeignKeyValues(Collection<Entity> entities);

  /**
   * Sets the values in the given list as the values for the respective foreign keys, uses the first
   * value found for each entity type in case of multiple entities of that type
   * @param entities the entities
   */
  void setForeignKeyValues(Collection<Entity> entities);

  /**
   * @return the validator
   */
  EntityValidator getValidator();

  /**
   * Validates the value associated with the given property, using the underlying validator.
   * @param property the property the value is associated with
   * @throws ValidationException if the given value is not valid for the given property
   * @see #getValidator()
   */
  void validate(Property<?> property) throws ValidationException;

  /**
   * Validates the current state of the entity
   * @throws ValidationException in case the entity is invalid
   */
  void validate() throws ValidationException;

  /**
   * Validates the given entity, using the underlying validator.
   * @param entity the entity to validate
   * @throws ValidationException in case the entity is invalid
   * @see #getValidator()
   */
  void validate(Entity entity) throws ValidationException;

  /**
   * Validates the given entities, using the underlying validator.
   * @param entities the entities to validate
   * @throws ValidationException on finding the first invalid entity
   * @see #getValidator()
   */
  void validate(Collection<Entity> entities) throws ValidationException;

  /**
   * Returns true if the value associated with the given property is valid, using the {@code validate} method.
   * @param property the property the value is associated with
   * @return true if the value is valid
   * @see #validate(Property)
   * @see EntityValidator#validate(Entity, EntityDefinition)
   */
  boolean isValid(Property<?> property);

  /**
   * @return true if the underlying Entity contains only valid values
   * @see #getValidObserver()
   */
  boolean isValid();

  /**
   * @return a StateObserver indicating the valid status of the underlying Entity.
   * @see #getValidator()
   * @see #isValid()
   */
  StateObserver getValidObserver();

  /**
   * Returns a StateObserver responsible for indicating when and if any values in the underlying Entity have been modified.
   * @return a StateObserver indicating the modified state of this edit model
   * @see #isModified()
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
   * Adds a listener notified each time the value associated with the given attribute is edited via
   * {@link #put(Attribute, Object)} or {@link #remove(Attribute)}, note that this event is only fired
   * when the value actually changes.
   * @param attribute the attribute for which to monitor value edits
   * @param listener a listener notified each time the value of the given property is edited via this model
   * @param <T> the value type
   */
  <T> void addValueEditListener(Attribute<T> attribute, EventDataListener<ValueChange<T>> listener);

  /**
   * Removes the given listener.
   * @param attribute the attribute
   * @param listener the listener to remove
   * @param <T> the value type
   */
  <T> void removeValueEditListener(Attribute<T> attribute, EventDataListener<ValueChange<T>> listener);

  /**
   * Adds a listener notified each time the value associated with the given attribute changes, either
   * via editing or when the active entity is set.
   * @param attribute the attribute for which to monitor value changes
   * @param listener a listener notified each time the value of the {@code attribute} changes
   * @param <T> the value type
   * @see #setEntity(Entity)
   */
  <T> void addValueListener(Attribute<T> attribute, EventDataListener<ValueChange<T>> listener);

  /**
   * Removes the given listener.
   * @param attribute the attribute for which to remove the listener
   * @param listener the listener to remove
   * @param <T> the value type
   */
  <T> void removeValueListener(Attribute<T> attribute, EventDataListener<ValueChange<T>> listener);

  /**
   * @param listener a listener notified each time the entity is set
   * @see #setEntity(Entity)
   */
  void addEntitySetListener(EventDataListener<Entity> listener);

  /**
   * Removes the given listener.
   * @param listener the listener to remove
   */
  void removeEntitySetListener(EventDataListener<Entity> listener);

  /**
   * @param listener a listener to be notified before an insert is performed
   */
  void addBeforeInsertListener(EventDataListener<List<Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeBeforeInsertListener(EventDataListener<List<Entity>> listener);

  /**
   * @param listener a listener to be notified each time a insert has been performed
   */
  void addAfterInsertListener(EventDataListener<List<Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeAfterInsertListener(EventDataListener<List<Entity>> listener);

  /**
   * @param listener a listener to be notified before an update is performed
   */
  void addBeforeUpdateListener(EventDataListener<Map<Entity.Key, Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeBeforeUpdateListener(EventDataListener<Map<Entity.Key, Entity>> listener);

  /**
   * @param listener a listener to be notified each time an update has been performed,
   * with the updated entities, mapped to their respective original primary keys, that is,
   * the primary keys before the update was performed
   */
  void addAfterUpdateListener(EventDataListener<Map<Entity.Key, Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeAfterUpdateListener(EventDataListener<Map<Entity.Key, Entity>> listener);

  /**
   * @param listener a listener to be notified before a delete is performed
   */
  void addBeforeDeleteListener(EventDataListener<List<Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeBeforeDeleteListener(EventDataListener<List<Entity>> listener);

  /**
   * @param listener a listener to be notified each time a delete has been performed
   */
  void addAfterDeleteListener(EventDataListener<List<Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeAfterDeleteListener(EventDataListener<List<Entity>> listener);

  /**
   * @param listener a listener to be notified before a refresh is performed
   */
  void addBeforeRefreshListener(EventListener listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeBeforeRefreshListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time a refresh has been performed
   */
  void addAfterRefreshListener(EventListener listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeAfterRefreshListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time a entity is modified via this model,
   * updated, inserted or deleted
   */
  void addEntitiesChangedListener(EventListener listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeEntitiesChangedListener(EventListener listener);

  /**
   * @param listener a listener notified each time the active entity is about to be set
   */
  void addConfirmSetEntityObserver(EventDataListener<State> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeConfirmSetEntityObserver(EventDataListener<State> listener);
}
