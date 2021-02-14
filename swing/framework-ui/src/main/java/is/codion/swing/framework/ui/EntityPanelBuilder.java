/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.EventDataListener;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;
import static java.util.Objects.requireNonNull;

final class EntityPanelBuilder implements EntityPanel.Builder {

  private static final double DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT = 0.5;

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

  private final SwingEntityModel.Builder modelBuilder;

  private final List<EntityPanel.Builder> detailPanelBuilders = new ArrayList<>();

  EntityPanelBuilder(final SwingEntityModel.Builder modelBuilder) {
    this.modelBuilder = requireNonNull(modelBuilder, "modelBuilder");
  }

  @Override
  public EntityType<?> getEntityType() {
    return modelBuilder.getEntityType();
  }

  @Override
  public SwingEntityModel.Builder getModelBuilder() {
    return modelBuilder;
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
      modelBuilder.detailModelBuilder(panelBuilder.getModelBuilder());//todo not very clean
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
    return obj instanceof EntityPanelBuilder &&
            ((EntityPanelBuilder) obj).modelBuilder.getEntityType().equals(modelBuilder.getEntityType()) &&
            ((EntityPanelBuilder) obj).modelBuilder.getModelClass().equals(modelBuilder.getModelClass());
  }

  @Override
  public int hashCode() {
    return modelBuilder.getEntityType().hashCode() + modelBuilder.getModelClass().hashCode();
  }

  @Override
  public EntityPanel buildPanel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, "connectionProvider");
    try {
      return buildPanel(modelBuilder.buildModel(connectionProvider));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
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
    return new InsertEntityAction(comboBox);
  }

  @Override
  public Action createEditPanelAction(final EntityLookupField lookupField) {
    return new InsertEntityAction(lookupField);
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
      throw new IllegalArgumentException("No edit panel class has been specified for entity panel provider: " + getEntityType());
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

    private InsertEntityAction(final EntityLookupField lookupField) {
      this(requireNonNull(lookupField, "lookupField"), lookupField.getModel().getConnectionProvider(), inserted ->
              lookupField.getModel().setSelectedEntities(inserted));
    }

    private InsertEntityAction(final JComponent component, final EntityConnectionProvider connectionProvider,
                               final EventDataListener<List<Entity>> insertListener) {
      super("", frameworkIcons().add());
      this.component = component;
      this.connectionProvider = connectionProvider;
      this.insertListener = insertListener;
      this.component.addPropertyChangeListener("enabled", changeEvent -> setEnabled((Boolean) changeEvent.getNewValue()));
      setEnabled(component.isEnabled());
      addLookupKey();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      final EntityEditPanel editPanel = buildEditPanel(connectionProvider);
      editPanel.initializePanel();
      editPanel.getEditModel().addAfterInsertListener(inserted -> {
        this.insertedEntities.clear();
        this.insertedEntities.addAll(inserted);
      });
      final JOptionPane pane = new JOptionPane(editPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
      final JDialog dialog = pane.createDialog(component, getCaption() == null ?
              connectionProvider.getEntities().getDefinition(getEntityType()).getCaption() : getCaption());
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      Components.addInitialFocusHack(editPanel, Controls.control(editPanel::requestInitialFocus));
      dialog.setVisible(true);
      if (pane.getValue() != null && pane.getValue().equals(0)) {
        final boolean insertPerformed = editPanel.insert();//todo exception during insert, f.ex validation failure not handled
        if (insertPerformed && !insertedEntities.isEmpty()) {
          insertListener.onEvent(insertedEntities);
        }
      }
      component.requestFocusInWindow();
    }

    private void addLookupKey() {
      JComponent keyComponent = component;
      if (component instanceof JComboBox && ((JComboBox<?>) component).isEditable()) {
        keyComponent = (JComponent) ((JComboBox<?>) component).getEditor().getEditorComponent();
      }
      KeyEvents.addKeyEvent(keyComponent, KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK, this);
      KeyEvents.addKeyEvent(keyComponent, KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK, this);
    }
  }
}
