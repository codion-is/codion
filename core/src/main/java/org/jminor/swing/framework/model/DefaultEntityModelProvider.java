/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.EntityModelProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A default {@link EntityModelProvider} implementation.
 */
public class DefaultEntityModelProvider implements EntityModelProvider<SwingEntityModel, SwingEntityEditModel, DefaultEntityTableModel> {

  protected static final Logger LOG = LoggerFactory.getLogger(DefaultEntityModelProvider.class);

  private final String entityID;

  private final List<EntityModelProvider<SwingEntityModel, SwingEntityEditModel, DefaultEntityTableModel>> detailModelProviders = new ArrayList<>();

  private Class<? extends SwingEntityModel> modelClass = SwingEntityModel.class;
  private Class<? extends SwingEntityEditModel> editModelClass = SwingEntityEditModel.class;
  private Class<? extends DefaultEntityTableModel> tableModelClass = DefaultEntityTableModel.class;

  /**
   * Instantiates a new DefaultEntityModelProvider based on the given entity ID
   * @param entityID the entity ID
   */
  public DefaultEntityModelProvider(final String entityID) {
    this.entityID = entityID;
  }

  /**
   * Instantiates a new DefaultEntityModelProvider based on the given entity ID
   * @param entityID the entity ID
   * @param entityModelClass the entity model class
   */
  public DefaultEntityModelProvider(final String entityID, final Class<? extends SwingEntityModel> entityModelClass) {
    this.entityID = entityID;
    this.modelClass = entityModelClass;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final DefaultEntityModelProvider setModelClass(final Class<? extends SwingEntityModel> modelClass) {
    Util.rejectNullValue(modelClass, "modelClass");
    this.modelClass = modelClass;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final DefaultEntityModelProvider setEditModelClass(final Class<? extends SwingEntityEditModel> editModelClass) {
    Util.rejectNullValue(editModelClass, "editModelClass");
    this.editModelClass = editModelClass;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final DefaultEntityModelProvider setTableModelClass(final Class<? extends DefaultEntityTableModel> tableModelClass) {
    Util.rejectNullValue(tableModelClass, "tableModelClass");
    this.tableModelClass = tableModelClass;
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
  public final Class<? extends DefaultEntityTableModel> getTableModelClass() {
    return tableModelClass;
  }

  /** {@inheritDoc} */
  @Override
  public final DefaultEntityModelProvider addDetailModelProvider(final EntityModelProvider<SwingEntityModel,
          SwingEntityEditModel, DefaultEntityTableModel> detailModelProvider) {
    if (!detailModelProviders.contains(detailModelProvider)) {
      detailModelProviders.add(detailModelProvider);
    }

    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsDetailModelProvider(final EntityModelProvider<SwingEntityModel, SwingEntityEditModel,
          DefaultEntityTableModel> detailModelProvider) {
    return detailModelProviders.contains(detailModelProvider);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof EntityModelProvider && ((EntityModelProvider) obj).getEntityID().equals(getEntityID());
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    return getEntityID().hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityModel createModel(final EntityConnectionProvider connectionProvider, final boolean detailModel) {
    try {
      final SwingEntityModel model;
      if (modelClass.equals(SwingEntityModel.class)) {
        LOG.debug("{} initializing a default entity model", this);
        model = initializeDefaultModel(connectionProvider, detailModel);
      }
      else {
        LOG.debug("{} initializing a custom entity model: {}", this, modelClass);
        model = modelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      for (final EntityModelProvider<SwingEntityModel, SwingEntityEditModel, DefaultEntityTableModel> detailProvider : detailModelProviders) {
        model.addDetailModel(detailProvider.createModel(connectionProvider, true));
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
  public final DefaultEntityTableModel createTableModel(final EntityConnectionProvider connectionProvider, final boolean detailModel) {
    try {
      final DefaultEntityTableModel tableModel;
      if (tableModelClass.equals(DefaultEntityTableModel.class)) {
        LOG.debug("{} initializing a default table model", this);
        tableModel = initializeDefaultTableModel(connectionProvider);
      }
      else {
        LOG.debug("{} initializing a custom table model: {}", this, tableModelClass);
        tableModel = tableModelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      if (detailModel) {
        tableModel.setQueryCriteriaRequired(true);
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
  protected void configureTableModel(final DefaultEntityTableModel tableModel) {/*Provided for subclasses*/}

  private SwingEntityModel initializeDefaultModel(final EntityConnectionProvider connectionProvider, final boolean detailModel) {
    final DefaultEntityTableModel tableModel = createTableModel(connectionProvider, detailModel);
    if (!tableModel.hasEditModel()) {
      final SwingEntityEditModel editModel = createEditModel(connectionProvider);
      tableModel.setEditModel(editModel);
    }

    return new SwingEntityModel(tableModel.getEditModel(), tableModel);
  }

  private SwingEntityEditModel initializeDefaultEditModel(final EntityConnectionProvider connectionProvider) {
    return new SwingEntityEditModel(entityID, connectionProvider);
  }

  private DefaultEntityTableModel initializeDefaultTableModel(final EntityConnectionProvider connectionProvider) {
    return new DefaultEntityTableModel(entityID, connectionProvider);
  }
}