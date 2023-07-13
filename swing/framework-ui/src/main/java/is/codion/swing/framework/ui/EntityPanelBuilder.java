/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.EntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.util.Objects.requireNonNull;

final class EntityPanelBuilder implements EntityPanel.Builder {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityPanelBuilder.class.getName());

  private static final double DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT = 0.5;
  private static final String ENABLED = "enabled";

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
        entityPanel.setDetailPanelState(detailPanelState);
        entityPanel.setDetailSplitPanelResizeWeight(detailSplitPanelResizeWeight);
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

  @Override
  public Control createInsertControl(EntityComboBox comboBox) {
    requireNonNull(comboBox);
    if (editPanelClass == null) {
      throw new IllegalStateException("Can not create a insert control when no edit panel class is specified");
    }

    return createInsertControl(comboBox, comboBox.getModel().connectionProvider(), inserted -> {
      EntityComboBoxModel comboBoxModel = comboBox.getModel();
      Entity item = inserted.get(0);
      comboBoxModel.addItem(item);
      comboBoxModel.setSelectedItem(item);
    });
  }

  @Override
  public Control createInsertControl(EntitySearchField searchField) {
    requireNonNull(searchField);
    if (editPanelClass == null) {
      throw new IllegalStateException("Can not create a insert control when no edit panel class is specified");
    }

    return createInsertControl(searchField, searchField.model().connectionProvider(), inserted ->
            searchField.model().setSelectedEntities(inserted));
  }

  @Override
  public Control createInsertControl(JComponent component, EntityConnectionProvider connectionProvider,
                                     EventDataListener<List<Entity>> onInsert) {
    requireNonNull(component);
    State enabledState = State.state(component.isEnabled());
    component.addPropertyChangeListener(ENABLED, changeEvent -> enabledState.set((Boolean) changeEvent.getNewValue()));

    Control control = Control.builder(new InsertEntityCommand(component, connectionProvider, onInsert))
            .smallIcon(FrameworkIcons.instance().add())
            .description(MESSAGES.getString("insert_new_item"))
            .enabledState(enabledState)
            .build();

    KeyEvents.builder(VK_INSERT)
            .action(control)
            .enable(component);

    return control;
  }

  @Override
  public Control createUpdateControl(EntityComboBox comboBox) {
    requireNonNull(comboBox);
    if (editPanelClass == null) {
      throw new IllegalStateException("Can not create an update control when no edit panel class is specified");
    }

    State enabledState = State.state(comboBox.isEnabled() && !comboBox.getModel().isSelectionEmpty());
    comboBox.addPropertyChangeListener(ENABLED, changeEvent -> enabledState.set((Boolean) changeEvent.getNewValue()));
    comboBox.getModel().addSelectionListener(selected -> enabledState.set(selected != null));

    return createUpdateControl(new UpdateEntityCommand(comboBox), comboBox, enabledState);
  }

  @Override
  public Control createUpdateControl(EntitySearchField searchField) {
    requireNonNull(searchField);
    if (editPanelClass == null) {
      throw new IllegalStateException("Can not create an update control when no edit panel class is specified");
    }

    State enabledState = State.state(searchField.isEnabled() && !searchField.model().getSelectedEntities().isEmpty());
    searchField.addPropertyChangeListener(ENABLED, changeEvent -> enabledState.set((Boolean) changeEvent.getNewValue()));
    searchField.model().addSelectedEntitiesListener(selected -> enabledState.set(!selected.isEmpty()));

    return createUpdateControl(new UpdateEntityCommand(searchField), searchField, enabledState);
  }

  private Control createUpdateControl(UpdateEntityCommand updateEntityCommand, JComponent component, State enabledState) {
    Control control = Control.builder(updateEntityCommand)
            .smallIcon(FrameworkIcons.instance().edit())
            .description(MESSAGES.getString("update_selected_item"))
            .enabledState(enabledState)
            .build();

    KeyEvents.builder(VK_INSERT)
            .modifiers(CTRL_DOWN_MASK)
            .action(control)
            .enable(component);

    return control;
  }

  private EntityPanel createPanel(SwingEntityModel entityModel) {
    try {
      EntityPanel entityPanel;
      if (panelClass().equals(EntityPanel.class)) {
        EntityTablePanel tablePanel = entityModel.containsTableModel() ? createTablePanel(entityModel.tableModel()) : null;
        EntityEditPanel editPanel = editPanelClass() == null ? null : createEditPanel(entityModel.editModel());
        entityPanel = panelClass().getConstructor(SwingEntityModel.class, EntityEditPanel.class, EntityTablePanel.class)
                .newInstance(entityModel, editPanel, tablePanel);
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

  private final class InsertEntityCommand implements Control.Command {

    private static final int BORDER = 10;

    private final JComponent component;
    private final EntityConnectionProvider connectionProvider;
    private final EventDataListener<List<Entity>> onInsert;
    private final List<Entity> insertedEntities = new ArrayList<>();

    private InsertEntityCommand(JComponent component, EntityConnectionProvider connectionProvider,
                                EventDataListener<List<Entity>> onInsert) {
      this.component = requireNonNull(component);
      this.connectionProvider = requireNonNull(connectionProvider);
      this.onInsert = requireNonNull(onInsert);
    }

    @Override
    public void perform() throws Exception {
      EntityEditPanel editPanel = createEditPanel();
      State cancelled = State.state();
      Value<Attribute<?>> invalidAttribute = Value.value();
      JDialog dialog = Dialogs.okCancelDialog(editPanel)
              .owner(component)
              .title(caption == null ? connectionProvider.entities().definition(entityType).caption() : caption)
              .onShown(d -> invalidAttribute.optional()
                      .ifPresent(editPanel::requestComponentFocus))
              .onCancel(() -> cancelled.set(true))
              .build();
      try {
        boolean successfulInsert = false;
        while (!successfulInsert) {
          dialog.setVisible(true);
          if (cancelled.get()) {
            return;//cancelled
          }
          successfulInsert = insert(editPanel.editModel(), invalidAttribute);
          if (successfulInsert && !insertedEntities.isEmpty()) {
            onInsert.onEvent(insertedEntities);
          }
        }
      }
      finally {
        insertedEntities.clear();
      }
    }

    private EntityEditPanel createEditPanel() {
      EntityEditPanel editPanel = buildEditPanel(connectionProvider);
      editPanel.initialize();
      editPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
      editPanel.editModel().addAfterInsertListener(new AfterInsertListener());

      return editPanel;
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
        attributeWithInvalidValue.set(e.attribute());
        JOptionPane.showMessageDialog(component, e.getMessage(), Messages.error(), JOptionPane.ERROR_MESSAGE);
      }
      catch (Exception e) {
        Dialogs.displayExceptionDialog(e, Utilities.parentWindow(component));
      }

      return false;
    }

    private final class AfterInsertListener implements EventDataListener<Collection<Entity>> {

      @Override
      public void onEvent(Collection<Entity> inserted) {
        insertedEntities.clear();
        insertedEntities.addAll(inserted);
      }
    }
  }

  private final class UpdateEntityCommand implements Control.Command {

    private static final int BORDER = 10;

    private final JComponent component;
    private final EntityConnectionProvider connectionProvider;
    private final EventDataListener<List<Entity>> onUpdate;
    private final List<Entity> updatedEntities = new ArrayList<>();

    private Entity entityToUpdate;

    private UpdateEntityCommand(EntityComboBox comboBox) {
      this.component = requireNonNull(comboBox);
      this.connectionProvider = comboBox.getModel().connectionProvider();
      this.onUpdate = new EntityComboBoxOnUpdate();
    }

    private UpdateEntityCommand(EntitySearchField searchField) {
      this.component = requireNonNull(searchField);
      this.connectionProvider = searchField.model().connectionProvider();
      this.onUpdate = new EntitySearchFieldOnUpdate();
    }

    @Override
    public void perform() throws Exception {
      if (component instanceof EntityComboBox) {
        if (((EntityComboBox) component).isPopupVisible()) {
          ((EntityComboBox) component).hidePopup();
        }
        entityToUpdate = ((EntityComboBox) component).getModel().selectedValue();
      }
      else {
        entityToUpdate = ((EntitySearchField) component).model().getSelectedEntities().get(0);
      }
      EntityEditPanel editPanel = createEditPanel();
      editPanel.editModel().setEntity(connectionProvider.connection().select(entityToUpdate.primaryKey()));
      State cancelled = State.state();
      Value<Attribute<?>> invalidAttribute = Value.value();
      JDialog dialog = Dialogs.okCancelDialog(editPanel)
              .owner(component)
              .title(caption == null ? connectionProvider.entities().definition(entityType).caption() : caption)
              .onShown(d -> invalidAttribute.optional()
                      .ifPresent(editPanel::requestComponentFocus))
              .onCancel(() -> cancelled.set(true))
              .build();
      try {
        boolean successfulUpdate = false;
        while (!successfulUpdate) {
          dialog.setVisible(true);
          if (cancelled.get()) {
            return;//cancelled
          }
          successfulUpdate = update(editPanel.editModel(), invalidAttribute);
          if (successfulUpdate && !updatedEntities.isEmpty()) {
            onUpdate.onEvent(updatedEntities);
          }
        }
      }
      finally {
        entityToUpdate = null;
        updatedEntities.clear();
      }
    }

    private EntityEditPanel createEditPanel() {
      EntityEditPanel editPanel = buildEditPanel(connectionProvider);
      editPanel.initialize();
      editPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
      editPanel.editModel().addAfterUpdateListener(new AfterUpdateListener());

      return editPanel;
    }

    private boolean update(SwingEntityEditModel editModel, Value<Attribute<?>> attributeWithInvalidValue) {
      try {
        WaitCursor.show(component);
        try {
          if (editModel.isModified()) {
            editModel.update();
          }

          return true;
        }
        finally {
          WaitCursor.hide(component);
        }
      }
      catch (ValidationException e) {
        attributeWithInvalidValue.set(e.attribute());
        JOptionPane.showMessageDialog(component, e.getMessage(), Messages.error(), JOptionPane.ERROR_MESSAGE);
      }
      catch (Exception e) {
        Dialogs.displayExceptionDialog(e, Utilities.parentWindow(component));
      }

      return false;
    }

    private final class EntityComboBoxOnUpdate implements EventDataListener<List<Entity>> {

      @Override
      public void onEvent(List<Entity> updated) {
        EntityComboBoxModel comboBoxModel = ((EntityComboBox) component).getModel();
        Entity item = updated.get(0);
        comboBoxModel.replaceItem(entityToUpdate, item);
        comboBoxModel.setSelectedItem(item);
      }
    }

    private final class EntitySearchFieldOnUpdate implements EventDataListener<List<Entity>> {

      @Override
      public void onEvent(List<Entity> updated) {
        ((EntitySearchField) component).model().setSelectedEntities(updated);
      }
    }

    private final class AfterUpdateListener implements EventDataListener<Map<Key, Entity>> {

      @Override
      public void onEvent(Map<Key, Entity> updated) {
        updatedEntities.clear();
        updatedEntities.addAll(updated.values());
      }
    }
  }

  private static final class EmptyOnBuild<T> implements Consumer<T> {
    @Override
    public void accept(T panel) {/*Do nothing*/}
  }
}
