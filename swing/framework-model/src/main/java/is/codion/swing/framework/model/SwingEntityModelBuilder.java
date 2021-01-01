/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.EntityModelBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A default Swing based {@link EntityModelBuilder} implementation.
 */
public class SwingEntityModelBuilder
        implements EntityModelBuilder<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  private static final Logger LOG = LoggerFactory.getLogger(SwingEntityModelBuilder.class);

  private static final String CONNECTION_PROVIDER_PARAMETER = "connectionProvider";

  private final EntityType<?> entityType;

  private final List<EntityModelBuilder<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel>>
          detailModelBuilders = new ArrayList<>();

  private Class<? extends SwingEntityModel> modelClass;
  private Class<? extends SwingEntityEditModel> editModelClass;
  private Class<? extends SwingEntityTableModel> tableModelClass;

  /**
   * Instantiates a new SwingeEntityModelBuilder based on the given entityType
   * @param entityType the entityType
   */
  public SwingEntityModelBuilder(final EntityType<?> entityType) {
    this.entityType = requireNonNull(entityType, "entityType");
  }

  @Override
  public final EntityType<?> getEntityType() {
    return entityType;
  }

  @Override
  public final SwingEntityModelBuilder modelClass(final Class<? extends SwingEntityModel> modelClass) {
    if (editModelClass != null || tableModelClass != null) {
      throw new IllegalStateException("Edit or table model class has been set");
    }
    this.modelClass = requireNonNull(modelClass, "modelClass");
    return this;
  }

  @Override
  public final SwingEntityModelBuilder editModelClass(final Class<? extends SwingEntityEditModel> editModelClass) {
    if (modelClass != null) {
      throw new IllegalStateException("Model class has been set");
    }
    this.editModelClass = requireNonNull(editModelClass, "editModelClass");
    return this;
  }

  @Override
  public final SwingEntityModelBuilder tableModelClass(final Class<? extends SwingEntityTableModel> tableModelClass) {
    if (modelClass != null) {
      throw new IllegalStateException("Model class has been set");
    }
    this.tableModelClass = requireNonNull(tableModelClass, "tableModelClass");
    return this;
  }

  @Override
  public final Class<? extends SwingEntityModel> getModelClass() {
    return modelClass == null ? SwingEntityModel.class : modelClass;
  }

  @Override
  public final Class<? extends SwingEntityEditModel> getEditModelClass() {
    return editModelClass ==  null ? SwingEntityEditModel.class : editModelClass;
  }

  @Override
  public final Class<? extends SwingEntityTableModel> getTableModelClass() {
    return tableModelClass == null ? SwingEntityTableModel.class : tableModelClass;
  }

  @Override
  public final SwingEntityModelBuilder detailModelBuilder(final EntityModelBuilder<SwingEntityModel,
            SwingEntityEditModel, SwingEntityTableModel> detailModelBuilder) {
    requireNonNull(detailModelBuilder, "detailModelBuilder");
    if (!detailModelBuilders.contains(detailModelBuilder)) {
      detailModelBuilders.add(detailModelBuilder);
    }

    return this;
  }

  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof EntityModelBuilder && ((EntityModelBuilder<?, ?, ?>) obj).getEntityType().equals(getEntityType());
  }

  @Override
  public final int hashCode() {
    return getEntityType().hashCode();
  }

  @Override
  public final SwingEntityModel buildModel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      final SwingEntityModel model;
      if (getModelClass().equals(SwingEntityModel.class)) {
        LOG.debug("{} initializing a default entity model", this);
        model = buildDefaultModel(connectionProvider);
      }
      else {
        LOG.debug("{} initializing a custom entity model: {}", this, getModelClass());
        model = getModelClass().getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      for (final EntityModelBuilder<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> detailProvider : detailModelBuilders) {
        model.addDetailModel(detailProvider.buildModel(connectionProvider));
      }
      configureModel(model);

      return model;
    }
    catch (final RuntimeException re) {
      throw re;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public final SwingEntityEditModel buildEditModel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      final SwingEntityEditModel editModel;
      if (getEditModelClass().equals(SwingEntityEditModel.class)) {
        LOG.debug("{} initializing a default model", this);
        editModel = new SwingEntityEditModel(entityType, connectionProvider);
      }
      else {
        LOG.debug("{} initializing a custom edit model: {}", this, getEditModelClass());
        editModel = getEditModelClass().getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      configureEditModel(editModel);

      return editModel;
    }
    catch (final RuntimeException re) {
      throw re;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public final SwingEntityTableModel buildTableModel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      final SwingEntityTableModel tableModel;
      if (getTableModelClass().equals(SwingEntityTableModel.class)) {
        LOG.debug("{} initializing a default table model", this);
        tableModel = new SwingEntityTableModel(entityType, connectionProvider);
      }
      else {
        LOG.debug("{} initializing a custom table model: {}", this, getTableModelClass());
        tableModel = getTableModelClass().getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      configureTableModel(tableModel);

      return tableModel;
    }
    catch (final RuntimeException re) {
      throw re;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Override to configure the provided EntityModel instance.
   * Called after each initialization.
   * @param entityModel the entity model to configure
   */
  protected void configureModel(final SwingEntityModel entityModel) {/*Provided for subclasses*/}

  /**
   * Override to configure the provided EntityEditModel instance.
   * Called after each initialization.
   * @param editModel the edit model to configure
   */
  protected void configureEditModel(final SwingEntityEditModel editModel) {/*Provided for subclasses*/}

  /**
   * Override to configure the provided EntityTableModel instance.
   * Called after each initialization.
   * @param tableModel the edit model to configure
   */
  protected void configureTableModel(final SwingEntityTableModel tableModel) {/*Provided for subclasses*/}

  private SwingEntityModel buildDefaultModel(final EntityConnectionProvider connectionProvider) {
    final SwingEntityTableModel tableModel = buildTableModel(connectionProvider);
    final SwingEntityEditModel editModel = tableModel.hasEditModel() ?
            tableModel.getEditModel() : buildEditModel(connectionProvider);

    return new SwingEntityModel(editModel, tableModel);
  }
}