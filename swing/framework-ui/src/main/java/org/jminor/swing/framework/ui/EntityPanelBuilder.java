/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityModelBuilder;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A class providing EntityPanel instances.
 */
public class EntityPanelBuilder {

  private static final double DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT = 0.5;

  private String caption;
  private boolean refreshOnInit = true;
  private EntityPanel.PanelState detailPanelState = EntityPanel.PanelState.EMBEDDED;
  private double detailSplitPanelResizeWeight = DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT;
  private boolean tableConditionPanelVisible = EntityTablePanel.TABLE_CONDITION_PANEL_VISIBLE.get();

  private Class<? extends EntityPanel> panelClass = EntityPanel.class;
  private Class<? extends EntityTablePanel> tablePanelClass = EntityTablePanel.class;
  private Class<? extends EntityEditPanel> editPanelClass;

  private final SwingEntityModelBuilder modelBuilder;

  private final List<EntityPanelBuilder> detailPanelBuilders = new ArrayList<>();

  /**
   * Instantiates a new EntityPanelBuilder for the given entity type
   * @param entityId the entity ID
   */
  public EntityPanelBuilder(final String entityId) {
    this(entityId, SwingEntityModel.class, EntityPanel.class);
  }

  /**
   * Instantiates a new EntityPanelBuilder
   * @param entityId the entityId
   * @param entityModelClass the Class of the EntityModel
   * @param entityPanelClass the Class of the EntityPanel
   */
  public EntityPanelBuilder(final String entityId, final Class<? extends SwingEntityModel> entityModelClass,
                            final Class<? extends EntityPanel> entityPanelClass) {
    this(new SwingEntityModelBuilder(entityId).setModelClass(entityModelClass));
    setPanelClass(entityPanelClass);
  }

  /**
   * Instantiates a new EntityPanelBuilder
   * @param modelBuilder the EntityModelBuilder to base this panel provider on
   */
  public EntityPanelBuilder(final SwingEntityModelBuilder modelBuilder) {
    this.modelBuilder = requireNonNull(modelBuilder, "modelBuilder");
  }

  /**
   * @return the entity ID
   */
  public final String getEntityId() {
    return modelBuilder.getEntityId();
  }

  /**
   * @return the EntityModelBuilder this panel provider is based on
   */
  public final SwingEntityModelBuilder getModelBuilder() {
    return modelBuilder;
  }

  /**
   * @param caption the panel caption
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder setCaption(final String caption) {
    this.caption = caption;
    return this;
  }

  /**
   * @return the caption to use when this EntityPanelBuilder is shown in f.x. menus
   */
  public final String getCaption() {
    return caption;
  }

  /**
   * Adds the given panel provider as a detail panel provider for this panel provider instance
   * @param panelBuilder the detail panel provider
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder addDetailPanelBuilder(final EntityPanelBuilder panelBuilder) {
    if (!detailPanelBuilders.contains(panelBuilder)) {
      detailPanelBuilders.add(panelBuilder);
      if (!modelBuilder.containsDetailModelBuilder(panelBuilder.getModelBuilder())) {
        modelBuilder.addDetailModelBuilder(panelBuilder.getModelBuilder());//todo not very clean
      }
    }

    return this;
  }

  /**
   * @return an unmodifiable view of the detail panel providers
   */
  public final List<EntityPanelBuilder> getDetailPanelBuilders() {
    return Collections.unmodifiableList(detailPanelBuilders);
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
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder setRefreshOnInit(final boolean refreshOnInit) {
    this.refreshOnInit = refreshOnInit;
    return this;
  }

  /**
   * @return whether or not the table condition panel is made visible when the panel is initialized
   */
  public final boolean isTableConditionPanelVisible() {
    return tableConditionPanelVisible;
  }

  /**
   * @param tableConditionPanelVisible if true then the table condition panel is made visible when the panel is initialized
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder setTableConditionPanelVisible(final boolean tableConditionPanelVisible) {
    this.tableConditionPanelVisible = tableConditionPanelVisible;
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
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder setDetailPanelState(final EntityPanel.PanelState detailPanelState) {
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
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder setDetailSplitPanelResizeWeight(final double detailSplitPanelResizeWeight) {
    this.detailSplitPanelResizeWeight = detailSplitPanelResizeWeight;
    return this;
  }

  /**
   * Note that setting the EntityPanel class overrides any table panel or edit panel classes that have been set.
   * @param panelClass the EntityPanel class to use when providing this panel
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder setPanelClass(final Class<? extends EntityPanel> panelClass) {
    requireNonNull(panelClass, "panelClass");
    this.panelClass = panelClass;
    return this;
  }

  /**
   * @param editPanelClass the EntityEditPanel class to use when providing this panel
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder setEditPanelClass(final Class<? extends EntityEditPanel> editPanelClass) {
    this.editPanelClass = editPanelClass;
    return this;
  }

  /**
   * @param tablePanelClass the EntityTablePanel class to use when providing this panel
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder setTablePanelClass(final Class<? extends EntityTablePanel> tablePanelClass) {
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
  public final boolean equals(final Object obj) {
    return obj instanceof EntityPanelBuilder &&
            ((EntityPanelBuilder) obj).modelBuilder.getEntityId().equals(modelBuilder.getEntityId()) &&
            ((EntityPanelBuilder) obj).modelBuilder.getModelClass().equals(modelBuilder.getModelClass());
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    return modelBuilder.getEntityId().hashCode() + modelBuilder.getModelClass().hashCode();
  }

  /**
   * Creates an EntityPanel based on this provider configuration
   * @param connectionProvider the connection provider
   * @return an EntityPanel based on this provider configuration
   */
  public final EntityPanel createPanel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, "connectionProvider");
    try {
      final SwingEntityModel entityModel = modelBuilder.createModel(connectionProvider);

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
  public final EntityPanel createPanel(final SwingEntityModel model) {
    requireNonNull(model, "model");
    try {
      final EntityPanel entityPanel = initializePanel(model);
      if (entityPanel.getTablePanel() != null && tableConditionPanelVisible) {
        entityPanel.getTablePanel().setConditionPanelVisible(tableConditionPanelVisible);
      }
      if (!detailPanelBuilders.isEmpty()) {
        entityPanel.setDetailPanelState(detailPanelState);
        entityPanel.setDetailSplitPanelResizeWeight(detailSplitPanelResizeWeight);
        for (final EntityPanelBuilder detailPanelBuilder : detailPanelBuilders) {
          final SwingEntityModel detailModel = model.getDetailModel(detailPanelBuilder.getEntityId());
          final EntityPanel detailPanel = detailPanelBuilder.createPanel(detailModel);
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
    return initializeEditPanel(modelBuilder.createEditModel(connectionProvider));
  }

  /**
   * Creates an EntityTablePanel
   * @param connectionProvider the connection provider
   * @return an EntityTablePanel based on this provider
   */
  public final EntityTablePanel createTablePanel(final EntityConnectionProvider connectionProvider) {
    return initializeTablePanel(modelBuilder.createTableModel(connectionProvider));
  }

  /**
   * Called after the EntityPanel has been constructed, but before it is initialized, override to configure
   * @param entityPanel the EntityPanel just constructed
   */
  protected void configurePanel(final EntityPanel entityPanel) {/*Provided for subclasses*/}

  /**
   * Called after the EntityEditPanel has been constructed, but before it is initialized, override to configure
   * @param editPanel the EntityEditPanel just constructed
   */
  protected void configureEditPanel(final EntityEditPanel editPanel) {/*Provided for subclasses*/}

  /**
   * Called after the EntityTablePanel has been constructed, but before it is initialized, override to configure
   * @param tablePanel the EntityTablePanel just constructed
   */
  protected void configureTablePanel(final EntityTablePanel tablePanel) {/*Provided for subclasses*/}

  private EntityPanel initializePanel(final SwingEntityModel entityModel) {
    try {
      final EntityPanel entityPanel;
      if (panelClass.equals(EntityPanel.class)) {
        final EntityTablePanel tablePanel = entityModel.containsTableModel() ? initializeTablePanel(entityModel.getTableModel()) : null;
        final EntityEditPanel editPanel = editPanelClass == null ? null : initializeEditPanel(entityModel.getEditModel());
        entityPanel = panelClass.getConstructor(SwingEntityModel.class, EntityEditPanel.class, EntityTablePanel.class)
                .newInstance(entityModel, editPanel, tablePanel);
      }
      else {
        entityPanel = findModelConstructor(panelClass).newInstance(entityModel);
      }
      entityPanel.setCaption(caption == null ? entityModel.getConnectionProvider()
              .getDomain().getDefinition(entityModel.getEntityId()).getCaption() : caption);

      return entityPanel;
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private EntityEditPanel initializeEditPanel(final SwingEntityEditModel editModel) {
    if (editPanelClass == null) {
      throw new IllegalArgumentException("No edit panel class has been specified for entity panel provider: " + getEntityId());
    }
    if (!editModel.getEntityId().equals(getEntityId())) {
      throw new IllegalArgumentException("Entity ID mismatch, editModel: " + editModel.getEntityId() + ", required: " + getEntityId());
    }
    try {
      final EntityEditPanel editPanel = findEditModelConstructor(editPanelClass).newInstance(editModel);
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

  private EntityTablePanel initializeTablePanel(final SwingEntityTableModel tableModel) {
    try {
      if (!tableModel.getEntityId().equals(getEntityId())) {
        throw new IllegalArgumentException("Entity ID mismatch, tableModel: " + tableModel.getEntityId() + ", required: " + getEntityId());
      }
      final EntityTablePanel tablePanel = findTableModelConstructor(tablePanelClass).newInstance(tableModel);
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

  private static Constructor<EntityPanel> findModelConstructor(final Class<? extends EntityPanel> panelClass)
          throws NoSuchMethodException {
    for (final Constructor<?> constructor : panelClass.getConstructors()) {
      if (constructor.getParameterCount() == 1 &&
              SwingEntityModel.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
        return (Constructor<EntityPanel>) constructor;
      }
    }

    throw new NoSuchMethodException("Constructor with a single parameter of type SwingEntityModel (or subclass) not found in class: " + panelClass);
  }

  private static Constructor<EntityEditPanel> findEditModelConstructor(final Class<? extends EntityEditPanel> editPanelClass)
          throws NoSuchMethodException {
    for (final Constructor<?> constructor : editPanelClass.getConstructors()) {
      if (constructor.getParameterCount() == 1 &&
              SwingEntityEditModel.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
        return (Constructor<EntityEditPanel>) constructor;
      }
    }

    throw new NoSuchMethodException("Constructor with a single parameter of type SwingEntityEditModel (or subclass) not found in class: " + editPanelClass);
  }

  private static Constructor<EntityTablePanel> findTableModelConstructor(final Class<? extends EntityTablePanel> tablePanelClass)
          throws NoSuchMethodException {
    for (final Constructor<?> constructor : tablePanelClass.getConstructors()) {
      if (constructor.getParameterCount() == 1 &&
              SwingEntityTableModel.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
        return (Constructor<EntityTablePanel>) constructor;
      }
    }

    throw new NoSuchMethodException("Constructor with a single parameter of type SwingEntityTableModel (or subclass) not found in class: " + tablePanelClass);
  }
}
