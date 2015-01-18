/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
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
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A class providing EntityPanel instances.
 * Note: this class has a natural ordering based on the caption which is inconsistent with equals.
 */
public class EntityPanelProvider implements Comparable {

  private static final double DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT = 0.5;

  private final Comparator<String> comparator = Util.getSpaceAwareCollator();

  private final String caption;
  private boolean refreshOnInit = true;
  private EntityPanel.PanelState detailPanelState = EntityPanel.PanelState.EMBEDDED;
  private double detailSplitPanelResizeWeight = DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT;
  private boolean tableSearchPanelVisible = Configuration.getBooleanValue(Configuration.SEARCH_PANEL_STATE);

  private Class<? extends EntityPanel> panelClass = EntityPanel.class;
  private Class<? extends EntityTablePanel> tablePanelClass = EntityTablePanel.class;
  private Class<? extends EntityEditPanel> editPanelClass;

  private final EntityModelProvider modelProvider;

  private final List<EntityPanelProvider> detailPanelProviders = new ArrayList<>();

  /**
   * Instantiates a new EntityPanelProvider for the given entity type
   * @param entityID the entity ID
   */
  public EntityPanelProvider(final String entityID) {
    this(entityID, Entities.getCaption(entityID));
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
    this(entityID, Entities.getCaption(entityID), entityModelClass, entityPanelClass);
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

  /**
   * Instantiates a new EntityPanelProvider
   * @param modelProvider the EntityModelProvider to base this panel provider on
   */
  public EntityPanelProvider (final EntityModelProvider modelProvider) {
    this(modelProvider, Entities.getCaption(modelProvider.getEntityID()));
  }

  /**
   * Instantiates a new EntityPanelProvider
   * @param modelProvider the EntityModelProvider to base this panel provider on
   * @param caption the panel caption to use
   */
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

  /**
   * @return the EntityModelProvider this panel provider is based on
   */
  public final EntityModelProvider getModelProvider() {
    return modelProvider;
  }

  /**
   * @return the caption to use when this EntityPanelProvider is shown in f.x. menus
   */
  public final String getCaption() {
    return caption;
  }

  /**
   * Adds the given panel provider as a detail panel provider for this panel provider instance
   * @param panelProvider the detail panel provider
   * @return this EntityPanelProvider instance
   */
  public final EntityPanelProvider addDetailPanelProvider(final EntityPanelProvider panelProvider) {
    if (!detailPanelProviders.contains(panelProvider)) {
      detailPanelProviders.add(panelProvider);
      if (!modelProvider.containsDetailModelProvider(panelProvider.getModelProvider())) {
        modelProvider.addDetailModelProvider(panelProvider.getModelProvider());//todo not very clean
      }
    }

    return this;
  }

  /**
   * @return an unmodifiable view of the detail panel providers
   */
  public final List<EntityPanelProvider> getDetailPanelProviders() {
    return Collections.unmodifiableList(detailPanelProviders);
  }

  /**
   * @return true if the data model this panel is based on should be refreshed when the panel is initialized
   */
  public final boolean isRefreshOnInit() {
    return refreshOnInit;
  }

  /**
   * @param refreshOnInit if true then the data model this panel is based on will be refreshed when
   * the panel is initialized
   * @return this EntityPanelProvider instance
   */
  public final EntityPanelProvider setRefreshOnInit(final boolean refreshOnInit) {
    this.refreshOnInit = refreshOnInit;
    return this;
  }

  /**
   * @return whether or not the table search panel is made visible when the panel is initialized
   */
  public final boolean isTableSearchPanelVisible() {
    return tableSearchPanelVisible;
  }

  /**
   * @param tableSearchPanelVisible if true then the table search panel is made visible when the panel is initialized
   * @return this EntityPanelProvider instance
   */
  public final EntityPanelProvider setTableSearchPanelVisible(final boolean tableSearchPanelVisible) {
    this.tableSearchPanelVisible = tableSearchPanelVisible;
    return this;
  }

  /**
   * @return the state of the detail panels when this panel is initialized
   */
  public final EntityPanel.PanelState getDetailPanelState() {
    return detailPanelState;
  }

  /**
   * @param detailPanelState the state of the detail panels when this panel is initialized
   * @return this EntityPanelProvider instance
   */
  public final EntityPanelProvider setDetailPanelState(final EntityPanel.PanelState detailPanelState) {
    this.detailPanelState = detailPanelState;
    return this;
  }

  /**
   * @return the split panel resize weight to use when initializing this panel
   * with its detail panels
   */
  public final double getDetailSplitPanelResizeWeight() {
    return detailSplitPanelResizeWeight;
  }

  /**
   * @param detailSplitPanelResizeWeight the split panel resize weight to use when initializing this panel
   * with its detail panels
   * @return this EntityPanelProvider instance
   */
  public final EntityPanelProvider setDetailSplitPanelResizeWeight(final double detailSplitPanelResizeWeight) {
    this.detailSplitPanelResizeWeight = detailSplitPanelResizeWeight;
    return this;
  }

  /**
   * Note that setting the EntityPanel class overrides any table panel or edit panel classes that have been set.
   * @param panelClass the EntityPanel class to use when providing this panel
   * @return this EntityPanelProvider instance
   */
  public final EntityPanelProvider setPanelClass(final Class<? extends EntityPanel> panelClass) {
    this.panelClass = panelClass;
    return this;
  }

  /**
   * @param editPanelClass the EntityEditPanel class to use when providing this panel
   * @return this EntityPanelProvider instance
   */
  public final EntityPanelProvider setEditPanelClass(final Class<? extends EntityEditPanel> editPanelClass) {
    this.editPanelClass = editPanelClass;
    return this;
  }

  /**
   * @param tablePanelClass the EntityTablePanel class to use when providing this panel
   * @return this EntityPanelProvider instance
   */
  public final EntityPanelProvider setTablePanelClass(final Class<? extends EntityTablePanel> tablePanelClass) {
    this.tablePanelClass = tablePanelClass;
    return this;
  }

  /**
   * @return the EntityPanel class to use
   */
  public final Class<? extends EntityPanel> getPanelClass() {
    return panelClass;
  }

  /**
   * @return the EntityEditPanel class to use
   */
  public final Class<? extends EntityEditPanel> getEditPanelClass() {
    return editPanelClass;
  }

  /**
   * @return the EntityTablePanel class to use
   */
  public final Class<? extends EntityTablePanel> getTablePanelClass() {
    return tablePanelClass;
  }

  /** {@inheritDoc} */
  @Override
  public final int compareTo(final Object o) {
    final String thisCompare = caption == null ? modelProvider.getModelClass().getSimpleName() : caption;
    final String thatCompare = ((EntityPanelProvider) o).caption == null
            ? ((EntityPanelProvider) o).panelClass.getSimpleName() : ((EntityPanelProvider) o).caption;

    return comparator.compare(thisCompare, thatCompare);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof EntityPanelProvider &&
            ((EntityPanelProvider) obj).modelProvider.getEntityID().equals(modelProvider.getEntityID()) &&
            ((EntityPanelProvider) obj).modelProvider.getModelClass().equals(modelProvider.getModelClass());
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    return modelProvider.getEntityID().hashCode() + modelProvider.getModelClass().hashCode();
  }

  /**
   * Creates an EntityPanel based on this provider configuration
   * @param connectionProvider the connection provider
   * @return an EntityPanel based on this provider configuration
   */
  public final EntityPanel createPanel(final EntityConnectionProvider connectionProvider) {
    return createPanel(connectionProvider, false);
  }

  /**
   * Creates an EntityPanel based on this provider configuration
   * @param connectionProvider the connection provider
   * @param detailPanel if true then this panel is a detail panel
   * @return an EntityPanel based on this provider configuration
   */
  public final EntityPanel createPanel(final EntityConnectionProvider connectionProvider, final boolean detailPanel) {
    Util.rejectNullValue(connectionProvider, "connectionProvider");
    try {
      final EntityModel entityModel = modelProvider.createModel(connectionProvider, detailPanel);

      return createPanel(entityModel);
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates an EntityPanel based on this provider configuration
   * @param model the EntityModel to base this panel on
   * @return an EntityPanel based on this provider configuration
   */
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
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates an EntityEditPanel
   * @param connectionProvider the connection provider
   * @return an EntityEditPanel based on this provider
   */
  public final EntityEditPanel createEditPanel(final EntityConnectionProvider connectionProvider) {
    return initializeEditPanel(modelProvider.createEditModel(connectionProvider));
  }

  /**
   * Creates an EntityTablePanel
   * @param connectionProvider the connection provider
   * @return an EntityTablePanel based on this provider
   */
  public final EntityTablePanel createTablePanel(final EntityConnectionProvider connectionProvider, final boolean detailPanel) {
    return initializeTablePanel(modelProvider.createTableModel(connectionProvider, detailPanel));
  }

  /**
   * Called after the EntityPanel has been constructed, but before it is initialized, override to configure
   * @param entityPanel the EntityPanel just constructed
   */
  protected void configurePanel(final EntityPanel entityPanel) {}

  /**
   * Called after the EntityEditPanel has been constructed, but before it is initialized, override to configure
   * @param editPanel the EntityEditPanel just constructed
   */
  protected void configureEditPanel(final EntityEditPanel editPanel) {}

  /**
   * Called after the EntityTablePanel has been constructed, but before it is initialized, override to configure
   * @param tablePanel the EntityTablePanel just constructed
   */
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
        entityPanel = panelClass.getConstructor(EntityModel.class, String.class, EntityEditPanel.class, EntityTablePanel.class)
                .newInstance(entityModel, caption, editPanel, tablePanel);
      }
      else {
        entityPanel = panelClass.getConstructor(EntityModel.class).newInstance(entityModel);
      }

      return entityPanel;
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
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
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
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
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
