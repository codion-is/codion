/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
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
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
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
  private EntityPanel.PanelState detailPanelState = EntityPanel.PanelState.EMBEDDED;
  private double detailSplitPanelResizeWeight = DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT;
  private boolean tableConditionPanelVisible = EntityTablePanel.TABLE_CONDITION_PANEL_VISIBLE.get();

  private Class<? extends EntityPanel> panelClass;
  private Class<? extends EntityTablePanel> tablePanelClass;
  private Class<? extends EntityEditPanel> editPanelClass;

  private Consumer<EntityPanel> panelInitializer = panel -> {};
  private Consumer<EntityEditPanel> editPanelInitializer = editPanel ->  {};
  private Consumer<EntityTablePanel> tablePanelInitializer = tablePanel -> {};

  private final List<EntityPanel.Builder> detailPanelBuilders = new ArrayList<>();

  EntityPanelBuilder(final SwingEntityModel.Builder modelBuilder) {
    this.modelBuilder = requireNonNull(modelBuilder, "modelBuilder");
    this.entityType = modelBuilder.getEntityType();
    this.model = null;
  }

  EntityPanelBuilder(final SwingEntityModel model) {
    this.model = requireNonNull(model, "model");
    this.entityType = model.getEntityType();
    this.modelBuilder = null;
  }

  @Override
  public EntityType getEntityType() {
    return entityType;
  }

  @Override
  public SwingEntityModel.Builder getModelBuilder() {
    return modelBuilder;
  }

  @Override
  public boolean containsModel() {
    return model != null;
  }

  @Override
  public EntityPanelBuilder caption(final String caption) {
    this.caption = caption;
    return this;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public EntityPanel.Builder detailPanelBuilder(final EntityPanel.Builder panelBuilder) {
    if (!detailPanelBuilders.contains(panelBuilder)) {
      detailPanelBuilders.add(panelBuilder);
    }

    return this;
  }

  @Override
  public EntityPanel.Builder refreshOnInit(final boolean refreshOnInit) {
    this.refreshOnInit = refreshOnInit;
    return this;
  }

  @Override
  public EntityPanel.Builder tableConditionPanelVisible(final boolean tableConditionPanelVisible) {
    this.tableConditionPanelVisible = tableConditionPanelVisible;
    return this;
  }

  @Override
  public EntityPanel.Builder detailPanelState(final EntityPanel.PanelState detailPanelState) {
    this.detailPanelState = detailPanelState;
    return this;
  }

  @Override
  public EntityPanel.Builder detailSplitPanelResizeWeight(final double detailSplitPanelResizeWeight) {
    this.detailSplitPanelResizeWeight = detailSplitPanelResizeWeight;
    return this;
  }

  @Override
  public EntityPanel.Builder panelClass(final Class<? extends EntityPanel> panelClass) {
    if (editPanelClass != null || tablePanelClass != null) {
      throw new IllegalStateException("Edit or table panel class has been set");
    }
    this.panelClass = requireNonNull(panelClass, "panelClass");
    return this;
  }

  @Override
  public EntityPanel.Builder editPanelClass(final Class<? extends EntityEditPanel> editPanelClass) {
    if (panelClass != null) {
      throw new IllegalStateException("Panel class has been set");
    }
    this.editPanelClass = requireNonNull(editPanelClass, "editPanelClass");
    return this;
  }

  @Override
  public EntityPanel.Builder tablePanelClass(final Class<? extends EntityTablePanel> tablePanelClass) {
    if (panelClass != null) {
      throw new IllegalStateException("Panel class has been set");
    }
    this.tablePanelClass = requireNonNull(tablePanelClass, "tablePanelClass");
    return this;
  }

  @Override
  public EntityPanel.Builder panelInitializer(final Consumer<EntityPanel> panelInitializer) {
    this.panelInitializer = requireNonNull(panelInitializer);
    return this;
  }

  @Override
  public EntityPanelBuilder editPanelInitializer(final Consumer<EntityEditPanel> editPanelInitializer) {
    this.editPanelInitializer = requireNonNull(editPanelInitializer);
    return this;
  }

  @Override
  public EntityPanel.Builder tablePanelInitializer(final Consumer<EntityTablePanel> tablePanelInitializer) {
    this.tablePanelInitializer = requireNonNull(tablePanelInitializer);
    return this;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof EntityPanelBuilder) {
      final EntityPanelBuilder that = (EntityPanelBuilder) obj;

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
      throw new IllegalStateException("A SwingEntityModel is not avilable in this panel builder: " + getEntityType());
    }

    return buildPanel(model);
  }

  @Override
  public EntityPanel buildPanel(final EntityConnectionProvider connectionProvider) {
    if (modelBuilder == null) {
      throw new IllegalStateException("A SwingEntityModel.Builder is not avilable in this panel builder: " + getEntityType());
    }

    return buildPanel(modelBuilder.buildModel(requireNonNull(connectionProvider, "connectionProvider")));
  }

  @Override
  public EntityPanel buildPanel(final SwingEntityModel model) {
    requireNonNull(model, "model");
    try {
      final EntityPanel entityPanel = initializePanel(model);
      if (entityPanel.getTablePanel() != null && tableConditionPanelVisible) {
        entityPanel.getTablePanel().setConditionPanelVisible(tableConditionPanelVisible);
      }
      if (!detailPanelBuilders.isEmpty()) {
        entityPanel.setDetailPanelState(detailPanelState);
        entityPanel.setDetailSplitPanelResizeWeight(detailSplitPanelResizeWeight);
        for (final EntityPanel.Builder detailPanelBuilder : detailPanelBuilders) {
          final SwingEntityModel detailModel = model.getDetailModel(detailPanelBuilder.getEntityType());
          final EntityPanel detailPanel = detailPanelBuilder.buildPanel(detailModel);
          entityPanel.addDetailPanel(detailPanel);
        }
      }
      panelInitializer.accept(entityPanel);
      if (refreshOnInit && model.containsTableModel()) {
        model.getTableModel().refresh();
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

  @Override
  public EntityEditPanel buildEditPanel(final EntityConnectionProvider connectionProvider) {
    return initializeEditPanel(modelBuilder.buildEditModel(connectionProvider));
  }

  @Override
  public EntityTablePanel buildTablePanel(final EntityConnectionProvider connectionProvider) {
    return initializeTablePanel(modelBuilder.buildTableModel(connectionProvider));
  }

  @Override
  public Action createEditPanelAction(final EntityComboBox comboBox) {
    if (editPanelClass == null) {
      throw new IllegalStateException("Can not create a edit panel action when no edit panel class is specified");
    }

    return new InsertEntityAction(comboBox);
  }

  @Override
  public Action createEditPanelAction(final EntitySearchField searchField) {
    if (editPanelClass == null) {
      throw new IllegalStateException("Can not create a edit panel action when no edit panel class is specified");
    }

    return new InsertEntityAction(searchField);
  }

  @Override
  public Action createEditPanelAction(final JComponent component, final EntityConnectionProvider connectionProvider,
                                      final EventDataListener<List<Entity>> insertListener) {
    return new InsertEntityAction(component, connectionProvider, insertListener);
  }

  private EntityPanel initializePanel(final SwingEntityModel entityModel) {
    try {
      final EntityPanel entityPanel;
      if (getPanelClass().equals(EntityPanel.class)) {
        final EntityTablePanel tablePanel = entityModel.containsTableModel() ? initializeTablePanel(entityModel.getTableModel()) : null;
        final EntityEditPanel editPanel = getEditPanelClass() == null ? null : initializeEditPanel(entityModel.getEditModel());
        entityPanel = getPanelClass().getConstructor(SwingEntityModel.class, EntityEditPanel.class, EntityTablePanel.class)
                .newInstance(entityModel, editPanel, tablePanel);
      }
      else {
        entityPanel = findModelConstructor(getPanelClass()).newInstance(entityModel);
      }
      entityPanel.setCaption(caption == null ? entityModel.getConnectionProvider()
              .getEntities().getDefinition(entityModel.getEntityType()).getCaption() : caption);

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
      throw new IllegalArgumentException("No edit panel class has been specified for entity panel builder: " + getEntityType());
    }
    if (!editModel.getEntityType().equals(getEntityType())) {
      throw new IllegalArgumentException("Entity type mismatch, editModel: " + editModel.getEntityType() + ", required: " + getEntityType());
    }
    try {
      final EntityEditPanel editPanel = findEditModelConstructor(editPanelClass).newInstance(editModel);
      editPanelInitializer.accept(editPanel);

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
      if (!tableModel.getEntityType().equals(getEntityType())) {
        throw new IllegalArgumentException("Entity type mismatch, tableModel: " + tableModel.getEntityType() + ", required: " + getEntityType());
      }
      final EntityTablePanel tablePanel = findTableModelConstructor(getTablePanelClass()).newInstance(tableModel);
      tablePanelInitializer.accept(tablePanel);

      return tablePanel;
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
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

  private final class InsertEntityAction extends AbstractAction {

    private static final int BORDER = 10;

    private final JComponent component;
    private final EntityConnectionProvider connectionProvider;
    private final EventDataListener<List<Entity>> insertListener;
    private final List<Entity> insertedEntities = new ArrayList<>();

    private InsertEntityAction(final EntityComboBox comboBox) {
      this(requireNonNull(comboBox, "comboBox"), comboBox.getModel().getConnectionProvider(), inserted -> {
        final EntityComboBoxModel comboBoxModel = comboBox.getModel();
        final Entity item = inserted.get(0);
        comboBoxModel.addItem(item);
        comboBoxModel.setSelectedItem(item);
      });
    }

    private InsertEntityAction(final EntitySearchField searchField) {
      this(requireNonNull(searchField, "searchField"), searchField.getModel().getConnectionProvider(), inserted ->
              searchField.getModel().setSelectedEntities(inserted));
    }

    private InsertEntityAction(final JComponent component, final EntityConnectionProvider connectionProvider,
                               final EventDataListener<List<Entity>> insertListener) {
      super("", frameworkIcons().add());
      this.component = component;
      this.connectionProvider = connectionProvider;
      this.insertListener = insertListener;
      this.component.addPropertyChangeListener("enabled", changeEvent -> setEnabled((Boolean) changeEvent.getNewValue()));
      setEnabled(component.isEnabled());
      addShortcutKey();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      if (component instanceof JComboBox && ((JComboBox<?>) component).isPopupVisible()) {
        ((JComboBox<?>) component).hidePopup();
      }
      final EntityEditPanel editPanel = buildEditPanel(connectionProvider);
      editPanel.initializePanel();
      editPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
      editPanel.getEditModel().addAfterInsertListener(inserted -> {
        this.insertedEntities.clear();
        this.insertedEntities.addAll(inserted);
      });
      final State cancelled = State.state();
      final Value<Attribute<?>> invalidAttribute = Value.value();
      final JDialog dialog = Dialogs.okCancelDialog(editPanel)
              .owner(component)
              .title(getCaption() == null ? connectionProvider.getEntities().getDefinition(getEntityType()).getCaption() : getCaption())
              .onShown(dlg -> invalidAttribute.toOptional()
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
          insertPerformed = insert(editPanel.getEditModel(), invalidAttribute);
          if (insertPerformed && !insertedEntities.isEmpty()) {
            insertListener.onEvent(insertedEntities);
          }
        }
      }
      finally {
        component.requestFocusInWindow();
      }
    }

    private boolean insert(final SwingEntityEditModel editModel, final Value<Attribute<?>> invalidAttribute) {
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
      catch (final ValidationException e) {
        invalidAttribute.set(e.getAttribute());
        JOptionPane.showMessageDialog(component, e.getMessage(),
                Messages.get(Messages.ERROR), JOptionPane.ERROR_MESSAGE);
      }
      catch (final Exception e) {
        DefaultDialogExceptionHandler.getInstance().displayException(e, Windows.getParentWindow(component).orElse(null));
      }

      return false;
    }

    private void addShortcutKey() {
      JComponent keyComponent = component;
      if (component instanceof JComboBox && ((JComboBox<?>) component).isEditable()) {
        keyComponent = (JComponent) ((JComboBox<?>) component).getEditor().getEditorComponent();
      }
      KeyEvents.builder(KeyEvent.VK_INSERT)
              .action(this)
              .enable(keyComponent);
      KeyEvents.builder(KeyEvent.VK_ADD)
              .modifiers(InputEvent.CTRL_DOWN_MASK)
              .action(this)
              .enable(keyComponent);
      KeyEvents.builder(KeyEvent.VK_PLUS)
              .modifiers(InputEvent.CTRL_DOWN_MASK)
              .action(this)
              .enable(keyComponent);
    }
  }
}
