/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.event.EventDataListener;
import is.codion.common.properties.PropertyValue;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.util.Collection;

/**
 * Specifies a class responsible for, among other things, coordinating a {@link EntityEditModel} and an {@link EntityTableModel}.
 * @param <M> the type of {@link EntityModel} used for detail models
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public interface EntityModel<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

  /**
   * Specifies whether the client should save and apply user preferences<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> USE_CLIENT_PREFERENCES =
          Configuration.booleanValue("is.codion.framework.model.EntityModel.useClientPreferences", true);

  /**
   * @return the type of the entity this entity model is based on
   */
  EntityType entityType();

  /**
   * @return the connection provider used by this entity model
   */
  EntityConnectionProvider connectionProvider();

  /**
   * @return the underlying domain entities
   */
  Entities entities();

  /**
   * @return the {@link EntityEditModel} instance used by this {@link EntityModel}
   */
  E editModel();

  /**
   * @return the {@link EntityTableModel}, null if none is specified
   */
  T tableModel();

  /**
   * @return true if this {@link EntityModel} contains a {@link EntityTableModel}
   */
  boolean containsTableModel();

  /**
   * @return an unmodifiable collection containing the active detail models, that is, those that should respond to master model events
   */
  Collection<M> activeDetailModels();

  /**
   * Adds the given model to the currently active detail models. Active detail models are updated and filtered according
   * to the entity/entities selected in this (the master) model.
   * Calling this method with a model which is already active has no effect.
   * @param detailModel links the given detail model to this model
   */
  void activateDetailModel(M detailModel);

  /**
   * Removes the given model from the currently active detail models. Active models are updated and filtered according
   * to the entity/entities selected in this (the master) model.
   * Calling this method with a model which is not active has no effect.
   * @param detailModel unlinks the given detail model from this model
   */
  void deactivateDetailModel(M detailModel);

  /**
   * Adds the given detail model to this model, a side effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#queryConditionRequiredState()}.
   * Note that the detail model is associated with the first foreign key found referencing this models entity.
   * @param detailModels the detail models to add
   */
  void addDetailModels(M... detailModels);

  /**
   * Adds the given detail model to this model, a side effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#queryConditionRequiredState()}.
   * Note that the detail model is associated with the first foreign key found referencing this models entity.
   * @param detailModel the detail model
   * @return the resulting {@link EntityModelLink}
   */
  ForeignKeyEntityModelLink<M, E, T> addDetailModel(M detailModel);

  /**
   * Adds the given detail model to this model, a side effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#queryConditionRequiredState()}
   * Specify the foreign key in case the detail model is based on an entity which contains multiple foreign keys to the
   * same master entity.
   * @param detailModel the detail model
   * @param foreignKey the foreign key to base the detail model on
   * @return the resulting {@link EntityModelLink}
   */
  ForeignKeyEntityModelLink<M, E, T> addDetailModel(M detailModel, ForeignKey foreignKey);

  /**
   * Adds the given detail model to this model, a side effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#queryConditionRequiredState()}
   * @param modelLink the {@link EntityModelLink} to add
   * @param <L> the {@link EntityModelLink} type
   * @return the {@link EntityModelLink}
   */
  <L extends EntityModelLink<M, E, T>> L addDetailModel(L modelLink);

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
  <T extends M> T detailModel(Class<? extends M> modelClass);

  /**
   * Returns a detail model of the given type
   * @param entityType the entityType of the required EntityModel
   * @return the detail model of type {@code entityModelClass}
   * @throws IllegalArgumentException in case no detail model for the given entityType is found
   */
  M detailModel(EntityType entityType);

  /**
   * @return an unmodifiable collection containing the detail models this model contains
   */
  Collection<M> detailModels();

  /**
   * @param detailModel the detail model
   * @param <L> the {@link EntityModelLink} type
   * @return the model link associated with the given detail model
   */
  <L extends EntityModelLink<M, E, T>> L detailModelLink(M detailModel);

  /**
   * Clears all data models used by this model.
   */
  void clear();

  /**
   * Clears the detail models.
   */
  void clearDetailModels();

  /**
   * @param listener a listener to be notified each time a detail model is activated
   */
  void addDetailModelActivatedListener(EventDataListener<M> listener);

  /**
   * @param listener a listener to be removed
   */
  void removeDetailModelActivatedListener(EventDataListener<M> listener);

  /**
   * @param listener a listener to be notified each time a detail model is deactivated
   */
  void addDetailModelDeactivatedListener(EventDataListener<M> listener);

  /**
   * @param listener a listener to be removed
   */
  void removeDetailModelDeactivatedListener(EventDataListener<M> listener);

  /**
   * Saves any user preferences
   */
  void savePreferences();
}
