/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Util;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.DefaultEntityTableModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.EntityRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class providing EntityPanel instances.
 */
public class EntityPanelProvider implements Comparable {

  private final String entityID;
  private String caption;
  private boolean refreshOnInit = true;
  private int detailPanelState = EntityPanel.EMBEDDED;
  private double detailSplitPanelResizeWeight = 0.5;

  private Class<? extends EntityModel> modelClass = DefaultEntityModel.class;
  private Class<? extends EntityEditModel> editModelClass = DefaultEntityEditModel.class;
  private Class<? extends EntityTableModel> tableModelClass = DefaultEntityTableModel.class;
  private Class<? extends EntityPanel> panelClass = EntityPanel.class;
  private Class<? extends EntityTablePanel> tablePanelClass = EntityTablePanel.class;

  private Class<? extends EntityEditPanel> editPanelClass;

  private List<EntityPanelProvider> detailPanelProviders = new ArrayList<EntityPanelProvider>();

  private static final Map<String, EntityPanelProvider> panelProviders = Collections.synchronizedMap(new HashMap<String, EntityPanelProvider>());

  private EntityPanel instance;

  public EntityPanelProvider(final String entityID) {
    this(entityID, null);
  }

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
    Util.rejectNullValue(entityID);
    Util.rejectNullValue(entityModelClass);
    Util.rejectNullValue(entityPanelClass);
    this.entityID = entityID;
    this.caption = caption == null ? "" : caption;
    this.modelClass = entityModelClass;
    this.panelClass = entityPanelClass;
  }

  public EntityPanelProvider register() {
    synchronized (panelProviders) {
      if (panelProviders.containsKey(entityID)) {
        throw new RuntimeException("Panel provider has already been set for entity: " + entityID);
      }
      panelProviders.put(entityID, this);
    }

    return this;
  }

  public String getEntityID() {
    return entityID;
  }

  /**
   * @return the caption to use when this EntityPanelProvider is shown in f.x. menus
   */
  public String getCaption() {
    if (caption == null || caption.length() == 0) {
      this.caption = EntityRepository.getEntityDefinition(entityID).getCaption();
    }

    if (caption == null) {
      return "<no caption>";
    }

    return caption;
  }

  public EntityPanelProvider addDetailPanelProvider(final EntityPanelProvider panelProvider) {
    if (!detailPanelProviders.contains(panelProvider)) {
      detailPanelProviders.add(panelProvider);
    }

    return this;
  }

  public List<EntityPanelProvider> getDetailPanelProviders() {
    return Collections.unmodifiableList(detailPanelProviders);
  }

  public boolean isRefreshOnInit() {
    return refreshOnInit;
  }

  public EntityPanelProvider setRefreshOnInit(final boolean refreshOnInit) {
    this.refreshOnInit = refreshOnInit;
    return this;
  }

  public int getDetailPanelState() {
    return detailPanelState;
  }

  public EntityPanelProvider setDetailPanelState(final int detailPanelState) {
    this.detailPanelState = detailPanelState;
    return this;
  }

  public double getDetailSplitPanelResizeWeight() {
    return detailSplitPanelResizeWeight;
  }

  public EntityPanelProvider setDetailSplitPanelResizeWeight(final double detailSplitPanelResizeWeight) {
    this.detailSplitPanelResizeWeight = detailSplitPanelResizeWeight;
    return this;
  }

  public EntityPanelProvider setModelClass(final Class<? extends EntityModel> modelClass) {
    this.modelClass = modelClass;
    return this;
  }

  public EntityPanelProvider setPanelClass(final Class<? extends EntityPanel> panelClass) {
    this.panelClass = panelClass;
    return this;
  }

  public EntityPanelProvider setEditPanelClass(final Class<? extends EntityEditPanel> editPanelClass) {
    this.editPanelClass = editPanelClass;
    return this;
  }

  public EntityPanelProvider setTablePanelClass(final Class<? extends EntityTablePanel> tablePanelClass) {
    this.tablePanelClass = tablePanelClass;
    return this;
  }

  public EntityPanelProvider setEditModelClass(final Class<? extends EntityEditModel> editModelClass) {
    this.editModelClass = editModelClass;
    return this;
  }

  public EntityPanelProvider setTableModelClass(final Class<? extends EntityTableModel> tableModelClass) {
    this.tableModelClass = tableModelClass;
    return this;
  }

  /**
   * @return the EntityModel Class to use when instantiating an EntityPanel
   */
  public Class<? extends EntityModel> getModelClass() {
    return modelClass;
  }

  /**
   * @return the EntityPanel Class to instantiate
   */
  public Class<? extends EntityPanel> getPanelClass() {
    return panelClass;
  }

  public Class<? extends EntityEditPanel> getEditPanelClass() {
    return editPanelClass;
  }

  public Class<? extends EntityTablePanel> getTablePanelClass() {
    return tablePanelClass;
  }

  public Class<? extends EntityEditModel> getEditModelClass() {
    return editModelClass;
  }

  public Class<? extends EntityTableModel> getTableModelClass() {
    return tableModelClass;
  }

  public int compareTo(final Object o) {
    final String thisCompare = caption == null ? modelClass.getSimpleName() : caption;
    final String thatCompare = ((EntityPanelProvider) o).caption == null
            ? ((EntityPanelProvider) o).panelClass.getSimpleName() : ((EntityPanelProvider) o).caption;

    return thisCompare.compareTo(thatCompare);
  }

  public EntityPanel createInstance(final EntityDbProvider dbProvider) {
    try {
      final EntityModel entityModel = initializeModel(this, dbProvider);
      if (refreshOnInit) {
        entityModel.refresh();
      }

      return createInstance(entityModel);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void setInstance(final EntityPanel instance) {
    if (this.instance != null) {
      throw new RuntimeException("EntityPanel instance has already been set for this provider: " + this.instance);
    }
    this.instance = instance;
  }

  public EntityPanel getInstance() {
    return instance;
  }

  public static EntityPanelProvider getProvider(final String entityID) {
    return panelProviders.get(entityID);
  }

  protected void configurePanel(final EntityPanel panel) {}

  protected void configureEditPanel(final EntityEditPanel editPanel) {}

  protected void configureTablePanel(final EntityTablePanel tablePanel) {}

  protected void configureModel(final EntityModel model) {}

  protected void configureEditModel(final EntityEditModel editModel) {}

  protected void configureTableModel(final EntityTableModel tableModel) {}

  private EntityPanel createInstance(final EntityModel model) {
    if (model == null) {
      throw new RuntimeException("Can not create a EntityPanel without an EntityModel");
    }
    try {
      final EntityEditPanel editPanel;
      if (editPanelClass != null) {
        editPanel = initializeEditPanel(model.getEditModel());
      }
      else {
        editPanel = null;
      }
      final EntityTablePanel tablePanel;
      if (model.containsTableModel()) {
        tablePanel = initializeTablePanel(model.getTableModel());
      }
      else {
        tablePanel = null;
      }
      final EntityPanel entityPanel= initializePanel(model);
      if (editPanel != null) {
        entityPanel.setEditPanel(editPanel);
      }
      if (tablePanel != null) {
        entityPanel.setTablePanel(tablePanel);
      }
      if (detailPanelProviders.size() > 0) {
        entityPanel.setDetailPanelState(detailPanelState);
        entityPanel.setDetailSplitPanelResizeWeight(detailSplitPanelResizeWeight);
        for (final EntityPanelProvider detailProvider : detailPanelProviders) {
          final EntityPanel detailPanel = detailProvider.createInstance(model.getDbProvider());
          model.addDetailModel(detailPanel.getModel());
          entityPanel.addDetailPanel(detailPanel);
        }
      }
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

  private EntityPanel initializePanel(final EntityModel model) {
    try {
      final EntityPanel panel = panelClass.getConstructor(EntityModel.class).newInstance(model);
      configurePanel(panel);
      if (editPanelClass != null) {
        panel.setEditPanel(initializeEditPanel(model.getEditModel()));
      }
      if (model.containsTableModel()) {
        panel.setTablePanel(initializeTablePanel(model.getTableModel()));
      }

      return panel;
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
    return new DefaultEntityEditModel(entityID, dbProvider);
  }

  private EntityTableModel initializeDefaultTableModel(final EntityDbProvider dbProvider) {
    return new DefaultEntityTableModel(entityID, dbProvider);
  }
}
