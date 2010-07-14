/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import javax.swing.ComboBoxModel;
import java.util.Collection;
import java.util.List;

public interface EntityEditModel extends ValueChangeMapEditModel<String, Object> {

  /**
   * Code for the insert action, used during validation
   */
  int INSERT = 1;
  /**
   * Code for the update action, used during validation
   */
  int UPDATE = 2;
  /**
   * Code for an unknown action, used during validation
   */
  int UNKNOWN = 3;

  /**
   * Indicates whether the model is active and ready to receive input
   * @return a state indicating whether the model is active and ready to receive input
   */
  State stateActive();

  /**
   * @return the state used to determine if deleting should be enabled
   * @see #isDeleteAllowed()
   * @see #setDeleteAllowed(boolean)
   */
  State stateAllowDelete();

  /**
   * @return a State indicating whether or not the active entity is null
   */
  State stateEntityNull();

  /**
   * @return the state used to determine if updating should be enabled
   * @see #isUpdateAllowed()
   * @see #setUpdateAllowed(boolean)
   */
  State stateAllowUpdate();

  /**
   * @return the state used to determine if inserting should be enabled
   * @see #isInsertAllowed()
   * @see #setInsertAllowed(boolean)
   */
  State stateAllowInsert();

  /**
   * @return an Event fired after a successful insert
   */
  Event eventAfterInsert();

  /**
   * @return an Event fired after a successful update
   */
  Event eventAfterUpdate();

  /**
   * @return an Event fired after a successful delete
   */
  Event eventAfterDelete();

  /**
   * @return an event fired before a refresh
   */
  Event eventRefreshStarted();

  /**
   * @return an event fired after a refresh has been performed
   */
  Event eventRefreshDone();

  /**
   * @return an Event fired before a delete
   */
  Event eventBeforeDelete();

  /**
   * @return an Event fired before a insert
   */
  Event eventBeforeInsert();

  /**
   * @return an Event fired before a update
   */
  Event eventBeforeUpdate();

  /**
   * @return an Event fired when the underlying table has undergone changes,
   * such as insert, update or delete
   */
  Event eventEntitiesChanged();

  /**
   * @return the ID of the entity this EntityEditModel is based on
   */
  String getEntityID();

  /**
   * @return the EntityDbProvider instance used by this EntityEditModel
   */
  EntityDbProvider getDbProvider();

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
   * @see org.jminor.framework.domain.Entity#isNull()
   */
  boolean isEntityNew();

  /**
   * Validates the given Entity objects.
   * @param entities the entities to validate
   * @param action describes the action requiring validation,
   * EntityEditor.INSERT, EntityEditor.UPDATE or EntityEditor.UNKNOWN
   * @throws ValidationException in case the validation fails
   */
  void validateEntities(final Collection<Entity> entities, final int action) throws ValidationException;

  /**
   * Sets the active state of this edit model, an active edit model should be
   * ready to recieve input, this is relevant when a UI is based on the model
   * @param active the active state
   */
  void setActive(final boolean active);

  /**
   * Returns the value associated with the given propertyID assuming it
   * is an Entity instance
   * @param foreignKeyPropertyID the ID of the property
   * @return the value assuming it is an Entity
   * @throws ClassCastException in case the value was not an Entity
   */
  Entity getEntityValue(final String foreignKeyPropertyID);

  /**
   * @param property the property
   * @return a value provider providing the values of the given property
   */
  ValueCollectionProvider getValueProvider(final Property property);

  /**
   * @return true if this model is read only,
   * by default this returns the isReadOnly value of the underlying entity
   */
  boolean isReadOnly();

  /**
   * @return true if this model should allow records to be inserted
   */
  boolean isInsertAllowed();

  /**
   * @param value true if this model should allow inserts
   */
  void setInsertAllowed(final boolean value);

  /**
   * @return true if this model should allow records to be updated
   */
  boolean isUpdateAllowed();

  /**
   * @param value true if this model should allow records to be updated
   */
  void setUpdateAllowed(final boolean value);

  /**
   * @return true if this model should allow records to be deleted
   */
  boolean isDeleteAllowed();

  /**
   * @param value true if this model should allow records to be deleted
   */
  void setDeleteAllowed(final boolean value);

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
  EntityLookupModel createEntityLookupModel(final String entityID, final List<Property> lookupProperties,
                                            final Criteria additionalSearchCriteria);

  /**
   * @param property the property for which to get the ComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires,
   * if none is specified EntityModel.eventEntitiesChanged is used
   * @param nullValueString the value to use for representing the null item at the top of the list,
   * if this value is null then no such item is included
   * @return a PropertyComboBoxModel representing <code>property</code>, if no combo box model
   * has been initialized for the given property, a new one is created and associated with
   * the property, to be returned the next time this method is called
   */
  ComboBoxModel initializePropertyComboBoxModel(final Property property, final Event refreshEvent, final String nullValueString);

  /**
   * @param property the property for which to get the ComboBoxModel
   * @return a PropertyComboBoxModel representing <code>property</code>
   * @throws RuntimeException if no combo box has been initialized for the given property
   */
  PropertyComboBoxModel getPropertyComboBoxModel(final Property property);

  /**
   * Returns true if this edit model contains a ComboBoxModel for the given property
   * @param propertyID the ID of the property
   * @return true if a ComboBoxModel has been initialized for the given property
   */
  boolean containsComboBoxModel(final String propertyID);

  /**
   * Returns true if this edit model contains a ComboBoxModel for the given property
   * @param property the property
   * @return true if a ComboBoxModel has been initialized for the given property
   */
  boolean containsComboBoxModel(final Property property);

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
   * Creates a combo box model providing the values of the given property
   * @param property the property
   * @param refreshEvent the combo box model is refreshed each time this event is fired
   * @param nullValueString the string to use as a null value caption
   * @return a combo box model based on the given property
   */
  PropertyComboBoxModel createPropertyComboBoxModel(final Property property, final Event refreshEvent,
                                                    final String nullValueString);

  /**
   * Refreshes the Refreshable ComboBoxModels associated with this EntityModel
   * @see org.jminor.common.model.Refreshable
   */
  void refreshComboBoxModels();

  /**
   * Clears the data from all combo boxe models
   */
  void clearComboBoxModels();

  /**
   * @return true if the active entity has been modified
   * @see org.jminor.framework.domain.Entity#isModified()
   */
  boolean isEntityModified();

  /**
   * Performs a insert on the active entity
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws org.jminor.common.model.valuemap.exception.ValidationException in case validation fails
   * @see #validateEntities(java.util.Collection, int)
   */
  void insert() throws CancelException, DbException, ValidationException;

  /**
   * Performs a insert on the given entities
   * @param entities the entities to insert
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws org.jminor.common.model.valuemap.exception.ValidationException in case validation fails
   * @see #validateEntities(java.util.Collection, int)
   */
  void insert(List<Entity> entities) throws CancelException, DbException, ValidationException;

  /**
   * Performs a update on the active entity
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws org.jminor.common.model.valuemap.exception.ValidationException in case validation fails
   * @see #validateEntities(java.util.Collection, int)
   */
  void update() throws CancelException, DbException, ValidationException;

  /**
   * Updates the given Entities. If the entities are unmodified or the list is empty
   * this method returns silently.
   * @param entities the Entities to update
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity was modified by another user
   * @throws ValidationException in case validation fails
   * @see #eventBeforeUpdate
   * @see #eventAfterUpdate
   * @see #validateEntities(java.util.Collection, int)
   */
  void update(final List<Entity> entities) throws DbException, CancelException, ValidationException;

  /**
   * Deletes the active entity
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @see #eventBeforeDelete
   * @see #eventAfterDelete
   */
  void delete() throws DbException, CancelException;

  /**
   * Deletes the given entities, returns silently on recieving an empty list
   * @param entities the entities to delete
   * @throws DbException in case of a database exception
   * @throws CancelException in case the user cancels the operation
   * @see #eventBeforeDelete
   * @see #eventAfterDelete
   */
  void delete(final List<Entity> entities) throws DbException, CancelException;
}
