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
   * Instantiates a new SwingEntityModel with default EntityEditModel and EntityTableModel implementations.
   * @param entityType the type of the entity this DefaultEntityModel represents
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
     * @return this SwingEntityModel.Builder instance
     * @throws IllegalStateException in case the edit or table model classes have already been set
     */
    Builder modelClass(Class<? extends SwingEntityModel> modelClass);

    /**
     * Sets the edit model class
     * @param editModelClass the edit model class
     * @return this SwingEntityModel.Builder instance
     * @throws IllegalStateException in case the model class has already been set
     * @throws IllegalStateException in case the table model class has already been set
     */
    Builder editModelClass(Class<? extends SwingEntityEditModel> editModelClass);

    /**
     * Sets the table model class
     * @param tableModelClass the table model class
     * @return this SwingEntityModel.Builder instance
     * @throws IllegalStateException in case the model class has already been set
     * @throws IllegalStateException in case the edit model class has already been set
     */
    Builder tableModelClass(Class<? extends SwingEntityTableModel> tableModelClass);

    /**
     * Takes precedence over {@link #modelClass(Class)}.
     * @param modelBuilder builds the model
     * @return this SwingEntityModel.Builder instance
     */
    Builder modelBuilder(ModelBuilder modelBuilder);

    /**
     * Takes precedence over {@link #editModelClass(Class)}.
     * @param editModelBuilder builds the edit model
     * @return this SwingEntityModel.Builder instance
     */
    Builder editModelBuilder(EditModelBuilder editModelBuilder);

    /**
     * Takes precedence over {@link #tableModelClass(Class)}.
     * @param tableModelBuilder builds the table model
     * @return this SwingEntityModel.Builder instance
     */
    Builder tableModelBuilder(TableModelBuilder tableModelBuilder);

    /**
     * @param modelInitializer initializes the model post construction
     * @return this SwingEntityModel.Builder instance
     */
    Builder modelInitializer(Consumer<SwingEntityModel> modelInitializer);

    /**
     * @param editModelInitializer initializes the edit model post construction
     * @return this SwingEntityModel.Builder instance
     */
    Builder editModelInitializer(Consumer<SwingEntityEditModel> editModelInitializer);

    /**
     * @param tableModelInitializer initializes the table model post construction
     * @return this SwingEntityModel.Builder instance
     */
    Builder tableModelInitializer(Consumer<SwingEntityTableModel> tableModelInitializer);

    /**
     * Adds a detail model builder to this model builder
     * @param detailModelBuilder the detail model builder to add
     * @return this SwingEntityModel.Builder instance
     */
    Builder detailModelBuilder(Builder detailModelBuilder);

    /**
     * @return the model class
     */
    Class<? extends SwingEntityModel> getModelClass();

    /**
     * @return the edit model class
     */
    Class<? extends SwingEntityEditModel> getEditModelClass();

    /**
     * @return the table model class
     */
    Class<? extends SwingEntityTableModel> getTableModelClass();

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
     * Builds a SwingEntityModel instance.
     */
    interface ModelBuilder {

      /**
       * @param connectionProvider the connection provider
       * @return a new SwingEntityModel instance
       * @throws Exception in case of an exception
       */
      SwingEntityModel build(EntityConnectionProvider connectionProvider) throws Exception;
    }

    /**
     * Builds a SwingEntityEditModel instance.
     */
    interface EditModelBuilder {

      /**
       * @param connectionProvider the connection provider
       * @return a new SwingEntityEditModel instance
       * @throws Exception in case of an exception
       */
      SwingEntityEditModel build(EntityConnectionProvider connectionProvider) throws Exception;
    }

    /**
     * Builds a SwingEntityTableModel instance.
     */
    interface TableModelBuilder {

      /**
       * @param connectionProvider the connection provider
       * @return a new SwingEntityTableModel instance
       * @throws Exception in case of an exception
       */
      SwingEntityTableModel build(EntityConnectionProvider connectionProvider) throws Exception;
    }
  }
}
