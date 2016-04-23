/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;

/**
 * Specifies a class which provides EntityModel, EntityTableModel and EntityEditModel
 * instances for a given entityID.
 */
public interface EntityModelProvider<Model extends EntityModel<Model, EditModel, TableModel>,
        EditModel extends EntityEditModel, TableModel extends EntityTableModel<EditModel>> {

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
  Model createModel(final EntityConnectionProvider connectionProvider, final boolean detailModel);

  /**
   * Creates a {@link EntityEditModel} instance, based on the given connection provider
   * @param connectionProvider the connection provider
   * @return the edit model instance
   */
  EditModel createEditModel(final EntityConnectionProvider connectionProvider);

  /**
   * Creates a {@link EntityTableModel} instance, based on the given connection provider
   * @param connectionProvider the connection provider
   * @param detailModel if true the model should be configured as a detail model
   * @return the table model instance
   */
  TableModel createTableModel(final EntityConnectionProvider connectionProvider, final boolean detailModel);

  /**
   * Sets the model class
   * @param modelClass the class of the model provided
   * @return this EntityModelProvider instance
   * @throws java.lang.IllegalArgumentException in case modelClass is null
   */
  EntityModelProvider<Model, EditModel, TableModel> setModelClass(final Class<? extends Model> modelClass);

  /**
   * Sets the edit model class
   * @param editModelClass the class of the edit model provided
   * @return this EntityModelProvider instance
   * @throws java.lang.IllegalArgumentException in case editModelClass is null
   */
  EntityModelProvider<Model, EditModel, TableModel> setEditModelClass(final Class<? extends EditModel> editModelClass);

  /**
   * Sets the table model class
   * @param tableModelClass the class of the table model provided
   * @return this EntityModelProvider instance
   * @throws java.lang.IllegalArgumentException in case tableModelClass is null
   */
  EntityModelProvider<Model, EditModel, TableModel> setTableModelClass(final Class<? extends TableModel> tableModelClass);

  /**
   * @param detailModelProvider an EntityModelProvider providing a detail model
   * @return this EntityModelProvider instance
   */
  EntityModelProvider<Model, EditModel, TableModel> addDetailModelProvider(final EntityModelProvider<Model, EditModel, TableModel> detailModelProvider);

  /**
   * @param detailModelProvider the detail model provider
   * @return true if this model provider contains the given detail model provider
   */
  boolean containsDetailModelProvider(final EntityModelProvider<Model, EditModel, TableModel> detailModelProvider);

  /**
   * @return the class of the {@link EntityModel}s provided
   */
  Class<? extends Model> getModelClass();

  /**
   * @return the class of the {@link EntityEditModel}s provided
   */
  Class<? extends EditModel> getEditModelClass();

  /**
   * @return the class of the {@link EntityTableModel}s provided
   */
  Class<? extends TableModel> getTableModelClass();
}
