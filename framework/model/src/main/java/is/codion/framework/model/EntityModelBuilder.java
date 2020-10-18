/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;

/**
 * Specifies a class which provides EntityModel, EntityTableModel and EntityEditModel instances for a given entityType.
 * @param <M> the type of {@link EntityModel} provided
 * @param <E> the type of {@link EntityEditModel} provided
 * @param <T> the type of {@link EntityTableModel} provided
 */
public interface EntityModelBuilder<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

  /**
   * @return the entityType of the models provided by this model provider
   */
  EntityType<?> getEntityType();

  /**
   * Creates a new {@link EntityModel} instance, based on the given connection provider
   * @param connectionProvider the connection provider
   * @return the entity model instance
   */
  M buildModel(EntityConnectionProvider connectionProvider);

  /**
   * Creates a new {@link EntityEditModel} instance, based on the given connection provider
   * @param connectionProvider the connection provider
   * @return the edit model instance
   */
  E buildEditModel(EntityConnectionProvider connectionProvider);

  /**
   * Creates a new {@link EntityTableModel} instance, based on the given connection provider
   * @param connectionProvider the connection provider
   * @return the table model instance
   */
  T buildTableModel(EntityConnectionProvider connectionProvider);

  /**
   * Sets the model class
   * @param modelClass the class of the model provided
   * @return this EntityModelBuilder instance
   * @throws java.lang.IllegalArgumentException in case modelClass is null
   */
  EntityModelBuilder<M, E, T> modelClass(Class<? extends M> modelClass);

  /**
   * Sets the edit model class
   * @param editModelClass the class of the edit model provided
   * @return this EntityModelBuilder instance
   * @throws java.lang.IllegalArgumentException in case editModelClass is null
   */
  EntityModelBuilder<M, E, T> editModelClass(Class<? extends E> editModelClass);

  /**
   * Sets the table model class
   * @param tableModelClass the class of the table model provided
   * @return this EntityModelBuilder instance
   * @throws java.lang.IllegalArgumentException in case tableModelClass is null
   */
  EntityModelBuilder<M, E, T> tableModelClass(Class<? extends T> tableModelClass);

  /**
   * Adds the given detail model builder to this model builder, if it hasn't been previously added
   * @param detailModelBuilder an EntityModelBuilder providing a detail model
   * @return this EntityModelBuilder instance
   */
  EntityModelBuilder<M, E, T> detailModelBuilder(EntityModelBuilder<M, E, T> detailModelBuilder);

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
