/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.swing.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.common.model.valuemap.ValueMapEditModel;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.common.swing.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Specifies a class for editing {@link Entity} instances.
 */
public interface EntityEditModel extends ValueMapEditModel<String, Object>, Refreshable, EntityDataProvider {

  /**
   * @return an Entity instance populated with default values for all properties
   * @see #getDefaultValue(org.jminor.framework.domain.Property)
   */
  Entity getDefaultEntity();

  /**
   * Copies the values from the given {@link Entity} into the underlying
   * {@link Entity} being edited by this edit model. If <code>entity</code>
   * is null then the entity being edited is populated with default values
   * @param entity the entity
   */
  void setEntity(final Entity entity);

  /**
   * Refreshes the active Entity from the database, discarding all changes.
   * If the active Entity is new then calling this method has no effect.
   */
  void refreshEntity();

  /**
   * @return a deep copy of the active entity
   * @see org.jminor.framework.domain.Entity#getCopy()
   */
  Entity getEntityCopy();

  /**
   * @param includePrimaryKeyValues if false then the primary key values are excluded
   * @return a deep copy of the active entity
   * @see org.jminor.framework.domain.Entity#getCopy()
   */
  Entity getEntityCopy(final boolean includePrimaryKeyValues);

  /**
   * Returns true if the active entity is new or false if it represents a row already persisted.
   * By default an entity is new if either its primary key or the original primary key are null.
   * It is not recommended to base the result of this function on a database query since it is called frequently,
   * as in, every time a property value changes.
   * @return true if the active entity is new, that is, does not represent a persistent row
   * @see #getPrimaryKeyNullObserver
   * @see org.jminor.framework.domain.Entity#isPrimaryKeyNull()
   */
  boolean isEntityNew();

  /**
   * Returns true if an entity is selected and a value has been modified or if the entity is new
   * and one or more non-default values have been entered
   * @return true if this edit model contains unsaved data
   * @see org.jminor.framework.Configuration#WARN_ABOUT_UNSAVED_DATA
   */
  boolean containsUnsavedData();

  /**
   * Returns the value associated with the given propertyID assuming it
   * is an {@link Entity} instance
   * @param foreignKeyPropertyID the ID of the property
   * @return the value assuming it is an {@link Entity}
   * @throws ClassCastException in case the value was not an {@link Entity}
   */
  Entity getForeignKeyValue(final String foreignKeyPropertyID);

  /**
   * For every field referencing the given foreign key values, replaces that foreign key instance with
   * the corresponding entity from <code>foreignKeyValues</code>, useful when property
   * values have been changed in the referenced entity that must be reflected in the edit model.
   * @param foreignKeyEntityID the entity ID of the foreign key values
   * @param foreignKeyValues the new foreign key entities
   */
  void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> foreignKeyValues);

  /**
   * Initializes a value provider for the given property, useful for adding lookup
   * functionality to input fields for example.
   * @param property the property
   * @return a value provider for the given property
   */
  ValueCollectionProvider getValueProvider(final Property property);

  /**
   * @return true if this model is read only,
   * by default this returns the isReadOnly value of the underlying entity
   */
  boolean isReadOnly();

  /**
   * @param readOnly the read only status
   * @return this edit model instance
   */
  EntityEditModel setReadOnly(final boolean readOnly);

  /**
   * @return true if this model should allow records to be inserted
   */
  boolean isInsertAllowed();

  /**
   * @param value true if this model should allow inserts
   * @return this edit model instance
   */
  EntityEditModel setInsertAllowed(final boolean value);

  /**
   * @return true if this model should allow records to be updated
   */
  boolean isUpdateAllowed();

  /**
   * @param value true if this model should allow records to be updated
   * @return this edit model instance
   */
  EntityEditModel setUpdateAllowed(final boolean value);

  /**
   * @return true if this model should allow records to be deleted
   */
  boolean isDeleteAllowed();

  /**
   * @param value true if this model should allow records to be deleted
   * @return this edit model instance
   */
  EntityEditModel setDeleteAllowed(final boolean value);

  /**
   * Creates a default {@link EntityComboBoxModel} for the given property, override to provide
   * a specific {@link EntityComboBoxModel} (filtered for example) for properties.
   * This method is called when creating a {@link EntityComboBoxModel} for entity properties, both
   * for the edit fields used when editing a single record and the edit field used
   * when updating multiple records.
   * This default implementation returns a sorted {@link EntityComboBoxModel} with the default nullValueItem
   * if the underlying property is nullable
   * @param foreignKeyProperty the foreign key property for which to create a {@link EntityComboBoxModel}
   * @return a {@link EntityComboBoxModel} for the given property
   * @see org.jminor.framework.Configuration#COMBO_BOX_NULL_VALUE_ITEM
   * @see org.jminor.framework.domain.Property#isNullable()
   */
  EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Creates a combo box model containing the current values of the given property.
   * This default implementation returns a sorted {@link FilteredComboBoxModel} with the default nullValueItem
   * if the underlying property is nullable
   * @param property the property
   * @return a combo box model based on the given property
   */
  FilteredComboBoxModel createPropertyComboBoxModel(final Property.ColumnProperty property);

  /**
   * Creates a {@link EntityLookupModel} for looking up entities referenced by the given foreign key property,
   * using the search properties defined for that entity type, or if none are defined all string based searchable
   * properties in that entity.
   * @param foreignKeyProperty the foreign key property for which to create a {@link EntityLookupModel}
   * @return a {@link EntityLookupModel} for looking up entities of the type referenced by the given foreign key property,
   * @throws IllegalStateException in case no searchable properties can be found for the entity type referenced by the
   * given foreign key property
   */
  EntityLookupModel createEntityLookupModel(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Returns true if this edit model contains a {@link EntityLookupModel} for the given foreign key property
   * @param foreignKeyPropertyID the ID of the property
   * @return true if a {@link EntityLookupModel} has been initialized for the given foreign key property
   */
  boolean containsLookupModel(final String foreignKeyPropertyID);

  /**
   * @param foreignKeyPropertyID the ID of the property for which to retrieve the {@link EntityLookupModel}
   * @return the {@link EntityLookupModel} associated with the <code>property</code>, if no lookup model
   * has been initialized for the given property, a new one is created and associated with
   * the property, to be returned the next time this method is called
   */
  EntityLookupModel getEntityLookupModel(final String foreignKeyPropertyID);

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the {@link EntityLookupModel}
   * @return the {@link EntityLookupModel} associated with the <code>property</code>, if no lookup model
   * has been initialized for the given property, a new one is created and associated with
   * the property, to be returned the next time this method is called
   */
  EntityLookupModel getEntityLookupModel(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * @param property the property for which to get the ComboBoxModel
   * @return a ComboBoxModel representing <code>property</code>,
   * if no combo box model is associated with the property a new one is created, and associated
   * with the given property
   */
  FilteredComboBoxModel getPropertyComboBoxModel(final Property.ColumnProperty property);

  /**
   * Returns true if this edit model contains a ComboBoxModel for the given property
   * @param propertyID the ID of the property
   * @return true if a ComboBoxModel has been initialized for the given property
   */
  boolean containsComboBoxModel(final String propertyID);

  /**
   * @param foreignKeyPropertyID the ID of the property for which to retrieve the {@link EntityComboBoxModel}
   * @return the {@link EntityComboBoxModel} for the property identified by <code>propertyID</code>,
   * if no combo box model is associated with the property a new one is created, and associated
   * with the given property
   */
  EntityComboBoxModel getEntityComboBoxModel(final String foreignKeyPropertyID);

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the {@link EntityComboBoxModel}
   * @return the {@link EntityComboBoxModel} associated with the <code>property</code>,
   * if no combo box model is associated with the property a new one is created, and associated
   * with the given property
   */
  EntityComboBoxModel getEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Refreshes the Refreshable ComboBoxModels associated with this {@link EntityEditModel}
   * @see org.jminor.common.model.Refreshable
   */
  void refreshComboBoxModels();

  /**
   * Clears the data from all combo box models
   */
  void clearComboBoxModels();

  /**
   * Returns the default value for the given property, used when initializing a new
   * default entity for this edit model. This does not apply to denormalized properties
   * nor properties that are wrapped in foreign key properties.
   * If the default value of a property should be the last value used, call {@link #setValuePersistent(String, boolean)}
   * with <code>true</code> for the given property or override {@link #isValuePersistent} so that it
   * returns <code>true</code> for that property in case the value should persist.
   * @param property the property
   * @return the default value for the property
   * @see Property#setDefaultValue(Object)
   * @see #setValuePersistent(String, boolean)
   * @see #isValuePersistent(org.jminor.framework.domain.Property)
   */
  Object getDefaultValue(final Property property);

  /**
   * Returns true if the last available value for this property should be used when initializing
   * a default entity.
   * Override for selective reset of field values when the model is cleared.
   * For foreign key property values this method by default returns the value of the
   * property {@link org.jminor.framework.Configuration#PERSIST_FOREIGN_KEY_VALUES}.
   * @param property the property
   * @return true if the given field value should be reset when the model is cleared
   * @see org.jminor.framework.Configuration#PERSIST_FOREIGN_KEY_VALUES
   */
  boolean isValuePersistent(final Property property);

  /**
   * Returns true if values based on this property should be available for lookup via this EditModel.
   * This means displaying all the distinct property values to the user, allowing her to select one.
   * @param property the property
   * @return true if value lookup should be allowed for this property
   */
  boolean isLookupAllowed(Property property);

  /**
   * @param propertyID the property ID
   * @param persistValueOnClear true if this model should persist the value of the given property on clear
   * @return this edit model instance
   * @see org.jminor.framework.Configuration#PERSIST_FOREIGN_KEY_VALUES
   */
  EntityEditModel setValuePersistent(final String propertyID, final boolean persistValueOnClear);

  /**
   * Performs a insert on the active entity, sets the primary key values of the active entity
   * according to the primary key of the inserted entity
   * @return a list containing the inserted entity
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws org.jminor.common.model.valuemap.exception.ValidationException in case validation fails
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection)
   */
  List<Entity> insert() throws DatabaseException, ValidationException;

  /**
   * Performs an insert on the given entities, returns silently on receiving an empty list
   * @param entities the entities to insert
   * @return a list containing the inserted entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws ValidationException in case validation fails
   * @see #addBeforeInsertListener(EventInfoListener)
   * @see #addAfterInsertListener(EventInfoListener)
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection)
   */
  List<Entity> insert(final List<Entity> entities) throws DatabaseException, ValidationException;

  /**
   * Performs a update on the active entity
   * @return the updated entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws org.jminor.common.model.valuemap.exception.ValidationException in case validation fails
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection)
   */
  List<Entity> update() throws DatabaseException, ValidationException;

  /**
   * Updates the given entities. If the entities are unmodified or the list is empty
   * this method returns silently.
   * @param entities the entities to update
   * @return the updated entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @see #addBeforeUpdateListener(EventInfoListener)
   * @see #addAfterUpdateListener(EventInfoListener)
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection)
   */
  List<Entity> update(final List<Entity> entities) throws DatabaseException, ValidationException;

  /**
   * Deletes the active entity
   * @return the deleted entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @see #addBeforeDeleteListener(EventInfoListener)
   * @see #addAfterDeleteListener(EventInfoListener)
   */
  List<Entity> delete() throws DatabaseException;

  /**
   * Deletes the given entities, returns silently on receiving an empty list
   * @param entities the entities to delete
   * @return the deleted entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @see #addBeforeDeleteListener(EventInfoListener)
   * @see #addAfterDeleteListener(EventInfoListener)
   */
  List<Entity> delete(final List<Entity> entities) throws DatabaseException;

  /**
   * @return an observer indicating whether or not the active entity is new
   * @see #isEntityNew()
   */
  StateObserver getEntityNewObserver();

  /**
   * @return the state used to determine if deleting should be enabled
   * @see #isDeleteAllowed()
   * @see #setDeleteAllowed(boolean)
   */
  StateObserver getAllowDeleteObserver();

  /**
   * @return a {@link StateObserver} indicating whether or not the primary key of the active entity is null
   */
  StateObserver getPrimaryKeyNullObserver();

  /**
   * @return the {@link StateObserver} used to determine if updating should be enabled
   * @see #isUpdateAllowed()
   * @see #setUpdateAllowed(boolean)
   */
  StateObserver getAllowUpdateObserver();

  /**
   * @return the {@link StateObserver} used to determine if inserting should be enabled
   * @see #isInsertAllowed()
   * @see #setInsertAllowed(boolean)
   */
  StateObserver getAllowInsertObserver();

  /**
   * @param listener a listener notified each time the entity is set
   */
  void addEntitySetListener(final EventInfoListener<Entity> listener);

  /**
   * @param listener the listener to remove
   */
  void removeEntitySetListener(final EventInfoListener listener);

  /**
   * @param listener a listener to be notified before an insert is performed
   */
  void addBeforeInsertListener(final EventInfoListener<InsertEvent> listener);

  /**
   * @param listener a listener to remove
   */
  void removeBeforeInsertListener(final EventInfoListener listener);

  /**
   * @param listener a listener to be notified each time a insert has been performed
   */
  void addAfterInsertListener(final EventInfoListener<InsertEvent> listener);

  /**
   * @param listener a listener to remove
   */
  void removeAfterInsertListener(final EventInfoListener listener);

  /**
   * @param listener a listener to be notified before an update is performed
   */
  void addBeforeUpdateListener(final EventInfoListener<UpdateEvent> listener);

  /**
   * @param listener a listener to remove
   */
  void removeBeforeUpdateListener(final EventInfoListener listener);

  /**
   * @param listener a listener to be notified each time an update has been performed
   */
  void addAfterUpdateListener(final EventInfoListener<UpdateEvent> listener);

  /**
   * @param listener a listener to remove
   */
  void removeAfterUpdateListener(final EventInfoListener listener);

  /**
   * @param listener a listener to be notified before a delete is performed
   */
  void addBeforeDeleteListener(final EventInfoListener<DeleteEvent> listener);

  /**
   * @param listener a listener to remove
   */
  void removeBeforeDeleteListener(final EventInfoListener listener);

  /**
   * @param listener a listener to be notified each time a delete has been performed
   */
  void addAfterDeleteListener(final EventInfoListener<DeleteEvent> listener);

  /**
   * @param listener a listener to remove
   */
  void removeAfterDeleteListener(final EventInfoListener listener);

  /**
   * @param listener a listener to be notified before a refresh is performed
   */
  void addBeforeRefreshListener(final EventListener listener);

  /**
   * @param listener a listener to remove
   */
  void removeBeforeRefreshListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time a refresh has been performed
   */
  void addAfterRefreshListener(final EventListener listener);

  /**
   * @param listener a listener to remove
   */
  void removeAfterRefreshListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time a entity is modified via this model,
   * updated, inserted or deleted
   */
  void addEntitiesChangedListener(final EventListener listener);

  /**
   * @param listener a listener to remove
   */
  void removeEntitiesChangedListener(final EventListener listener);

  /**
   * @param listener a listener notified each time the active entity is about to be set
   */
  void addConfirmSetEntityObserver(final EventInfoListener<State> listener);

  /**
   * @param listener a listener to remove
   */
  void removeConfirmSetEntityObserver(final EventInfoListener listener);

  /**
   * An event describing a insert action.
   */
  interface InsertEvent {
    /**
     * @return the entities just inserted
     */
    List<Entity> getInsertedEntities();
  }

  /**
   * An event describing a delete action.
   */
  interface DeleteEvent {
    /**
     * @return the deleted entities
     */
    List<Entity> getDeletedEntities();
  }

  /**
   * An event describing a update action.
   */
  interface UpdateEvent {
    /**
     * @return the updated entities, mapped to their respective original primary keys, that is,
     * the primary keys before the update was performed
     */
    Map<Entity.Key, Entity> getUpdatedEntities();
  }
}
