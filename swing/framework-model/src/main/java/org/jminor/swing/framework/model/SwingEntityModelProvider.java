/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.EntityModelProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A default Swing based {@link EntityModelProvider} implementation.
 */
public class SwingEntityModelProvider
        implements EntityModelProvider<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  protected static final Logger LOG = LoggerFactory.getLogger(SwingEntityModelProvider.class);

  private static final String CONNECTION_PROVIDER_PARAMETER = "connectionProvider";

  private final String entityId;

  private final List<EntityModelProvider<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel>>
          detailModelProviders = new ArrayList<>();

  private Class<? extends SwingEntityModel> modelClass = SwingEntityModel.class;
  private Class<? extends SwingEntityEditModel> editModelClass = SwingEntityEditModel.class;
  private Class<? extends SwingEntityTableModel> tableModelClass = SwingEntityTableModel.class;

  /**
   * Instantiates a new SwingeEntityModelProvider based on the given entity ID
   * @param entityId the entity ID
   */
  public SwingEntityModelProvider(final String entityId) {
    this.entityId = requireNonNull(entityId, "entityId");
  }

  /**
   * Instantiates a new SwingEntityModelProvider based on the given entity ID
   * @param entityId the entity ID
   * @param entityModelClass the entity model class
   */
  public SwingEntityModelProvider(final String entityId, final Class<? extends SwingEntityModel> entityModelClass) {
    this.entityId = requireNonNull(entityId, "entityId");
    this.modelClass = requireNonNull(entityModelClass, "entityModelClass");
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityModelProvider setModelClass(final Class<? extends SwingEntityModel> modelClass) {
    this.modelClass = requireNonNull(modelClass, "modelClass");
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityModelProvider setEditModelClass(final Class<? extends SwingEntityEditModel> editModelClass) {
    this.editModelClass = requireNonNull(editModelClass, "editModelClass");
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityModelProvider setTableModelClass(final Class<? extends SwingEntityTableModel> tableModelClass) {
    this.tableModelClass = requireNonNull(tableModelClass, "tableModelClass");
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Class<? extends SwingEntityModel> getModelClass() {
    return modelClass;
  }

  /** {@inheritDoc} */
  @Override
  public final Class<? extends SwingEntityEditModel> getEditModelClass() {
    return editModelClass;
  }

  /** {@inheritDoc} */
  @Override
  public final Class<? extends SwingEntityTableModel> getTableModelClass() {
    return tableModelClass;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityModelProvider addDetailModelProvider(final EntityModelProvider<SwingEntityModel,
          SwingEntityEditModel, SwingEntityTableModel> detailModelProvider) {
    requireNonNull(detailModelProvider, "detailModelProvider");
    if (!detailModelProviders.contains(detailModelProvider)) {
      detailModelProviders.add(detailModelProvider);
    }

    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsDetailModelProvider(final EntityModelProvider<SwingEntityModel, SwingEntityEditModel,
          SwingEntityTableModel> detailModelProvider) {
    return detailModelProviders.contains(detailModelProvider);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof EntityModelProvider && ((EntityModelProvider) obj).getEntityId().equals(getEntityId());
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    return getEntityId().hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityModel createModel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      final SwingEntityModel model;
      if (modelClass.equals(SwingEntityModel.class)) {
        LOG.debug("{} initializing a default entity model", this);
        model = initializeDefaultModel(connectionProvider);
      }
      else {
        LOG.debug("{} initializing a custom entity model: {}", this, modelClass);
        model = modelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      for (final EntityModelProvider<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> detailProvider : detailModelProviders) {
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

  /** {@inheritDoc} */
  @Override
  public final SwingEntityEditModel createEditModel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      final SwingEntityEditModel editModel;
      if (editModelClass.equals(SwingEntityEditModel.class)) {
        LOG.debug("{} initializing a default model", this);
        editModel = initializeDefaultEditModel(connectionProvider);
      }
      else {
        LOG.debug("{} initializing a custom edit model: {}", this, editModelClass);
        editModel = editModelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
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

  /** {@inheritDoc} */
  @Override
  public final SwingEntityTableModel createTableModel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      final SwingEntityTableModel tableModel;
      if (tableModelClass.equals(SwingEntityTableModel.class)) {
        LOG.debug("{} initializing a default table model", this);
        tableModel = initializeDefaultTableModel(connectionProvider);
      }
      else {
        LOG.debug("{} initializing a custom table model: {}", this, tableModelClass);
        tableModel = tableModelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
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