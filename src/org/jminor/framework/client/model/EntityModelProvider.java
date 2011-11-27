/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.provider.EntityConnectionProvider;

/**
 * Specifies a class which provides EntityModel, EntityTableModel and EntityEditModel
 * instances for a given entityID.
 */
public interface EntityModelProvider {

  /**
   * @return the entityID of the models provided by this model provider
   */
  String getEntityID();

  /**
   * Creates a {@link EntityModel} instance, based on the given connection provider
   * @param connectionProvider the connection provider
   * @param detailModel if true the model should be configured as a detail model
   * @return the entity model instance
   */
  EntityModel createModel(final EntityConnectionProvider connectionProvider, final boolean detailModel);

  /**
   * Creates a {@link EntityEditModel} instance, based on the given connection provider
   * @param connectionProvider the connection provider
   * @return the edit model instance
   */
  EntityEditModel createEditModel(final EntityConnectionProvider connectionProvider);

  /**
   * Creates a {@link EntityTableModel} instance, based on the given connection provider
   * @param connectionProvider the connection provider
   * @param detailModel if true the model should be configured as a detail model
   * @return the table model instance
   */
  EntityTableModel createTableModel(final EntityConnectionProvider connectionProvider, final boolean detailModel);

  /**
   * Sets the model class
   * @param modelClass the class of the model provided
   * @return this EntityModelProvider instance
   */
  EntityModelProvider setModelClass(final Class<? extends EntityModel> modelClass);

  /**
   * Sets the edit model class
   * @param editModelClass the class of the edit model provided
   * @return this EntityModelProvider instance
   */
  EntityModelProvider setEditModelClass(final Class<? extends EntityEditModel> editModelClass);

  /**
   * Sets the table model class
   * @param tableModelClass the class of the table model provided
   * @return this EntityModelProvider instance
   */
  EntityModelProvider setTableModelClass(final Class<? extends EntityTableModel> tableModelClass);

  /**
   * @param detailModelProvider an EntityModelProvider providing a detail model
   * @return this EntityModelProvider instance
   */
  EntityModelProvider addDetailModelProvider(final EntityModelProvider detailModelProvider);

  /**
   * @param detailModelProvider the detail model provider
   * @return true if this model provider contains the given detail model provider
   */
  boolean containsDetailModelProvider(final EntityModelProvider detailModelProvider);

  /**
   * @return the class of the {@link EntityModel}s provided
   */
  Class<? extends EntityModel> getModelClass();

  /**
   * @return the class of the {@link EntityEditModel}s provided
   */
  Class<? extends EntityEditModel> getEditModelClass();

  /**
   * @return the class of the {@link EntityTableModel}s provided
   */
  Class<? extends EntityTableModel> getTableModelClass();
}
