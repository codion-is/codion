/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.event.Event;
import is.codion.common.i18n.Messages;
import is.codion.common.property.PropertyValue;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.Utilities.parentOfType;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.button.ToggleButtonType.CHECKBOX;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.EntityPanel.Direction.*;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.*;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.SwingConstants.HORIZONTAL;
import static javax.swing.SwingConstants.VERTICAL;

/**
 * A panel representing an Entity via a EntityModel, which facilitates browsing and editing of records.
 * <pre>
 *   EntityType entityType = ...;
 *   EntityConnectionProvider connectionProvider = ...;
 *   SwingEntityModel entityModel = new SwingEntityModel(entityType, connectionProvider);
 *   EntityPanel entityPanel = new EntityPanel(entityModel);
 *   entityPanel.initialize();
 *   JFrame frame = new JFrame();
 *   frame.add(entityPanel);
 *   frame.pack();
 *   frame.setVisible(true);
 * </pre>
 */
public class EntityPanel extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityPanel.class.getName());

  /**
   * Indicates whether keyboard navigation will be enabled<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> USE_KEYBOARD_NAVIGATION =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.useKeyboardNavigation", true);

  /**
   * Indicates whether entity edit panel dialogs should be closed on escape<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> DISPOSE_EDIT_DIALOG_ON_ESCAPE =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.disposeEditDialogOnEscape", true);

  /**
   * Specifies whether a control for toggling the edit panel is available to the user<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_TOGGLE_EDIT_PANEL_CONTROL =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.includeToggleEditPanelControl", true);

  /**
   * Specifies whether actions to hide detail panels or show them in a dialog are available to the user<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_DETAIL_PANEL_CONTROLS =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.includeDetailPanelControls", true);

  /**
   * Specifies whether the edit controls (Save, update, delete, clear, refresh) should be on a toolbar instead of a button panel<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> TOOLBAR_CONTROLS =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.toolbarControls", false);

  /**
   * Specifies whether detail and edit panels should be displayed in a frame instead of the default dialog<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> USE_FRAME_PANEL_DISPLAY =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.useFramePanelDisplay", false);

  /**
   * Specifies where the control panel should be placed in a BorderLayout<br>
   * Value type: Boolean<br>
   * Default value: {@link BorderLayout#EAST}
   * @see #TOOLBAR_CONTROLS
   */
  public static final PropertyValue<String> CONTROL_PANEL_CONSTRAINTS =
          Configuration.stringValue("is.codion.swing.framework.ui.EntityPanel.controlPanelConstraints", BorderLayout.EAST);

  /**
   * Specifies where the control toolbar should be placed in a BorderLayout<br>
   * Value type: Boolean<br>
   * Default value: BorderLayout.WEST
   * @see #TOOLBAR_CONTROLS
   */
  public static final PropertyValue<String> CONTROL_TOOLBAR_CONSTRAINTS =
          Configuration.stringValue("is.codion.swing.framework.ui.EntityPanel.controlToolbarConstraints", BorderLayout.WEST);

  /**
   * The possible states of a detail or edit panel.
   */
  public enum PanelState {
    WINDOW, EMBEDDED, HIDDEN
  }

  /**
   * The navigation and resizing directions.
   */
  public enum Direction {
    UP, DOWN, RIGHT, LEFT
  }

  private final SwingEntityModel entityModel;
  private final List<EntityPanel> detailPanels = new ArrayList<>();
  private final EntityEditPanel editPanel;
  private final EntityTablePanel tablePanel;
  private final JPanel editControlPanel = new JPanel(borderLayout());
  private final JPanel editControlTablePanel = new JPanel(borderLayout());
  private final Event<EntityPanel> activateEvent = Event.event();
  private final PanelLayout panelLayout;
  private final DetailController detailController;
  private final Value<String> caption;
  private final Value<PanelState> editPanelState = Value.value(EMBEDDED, EMBEDDED);

  private String description;
  private EntityPanel parentPanel;
  private EntityPanel previousSiblingPanel;
  private EntityPanel nextSiblingPanel;
  private boolean toolbarControls = TOOLBAR_CONTROLS.get();
  private String controlComponentConstraints = TOOLBAR_CONTROLS.get() ?
          CONTROL_TOOLBAR_CONSTRAINTS.get() : CONTROL_PANEL_CONSTRAINTS.get();
  private boolean includeControls = true;
  private boolean includeToggleEditPanelControl = INCLUDE_TOGGLE_EDIT_PANEL_CONTROL.get();
  private boolean disposeEditDialogOnEscape = DISPOSE_EDIT_DIALOG_ON_ESCAPE.get();
  private boolean useKeyboardNavigation = USE_KEYBOARD_NAVIGATION.get();
  private boolean initialized = false;

  static {
    if (EntityEditPanel.USE_FOCUS_ACTIVATION.get()) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", new FocusActivationListener());
    }
  }

  /**
   * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
   * @param entityModel the EntityModel
   */
  public EntityPanel(SwingEntityModel entityModel) {
    this(requireNonNull(entityModel), TabbedPanelLayout.builder().build());
  }

  /**
   * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
   * @param entityModel the EntityModel
   * @param panelLayout the detail panel layout
   */
  public EntityPanel(SwingEntityModel entityModel, PanelLayout panelLayout) {
    this(requireNonNull(entityModel), null, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.tableModel()) : null, panelLayout);
  }

  /**
   * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   */
  public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel) {
    this(requireNonNull(entityModel), editPanel, TabbedPanelLayout.builder().build());
  }

  /**
   * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   * @param panelLayout the detail panel layout
   */
  public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel, PanelLayout panelLayout) {
    this(requireNonNull(entityModel), editPanel, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.tableModel()) : null, panelLayout);
  }

  /**
   * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
   * @param entityModel the EntityModel
   * @param tablePanel the table panel
   */
  public EntityPanel(SwingEntityModel entityModel, EntityTablePanel tablePanel) {
    this(entityModel, tablePanel, TabbedPanelLayout.builder().build());
  }

  /**
   * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
   * @param entityModel the EntityModel
   * @param tablePanel the table panel
   * @param panelLayout the detail panel layout
   */
  public EntityPanel(SwingEntityModel entityModel, EntityTablePanel tablePanel, PanelLayout panelLayout) {
    this(entityModel, null, tablePanel, panelLayout);
  }

  /**
   * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   * @param tablePanel the table panel
   */
  public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel, EntityTablePanel tablePanel) {
    this(entityModel, editPanel, tablePanel, TabbedPanelLayout.builder().build());
  }

  /**
   * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   * @param tablePanel the table panel
   * @param panelLayout the detail panel layout
   */
  public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel, EntityTablePanel tablePanel,
                     PanelLayout panelLayout) {
    requireNonNull(entityModel, "entityModel");
    setFocusCycleRoot(true);
    this.entityModel = entityModel;
    String defaultCaption = entityModel.editModel().entityDefinition().caption();
    this.caption = Value.value(defaultCaption, defaultCaption);
    this.description = entityModel.editModel().entityDefinition().description();
    this.editPanel = editPanel;
    this.tablePanel = tablePanel;
    this.panelLayout = requireNonNull(panelLayout);
    this.detailController = panelLayout.detailController().orElse(new NullDetailController());
    editPanelState.addListener(this::updateEditPanelState);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(editControlPanel, editControlTablePanel, tablePanel, editPanel);
    if (detailPanels != null) {
      Utilities.updateUI(detailPanels);
    }
    if (panelLayout != null) {
      panelLayout.updateUI();
    }
  }

  /**
   * @param <T> the model type
   * @return the EntityModel
   */
  public final <T extends SwingEntityModel> T model() {
    return (T) entityModel;
  }

  /**
   * @param <T> the edit model type
   * @return the EntityEditModel
   */
  public final <T extends SwingEntityEditModel> T editModel() {
    return entityModel.editModel();
  }

  /**
   * @param <T> the table model type
   * @return the EntityTableModel
   * @throws IllegalStateException in case no table model is available
   */
  public final <T extends SwingEntityTableModel> T tableModel() {
    return entityModel.tableModel();
  }

  /**
   * @return the parent panel or an empty Optional in case of a root panel
   */
  public final Optional<EntityPanel> parentPanel() {
    return Optional.ofNullable(parentPanel);
  }

  /**
   * @return the detail panel controller
   * @param <T> the detail panel controller type
   */
  public final <T extends DetailController> T detailController() {
    return (T) detailController;
  }

  /**
   * @return true if the edit controls should be on a toolbar instead of a button panel
   */
  public final boolean isToolbarControls() {
    return toolbarControls;
  }

  /**
   * @param toolbarControls true if the edit controls should be on a toolbar instead of a button panel
   * @throws IllegalStateException if the panel has been initialized
   * @see #TOOLBAR_CONTROLS
   */
  public final void setToolbarControls(boolean toolbarControls) {
    checkIfInitialized();
    this.toolbarControls = toolbarControls;
  }

  /**
   * @return the controls component layout constraints (BorderLayout constraints)
   */
  public final String getControlComponentConstraints() {
    return controlComponentConstraints;
  }

  /**
   * Sets the layout constraints to use for the control panel
   * <pre>
   * The default layout is as follows (BorderLayout.WEST):
   * __________________________________
   * |   edit panel           |control|
   * |  (EntityEditPanel)     | panel | } editControlPanel
   * |________________________|_______|
   *
   * With (BorderLayout.SOUTH):
   * __________________________
   * |         edit           |
   * |        panel           |
   * |________________________| } editControlPanel
   * |     control panel      |
   * |________________________|
   *
   * etc.
   * </pre>
   * @param controlComponentConstraints the controls component layout constraints (BorderLayout constraints)
   * @throws IllegalStateException if the panel has been initialized
   * @throws IllegalArgumentException in case the given constraint is not one of BorderLayout.SOUTH, NORTH, EAST or WEST
   */
  public final void setControlComponentConstraints(String controlComponentConstraints) {
    checkIfInitialized();
    switch (requireNonNull(controlComponentConstraints)) {
      case BorderLayout.SOUTH:
      case BorderLayout.NORTH:
      case BorderLayout.EAST:
      case BorderLayout.WEST:
        break;
      default:
        throw new IllegalArgumentException("Control component constraints must be one of BorderLayout.SOUTH, NORTH, EAST or WEST");
    }
    this.controlComponentConstraints = controlComponentConstraints;
  }

  /**
   * @param detailPanels the detail panels
   */
  public final void addDetailPanels(EntityPanel... detailPanels) {
    requireNonNull(detailPanels, "detailPanels");
    for (EntityPanel detailPanel : detailPanels) {
      addDetailPanel(detailPanel);
    }
  }

  /**
   * Adds the given detail panel and sets this panel as the parent panel of the given detail panel.
   * @param detailPanel the detail panel to add
   * @throws IllegalStateException if the panel has been initialized or if it already contains the given detail panel
   */
  public final void addDetailPanel(EntityPanel detailPanel) {
    checkIfInitialized();
    if (detailPanels.contains(requireNonNull(detailPanel))) {
      throw new IllegalStateException("Panel already contains detail panel: " + detailPanel);
    }
    addEntityPanelAndLinkSiblings(detailPanel, detailPanels);
    detailPanel.setParentPanel(this);
    detailPanel.addActivateListener(detailController::select);
  }

  /**
   * Initializes this EntityPanel, in case of some specific initialization code you can override the
   * {@link #initializeUI()} method and add your code there. Calling this method a second time has no effect.
   * @param <T> the entity panel type
   * @return this EntityPanel instance
   */
  public final <T extends EntityPanel> T initialize() {
    if (!initialized) {
      try {
        initializeUI();
      }
      finally {
        initialized = true;
      }
    }

    return (T) this;
  }

  /**
   * @param <T> the edit panel type
   * @return the edit panel
   * @throws IllegalStateException in case no edit panel is avilable
   * @see #containsEditPanel()
   */
  public final <T extends EntityEditPanel> T editPanel() {
    if (editPanel == null) {
      throw new IllegalStateException("No edit panel available");
    }

    return (T) editPanel;
  }

  /**
   * @return true if this panel contains a edit panel.
   */
  public final boolean containsEditPanel() {
    return editPanel != null;
  }

  /**
   * @param <T> the table panel type
   * @return the table panel
   * @throws IllegalStateException in case no table panel is avilable
   * @see #containsTablePanel()
   */
  public final <T extends EntityTablePanel> T tablePanel() {
    if (tablePanel == null) {
      throw new IllegalStateException("No table panel available");
    }

    return (T) tablePanel;
  }

  /**
   * @return true if this panel contains a table panel.
   */
  public final boolean containsTablePanel() {
    return tablePanel != null;
  }

  /**
   * Returns the panel containing the edit panel and the controls component.
   * @return the edit control panel
   */
  public final JPanel editControlPanel() {
    return editControlPanel;
  }

  /**
   * @return the currently visible/active detail EntityPanels, if any
   */
  public final Collection<EntityPanel> activeDetailPanels() {
    Collection<SwingEntityModel> activeDetailModels = entityModel.activeDetailModels();

    return detailPanels.stream()
            .filter(detailPanel -> activeDetailModels.contains(detailPanel.entityModel))
            .collect(toList());
  }

  /**
   * Returns the detail panel for the given {@code entityType}, if one is available
   * @param <T> the entity panel type
   * @param entityType the entityType of the detail panel to retrieve
   * @return the detail panel of the given type
   * @throws IllegalArgumentException in case the panel was not found
   */
  public final <T extends EntityPanel> T detailPanel(EntityType entityType) {
    requireNonNull(entityType);
    for (EntityPanel detailPanel : detailPanels) {
      if (detailPanel.entityModel.entityType().equals(entityType)) {
        return (T) detailPanel;
      }
    }

    throw new IllegalArgumentException("Detail panel for entity: " + entityType + " not found in panel: " + getClass());
  }

  /**
   * Returns all detail panels.
   * @return the detail panels
   */
  public final Collection<EntityPanel> detailPanels() {
    return unmodifiableCollection(detailPanels);
  }

  /**
   * Returns true if this panel contains a detail panel for the given {@code entityType}
   * @param entityType the entityType
   * @return true if a detail panel for the given entityType is found
   */
  public final boolean containsDetailPanel(EntityType entityType) {
    requireNonNull(entityType);
    return detailPanels.stream()
            .anyMatch(detailPanel -> detailPanel.entityModel.entityType().equals(entityType));
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + caption.get();
  }

  /**
   * Setting this caption Value to null reverts back to the default entity caption.
   * @return a Value for the caption used when presenting this entity panel
   */
  public final Value<String> caption() {
    return caption;
  }

  /**
   * Sets the description text to use in f.ex. tool tips for tabbed panes
   * @param description the description
   */
  public final void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the description
   */
  public final String getDescription() {
    return description;
  }

  /**
   * @param listener notified before this panel is activated
   * @see #activate()
   */
  public final void addActivateListener(Consumer<EntityPanel> listener) {
    activateEvent.addDataListener(listener);
  }

  /**
   * Activates this panel, by initializing it, bringing its parent window to front and requesting initial focus.
   * It is up the panel or application layout to make sure this panel is visible before activation.
   * @see #addActivateListener(Consumer)
   */
  public final void activate() {
    activateEvent.accept(this);
    initialize();
    Window parentWindow = parentWindow(this);
    if (parentWindow != null) {
      parentWindow.toFront();
    }
    Window editPanelWindow = parentWindow(editControlPanel);
    if (editPanelWindow != null) {
      editPanelWindow.toFront();
    }
    requestInitialFocus();
  }

  /**
   * Displays the exception in a dialog, with the dialog owner as the current focus owner
   * or this panel if none is available.
   * @param exception the exception to display
   */
  public final void displayException(Throwable exception) {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusOwner == null) {
      focusOwner = EntityPanel.this;
    }
    Dialogs.displayExceptionDialog(exception, parentWindow(focusOwner));
  }

  /**
   * @return true if the edit panel control should be shown
   * @see EntityPanel#INCLUDE_TOGGLE_EDIT_PANEL_CONTROL
   */
  public final boolean isIncludeToggleEditPanelControl() {
    return includeToggleEditPanelControl;
  }

  /**
   * @param includeToggleEditPanelControl true if a control for toggling the edit panel should be included
   * @throws IllegalStateException if the panel has been initialized
   */
  public final void setIncludeToggleEditPanelControl(boolean includeToggleEditPanelControl) {
    checkIfInitialized();
    this.includeToggleEditPanelControl = includeToggleEditPanelControl;
  }

  /**
   * @return true if the edit and table panel controls should be included
   */
  public final boolean isIncludeControls() {
    return includeControls;
  }

  /**
   * @param includeControls true if the edit an table panel controls should be included
   * @throws IllegalStateException if the panel has been initialized
   */
  public final void setIncludeControls(boolean includeControls) {
    checkIfInitialized();
    this.includeControls = includeControls;
  }

  /**
   * @return true if the edit dialog is disposed of on ESC
   * @see EntityPanel#DISPOSE_EDIT_DIALOG_ON_ESCAPE
   */
  public final boolean isDisposeEditDialogOnEscape() {
    return disposeEditDialogOnEscape;
  }

  /**
   * @param disposeEditDialogOnEscape if true then the edit dialog is disposed of on ESC
   * @see EntityPanel#DISPOSE_EDIT_DIALOG_ON_ESCAPE
   */
  public final void setDisposeEditDialogOnEscape(boolean disposeEditDialogOnEscape) {
    this.disposeEditDialogOnEscape = disposeEditDialogOnEscape;
  }

  /**
   * @return true if keyboard navigation is enabled
   */
  public final boolean isUseKeyboardNavigation() {
    return useKeyboardNavigation;
  }

  /**
   * @param useKeyboardNavigation true if keyboard navigation should be enabled
   */
  public final void setUseKeyboardNavigation(boolean useKeyboardNavigation) {
    checkIfInitialized();
    this.useKeyboardNavigation = useKeyboardNavigation;
  }

  /**
   * @return the value controlling the edit panel state, either HIDDEN, EMBEDDED or WINDOW
   */
  public final Value<PanelState> editPanelState() {
    return editPanelState;
  }

  /**
   * Requests focus for this panel. If an edit panel is available and not hidden, the component
   * defined as the initialFocusComponent gets the input focus.
   * If no edit panel is available the table panel gets the focus, otherwise the first child
   * component of this EntityPanel is used.
   * @see EntityEditPanel#initialFocusComponent()
   */
  public final void requestInitialFocus() {
    if (editPanel != null && editPanel.isShowing()) {
      editPanel.requestInitialFocus();
    }
    else if (tablePanel != null) {
      tablePanel.table().requestFocus();
    }
    else if (getComponentCount() > 0) {
      getComponents()[0].requestFocus();
    }
    else {
      requestFocus();
    }
  }

  /**
   * Saves any user preferences for all entity panels and associated elements
   */
  public void savePreferences() {
    detailPanels.forEach(EntityPanel::savePreferences);
  }

  @Override
  public final void setFocusCycleRoot(boolean focusCycleRoot) {
    //override as final since it's called in constructor
    super.setFocusCycleRoot(focusCycleRoot);
  }

  /**
   * Instantiates a new EntityPanel.Builder
   * @param entityType the entity type to base this panel builder on
   * @return a panel builder
   */
  public static EntityPanel.Builder builder(EntityType entityType) {
    return new EntityPanelBuilder(SwingEntityModel.builder(entityType));
  }

  /**
   * Instantiates a new EntityPanel.Builder
   * @param modelBuilder the SwingEntityModel.Builder to base this panel builder on
   * @return a panel builder
   */
  public static EntityPanel.Builder builder(SwingEntityModel.Builder modelBuilder) {
    return new EntityPanelBuilder(modelBuilder);
  }

  /**
   * Instantiates a new EntityPanel.Builder
   * @param model the SwingEntityModel to base this panel builder on
   * @return a panel builder
   */
  public static EntityPanel.Builder builder(SwingEntityModel model) {
    return new EntityPanelBuilder(model);
  }

  //#############################################################################################
  // Begin - initialization methods
  //#############################################################################################

  /**
   * Initializes this EntityPanels UI.
   * @see #panelLayout()
   * @see #editControlPanel()
   * @see #editControlTablePanel()
   */
  protected void initializeUI() {
    setupToggleEditPanelControl();
    panelLayout().layout(this);
    if (containsTablePanel()) {
      initializeTablePanel();
    }
    if (containsEditPanel()) {
      initializeEditPanel();
      updateEditPanelState();
    }
    setupKeyboardActions();
  }

  /**
   * Creates a base panel containing the given edit panel.
   * The default layout is a {@link FlowLayout} with the alignment depending on the {@link #getControlComponentConstraints()}.
   * The resulting panel is added at {@link BorderLayout#CENTER} on the {@link #editControlPanel()}
   * @param editPanel the initialized edit panel
   * @return a base panel for the edit panel
   */
  protected JPanel createEditBasePanel(EntityEditPanel editPanel) {
    return panel(new FlowLayout(horizontalControlLayout() ? FlowLayout.CENTER : FlowLayout.LEADING, 0, 0))
            .add(editPanel)
            .build();
  }

  /**
   * Creates the component to place next to the edit panel, containing the available controls,
   * such as insert, update, delete, clear and refresh.
   * Only called if {@link #isIncludeControls()} returns true.
   * @param controls the controls to display on the component
   * @return the component containing the edit and table panel controls, null if no controls are available
   * @see EntityEditPanel#controls()
   * @see #createControls()
   * @see EntityPanel#TOOLBAR_CONTROLS
   * @see EntityPanel#CONTROL_PANEL_CONSTRAINTS
   * @see EntityPanel#CONTROL_TOOLBAR_CONSTRAINTS
   */
  protected JComponent createControlComponent(Controls controls) {
    if (requireNonNull(controls).empty()) {
      return null;
    }

    return isToolbarControls() ? createControlToolbar(controls) : createControlPanel(controls);
  }

  /**
   * Creates the {@link Controls} instance on which to base the controls component.
   * By default all controls from {@link EntityEditPanel#controls} are included and if a
   * table panel is available a table refresh controls is included as well.
   * @return the control component controls, an empty {@link Controls} instance in case of no controls.
   * @see #createControlComponent(Controls)
   */
  protected Controls createControls() {
    Controls controls = Controls.controls();
    if (containsEditPanel()) {
      controls.addAll(editPanel().controls());
    }
    if (containsTablePanel()) {
      controls.add(createRefreshControl());
    }

    return controls;
  }

  /**
   * Returns the base panel containing the edit and table panels (north, center).
   * @return the edit and table base panel
   */
  protected final JPanel editControlTablePanel() {
    return editControlTablePanel;
  }

  /**
   * Sets up the keyboard navigation actions.
   * CTRL-T transfers focus to the table in case one is available,
   * CTR-E transfers focus to the edit panel in case one is available,
   * CTR-S opens a select search condition panel dialog, in case one is available,
   * CTR-I opens a select input field dialog and
   * CTR-F selects the table search field
   */
  protected final void setupKeyboardActions() {
    if (containsTablePanel()) {
      tablePanel.control(EntityTablePanel.ControlCode.REQUEST_TABLE_FOCUS).ifPresent(control ->
              KeyEvents.builder(VK_T)
                      .modifiers(CTRL_DOWN_MASK)
                      .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                      .action(control)
                      .enable(this));
      tablePanel.control(EntityTablePanel.ControlCode.TOGGLE_CONDITION_PANEL).ifPresent(control ->
              KeyEvents.builder(VK_S)
                      .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
                      .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                      .action(control)
                      .enable(this));
      tablePanel.control(EntityTablePanel.ControlCode.SELECT_CONDITION_PANEL).ifPresent(control ->
              KeyEvents.builder(VK_S)
                      .modifiers(CTRL_DOWN_MASK)
                      .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                      .action(control)
                      .enable(this));
      tablePanel.control(EntityTablePanel.ControlCode.TOGGLE_FILTER_PANEL).ifPresent(control ->
              KeyEvents.builder(VK_F)
                      .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
                      .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                      .action(control)
                      .enable(this));
      tablePanel.control(EntityTablePanel.ControlCode.SELECT_FILTER_PANEL).ifPresent(control ->
              KeyEvents.builder(VK_F)
                      .modifiers(CTRL_DOWN_MASK | SHIFT_DOWN_MASK)
                      .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                      .action(control)
                      .enable(this));
      KeyEvents.builder(VK_F)
              .modifiers(CTRL_DOWN_MASK)
              .action(Control.control(tablePanel.table().searchField()::requestFocusInWindow))
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .enable(this);
      if (containsEditPanel()) {
        tablePanel.control(EntityTablePanel.ControlCode.REQUEST_TABLE_FOCUS).ifPresent(control ->
                KeyEvents.builder(VK_T)
                        .modifiers(CTRL_DOWN_MASK)
                        .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                        .action(control)
                        .enable(editControlPanel));
        tablePanel.control(EntityTablePanel.ControlCode.TOGGLE_CONDITION_PANEL).ifPresent(control ->
                KeyEvents.builder(VK_S)
                        .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
                        .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                        .action(control)
                        .enable(editControlPanel));
        tablePanel.control(EntityTablePanel.ControlCode.SELECT_CONDITION_PANEL).ifPresent(control ->
                KeyEvents.builder(VK_S)
                        .modifiers(CTRL_DOWN_MASK)
                        .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                        .action(control)
                        .enable(editControlPanel));
        tablePanel.control(EntityTablePanel.ControlCode.TOGGLE_FILTER_PANEL).ifPresent(control ->
                KeyEvents.builder(VK_F)
                        .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
                        .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                        .action(control)
                        .enable(editControlPanel));
        tablePanel.control(EntityTablePanel.ControlCode.SELECT_FILTER_PANEL).ifPresent(control ->
                KeyEvents.builder(VK_F)
                        .modifiers(CTRL_DOWN_MASK | SHIFT_DOWN_MASK)
                        .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                        .action(control)
                        .enable(editControlPanel));
      }
    }
    if (containsEditPanel()) {
      Control selectEditPanel = Control.control(this::selectEditPanel);
      Control selectInputComponent = Control.control(this::selectInputComponent);
      KeyEvents.builder(VK_E)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectEditPanel)
              .enable(this);
      KeyEvents.builder(VK_I)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectInputComponent)
              .enable(this);
      KeyEvents.builder(VK_I)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectInputComponent)
              .enable(editControlPanel);
      ToggleEditPanel toggleEditPanel = new ToggleEditPanel(this);
      KeyEvents.builder(VK_E)
              .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(toggleEditPanel)
              .enable(this);
      KeyEvents.builder(VK_E)
              .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(toggleEditPanel)
              .enable(editControlPanel);
    }
    if (useKeyboardNavigation) {
      setupNavigation();
    }
  }

  protected final void setupNavigation() {
    KeyEvents.builder(VK_UP)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new Navigate(UP))
            .enable(this);
    KeyEvents.builder(VK_DOWN)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new Navigate(DOWN))
            .enable(this);
    KeyEvents.builder(VK_RIGHT)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new Navigate(RIGHT))
            .enable(this);
    KeyEvents.builder(VK_LEFT)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new Navigate(LEFT))
            .enable(this);
    if (containsEditPanel()) {
      KeyEvents.builder(VK_UP)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new Navigate(UP))
              .enable(editControlPanel);
      KeyEvents.builder(VK_DOWN)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new Navigate(DOWN))
              .enable(editControlPanel);
      KeyEvents.builder(VK_RIGHT)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new Navigate(RIGHT))
              .enable(editControlPanel);
      KeyEvents.builder(VK_LEFT)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new Navigate(LEFT))
              .enable(editControlPanel);
    }
  }

  protected final void initializeEditPanel() {
    editPanel.initialize();
    editControlPanel.setMinimumSize(new Dimension(0, 0));
    int gap = Layouts.GAP.get();
    editControlPanel.setBorder(createEmptyBorder(gap, 0, gap, 0));
    editControlPanel.add(createEditBasePanel(editPanel), BorderLayout.CENTER);
    if (includeControls) {
      JComponent controlComponent = createControlComponent(createControls());
      if (controlComponent != null) {
        editControlPanel.add(controlComponent, controlComponentConstraints);
      }
    }
  }

  protected final void initializeTablePanel() {
    editControlTablePanel.add(tablePanel, BorderLayout.CENTER);
    if (tablePanel.table().doubleClickAction().get() == null) {
      tablePanel.table().doubleClickAction().set(Control.control(new ShowHiddenEditPanel()));
    }
    tablePanel.initialize();
    tablePanel.setMinimumSize(new Dimension(0, 0));
  }

  final void setParentPanel(EntityPanel parentPanel) {
    if (this.parentPanel != null) {
      throw new IllegalStateException("Parent panel has already been set for " + this);
    }
    this.parentPanel = requireNonNull(parentPanel);
  }

  final void setPreviousSiblingPanel(EntityPanel previousSiblingPanel) {
    this.previousSiblingPanel = requireNonNull(previousSiblingPanel);
  }

  final void setNextSiblingPanel(EntityPanel nextSiblingPanel) {
    this.nextSiblingPanel = requireNonNull(nextSiblingPanel);
  }

  final <T extends PanelLayout> T panelLayout() {
    return (T) panelLayout;
  }

  static void addEntityPanelAndLinkSiblings(EntityPanel detailPanel, List<EntityPanel> entityPanels) {
    if (!entityPanels.isEmpty()) {
      EntityPanel leftSibling = entityPanels.get(entityPanels.size() - 1);
      detailPanel.setPreviousSiblingPanel(leftSibling);
      leftSibling.setNextSiblingPanel(detailPanel);
      EntityPanel firstPanel = entityPanels.get(0);
      detailPanel.setNextSiblingPanel(firstPanel);
      firstPanel.setPreviousSiblingPanel(detailPanel);
    }
    entityPanels.add(detailPanel);
  }

  private JToolBar createControlToolbar(Controls controls) {
    return toolBar(controls)
            .orientation(horizontalControlLayout() ? HORIZONTAL : VERTICAL)
            .build();
  }

  private JPanel createControlPanel(Controls controls) {
    if (horizontalControlLayout()) {
      return flowLayoutPanel(FlowLayout.CENTER)
              .add(buttonPanel(controls)
                      .toggleButtonType(CHECKBOX)
                      .build())
              .build();
    }

    return borderLayoutPanel()
            .northComponent(buttonPanel(controls)
                    .orientation(VERTICAL)
                    .buttonBuilder(ButtonBuilder.builder()
                            .horizontalAlignment(SwingConstants.LEADING))
                    .toggleButtonType(CHECKBOX)
                    .build())
            .build();
  }

  private boolean horizontalControlLayout() {
    return controlComponentConstraints.equals(BorderLayout.SOUTH) || controlComponentConstraints.equals(BorderLayout.NORTH);
  }

  private void setupToggleEditPanelControl() {
    if (containsTablePanel() && containsEditPanel() && includeToggleEditPanelControl ) {
      tablePanel.addToolBarControls(Controls.builder()
              .control(createToggleEditPanelControl())
              .build());
    }
  }

  private Control createToggleEditPanelControl() {
    return Control.builder(this::toggleEditPanelState)
            .smallIcon(FrameworkIcons.instance().editPanel())
            .description(MESSAGES.getString("toggle_edit"))
            .build();
  }

  private Control createRefreshControl() {
    return Control.builder(tableModel()::refresh)
            .name(Messages.refresh())
            .enabled(editPanel == null ? null : editPanel.active())
            .description(Messages.refreshTip() + " (ALT-" + Messages.refreshMnemonic() + ")")
            .mnemonic(Messages.refreshMnemonic())
            .smallIcon(FrameworkIcons.instance().refresh())
            .build();
  }

  //#############################################################################################
  // End - initialization methods
  //#############################################################################################

  private void selectEditPanel() {
    if (editPanelState.equalTo(HIDDEN)) {
      editPanelState.set(EMBEDDED);
    }
    editPanel().requestInitialFocus();
  }

  private void selectInputComponent() {
    if (editPanelState.equalTo(HIDDEN)) {
      editPanelState.set(EMBEDDED);
    }
    editPanel().selectInputComponent();
  }

  private void updateEditPanelState() {
    if (editPanelState.notEqualTo(WINDOW)) {
      Window editPanelWindow = parentWindow(editControlPanel);
      if (editPanelWindow != null) {
        editPanelWindow.dispose();
      }
    }
    if (editPanelState.equalTo(EMBEDDED)) {
      editControlTablePanel.add(editControlPanel, BorderLayout.NORTH);
    }
    else if (editPanelState.equalTo(HIDDEN)) {
      editControlTablePanel.remove(editControlPanel);
    }
    else {
      createEditWindow().setVisible(true);
    }
    requestInitialFocus();

    revalidate();
  }

  private void toggleEditPanelState() {
    if (editPanelState.equalTo(WINDOW)) {
      editPanelState.set(HIDDEN);
    }
    else if (editPanelState.equalTo(EMBEDDED)) {
      editPanelState.set(WINDOW);
    }
    else {
      editPanelState.set(EMBEDDED);
    }
  }

  private Window createEditWindow() {
    int gap = Layouts.GAP.get();
    JPanel basePanel = Components.borderLayoutPanel()
            .border(createEmptyBorder(gap, gap, 0, gap))
            .centerComponent(editControlPanel)
            .build();
    if (USE_FRAME_PANEL_DISPLAY.get()) {
      return Windows.frame(basePanel)
              .locationRelativeTo(this)
              .title(caption.get())
              .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
              .onClosed(windowEvent -> editPanelState.set(HIDDEN))
              .build();
    }

    return Dialogs.componentDialog(basePanel)
            .owner(this)
            .title(caption.get())
            .modal(false)
            .disposeOnEscape(disposeEditDialogOnEscape)
            .onClosed(windowEvent -> editPanelState.set(HIDDEN))
            .build();
  }

  private void checkIfInitialized() {
    if (initialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
  }

  private final class ShowHiddenEditPanel implements Control.Command {

    @Override
    public void perform() {
      if (containsEditPanel() && editPanelState.equalTo(HIDDEN)) {
        editPanelState.set(WINDOW);
      }
    }
  }

  private final class Navigate extends AbstractAction {

    private final Direction direction;

    private Navigate(Direction direction) {
      super("Navigate " + direction);
      this.direction = direction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      switch (direction) {
        case LEFT:
          if (previousSiblingPanel != null) {
            previousSiblingPanel.activate();
          }
          break;
        case RIGHT:
          if (nextSiblingPanel != null) {
            nextSiblingPanel.activate();
          }
          break;
        case UP:
          if (parentPanel != null) {
            parentPanel.activate();
          }
          break;
        case DOWN:
          activeDetailPanel()
                  .ifPresent(EntityPanel::activate);
          break;
        default:
          throw new IllegalArgumentException("Unknown direction: " + direction);
      }
    }

    private Optional<EntityPanel> activeDetailPanel() {
      Collection<EntityPanel> activeDetailPanels = activeDetailPanels();
      if (!activeDetailPanels.isEmpty()) {
        return Optional.of(activeDetailPanels.iterator().next());
      }

      return Optional.empty();
    }
  }

  private static final class ToggleEditPanel extends AbstractAction {

    private final EntityPanel panel;

    private ToggleEditPanel(EntityPanel panel) {
      super("ToggleEditPanelState");
      this.panel = panel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      panel.toggleEditPanelState();
    }
  }

  private static class FocusActivationListener implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent changeEvent) {
      Component focusedComponent = (Component) changeEvent.getNewValue();
      EntityPanel entityPanelParent = parentOfType(EntityPanel.class, focusedComponent);
      if (entityPanelParent != null) {
        if (entityPanelParent.containsEditPanel()) {
          entityPanelParent.editPanel().active().set(true);
        }
      }
      else {
        EntityEditPanel editPanelParent = parentOfType(EntityEditPanel.class, focusedComponent);
        if (editPanelParent != null) {
          editPanelParent.active().set(true);
        }
      }
    }
  }

  /**
   * Handles the layout of a EntityPanel
   */
  public interface PanelLayout {

    /**
     * Updates the UI of all associated components.
     * Override to update the UI of components that may be hidden and
     * therefore not updated along with the component tree.
     */
    default void updateUI() {}

    /**
     * Lays out the panel and adds any layout or detail panel related controls to this panel
     * @param entityPanel the panel to lay out and configure
     */
    void layout(EntityPanel entityPanel);

    /**
     * @return the {@link DetailController} provided by this {@link PanelLayout}
     * @param <T> the detail panel controller type
     */
    default <T extends DetailController> Optional<T> detailController() {
      return Optional.empty();
    }
  }

  /**
   * Selects an entity panel.
   */
  public interface Selector {

    /**
     * Selects the given entity panel. If the entityPanel
     * is not available, calling this method has no effect.
     * @param entityPanel the entity panel to select
     */
    void select(EntityPanel entityPanel);
  }

  /**
   * Controls the detail panels of a entity panel
   */
  public interface DetailController extends Selector {

    /**
     * Note that the detail panel state may be shared between detail panels,
     * as they may be displayed in a shared window.
     * @param detailPanel the detail panel
     * @return the value controlling the state of the given detail panel
     */
    Value<PanelState> panelState(EntityPanel detailPanel);
  }

  /**
   * A builder for {@link EntityPanel} instances.
   */
  public interface Builder {

    /**
     * @return the entityType
     */
    EntityType entityType();

    /**
     * @param caption the panel caption
     * @return this builder instance
     */
    Builder caption(String caption);

    /**
     * @return the caption, an empty Optional if none has been set
     */
    Optional<String> caption();

    /**
     * Adds the given detail panel builder to this panel builder, if it hasn't been previously added
     * @param panelBuilder the detail panel provider
     * @return this builder instance
     */
    Builder detailPanelBuilder(EntityPanel.Builder panelBuilder);

    /**
     * Default true.
     * @param refreshWhenInitialized if true then the table model this panel is based on
     * will be refreshed when the panel is initialized
     * @return this builder instance
     */
    Builder refreshWhenInitialized(boolean refreshWhenInitialized);

    /**
     * @param conditionPanelVisible if true then the table condition panel is made visible when the panel is initialized
     * @return this builder instance
     */
    Builder conditionPanelVisible(boolean conditionPanelVisible);

    /**
     * @param filterPanelVisible if true then the table filter panel is made visible when the panel is initialized
     * @return this builder instance
     */
    Builder filterPanelVisible(boolean filterPanelVisible);

    /**
     * @param panelLayout the panel layout to use
     * @return this builder instane
     */
    Builder panelLayout(PanelLayout panelLayout);

    /**
     * @param preferredSize the preferred panel size
     * @return this builder instance
     */
    Builder preferredSize(Dimension preferredSize);

    /**
     * Note that setting the EntityPanel class overrides any table panel or edit panel classes that have been set.
     * @param panelClass the EntityPanel class to use when providing this panel
     * @return this builder instance
     */
    Builder panelClass(Class<? extends EntityPanel> panelClass);

    /**
     * @param editPanelClass the EntityEditPanel class to use when providing this panel
     * @return this builder instance
     */
    Builder editPanelClass(Class<? extends EntityEditPanel> editPanelClass);

    /**
     * @param tablePanelClass the EntityTablePanel class to use when providing this panel
     * @return this builder instance
     */
    Builder tablePanelClass(Class<? extends EntityTablePanel> tablePanelClass);

    /**
     * @param onBuildPanel called after the entity panel has been built
     * @return this builder instance
     */
    Builder onBuildPanel(Consumer<EntityPanel> onBuildPanel);

    /**
     * @param onBuildEditPanel called after the edit panel has been built
     * @return this builder instance
     */
    Builder onBuildEditPanel(Consumer<EntityEditPanel> onBuildEditPanel);

    /**
     * @param onBuildTablePanel called after the table panel has been built
     * @return this builder instance
     */
    Builder onBuildTablePanel(Consumer<EntityTablePanel> onBuildTablePanel);

    /**
     * Creates an EntityPanel based on this builder,
     * assuming a EntityModel is available.
     * @return an EntityPanel based on this builder
     * @throws IllegalStateException in case no {@link SwingEntityModel} has been set
     */
    EntityPanel buildPanel();

    /**
     * Creates an EntityPanel based on this builder
     * @param connectionProvider the connection provider
     * @return an EntityPanel based on this builder
     * @throws IllegalStateException in case no {@link SwingEntityModel.Builder} has been set
     */
    EntityPanel buildPanel(EntityConnectionProvider connectionProvider);

    /**
     * Creates an EntityPanel based on this builder
     * @param model the EntityModel to base this panel on
     * @return an EntityPanel based on this builder
     */
    EntityPanel buildPanel(SwingEntityModel model);

    /**
     * Creates an EntityEditPanel
     * @param connectionProvider the connection provider
     * @return an EntityEditPanel based on this provider
     */
    EntityEditPanel buildEditPanel(EntityConnectionProvider connectionProvider);

    /**
     * Creates an EntityTablePanel
     * @param connectionProvider the connection provider
     * @return an EntityTablePanel based on this provider
     */
    EntityTablePanel buildTablePanel(EntityConnectionProvider connectionProvider);
  }

  private static final class NullDetailController implements DetailController {

    private final Value<PanelState> panelState = Value.value(HIDDEN);

    private NullDetailController() {
      panelState.addValidator(value -> {
        if (value != HIDDEN) {
          throw new IllegalArgumentException("No detail controller available, can not set the detail panel state");
        }
      });
    }

    @Override
    public void select(EntityPanel entityPanel) {}

    @Override
    public Value<PanelState> panelState(EntityPanel detailPanel) {
      requireNonNull(detailPanel);

      return panelState;
    }
  }
}
