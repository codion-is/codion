/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
   * @param <C> the edit model type
   * @return the {@link EntityEditModel} instance used by this {@link EntityModel}
   */
  <C extends E> C editModel();

  /**
   * @param <C> the table model type
   * @return the {@link EntityTableModel}
   * @throws IllegalStateException in case no table model is available
   */
  <C extends T> C tableModel();

  /**
   * @return true if this {@link EntityModel} contains a {@link EntityTableModel}
   */
  boolean containsTableModel();

  /**
   * @return an unmodifiable collection containing the active detail models, that is, those that should respond to master model events
   */
  Collection<M> activeDetailModels();

  /**
   * Adds the given detail model to this model, a side effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#queryConditionRequiredState()}.
   * Note that each detail model is associated with the first foreign key found referencing this models entity.
   * @param detailModels the detail models to add
   * @throws IllegalArgumentException in case no foreign key exists between the entities involved
   */
  void addDetailModels(M... detailModels);

  /**
   * Adds the given detail model to this model, a side effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#queryConditionRequiredState()}.
   * Note that the detail model is associated with the first foreign key found referencing this models entity.
   * @param detailModel the detail model
   * @return the resulting {@link ForeignKeyDetailModelLink}
   * @throws IllegalArgumentException in case no foreign key exists between the entities involved
   */
  ForeignKeyDetailModelLink<M, E, T> addDetailModel(M detailModel);

  /**
   * Adds the given detail model to this model, a side effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#queryConditionRequiredState()}
   * Specify the foreign key in case the detail model is based on an entity which contains multiple foreign keys to the
   * same master entity.
   * @param detailModel the detail model
   * @param foreignKey the foreign key to base the detail model on
   * @return the resulting {@link ForeignKeyDetailModelLink}
   */
  ForeignKeyDetailModelLink<M, E, T> addDetailModel(M detailModel, ForeignKey foreignKey);

  /**
   * Adds the given detail model to this model, a side effect if the detail model contains
   * a table model is that it is configured so that a query condition is required for it to show
   * any data, via {@link EntityTableModel#queryConditionRequiredState()}
   * @param detailModelLink the {@link DetailModelLink} to add
   * @param <L> the {@link DetailModelLink} type
   * @return the {@link DetailModelLink}
   * @throws IllegalArgumentException in case the model has already been added
   */
  <L extends DetailModelLink<M, E, T>> L addDetailModel(L detailModelLink);

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
   * @param <C> the model type
   * @param modelClass the type of the required {@link EntityModel}
   * @return the detail model of type {@code entityModelClass}
   * @throws IllegalArgumentException in case this model does not contain a detail model of the given type
   */
  <C extends M> C detailModel(Class<C> modelClass);

  /**
   * Returns a detail model of the given type
   * @param <C> the detail model type
   * @param entityType the entityType of the required EntityModel
   * @return the detail model of type {@code entityModelClass}
   * @throws IllegalArgumentException in case this model does not contain a detail model for the entityType
   */
  <C extends M> C detailModel(EntityType entityType);

  /**
   * @return an unmodifiable collection containing the detail models this model contains
   */
  Collection<M> detailModels();

  /**
   * @param detailModel the detail model
   * @param <L> the {@link DetailModelLink} type
   * @return the link for the given detail model
   * @throws IllegalArgumentException in case this model does not contain the given detail model
   */
  <L extends DetailModelLink<M, E, T>> L detailModelLink(M detailModel);

  /**
   * Saves any user preferences for this model, its table model and each detail model.
   * Note that if {@link EntityModel#USE_CLIENT_PREFERENCES} is set to 'false', calling this method has no effect.
   * Remember to call super.savePreferences() when overriding.
   */
  void savePreferences();

  /**
   * @param listener notified each time the active detail models change
   */
  void addActiveDetailModelsListener(EventDataListener<Collection<M>> listener);

  /**
   * @param listener the listener to remove
   */
  void removeActiveDetailModelsListener(EventDataListener<Collection<M>> listener);
}
