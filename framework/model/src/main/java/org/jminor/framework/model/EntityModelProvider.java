/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;

/**
 * Specifies a class which provides EntityModel, EntityTableModel and EntityEditModel instances for a given entityId.
 * @param <M> the type of {@link EntityModel} provided
 * @param <E> the type of {@link EntityEditModel} provided
 * @param <T> the type of {@link EntityTableModel} provided
 */
public interface EntityModelProvider<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

  /**
   * @return the entityId of the models provided by this model provider
   */
  String getEntityId();

  /**
   * Creates a {@link EntityModel} instance, based on the given connection provider
   * @param connectionProvider the connection provider
   * @return the entity model instance
   */
  M createModel(EntityConnectionProvider connectionProvider);

  /**
   * Creates a {@link EntityEditModel} instance, based on the given connection provider
   * @param connectionProvider the connection provider
   * @return the edit model instance
   */
  E createEditModel(EntityConnectionProvider connectionProvider);

  /**
   * Creates a {@link EntityTableModel} instance, based on the given connection provider
   * @param connectionProvider the connection provider
   * @return the table model instance
   */
  T createTableModel(EntityConnectionProvider connectionProvider);

  /**
   * Sets the model class
   * @param modelClass the class of the model provided
   * @return this EntityModelProvider instance
   * @throws java.lang.IllegalArgumentException in case modelClass is null
   */
  EntityModelProvider<M, E, T> setModelClass(Class<? extends M> modelClass);

  /**
   * Sets the edit model class
   * @param editModelClass the class of the edit model provided
   * @return this EntityModelProvider instance
   * @throws java.lang.IllegalArgumentException in case editModelClass is null
   */
  EntityModelProvider<M, E, T> setEditModelClass(Class<? extends E> editModelClass);

  /**
   * Sets the table model class
   * @param tableModelClass the class of the table model provided
   * @return this EntityModelProvider instance
   * @throws java.lang.IllegalArgumentException in case tableModelClass is null
   */
  EntityModelProvider<M, E, T> setTableModelClass(Class<? extends T> tableModelClass);

  /**
   * @param detailModelProvider an EntityModelProvider providing a detail model
   * @return this EntityModelProvider instance
   */
  EntityModelProvider<M, E, T> addDetailModelProvider(EntityModelProvider<M, E, T> detailModelProvider);

  /**
   * @param detailModelProvider the detail model provider
   * @return true if this model provider contains the given detail model provider
   */
  boolean containsDetailModelProvider(EntityModelProvider<M, E, T> detailModelProvider);

  /**
   * @return the class of the {@link EntityModel}s provided
   */
  Class<? extends M> getModelClass();

  /**
   * @return the class of the {@link EntityEditModel}s provided
   */
  Class<? extends E> getEditModelClass();

  /**
   * @return the class of the {@link EntityTableModel}s provided
   */
  Class<? extends T> getTableModelClass();
}
