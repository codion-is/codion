/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.DefaultEntityModelProvider;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityModelProvider;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.provider.EntityConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A class providing EntityPanel instances.
 * Note: this class has a natural ordering based on the caption which is inconsistent with equals.
 */
public class EntityPanelProvider implements Comparable {

  protected static final Logger LOG = LoggerFactory.getLogger(EntityPanelProvider.class);

  private final Comparator<String> comparator = Util.getSpaceAwareCollator();

  private final String caption;
  private boolean refreshOnInit = true;
  private int detailPanelState = EntityPanel.EMBEDDED;
  private double detailSplitPanelResizeWeight = 0.5;
  private boolean tableSearchPanelVisible = Configuration.getBooleanValue(Configuration.DEFAULT_SEARCH_PANEL_STATE);

  private Class<? extends EntityPanel> panelClass = EntityPanel.class;
  private Class<? extends EntityTablePanel> tablePanelClass = EntityTablePanel.class;
  private Class<? extends EntityEditPanel> editPanelClass;

  private final EntityModelProvider modelProvider;

  private final List<EntityPanelProvider> detailPanelProviders = new ArrayList<EntityPanelProvider>();

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
    this.caption = caption;
    this.panelClass = entityPanelClass;
    this.modelProvider = new DefaultEntityModelProvider(entityID, entityModelClass);
  }

  public EntityPanelProvider (final EntityModelProvider modelProvider) {
    this(modelProvider, null);
  }

  public EntityPanelProvider (final EntityModelProvider modelProvider, final String caption) {
    Util.rejectNullValue(modelProvider, "modelProvider");
    this.modelProvider = modelProvider;
    this.caption = caption;
  }

  /**
   * @return the entity ID
   */
  public final String getEntityID() {
    return modelProvider.getEntityID();
  }

  public final EntityModelProvider getModelProvider() {
    return modelProvider;
  }

  /**
   * @return the caption to use when this EntityPanelProvider is shown in f.x. menus
   */
  public final String getCaption() {
    return caption;
  }

  public final EntityPanelProvider addDetailPanelProvider(final EntityPanelProvider panelProvider) {
    if (!detailPanelProviders.contains(panelProvider)) {
      detailPanelProviders.add(panelProvider);
      if (!modelProvider.containsDetailModelProvider(panelProvider.getModelProvider())) {
        modelProvider.addDetailModelProvider(panelProvider.getModelProvider());//todo not very clean
      }
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

  public final int compareTo(final Object o) {
    final String thisCompare = caption == null ? modelProvider.getModelClass().getSimpleName() : caption;
    final String thatCompare = ((EntityPanelProvider) o).caption == null
            ? ((EntityPanelProvider) o).panelClass.getSimpleName() : ((EntityPanelProvider) o).caption;

    return comparator.compare(thisCompare, thatCompare);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof EntityPanelProvider && ((EntityPanelProvider) obj).modelProvider.getEntityID().equals(modelProvider.getEntityID());
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    return modelProvider.getEntityID().hashCode();
  }

  public final EntityPanel createPanel(final EntityConnectionProvider connectionProvider) {
    return createPanel(connectionProvider, false);
  }

  public final EntityPanel createPanel(final EntityConnectionProvider connectionProvider, final boolean detailPanel) {
    Util.rejectNullValue(connectionProvider, "connectionProvider");
    try {
      final EntityModel entityModel = modelProvider.createModel(connectionProvider, detailPanel);

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
      throw new IllegalArgumentException("Can not create EntityPanel without an EntityModel");
    }
    try {
      final EntityPanel entityPanel = initializePanel(model);
      if (entityPanel.getTablePanel() != null && tableSearchPanelVisible) {
        entityPanel.getTablePanel().setSearchPanelVisible(tableSearchPanelVisible);
      }
      if (!detailPanelProviders.isEmpty()) {
        entityPanel.setDetailPanelState(detailPanelState);
        entityPanel.setDetailSplitPanelResizeWeight(detailSplitPanelResizeWeight);
        for (final EntityPanelProvider detailPanelProvider : detailPanelProviders) {
          final EntityModel detailModel = model.getDetailModel(detailPanelProvider.getEntityID());
          final EntityPanel detailPanel = detailPanelProvider.createPanel(detailModel);
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
    return initializeEditPanel(modelProvider.createEditModel(connectionProvider));
  }

  public final EntityTablePanel createTablePanel(final EntityConnectionProvider connectionProvider, final boolean detailPanel) {
    return initializeTablePanel(modelProvider.createTableModel(connectionProvider, detailPanel));
  }

  protected void configurePanel(final EntityPanel entityPanel) {}

  protected void configureEditPanel(final EntityEditPanel editPanel) {}

  protected void configureTablePanel(final EntityTablePanel tablePanel) {}

  private EntityPanel initializePanel(final EntityModel entityModel) {
    try {
      final EntityPanel entityPanel;
      if (panelClass.equals(EntityPanel.class)) {
        final EntityTablePanel tablePanel;
        if (entityModel.containsTableModel()) {
          tablePanel = initializeTablePanel(entityModel.getTableModel());
        }
        else {
          tablePanel = null;
        }
        final EntityEditPanel editPanel = editPanelClass == null ? null : initializeEditPanel(entityModel.getEditModel());
        entityPanel = panelClass.getConstructor(EntityModel.class, EntityEditPanel.class, EntityTablePanel.class)
                .newInstance(entityModel, editPanel, tablePanel);
      }
      else {
        entityPanel = panelClass.getConstructor(EntityModel.class).newInstance(entityModel);
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
      throw new IllegalArgumentException("No edit panel class has been specified for entity panel provider: " + getEntityID());
    }
    if (!editModel.getEntityID().equals(getEntityID())) {
      throw new IllegalArgumentException("Entity ID mismatch, editModel: " + editModel.getEntityID() + ", required: " + getEntityID());
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
      if (!tableModel.getEntityID().equals(getEntityID())) {
        throw new IllegalArgumentException("Entity ID mismatch, tableModel: " + tableModel.getEntityID() + ", required: " + getEntityID());
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
}
