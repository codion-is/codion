/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Util;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.EntityRepository;

import java.lang.reflect.InvocationTargetException;
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

  private Class<? extends EntityModel> modelClass;
  private Class<? extends EntityEditModel> editModelClass;
  private Class<? extends EntityTableModel> tableModelClass;
  private Class<? extends EntityPanel> panelClass;
  private Class<? extends EntityEditPanel> editPanelClass;
  private Class<? extends EntityTablePanel> tablePanelClass;

  private List<EntityPanelProvider> detailPanelProviders = new ArrayList<EntityPanelProvider>();

  private static final Map<String, EntityPanelProvider> panelProviders = Collections.synchronizedMap(new HashMap<String, EntityPanelProvider>());

  private EntityPanel instance;

  public EntityPanelProvider(final String entityID) {
    this(entityID, null);
  }

  public EntityPanelProvider(final String entityID, final String caption) {
    this(entityID, caption, null, null);
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

  public EntityPanel createInstance(final EntityModel model) {
    if (model == null) {
      throw new RuntimeException("Can not create a EntityPanel without an EntityModel");
    }
    try {
      final EntityEditPanel editPanel;
      if (editPanelClass != null) {
         editPanel = editPanelClass.getConstructor(EntityEditModel.class).newInstance(model.getEditModel());
      }
      else {
        editPanel = null;
      }
      final EntityTablePanel tablePanel;
      if (model.containsTableModel() && tablePanelClass != null) {
        tablePanel = tablePanelClass.getConstructor(EntityEditModel.class).newInstance(model.getTableModel());
      }
      else {
        tablePanel = null;
      }
      final EntityPanel entityPanel;
      if (panelClass != null) {
        entityPanel = panelClass.getConstructor(EntityModel.class).newInstance(model);
      }
      else {
        entityPanel = new EntityPanel(model, getCaption());
      }
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
          entityPanel.addDetailPanel(detailProvider.createInstance(model.getDetailModel(detailProvider.entityID)));
        }
      }
      if (refreshOnInit) {
        model.refresh();
      }

      return entityPanel;
    }
    catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof RuntimeException) {
        throw (RuntimeException) ite.getCause();
      }

      throw new RuntimeException(ite.getCause());
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public EntityPanel createInstance(final EntityDbProvider dbProvider) {
    try {
      final EntityModel entityModel;
      if (entityID == null) {
        entityModel = modelClass.getConstructor(EntityDbProvider.class).newInstance(dbProvider);
      }
      else {
        entityModel = new DefaultEntityModel(entityID, dbProvider);
      }
      if (refreshOnInit) {
        entityModel.refresh();
      }

      return createInstance(entityModel);
    }
    catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof RuntimeException) {
        throw (RuntimeException) ite.getCause();
      }

      throw new RuntimeException(ite.getCause());
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
}
