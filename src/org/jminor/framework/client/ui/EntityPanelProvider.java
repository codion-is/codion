/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Util;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.DefaultEntityTableModel;
import org.jminor.framework.client.model.DefaultEntityValidator;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.EntityValidator;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entities;

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

  private final String entityID;
  private String caption;
  private boolean refreshOnInit = true;
  private int detailPanelState = EntityPanel.EMBEDDED;
  private double detailSplitPanelResizeWeight = 0.5;

  private Class<? extends EntityModel> modelClass = DefaultEntityModel.class;
  private Class<? extends EntityEditModel> editModelClass = DefaultEntityEditModel.class;
  private Class<? extends EntityValidator> validatorClass = DefaultEntityValidator.class;
  private Class<? extends EntityTableModel> tableModelClass = DefaultEntityTableModel.class;
  private Class<? extends EntityPanel> panelClass = EntityPanel.class;
  private Class<? extends EntityTablePanel> tablePanelClass = EntityTablePanel.class;
  private Class<? extends EntityEditPanel> editPanelClass;

  private final List<EntityPanelProvider> detailPanelProviders = new ArrayList<EntityPanelProvider>();

  private static final Map<String, EntityPanelProvider> PANEL_PROVIDERS = Collections.synchronizedMap(new HashMap<String, EntityPanelProvider>());

  private EntityPanel instance;

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

    return thisCompare.compareTo(thatCompare);
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

  public final EntityPanel createInstance(final EntityDbProvider dbProvider) {
    return createInstance(dbProvider, false);
  }

  public final EntityPanel createInstance(final EntityDbProvider dbProvider, final boolean detailPanel) {
    Util.rejectNullValue(dbProvider, "dbProvider");
    try {
      final EntityModel entityModel = initializeModel(this, dbProvider);
      if (detailPanel && entityModel.containsTableModel()) {
        entityModel.getTableModel().setDetailModel(true);
      }

      return createInstance(entityModel, detailPanel);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public final void setInstance(final EntityPanel instance) {
    if (this.instance != null) {
      throw new IllegalStateException("EntityPanel instance has already been set for this provider: " + this.instance);
    }
    this.instance = instance;
  }

  public final EntityPanel getInstance() {
    return instance;
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

  private EntityPanel createInstance(final EntityModel model, final boolean isDetailPanel) {
    if (model == null) {
      throw new IllegalArgumentException("Can not create a EntityPanel without an EntityModel");
    }
    try {
      final EntityPanel entityPanel = initializePanel(model);
      if (!detailPanelProviders.isEmpty()) {
        entityPanel.setDetailPanelState(detailPanelState);
        entityPanel.setDetailSplitPanelResizeWeight(detailSplitPanelResizeWeight);
        for (final EntityPanelProvider detailProvider : detailPanelProviders) {
          final EntityPanel detailPanel = detailProvider.createInstance(model.getDbProvider(), true);
          model.addDetailModel(detailPanel.getModel());
          entityPanel.addDetailPanel(detailPanel);
        }
      }
      if (!isDetailPanel && refreshOnInit) {
        final boolean cascadeRefresh = model.isCascadeRefresh();
        try {
          model.setCascadeRefresh(true);
          model.refresh();
        }
        finally {
          model.setCascadeRefresh(cascadeRefresh);
        }
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

      configurePanel(entityPanel);

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

  private static EntityModel initializeModel(final EntityPanelProvider panelProvider, final EntityDbProvider dbProvider) {
    try {
      final EntityModel model;
      if (panelProvider.modelClass.equals(DefaultEntityModel.class)) {
        model = panelProvider.initializeDefaultModel(dbProvider);
      }
      else {
        model = panelProvider.modelClass.getConstructor(EntityDbProvider.class).newInstance(dbProvider);
      }
      panelProvider.configureModel(model);

      return model;
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private EntityEditModel initializeEditModel(final EntityDbProvider dbProvider) {
    try {
      final EntityEditModel editModel;
      if (editModelClass.equals(DefaultEntityEditModel.class)) {
        editModel = initializeDefaultEditModel(dbProvider);
      }
      else {
        editModel = editModelClass.getConstructor(EntityDbProvider.class).newInstance(dbProvider);
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

  private EntityValidator initializeValidator(final EntityDbProvider dbProvider) {
    try {
      final EntityValidator validator;
      if (validatorClass.equals(DefaultEntityValidator.class)) {
        validator = initializeDefaultValidator(dbProvider);
      }
      else {
        validator = validatorClass.getConstructor(EntityDbProvider.class).newInstance(dbProvider);
      }

      return validator;
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private EntityTableModel initializeTableModel(final EntityDbProvider dbProvider) {
    try {
      final EntityTableModel tableModel;
      if (tableModelClass.equals(DefaultEntityTableModel.class)) {
        tableModel = initializeDefaultTableModel(dbProvider);
      }
      else {
        tableModel = tableModelClass.getConstructor(EntityDbProvider.class).newInstance(dbProvider);
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

  private EntityModel initializeDefaultModel(final EntityDbProvider dbProvider) {
    final EntityEditModel editModel = initializeEditModel(dbProvider);
    final EntityTableModel tableModel = initializeTableModel(dbProvider);

    return new DefaultEntityModel(editModel, tableModel);
  }

  private EntityEditModel initializeDefaultEditModel(final EntityDbProvider dbProvider) {
    final EntityValidator validator = initializeValidator(dbProvider);
    return new DefaultEntityEditModel(entityID, dbProvider, validator);
  }

  private EntityTableModel initializeDefaultTableModel(final EntityDbProvider dbProvider) {
    return new DefaultEntityTableModel(entityID, dbProvider);
  }

  private EntityValidator initializeDefaultValidator(final EntityDbProvider dbProvider) {
    return new DefaultEntityValidator(entityID, dbProvider);
  }
}
