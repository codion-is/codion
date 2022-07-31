/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.EventDataListener;
import is.codion.common.i18n.Messages;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.dialog.DialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;
import static java.util.Objects.requireNonNull;

final class EntityPanelBuilder implements EntityPanel.Builder {

  private static final double DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT = 0.5;

  private final EntityType entityType;
  private final SwingEntityModel.Builder modelBuilder;
  private final SwingEntityModel model;

  private String caption;
  private boolean refreshOnInit = true;
  private Dimension preferredSize;
  private EntityPanel.PanelState detailPanelState = EntityPanel.PanelState.EMBEDDED;
  private double detailSplitPanelResizeWeight = DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT;
  private boolean tableConditionPanelVisible = EntityTablePanel.CONDITION_PANEL_VISIBLE.get();

  private Class<? extends EntityPanel> panelClass;
  private Class<? extends EntityTablePanel> tablePanelClass;
  private Class<? extends EntityEditPanel> editPanelClass;

  private Consumer<EntityPanel> onBuildPanel = new EmptyOnBuild<>();
  private Consumer<EntityEditPanel> onBuildEditPanel = new EmptyOnBuild<>();
  private Consumer<EntityTablePanel> onBuildTablePanel = new EmptyOnBuild<>();

  private final List<EntityPanel.Builder> detailPanelBuilders = new ArrayList<>();

  EntityPanelBuilder(SwingEntityModel.Builder modelBuilder) {
    this.modelBuilder = requireNonNull(modelBuilder, "modelBuilder");
    this.entityType = modelBuilder.getEntityType();
    this.model = null;
  }

  EntityPanelBuilder(SwingEntityModel model) {
    this.model = requireNonNull(model, "model");
    this.entityType = model.getEntityType();
    this.modelBuilder = null;
  }

  @Override
  public EntityType getEntityType() {
    return entityType;
  }

  @Override
  public EntityPanelBuilder caption(String caption) {
    this.caption = caption;
    return this;
  }

  @Override
  public String getCaption() {
    return caption;
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
  public EntityPanel.Builder detailPanelState(EntityPanel.PanelState detailPanelState) {
    this.detailPanelState = detailPanelState;
    return this;
  }

  @Override
  public EntityPanel.Builder detailSplitPanelResizeWeight(double detailSplitPanelResizeWeight) {
    this.detailSplitPanelResizeWeight = detailSplitPanelResizeWeight;
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
      throw new IllegalStateException("A SwingEntityModel is not avilable in this panel builder: " + entityType);
    }

    return buildPanel(model);
  }

  @Override
  public EntityPanel buildPanel(EntityConnectionProvider connectionProvider) {
    if (modelBuilder == null) {
      throw new IllegalStateException("A SwingEntityModel.Builder is not avilable in this panel builder: " + entityType);
    }

    return buildPanel(modelBuilder.buildModel(requireNonNull(connectionProvider, "connectionProvider")));
  }

  @Override
  public EntityPanel buildPanel(SwingEntityModel model) {
    requireNonNull(model, "model");
    try {
      EntityPanel entityPanel = initializePanel(model);
      if (entityPanel.getTablePanel() != null && tableConditionPanelVisible) {
        entityPanel.getTablePanel().setConditionPanelVisible(tableConditionPanelVisible);
      }
      if (!detailPanelBuilders.isEmpty()) {
        entityPanel.setDetailPanelState(detailPanelState);
        entityPanel.setDetailSplitPanelResizeWeight(detailSplitPanelResizeWeight);
        for (EntityPanel.Builder detailPanelBuilder : detailPanelBuilders) {
          SwingEntityModel detailModel = model.getDetailModel(detailPanelBuilder.getEntityType());
          EntityPanel detailPanel = detailPanelBuilder.buildPanel(detailModel);
          entityPanel.addDetailPanel(detailPanel);
        }
      }
      onBuildPanel.accept(entityPanel);
      if (refreshOnInit && model.containsTableModel()) {
        model.getTableModel().refresh();
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
    return initializeEditPanel(modelBuilder.buildEditModel(connectionProvider));
  }

  @Override
  public EntityTablePanel buildTablePanel(EntityConnectionProvider connectionProvider) {
    return initializeTablePanel(modelBuilder.buildTableModel(connectionProvider));
  }

  @Override
  public Action createEditPanelAction(EntityComboBox comboBox) {
    if (editPanelClass == null) {
      throw new IllegalStateException("Can not create a edit panel action when no edit panel class is specified");
    }

    return new InsertEntityAction(comboBox);
  }

  @Override
  public Action createEditPanelAction(EntitySearchField searchField) {
    if (editPanelClass == null) {
      throw new IllegalStateException("Can not create a edit panel action when no edit panel class is specified");
    }

    return new InsertEntityAction(searchField);
  }

  @Override
  public Action createEditPanelAction(JComponent component, EntityConnectionProvider connectionProvider,
                                      EventDataListener<List<Entity>> insertListener) {
    return new InsertEntityAction(component, connectionProvider, insertListener);
  }

  private EntityPanel initializePanel(SwingEntityModel entityModel) {
    try {
      EntityPanel entityPanel;
      if (getPanelClass().equals(EntityPanel.class)) {
        EntityTablePanel tablePanel = entityModel.containsTableModel() ? initializeTablePanel(entityModel.getTableModel()) : null;
        EntityEditPanel editPanel = getEditPanelClass() == null ? null : initializeEditPanel(entityModel.getEditModel());
        entityPanel = getPanelClass().getConstructor(SwingEntityModel.class, EntityEditPanel.class, EntityTablePanel.class)
                .newInstance(entityModel, editPanel, tablePanel);
      }
      else {
        entityPanel = findModelConstructor(getPanelClass()).newInstance(entityModel);
      }
      entityPanel.setCaption(caption == null ? entityModel.getConnectionProvider()
              .getEntities().getDefinition(entityModel.getEntityType()).getCaption() : caption);
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

  private EntityEditPanel initializeEditPanel(SwingEntityEditModel editModel) {
    if (editPanelClass == null) {
      throw new IllegalArgumentException("No edit panel class has been specified for entity panel builder: " + entityType);
    }
    if (!editModel.getEntityType().equals(entityType)) {
      throw new IllegalArgumentException("Entity type mismatch, editModel: " + editModel.getEntityType() + ", required: " + entityType);
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

  private EntityTablePanel initializeTablePanel(SwingEntityTableModel tableModel) {
    try {
      if (!tableModel.getEntityType().equals(entityType)) {
        throw new IllegalArgumentException("Entity type mismatch, tableModel: " + tableModel.getEntityType() + ", required: " + entityType);
      }
      EntityTablePanel tablePanel = findTableModelConstructor(getTablePanelClass()).newInstance(tableModel);
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

  private Class<? extends EntityPanel> getPanelClass() {
    return panelClass == null ? EntityPanel.class : panelClass;
  }

  private Class<? extends EntityEditPanel> getEditPanelClass() {
    return editPanelClass;
  }

  private Class<? extends EntityTablePanel> getTablePanelClass() {
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

  private final class InsertEntityAction extends AbstractAction {

    private static final int BORDER = 10;

    private final JComponent component;
    private final EntityConnectionProvider connectionProvider;
    private final EventDataListener<List<Entity>> insertListener;
    private final List<Entity> insertedEntities = new ArrayList<>();

    private InsertEntityAction(EntityComboBox comboBox) {
      this(requireNonNull(comboBox, "comboBox"), comboBox.getModel().getConnectionProvider(), inserted -> {
        EntityComboBoxModel comboBoxModel = comboBox.getModel();
        Entity item = inserted.get(0);
        comboBoxModel.addItem(item);
        comboBoxModel.setSelectedItem(item);
      });
    }

    private InsertEntityAction(EntitySearchField searchField) {
      this(requireNonNull(searchField, "searchField"), searchField.getModel().getConnectionProvider(), inserted ->
              searchField.getModel().setSelectedEntities(inserted));
    }

    private InsertEntityAction(JComponent component, EntityConnectionProvider connectionProvider,
                               EventDataListener<List<Entity>> insertListener) {
      super("", frameworkIcons().add());
      this.component = component;
      this.connectionProvider = connectionProvider;
      this.insertListener = insertListener;
      this.component.addPropertyChangeListener("enabled", changeEvent -> setEnabled((Boolean) changeEvent.getNewValue()));
      setEnabled(component.isEnabled());
      addShortcutKey();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (component instanceof JComboBox && ((JComboBox<?>) component).isPopupVisible()) {
        ((JComboBox<?>) component).hidePopup();
      }
      EntityEditPanel editPanel = buildEditPanel(connectionProvider);
      editPanel.initializePanel();
      editPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
      editPanel.getEditModel().addAfterInsertListener(inserted -> {
        this.insertedEntities.clear();
        this.insertedEntities.addAll(inserted);
      });
      State cancelled = State.state();
      Value<Attribute<?>> attributeWithInvalidValue = Value.value();
      JDialog dialog = Dialogs.okCancelDialog(editPanel)
              .owner(component)
              .title(caption == null ? connectionProvider.getEntities().getDefinition(entityType).getCaption() : caption)
              .onShown(dlg -> attributeWithInvalidValue.toOptional()
                      .ifPresent(editPanel::requestComponentFocus))
              .onCancel(() -> cancelled.set(true))
              .build();
      try {
        boolean insertPerformed = false;
        while (!insertPerformed) {
          dialog.setVisible(true);
          if (cancelled.get()) {
            return;//cancelled
          }
          insertPerformed = insert(editPanel.getEditModel(), attributeWithInvalidValue);
          if (insertPerformed && !insertedEntities.isEmpty()) {
            insertListener.onEvent(insertedEntities);
          }
        }
      }
      finally {
        component.requestFocusInWindow();
      }
    }

    private boolean insert(SwingEntityEditModel editModel, Value<Attribute<?>> attributeWithInvalidValue) {
      try {
        WaitCursor.show(component);
        try {
          editModel.insert();

          return true;
        }
        finally {
          WaitCursor.hide(component);
        }
      }
      catch (ValidationException e) {
        attributeWithInvalidValue.set(e.getAttribute());
        JOptionPane.showMessageDialog(component, e.getMessage(),
                Messages.error(), JOptionPane.ERROR_MESSAGE);
      }
      catch (Exception e) {
        DialogExceptionHandler.getInstance().displayException(e, Windows.getParentWindow(component).orElse(null));
      }

      return false;
    }

    private void addShortcutKey() {
      KeyEvents.builder(KeyEvent.VK_INSERT)
              .action(this)
              .enable(component);
    }
  }

  private static final class EmptyOnBuild<T> implements Consumer<T> {
    @Override
    public void accept(T panel) {/*Do nothing*/}
  }
}
