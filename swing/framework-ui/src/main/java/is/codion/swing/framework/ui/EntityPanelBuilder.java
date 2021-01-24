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
import is.codion.swing.framework.model.SwingEntityModelBuilder;
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

import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;
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

  private Class<? extends EntityPanel> panelClass;
  private Class<? extends EntityTablePanel> tablePanelClass;
  private Class<? extends EntityEditPanel> editPanelClass;

  private final SwingEntityModelBuilder modelBuilder;

  private final List<EntityPanelBuilder> detailPanelBuilders = new ArrayList<>();

  /**
   * Instantiates a new EntityPanelBuilder for the given entity type
   * @param entityType the entityType
   */
  public EntityPanelBuilder(final EntityType<?> entityType) {
    this(new SwingEntityModelBuilder(entityType));
  }

  /**
   * Instantiates a new EntityPanelBuilder
   * @param modelBuilder the EntityModelBuilder to base this panel provider on
   */
  public EntityPanelBuilder(final SwingEntityModelBuilder modelBuilder) {
    this.modelBuilder = requireNonNull(modelBuilder, "modelBuilder");
  }

  /**
   * @return the entityType
   */
  public final EntityType<?> getEntityType() {
    return modelBuilder.getEntityType();
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
  public final EntityPanelBuilder caption(final String caption) {
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
   * Adds the given detail panel builder to this panel builder, if it hasn't been previously added
   * @param panelBuilder the detail panel provider
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder detailPanelBuilder(final EntityPanelBuilder panelBuilder) {
    if (!detailPanelBuilders.contains(panelBuilder)) {
      detailPanelBuilders.add(panelBuilder);
      modelBuilder.detailModelBuilder(panelBuilder.getModelBuilder());//todo not very clean
    }

    return this;
  }

  /**
   * @param refreshOnInit if true then the data model this panel is based on will be refreshed when
   * the panel is initialized
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder refreshOnInit(final boolean refreshOnInit) {
    this.refreshOnInit = refreshOnInit;
    return this;
  }

  /**
   * @param tableConditionPanelVisible if true then the table condition panel is made visible when the panel is initialized
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder tableConditionPanelVisible(final boolean tableConditionPanelVisible) {
    this.tableConditionPanelVisible = tableConditionPanelVisible;
    return this;
  }

  /**
   * @param detailPanelState the state of the detail panels when this panel is initialized
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder detailPanelState(final EntityPanel.PanelState detailPanelState) {
    this.detailPanelState = detailPanelState;
    return this;
  }

  /**
   * @param detailSplitPanelResizeWeight the split panel resize weight to use when initializing this panel
   * with its detail panels
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder detailSplitPanelResizeWeight(final double detailSplitPanelResizeWeight) {
    this.detailSplitPanelResizeWeight = detailSplitPanelResizeWeight;
    return this;
  }

  /**
   * Note that setting the EntityPanel class overrides any table panel or edit panel classes that have been set.
   * @param panelClass the EntityPanel class to use when providing this panel
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder panelClass(final Class<? extends EntityPanel> panelClass) {
    if (editPanelClass != null || tablePanelClass != null) {
      throw new IllegalStateException("Edit or table panel class has been set");
    }
    this.panelClass = requireNonNull(panelClass, "panelClass");
    return this;
  }

  /**
   * @param editPanelClass the EntityEditPanel class to use when providing this panel
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder editPanelClass(final Class<? extends EntityEditPanel> editPanelClass) {
    if (panelClass != null) {
      throw new IllegalStateException("Panel class has been set");
    }
    this.editPanelClass = requireNonNull(editPanelClass, "editPanelClass");
    return this;
  }

  /**
   * @param tablePanelClass the EntityTablePanel class to use when providing this panel
   * @return this EntityPanelBuilder instance
   */
  public final EntityPanelBuilder tablePanelClass(final Class<? extends EntityTablePanel> tablePanelClass) {
    if (panelClass != null) {
      throw new IllegalStateException("Panel class has been set");
    }
    this.tablePanelClass = requireNonNull(tablePanelClass, "tablePanelClass");
    return this;
  }

  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof EntityPanelBuilder &&
            ((EntityPanelBuilder) obj).modelBuilder.getEntityType().equals(modelBuilder.getEntityType()) &&
            ((EntityPanelBuilder) obj).modelBuilder.getModelClass().equals(modelBuilder.getModelClass());
  }

  @Override
  public final int hashCode() {
    return modelBuilder.getEntityType().hashCode() + modelBuilder.getModelClass().hashCode();
  }

  /**
   * Creates an EntityPanel based on this provider configuration
   * @param connectionProvider the connection provider
   * @return an EntityPanel based on this provider configuration
   */
  public final EntityPanel buildPanel(final EntityConnectionProvider connectionProvider) {
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

  /**
   * Creates an EntityPanel based on this provider configuration
   * @param model the EntityModel to base this panel on
   * @return an EntityPanel based on this provider configuration
   */
  public final EntityPanel buildPanel(final SwingEntityModel model) {
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
          final SwingEntityModel detailModel = model.getDetailModel(detailPanelBuilder.getEntityType());
          final EntityPanel detailPanel = detailPanelBuilder.buildPanel(detailModel);
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
  public final EntityEditPanel buildEditPanel(final EntityConnectionProvider connectionProvider) {
    return initializeEditPanel(modelBuilder.buildEditModel(connectionProvider));
  }

  /**
   * Creates an EntityTablePanel
   * @param connectionProvider the connection provider
   * @return an EntityTablePanel based on this provider
   */
  public final EntityTablePanel buildTablePanel(final EntityConnectionProvider connectionProvider) {
    return initializeTablePanel(modelBuilder.buildTableModel(connectionProvider));
  }

  /**
   * Creates a new Action which shows the edit panel provided by this panel builder and if an insert is performed
   * adds the new entity to the {@code comboBox} and selects it.
   * @param comboBox the combo box in which to select the new entity, if created
   * @return the Action
   */
  public final Action createEditPanelAction(final EntityComboBox comboBox) {
    return new InsertEntityAction(comboBox);
  }

  /**
   * Creates a new Action which shows the edit panel provided by this panel builder and if an insert is performed
   * selects the new entity in the {@code lookupField}.
   * @param lookupField the lookup field in which to select the new entity, if created
   * @return the Action
   */
  public final Action createEditPanelAction(final EntityLookupField lookupField) {
    return new InsertEntityAction(lookupField);
  }

  /**
   * Creates a new Action which shows the edit panel provided by this panel builder and if an insert is performed
   * {@code insertListener} is notified.
   * @param component this component used as dialog parent, receives the focus after insert
   * @param connectionProvider the connection provider
   * @param insertListener the listener notified when insert has been performed
   * @return the Action
   */
  public final Action createEditPanelAction(final JComponent component, final EntityConnectionProvider connectionProvider,
                                      final EventDataListener<List<Entity>> insertListener) {
    return new InsertEntityAction(component, connectionProvider, insertListener);
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
      if (!tableModel.getEntityType().equals(getEntityType())) {
        throw new IllegalArgumentException("Entity type mismatch, tableModel: " + tableModel.getEntityType() + ", required: " + getEntityType());
      }
      final EntityTablePanel tablePanel = findTableModelConstructor(getTablePanelClass()).newInstance(tableModel);
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
      this(comboBox, comboBox.getModel().getConnectionProvider(), inserted -> {
        final EntityComboBoxModel comboBoxModel = comboBox.getModel();
        final Entity item = inserted.get(0);
        comboBoxModel.addItem(item);
        comboBoxModel.setSelectedItem(item);
      });
    }

    private InsertEntityAction(final EntityLookupField lookupField) {
      this(lookupField, lookupField.getModel().getConnectionProvider(), inserted ->
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
