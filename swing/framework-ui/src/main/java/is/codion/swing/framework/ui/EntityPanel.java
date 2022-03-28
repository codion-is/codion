/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.event.EventDataListener;
import is.codion.common.properties.PropertyValue;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.panel.HierarchyPanel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static is.codion.swing.framework.ui.EntityPanel.Direction.*;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.*;
import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A panel representing an Entity via a EntityModel, which facilitates browsing and editing of records.
 * <pre>
 *   EntityType entityType = ...;
 *   EntityConnectionProvider connectionProvider = ...;
 *   SwingEntityModel entityModel = new SwingEntityModel(entityType, connectionProvider);
 *   EntityPanel entityPanel = new EntityPanel(entityModel);
 *   entityPanel.initializePanel();
 *   JFrame frame = new JFrame();
 *   frame.add(entityPanel);
 *   frame.pack();
 *   frame.setVisible(true);
 * </pre>
 */
public class EntityPanel extends JPanel implements HierarchyPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityPanel.class.getName());

  private static final String ENTITY_MODEL_PARAM = "entityModel";
  private static final String MSG_DETAIL_TABLES = "detail_tables";

  private static final int DEFAULT_SPLIT_PANE_DIVIDER_SIZE = 18;

  /**
   * Indicates whether entity panels should be activated when the panel receives focus<br>
   * Value type: Boolean<br>
   * Default value: true
   * @see is.codion.swing.framework.ui.EntityEditPanel#ALL_PANELS_ACTIVE
   */
  public static final PropertyValue<Boolean> USE_FOCUS_ACTIVATION =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.useFocusActivation", true);

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
  public static final PropertyValue<Boolean> SHOW_TOGGLE_EDIT_PANEL_CONTROL =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.showToggleEditPanelControl", true);

  /**
   * Specifies whether actions to hide detail panels or show them in a dialog are available to the user<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> SHOW_DETAIL_PANEL_CONTROLS =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.showDetailPanelControls", true);

  /**
   * Specifies the default size of the divider for detail panel split panes.<br>
   * Value type: Integer<br>
   * Default value: 18<br>
   */
  public static final PropertyValue<Integer> SPLIT_PANE_DIVIDER_SIZE =
          Configuration.integerValue("is.codion.swing.framework.ui.EntityPanel.splitPaneDividerSize", DEFAULT_SPLIT_PANE_DIVIDER_SIZE);

  /**
   * Specifies whether the action buttons (Save, update, delete, clear, refresh) should be on a toolbar<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> TOOLBAR_BUTTONS =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.toolbarButtons", false);

  /**
   * Specifies if detail and edit panels should be displayed in a frame instead of the default dialog<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> DISPLAY_PANELS_IN_FRAME =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.displayPanelsInFrame", false);

  /**
   * The possible states of a detail or edit panel.
   */
  public enum PanelState {
    WINDOW, EMBEDDED, HIDDEN
  }

  /**
   * The navigation and resizing directions.
   */
  enum Direction {
    UP, DOWN, RIGHT, LEFT
  }

  private static final int RESIZE_AMOUNT = 30;
  private static final double DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT = 0.5;
  private static final int DETAIL_WINDOW_OFFSET = 29;
  private static final double DETAIL_WINDOW_SIZE_RATIO = 1.5;
  private static final int DETAIL_WINDOW_HEIGHT_OFFSET = 54;

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
   * The base edit panel which contains the controls required for editing an entity
   */
  private final JPanel editControlPanel = new JPanel(Layouts.borderLayout());

  /**
   * The base panel containing the edit, control and table panels
   */
  private final JPanel editControlTablePanel = new JPanel(Layouts.borderLayout());

  /**
   * The caption to use when presenting this entity panel
   */
  private String caption;

  /**
   * The horizontal split pane, which is used in case this entity panel has detail panels.
   * It splits the lower section of this EntityPanel into the EntityTablePanel
   * on the left, and the detail panels on the right.
   */
  private JSplitPane horizontalSplitPane;

  /**
   * The master panel, if any, so that detail panels can refer to their masters
   */
  private EntityPanel masterPanel;

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
   * indicates where the control panel should be placed in a BorderLayout
   */
  private String controlPanelConstraints = TOOLBAR_BUTTONS.get() ? BorderLayout.WEST : BorderLayout.EAST;

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
  private boolean showDetailPanelControls = SHOW_DETAIL_PANEL_CONTROLS.get();

  /**
   * if true and an edit panel is available the actions to toggle it is included
   */
  private boolean showToggleEditPanelControl = SHOW_TOGGLE_EDIT_PANEL_CONTROL.get();

  /**
   * if true then the ESC key disposes the edit dialog
   */
  private boolean disposeEditDialogOnEscape = DISPOSE_EDIT_DIALOG_ON_ESCAPE.get();

  /**
   * if true then keyboard navigation is enabled
   */
  private boolean useKeyboardNavigation = USE_KEYBOARD_NAVIGATION.get();

  /**
   * True after {@code initializePanel()} has been called
   */
  private boolean panelInitialized = false;

  private double detailSplitPanelResizeWeight = DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT;

  static {
    if (USE_FOCUS_ACTIVATION.get()) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", new FocusActivationListener());
    }
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   */
  public EntityPanel(SwingEntityModel entityModel) {
    this(entityModel, null, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.getTableModel()) : null);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   */
  public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel) {
    this(entityModel, editPanel, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.getTableModel()) : null);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param tablePanel the table panel
   */
  public EntityPanel(SwingEntityModel entityModel, EntityTablePanel tablePanel) {
    this(entityModel, null, tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   * @param tablePanel the table panel
   */
  public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel, EntityTablePanel tablePanel) {
    requireNonNull(entityModel, ENTITY_MODEL_PARAM);
    setFocusCycleRoot(true);
    this.entityModel = entityModel;
    this.caption = entityModel.getEditModel().getEntityDefinition().getCaption();
    this.editPanel = editPanel;
    this.tablePanel = tablePanel;
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(editControlPanel, editControlTablePanel, horizontalSplitPane, tablePanel, editPanel);
    if (detailEntityPanels != null) {
      Utilities.updateUI(detailEntityPanels);
    }
  }

  /**
   * @return the EntityModel
   */
  public final SwingEntityModel getModel() {
    return entityModel;
  }

  /**
   * @return the EntityEditModel
   */
  public final SwingEntityEditModel getEditModel() {
    return entityModel.getEditModel();
  }

  /**
   * @return the EntityTableModel, null if none is available
   */
  public final SwingEntityTableModel getTableModel() {
    return entityModel.getTableModel();
  }

  /**
   * @return the control panel layout constraints (BorderLayout constraints)
   */
  public final String getControlPanelConstraints() {
    return controlPanelConstraints;
  }

  /**
   * Sets the layout constraints to use for the control panel
   *<pre>
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
   *</pre>
   * @param controlPanelConstraints the control panel layout constraints (BorderLayout constraints)
   * @throws IllegalStateException if the panel has been initialized
   * @throws IllegalArgumentException in case the given constraint is not one of BorderLayout.SOUTH, NORTH, EAST or WEST
   */
  public final void setControlPanelConstraints(String controlPanelConstraints) {
    checkIfInitialized();
    switch (controlPanelConstraints) {
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
   * Adds the given detail panel, and adds the detail model to the underlying
   * model if it does not contain it already, and then sets {@code includeDetailPanelTabPane}
   * to true
   * @param detailPanel the detail panel to add
   * @throws IllegalStateException if the panel has been initialized
   */
  public final void addDetailPanel(EntityPanel detailPanel) {
    checkIfInitialized();
    detailPanel.setMasterPanel(this);
    detailEntityPanels.add(detailPanel);
  }

  /**
   * Initializes this EntityPanels UI, in case of some specific initialization code you can override the
   * {@code initialize()} method and add your code there.
   * This method marks this panel as initialized which prevents it from running again, whether an exception occurs or not.
   * @return this EntityPanel instance
   * @see #initialize()
   * @see #isPanelInitialized()
   */
  public final EntityPanel initializePanel() {
    if (!panelInitialized) {
      WaitCursor.show(this);
      try {
        initializeAssociatedPanels();
        initializeControlPanels();
        initializeUI();
        bindEvents();
        initialize();
      }
      finally {
        panelInitialized = true;
        WaitCursor.hide(this);
      }
    }

    return this;
  }

  /**
   * @return true if the method initializePanel() has been called on this EntityPanel instance
   * @see #initializePanel()
   */
  public final boolean isPanelInitialized() {
    return panelInitialized;
  }

  /**
   * @return the edit panel
   */
  public final EntityEditPanel getEditPanel() {
    return editPanel;
  }

  /**
   * @return true if this panel contains a edit panel.
   */
  public final boolean containsEditPanel() {
    return editPanel != null;
  }

  /**
   * @return the EntityTablePanel used by this EntityPanel
   */
  public final EntityTablePanel getTablePanel() {
    return tablePanel;
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
  public final JPanel getEditControlPanel() {
    return editControlPanel;
  }

  /**
   * @return the currently visible/linked detail EntityPanel, if any
   */
  public final Collection<EntityPanel> getLinkedDetailPanels() {
    Collection<SwingEntityModel> linkedDetailModels = entityModel.getLinkedDetailModels();

    return detailEntityPanels.stream()
            .filter(detailPanel -> linkedDetailModels.contains(detailPanel.entityModel))
            .collect(toList());
  }

  /**
   * Returns the detail panel for the given {@code entityType}, if one is available
   * @param entityType the entityType of the detail panel to retrieve
   * @return the detail panel of the given type
   * @throws IllegalArgumentException in case the panel was not found
   */
  public final EntityPanel getDetailPanel(EntityType entityType) {
    for (EntityPanel detailPanel : detailEntityPanels) {
      if (detailPanel.entityModel.getEntityType().equals(entityType)) {
        return detailPanel;
      }
    }

    throw new IllegalArgumentException("Detail panel for entity: " + entityType + " not found in panel: " + getClass());
  }

  /**
   * Returns all detail panels.
   * @return the detail panels
   */
  public final Collection<EntityPanel> getDetailPanels() {
    return Collections.unmodifiableCollection(detailEntityPanels);
  }

  /**
   * Returns true if this panel contains a detail panel for the given {@code entityType}
   * @param entityType the entityType
   * @return true if a detail panel for the given entityType is found
   */
  public final boolean containsDetailPanel(EntityType entityType) {
    return detailEntityPanels.stream()
            .anyMatch(detailPanel -> detailPanel.entityModel.getEntityType().equals(entityType));
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

  @Override
  public final void activatePanel() {
    getParentPanel().ifPresent(parentPanel ->
            parentPanel.setSelectedChildPanel(this));
    initializePanel();
    requestInitialFocus();
    if (editPanelWindow != null) {
      editPanelWindow.toFront();
    }
  }

  @Override
  public final Optional<HierarchyPanel> getParentPanel() {
    if (masterPanel == null) {
      return Utilities.getParentOfType(HierarchyPanel.class, this);
    }

    return Optional.of(masterPanel);
  }

  @Override
  public final Optional<HierarchyPanel> getSelectedChildPanel() {
    Collection<EntityPanel> linkedDetailPanels = getLinkedDetailPanels();
    if (!linkedDetailPanels.isEmpty()) {
      return Optional.of(linkedDetailPanels.iterator().next());
    }

    return Optional.empty();
  }

  @Override
  public final void setSelectedChildPanel(HierarchyPanel childPanel) {
    if (detailPanelTabbedPane != null) {
      detailPanelTabbedPane.setSelectedComponent((JComponent) childPanel);
      for (SwingEntityModel linkedModel : new ArrayList<>(entityModel.getLinkedDetailModels())) {
        entityModel.removeLinkedDetailModel(linkedModel);
      }
      entityModel.addLinkedDetailModel(getTabbedDetailPanel().getModel());
    }
  }

  @Override
  public final Optional<HierarchyPanel> getPreviousSiblingPanel() {
    Optional<HierarchyPanel> optionalParent = getParentPanel();
    if (!optionalParent.isPresent()) {//no parent, no siblings
      return Optional.empty();
    }
    HierarchyPanel parentPanel = optionalParent.get();
    List<? extends HierarchyPanel> siblingPanels = parentPanel.getChildPanels();
    if (siblingPanels.contains(this)) {
      int index = siblingPanels.indexOf(this);
      if (index == 0) {//wrap around
        return Optional.of(siblingPanels.get(siblingPanels.size() - 1));
      }

      return Optional.of(siblingPanels.get(index - 1));
    }

    return Optional.empty();
  }

  @Override
  public final Optional<HierarchyPanel> getNextSiblingPanel() {
    Optional<HierarchyPanel> optionalParent = getParentPanel();
    if (!optionalParent.isPresent()) {//no parent, no siblings
      return Optional.empty();
    }
    HierarchyPanel parentPanel = optionalParent.get();
    List<? extends HierarchyPanel> siblingPanels = parentPanel.getChildPanels();
    if (siblingPanels.contains(this)) {
      int index = siblingPanels.indexOf(this);
      if (index == siblingPanels.size() - 1) {//wrap around
        return Optional.of(siblingPanels.get(0));
      }

      return Optional.of(siblingPanels.get(index + 1));
    }

    return Optional.empty();
  }

  @Override
  public final List<? extends HierarchyPanel> getChildPanels() {
    return Collections.unmodifiableList(detailEntityPanels);
  }

  /**
   * @return a control for toggling the edit panel
   */
  public final Control getToggleEditPanelControl() {
    return Control.builder(this::toggleEditPanelState)
            .smallIcon(frameworkIcons().editPanel())
            .description(MESSAGES.getString("toggle_edit"))
            .build();
  }

  /**
   * @return a control for toggling the detail panel
   */
  public final Control getToggleDetailPanelControl() {
    return Control.builder(this::toggleDetailPanelState)
            .smallIcon(frameworkIcons().detail())
            .description(MESSAGES.getString("toggle_detail"))
            .build();
  }

  /**
   * Displays the exception in a dialog
   * @param exception the exception to handle
   * @see DefaultDialogExceptionHandler
   */
  public final void displayException(Throwable exception) {
    DefaultDialogExceptionHandler.getInstance().displayException(exception, Windows.getParentWindow(this).orElse(null));
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
   * @see EntityPanel#SHOW_TOGGLE_EDIT_PANEL_CONTROL
   */
  public final boolean isShowToggleEditPanelControl() {
    return showToggleEditPanelControl;
  }

  /**
   * @param showToggleEditPanelControl true if a control for toggling the edit panel should be shown
   * @throws IllegalStateException if the panel has been initialized
   */
  public final void setShowToggleEditPanelControl(boolean showToggleEditPanelControl) {
    checkIfInitialized();
    this.showToggleEditPanelControl = showToggleEditPanelControl;
  }

  /**
   * @return true if detail panel controls should be shown
   * @see EntityPanel#SHOW_DETAIL_PANEL_CONTROLS
   */
  public final boolean isShowDetailPanelControls() {
    return showDetailPanelControls;
  }

  /**
   * @param showDetailPanelControls true if detail panel controls should be shown
   * @throws IllegalStateException if the panel has been initialized
   */
  public final void setShowDetailPanelControls(boolean showDetailPanelControls) {
    checkIfInitialized();
    this.showDetailPanelControls = showDetailPanelControls;
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
    if (detailPanelTabbedPane == null) {
      this.detailPanelState = state;
      return;
    }

    if (state != HIDDEN) {
      getTabbedDetailPanel().initializePanel();
    }

    if (detailPanelState == WINDOW) {//if we are leaving the WINDOW state, hide all child detail windows
      for (EntityPanel detailPanel : detailEntityPanels) {
        if (detailPanel.detailPanelState == WINDOW) {
          detailPanel.setDetailPanelState(HIDDEN);
        }
      }
    }

    if (state == HIDDEN) {
      entityModel.removeLinkedDetailModel(getTabbedDetailPanel().entityModel);
    }
    else {
      entityModel.addLinkedDetailModel(getTabbedDetailPanel().entityModel);
    }

    detailPanelState = state;
    if (state != WINDOW) {
      disposeDetailWindow();
    }

    if (state == EMBEDDED) {
      horizontalSplitPane.setRightComponent(detailPanelTabbedPane);
    }
    else if (state == HIDDEN) {
      horizontalSplitPane.setRightComponent(null);
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
    if (!containsEditPanel() || (editPanelState == state)) {
      return;
    }

    editPanelState = state;
    updateEditPanelState();
  }

  /**
   * Hides or shows the active filter panels for this panel and all its child panels
   * (detail panels and their detail panels etc.)
   * @param visible true if the active panels should be shown, false if they should be hidden
   */
  public final void setFilterPanelsVisible(boolean visible) {
    if (!panelInitialized) {
      return;
    }

    if (containsTablePanel()) {
      tablePanel.getTable().setFilterPanelsVisible(visible);
    }
    for (EntityPanel detailEntityPanel : detailEntityPanels) {
      detailEntityPanel.setFilterPanelsVisible(visible);
    }
  }

  /**
   * Resizes this panel in the given direction
   * @param direction the resize direction
   * @param pixelAmount the resize amount
   */
  public final void resizePanel(Direction direction, int pixelAmount) {
    switch (direction) {
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
        if (horizontalSplitPane != null) {
          horizontalSplitPane.setDividerLocation(Math.min(horizontalSplitPane.getDividerLocation() + pixelAmount,
                  horizontalSplitPane.getMaximumDividerLocation()));
        }
        break;
      case LEFT:
        if (horizontalSplitPane != null) {
          horizontalSplitPane.setDividerLocation(Math.max(horizontalSplitPane.getDividerLocation() - pixelAmount, 0));
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
      tablePanel.getTable().requestFocus();
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
   *<pre>
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
   * @see #getEditControlPanel()
   * @see #getEditControlTablePanel()
   */
  protected void initializeUI() {
    if (editPanel != null) {
      initializeEditControlPanel();
    }
    if (tablePanel != null) {
      initializeTablePanel();
      editControlTablePanel.add(tablePanel, BorderLayout.CENTER);
    }
    setLayout(Layouts.borderLayout());
    if (!includeDetailTabPane || detailEntityPanels.isEmpty()) {
      horizontalSplitPane = null;
      detailPanelTabbedPane = null;
      add(editControlTablePanel, BorderLayout.CENTER);
    }
    else {
      horizontalSplitPane = initializeHorizontalSplitPane();
      detailPanelTabbedPane = initializeDetailTabPane();
      horizontalSplitPane.setLeftComponent(editControlTablePanel);
      horizontalSplitPane.setRightComponent(detailPanelTabbedPane);
      add(horizontalSplitPane, BorderLayout.CENTER);
    }
    setDetailPanelState(detailPanelState);
    if (containsEditPanel()) {
      updateEditPanelState();
    }
    initializeKeyboardActions();
    if (useKeyboardNavigation) {
      initializeNavigation();
    }
    initializeResizing();
  }

  /**
   * Called during initialization, before controls have been initialized
   * @see #initializePanel()
   */
  protected void initializeAssociatedPanels() {/*Provided for subclasses*/}

  /**
   * Called during initialization, after controls have been initialized,
   * use this method to initialize any application panels that rely on controls having been initialized
   * @see #initializePanel()
   */
  protected void initializeControlPanels() {/*Provided for subclasses*/}

  /**
   * Override to add code that should be called during the initialization routine after the panel has been initialized
   * @see #initializePanel()
   */
  protected void initialize() {/*Provided for subclasses*/}

  /**
   * Creates the control panel or component to place next to the edit panel, containing controls for managing
   * records, such as insert, update and delete.
   * Only called if {@link #isIncludeControlPanel()} returns true.
   * By default, the control panel provided by the edit panel is returned.
   * @return the control panel for managing records
   * @see EntityEditPanel#createHorizontalControlPanel()
   * @see EntityEditPanel#createVerticalControlPanel()
   * @see EntityPanel#TOOLBAR_BUTTONS
   */
  protected JComponent createEditControlPanel() {
    int alignment = controlPanelConstraints.equals(BorderLayout.SOUTH) ||
            controlPanelConstraints.equals(BorderLayout.NORTH) ? FlowLayout.CENTER : FlowLayout.LEADING;
    if (TOOLBAR_BUTTONS.get()) {
      return editPanel.createControlToolBar(SwingConstants.VERTICAL);
    }
    if (alignment == FlowLayout.CENTER) {
      return editPanel.createHorizontalControlPanel();
    }

    return editPanel.createVerticalControlPanel();
  }

  /**
   * Returns the base panel containing the edit and table panels (north, center).
   * @return the edit and table base panel
   */
  protected final JPanel getEditControlTablePanel() {
    return editControlTablePanel;
  }

  /**
   * @param masterPanel the panel serving as master panel for this entity panel
   * @throws IllegalStateException in case a master panel has already been set
   */
  protected final void setMasterPanel(EntityPanel masterPanel) {
    requireNonNull(masterPanel, "masterPanel");
    if (this.masterPanel != null) {
      throw new IllegalStateException("Master panel has already been set for " + this);
    }
    this.masterPanel = masterPanel;
  }

  /**
   * Initializes the keyboard navigation actions.
   * CTRL-T transfers focus to the table in case one is available,
   * CTR-E transfers focus to the edit panel in case one is available,
   * CTR-S opens a select search condition panel dialog, in case one is available,
   * CTR-I opens a select input field dialog and
   * CTR-F selects the table search field
   */
  protected final void initializeKeyboardActions() {
    if (containsTablePanel()) {
      Control selectTablePanelControl = getTablePanel().getControl(EntityTablePanel.ControlCode.REQUEST_TABLE_FOCUS);
      Control selectSearchFieldControl = getTablePanel().getControl(EntityTablePanel.ControlCode.REQUEST_SEARCH_FIELD_FOCUS);
      Control selectConditionPanelAction = getTablePanel().getControl(EntityTablePanel.ControlCode.SELECT_CONDITION_PANEL);
      KeyEvents.builder(KeyEvent.VK_T)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectTablePanelControl)
              .enable(this);
      KeyEvents.builder(KeyEvent.VK_F)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectSearchFieldControl)
              .enable(this);
      if (tablePanel.getConditionPanel() != null) {
        KeyEvents.builder(KeyEvent.VK_S)
                .modifiers(CTRL_DOWN_MASK)
                .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .action(selectConditionPanelAction)
                .enable(this);
      }
      if (containsEditPanel()) {
        KeyEvents.builder(KeyEvent.VK_T)
                .modifiers(CTRL_DOWN_MASK)
                .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .action(selectTablePanelControl)
                .enable(editControlPanel);
        KeyEvents.builder(KeyEvent.VK_F)
                .modifiers(CTRL_DOWN_MASK)
                .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .action(selectSearchFieldControl)
                .enable(editControlPanel);
        if (tablePanel.getConditionPanel() != null) {
          KeyEvents.builder(KeyEvent.VK_S)
                  .modifiers(CTRL_DOWN_MASK)
                  .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                  .action(selectConditionPanelAction)
                  .enable(editControlPanel);
        }
      }
    }
    if (containsEditPanel()) {
      Control selectEditPanelControl = Control.control(this::selectEditPanel);
      Control selectInputComponentControl = Control.control(this::selectInputComponent);
      KeyEvents.builder(KeyEvent.VK_E)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectEditPanelControl)
              .enable(this);
      KeyEvents.builder(KeyEvent.VK_I)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectInputComponentControl)
              .enable(this);
      KeyEvents.builder(KeyEvent.VK_I)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(selectInputComponentControl)
              .enable(editControlPanel);
    }
  }

  protected final void initializeResizing() {
    KeyEvents.builder(VK_UP)
            .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new ResizeVerticallyAction(this, UP))
            .enable(this);
    KeyEvents.builder(VK_DOWN)
            .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new ResizeVerticallyAction(this, DOWN))
            .enable(this);
    KeyEvents.builder(VK_RIGHT)
            .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .onKeyPressed()
            .action(new ResizeHorizontallyAction(this, RIGHT))
            .enable(this);
    KeyEvents.builder(VK_LEFT)
            .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .onKeyPressed()
            .action(new ResizeHorizontallyAction(this, LEFT))
            .enable(this);
    if (containsEditPanel()) {
      KeyEvents.builder(VK_UP)
              .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new ResizeVerticallyAction(this, UP))
              .enable(editControlPanel);
      KeyEvents.builder(VK_DOWN)
              .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new ResizeVerticallyAction(this, DOWN))
              .enable(editControlPanel);
      KeyEvents.builder(VK_RIGHT)
              .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new ResizeHorizontallyAction(this, RIGHT))
              .enable(editControlPanel);
      KeyEvents.builder(VK_LEFT)
              .modifiers(ALT_DOWN_MASK | SHIFT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new ResizeHorizontallyAction(this, LEFT))
              .enable(editControlPanel);
    }
  }

  protected final void initializeNavigation() {
    KeyEvents.builder(VK_UP)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new NavigateAction(this, UP))
            .enable(this);
    KeyEvents.builder(VK_DOWN)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new NavigateAction(this, DOWN))
            .enable(this);
    KeyEvents.builder(VK_RIGHT)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new NavigateAction(this, RIGHT))
            .enable(this);
    KeyEvents.builder(VK_LEFT)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(new NavigateAction(this, LEFT))
            .enable(this);
    if (containsEditPanel()) {
      KeyEvents.builder(VK_UP)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new NavigateAction(this, UP))
              .enable(editControlPanel);
      KeyEvents.builder(VK_DOWN)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new NavigateAction(this, DOWN))
              .enable(editControlPanel);
      KeyEvents.builder(VK_RIGHT)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new NavigateAction(this, RIGHT))
              .enable(editControlPanel);
      KeyEvents.builder(VK_LEFT)
              .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(new NavigateAction(this, LEFT))
              .enable(editControlPanel);
    }
  }

  protected final void initializeEditControlPanel() {
    editPanel.initializePanel();
    editControlPanel.setMinimumSize(new Dimension(0, 0));
    int alignment = controlPanelConstraints.equals(BorderLayout.SOUTH) ||
            controlPanelConstraints.equals(BorderLayout.NORTH) ? FlowLayout.CENTER : FlowLayout.LEADING;
    JPanel propertyBase = new JPanel(Layouts.flowLayout(alignment));
    propertyBase.add(editPanel);
    editControlPanel.add(propertyBase, BorderLayout.CENTER);
    if (includeControlPanel) {
      JComponent controlPanel = createEditControlPanel();
      if (controlPanel != null) {
        editControlPanel.add(controlPanel, controlPanelConstraints);
      }
    }
  }

  private void initializeTablePanel() {
    Controls toolbarControls = Controls.controls();
    if (showToggleEditPanelControl && editPanel != null) {
      toolbarControls.add(getToggleEditPanelControl());
    }
    if (showDetailPanelControls && !detailEntityPanels.isEmpty()) {
      toolbarControls.add(getToggleDetailPanelControl());
    }
    if (!toolbarControls.isEmpty()) {
      tablePanel.addToolBarControls(toolbarControls);
    }
    if (showDetailPanelControls && !detailEntityPanels.isEmpty()) {
      tablePanel.addPopupControls(getDetailPanelControls(EMBEDDED));
    }
    if (tablePanel.getTable().getDoubleClickAction() == null) {
      tablePanel.getTable().setDoubleClickAction(initializeTableDoubleClickAction());
    }
    tablePanel.initializePanel();
    tablePanel.setMinimumSize(new Dimension(0, 0));
  }

  /**
   * Initializes the horizontal split pane, used in the case of detail panel(s)
   * @return the horizontal split pane
   */
  private JSplitPane initializeHorizontalSplitPane() {
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
    splitPane.setBorder(BorderFactory.createEmptyBorder());
    splitPane.setOneTouchExpandable(true);
    splitPane.setResizeWeight(detailSplitPanelResizeWeight);
    splitPane.setDividerSize(SPLIT_PANE_DIVIDER_SIZE.get());

    return splitPane;
  }

  /**
   * Initializes the JTabbedPane containing the detail panels, used in case of multiple detail panels
   * @return the JTabbedPane for holding detail panels
   */
  private JTabbedPane initializeDetailTabPane() {
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setFocusable(false);
    for (EntityPanel detailPanel : detailEntityPanels) {
      tabbedPane.addTab(detailPanel.caption, detailPanel);
    }
    tabbedPane.addChangeListener(e -> getTabbedDetailPanel().activatePanel());
    if (showDetailPanelControls) {
      tabbedPane.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
          if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            setDetailPanelState(getDetailPanelState() == WINDOW ? EMBEDDED : WINDOW);
          }
          else if (e.getButton() == MouseEvent.BUTTON2) {
            setDetailPanelState(getDetailPanelState() == EMBEDDED ? HIDDEN : EMBEDDED);
          }
        }
      });
    }

    return tabbedPane;
  }

  /**
   * Initialize the Control to trigger when a double click is performed on the table, if a table is present.
   * The default implementation shows the edit panel in a window if one is available and hidden, if that is
   * not the case and the detail panels are hidden those are shown in a window.
   * @return the Control to trigger when a double click is performed on the table
   */
  private Control initializeTableDoubleClickAction() {
    return Control.control(() -> {
      if (containsEditPanel() || (!detailEntityPanels.isEmpty() && includeDetailTabPane)) {
        if (containsEditPanel() && getEditPanelState() == HIDDEN) {
          setEditPanelState(WINDOW);
        }
        else if (getDetailPanelState() == HIDDEN) {
          setDetailPanelState(WINDOW);
        }
      }
    });
  }

  /**
   * Initializes Controls containing a control for setting the state to {@code status} on each detail panel.
   * @param status the status
   * @return Controls for controlling the state of the detail panels
   */
  private Controls getDetailPanelControls(PanelState status) {
    if (detailEntityPanels.isEmpty()) {
      return null;
    }

    Controls.Builder controls = Controls.builder()
            .caption(MESSAGES.getString(MSG_DETAIL_TABLES))
            .smallIcon(frameworkIcons().detail());
    detailEntityPanels.forEach(detailPanel ->
            controls.control(Control.builder(createDetailPanelCommand(status, detailPanel))
                    .caption(detailPanel.getCaption())));

    return controls.build();
  }

  //#############################################################################################
  // End - initialization methods
  //#############################################################################################

  private void selectEditPanel() {
    if (getEditPanelState() == HIDDEN) {
      setEditPanelState(EMBEDDED);
    }
    getEditPanel().requestInitialFocus();
  }

  private void selectInputComponent() {
    if (getEditPanelState() == HIDDEN) {
      setEditPanelState(EMBEDDED);
    }
    getEditPanel().selectInputComponent();
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
    Windows.getParentWindow(this).ifPresent(parent -> {
      Dimension parentSize = parent.getSize();
      Dimension size = getDetailWindowSize(parentSize);
      Point parentLocation = parent.getLocation();
      Point location = new Point(parentLocation.x + (parentSize.width - size.width),
              parentLocation.y + (parentSize.height - size.height) - DETAIL_WINDOW_OFFSET);
      detailPanelWindow = createDetailPanelWindow();
      detailPanelWindow.setSize(size);
      detailPanelWindow.setLocation(location);
      detailPanelWindow.setVisible(true);
    });
  }

  /**
   * @param parentSize the size of the parent window
   * @return the size to use when showing the detail window
   */
  private Dimension getDetailWindowSize(Dimension parentSize) {
    return new Dimension((int) (parentSize.width / DETAIL_WINDOW_SIZE_RATIO), containsEditPanel() ?
            (int) (parentSize.height / DETAIL_WINDOW_SIZE_RATIO) : parentSize.height - DETAIL_WINDOW_HEIGHT_OFFSET);
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
  private EntityPanel getTabbedDetailPanel() {
    if (detailPanelTabbedPane == null) {
      throw new IllegalStateException("No detail panels available");
    }

    return (EntityPanel) detailPanelTabbedPane.getSelectedComponent();
  }

  private Window createEditWindow() {
    if (DISPLAY_PANELS_IN_FRAME.get()) {
      return Windows.frameBuilder(editControlPanel)
              .relativeTo(this)
              .title(caption)
              .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
              .onClosed(windowEvent -> setEditPanelState(HIDDEN))
              .build();
    }
    else {
      return Dialogs.componentDialog(editControlPanel)
              .owner(this)
              .title(caption)
              .modal(false)
              .disposeOnEscape(disposeEditDialogOnEscape)
              .onClosed(e -> setEditPanelState(HIDDEN))
              .build();
    }
  }

  private Window createDetailPanelWindow() {
    if (DISPLAY_PANELS_IN_FRAME.get()) {
      return Windows.frameBuilder(detailPanelTabbedPane)
              .title(caption + " - " + MESSAGES.getString(MSG_DETAIL_TABLES))
              .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
              .onClosed(windowEvent -> {
                //the frame can be closed when embedding the panel, don't hide if that's the case
                if (getDetailPanelState() != EMBEDDED) {
                  setDetailPanelState(HIDDEN);
                }
              })
              .build();
    }
    else {
      return Dialogs.componentDialog(detailPanelTabbedPane)
              .owner(this)
              .title(caption + " - " + MESSAGES.getString(MSG_DETAIL_TABLES))
              .modal(false)
              .onClosed(e -> {
                //the dialog can be closed when embedding the panel, don't hide if that's the case
                if (getDetailPanelState() != EMBEDDED) {
                  setDetailPanelState(HIDDEN);
                }
              })
              .build();
    }
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

  private Control.Command createDetailPanelCommand(PanelState status, EntityPanel detailPanel) {
    return () -> {
      setDetailPanelState(status);
      detailPanel.activatePanel();
    };
  }

  private void bindEvents() {
    addComponentListener(new EntityPanelComponentAdapter());
//    if (containsEditPanel() && editPanel.containsControl(ControlCode.REFRESH)) {
//      final Control refreshControl = editPanel.getControl(ControlCode.REFRESH);
//      getTableModel().getTableConditionModel().getConditionChangedObserver().addDataListener(changed -> {
//        refreshControl.setForeground(changed ? Color.RED.darker() : UIManager.getColor("Button.foreground"));
//        refreshControl.setSmallIcon(changed ? FrameworkIcons.frameworkIcons().refreshRequired() : FrameworkIcons.frameworkIcons().refresh());
//      });
//    }
  }

  private void checkIfInitialized() {
    if (panelInitialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
  }

  private static final class NavigateAction extends AbstractAction {

    private final EntityPanel entityPanel;
    private final Direction direction;

    private NavigateAction(EntityPanel entityPanel, Direction direction) {
      super("Navigate " + direction);
      this.entityPanel = entityPanel;
      this.direction = direction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      switch (direction) {
        case LEFT:
          entityPanel.getPreviousSiblingPanel()
                  .ifPresent(HierarchyPanel::activatePanel);
          break;
        case RIGHT:
          entityPanel.getNextSiblingPanel()
                  .ifPresent(HierarchyPanel::activatePanel);
          break;
        case UP:
          entityPanel.getParentPanel()
                  .ifPresent(HierarchyPanel::activatePanel);
          break;
        case DOWN:
          if (entityPanel.getDetailPanelState() == HIDDEN) {
            entityPanel.setDetailPanelState(EMBEDDED);
          }
          entityPanel.getSelectedChildPanel()
                  .ifPresent(HierarchyPanel::activatePanel);
          break;
        default:
          throw new IllegalArgumentException("Unknown direction: " + direction);
      }
    }
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
      if (panel.masterPanel != null) {
        panel.masterPanel.resizePanel(direction, RESIZE_AMOUNT);
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
      EntityEditPanel editPanelParent = Utilities.getParentOfType(EntityEditPanel.class, (Component) changeEvent.getNewValue()).orElse(null);
      if (editPanelParent != null) {
        editPanelParent.setActive(true);
      }
      else {
        Utilities.getParentOfType(EntityPanel.class, (Component) changeEvent.getNewValue()).ifPresent(parent -> {
          if (parent.getEditPanel() != null) {
            parent.getEditPanel().setActive(true);
          }
        });
      }
    }
  }

  private final class EntityPanelComponentAdapter extends ComponentAdapter {
    @Override
    public void componentHidden(ComponentEvent e) {
      SwingUtilities.invokeLater(() -> setFilterPanelsVisible(false));
    }
    @Override
    public void componentShown(ComponentEvent e) {
      SwingUtilities.invokeLater(() -> setFilterPanelsVisible(true));
    }
  }

  /**
   * A class providing EntityPanel instances.
   */
  public interface Builder {

    /**
     * @return the entityType
     */
    EntityType getEntityType();

    /**
     * @return the SwingEntityModel.Builder this panel builder is based on
     */
    SwingEntityModel.Builder getModelBuilder();

    /**
     * @return true if this panel builder contains an instantiated model
     */
    boolean containsModel();

    /**
     * @param caption the panel caption
     * @return this EntityPanel.Builder instance
     */
    Builder caption(String caption);

    /**
     * @return the caption to use when the EntityPanel is shown in f.x. menus
     */
    String getCaption();

    /**
     * Adds the given detail panel builder to this panel builder, if it hasn't been previously added
     * @param panelBuilder the detail panel provider
     * @return this EntityPanel.Builder instance
     */
    Builder detailPanelBuilder(EntityPanel.Builder panelBuilder);

    /**
     * @param refreshOnInit if true then the data model this panel is based on will be refreshed when
     * the panel is initialized
     * @return this EntityPanel.Builder instance
     */
    Builder refreshOnInit(boolean refreshOnInit);

    /**
     * @param tableConditionPanelVisible if true then the table condition panel is made visible when the panel is initialized
     * @return this EntityPanel.Builder instance
     */
    Builder tableConditionPanelVisible(boolean tableConditionPanelVisible);

    /**
     * @param detailPanelState the state of the detail panels when this panel is initialized
     * @return this EntityPanel.Builder instance
     */
    Builder detailPanelState(PanelState detailPanelState);

    /**
     * @param detailSplitPanelResizeWeight the split panel resize weight to use when initializing this panel
     * with its detail panels
     * @return this EntityPanel.Builder instance
     */
    Builder detailSplitPanelResizeWeight(double detailSplitPanelResizeWeight);

    /**
     * Note that setting the EntityPanel class overrides any table panel or edit panel classes that have been set.
     * @param panelClass the EntityPanel class to use when providing this panel
     * @return this EntityPanel.Builder instance
     */
    Builder panelClass(Class<? extends EntityPanel> panelClass);

    /**
     * @param editPanelClass the EntityEditPanel class to use when providing this panel
     * @return this EntityPanel.Builder instance
     */
    Builder editPanelClass(Class<? extends EntityEditPanel> editPanelClass);

    /**
     * @param tablePanelClass the EntityTablePanel class to use when providing this panel
     * @return this EntityPanel.Builder instance
     */
    Builder tablePanelClass(Class<? extends EntityTablePanel> tablePanelClass);

    /**
     * @param panelInitializer initializes the panel post construction
     * @return this EntityPanel.Builder instance
     */
    Builder panelInitializer(Consumer<EntityPanel> panelInitializer);

    /**
     * @param editPanelInitializer initializes the edit panel post construction
     * @return this EntityPanel.Builder instance
     */
    Builder editPanelInitializer(Consumer<EntityEditPanel> editPanelInitializer);

    /**
     * @param tablePanelInitializer initializes the table panel post construction
     * @return this EntityPanel.Builder instance
     */
    Builder tablePanelInitializer(Consumer<EntityTablePanel> tablePanelInitializer);

    /**
     * Creates an EntityPanel based on this provider configuration,
     * assuming the underlying model is available.
     * @return an EntityPanel based on this provider configuration
     * @see #containsModel()
     */
    EntityPanel buildPanel();

    /**
     * Creates an EntityPanel based on this provider configuration
     * @param connectionProvider the connection provider
     * @return an EntityPanel based on this provider configuration
     */
    EntityPanel buildPanel(EntityConnectionProvider connectionProvider);

    /**
     * Creates an EntityPanel based on this provider configuration
     * @param model the EntityModel to base this panel on
     * @return an EntityPanel based on this provider configuration
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

    /**
     * Creates a new Action which shows the edit panel provided by this panel builder and if an insert is performed
     * adds the new entity to the {@code comboBox} and selects it.
     * @param comboBox the combo box in which to select the new entity, if created
     * @return the Action
     */
    Action createEditPanelAction(EntityComboBox comboBox);

    /**
     * Creates a new Action which shows the edit panel provided by this panel builder and if an insert is performed
     * selects the new entity in the {@code searchField}.
     * @param searchField the search field in which to select the new entity, if created
     * @return the Action
     */
    Action createEditPanelAction(EntitySearchField searchField);

    /**
     * Creates a new Action which shows the edit panel provided by this panel builder and if an insert is performed
     * {@code insertListener} is notified.
     * @param component this component used as dialog parent, receives the focus after insert
     * @param connectionProvider the connection provider
     * @param insertListener the listener notified when insert has been performed
     * @return the Action
     */
    Action createEditPanelAction(JComponent component, EntityConnectionProvider connectionProvider,
                                 EventDataListener<List<Entity>> insertListener);
  }
}
