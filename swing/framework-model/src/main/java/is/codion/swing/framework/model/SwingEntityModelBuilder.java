/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.property.Identity;
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

  protected static final Logger LOG = LoggerFactory.getLogger(SwingEntityModelBuilder.class);

  private static final String CONNECTION_PROVIDER_PARAMETER = "connectionProvider";

  private final Identity entityId;

  private final List<EntityModelBuilder<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel>>
          detailModelBuilders = new ArrayList<>();

  private Class<? extends SwingEntityModel> modelClass;
  private Class<? extends SwingEntityEditModel> editModelClass;
  private Class<? extends SwingEntityTableModel> tableModelClass;

  /**
   * Instantiates a new SwingeEntityModelBuilder based on the given  entityId
   * @param entityId the entityId
   */
  public SwingEntityModelBuilder(final Identity entityId) {
    this.entityId = requireNonNull(entityId, "entityId");
  }

  @Override
  public final Identity getEntityId() {
    return entityId;
  }

  @Override
  public final SwingEntityModelBuilder setModelClass(final Class<? extends SwingEntityModel> modelClass) {
    if (editModelClass != null || tableModelClass != null) {
      throw new IllegalStateException("Edit or table model class has been set");
    }
    this.modelClass = requireNonNull(modelClass, "modelClass");
    return this;
  }

  @Override
  public final SwingEntityModelBuilder setEditModelClass(final Class<? extends SwingEntityEditModel> editModelClass) {
    if (modelClass != null) {
      throw new IllegalStateException("Model class has been set");
    }
    this.editModelClass = requireNonNull(editModelClass, "editModelClass");
    return this;
  }

  @Override
  public final SwingEntityModelBuilder setTableModelClass(final Class<? extends SwingEntityTableModel> tableModelClass) {
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
  public final SwingEntityModelBuilder addDetailModelBuilder(final EntityModelBuilder<SwingEntityModel,
            SwingEntityEditModel, SwingEntityTableModel> detailModelBuilder) {
    requireNonNull(detailModelBuilder, "detailModelBuilder");
    if (!detailModelBuilders.contains(detailModelBuilder)) {
      detailModelBuilders.add(detailModelBuilder);
    }

    return this;
  }

  @Override
  public final boolean containsDetailModelBuilder(final EntityModelBuilder<SwingEntityModel, SwingEntityEditModel,
            SwingEntityTableModel> detailModelBuilder) {
    return detailModelBuilders.contains(detailModelBuilder);
  }

  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof EntityModelBuilder && ((EntityModelBuilder) obj).getEntityId().equals(getEntityId());
  }

  @Override
  public final int hashCode() {
    return getEntityId().hashCode();
  }

  @Override
  public final SwingEntityModel createModel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      final SwingEntityModel model;
      if (getModelClass().equals(SwingEntityModel.class)) {
        LOG.debug("{} initializing a default entity model", this);
        model = initializeDefaultModel(connectionProvider);
      }
      else {
        LOG.debug("{} initializing a custom entity model: {}", this, getModelClass());
        model = getModelClass().getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      for (final EntityModelBuilder<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> detailProvider : detailModelBuilders) {
        model.addDetailModel(detailProvider.createModel(connectionProvider));
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
  public final SwingEntityEditModel createEditModel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      final SwingEntityEditModel editModel;
      if (getEditModelClass().equals(SwingEntityEditModel.class)) {
        LOG.debug("{} initializing a default model", this);
        editModel = initializeDefaultEditModel(connectionProvider);
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
  public final SwingEntityTableModel createTableModel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      final SwingEntityTableModel tableModel;
      if (getTableModelClass().equals(SwingEntityTableModel.class)) {
        LOG.debug("{} initializing a default table model", this);
        tableModel = initializeDefaultTableModel(connectionProvider);
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

  private SwingEntityModel initializeDefaultModel(final EntityConnectionProvider connectionProvider) {
    final SwingEntityTableModel tableModel = createTableModel(connectionProvider);
    final SwingEntityEditModel editModel = tableModel.hasEditModel() ?
            tableModel.getEditModel() : createEditModel(connectionProvider);

    return new SwingEntityModel(editModel, tableModel);
  }

  private SwingEntityEditModel initializeDefaultEditModel(final EntityConnectionProvider connectionProvider) {
    return new SwingEntityEditModel(entityId, connectionProvider);
  }

  private SwingEntityTableModel initializeDefaultTableModel(final EntityConnectionProvider connectionProvider) {
    return new SwingEntityTableModel(entityId, connectionProvider);
  }
}