/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.provider.EntityConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A default {@link EntityModelProvider} implementation.
 */
public class DefaultEntityModelProvider implements EntityModelProvider {

  protected static final Logger LOG = LoggerFactory.getLogger(DefaultEntityModelProvider.class);

  private final String entityID;

  private final List<EntityModelProvider> detailModelProviders = new ArrayList<EntityModelProvider>();

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
  @Override
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityModelProvider setModelClass(final Class<? extends EntityModel> modelClass) {
    this.modelClass = modelClass;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityModelProvider setEditModelClass(final Class<? extends EntityEditModel> editModelClass) {
    this.editModelClass = editModelClass;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityModelProvider setTableModelClass(final Class<? extends EntityTableModel> tableModelClass) {
    this.tableModelClass = tableModelClass;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Class<? extends EntityModel> getModelClass() {
    return modelClass;
  }

  /** {@inheritDoc} */
  @Override
  public final Class<? extends EntityEditModel> getEditModelClass() {
    return editModelClass;
  }

  /** {@inheritDoc} */
  @Override
  public final Class<? extends EntityTableModel> getTableModelClass() {
    return tableModelClass;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityModelProvider addDetailModelProvider(final EntityModelProvider detailModelProvider) {
    if (!detailModelProviders.contains(detailModelProvider)) {
      detailModelProviders.add(detailModelProvider);
    }

    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsDetailModelProvider(final EntityModelProvider detailModelProvider) {
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
  public final EntityModel createModel(final EntityConnectionProvider connectionProvider, final boolean detailModel) {
    try {
      final EntityModel model;
      if (modelClass.equals(DefaultEntityModel.class)) {
        LOG.debug("{} initializing a default entity model", this);
        model = initializeDefaultModel(connectionProvider, detailModel);
      }
      else {
        LOG.debug("{} initializing a custom entity model: {}", this, modelClass);
        model = modelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      for (final EntityModelProvider detailProvider : detailModelProviders) {
        model.addDetailModel(detailProvider.createModel(connectionProvider, true));
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
  @Override
  public final EntityEditModel createEditModel(final EntityConnectionProvider connectionProvider) {
    try {
      final EntityEditModel editModel;
      if (editModelClass.equals(DefaultEntityEditModel.class)) {
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
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel createTableModel(final EntityConnectionProvider connectionProvider, final boolean detailModel) {
    try {
      final EntityTableModel tableModel;
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
    final EntityTableModel tableModel = createTableModel(connectionProvider, detailModel);
    if (!tableModel.hasEditModel()) {
      final EntityEditModel editModel = createEditModel(connectionProvider);
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
