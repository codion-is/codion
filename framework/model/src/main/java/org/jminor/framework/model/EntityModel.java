/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Configuration;
import org.jminor.common.Util;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.model.Refreshable;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;

import java.util.Collection;
import java.util.List;

/**
 * Specifies a class responsible for, among other things, coordinating a {@link EntityEditModel} and an {@link EntityTableModel}.
 * @param <M> the type of {@link EntityModel} used for detail models
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public interface EntityModel<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>>
        extends Refreshable, EntityDataProvider {

  /**
   * Specifies whether or not a table model should be automatically filtered when an insert is performed
   * in a master model, using the inserted entity.
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> FILTER_ON_MASTER_INSERT = Configuration.booleanValue(
          "org.jminor.framework.model.EntityModel.filterOnMasterInsert", false);

  /**
   * Specifies whether or not the client should save and apply user preferences<br>
   * Value type: Boolean<br>
   * Default value: true if required JSON library is found on classpath, false otherwise
   */
  PropertyValue<Boolean> USE_CLIENT_PREFERENCES = Configuration.booleanValue(
          "org.jminor.framework.model.EntityModel.useClientPreferences", Util.onClasspath("org.json.JSONObject"));

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
  void addLinkedDetailModel(final M detailModel);

  /**
   * Removes the given model from the currently linked detail models. Linked models are updated and filtered according
   * to the entity/entities selected in this (the master) model.
   * Calling this method with a null argument or a model which is not linked is safe.
   * @param detailModel unlinks the given detail model from this model
   */
  void removeLinkedDetailModel(final M detailModel);

  /**
   * Initializes this {@link EntityModel} according to the given foreign key entities.
   * It sets the value for the first available foreign key property representing the given entityId
   * in the {@link EntityEditModel} and sets the search values in the {@link EntityTableModel}.
   * @param foreignKeyEntityId the ID of the master entity
   * @param foreignKeyValues the master entities
   */
  void initialize(final String foreignKeyEntityId, final List<Entity> foreignKeyValues);

  /**
   * Initializes this {@link EntityModel} according to the given foreign key entities,
   * sets the appropriate property value in the {@link EntityEditModel} and filters the {@link EntityTableModel}
   * @param foreignKeyProperty the ID of the foreign key
   * @param foreignKeyValues the foreign key values
   */
  void initialize(final ForeignKeyProperty foreignKeyProperty, final List<Entity> foreignKeyValues);

  /**
   * Sets the model serving as master model
   * @param entityModel the master entity model
   * @throws IllegalStateException if the master model has already been set
   */
  void setMasterModel(final M entityModel);

  /**
   * @return the master model, if any
   */
  M getMasterModel();

  /**
   * Adds the given detail model to this model, sets this model as the master model of the
   * given detail models via {@link #setMasterModel(EntityModel)}, a side-effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#getQueryConditionRequiredState()}
   * @param detailModels the detail models to add
   */
  void addDetailModels(final M... detailModels);

  /**
   * Adds the given detail model to this model, sets this model as the master model of the
   * given detail model via {@link #setMasterModel(EntityModel)}, a side-effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#getQueryConditionRequiredState()}
   * @param detailModel the detail model
   * @return the detail model just added
   */
  M addDetailModel(final M detailModel);

  /**
   * @param modelClass the detail model class
   * @return true if this model contains a detail model of the given class
   */
  boolean containsDetailModel(final Class<? extends M> modelClass);

  /**
   * @param entityId the entity ID
   * @return true if this model contains a detail model for the given entity ID
   */
  boolean containsDetailModel(final String entityId);

  /**
   * @param detailModel the detail model
   * @return true if this model contains the given detail model
   */
  boolean containsDetailModel(final M detailModel);

  /**
   * Returns the first detail model of the given type
   * @param modelClass the type of the required {@link EntityModel}
   * @return the detail model of type {@code entityModelClass}
   * @throws IllegalArgumentException in case a model is not found
   */
  M getDetailModel(final Class<? extends M> modelClass);

  /**
   * Returns a detail model of the given type
   * @param entityId the entity ID of the required EntityModel
   * @return the detail model of type {@code entityModelClass}
   * @throws IllegalArgumentException in case no detail model for the given entityId is found
   */
  M getDetailModel(final String entityId);

  /**
   * @return an unmodifiable collection containing the detail models this model contains
   */
  Collection<M> getDetailModels();

  /**
   * Indicates that the given detail model is based on the foreign key with the given ID, this becomes
   * practical when a detail model is based on an entity which contains multiple foreign keys to the
   * same master entity. When initializing this detail model only the value for that foreignKeyProperty is set.
   * If {@code foreignKeyPropertyId} is null the association is removed.
   * @param detailModel the detail model
   * @param foreignKeyPropertyId the foreign key property ID
   * @see #initialize(ForeignKeyProperty, java.util.List)
   * @throws IllegalArgumentException in case this EntityModel does not contain the given detail model
   */
  void setDetailModelForeignKey(final M detailModel, final String foreignKeyPropertyId);

  /**
   * @param detailModel the detail model
   * @return the {@link ForeignKeyProperty}
   * the given detail model is based on, null if none has been defined
   */
  ForeignKeyProperty getDetailModelForeignKey(final M detailModel);

  /**
   * Refreshes the detail models.
   */
  void refreshDetailModels();

  /**
   * Clears the detail models.
   */
  void clearDetailModels();

  /**
   * @return true if this table model is automatically filtered when insert is performed in a master model
   * @see EntityModel#FILTER_ON_MASTER_INSERT
   */
  boolean isFilterOnMasterInsert();

  /**
   * @param filterDetailOnInsert if true then the table model is automatically filtered when insert is performed in a master model
   * @see EntityModel#FILTER_ON_MASTER_INSERT
   */
  void setFilterOnMasterInsert(final boolean filterDetailOnInsert);

  /**
   * @param listener a listener to be notified before a refresh is performed
   */
  void addBeforeRefreshListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeBeforeRefreshListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time a refresh has been performed
   */
  void addAfterRefreshListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeAfterRefreshListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time a linked detail model is added
   */
  void addLinkedDetailModelAddedListener(final EventDataListener<M> listener);

  /**
   * @param listener a listener to be removed
   */
  void removeLinkedDetailModelAddedListener(final EventDataListener listener);

    /**
   * @param listener a listener to be notified each time a linked detail model is removed
   */
  void addLinkedDetailModelRemovedListener(final EventDataListener<M> listener);

    /**
   * @param listener a listener to be removed
   */
  void removeLinkedDetailModelRemovedListener(final EventDataListener listener);

  /**
   * Saves any user preferences
   */
  void savePreferences();
}
