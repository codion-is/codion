/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.DefaultEntityTableModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class providing EntityPanel instances.
 * Note: this class has a natural ordering based on the caption which is inconsistent with equals.
 */
public class EntityPanelProvider implements Comparable {

  protected static final Logger LOG = LoggerFactory.getLogger(EntityPanelProvider.class);

  private final Collator collator = Collator.getInstance();

  private final String entityID;
  private String caption;
  private boolean refreshOnInit = true;
  private int detailPanelState = EntityPanel.EMBEDDED;
  private double detailSplitPanelResizeWeight = 0.5;
  private boolean tableSearchPanelVisible = Configuration.getBooleanValue(Configuration.DEFAULT_SEARCH_PANEL_STATE);

  private Class<? extends EntityModel> modelClass = DefaultEntityModel.class;
  private Class<? extends EntityEditModel> editModelClass = DefaultEntityEditModel.class;
  private Class<? extends EntityTableModel> tableModelClass = DefaultEntityTableModel.class;
  private Class<? extends EntityPanel> panelClass = EntityPanel.class;
  private Class<? extends EntityTablePanel> tablePanelClass = EntityTablePanel.class;
  private Class<? extends EntityEditPanel> editPanelClass;

  private final List<EntityPanelProvider> detailPanelProviders = new ArrayList<EntityPanelProvider>();

  private static final Map<String, EntityPanelProvider> PANEL_PROVIDERS = Collections.synchronizedMap(new HashMap<String, EntityPanelProvider>());

  /**
   * Instantiates a new EntityPanelProvider for the given entity type
   * @param entityID the entity ID
   */
  public EntityPanelProvider(final String entityID) {
    this(entityID, null);
  }

  /**
   * Instantiates a new EntityPanelProvider for the given entity type
   * @param entityID the entity ID
   * @param caption the panel caption
   */
  public EntityPanelProvider(final String entityID, final String caption) {
    this(entityID, caption, DefaultEntityModel.class, EntityPanel.class);
  }

  /**
   * Instantiates a new EntityPanelProvider
   * @param entityID the entity ID
   * @param entityModelClass the Class of the EntityModel
   * @param entityPanelClass the Class of the EntityPanel
   */
  public EntityPanelProvider(final String entityID, final Class<? extends EntityModel> entityModelClass,
                             final Class<? extends EntityPanel> entityPanelClass) {
    this(entityID, null, entityModelClass, entityPanelClass);
  }

  /**
   * Instantiates a new EntityPanelProvider
   * @param entityID the entityID
   * @param caption the caption to use when this EntityPanelProvider is shown in f.x. menus
   * @param entityModelClass the Class of the EntityModel
   * @param entityPanelClass the Class of the EntityPanel
   */
  public EntityPanelProvider(final String entityID, final String caption, final Class<? extends EntityModel> entityModelClass,
                             final Class<? extends EntityPanel> entityPanelClass) {
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(entityModelClass, "entityModelClass");
    Util.rejectNullValue(entityPanelClass, "entityPanelClass");
    this.entityID = entityID;
    this.caption = caption == null ? "" : caption;
    this.modelClass = entityModelClass;
    this.panelClass = entityPanelClass;
  }

  public final EntityPanelProvider register() {
    synchronized (PANEL_PROVIDERS) {
      if (PANEL_PROVIDERS.containsKey(entityID)) {
        throw new IllegalStateException("Panel provider has already been set for entity: " + entityID);
      }
      PANEL_PROVIDERS.put(entityID, this);
    }

    return this;
  }

  /**
   * @return the entity ID
   */
  public final String getEntityID() {
    return entityID;
  }

  /**
   * @return the caption to use when this EntityPanelProvider is shown in f.x. menus
   */
  public final String getCaption() {
    if (Util.nullOrEmpty(caption)) {
      this.caption = Entities.getCaption(entityID);
    }

    if (caption == null) {
      return "<no caption>";
    }

    return caption;
  }

  public final EntityPanelProvider addDetailPanelProvider(final EntityPanelProvider panelProvider) {
    if (!detailPanelProviders.contains(panelProvider)) {
      detailPanelProviders.add(panelProvider);
    }

    return this;
  }

  public final List<EntityPanelProvider> getDetailPanelProviders() {
    return Collections.unmodifiableList(detailPanelProviders);
  }

  public final boolean isRefreshOnInit() {
    return refreshOnInit;
  }

  public final EntityPanelProvider setRefreshOnInit(final boolean refreshOnInit) {
    this.refreshOnInit = refreshOnInit;
    return this;
  }

  public final boolean isTableSearchPanelVisible() {
    return tableSearchPanelVisible;
  }

  public final EntityPanelProvider setTableSearchPanelVisible(final boolean tableSearchPanelVisible) {
    this.tableSearchPanelVisible = tableSearchPanelVisible;
    return this;
  }

  public final int getDetailPanelState() {
    return detailPanelState;
  }

  public final EntityPanelProvider setDetailPanelState(final int detailPanelState) {
    this.detailPanelState = detailPanelState;
    return this;
  }

  public final double getDetailSplitPanelResizeWeight() {
    return detailSplitPanelResizeWeight;
  }

  public final EntityPanelProvider setDetailSplitPanelResizeWeight(final double detailSplitPanelResizeWeight) {
    this.detailSplitPanelResizeWeight = detailSplitPanelResizeWeight;
    return this;
  }

  public final EntityPanelProvider setModelClass(final Class<? extends EntityModel> modelClass) {
    this.modelClass = modelClass;
    return this;
  }

  public final EntityPanelProvider setPanelClass(final Class<? extends EntityPanel> panelClass) {
    this.panelClass = panelClass;
    return this;
  }

  public final EntityPanelProvider setEditPanelClass(final Class<? extends EntityEditPanel> editPanelClass) {
    this.editPanelClass = editPanelClass;
    return this;
  }

  public final EntityPanelProvider setTablePanelClass(final Class<? extends EntityTablePanel> tablePanelClass) {
    this.tablePanelClass = tablePanelClass;
    return this;
  }

  public final EntityPanelProvider setEditModelClass(final Class<? extends EntityEditModel> editModelClass) {
    this.editModelClass = editModelClass;
    return this;
  }

  public final EntityPanelProvider setTableModelClass(final Class<? extends EntityTableModel> tableModelClass) {
    this.tableModelClass = tableModelClass;
    return this;
  }

  /**
   * @return the EntityModel Class to use when instantiating an EntityPanel
   */
  public final Class<? extends EntityModel> getModelClass() {
    return modelClass;
  }

  /**
   * @return the EntityPanel Class to instantiate
   */
  public final Class<? extends EntityPanel> getPanelClass() {
    return panelClass;
  }

  public final Class<? extends EntityEditPanel> getEditPanelClass() {
    return editPanelClass;
  }

  public final Class<? extends EntityTablePanel> getTablePanelClass() {
    return tablePanelClass;
  }

  public final Class<? extends EntityEditModel> getEditModelClass() {
    return editModelClass;
  }

  public final Class<? extends EntityTableModel> getTableModelClass() {
    return tableModelClass;
  }

  public final int compareTo(final Object o) {
    final String thisCompare = caption == null ? modelClass.getSimpleName() : caption;
    final String thatCompare = ((EntityPanelProvider) o).caption == null
            ? ((EntityPanelProvider) o).panelClass.getSimpleName() : ((EntityPanelProvider) o).caption;

    return collator.compare(thisCompare, thatCompare);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof EntityPanelProvider && ((EntityPanelProvider) obj).entityID.equals(entityID);
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    return entityID.hashCode();
  }

  public final EntityPanel createPanel(final EntityConnectionProvider connectionProvider) {
    return createPanel(connectionProvider, false);
  }

  public final EntityPanel createPanel(final EntityConnectionProvider connectionProvider, final boolean detailPanel) {
    Util.rejectNullValue(connectionProvider, "connectionProvider");
    try {
      final EntityModel entityModel = initializeModel(connectionProvider);
      if (detailPanel && entityModel.containsTableModel()) {
        entityModel.getTableModel().setDetailModel(true);
      }

      return createPanel(entityModel);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public final EntityPanel createPanel(final EntityModel model) {
    if (model == null) {
      throw new IllegalArgumentException("Can not create a EntityPanel without an EntityModel");
    }
    try {
      final EntityPanel entityPanel = initializePanel(model);
      if (entityPanel.getTablePanel() != null && tableSearchPanelVisible) {
        entityPanel.getTablePanel().setSearchPanelVisible(tableSearchPanelVisible);
      }
      if (!detailPanelProviders.isEmpty()) {
        entityPanel.setDetailPanelState(detailPanelState);
        entityPanel.setDetailSplitPanelResizeWeight(detailSplitPanelResizeWeight);
        for (final EntityPanelProvider detailProvider : detailPanelProviders) {
          final EntityModel detailModel = model.getDetailModel(detailProvider.modelClass);
          final EntityPanel detailPanel;
          if (detailModel == null) {
            detailPanel = detailProvider.createPanel(model.getConnectionProvider(), true);
          }
          else {
            detailPanel = detailProvider.createPanel(detailModel);
          }
          entityPanel.addDetailPanel(detailPanel);
        }
      }
      configurePanel(entityPanel);
      if (refreshOnInit) {
        model.refresh();
      }

      return entityPanel;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public final EntityEditPanel createEditPanel(final EntityConnectionProvider connectionProvider) {
    return initializeEditPanel(initializeEditModel(connectionProvider));
  }

  public final EntityTablePanel createTablePanel(final EntityConnectionProvider connectionProvider) {
    return initializeTablePanel(initializeTableModel(connectionProvider));
  }

  public static EntityPanelProvider getProvider(final String entityID) {
    return PANEL_PROVIDERS.get(entityID);
  }

  protected void configurePanel(final EntityPanel panel) {}

  protected void configureEditPanel(final EntityEditPanel editPanel) {}

  protected void configureTablePanel(final EntityTablePanel tablePanel) {}

  protected void configureModel(final EntityModel model) {}

  protected void configureEditModel(final EntityEditModel editModel) {}

  protected void configureTableModel(final EntityTableModel tableModel) {}

  private EntityPanel initializePanel(final EntityModel model) {
    try {
      final EntityPanel entityPanel;
      if (panelClass.equals(EntityPanel.class)) {
        final EntityTablePanel tablePanel;
        if (model.containsTableModel()) {
          tablePanel = initializeTablePanel(model.getTableModel());
        }
        else {
          tablePanel = null;
        }
        final EntityEditPanel editPanel = editPanelClass == null ? null : initializeEditPanel(model.getEditModel());
        entityPanel = panelClass.getConstructor(EntityModel.class, EntityEditPanel.class, EntityTablePanel.class)
                .newInstance(model, editPanel, tablePanel);
      }
      else {
        entityPanel = panelClass.getConstructor(EntityModel.class).newInstance(model);
      }

      return entityPanel;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    if (editPanelClass == null) {
      throw new IllegalArgumentException("No edit panel class has been specified for entity panel provider: " + entityID);
    }
    if (!editModel.getEntityID().equals(entityID)) {
      throw new IllegalArgumentException("Entity ID mismatch, editModel: " + editModel.getEntityID() + ", required: " + entityID);
    }
    try {
      final EntityEditPanel editPanel = editPanelClass.getConstructor(EntityEditModel.class).newInstance(editModel);
      configureEditPanel(editPanel);

      return editPanel;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private EntityTablePanel initializeTablePanel(final EntityTableModel tableModel) {
    try {
      if (!tableModel.getEntityID().equals(entityID)) {
        throw new IllegalArgumentException("Entity ID mismatch, tableModel: " + tableModel.getEntityID() + ", required: " + entityID);
      }
      final EntityTablePanel tablePanel = tablePanelClass.getConstructor(EntityTableModel.class).newInstance(tableModel);
      configureTablePanel(tablePanel);

      return tablePanel;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private EntityModel initializeModel(final EntityConnectionProvider connectionProvider) {
    try {
      final EntityModel model;
      if (modelClass.equals(DefaultEntityModel.class)) {
        LOG.debug(toString() + " initializing a default entity model");
        model = initializeDefaultModel(connectionProvider);
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

  private EntityEditModel initializeEditModel(final EntityConnectionProvider connectionProvider) {
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

  private EntityTableModel initializeTableModel(final EntityConnectionProvider connectionProvider) {
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

  private EntityModel initializeDefaultModel(final EntityConnectionProvider connectionProvider) {
    final EntityTableModel tableModel = initializeTableModel(connectionProvider);
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
