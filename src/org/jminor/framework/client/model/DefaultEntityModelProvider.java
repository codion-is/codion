/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.provider.EntityConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default {@link EntityModelProvider} implementation.
 */
public class DefaultEntityModelProvider implements EntityModelProvider {

  protected static final Logger LOG = LoggerFactory.getLogger(DefaultEntityModelProvider.class);

  private final String entityID;

  private Class<? extends EntityModel> modelClass = DefaultEntityModel.class;
  private Class<? extends EntityEditModel> editModelClass = DefaultEntityEditModel.class;
  private Class<? extends EntityTableModel> tableModelClass = DefaultEntityTableModel.class;

  /**
   * Instantiates a new DefaultEntityModelProvider based on the given entity ID
   * @param entityID the entity ID
   */
  public DefaultEntityModelProvider(final String entityID) {
    this(entityID, DefaultEntityModel.class);
  }

  /**
   * Instantiates a new DefaultEntityModelProvider based on the given entity ID
   * @param entityID the entity ID
   * @param entityModelClass the entity model class
   */
  public DefaultEntityModelProvider(final String entityID, final Class<? extends EntityModel> entityModelClass) {
    this.entityID = entityID;
    this.modelClass = entityModelClass;
  }

  /** {@inheritDoc} */
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  public final EntityModelProvider setModelClass(final Class<? extends EntityModel> modelClass) {
    this.modelClass = modelClass;
    return this;
  }

  /** {@inheritDoc} */
  public final EntityModelProvider setEditModelClass(final Class<? extends EntityEditModel> editModelClass) {
    this.editModelClass = editModelClass;
    return this;
  }

  /** {@inheritDoc} */
  public final EntityModelProvider setTableModelClass(final Class<? extends EntityTableModel> tableModelClass) {
    this.tableModelClass = tableModelClass;
    return this;
  }

  /** {@inheritDoc} */
  public final Class<? extends EntityModel> getModelClass() {
    return modelClass;
  }

  /** {@inheritDoc} */
  public final Class<? extends EntityEditModel> getEditModelClass() {
    return editModelClass;
  }

  /** {@inheritDoc} */
  public final Class<? extends EntityTableModel> getTableModelClass() {
    return tableModelClass;
  }

  /** {@inheritDoc} */
  public final EntityModel initializeModel(final EntityConnectionProvider connectionProvider, final boolean detailModel) {
    try {
      final EntityModel model;
      if (modelClass.equals(DefaultEntityModel.class)) {
        LOG.debug(toString() + " initializing a default entity model");
        model = initializeDefaultModel(connectionProvider, detailModel);
      }
      else {
        LOG.debug(toString() + " initializing a custom entity model: " + modelClass);
        model = modelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      configureModel(model);

      return model;
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  public final EntityEditModel initializeEditModel(final EntityConnectionProvider connectionProvider) {
    try {
      final EntityEditModel editModel;
      if (editModelClass.equals(DefaultEntityEditModel.class)) {
        LOG.debug(toString() + " initializing a default model");
        editModel = initializeDefaultEditModel(connectionProvider);
      }
      else {
        LOG.debug(toString() + " initializing a custom edit model: " + editModelClass);
        editModel = editModelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      configureEditModel(editModel);

      return editModel;
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  public final EntityTableModel initializeTableModel(final EntityConnectionProvider connectionProvider, final boolean detailModel) {
    try {
      final EntityTableModel tableModel;
      if (tableModelClass.equals(DefaultEntityTableModel.class)) {
        LOG.debug(toString() + " initializing a default table model");
        tableModel = initializeDefaultTableModel(connectionProvider);
      }
      else {
        LOG.debug(toString() + " initializing a custom table model: " + tableModelClass);
        tableModel = tableModelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      if (detailModel) {
        tableModel.setDetailModel(true);
      }
      configureTableModel(tableModel);

      return tableModel;
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Override to configure the provided EntityModel instance.
   * Called after each initialization.
   * @param entityModel the entity model to configure
   */
  protected void configureModel(final EntityModel entityModel) {}

  /**
   * Override to configure the provided EntityEditModel instance.
   * Called after each initialization.
   * @param editModel the edit model to configure
   */
  protected void configureEditModel(final EntityEditModel editModel) {}

  /**
   * Override to configure the provided EntityTableModel instance.
   * Called after each initialization.
   * @param tableModel the edit model to configure
   */
  protected void configureTableModel(final EntityTableModel tableModel) {}

  private EntityModel initializeDefaultModel(final EntityConnectionProvider connectionProvider, final boolean detailModel) {
    final EntityTableModel tableModel = initializeTableModel(connectionProvider, detailModel);
    if (!tableModel.hasEditModel()) {
      final EntityEditModel editModel = initializeEditModel(connectionProvider);
      tableModel.setEditModel(editModel);
    }

    return new DefaultEntityModel(tableModel.getEditModel(), tableModel);
  }

  private EntityEditModel initializeDefaultEditModel(final EntityConnectionProvider connectionProvider) {
    return new DefaultEntityEditModel(entityID, connectionProvider);
  }

  private EntityTableModel initializeDefaultTableModel(final EntityConnectionProvider connectionProvider) {
    return new DefaultEntityTableModel(entityID, connectionProvider);
  }
}
