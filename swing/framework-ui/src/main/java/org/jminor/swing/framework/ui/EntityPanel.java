/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Configuration;
import org.jminor.common.value.PropertyValue;
import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.HierarchyPanel;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.common.ui.dialog.DisposeOnEscape;
import org.jminor.swing.common.ui.dialog.Modal;
import org.jminor.swing.common.ui.layout.Layouts;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
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
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.util.Objects.requireNonNull;
import static org.jminor.swing.common.ui.KeyEvents.addKeyEvent;
import static org.jminor.swing.framework.ui.EntityPanel.Direction.*;
import static org.jminor.swing.framework.ui.EntityPanel.PanelState.*;
import static org.jminor.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;

/**
 * A panel representing a Entity via a EntityModel, which facilitates browsing and editing of records.
 * <pre>
 *   String entityId = ...;
 *   EntityConnectionProvider connectionProvider = ...;
 *   SwingEntityModel entityModel = new SwingEntityModel(entityId, connectionProvider);
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
   * @see org.jminor.swing.framework.ui.EntityEditPanel#ALL_PANELS_ACTIVE
   */
  public static final PropertyValue<Boolean> USE_FOCUS_ACTIVATION = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityPanel.useFocusActivation", true);

  /**
   * Indicates whether keyboard navigation will be enabled<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> USE_KEYBOARD_NAVIGATION = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityPanel.useKeyboardNavigation", true);

  /**
   * Indicates whether dialogs opened by child panels in the application should be centered
   * on their respective parent panel or the application frame/dialog.
   * This applies to edit panels.
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> CENTER_APPLICATION_DIALOGS = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityPanel.centerApplicationDialogs", false);

  /**
   * Indicates whether entity edit panel dialogs should be closed on escape<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> DISPOSE_EDIT_DIALOG_ON_ESCAPE = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityPanel.disposeEditDialogOnEscape", true);

  /**
   * Specifies whether or not a control for toggling the edit panel is available to the user<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> SHOW_TOGGLE_EDIT_PANEL_CONTROL = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityPanel.showToggleEditPanelControl", true);

  /**
   * Specifies whether or not actions to hide detail panels or show them in a dialog are available to the user<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> SHOW_DETAIL_PANEL_CONTROLS = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityPanel.showDetailPanelControls", true);

  /**
   * Specifies the default size of the divider for detail panel split panes.<br>
   * Value type: Integer<br>
   * Default value: 18<br>
   */
  public static final PropertyValue<Integer> SPLIT_PANE_DIVIDER_SIZE = Configuration.integerValue(
          "org.jminor.swing.framework.ui.EntityPanel.splitPaneDividerSize", DEFAULT_SPLIT_PANE_DIVIDER_SIZE);

  /**
   * Indicates whether entity panels containing detail panels should by default be laid out in a compact manner<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> COMPACT_ENTITY_PANEL_LAYOUT = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityPanel.compactEntityPanelLayout", true);

  /**
   * Specifies whether the action buttons (Save, update, delete, clear, refresh) should be on a toolbar<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> TOOLBAR_BUTTONS = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityPanel.toolbarButtons", false);

  /**
   * The possible states of a detail panel.
   */
  public enum PanelState {
    DIALOG, EMBEDDED, HIDDEN
  }

  /**
   * The navigation and resizing directions.
   */
  enum Direction {
    UP, DOWN, RIGHT, LEFT
  }

  private static final int RESIZE_AMOUNT = 30;
  private static final double DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT = 0.5;
  private static final int DETAIL_DIALOG_OFFSET = 29;
  private static final double DETAIL_DIALOG_SIZE_RATIO = 1.5;
  private static final int DETAIL_DIALOG_HEIGHT_OFFSET = 54;

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
   * The base edit panel which contains the controls required for editing a entity
   */
  private final JPanel editControlPanel = new JPanel(Layouts.borderLayout());

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
   * A base panel used in case this EntityPanel is in a compact configuration
   */
  private JPanel compactBase;

  /**
   * The dialog used when detail panels are undocked
   */
  private JDialog detailPanelDialog;

  /**
   * The dialog used when the edit panel is undocked
   */
  private JDialog editPanelDialog;

  /**
   * true if this panel should use a compact layout
   */
  private boolean compactDetailLayout = COMPACT_ENTITY_PANEL_LAYOUT.get();

  /**
   * indicates where the control panel should be placed in a BorderLayout
   */
  private String controlPanelConstraints = TOOLBAR_BUTTONS.get() ? BorderLayout.WEST : BorderLayout.EAST;

  /**
   * Holds the current state of the edit panel (HIDDEN, EMBEDDED or DIALOG)
   */
  private PanelState editPanelState = EMBEDDED;

  /**
   * Holds the current state of the detail panels (HIDDEN, EMBEDDED or DIALOG)
   */
  private PanelState detailPanelState = EMBEDDED;

  /**
   * if true then the edit control panel should be included
   */
  private boolean includeControlPanel = true;

  /**
   * if true and detail panels are available then the detail panel tab pane should be included
   */
  private boolean includeDetailPanelTabPane = true;

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
  public EntityPanel(final SwingEntityModel entityModel) {
    this(entityModel, null, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.getTableModel()) : null);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   */
  public EntityPanel(final SwingEntityModel entityModel, final EntityEditPanel editPanel) {
    this(entityModel, editPanel, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.getTableModel()) : null);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param tablePanel the table panel
   */
  public EntityPanel(final SwingEntityModel entityModel, final EntityTablePanel tablePanel) {
    this(entityModel, null, tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   * @param tablePanel the table panel
   */
  public EntityPanel(final SwingEntityModel entityModel, final EntityEditPanel editPanel, final EntityTablePanel tablePanel) {
    requireNonNull(entityModel, ENTITY_MODEL_PARAM);
    this.entityModel = entityModel;
    this.caption = entityModel.getEditModel().getEntityDefinition().getCaption();
    this.editPanel = editPanel;
    this.tablePanel = tablePanel;
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
   * @return this entity panel
   * @throws IllegalStateException if the panel has been initialized
   * @throws IllegalArgumentException in case the given constraint is not one of BorderLayout.SOUTH, NORTH, EAST or WEST
   */
  public final EntityPanel setControlPanelConstraints(final String controlPanelConstraints) {
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
    return this;
  }

  /**
   * @return true if this entity panel is using a compact detail layout
   */
  public final boolean isCompactDetailLayout() {
    return compactDetailLayout;
  }

  /**
   * @param compactDetailLayout true if this panel and its detail panels should be laid out in a compact state
   * @return this EntityPanel instance
   */
  public final EntityPanel setCompactDetailLayout(final boolean compactDetailLayout) {
    if (detailEntityPanels.isEmpty()) {
      throw new IllegalArgumentException("This panel contains no detail panels, compact detail layout not available");
    }
    this.compactDetailLayout = compactDetailLayout;
    return this;
  }

  /**
   * @param detailPanels the detail panels
   * @return this entity panel
   */
  public final EntityPanel addDetailPanels(final EntityPanel... detailPanels) {
    requireNonNull(detailPanels, "detailPanels");
    for (final EntityPanel detailPanel : detailPanels) {
      addDetailPanel(detailPanel);
    }

    return this;
  }

  /**
   * Adds the given detail panel, and adds the detail model to the underlying
   * model if it does not contain it already, and then sets {@code includeDetailPanelTabPane}
   * to true
   * @param detailPanel the detail panel to add
   * @return this entity panel
   * @throws IllegalStateException if the panel has been initialized
   */
  public final EntityPanel addDetailPanel(final EntityPanel detailPanel) {
    checkIfInitialized();
    detailPanel.setMasterPanel(this);
    detailEntityPanels.add(detailPanel);

    return this;
  }

  /**
   * Initializes this EntityPanels UI, in case of some specific initialization code you can override the
   * {@code initialize()} method and add your code there.
   * This method marks this panel as initialized which prevents it from running again, whether or not an exception occurs.
   * @return this EntityPanel instance
   * @see #initialize()
   * @see #isPanelInitialized()
   */
  public final EntityPanel initializePanel() {
    if (!panelInitialized) {
      try {
        Components.showWaitCursor(this);
        initializeAssociatedPanels();
        initializeControlPanels();
        bindEvents();
        initializeUI();
        initialize();
      }
      finally {
        panelInitialized = true;
        Components.hideWaitCursor(this);
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
    final Collection<SwingEntityModel> linkedDetailModels = entityModel.getLinkedDetailModels();

    return detailEntityPanels.stream().filter(detailPanel ->
            linkedDetailModels.contains(detailPanel.entityModel)).collect(Collectors.toList());
  }

  /**
   * Returns the detail panel for the given {@code entityId}, if one is available
   * @param entityId the entity ID of the detail panel to retrieve
   * @return the detail panel of the given type
   * @throws IllegalArgumentException in case the panel was not found
   */
  public final EntityPanel getDetailPanel(final String entityId) {
    for (final EntityPanel detailPanel : detailEntityPanels) {
      if (detailPanel.entityModel.getEntityId().equals(entityId)) {
        return detailPanel;
      }
    }

    throw new IllegalArgumentException("Detail panel for entity: " + entityId + " not found in panel: " + getClass());
  }

  /**
   * Returns true if this panel contains a detail panel for the given {@code entityId}
   * @param entityId the entityId
   * @return true if a detail panel for the given entityId is found
   */
  public final boolean containsDetailPanel(final String entityId) {
    return detailEntityPanels.stream().anyMatch(detailPanel -> detailPanel.entityModel.getEntityId().equals(entityId));
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + caption;
  }

  /**
   * Sets the caption to use when this panel is displayed.
   * @param caption the caption
   */
  public final void setCaption(final String caption) {
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
    if (getParentPanel() != null) {
      getParentPanel().setSelectedChildPanel(this);
    }
    initializePanel();
    requestInitialFocus();
  }

  @Override
  public final HierarchyPanel getParentPanel() {
    HierarchyPanel parentPanel = masterPanel;
    if (parentPanel == null) {
      parentPanel = Components.getParentOfType(this, HierarchyPanel.class);
    }

    return parentPanel;
  }

  @Override
  public final EntityPanel getSelectedChildPanel() {
    final Collection<EntityPanel> linkedDetailPanels = getLinkedDetailPanels();
    if (!linkedDetailPanels.isEmpty()) {
      return linkedDetailPanels.iterator().next();
    }

    return null;
  }

  @Override
  public final void setSelectedChildPanel(final HierarchyPanel childPanel) {
    if (detailPanelTabbedPane != null) {
      detailPanelTabbedPane.setSelectedComponent((JComponent) childPanel);
      for (final SwingEntityModel linkedModel : new ArrayList<>(entityModel.getLinkedDetailModels())) {
        entityModel.removeLinkedDetailModel(linkedModel);
      }
      entityModel.addLinkedDetailModel(getTabbedDetailPanel().getModel());
    }
  }

  @Override
  public final HierarchyPanel getPreviousSiblingPanel() {
    if (getParentPanel() == null) {//no parent, no siblings
      return null;
    }
    final List<? extends HierarchyPanel> siblingPanels = getParentPanel().getChildPanels();
    if (siblingPanels.contains(this)) {
      final int index = siblingPanels.indexOf(this);
      if (index == 0) {//wrap around
        return siblingPanels.get(siblingPanels.size() - 1);
      }

      return siblingPanels.get(index - 1);
    }

    return null;
  }

  @Override
  public final HierarchyPanel getNextSiblingPanel() {
    if (getParentPanel() == null) {//no parent, no siblings
      return null;
    }
    final List<? extends HierarchyPanel> siblingPanels = getParentPanel().getChildPanels();
    if (siblingPanels.contains(this)) {
      final int index = siblingPanels.indexOf(this);
      if (index == siblingPanels.size() - 1) {//wrap around
        return siblingPanels.get(0);
      }

      return siblingPanels.get(index + 1);
    }

    return null;
  }

  @Override
  public final List<HierarchyPanel> getChildPanels() {
    return Collections.unmodifiableList(detailEntityPanels);
  }

  /**
   * @return a control for toggling the edit panel
   */
  public final Control getToggleEditPanelControl() {
    final Control toggle = Controls.control(this::toggleEditPanelState, frameworkIcons().editPanel());
    toggle.setDescription(MESSAGES.getString("toggle_edit"));

    return toggle;
  }

  /**
   * @return a control for toggling the detail panel
   */
  public final Control getToggleDetailPanelControl() {
    final Control toggle = Controls.control(this::toggleDetailPanelState, frameworkIcons().detail());
    toggle.setDescription(MESSAGES.getString("toggle_detail"));

    return toggle;
  }

  /**
   * Displays the exception in a dialog
   * @param exception the exception to handle
   * @see DefaultDialogExceptionHandler
   */
  public final void displayException(final Exception exception) {
    DefaultDialogExceptionHandler.getInstance().displayException(exception, Windows.getParentWindow(this));
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
   * @return this entity panel
   * @throws IllegalStateException if the panel has been initialized
   */
  public final EntityPanel setDetailSplitPanelResizeWeight(final double detailSplitPanelResizeWeight) {
    checkIfInitialized();
    this.detailSplitPanelResizeWeight = detailSplitPanelResizeWeight;
    return this;
  }

  /**
   * @return true if the detail panel tab pane should be included
   */
  public final boolean isIncludeDetailPanelTabPane() {
    return includeDetailPanelTabPane;
  }

  /**
   * @param includeDetailPanelTabPane true if the detail panel tab pane should be included
   * @return this entity panel
   * @throws IllegalStateException if the panel has been initialized
   */
  public final EntityPanel setIncludeDetailPanelTabPane(final boolean includeDetailPanelTabPane) {
    checkIfInitialized();
    this.includeDetailPanelTabPane = includeDetailPanelTabPane;
    return this;
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
   * @return this EntityPane instance
   * @throws IllegalStateException if the panel has been initialized
   */
  public final EntityPanel setShowToggleEditPanelControl(final boolean showToggleEditPanelControl) {
    checkIfInitialized();
    this.showToggleEditPanelControl = showToggleEditPanelControl;
    return this;
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
   * @return this EntityPane instance
   * @throws IllegalStateException if the panel has been initialized
   */
  public final EntityPanel setShowDetailPanelControls(final boolean showDetailPanelControls) {
    checkIfInitialized();
    this.showDetailPanelControls = showDetailPanelControls;
    return this;
  }

  /**
   * @return true if the control panel should be included
   */
  public final boolean isIncludeControlPanel() {
    return includeControlPanel;
  }

  /**
   * @param includeControlPanel true if the control panel should be included
   * @return this EntityPane instance
   * @throws IllegalStateException if the panel has been initialized
   */
  public final EntityPanel setIncludeControlPanel(final boolean includeControlPanel) {
    checkIfInitialized();
    this.includeControlPanel = includeControlPanel;
    return this;
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
  public final void setDisposeEditDialogOnEscape(final boolean disposeEditDialogOnEscape) {
    this.disposeEditDialogOnEscape = disposeEditDialogOnEscape;
  }

  /**
   * @return true if keyboard navigation is enabled
   */
  public boolean isUseKeyboardNavigation() {
    return useKeyboardNavigation;
  }

  /**
   * @param useKeyboardNavigation true if keyboard navigation should be enabled
   */
  public void setUseKeyboardNavigation(final boolean useKeyboardNavigation) {
    checkIfInitialized();
    this.useKeyboardNavigation = useKeyboardNavigation;
  }

  /**
   * Toggles the detail panel state between DIALOG, HIDDEN and EMBEDDED
   */
  public final void toggleDetailPanelState() {
    if (detailPanelState == DIALOG) {
      setDetailPanelState(HIDDEN);
    }
    else if (detailPanelState == EMBEDDED) {
      setDetailPanelState(DIALOG);
    }
    else {
      setDetailPanelState(EMBEDDED);
    }
  }

  /**
   * Toggles the edit panel state between DIALOG, HIDDEN and EMBEDDED
   */
  public final void toggleEditPanelState() {
    if (editPanelState == DIALOG) {
      setEditPanelState(HIDDEN);
    }
    else if (editPanelState == EMBEDDED) {
      setEditPanelState(DIALOG);
    }
    else {
      setEditPanelState(EMBEDDED);
    }
  }

  /**
   * @return the detail panel state, either HIDDEN, EMBEDDED or DIALOG
   */
  public final PanelState getDetailPanelState() {
    return detailPanelState;
  }

  /**
   * @return the edit panel state, either HIDDEN, EMBEDDED or DIALOG
   */
  public final PanelState getEditPanelState() {
    return editPanelState;
  }

  /**
   * @param state the detail panel state (HIDDEN or EMBEDDED, DIALOG)
   */
  public final void setDetailPanelState(final PanelState state) {
    if (detailPanelTabbedPane == null) {
      this.detailPanelState = state;
      return;
    }

    if (state != HIDDEN) {
      getTabbedDetailPanel().initializePanel();
    }

    if (detailPanelState == DIALOG) {//if we are leaving the DIALOG state, hide all child detail dialogs
      for (final EntityPanel detailPanel : detailEntityPanels) {
        if (detailPanel.detailPanelState == DIALOG) {
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
    if (state != DIALOG) {
      disposeDetailDialog();
    }

    if (state == EMBEDDED) {
      horizontalSplitPane.setRightComponent(detailPanelTabbedPane);
    }
    else if (state == HIDDEN) {
      horizontalSplitPane.setRightComponent(null);
    }
    else {
      showDetailDialog();
    }

    revalidate();
  }

  /**
   * @param state the edit panel state, either HIDDEN, EMBEDDED or DIALOG
   */
  public final void setEditPanelState(final PanelState state) {
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
  public final void setFilterPanelsVisible(final boolean visible) {
    if (!panelInitialized) {
      return;
    }

    if (containsTablePanel()) {
      tablePanel.getTable().setFilterPanelsVisible(visible);
    }
    for (final EntityPanel detailEntityPanel : detailEntityPanels) {
      detailEntityPanel.setFilterPanelsVisible(visible);
    }
  }

  /**
   * Resizes this panel in the given direction
   * @param direction the resize direction
   * @param pixelAmount the resize amount
   */
  public final void resizePanel(final Direction direction, final int pixelAmount) {
    switch (direction) {
      case UP:
        setEditPanelState(HIDDEN);
        break;
      case DOWN:
        if (editPanelState == EMBEDDED) {
          setEditPanelState(DIALOG);
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

  //#############################################################################################
  // Begin - initialization methods
  //#############################################################################################

  /**
   * Initializes this EntityPanels UI.
   *<pre>
   * The default layout is as follows:
   * __________________________________
   * |      edit panel        |control|
   * |   (EntityEditPanel)    | panel | } edit control panel
   * |________________________|_______|
   * |                  |             |
   * |   table panel    |   detail    |
   * |(EntityTablePanel)|   panel     |
   * |                  |             |
   * |__________________|_____________|
   *
   * or in case of compact layout:
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
   */
  protected void initializeUI() {
    if (editPanel != null) {
      initializeEditControlPanel();
    }
    if (tablePanel != null) {
      initializeTablePanel();
    }
    if (!includeDetailPanelTabPane || detailEntityPanels.isEmpty()) {
      horizontalSplitPane = null;
      detailPanelTabbedPane = null;
    }
    else {
      horizontalSplitPane = initializeHorizontalSplitPane();
      detailPanelTabbedPane = initializeDetailTabPane();
    }
    setLayout(Layouts.borderLayout());
    if (detailPanelTabbedPane != null || tablePanel != null) {
      initializeDetailAndTablePanels();
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
   * Only called if {@link #includeControlPanel} returns true.
   * By default the control panel provided by the edit panel is returned.
   * @return the control panel for managing records
   * @see EntityEditPanel#createControlPanel(boolean)
   * @see EntityEditPanel#createControlToolBar(int)
   * @see EntityPanel#TOOLBAR_BUTTONS
   */
  protected JComponent createEditControlPanel() {
    final int alignment = controlPanelConstraints.equals(BorderLayout.SOUTH) ||
            controlPanelConstraints.equals(BorderLayout.NORTH) ? FlowLayout.CENTER : FlowLayout.LEADING;
    return TOOLBAR_BUTTONS.get() ?
            editPanel.createControlToolBar(JToolBar.VERTICAL) : editPanel.createControlPanel(alignment == FlowLayout.CENTER);
  }

  /**
   * @param masterPanel the panel serving as master panel for this entity panel
   * @throws IllegalStateException in case a master panel has already been set
   */
  protected final void setMasterPanel(final EntityPanel masterPanel) {
    requireNonNull(masterPanel, "masterPanel");
    if (this.masterPanel != null) {
      throw new IllegalStateException("Master panel has already been set for " + this);
    }
    this.masterPanel = masterPanel;
  }

  /**
   * Initializes the keyboard navigation actions.
   * By default CTRL-T transfers focus to the table in case one is available,
   * CTR-E transfers focus to the edit panel in case one is available,
   * CTR-S transfers focus to the condition panel, CTR-C opens a select control dialog
   * and CTR-F selects the table search field
   */
  protected final void initializeKeyboardActions() {
    final Control selectEditPanelControl =
            Controls.control(this::selectEditPanel, "EntityPanel.selectEditPanel");
    final Control selectInputComponentControl =
            Controls.control(this::selectInputComponent, "EntityPanel.selectInputComponent");
    final Control selectTablePanelControl =
            Controls.control(getTablePanel().getTable()::requestFocus, "EntityPanel.selectTablePanel");
    final Control selectSearchFieldControl =
            Controls.control(getTablePanel().getTable().getSearchField()::requestFocus, "EntityPanel.selectSearchField");
    if (containsTablePanel()) {
      final Control selectConditionPanelAction =
              Controls.control(getTablePanel()::selectConditionPanel, "EntityPanel.selectConditionPanel");
      addKeyEvent(this, KeyEvent.VK_T, CTRL_DOWN_MASK, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              selectTablePanelControl);
      addKeyEvent(this, KeyEvent.VK_F, CTRL_DOWN_MASK, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              selectSearchFieldControl);
      if (tablePanel.getConditionPanel() != null) {
        addKeyEvent(this, KeyEvent.VK_S, CTRL_DOWN_MASK, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                selectConditionPanelAction);
      }
      if (containsEditPanel()) {
        addKeyEvent(editControlPanel, KeyEvent.VK_T, CTRL_DOWN_MASK, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                selectTablePanelControl);
        addKeyEvent(editControlPanel, KeyEvent.VK_F, CTRL_DOWN_MASK, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                selectSearchFieldControl);
        if (tablePanel.getConditionPanel() != null) {
          addKeyEvent(editControlPanel, KeyEvent.VK_S, CTRL_DOWN_MASK, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                  selectConditionPanelAction);
        }
      }
    }
    if (containsEditPanel()) {
      addKeyEvent(this, KeyEvent.VK_E, CTRL_DOWN_MASK, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              selectEditPanelControl);
      addKeyEvent(this, KeyEvent.VK_I, CTRL_DOWN_MASK, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              selectInputComponentControl);
      addKeyEvent(editControlPanel, KeyEvent.VK_I, CTRL_DOWN_MASK, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              selectInputComponentControl);
    }
  }

  protected final void initializeResizing() {
    addKeyEvent(this, VK_UP, ALT_DOWN_MASK + SHIFT_DOWN_MASK,
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeVerticallyAction(this, UP));
    addKeyEvent(this, VK_DOWN, ALT_DOWN_MASK + SHIFT_DOWN_MASK,
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeVerticallyAction(this, DOWN));
    addKeyEvent(this, VK_RIGHT, ALT_DOWN_MASK + SHIFT_DOWN_MASK,
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeHorizontallyAction(this, RIGHT));
    addKeyEvent(this, VK_LEFT, ALT_DOWN_MASK + SHIFT_DOWN_MASK,
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeHorizontallyAction(this, LEFT));
    if (containsEditPanel()) {
      addKeyEvent(editControlPanel, VK_UP, ALT_DOWN_MASK + SHIFT_DOWN_MASK,
              WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeVerticallyAction(this, UP));
      addKeyEvent(editControlPanel, VK_DOWN, ALT_DOWN_MASK + SHIFT_DOWN_MASK,
              WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeVerticallyAction(this, DOWN));
      addKeyEvent(editControlPanel, VK_RIGHT, ALT_DOWN_MASK + SHIFT_DOWN_MASK,
              WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeHorizontallyAction(this, RIGHT));
      addKeyEvent(editControlPanel, VK_LEFT, ALT_DOWN_MASK + SHIFT_DOWN_MASK,
              WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeHorizontallyAction(this, LEFT));
    }
  }

  protected final void initializeNavigation() {
    addKeyEvent(this, VK_UP, ALT_DOWN_MASK + CTRL_DOWN_MASK,
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, UP));
    addKeyEvent(this, VK_DOWN, ALT_DOWN_MASK + CTRL_DOWN_MASK,
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, DOWN));
    addKeyEvent(this, VK_RIGHT, ALT_DOWN_MASK + CTRL_DOWN_MASK,
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, RIGHT));
    addKeyEvent(this, VK_LEFT, ALT_DOWN_MASK + CTRL_DOWN_MASK,
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, LEFT));
    if (containsEditPanel()) {
      addKeyEvent(editControlPanel, VK_UP, ALT_DOWN_MASK + CTRL_DOWN_MASK,
              WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, UP));
      addKeyEvent(editControlPanel, VK_DOWN, ALT_DOWN_MASK + CTRL_DOWN_MASK,
              WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, DOWN));
      addKeyEvent(editControlPanel, VK_RIGHT, ALT_DOWN_MASK + CTRL_DOWN_MASK,
              WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, RIGHT));
      addKeyEvent(editControlPanel, VK_LEFT, ALT_DOWN_MASK + CTRL_DOWN_MASK,
              WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, LEFT));
    }
  }

  protected void initializeEditControlPanel() {
    editPanel.initializePanel();
    editControlPanel.setMinimumSize(new Dimension(0, 0));
    final int alignment = controlPanelConstraints.equals(BorderLayout.SOUTH) ||
            controlPanelConstraints.equals(BorderLayout.NORTH) ? FlowLayout.CENTER : FlowLayout.LEADING;
    final JPanel propertyBase = new JPanel(Layouts.flowLayout(alignment));
    propertyBase.add(editPanel);
    editControlPanel.add(propertyBase, BorderLayout.CENTER);
    if (includeControlPanel) {
      final JComponent controlPanel = createEditControlPanel();
      if (controlPanel != null) {
        editControlPanel.add(controlPanel, controlPanelConstraints);
      }
    }
  }

  private void initializeTablePanel() {
    final ControlSet toolbarControls = new ControlSet("");
    if (showToggleEditPanelControl && editPanel != null) {
      toolbarControls.add(getToggleEditPanelControl());
    }
    if (showDetailPanelControls && !detailEntityPanels.isEmpty()) {
      toolbarControls.add(getToggleDetailPanelControl());
    }
    if (toolbarControls.size() > 0) {
      tablePanel.addToolBarControls(toolbarControls);
    }
    if (showDetailPanelControls) {
      final ControlSet detailPanelControlSet = getDetailPanelControlSet();
      if (detailPanelControlSet != null) {
        tablePanel.addPopupControls(detailPanelControlSet);
      }
    }
    if (tablePanel.getTable().getDoubleClickAction() == null) {
      tablePanel.getTable().setDoubleClickAction(initializeTableDoubleClickAction());
    }
    tablePanel.initializePanel();
    tablePanel.setMinimumSize(new Dimension(0, 0));
  }

  private void initializeDetailAndTablePanels() {
    if (detailPanelTabbedPane == null) { //no left right split pane
      add(tablePanel, BorderLayout.CENTER);
    }
    else {
      if (compactDetailLayout) {
        compactBase = new JPanel(Layouts.borderLayout());
        compactBase.add(tablePanel, BorderLayout.CENTER);
        horizontalSplitPane.setLeftComponent(compactBase);
      }
      else {
        horizontalSplitPane.setLeftComponent(tablePanel);
      }
      horizontalSplitPane.setRightComponent(detailPanelTabbedPane);
      add(horizontalSplitPane, BorderLayout.CENTER);
    }
  }

  /**
   * Initializes the horizontal split pane, used in the case of detail panel(s)
   * @return the horizontal split pane
   */
  private JSplitPane initializeHorizontalSplitPane() {
    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
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
    final JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setFocusable(false);
    for (final EntityPanel detailPanel : detailEntityPanels) {
      tabbedPane.addTab(detailPanel.caption, detailPanel);
    }
    tabbedPane.addChangeListener(e -> getTabbedDetailPanel().activatePanel());
    if (showDetailPanelControls) {
      tabbedPane.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseReleased(final MouseEvent e) {
          if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            setDetailPanelState(getDetailPanelState() == DIALOG ? EMBEDDED : DIALOG);
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
   * Returns a ControlSet containing the detail panel controls, if no detail
   * panels exist null is returned.
   * @return a ControlSet for activating individual detail panels
   * @see #getDetailPanelControls(PanelState)
   */
  private ControlSet getDetailPanelControlSet() {
    if (!detailEntityPanels.isEmpty()) {
      final ControlSet controlSet = new ControlSet("");
      controlSet.add(getDetailPanelControls(EMBEDDED));

      return controlSet;
    }

    return null;
  }

  /**
   * Initialize the Control to trigger when a double click is performed on the table, if a table is present.
   * The default implementation shows the edit panel in a dialog if one is available and hidden, if that is
   * not the case and the detail panels are hidden those are shown in a dialog.
   * @return the Control to trigger when the a double click is performed on the table
   */
  private Control initializeTableDoubleClickAction() {
    return Controls.control(() -> {
      if (containsEditPanel() || (!detailEntityPanels.isEmpty() && includeDetailPanelTabPane)) {
        if (containsEditPanel() && getEditPanelState() == HIDDEN) {
          setEditPanelState(DIALOG);
        }
        else if (getDetailPanelState() == HIDDEN) {
          setDetailPanelState(DIALOG);
        }
      }
    });
  }

  /**
   * Initializes a ControlSet containing a control for setting the state to {@code status} on each detail panel.
   * @param status the status
   * @return a ControlSet for controlling the state of the detail panels
   */
  private ControlSet getDetailPanelControls(final PanelState status) {
    if (detailEntityPanels.isEmpty()) {
      return null;
    }

    final ControlSet controlSet = new ControlSet(MESSAGES.getString(MSG_DETAIL_TABLES));
    for (final EntityPanel detailPanel : detailEntityPanels) {
      controlSet.add(Controls.control(() -> {
        setDetailPanelState(status);
        detailPanel.activatePanel();
      }, detailPanel.getCaption()));
    }

    return controlSet;
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
    if (editPanelState != DIALOG) {
      disposeEditDialog();
    }

    if (editPanelState == EMBEDDED) {
      if (compactBase != null) {
        compactBase.add(editControlPanel, BorderLayout.NORTH);
      }
      else {
        add(editControlPanel, BorderLayout.NORTH);
      }
    }
    else if (editPanelState == HIDDEN) {
      if (compactBase != null && !detailEntityPanels.isEmpty()) {
        compactBase.remove(editControlPanel);
      }
      else {
        remove(editControlPanel);
      }
    }
    else {
      showEditDialog();
    }
    requestInitialFocus();

    revalidate();
  }

  /**
   * Shows the detail panels in a non-modal dialog
   */
  private void showDetailDialog() {
    final Window parent = Windows.getParentWindow(this);
    final Dimension parentSize = parent.getSize();
    final Dimension size = getDetailDialogSize(parentSize);
    final Point parentLocation = parent.getLocation();
    final Point location = new Point(parentLocation.x + (parentSize.width - size.width),
            parentLocation.y + (parentSize.height - size.height) - DETAIL_DIALOG_OFFSET);
    detailPanelDialog = Dialogs.displayInDialog(EntityPanel.this, detailPanelTabbedPane,
            caption + " - " + MESSAGES.getString(MSG_DETAIL_TABLES), Modal.NO,
            Controls.control(() -> {
              //the dialog can be closed when embedding the panel, don't hide if that's the case
              if (getDetailPanelState() != EMBEDDED) {
                setDetailPanelState(HIDDEN);
              }
            }));
    detailPanelDialog.setSize(size);
    detailPanelDialog.setLocation(location);
  }

  /**
   * @param parentSize the size of the parent window
   * @return the size to use when showing the detail dialog
   */
  private Dimension getDetailDialogSize(final Dimension parentSize) {
    return new Dimension((int) (parentSize.width / DETAIL_DIALOG_SIZE_RATIO), containsEditPanel() ?
            (int) (parentSize.height / DETAIL_DIALOG_SIZE_RATIO) : parentSize.height - DETAIL_DIALOG_HEIGHT_OFFSET);
  }

  /**
   * Shows the edit panel in a non-modal dialog
   */
  private void showEditDialog() {
    Container dialogOwner = this;
    if (CENTER_APPLICATION_DIALOGS.get()) {
      dialogOwner = Windows.getParentWindow(this);
    }
    editPanelDialog = Dialogs.displayInDialog(dialogOwner, editControlPanel, caption, Modal.NO,
            disposeEditDialogOnEscape ? DisposeOnEscape.YES : DisposeOnEscape.NO,
            Controls.control(() -> setEditPanelState(HIDDEN)));
  }

  /**
   * @return the detail panel selected in the detail tab pane.
   * @throws IllegalStateException in case no detail panels are define
   */
  private EntityPanel getTabbedDetailPanel() {
    if (detailPanelTabbedPane == null) {
      throw new IllegalStateException("No detail panels available");
    }

    return (EntityPanel) detailPanelTabbedPane.getSelectedComponent();
  }

  private void disposeEditDialog() {
    if (editPanelDialog != null) {
      editPanelDialog.setVisible(false);
      editPanelDialog.dispose();
      editPanelDialog = null;
    }
  }

  private void disposeDetailDialog() {
    if (detailPanelDialog != null) {
      detailPanelDialog.setVisible(false);
      detailPanelDialog.dispose();
      detailPanelDialog = null;
    }
  }

  private void bindEvents() {
    entityModel.addBeforeRefreshListener(() -> Components.showWaitCursor(EntityPanel.this));
    entityModel.addAfterRefreshListener(() -> Components.hideWaitCursor(EntityPanel.this));
    addComponentListener(new EntityPanelComponentAdapter());
  }

  private void checkIfInitialized() {
    if (panelInitialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
  }

  private static final class NavigateAction extends AbstractAction {

    private final EntityPanel entityPanel;
    private final Direction direction;

    private NavigateAction(final EntityPanel entityPanel, final Direction direction) {
      super("Navigate " + direction);
      this.entityPanel = entityPanel;
      this.direction = direction;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      final HierarchyPanel panel;
      switch (direction) {
        case LEFT:
          panel = entityPanel.getPreviousSiblingPanel();
          break;
        case RIGHT:
          panel = entityPanel.getNextSiblingPanel();
          break;
        case UP:
          panel = entityPanel.getParentPanel();
          break;
        case DOWN:
          if (entityPanel.getDetailPanelState() == HIDDEN) {
            entityPanel.setDetailPanelState(EMBEDDED);
          }
          panel = entityPanel.getSelectedChildPanel();
          break;
        default:
          throw new IllegalArgumentException("Unknown direction: " + direction);
      }

      if (panel != null) {
        panel.activatePanel();
      }
    }
  }

  private static final class ResizeHorizontallyAction extends AbstractAction {

    private final EntityPanel panel;
    private final Direction direction;

    private ResizeHorizontallyAction(final EntityPanel panel, final Direction direction) {
      super("Resize " + direction);
      this.panel = panel;
      this.direction = direction;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      if (panel.masterPanel != null) {
        panel.masterPanel.resizePanel(direction, RESIZE_AMOUNT);
      }
    }
  }

  private static final class ResizeVerticallyAction extends AbstractAction {

    private final EntityPanel panel;
    private final Direction direction;

    private ResizeVerticallyAction(final EntityPanel panel, final Direction direction) {
      super("Resize " + direction);
      this.panel = panel;
      this.direction = direction;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      panel.resizePanel(direction, RESIZE_AMOUNT);
    }
  }

  private static class FocusActivationListener implements PropertyChangeListener {
    @Override
    public void propertyChange(final PropertyChangeEvent changeEvent) {
      final EntityEditPanel editPanelParent = Components.getParentOfType((Component) changeEvent.getNewValue(), EntityEditPanel.class);
      if (editPanelParent != null) {
        editPanelParent.setActive(true);
      }
      else {
        final EntityPanel parent = Components.getParentOfType((Component) changeEvent.getNewValue(), EntityPanel.class);
        if (parent != null && parent.getEditPanel() != null) {
          parent.getEditPanel().setActive(true);
        }
      }
    }
  }

  private final class EntityPanelComponentAdapter extends ComponentAdapter {
    @Override
    public void componentHidden(final ComponentEvent e) {
      SwingUtilities.invokeLater(() -> setFilterPanelsVisible(false));
    }
    @Override
    public void componentShown(final ComponentEvent e) {
      SwingUtilities.invokeLater(() -> setFilterPanelsVisible(true));
    }
  }
}
