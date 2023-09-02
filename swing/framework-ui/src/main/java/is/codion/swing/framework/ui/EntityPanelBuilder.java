/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityPanel.PanelLayout;

import java.awt.Dimension;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class EntityPanelBuilder implements EntityPanel.Builder {

  private final EntityType entityType;
  private final SwingEntityModel.Builder modelBuilder;
  private final SwingEntityModel model;

  private String caption;
  private boolean refreshOnInit = true;
  private Dimension preferredSize;
  private boolean tableConditionPanelVisible = EntityTablePanel.CONDITION_PANEL_VISIBLE.get();
  private PanelLayout panelLayout;

  private Class<? extends EntityPanel> panelClass;
  private Class<? extends EntityTablePanel> tablePanelClass;
  private Class<? extends EntityEditPanel> editPanelClass;

  private Consumer<EntityPanel> onBuildPanel = new EmptyOnBuild<>();
  private Consumer<EntityEditPanel> onBuildEditPanel = new EmptyOnBuild<>();
  private Consumer<EntityTablePanel> onBuildTablePanel = new EmptyOnBuild<>();

  private final List<EntityPanel.Builder> detailPanelBuilders = new ArrayList<>();

  EntityPanelBuilder(SwingEntityModel.Builder modelBuilder) {
    this.modelBuilder = requireNonNull(modelBuilder, "modelBuilder");
    this.entityType = modelBuilder.entityType();
    this.model = null;
  }

  EntityPanelBuilder(SwingEntityModel model) {
    this.model = requireNonNull(model, "model");
    this.entityType = model.entityType();
    this.modelBuilder = null;
  }

  @Override
  public EntityType entityType() {
    return entityType;
  }

  @Override
  public EntityPanelBuilder caption(String caption) {
    this.caption = caption;
    return this;
  }

  @Override
  public Optional<String> caption() {
    return Optional.ofNullable(caption);
  }

  @Override
  public EntityPanel.Builder detailPanelBuilder(EntityPanel.Builder panelBuilder) {
    if (!detailPanelBuilders.contains(panelBuilder)) {
      detailPanelBuilders.add(panelBuilder);
    }

    return this;
  }

  @Override
  public EntityPanel.Builder refreshOnInit(boolean refreshOnInit) {
    this.refreshOnInit = refreshOnInit;
    return this;
  }

  @Override
  public EntityPanel.Builder tableConditionPanelVisible(boolean tableConditionPanelVisible) {
    this.tableConditionPanelVisible = tableConditionPanelVisible;
    return this;
  }

  @Override
  public EntityPanel.Builder panelLayout(PanelLayout panelLayout) {
    this.panelLayout = requireNonNull(panelLayout);
    return this;
  }

  @Override
  public EntityPanel.Builder preferredSize(Dimension preferredSize) {
    this.preferredSize = requireNonNull(preferredSize);
    return this;
  }

  @Override
  public EntityPanel.Builder panelClass(Class<? extends EntityPanel> panelClass) {
    if (editPanelClass != null || tablePanelClass != null) {
      throw new IllegalStateException("Edit or table panel class has been set");
    }
    this.panelClass = requireNonNull(panelClass, "panelClass");
    return this;
  }

  @Override
  public EntityPanel.Builder editPanelClass(Class<? extends EntityEditPanel> editPanelClass) {
    if (panelClass != null) {
      throw new IllegalStateException("Panel class has been set");
    }
    this.editPanelClass = requireNonNull(editPanelClass, "editPanelClass");
    return this;
  }

  @Override
  public EntityPanel.Builder tablePanelClass(Class<? extends EntityTablePanel> tablePanelClass) {
    if (panelClass != null) {
      throw new IllegalStateException("Panel class has been set");
    }
    this.tablePanelClass = requireNonNull(tablePanelClass, "tablePanelClass");
    return this;
  }

  @Override
  public EntityPanel.Builder onBuildPanel(Consumer<EntityPanel> onBuildPanel) {
    this.onBuildPanel = requireNonNull(onBuildPanel);
    return this;
  }

  @Override
  public EntityPanelBuilder onBuildEditPanel(Consumer<EntityEditPanel> onBuildEditPanel) {
    this.onBuildEditPanel = requireNonNull(onBuildEditPanel);
    return this;
  }

  @Override
  public EntityPanel.Builder onBuildTablePanel(Consumer<EntityTablePanel> onBuildTablePanel) {
    this.onBuildTablePanel = requireNonNull(onBuildTablePanel);
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof EntityPanelBuilder) {
      EntityPanelBuilder that = (EntityPanelBuilder) obj;

      return Objects.equals(modelBuilder, that.model) &&
              Objects.equals(model, that.model) &&
              Objects.equals(panelClass, that.panelClass) &&
              Objects.equals(editPanelClass, that.editPanelClass) &&
              Objects.equals(tablePanelClass, that.tablePanelClass);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(modelBuilder, model, panelClass, editPanelClass, tablePanelClass);
  }

  @Override
  public EntityPanel buildPanel() {
    if (model == null) {
      throw new IllegalStateException("A SwingEntityModel is not available in this panel builder: " + entityType);
    }

    return buildPanel(model);
  }

  @Override
  public EntityPanel buildPanel(EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, "connectionProvider");
    if (modelBuilder == null) {
      throw new IllegalStateException("A SwingEntityModel.Builder is not available in this panel builder: " + entityType);
    }

    return buildPanel(modelBuilder.buildModel(connectionProvider));
  }

  @Override
  public EntityPanel buildPanel(SwingEntityModel model) {
    requireNonNull(model, "model");
    try {
      EntityPanel entityPanel = createPanel(model);
      if (entityPanel.tablePanel() != null && tableConditionPanelVisible) {
        entityPanel.tablePanel().setConditionPanelVisible(tableConditionPanelVisible);
      }
      if (!detailPanelBuilders.isEmpty()) {
        for (EntityPanel.Builder detailPanelBuilder : detailPanelBuilders) {
          SwingEntityModel detailModel = model.detailModel(detailPanelBuilder.entityType());
          EntityPanel detailPanel = detailPanelBuilder.buildPanel(detailModel);
          entityPanel.addDetailPanel(detailPanel);
        }
      }
      onBuildPanel.accept(entityPanel);
      if (refreshOnInit && model.containsTableModel()) {
        model.tableModel().refresh();
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

  @Override
  public EntityEditPanel buildEditPanel(EntityConnectionProvider connectionProvider) {
    return createEditPanel(modelBuilder.buildEditModel(connectionProvider));
  }

  @Override
  public EntityTablePanel buildTablePanel(EntityConnectionProvider connectionProvider) {
    return createTablePanel(modelBuilder.buildTableModel(connectionProvider));
  }

  private EntityPanel createPanel(SwingEntityModel entityModel) {
    try {
      EntityPanel entityPanel;
      if (panelClass().equals(EntityPanel.class)) {
        EntityTablePanel tablePanel = entityModel.containsTableModel() ? createTablePanel(entityModel.tableModel()) : null;
        EntityEditPanel editPanel = editPanelClass() == null ? null : createEditPanel(entityModel.editModel());
        entityPanel = createPanel(entityModel, editPanel, tablePanel);
      }
      else {
        entityPanel = findModelConstructor(panelClass()).newInstance(entityModel);
      }
      entityPanel.setCaption(caption == null ? entityModel.connectionProvider()
              .entities().definition(entityModel.entityType()).caption() : caption);
      if (preferredSize != null) {
        entityPanel.setPreferredSize(preferredSize);
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

  private EntityPanel createPanel(SwingEntityModel entityModel, EntityEditPanel editPanel, EntityTablePanel tablePanel)
          throws Exception {
    if (panelLayout != null) {
      return panelClass().getConstructor(SwingEntityModel.class, EntityEditPanel.class, EntityTablePanel.class, PanelLayout.class)
              .newInstance(entityModel, editPanel, tablePanel, panelLayout);
    }

    return  panelClass().getConstructor(SwingEntityModel.class, EntityEditPanel.class, EntityTablePanel.class)
            .newInstance(entityModel, editPanel, tablePanel);
  }

  private EntityEditPanel createEditPanel(SwingEntityEditModel editModel) {
    if (editPanelClass == null) {
      throw new IllegalArgumentException("No edit panel class has been specified for entity panel builder: " + entityType);
    }
    if (!editModel.entityType().equals(entityType)) {
      throw new IllegalArgumentException("Entity type mismatch, editModel: " + editModel.entityType() + ", required: " + entityType);
    }
    try {
      EntityEditPanel editPanel = findEditModelConstructor(editPanelClass).newInstance(editModel);
      onBuildEditPanel.accept(editPanel);

      return editPanel;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private EntityTablePanel createTablePanel(SwingEntityTableModel tableModel) {
    try {
      if (!tableModel.entityType().equals(entityType)) {
        throw new IllegalArgumentException("Entity type mismatch, tableModel: " + tableModel.entityType() + ", required: " + entityType);
      }
      EntityTablePanel tablePanel = findTableModelConstructor(tablePanelClass()).newInstance(tableModel);
      onBuildTablePanel.accept(tablePanel);

      return tablePanel;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Class<? extends EntityPanel> panelClass() {
    return panelClass == null ? EntityPanel.class : panelClass;
  }

  private Class<? extends EntityEditPanel> editPanelClass() {
    return editPanelClass;
  }

  private Class<? extends EntityTablePanel> tablePanelClass() {
    return tablePanelClass == null ? EntityTablePanel.class : tablePanelClass;
  }

  private static Constructor<EntityPanel> findModelConstructor(Class<? extends EntityPanel> panelClass)
          throws NoSuchMethodException {
    for (Constructor<?> constructor : panelClass.getConstructors()) {
      if (constructor.getParameterCount() == 1 &&
              SwingEntityModel.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
        return (Constructor<EntityPanel>) constructor;
      }
    }

    throw new NoSuchMethodException("Constructor with a single parameter of type SwingEntityModel (or subclass) not found in class: " + panelClass);
  }

  private static Constructor<EntityEditPanel> findEditModelConstructor(Class<? extends EntityEditPanel> editPanelClass)
          throws NoSuchMethodException {
    for (Constructor<?> constructor : editPanelClass.getConstructors()) {
      if (constructor.getParameterCount() == 1 &&
              SwingEntityEditModel.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
        return (Constructor<EntityEditPanel>) constructor;
      }
    }

    throw new NoSuchMethodException("Constructor with a single parameter of type SwingEntityEditModel (or subclass) not found in class: " + editPanelClass);
  }

  private static Constructor<EntityTablePanel> findTableModelConstructor(Class<? extends EntityTablePanel> tablePanelClass)
          throws NoSuchMethodException {
    for (Constructor<?> constructor : tablePanelClass.getConstructors()) {
      if (constructor.getParameterCount() == 1 &&
              SwingEntityTableModel.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
        return (Constructor<EntityTablePanel>) constructor;
      }
    }

    throw new NoSuchMethodException("Constructor with a single parameter of type SwingEntityTableModel (or subclass) not found in class: " + tablePanelClass);
  }

  private static final class EmptyOnBuild<T> implements Consumer<T> {
    @Override
    public void accept(T panel) {/*Do nothing*/}
  }
}
