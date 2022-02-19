/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.event.EventDataListener;
import is.codion.common.value.PropertyValue;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.util.Collection;
import java.util.List;

/**
 * Specifies a class responsible for, among other things, coordinating a {@link EntityEditModel} and an {@link EntityTableModel}.
 * @param <M> the type of {@link EntityModel} used for detail models
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public interface EntityModel<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

  /**
   * Specifies whether a table model should automatically search by the inserted entity
   * when an insert is performed in a master model.
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> SEARCH_ON_MASTER_INSERT = Configuration.booleanValue(
          "is.codion.framework.model.EntityModel.searchOnMasterInsert", false);

  /**
   * Specifies whether the client should save and apply user preferences<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> USE_CLIENT_PREFERENCES = Configuration.booleanValue(
          "is.codion.framework.model.EntityModel.useClientPreferences", true);

  /**
   * @return the type of the entity this entity model is based on
   */
  EntityType getEntityType();

  /**
   * @return the connection provider used by this entity model
   */
  EntityConnectionProvider getConnectionProvider();

  /**
   * @return the underlying domain entities
   */
  Entities getEntities();

  /**
   * @return the {@link EntityEditModel} instance used by this {@link EntityModel}
   */
  E getEditModel();

  /**
   * @return the {@link EntityTableModel}, null if none is specified
   */
  T getTableModel();

  /**
   * @return true if this {@link EntityModel} contains a {@link EntityTableModel}
   */
  boolean containsTableModel();

  /**
   * @return an unmodifiable collection containing the detail models that are currently linked to this model
   */
  Collection<M> getLinkedDetailModels();

  /**
   * Adds the given model to the currently linked detail models. Linked models are updated and filtered according
   * to the entity/entities selected in this (the master) model.
   * Calling this method with a null argument or a model which is already linked is safe.
   * @param detailModel links the given detail model to this model
   */
  void addLinkedDetailModel(M detailModel);

  /**
   * Removes the given model from the currently linked detail models. Linked models are updated and filtered according
   * to the entity/entities selected in this (the master) model.
   * Calling this method with a null argument or a model which is not linked is safe.
   * @param detailModel unlinks the given detail model from this model
   */
  void removeLinkedDetailModel(M detailModel);

  /**
   * Initializes this {@link EntityModel} according to the given foreign key entities.
   * It sets the value for the first available foreign key property representing the given entityType
   * in the {@link EntityEditModel} and sets the search values in the {@link EntityTableModel}.
   * @param foreignKeyEntityType the id of the master entity
   * @param foreignKeyValues the master entities, empty list for none
   */
  void initialize(EntityType foreignKeyEntityType, List<Entity> foreignKeyValues);

  /**
   * Initializes this {@link EntityModel} according to the given foreign key entities,
   * sets the appropriate attribute value in the {@link EntityEditModel} and filters the {@link EntityTableModel}
   * @param foreignKey the foreign key
   * @param foreignKeyValues the foreign key values, empty list for none
   */
  void initialize(ForeignKey foreignKey, List<Entity> foreignKeyValues);

  /**
   * Sets the model serving as master model
   * @param entityModel the master entity model
   * @throws IllegalStateException if the master model has already been set
   */
  void setMasterModel(M entityModel);

  /**
   * @return the master model, if any
   */
  M getMasterModel();

  /**
   * Adds the given detail model to this model, sets this model as the master model of the
   * given detail models via {@link #setMasterModel(EntityModel)}, a side effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#getQueryConditionRequiredState()}
   * @param detailModels the detail models to add
   */
  void addDetailModels(M... detailModels);

  /**
   * Adds the given detail model to this model, sets this model as the master model of the
   * given detail model via {@link #setMasterModel(EntityModel)}, a side effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#getQueryConditionRequiredState()}
   * @param detailModel the detail model
   * @return the detail model just added
   */
  M addDetailModel(M detailModel);

  /**
   * @param modelClass the detail model class
   * @return true if this model contains a detail model of the given class
   */
  boolean containsDetailModel(Class<? extends M> modelClass);

  /**
   * @param entityType the entityType
   * @return true if this model contains a detail model for the given entityType
   */
  boolean containsDetailModel(EntityType entityType);

  /**
   * @param detailModel the detail model
   * @return true if this model contains the given detail model
   */
  boolean containsDetailModel(M detailModel);

  /**
   * Returns the first detail model of the given type
   * @param <T> the model type
   * @param modelClass the type of the required {@link EntityModel}
   * @return the detail model of type {@code entityModelClass}
   * @throws IllegalArgumentException in case a model is not found
   */
  <T extends M> T getDetailModel(Class<? extends M> modelClass);

  /**
   * Returns a detail model of the given type
   * @param entityType the entityType of the required EntityModel
   * @return the detail model of type {@code entityModelClass}
   * @throws IllegalArgumentException in case no detail model for the given entityType is found
   */
  M getDetailModel(EntityType entityType);

  /**
   * @return an unmodifiable collection containing the detail models this model contains
   */
  Collection<M> getDetailModels();

  /**
   * Indicates that the given detail model is based on the foreign key attribute, this becomes
   * practical when a detail model is based on an entity which contains multiple foreign keys to the
   * same master entity. When initializing this detail model only the value for that foreignKey is set.
   * If {@code foreignKey} is null the association is removed.
   * @param detailModel the detail model
   * @param foreignKey the foreign key
   * @see #initialize(ForeignKey, List)
   * @throws IllegalArgumentException in case this EntityModel does not contain the given detail model
   */
  void setDetailModelForeignKey(M detailModel, ForeignKey foreignKey);

  /**
   * @param detailModel the detail model
   * @return the foreign key the given detail model is based on, null if none has been defined
   */
  ForeignKey getDetailModelForeignKey(M detailModel);

  /**
   * Clears all data models used by this model.
   */
  void clear();

  /**
   * Clears the detail models.
   */
  void clearDetailModels();

  /**
   * @return true if this models table model should automatically search by the inserted entity
   * when an insert is performed in a master model
   * @see EntityModel#SEARCH_ON_MASTER_INSERT
   */
  boolean isSearchOnMasterInsert();

  /**
   * @param searchOnMasterInsert if true then this models table model will automatically search by the inserted entity
   * when an insert is performed in a master model
   * @see EntityModel#SEARCH_ON_MASTER_INSERT
   */
  void setSearchOnMasterInsert(boolean searchOnMasterInsert);

  /**
   * @param listener a listener to be notified each time a linked detail model is added
   */
  void addLinkedDetailModelAddedListener(EventDataListener<M> listener);

  /**
   * @param listener a listener to be removed
   */
  void removeLinkedDetailModelAddedListener(EventDataListener<M> listener);

    /**
   * @param listener a listener to be notified each time a linked detail model is removed
   */
  void addLinkedDetailModelRemovedListener(EventDataListener<M> listener);

    /**
   * @param listener a listener to be removed
   */
  void removeLinkedDetailModelRemovedListener(EventDataListener<M> listener);

  /**
   * Saves any user preferences
   */
  void savePreferences();
}
