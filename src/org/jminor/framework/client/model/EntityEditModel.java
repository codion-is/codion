/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

/**
 * Specifies a class for editing entity instances.
 */
public interface EntityEditModel extends ValueChangeMapEditModel<String, Object>, EntityDataProvider {

  /**
   * Sets the Entity instance to edit
   * @param entity the entity
   */
  void setEntity(final Entity entity);

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
   * @return true if the active entity is new, that is, has a primary key with null value
   * or a original null value
   * @see org.jminor.framework.domain.Entity#isPrimaryKeyNull()
   */
  boolean isEntityNew();

  /**
   * Returns the value associated with the given propertyID assuming it
   * is an Entity instance
   * @param foreignKeyPropertyID the ID of the property
   * @return the value assuming it is an Entity
   * @throws ClassCastException in case the value was not an Entity
   */
  Entity getForeignKeyValue(final String foreignKeyPropertyID);

  /**
   * For every field referencing the given foreign key values, replaces that foreign key instance with
   * the corresponding entity from <code>foreignKeyValues</code>, useful when property
   * values have been changed in the referenced entity that must be reflected in the edit model.
   * @param foreignKeyEntityID the entity ID of the foreign key values
   * @param newForeignKeyValues the new foreign key entities
   */
  void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> newForeignKeyValues);

  /**
   * Initializes a value provider for the given property, used for adding lookup
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
   * @param propertyID the property ID
   * @param persistValueOnClear true if this model should persist the value of the given property on clear
   * @return this edit model instance
   * @see org.jminor.framework.Configuration#PERSIST_FOREIGN_KEY_VALUES
   */
  EntityEditModel setPersistValueOnClear(final String propertyID, final boolean persistValueOnClear);

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
   * Creates a default EntityComboBoxModel for the given property, override to provide
   * specific EntityComboBoxModels (filtered for example) for properties.
   * This method is called when creating a EntityComboBoxModel for entity properties, both
   * for the edit fields used when editing a single record and the edit field used
   * when updating multiple records.
   * This default implementation returns a sorted EntityComboBoxModel with the default nullValueItem
   * if the underlying property is nullable
   * @param foreignKeyProperty the foreign key property for which to create a EntityComboBoxModel
   * @return a EntityComboBoxModel for the given property
   * @see org.jminor.framework.Configuration#DEFAULT_COMBO_BOX_NULL_VALUE_ITEM
   * @see org.jminor.framework.domain.Property#isNullable()
   */
  EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Creates a EntityLookupModel for the given entityID
   * @param entityID the ID of the entity
   * @param lookupProperties the properties involved in the lookup
   * @param additionalSearchCriteria an additional search criteria applied when performing the lookup
   * @return a EntityLookupModel
   */
  EntityLookupModel createEntityLookupModel(final String entityID, final List<Property.ColumnProperty> lookupProperties,
                                            final Criteria additionalSearchCriteria);

  /**
   * @param property the property for which to get the ComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires,
   * if none is specified <code>addEntitiesChangedListener()</code> is used.
   * @param nullValueString the value to use for representing the null item at the top of the list,
   * if this value is null then no such item is included
   * @return a PropertyComboBoxModel representing <code>property</code>, if no combo box model
   * has been initialized for the given property, a new one is created and associated with
   * the property, to be returned the next time this method is called
   */
  FilteredComboBoxModel initializePropertyComboBoxModel(final Property.ColumnProperty property, final EventObserver refreshEvent,
                                                        final String nullValueString);

  /**
   * @param property the property for which to get the ComboBoxModel
   * @return a PropertyComboBoxModel representing <code>property</code>
   * @throws RuntimeException if no combo box has been initialized for the given property
   */
  FilteredComboBoxModel getPropertyComboBoxModel(final Property.ColumnProperty property);

  /**
   * Returns true if this edit model contains a ComboBoxModel for the given property
   * @param propertyID the ID of the property
   * @return true if a ComboBoxModel has been initialized for the given property
   */
  boolean containsComboBoxModel(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel for the property identified by <code>propertyID</code>,
   * if no combo box model is associated with the property a new one is initialized, and associated
   * with the given property
   * @throws RuntimeException if no combo box has been initialized for the given property
   */
  EntityComboBoxModel getEntityComboBoxModel(final String propertyID);

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel associated with the <code>property</code>
   * @throws RuntimeException if no combo box has been initialized for the given property
   */
  EntityComboBoxModel getEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * @param propertyID the ID of the foreign key property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel for the <code>property</code>,
   * if no combo box model is associated with the property a new one is initialized, and associated
   * with the given property
   */
  EntityComboBoxModel initializeEntityComboBoxModel(final String propertyID);

  /**
   * @param foreignKeyProperty the foreign key property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel for the <code>property</code>,
   * if no combo box model is associated with the property a new one is initialized, and associated
   * with the given property
   */
  EntityComboBoxModel initializeEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Creates a combo box model containing the current values of the given property
   * @param property the property
   * @param refreshEvent the combo box model is refreshed each time this event is fired
   * @param nullValueString the string to use as a null value caption
   * @return a combo box model based on the given property
   */
  FilteredComboBoxModel createPropertyComboBoxModel(final Property.ColumnProperty property, final EventObserver refreshEvent,
                                                    final String nullValueString);

  /**
   * Refreshes the Refreshable ComboBoxModels associated with this EntityEditModel
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
   * (Property.DenormalizedProperty) nor properties that are wrapped in foreign key properties
   * (Property.ForeignKeyProperty)
   * If the default value of a property should be the last value used <code>persistValueOnClear</code>
   * should be overridden so that it returns <code>true</code> for that property.
   * @param property the property
   * @return the default value for the property
   * @see Property#setDefaultValue(Object)
   * @see #persistValueOnClear(org.jminor.framework.domain.Property)
   */
  Object getDefaultValue(final Property property);

  /**
   * Returns true if the last available value for this property should be used when initializing
   * a default entity.
   * Override for selective reset of field values when the model is cleared.
   * For Property.ForeignKeyProperty values this method by default returns the value of the
   * property <code>Configuration.PERSIST_FOREIGN_KEY_VALUES</code>.
   * @param property the property
   * @return true if the given field value should be reset when the model is cleared
   * @see org.jminor.framework.Configuration#PERSIST_FOREIGN_KEY_VALUES
   */
  boolean persistValueOnClear(final Property property);

  /**
   * Performs a insert on the active entity
   * @return the primary keys of the inserted entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws org.jminor.common.model.valuemap.exception.ValidationException in case validation fails
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection, int)
   */
  List<Entity.Key> insert() throws CancelException, DatabaseException, ValidationException;

  /**
   * Performs an insert on the given entities, returns silently on recieving an empty list
   * @param entities the entities to insert
   * @return the primary keys of the inserted entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws ValidationException in case validation fails
   * @see #addBeforeInsertListener(java.awt.event.ActionListener)
   * @see #addAfterInsertListener(java.awt.event.ActionListener)
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection, int)
   */
  List<Entity.Key> insert(List<Entity> entities) throws CancelException, DatabaseException, ValidationException;

  /**
   * Performs a update on the active entity
   * @return the updated entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws org.jminor.common.model.valuemap.exception.ValidationException in case validation fails
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection, int)
   */
  List<Entity> update() throws CancelException, DatabaseException, ValidationException;

  /**
   * Updates the given Entities. If the entities are unmodified or the list is empty
   * this method returns silently.
   * @param entities the Entities to update
   * @return the updated entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @see #addBeforeUpdateListener(java.awt.event.ActionListener)
   * @see #addAfterUpdateListener(java.awt.event.ActionListener)
   * @see org.jminor.framework.domain.Entity.Validator#validate(java.util.Collection, int)
   */
  List<Entity> update(final List<Entity> entities) throws DatabaseException, CancelException, ValidationException;

  /**
   * Deletes the active entity
   * @return the deleted entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @see #addBeforeDeleteListener(java.awt.event.ActionListener)
   * @see #addAfterDeleteListener(java.awt.event.ActionListener)
   */
  List<Entity> delete() throws DatabaseException, CancelException;

  /**
   * Deletes the given entities, returns silently on recieving an empty list
   * @param entities the entities to delete
   * @return the deleted entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @see #addBeforeDeleteListener(java.awt.event.ActionListener)
   * @see #addAfterDeleteListener(java.awt.event.ActionListener)
   */
  List<Entity> delete(final List<Entity> entities) throws DatabaseException, CancelException;

  /**
   * @return the state used to determine if deleting should be enabled
   * @see #isDeleteAllowed()
   * @see #setDeleteAllowed(boolean)
   */
  StateObserver getAllowDeleteObserver();

  /**
   * @return a State indicating whether or not the active entity is null
   */
  StateObserver getEntityNullObserver();

  /**
   * @return the state used to determine if updating should be enabled
   * @see #isUpdateAllowed()
   * @see #setUpdateAllowed(boolean)
   */
  StateObserver getAllowUpdateObserver();

  /**
   * @return the state used to determine if inserting should be enabled
   * @see #isInsertAllowed()
   * @see #setInsertAllowed(boolean)
   */
  StateObserver getAllowInsertObserver();

  /**
   * @param listener a listener to be notified before an insert is performed
   */
  void addBeforeInsertListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time
   */
  void removeBeforeInsertListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time a insert has been performed
   */
  void addAfterInsertListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time
   */
  void removeAfterInsertListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified before an update is performed
   */
  void addBeforeUpdateListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time
   */
  void removeBeforeUpdateListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time an update has been performed
   */
  void addAfterUpdateListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time
   */
  void removeAfterUpdateListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified before a delete is performed
   */
  void addBeforeDeleteListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time
   */
  void removeBeforeDeleteListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time a delete has been performed
   */
  void addAfterDeleteListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time
   */
  void removeAfterDeleteListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified before a refresh is performed
   */
  void addBeforeRefreshListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time
   */
  void removeBeforeRefreshListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time a refresh has been performed
   */
  void addAfterRefreshListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time
   */
  void removeAfterRefreshListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time a entity is modified via this model,
   * updated, inserted or deleted
   */
  void addEntitiesChangedListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time
   */
  void removeEntitiesChangedListener(final ActionListener listener);
}
