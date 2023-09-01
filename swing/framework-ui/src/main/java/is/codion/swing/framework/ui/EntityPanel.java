/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.event.Event;
import is.codion.common.property.PropertyValue;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.border.Borders;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
public class EntityPanel extends JPanel implements EntityPanelParent {

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

  private static final String DETAIL_TABLES = "detail_tables";
  private static final int RESIZE_AMOUNT = 30;
  private static final double DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT = 0.5;
  private static final int DETAIL_WINDOW_OFFSET = 38;//titlebar height
  private static final double DETAIL_WINDOW_SIZE_RATIO = 0.66;

  /**
   * The EntityModel instance used by this EntityPanel
   */
  private final SwingEntityModel entityModel;

  /**
   * A List containing the detail panels, if any
   */
  private final List<EntityPanel> detailEntityPanels = new ArrayList<>();

  /**
   * The EntityEditPanel instance
   */
  private final EntityEditPanel editPanel;

  /**
   * The EntityTablePanel instance
   */
  private final EntityTablePanel tablePanel;

  /**
   * The base panel containing the edit and control panels
   */
  private final JPanel editControlPanel = new JPanel(borderLayout());

  /**
   * The base panel containing the edit, control and table panels
   */
  private final JPanel editControlTablePanel = new JPanel(borderLayout());

  /**
   * Fired before this panel is activated.
   */
  private final Event<EntityPanel> beforeActivateEvent = Event.event();

  /**
   * The caption to use when presenting this entity panel
   */
  private String caption;

  /**
   * The description to display for this entity panel
   */
  private String description;

  /**
   * The horizontal split pane, which is used in case this entity panel has detail panels.
   * It splits the lower section of this EntityPanel into the EntityTablePanel
   * on the left, and the detail panels on the right.
   */
  private JSplitPane tableDetailSplitPane;

  /**
   * The parent panel, if any, so that detail panels can refer to their parents
   */
  private EntityPanel parentPanel;

  /**
   * The previous sibling panel, if any
   */
  private EntityPanel previousSiblingPanel;

  /**
   * The next sibling panel, if any
   */
  private EntityPanel nextSiblingPanel;

  /**
   * A tab pane for the detail panels, if any
   */
  private JTabbedPane detailPanelTabbedPane;

  /**
   * The window used when detail panels are undocked
   */
  private Window detailPanelWindow;

  /**
   * The window used when the edit panel is undocked
   */
  private Window editPanelWindow;

  /**
   * Specifies whether the edit controls buttons are on a toolbar instead of a button panel
   */
  private boolean toolbarControls = TOOLBAR_CONTROLS.get();

  /**
   * indicates where the control panel should be placed in a BorderLayout
   */
  private String controlPanelConstraints = TOOLBAR_CONTROLS.get() ? CONTROL_TOOLBAR_CONSTRAINTS.get() : CONTROL_PANEL_CONSTRAINTS.get();

  /**
   * Holds the current state of the edit panel (HIDDEN, EMBEDDED or WINDOW)
   */
  private PanelState editPanelState = EMBEDDED;

  /**
   * Holds the current state of the detail panels (HIDDEN, EMBEDDED or WINDOW)
   */
  private PanelState detailPanelState = EMBEDDED;

  /**
   * if true then the edit control panel should be included
   */
  private boolean includeControlPanel = true;

  /**
   * if true and detail panels are available then the detail panel tab pane should be included
   */
  private boolean includeDetailTabPane = true;

  /**
   * if true and detail panels are available the controls to hide and show detail panels are included
   */
  private boolean includeDetailPanelControls = INCLUDE_DETAIL_PANEL_CONTROLS.get();

  /**
   * if true and an edit panel is available the actions to toggle it is included
   */
  private boolean includeToggleEditPanelControl = INCLUDE_TOGGLE_EDIT_PANEL_CONTROL.get();

  /**
   * if true then the ESC key disposes the edit dialog
   */
  private boolean disposeEditDialogOnEscape = DISPOSE_EDIT_DIALOG_ON_ESCAPE.get();

  /**
   * if true then keyboard navigation is enabled
   */
  private boolean useKeyboardNavigation = USE_KEYBOARD_NAVIGATION.get();

  /**
   * True after {@link #initialize()} has been called
   */
  private boolean initialized = false;

  private double detailSplitPanelResizeWeight = DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT;

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
    this(requireNonNull(entityModel), null, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.tableModel()) : null);
  }

  /**
   * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   */
  public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel) {
    this(requireNonNull(entityModel), editPanel, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.tableModel()) : null);
  }

  /**
   * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
   * @param entityModel the EntityModel
   * @param tablePanel the table panel
   */
  public EntityPanel(SwingEntityModel entityModel, EntityTablePanel tablePanel) {
    this(entityModel, null, tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   * @param tablePanel the table panel
   */
  public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel, EntityTablePanel tablePanel) {
    requireNonNull(entityModel, "entityModel");
    setFocusCycleRoot(true);
    this.entityModel = entityModel;
    this.caption = entityModel.editModel().entityDefinition().caption();
    this.description = entityModel.editModel().entityDefinition().description();
    this.editPanel = editPanel;
    this.tablePanel = tablePanel;
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(editControlPanel, editControlTablePanel, tableDetailSplitPane, tablePanel, editPanel, detailPanelTabbedPane);
    if (detailEntityPanels != null) {
      Utilities.updateUI(detailEntityPanels);
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
   * @return the control panel layout constraints (BorderLayout constraints)
   */
  public final String getControlPanelConstraints() {
    return controlPanelConstraints;
  }

  /**
   * Sets the layout constraints to use for the control panel
   * <pre>
   * The default layout is as follows (BorderLayout.WEST):
   * __________________________________
   * |   edit panel           |control|
   * |  (EntityEditPanel)     | panel | } edit control panel
   * |________________________|_______|
   *
   * With (BorderLayout.SOUTH):
   * __________________________
   * |         edit           |
   * |        panel           |
   * |________________________| } edit control panel
   * |     control panel      |
   * |________________________|
   *
   * etc.
   * </pre>
   * @param controlPanelConstraints the control panel layout constraints (BorderLayout constraints)
   * @throws IllegalStateException if the panel has been initialized
   * @throws IllegalArgumentException in case the given constraint is not one of BorderLayout.SOUTH, NORTH, EAST or WEST
   */
  public final void setControlPanelConstraints(String controlPanelConstraints) {
    checkIfInitialized();
    switch (requireNonNull(controlPanelConstraints)) {
      case BorderLayout.SOUTH:
      case BorderLayout.NORTH:
      case BorderLayout.EAST:
      case BorderLayout.WEST:
        break;
      default:
        throw new IllegalArgumentException("Control panel constraint must be one of BorderLayout.SOUTH, NORTH, EAST or WEST");
    }
    this.controlPanelConstraints = controlPanelConstraints;
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
    if (detailEntityPanels.contains(requireNonNull(detailPanel))) {
      throw new IllegalStateException("Panel already contains detail panel: " + detailPanel);
    }
    addEntityPanelAndLinkSiblings(detailPanel, detailEntityPanels);
    detailPanel.setParentPanel(this);
    detailPanel.addBeforeActivateListener(this::selectEntityPanel);
  }

  /**
   * Initializes this EntityPanel, in case of some specific initialization code you can override the
   * {@link #initializeUI()} method and add your code there.
   * This method marks this panel as initialized which prevents it from running again, whether an exception occurs or not.
   * @param <T> the entity panel type
   * @return this EntityPanel instance
   */
  public final <T extends EntityPanel> T initialize() {
    if (!initialized) {
      WaitCursor.show(this);
      try {
        initializeUI();
      }
      finally {
        initialized = true;
        WaitCursor.hide(this);
      }
    }

    return (T) this;
  }

  /**
   * @param <T> the edit panel type
   * @return the edit panel
   */
  public final <T extends EntityEditPanel> T editPanel() {
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
   * @return the EntityTablePanel used by this EntityPanel
   */
  public final <T extends EntityTablePanel> T tablePanel() {
    return (T) tablePanel;
  }

  /**
   * @return true if this panel contains a table panel.
   */
  public final boolean containsTablePanel() {
    return tablePanel != null;
  }

  /**
   * Returns the panel containing the edit panel and the edit controls panel.
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

    return detailEntityPanels.stream()
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
    for (EntityPanel detailPanel : detailEntityPanels) {
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
    return unmodifiableCollection(detailEntityPanels);
  }

  /**
   * Returns true if this panel contains a detail panel for the given {@code entityType}
   * @param entityType the entityType
   * @return true if a detail panel for the given entityType is found
   */
  public final boolean containsDetailPanel(EntityType entityType) {
    requireNonNull(entityType);
    return detailEntityPanels.stream()
            .anyMatch(detailPanel -> detailPanel.entityModel.entityType().equals(entityType));
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + caption;
  }

  /**
   * Sets the caption to use when this panel is displayed.
   * @param caption the caption
   */
  public final void setCaption(String caption) {
    this.caption = caption;
  }

  /**
   * @return the caption to use when presenting this entity panel
   */
  public final String getCaption() {
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
   * @see #activatePanel()
   */
  public final void addBeforeActivateListener(Consumer<EntityPanel> listener) {
    beforeActivateEvent.addDataListener(listener);
  }

  public final void activatePanel() {
    beforeActivateEvent.accept(this);
    if (parentPanel != null && parentPanel.getDetailPanelState() == HIDDEN) {
      parentPanel.setDetailPanelState(EMBEDDED);
    }
    initialize();
    Window parentWindow = parentWindow(this);
    if (parentWindow != null) {
      parentWindow.toFront();
    }
    if (editPanelWindow != null) {
      editPanelWindow.toFront();
    }
    requestInitialFocus();
  }

  @Override
  public final void selectEntityPanel(EntityPanel entityPanel) {
    requireNonNull(entityPanel);
    if (detailPanelTabbedPane != null) {
      detailPanelTabbedPane.setSelectedComponent(entityPanel);
      for (SwingEntityModel activeModel : new ArrayList<>(entityModel.activeDetailModels())) {
        entityModel.detailModelLink(activeModel).setActive(false);
      }
      SwingEntityModel detailModel = entityPanel.model();
      if (entityModel.containsDetailModel(detailModel)) {
        entityModel.detailModelLink(detailModel).setActive(true);
      }
    }
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
   * @return the resize weight value to use when initializing the left/right split pane, which
   * controls the initial divider placement (0 - 1).
   * Override to control the initial divider placement
   */
  public final double getDetailSplitPaneResizeWeight() {
    return detailSplitPanelResizeWeight;
  }

  /**
   * @param detailSplitPanelResizeWeight the detail panel split size weight
   * @throws IllegalStateException if the panel has been initialized
   */
  public final void setDetailSplitPanelResizeWeight(double detailSplitPanelResizeWeight) {
    checkIfInitialized();
    this.detailSplitPanelResizeWeight = detailSplitPanelResizeWeight;
  }

  /**
   * @return true if the detail panel tab pane should be included
   */
  public final boolean isIncludeDetailTabPane() {
    return includeDetailTabPane;
  }

  /**
   * @param includeDetailTabPane true if the detail panel tab pane should be included
   * @throws IllegalStateException if the panel has been initialized
   */
  public final void setIncludeDetailTabPane(boolean includeDetailTabPane) {
    checkIfInitialized();
    this.includeDetailTabPane = includeDetailTabPane;
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
   * @return true if detail panel controls should be included
   * @see EntityPanel#INCLUDE_DETAIL_PANEL_CONTROLS
   */
  public final boolean isIncludeDetailPanelControls() {
    return includeDetailPanelControls;
  }

  /**
   * @param includeDetailPanelControls true if detail panel controls should be shown
   * @throws IllegalStateException if the panel has been initialized
   */
  public final void setIncludeDetailPanelControls(boolean includeDetailPanelControls) {
    checkIfInitialized();
    this.includeDetailPanelControls = includeDetailPanelControls;
  }

  /**
   * @return true if the control panel should be included
   */
  public final boolean isIncludeControlPanel() {
    return includeControlPanel;
  }

  /**
   * @param includeControlPanel true if the control panel should be included
   * @throws IllegalStateException if the panel has been initialized
   */
  public final void setIncludeControlPanel(boolean includeControlPanel) {
    checkIfInitialized();
    this.includeControlPanel = includeControlPanel;
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
   * Toggles the detail panel state between WINDOW, HIDDEN and EMBEDDED
   */
  public final void toggleDetailPanelState() {
    if (detailPanelState == WINDOW) {
      setDetailPanelState(HIDDEN);
    }
    else if (detailPanelState == EMBEDDED) {
      setDetailPanelState(WINDOW);
    }
    else {
      setDetailPanelState(EMBEDDED);
    }
  }

  /**
   * Toggles the edit panel state between WINDOW, HIDDEN and EMBEDDED
   */
  public final void toggleEditPanelState() {
    if (editPanelState == WINDOW) {
      setEditPanelState(HIDDEN);
    }
    else if (editPanelState == EMBEDDED) {
      setEditPanelState(WINDOW);
    }
    else {
      setEditPanelState(EMBEDDED);
    }
  }

  /**
   * @return the detail panel state, either HIDDEN, EMBEDDED or WINDOW
   */
  public final PanelState getDetailPanelState() {
    return detailPanelState;
  }

  /**
   * @return the edit panel state, either HIDDEN, EMBEDDED or WINDOW
   */
  public final PanelState getEditPanelState() {
    return editPanelState;
  }

  /**
   * @param state the detail panel state (HIDDEN, EMBEDDED or WINDOW)
   */
  public final void setDetailPanelState(PanelState state) {
    requireNonNull(state);
    if (detailPanelTabbedPane == null) {
      this.detailPanelState = state;
      return;
    }

    if (state != HIDDEN) {
      selectedDetailPanel().initialize();
    }

    if (detailPanelState == WINDOW) {//if we are leaving the WINDOW state, hide all child detail windows
      for (EntityPanel detailPanel : detailEntityPanels) {
        if (detailPanel.detailPanelState == WINDOW) {
          detailPanel.setDetailPanelState(HIDDEN);
        }
      }
    }

    SwingEntityModel detailModel = selectedDetailPanel().model();
    if (entityModel.containsDetailModel(detailModel)) {
      entityModel.detailModelLink(detailModel).setActive(state != HIDDEN);
    }

    detailPanelState = state;
    if (state != WINDOW) {
      disposeDetailWindow();
    }

    if (state == EMBEDDED) {
      if (tableDetailSplitPane.getRightComponent() != detailPanelTabbedPane) {
        tableDetailSplitPane.setRightComponent(detailPanelTabbedPane);
      }
    }
    else if (state == HIDDEN) {
      tableDetailSplitPane.setRightComponent(null);
    }
    else {
      showDetailWindow();
    }

    revalidate();
  }

  /**
   * @param state the edit panel state, either HIDDEN, EMBEDDED or DIALOG
   */
  public final void setEditPanelState(PanelState state) {
    requireNonNull(state);
    if (!containsEditPanel() || (editPanelState == state)) {
      return;
    }

    editPanelState = state;
    updateEditPanelState();
  }

  /**
   * Resizes this panel in the given direction
   * @param direction the resize direction
   * @param pixelAmount the resize amount
   */
  public final void resizePanel(Direction direction, int pixelAmount) {
    switch (requireNonNull(direction)) {
      case UP:
        setEditPanelState(HIDDEN);
        break;
      case DOWN:
        if (editPanelState == EMBEDDED) {
          setEditPanelState(WINDOW);
        }
        else {
          setEditPanelState(EMBEDDED);
        }
        break;
      case RIGHT:
        if (tableDetailSplitPane != null) {
          tableDetailSplitPane.setDividerLocation(Math.min(tableDetailSplitPane.getDividerLocation() + pixelAmount,
                  tableDetailSplitPane.getMaximumDividerLocation()));
        }
        break;
      case LEFT:
        if (tableDetailSplitPane != null) {
          tableDetailSplitPane.setDividerLocation(Math.max(tableDetailSplitPane.getDividerLocation() - pixelAmount, 0));
        }
        break;
      default:
        throw new IllegalArgumentException("Undefined resize direction: " + direction);
    }
  }

  /**
   * Requests focus for this panel. If an edit panel is available and not hidden, the component
   * defined as the initialFocusComponent gets the input focus.
   * If no edit panel is available the table panel gets the focus, otherwise the first child
   * component of this EntityPanel is used.
   * @see EntityEditPanel#setInitialFocusComponent(javax.swing.JComponent)
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
    detailEntityPanels.forEach(EntityPanel::savePreferences);
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
   * <pre>
   * The default layout is as follows:
   * __________________________________
   * |  edit    |control|             |
   * |  panel   | panel |             |
   * |__________|_______|   detail    |
   * |                  |   panel     |
   * |   table panel    |             |
   * |(EntityTablePanel)|             |
   * |                  |             |
   * |__________________|_____________|
   * </pre>
   * @see #editControlPanel()
   * @see #editControlTablePanel()
   */
  protected void initializeUI() {
    if (editPanel != null) {
      initializeEditControlPanel();
    }
    if (tablePanel != null) {
      initializeTablePanel();
      editControlTablePanel.add(tablePanel, BorderLayout.CENTER);
    }
    setLayout(borderLayout());
    if (!includeDetailTabPane || detailEntityPanels.isEmpty()) {
      tableDetailSplitPane = null;
      detailPanelTabbedPane = null;
      add(editControlTablePanel, BorderLayout.CENTER);
    }
    else {
      tableDetailSplitPane = createTableDetailSplitPane();
      detailPanelTabbedPane = createDetailTabbedPane();
      add(tableDetailSplitPane, BorderLayout.CENTER);
    }
    setDetailPanelState(detailPanelState);
    if (containsEditPanel()) {
      updateEditPanelState();
    }
    setupKeyboardActions();
    if (useKeyboardNavigation) {
      setupNavigation();
    }
    setupResizing();
  }

  /**
   * Creates a base panel for the edit panel.
   * The default layout is a {@link FlowLayout} with the alignment depending on the {@link #getControlPanelConstraints()}.
   * The resulting panel is added at {@link BorderLayout#CENTER} on the {@link #editControlPanel()}
   * @param editPanel the initialized edit panel
   * @return a base panel for the edit panel
   */
  protected JPanel createEditBasePanel(EntityEditPanel editPanel) {
    int alignment = controlPanelConstraints.equals(BorderLayout.SOUTH) ||
            controlPanelConstraints.equals(BorderLayout.NORTH) ? FlowLayout.CENTER : FlowLayout.LEADING;

    return flowLayoutPanel(alignment)
            .add(editPanel)
            .build();
  }

  /**
   * Creates the control panel or component to place next to the edit panel,
   * containing the edit controls, such as insert, update and delete.
   * Only called if {@link #isIncludeControlPanel()} returns true.
   * @return the panel containing the edit panel controls
   * @see EntityEditPanel#createControls()
   * @see EntityPanel#TOOLBAR_CONTROLS
   * @see EntityPanel#CONTROL_PANEL_CONSTRAINTS
   * @see EntityPanel#CONTROL_TOOLBAR_CONSTRAINTS
   */
  protected JComponent createEditControlPanel() {
    Controls controls = editPanel.createControls();
    if (controls == null || controls.isEmpty()) {
      return null;
    }
    boolean horizontalLayout = controlPanelConstraints.equals(BorderLayout.SOUTH) || controlPanelConstraints.equals(BorderLayout.NORTH);
    if (toolbarControls) {
      return toolBar(controls)
              .orientation(horizontalLayout ? HORIZONTAL : VERTICAL)
              .build();
    }

    if (horizontalLayout) {
      return flowLayoutPanel(FlowLayout.CENTER)
              .add(buttonPanel(controls)
                      .toggleButtonType(CHECKBOX)
                      .build())
              .build();
    }

    return panel(borderLayout())
            .add(buttonPanel(controls)
                    .orientation(VERTICAL)
                    .buttonBuilder(ButtonBuilder.builder()
                            .horizontalAlignment(SwingConstants.LEADING))
                    .toggleButtonType(CHECKBOX)
                    .build(), BorderLayout.NORTH)
            .build();
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
      Control selectEditPanelControl = Control.control(this::selectEditPanel);
      Control selectInputComponentControl = Control.control(this::selectInputComponent);
      KeyEvents.builder(VK_E)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectEditPanelControl)
              .enable(this);
      KeyEvents.builder(VK_I)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectInputComponentControl)
              .enable(this);
      KeyEvents.builder(VK_I)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectInputComponentControl)
              .enable(editControlPanel);
    }
  }

  protected final void setupResizing() {
    ResizeVerticallyAction resizeUpAction = new ResizeVerticallyAction(this, UP);
    ResizeVerticallyAction resizeDownAction = new ResizeVerticallyAction(this, DOWN);
    ResizeHorizontallyAction resizeRightAction = new ResizeHorizontallyAction(this, RIGHT);
    ResizeHorizontallyAction resizeLeftAction = new ResizeHorizontallyAction(this, LEFT);

    KeyEvents.builder(VK_UP)
            .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(resizeUpAction)
            .enable(this);
    KeyEvents.builder(VK_DOWN)
            .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(resizeDownAction)
            .enable(this);
    KeyEvents.builder(VK_RIGHT)
            .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(resizeRightAction)
            .enable(this);
    KeyEvents.builder(VK_LEFT)
            .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(resizeLeftAction)
            .enable(this);
    if (containsEditPanel()) {
      KeyEvents.builder(VK_UP)
              .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(resizeUpAction)
              .enable(editControlPanel);
      KeyEvents.builder(VK_DOWN)
              .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(resizeDownAction)
              .enable(editControlPanel);
      KeyEvents.builder(VK_RIGHT)
              .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(resizeRightAction)
              .enable(editControlPanel);
      KeyEvents.builder(VK_LEFT)
              .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(resizeLeftAction)
              .enable(editControlPanel);
    }
  }

  protected final void setupNavigation() {
    KeyEvents.builder(VK_UP)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new NavigateAction(UP))
            .enable(this);
    KeyEvents.builder(VK_DOWN)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new NavigateAction(DOWN))
            .enable(this);
    KeyEvents.builder(VK_RIGHT)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new NavigateAction(RIGHT))
            .enable(this);
    KeyEvents.builder(VK_LEFT)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new NavigateAction(LEFT))
            .enable(this);
    if (containsEditPanel()) {
      KeyEvents.builder(VK_UP)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new NavigateAction(UP))
              .enable(editControlPanel);
      KeyEvents.builder(VK_DOWN)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new NavigateAction(DOWN))
              .enable(editControlPanel);
      KeyEvents.builder(VK_RIGHT)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new NavigateAction(RIGHT))
              .enable(editControlPanel);
      KeyEvents.builder(VK_LEFT)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new NavigateAction(LEFT))
              .enable(editControlPanel);
    }
  }

  protected final void initializeEditControlPanel() {
    editPanel.initialize();
    editControlPanel.setBorder(Borders.createEmptyBorder());
    editControlPanel.setMinimumSize(new Dimension(0, 0));
    editControlPanel.add(createEditBasePanel(editPanel), BorderLayout.CENTER);
    if (includeControlPanel) {
      JComponent controlPanel = createEditControlPanel();
      if (controlPanel != null) {
        editControlPanel.add(controlPanel, controlPanelConstraints);
      }
    }
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

  private void initializeTablePanel() {
    Controls controls = Controls.controls();
    if (includeToggleEditPanelControl && editPanel != null) {
      controls.add(createToggleEditPanelControl());
    }
    if (includeDetailPanelControls && !detailEntityPanels.isEmpty()) {
      controls.add(createToggleDetailPanelControl());
    }
    if (controls.isNotEmpty()) {
      tablePanel.addToolBarControls(controls);
    }
    if (includeDetailPanelControls && !detailEntityPanels.isEmpty()) {
      tablePanel.addPopupMenuControls(createSelectDetailPanelControls());
    }
    if (tablePanel.table().getDoubleClickAction() == null) {
      tablePanel.table().setDoubleClickAction(createTableDoubleClickAction());
    }
    tablePanel.initialize();
    tablePanel.setMinimumSize(new Dimension(0, 0));
    int gap = Layouts.HORIZONTAL_VERTICAL_GAP.get();
    tablePanel.setBorder(BorderFactory.createEmptyBorder(0, gap, 0, gap));
  }

  /**
   * Creates the horizontal split pane, used in the case of detail panel(s)
   * @return the horizontal split pane
   */
  private JSplitPane createTableDetailSplitPane() {
    return splitPane()
            .orientation(JSplitPane.HORIZONTAL_SPLIT)
            .continuousLayout(true)
            .oneTouchExpandable(true)
            .border(BorderFactory.createEmptyBorder())//minor facelift when using metal laf
            .resizeWeight(detailSplitPanelResizeWeight)
            .leftComponent(editControlTablePanel)
            .rightComponent(detailPanelTabbedPane)
            .build();
  }

  /**
   * Creates the JTabbedPane containing the detail panels, used in case of multiple detail panels
   * @return the JTabbedPane for holding detail panels
   */
  private JTabbedPane createDetailTabbedPane() {
    TabbedPaneBuilder builder = tabbedPane()
            .focusable(false)
            .changeListener(e -> selectedDetailPanel().activatePanel());
    detailEntityPanels.forEach(detailPanel -> builder.tabBuilder(detailPanel.caption, detailPanel)
            .toolTipText(detailPanel.description)
            .add());
    if (includeDetailPanelControls) {
      builder.mouseListener(new TabbedPaneMouseReleasesListener());
    }

    return builder.build();
  }

  /**
   * Creates the Control to trigger when a double click is performed on the table, if a table is present.
   * The default implementation shows the edit panel in a window if one is available and hidden, if that is
   * not the case and the detail panels are hidden those are shown in a window.
   * @return the Control to trigger when a double click is performed on the table
   */
  private Control createTableDoubleClickAction() {
    return Control.control(new TableDoubleClickCommand());
  }

  /**
   * @return a control for toggling the edit panel
   */
  private Control createToggleEditPanelControl() {
    return Control.builder(this::toggleEditPanelState)
            .smallIcon(FrameworkIcons.instance().editPanel())
            .description(MESSAGES.getString("toggle_edit"))
            .build();
  }

  /**
   * @return a control for toggling the detail panel
   */
  private Control createToggleDetailPanelControl() {
    return Control.builder(this::toggleDetailPanelState)
            .smallIcon(FrameworkIcons.instance().detail())
            .description(MESSAGES.getString("toggle_detail"))
            .build();
  }

  private Controls createSelectDetailPanelControls() {
    if (detailEntityPanels.isEmpty()) {
      return null;
    }

    Controls.Builder controls = Controls.builder()
            .name(MESSAGES.getString(DETAIL_TABLES))
            .smallIcon(FrameworkIcons.instance().detail());
    detailEntityPanels.forEach(detailPanel ->
            controls.control(Control.builder(new SelectDetailPanelCommand(detailPanel))
                    .name(detailPanel.getCaption())));

    return controls.build();
  }

  //#############################################################################################
  // End - initialization methods
  //#############################################################################################

  private void selectEditPanel() {
    if (getEditPanelState() == HIDDEN) {
      setEditPanelState(EMBEDDED);
    }
    editPanel().requestInitialFocus();
  }

  private void selectInputComponent() {
    if (getEditPanelState() == HIDDEN) {
      setEditPanelState(EMBEDDED);
    }
    editPanel().selectInputComponent();
  }

  private void updateEditPanelState() {
    if (editPanelState != WINDOW) {
      disposeEditWindow();
    }

    if (editPanelState == EMBEDDED) {
      editControlTablePanel.add(editControlPanel, BorderLayout.NORTH);
    }
    else if (editPanelState == HIDDEN) {
      editControlTablePanel.remove(editControlPanel);
    }
    else {
      showEditWindow();
    }
    requestInitialFocus();

    revalidate();
  }

  /**
   * Shows the detail panels in a window
   */
  private void showDetailWindow() {
    Window parent = parentWindow(this);
    if (parent != null) {
      Dimension parentSize = parent.getSize();
      Dimension size = detailWindowSize(parentSize);
      Point parentLocation = parent.getLocation();
      int detailWindowX = parentLocation.x + (parentSize.width - size.width);
      int detailWindowY = parentLocation.y + (parentSize.height - size.height) - DETAIL_WINDOW_OFFSET;
      detailPanelWindow = createDetailPanelWindow();
      detailPanelWindow.setSize(size);
      detailPanelWindow.setLocation(new Point(detailWindowX, detailWindowY));
      detailPanelWindow.setVisible(true);
    }
  }

  /**
   * @param parentSize the size of the parent window
   * @return the size to use when showing the detail window
   */
  private Dimension detailWindowSize(Dimension parentSize) {
    int detailWindowWidth = (int) (parentSize.width * DETAIL_WINDOW_SIZE_RATIO);
    int detailWindowHeight = containsEditPanel() ? (int) (parentSize.height * DETAIL_WINDOW_SIZE_RATIO) : parentSize.height;

    return new Dimension(detailWindowWidth, detailWindowHeight);
  }

  /**
   * Shows the edit panel in a window
   */
  private void showEditWindow() {
    editPanelWindow = createEditWindow();
    editPanelWindow.setVisible(true);
  }

  /**
   * @return the detail panel selected in the detail tab pane.
   * @throws IllegalStateException in case no detail panels are defined
   */
  private EntityPanel selectedDetailPanel() {
    if (detailPanelTabbedPane == null) {
      throw new IllegalStateException("No detail panels available");
    }

    return (EntityPanel) detailPanelTabbedPane.getSelectedComponent();
  }

  private Window createEditWindow() {
    if (USE_FRAME_PANEL_DISPLAY.get()) {
      return Windows.frame(editControlPanel)
              .locationRelativeTo(this)
              .title(caption)
              .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
              .onClosed(windowEvent -> setEditPanelState(HIDDEN))
              .build();
    }

    return Dialogs.componentDialog(editControlPanel)
            .owner(this)
            .title(caption)
            .modal(false)
            .disposeOnEscape(disposeEditDialogOnEscape)
            .onClosed(e -> setEditPanelState(HIDDEN))
            .build();
  }

  private Window createDetailPanelWindow() {
    if (USE_FRAME_PANEL_DISPLAY.get()) {
      return Windows.frame(detailPanelTabbedPane)
              .title(caption + " - " + MESSAGES.getString(DETAIL_TABLES))
              .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
              .onClosed(windowEvent -> {
                //the frame can be closed when embedding the panel, don't hide if that's the case
                if (getDetailPanelState() != EMBEDDED) {
                  setDetailPanelState(HIDDEN);
                }
              })
              .build();
    }

    return Dialogs.componentDialog(detailPanelTabbedPane)
            .owner(this)
            .title(caption + " - " + MESSAGES.getString(DETAIL_TABLES))
            .modal(false)
            .onClosed(e -> {
              //the dialog can be closed when embedding the panel, don't hide if that's the case
              if (getDetailPanelState() != EMBEDDED) {
                setDetailPanelState(HIDDEN);
              }
            })
            .build();
  }

  private void disposeEditWindow() {
    if (editPanelWindow != null) {
      editPanelWindow.setVisible(false);
      editPanelWindow.dispose();
      editPanelWindow = null;
    }
  }

  private void disposeDetailWindow() {
    if (detailPanelWindow != null) {
      detailPanelWindow.setVisible(false);
      detailPanelWindow.dispose();
      detailPanelWindow = null;
    }
  }

  private void checkIfInitialized() {
    if (initialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
  }

  private final class TabbedPaneMouseReleasesListener extends MouseAdapter {

    @Override
    public void mouseReleased(MouseEvent e) {
      if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
        setDetailPanelState(getDetailPanelState() == WINDOW ? EMBEDDED : WINDOW);
      }
      else if (e.getButton() == MouseEvent.BUTTON2) {
        setDetailPanelState(getDetailPanelState() == EMBEDDED ? HIDDEN : EMBEDDED);
      }
    }
  }

  private final class TableDoubleClickCommand implements Control.Command {

    @Override
    public void perform() {
      if (containsEditPanel() || (!detailEntityPanels.isEmpty() && includeDetailTabPane)) {
        if (containsEditPanel() && getEditPanelState() == HIDDEN) {
          setEditPanelState(WINDOW);
        }
        else if (getDetailPanelState() == HIDDEN) {
          setDetailPanelState(WINDOW);
        }
      }
    }
  }

  private static final class SelectDetailPanelCommand implements Control.Command {

    private final EntityPanel detailPanel;

    private SelectDetailPanelCommand(EntityPanel detailPanel) {
      this.detailPanel = detailPanel;
    }

    @Override
    public void perform() {
      detailPanel.activatePanel();
    }
  }

  private final class NavigateAction extends AbstractAction {

    private final Direction direction;

    private NavigateAction(Direction direction) {
      super("Navigate " + direction);
      this.direction = direction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      switch (direction) {
        case LEFT:
          if (previousSiblingPanel != null) {
            previousSiblingPanel.activatePanel();
          }
          break;
        case RIGHT:
          if (nextSiblingPanel != null) {
            nextSiblingPanel.activatePanel();
          }
          break;
        case UP:
          if (parentPanel != null) {
            parentPanel.activatePanel();
          }
          break;
        case DOWN:
          activeDetailPanel()
                  .ifPresent(EntityPanel::activatePanel);
          break;
        default:
          throw new IllegalArgumentException("Unknown direction: " + direction);
      }
    }
  }

  private Optional<EntityPanel> activeDetailPanel() {
    Collection<EntityPanel> activeDetailPanels = activeDetailPanels();
    if (!activeDetailPanels.isEmpty()) {
      return Optional.of(activeDetailPanels.iterator().next());
    }

    return Optional.empty();
  }

  private static final class ResizeHorizontallyAction extends AbstractAction {

    private final EntityPanel panel;
    private final Direction direction;

    private ResizeHorizontallyAction(EntityPanel panel, Direction direction) {
      super("Resize " + direction);
      this.panel = panel;
      this.direction = direction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (panel.parentPanel != null) {
        panel.parentPanel.resizePanel(direction, RESIZE_AMOUNT);
      }
    }
  }

  private static final class ResizeVerticallyAction extends AbstractAction {

    private final EntityPanel panel;
    private final Direction direction;

    private ResizeVerticallyAction(EntityPanel panel, Direction direction) {
      super("Resize " + direction);
      this.panel = panel;
      this.direction = direction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      panel.resizePanel(direction, RESIZE_AMOUNT);
    }
  }

  private static class FocusActivationListener implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent changeEvent) {
      Component focusedComponent = (Component) changeEvent.getNewValue();
      EntityPanel entityPanelParent = parentOfType(EntityPanel.class, focusedComponent);
      if (entityPanelParent != null) {
        if (entityPanelParent.editPanel() != null) {
          entityPanelParent.editPanel().setActive(true);
        }
      }
      else {
        EntityEditPanel editPanelParent = parentOfType(EntityEditPanel.class, focusedComponent);
        if (editPanelParent != null) {
          editPanelParent.setActive(true);
        }
      }
    }
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
     * @param refreshOnInit if true then the data model this panel is based on will be refreshed when
     * the panel is initialized
     * @return this builder instance
     */
    Builder refreshOnInit(boolean refreshOnInit);

    /**
     * @param tableConditionPanelVisible if true then the table condition panel is made visible when the panel is initialized
     * @return this builder instance
     */
    Builder tableConditionPanelVisible(boolean tableConditionPanelVisible);

    /**
     * @param detailPanelState the state of the detail panels when this panel is initialized
     * @return this builder instance
     */
    Builder detailPanelState(PanelState detailPanelState);

    /**
     * @param detailSplitPanelResizeWeight the split panel resize weight to use when initializing this panel
     * with its detail panels
     * @return this builder instance
     */
    Builder detailSplitPanelResizeWeight(double detailSplitPanelResizeWeight);

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
}
