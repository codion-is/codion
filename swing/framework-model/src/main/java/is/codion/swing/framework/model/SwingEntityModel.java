/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.DefaultEntityModel;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link is.codion.framework.model.EntityModel}
 */
public class SwingEntityModel extends DefaultEntityModel<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  /**
   * Instantiates a new SwingEntityModel with default SwingEntityEditModel and SwingEntityTableModel implementations.
   * @param entityType the type of the entity to base this SwingEntityModel on
   * @param connectionProvider a EntityConnectionProvider
   */
  public SwingEntityModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this(new SwingEntityEditModel(requireNonNull(entityType, "entityType"),
            requireNonNull(connectionProvider, "connectionProvider")));
  }

  /**
   * Instantiates a new SwingEntityModel, with a default {@link SwingEntityTableModel}
   * @param editModel the edit model
   */
  public SwingEntityModel(SwingEntityEditModel editModel) {
    super(new SwingEntityTableModel(editModel));
  }

  /**
   * Instantiates a new SwingEntityModel
   * @param tableModel the table model
   */
  public SwingEntityModel(SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  /**
   * Instantiates a new {@link SwingEntityModel.Builder} instance
   * @param entityType the entity type
   * @return a new builder instance
   */
  public static SwingEntityModel.Builder builder(EntityType entityType) {
    return new SwingEntityModelBuilder(entityType);
  }

  /**
   * Builds a {@link SwingEntityModel}.
   */
  public interface Builder {

    /**
     * @return the underlying entity type
     */
    EntityType getEntityType();

    /**
     * Sets the model class
     * @param modelClass the model class
     * @return this builder instance
     * @throws IllegalStateException in case the edit or table model classes have already been set
     */
    Builder modelClass(Class<? extends SwingEntityModel> modelClass);

    /**
     * Sets the edit model class
     * @param editModelClass the edit model class
     * @return this builder instance
     * @throws IllegalStateException in case the model class has already been set
     * @throws IllegalStateException in case the table model class has already been set
     */
    Builder editModelClass(Class<? extends SwingEntityEditModel> editModelClass);

    /**
     * Sets the table model class
     * @param tableModelClass the table model class
     * @return this builder instance
     * @throws IllegalStateException in case the model class has already been set
     * @throws IllegalStateException in case the edit model class has already been set
     */
    Builder tableModelClass(Class<? extends SwingEntityTableModel> tableModelClass);

    /**
     * Takes precedence over {@link #modelClass(Class)}.
     * @param modelFactory creates the model
     * @return this builder instance
     */
    Builder modelFactory(ModelFactory<SwingEntityModel> modelFactory);

    /**
     * Takes precedence over {@link #editModelClass(Class)}.
     * @param editModelFactory creates the edit model
     * @return this builder instance
     */
    Builder editModelFactory(ModelFactory<SwingEntityEditModel> editModelFactory);

    /**
     * Takes precedence over {@link #tableModelClass(Class)}.
     * @param tableModelFactory creates the table model
     * @return this builder instance
     */
    Builder tableModelFactory(ModelFactory<SwingEntityTableModel> tableModelFactory);

    /**
     * @param onBuildModel called after the entity model has been built
     * @return this builder instance
     */
    Builder onBuildModel(Consumer<SwingEntityModel> onBuildModel);

    /**
     * @param onBuildEditModel called after the edit model has been built
     * @return this builder instance
     */
    Builder onBuildEditModel(Consumer<SwingEntityEditModel> onBuildEditModel);

    /**
     * @param onBuildTableModel called after the table model has been built
     * @return this builder instance
     */
    Builder onBuildTableModel(Consumer<SwingEntityTableModel> onBuildTableModel);

    /**
     * Adds a detail model builder to this model builder
     * @param detailModelBuilder the detail model builder to add
     * @return this builder instance
     */
    Builder detailModelBuilder(Builder detailModelBuilder);

    /**
     * Builds a {@link SwingEntityModel} instance
     * @param connectionProvider the connection provider
     * @return a SwingEntityModel instance
     */
    SwingEntityModel buildModel(EntityConnectionProvider connectionProvider);

    /**
     * Builds a {@link SwingEntityEditModel} instance
     * @param connectionProvider the connection provider
     * @return a SwingEntityEditModel instance
     */
    SwingEntityEditModel buildEditModel(EntityConnectionProvider connectionProvider);

    /**
     * Builds a {@link SwingEntityTableModel} instance
     * @param connectionProvider the connection provider
     * @return a SwingEntityTableModel instance
     */
    SwingEntityTableModel buildTableModel(EntityConnectionProvider connectionProvider);

    /**
     * Creates a model instance.
     */
    interface ModelFactory<T> {

      /**
       * @param connectionProvider the connection provider to base the model on
       * @return a new model instance
       * @throws Exception in case of an exception
       */
      T create(EntityConnectionProvider connectionProvider) throws Exception;
    }
  }
}
