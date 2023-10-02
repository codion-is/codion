/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.ValidationException;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Specifies a class for editing {@link Entity} instances.
 */
public interface EntityEditModel {

  /**
   * Specifies whether writable foreign key values should persist when the model is cleared or be initialized to null<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> PERSIST_FOREIGN_KEY_VALUES = Configuration.booleanValue("is.codion.framework.model.EntityEditModel.persistForeignKeyValues", true);

  /**
   * Indicates whether the application should ask for confirmation when exiting if some data is unsaved<br>
   * and whether it should warn when unsaved data is about to be lost, i.e. due to selection changes.
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> WARN_ABOUT_UNSAVED_DATA = Configuration.booleanValue("is.codion.framework.model.EntityEditModel.warnAboutUnsavedData", false);

  /**
   * Specifies whether edit models post their insert, update and delete events to {@link EntityEditEvents}<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> EDIT_EVENTS = Configuration.booleanValue("is.codion.framework.model.EntityEditModel.editEvents", true);

  /**
   * @return the type of the entity this edit model is based on
   */
  EntityType entityType();

  /**
   * @return the connection provider used by this edit model
   */
  EntityConnectionProvider connectionProvider();

  /**
   * Populates this edit model with default values.
   * @see #setDefaultValue(Attribute, Supplier)
   * @see AttributeDefinition#defaultValue()
   */
  void setDefaultValues();

  /**
   * Copies the values from the given {@link Entity} into the underlying
   * {@link Entity} being edited by this edit model. If {@code entity} is null
   * the effect is the same as calling {@link #setDefaultValues()}.
   * @param entity the entity
   * @see #setDefaultValues()
   */
  void set(Entity entity);

  /**
   * Refreshes the active Entity from the database, discarding all changes.
   * If the active Entity is new then calling this method has no effect.
   */
  void refreshEntity();

  /**
   * @return an immutable version of the {@link Entity} instance being edited
   * @see Entity#immutable()
   */
  Entity entity();

  /**
   * @param attribute the attribute
   * @return a {@link StateObserver} indicating whether the value of the given attribute is null
   */
  StateObserver isNull(Attribute<?> attribute);

  /**
   * @param attribute the attribute
   * @return a {@link StateObserver} indicating whether the value of the given attribute is not null
   */
  StateObserver isNotNull(Attribute<?> attribute);

  /**
   * @param attribute the attribute
   * @return true if this value is allowed to be null in the underlying entity
   */
  boolean nullable(Attribute<?> attribute);

  /**
   * Sets the given value in the underlying Entity
   * @param attribute the attribute to associate the given value with
   * @param value the value to associate with the given attribute
   * @param <T> the value type
   * @return the previous value, if any
   */
  <T> T put(Attribute<T> attribute, T value);

  /**
   * Removes the given value from the underlying Entity
   * @param attribute the attribute
   * @param <T> the value type
   * @return the value, if any
   */
  <T> T remove(Attribute<T> attribute);

  /**
   * Returns the value associated with the given attribute
   * @param attribute the attribute
   * @param <T> the value type
   * @return the value associated with the given attribute
   */
  <T> T get(Attribute<T> attribute);

  /**
   * Returns the value associated with the given attribute
   * @param attribute the attribute
   * @param <T> the value type
   * @return the value associated with the given attribute, an empty Optional in case it is null
   */
  <T> Optional<T> optional(Attribute<T> attribute);

  /**
   * Returns the value associated with the given foreign key.
   * @param foreignKey the foreign key
   * @return the foreign key value
   */
  Entity referencedEntity(ForeignKey foreignKey);

  /**
   * Returns a Value based on {@code attribute} in this edit model, note that
   * subsequent calls for the same attribute return the same value instance.
   * @param attribute the attribute
   * @param <T> the value type
   * @return a Value based on the given edit model value
   */
  <T> Value<T> value(Attribute<T> attribute);

  /**
   * @return the underlying domain entities
   */
  Entities entities();

  /**
   * @return the definition of the underlying entity
   */
  EntityDefinition entityDefinition();

  /**
   * @return a state controlling whether this edit model triggers a warning before overwriting unsaved data
   * @see #WARN_ABOUT_UNSAVED_DATA
   */
  State warnAboutOverwrite();

  /**
   * Making this edit model read-only prevents any changes from being
   * persisted to the database, trying to insert, update or delete will
   * cause an exception being thrown, it does not prevent editing.
   * Use {@link #insertEnabled()}, {@link #updateEnabled()} and {@link #deleteEnabled()}
   * to configure the enabled state of those specific actions.
   * @return the State controlling whether this model is read only
   */
  State readOnly();

  /**
   * Disabling insert causes an exception being thrown when inserting.
   * @return the state controlling whether inserting is enabled via this edit model
   */
  State insertEnabled();

  /**
   * Disabling update causes an exception being thrown when updating.
   * @return the state controlling whether updating is enabled via this edit model
   */
  State updateEnabled();

  /**
   * Disabling updating multiple entities causes an exception being thrown when
   * trying to update multiple entities at a time.
   * @return the state controlling whether updating multiple entities is enabled
   */
  State updateMultipleEnabled();

  /**
   * Disabling delete causes an exception being thrown when deleting.
   * @return the state controlling whether deleting is enabled via this edit model
   */
  State deleteEnabled();

  /**
   * Creates a {@link EntitySearchModel} for looking up entities referenced by the given foreign key,
   * using the search attributes defined for that entity type.
   * @param foreignKey the foreign key for which to create a {@link EntitySearchModel}
   * @return a {@link EntitySearchModel} for looking up entities of the type referenced by the given foreign key attribute,
   * @throws IllegalStateException in case no searchable attributes can be found for the entity type referenced by the given foreign key
   */
  EntitySearchModel createForeignKeySearchModel(ForeignKey foreignKey);

  /**
   * Returns true if this edit model contains a {@link EntitySearchModel} for the given foreign key
   * @param foreignKey the foreign key
   * @return true if a {@link EntitySearchModel} has been initialized for the given foreign key
   */
  boolean containsSearchModel(ForeignKey foreignKey);

  /**
   * @param foreignKey the foreign key for which to retrieve the {@link EntitySearchModel}
   * @return the {@link EntitySearchModel} associated with the {@code foreignKey}, if no search model
   * has been initialized for the given foreign key, a new one is created, associated with the foreign key and returned.
   */
  EntitySearchModel foreignKeySearchModel(ForeignKey foreignKey);

  /**
   * Sets the default value supplier for the given attribute. Used when the underlying value is not persistent.
   * Use {@link #setDefaultValues()} or {@link #set(Entity)} with a null parameter to populate the model with the default values.
   * @param attribute the attribute
   * @param valueSupplier the default value supplier
   * @param <T> the value type
   * @see #isPersistValue(Attribute)
   * @see #setPersistValue(Attribute, boolean)
   */
  <T> void setDefaultValue(Attribute<T> attribute, Supplier<T> valueSupplier);

  /**
   * @return a state controlling whether this edit model posts insert, update and delete events
   * on the {@link EntityEditEvents} event bus.
   * @see #EDIT_EVENTS
   */
  State editEvents();

  /**
   * Returns true if the last available value for this attribute should be used when initializing
   * a default entity.
   * Override for selective reset of field values when the model is cleared.
   * For foreign key attribute values this method by default returns the value of the
   * attribute {@link EntityEditModel#PERSIST_FOREIGN_KEY_VALUES}.
   * @param attribute the attribute
   * @return true if the given field value should be reset when the model is cleared
   * @see EntityEditModel#PERSIST_FOREIGN_KEY_VALUES
   */
  boolean isPersistValue(Attribute<?> attribute);

  /**
   * Specifies whether the value for the given attribute should be persisted when the model is cleared.
   * @param attribute the attribute
   * @param persistValue true if this model should persist the value of the given attribute on clear
   * @see EntityEditModel#PERSIST_FOREIGN_KEY_VALUES
   */
  void setPersistValue(Attribute<?> attribute, boolean persistValue);

  /**
   * Performs an insert on the active entity, sets the primary key values of the active entity
   * according to the primary key of the inserted entity
   * @return the inserted entity
   * @throws DatabaseException in case of a database exception
   * @throws ValidationException in case validation fails
   * @throws IllegalStateException in case inserting is not enabled
   * @see EntityValidator#validate(Entity)
   */
  Entity insert() throws DatabaseException, ValidationException;

  /**
   * Performs an insert on the given entities, returns silently on receiving an empty collection.
   * @param entities the entities to insert
   * @return a list containing the inserted entities
   * @throws DatabaseException in case of a database exception
   * @throws ValidationException in case validation fails
   * @throws IllegalStateException in case inserting is not enabled
   * @see #addBeforeInsertListener(Consumer)
   * @see #addAfterInsertListener(Consumer)
   * @see EntityValidator#validate(Entity)
   */
  Collection<Entity> insert(Collection<? extends Entity> entities) throws DatabaseException, ValidationException;

  /**
   * Performs an update on the active entity
   * @return the updated entity
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.RecordModifiedException in case the entity has been modified since it was loaded
   * @throws ValidationException in case validation fails
   * @throws IllegalStateException in case updating is not enabled
   * @throws is.codion.common.db.exception.UpdateException in case the active entity is not modified
   * @see EntityValidator#validate(Entity)
   */
  Entity update() throws DatabaseException, ValidationException;

  /**
   * Updates the given entities. If the entities are unmodified or the collection is empty this method returns silently.
   * @param entities the entities to update
   * @return the updated entities
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.RecordModifiedException  in case an entity has been modified since it was loaded
   * @throws ValidationException in case validation fails
   * @throws IllegalStateException in case updating is not enabled
   * @see #addBeforeUpdateListener(Consumer)
   * @see #addAfterUpdateListener(Consumer)
   * @see EntityValidator#validate(Entity)
   */
  Collection<Entity> update(Collection<? extends Entity> entities) throws DatabaseException, ValidationException;

  /**
   * Deletes the active entity
   * @throws DatabaseException in case of a database exception
   * @throws IllegalStateException in case deleting is not enabled
   * @see #addBeforeDeleteListener(Consumer)
   * @see #addAfterDeleteListener(Consumer)
   */
  void delete() throws DatabaseException;

  /**
   * Deletes the given entities, returns silently on receiving an empty collection
   * @param entities the entities to delete
   * @throws DatabaseException in case of a database exception
   * @throws IllegalStateException in case deleting is not enabled
   * @see #addBeforeDeleteListener(Consumer)
   * @see #addAfterDeleteListener(Consumer)
   */
  void delete(Collection<? extends Entity> entities) throws DatabaseException;

  /**
   * Refreshes all data models used by this edit model, combo box models f.ex.
   */
  void refresh();

  /**
   * Adds the given entities to all foreign key models based on that entity type
   * @param foreignKey the foreign key
   * @param entities the values
   */
  void add(ForeignKey foreignKey, Collection<Entity> entities);

  /**
   * Removes the given entities from all foreign key models based on that entity type and clears any foreign
   * key values referencing them.
   * @param foreignKey the foreign key
   * @param entities the values
   */
  void remove(ForeignKey foreignKey, Collection<Entity> entities);

  /**
   * For every field referencing the given foreign key values, replaces that foreign key instance with
   * the corresponding entity from {@code entities}, useful when attribute
   * values have been changed in the referenced entity that must be reflected in the edit model.
   * @param foreignKey the foreign key
   * @param entities the foreign key entities
   */
  void replace(ForeignKey foreignKey, Collection<Entity> entities);

  /**
   * @return the validator
   */
  EntityValidator validator();

  /**
   * Validates the value associated with the given attribute, using the underlying validator.
   * @param attribute the attribute the value is associated with
   * @throws ValidationException if the given value is not valid for the given attribute
   * @see #validator()
   */
  void validate(Attribute<?> attribute) throws ValidationException;

  /**
   * Validates the current state of the entity
   * @throws ValidationException in case the entity is invalid
   */
  void validate() throws ValidationException;

  /**
   * Validates the given entities, using the underlying validator.
   * For entities of a type other than this edit model is based on,
   * their respective validators are used.
   * @param entities the entities to validate
   * @throws ValidationException on finding the first invalid entity
   * @see #validator()
   * @see EntityDefinition#validator()
   */
  void validate(Collection<? extends Entity> entities) throws ValidationException;

  /**
   * Validates the given entity, using the underlying validator.
   * For entities of a type other than this edit model is based on,
   * their respective validators are used.
   * @param entity the entity to validate
   * @throws ValidationException in case the entity is invalid
   * @throws NullPointerException in case the entity is null
   * @see #validator()
   */
  void validate(Entity entity) throws ValidationException;

  /**
   * @return a {@link StateObserver} indicating the valid status of the underlying Entity.
   * @see #validator()
   * @see #validate(Attribute)
   * @see EntityValidator#validate(Entity)
   */
  StateObserver valid();

  /**
   * @param attribute the attribute
   * @return a {@link StateObserver} indicating the valid status of the given attribute.
   * @see #validator()
   */
  StateObserver valid(Attribute<?> attribute);

  /**
   * Returns a {@link StateObserver} indicating when and if any values in the underlying Entity have been modified.
   * @return a {@link StateObserver} indicating the modified state of this edit model
   */
  StateObserver modified();

  /**
   * Returns a {@link StateObserver} instance indicating whether the value of the given attribute has been modified.
   * @param attribute the attribute
   * @return a {@link StateObserver} indicating the modified state of the value of the given attribute
   * @throws IllegalArgumentException in case attribute is not part of the underlying entity
   * @see #modified()
   */
  StateObserver modified(Attribute<?> attribute);

  /**
   * @return a {@link StateObserver} indicating whether the active entity exists in the database
   */
  StateObserver exists();

  /**
   * @return a {@link StateObserver} indicating whether the primary key of the active entity is null
   */
  StateObserver primaryKeyNull();

  /**
   * @return a {@link StateObserver} active while data models (such as combo box models) are being refreshed
   */
  StateObserver refreshing();

  /**
   * Adds a listener notified each time the value associated with the given attribute is edited via
   * {@link #put(Attribute, Object)} or {@link #remove(Attribute)}, note that this event is only fired
   * if the value actually changes.
   * @param attribute the attribute for which to monitor value edits
   * @param listener a listener notified each time the value of the given attribute is edited via this model
   * @param <T> the value type
   */
  <T> void addEditListener(Attribute<T> attribute, Consumer<T> listener);

  /**
   * Removes the given listener.
   * @param attribute the attribute
   * @param listener the listener to remove
   * @param <T> the value type
   */
  <T> void removeEditListener(Attribute<T> attribute, Consumer<T> listener);

  /**
   * Adds a listener notified each time the value associated with the given attribute changes, either
   * via editing or when the active entity is set.
   * @param attribute the attribute for which to monitor value changes
   * @param listener a listener notified each time the value of the {@code attribute} changes
   * @param <T> the value type
   * @see #set(Entity)
   */
  <T> void addValueListener(Attribute<T> attribute, Consumer<T> listener);

  /**
   * Removes the given listener.
   * @param attribute the attribute for which to remove the listener
   * @param listener the listener to remove
   * @param <T> the value type
   */
  <T> void removeValueListener(Attribute<T> attribute, Consumer<T> listener);

  /**
   * @param listener a listener notified each time a value changes
   */
  void addValueListener(Consumer<Attribute<?>> listener);

  /**
   * @param listener the listener to remove
   */
  void removeValueListener(Consumer<Attribute<?>> listener);

  /**
   * Notified each time the entity is set via {@link #set(Entity)}.
   * @param listener a listener notified each time the entity is set, possibly to null
   * @see #set(Entity)
   */
  void addEntityListener(Consumer<Entity> listener);

  /**
   * Removes the given listener.
   * @param listener the listener to remove
   */
  void removeEntityListener(Consumer<Entity> listener);

  /**
   * @param listener a listener to be notified before an insert is performed
   */
  void addBeforeInsertListener(Consumer<Collection<Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeBeforeInsertListener(Consumer<Collection<Entity>> listener);

  /**
   * @param listener a listener to be notified each time insert has been performed
   */
  void addAfterInsertListener(Consumer<Collection<Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeAfterInsertListener(Consumer<Collection<Entity>> listener);

  /**
   * @param listener a listener to be notified before an update is performed
   */
  void addBeforeUpdateListener(Consumer<Map<Entity.Key, Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeBeforeUpdateListener(Consumer<Map<Entity.Key, Entity>> listener);

  /**
   * @param listener a listener to be notified each time an update has been performed,
   * with the updated entities, mapped to their respective original primary keys, that is,
   * the primary keys before the update was performed
   */
  void addAfterUpdateListener(Consumer<Map<Entity.Key, Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeAfterUpdateListener(Consumer<Map<Entity.Key, Entity>> listener);

  /**
   * @param listener a listener to be notified before a delete is performed
   */
  void addBeforeDeleteListener(Consumer<Collection<Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeBeforeDeleteListener(Consumer<Collection<Entity>> listener);

  /**
   * @param listener a listener to be notified each time delete has been performed
   */
  void addAfterDeleteListener(Consumer<Collection<Entity>> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeAfterDeleteListener(Consumer<Collection<Entity>> listener);

  /**
   * @param listener a listener to be notified each time a refresh has been performed
   * @see #refresh()
   */
  void addRefreshListener(Runnable listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeRefreshListener(Runnable listener);

  /**
   * @param listener a listener notified each time one or more entities are updated, inserted or deleted via this model
   */
  void addInsertUpdateOrDeleteListener(Runnable listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeInsertUpdateOrDeleteListener(Runnable listener);

  /**
   * @param listener a listener notified each time the active entity is about to be set
   */
  void addConfirmOverwriteListener(Consumer<State> listener);

  /**
   * Removes the given listener.
   * @param listener a listener to remove
   */
  void removeConfirmOverwriteListener(Consumer<State> listener);
}
