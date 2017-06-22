/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.swing.SwingConfiguration;
import org.jminor.swing.common.ui.DefaultExceptionHandler;
import org.jminor.swing.common.ui.MasterDetailPanel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.Action;
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
import java.util.Objects;

/**
 * A panel representing a Entity via a EntityModel, which facilitates browsing and editing of records.
 * <pre>
 *   String entityID = ...;
 *   EntityConnectionProvider connectionProvider = ...;
 *   EntityModel entityModel = new DefaultEntityModel(entityID, connectionProvider);
 *   EntityPanel entityPanel = new EntityPanel(entityModel);
 *   entityPanel.initializePanel();
 *   JFrame frame = new JFrame();
 *   frame.add(entityPanel);
 *   frame.pack();
 *   frame.setVisible(true);
 * </pre>
 */
public class EntityPanel extends JPanel implements MasterDetailPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityPanel.class);

  /**
   * The possible states of a detail panel.
   */
  public enum PanelState {
    DIALOG, EMBEDDED, HIDDEN
  }

  /**
   * The novigation directions.
   */
  public enum Direction {
    UP, DOWN, RIGHT, LEFT
  }

  private static final String NAVIGATE_UP = "navigateUp";
  private static final String NAVIGATE_DOWN = "navigateDown";
  private static final String NAVIGATE_RIGHT = "navigateRight";
  private static final String NAVIGATE_LEFT = "navigateLeft";

  private static final String RESIZE_LEFT = "resizeLeft";
  private static final String RESIZE_RIGHT = "resizeRight";
  private static final String RESIZE_UP = "resizeUp";
  private static final String RESIZE_DOWN = "resizeDown";

  private static final int RESIZE_AMOUNT = 30;

  private static final int SPLIT_PANE_DIVIDER_SIZE = SwingConfiguration.getIntValue(SwingConfiguration.SPLIT_PANE_DIVIDER_SIZE);
  private static final double DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT = 0.5;
  private static final int DETAIL_DIALOG_OFFSET = 29;
  private static final double DETAIL_DIALOG_SIZE_RATIO = 1.5;
  private static final int DETAIL_DIALOG_HEIGHT_OFFSET = 54;

  /**
   * The EntityModel instance used by this EntityPanel
   */
  private final SwingEntityModel entityModel;

  /**
   * The caption to use when presenting this entity panel
   */
  private final String caption;

  /**
   * A List containing the detail panels, if any
   */
  private final List<EntityPanel> detailEntityPanels = new ArrayList<>();

  /**
   * The EntityEditPanel instance
   */
  private final EntityEditPanel editPanel;

  /**
   * The EntityTablePanel instance used by this EntityPanel
   */
  private final EntityTablePanel tablePanel;

  /**
   * The base edit panel which contains the controls required for editing a entity
   */
  private final JPanel editControlPanel = new JPanel(UiUtil.createBorderLayout());

  /**
   * The horizontal split pane, which is used in case this entity panel has detail panels.
   * It splits the lower section of this EntityPanel into the EntityTablePanel on the left
   * and the detail panels on the right
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
   * A base panel used in case this EntityPanel is configured to be compact
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
   * true if this panel should be compact
   */
  private boolean compactDetailLayout = Configuration.getBooleanValue(Configuration.COMPACT_ENTITY_PANEL_LAYOUT);

  /**
   * indicates where the control panel should be placed in a BorderLayout
   */
  private String controlPanelConstraints = Configuration.getBooleanValue(Configuration.TOOLBAR_BUTTONS) ? BorderLayout.WEST : BorderLayout.EAST;

  /**
   * Holds the current state of the edit panel (HIDDEN, EMBEDDED or DIALOG)
   */
  private PanelState editPanelState = PanelState.EMBEDDED;

  /**
   * Holds the current state of the detail panels (HIDDEN, EMBEDDED or DIALOG)
   */
  private PanelState detailPanelState = PanelState.EMBEDDED;

  /**
   * if true then the edit control panel should be included
   */
  private boolean includeControlPanel = true;

  /**
   * if true and detail panels are available then the detail panel tab pane should be included
   */
  private boolean includeDetailPanelTabPane = true;

  /**
   * if true and an edit panel is available the actions to toggle it is included
   */
  private boolean showToggleEditPanelControl = Configuration.getBooleanValue(Configuration.SHOW_TOGGLE_EDIT_PANEL_CONTROL);

  /**
   * if true and detail panels are available the controls to hide and show detail panels are included
   */
  private boolean showDetailPanelControls = Configuration.getBooleanValue(Configuration.SHOW_DETAIL_PANEL_CONTROLS);

  /**
   * if true then the ESC key disposes the edit dialog
   */
  private boolean disposeEditDialogOnEscape = Configuration.getBooleanValue(Configuration.DISPOSE_EDIT_DIALOG_ON_ESCAPE);

  /**
   * True after {@code initializePanel()} has been called
   */
  private boolean panelInitialized = false;

  private double detailSplitPanelResizeWeight = DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT;

  static {
    if (Configuration.getBooleanValue(Configuration.USE_FOCUS_ACTIVATION)) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", new FocusActivationListener());
    }
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * The default caption of the underlying entity is used.
   * @param entityModel the EntityModel
   */
  public EntityPanel(final SwingEntityModel entityModel) {
    this(entityModel, Entities.getCaption(Objects.requireNonNull(entityModel, "entityModel").getEntityID()));
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param caption the caption to use when presenting this entity panel
   */
  public EntityPanel(final SwingEntityModel entityModel, final String caption) {
    this(entityModel, caption, null, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.getTableModel()) : null);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   */
  public EntityPanel(final SwingEntityModel entityModel, final EntityEditPanel editPanel) {
    this(entityModel, Entities.getCaption(entityModel.getEntityID()), editPanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param tablePanel the table panel
   */
  public EntityPanel(final SwingEntityModel entityModel, final EntityTablePanel tablePanel) {
    this(entityModel, Entities.getCaption(entityModel.getEntityID()), tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param editPanel the edit panel
   */
  public EntityPanel(final SwingEntityModel entityModel, final String caption, final EntityEditPanel editPanel) {
    this(entityModel, caption, editPanel, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.getTableModel()) : null);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param tablePanel the table panel
   */
  public EntityPanel(final SwingEntityModel entityModel, final String caption, final EntityTablePanel tablePanel) {
    this(entityModel, caption, null, tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   * @param tablePanel the table panel
   */
  public EntityPanel(final SwingEntityModel entityModel, final EntityEditPanel editPanel, final EntityTablePanel tablePanel) {
    this(entityModel, Entities.getCaption(entityModel.getEntityID()), editPanel, tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until {@link #initializePanel()} is called.
   * @param entityModel the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param editPanel the edit panel
   * @param tablePanel the table panel
   */
  public EntityPanel(final SwingEntityModel entityModel, final String caption, final EntityEditPanel editPanel,
                     final EntityTablePanel tablePanel) {
    Objects.requireNonNull(entityModel, "entityModel");
    this.entityModel = entityModel;
    this.caption = caption == null ? Entities.getCaption(entityModel.getEntityID()) : caption;
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
   * With  (BorderLayout.SOUTH):
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
    Objects.requireNonNull(detailPanels, "detailPanels");
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
        UiUtil.setWaitCursor(true, this);
        initializeAssociatedPanels();
        initializeControlPanels();
        bindEvents();
        initializeUI();
        initialize();
      }
      finally {
        panelInitialized = true;
        UiUtil.setWaitCursor(false, this);
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
    final Collection<EntityPanel> linkedDetailPanels = new ArrayList<>(linkedDetailModels.size());
    for (final EntityPanel detailPanel : detailEntityPanels) {
      if (linkedDetailModels.contains(detailPanel.entityModel)) {
        linkedDetailPanels.add(detailPanel);
      }
    }

    return linkedDetailPanels;
  }

  /**
   * Returns the detail panel for the given {@code entityID}, if one is available
   * @param entityID the entity ID of the detail panel to retrieve
   * @return the detail panel of the given type
   * @throws IllegalArgumentException in case the panel was not found
   */
  public final EntityPanel getDetailPanel(final String entityID) {
    for (final EntityPanel detailPanel : detailEntityPanels) {
      if (detailPanel.entityModel.getEntityID().equals(entityID)) {
        return detailPanel;
      }
    }

    throw new IllegalArgumentException("Detail panel for entity: " + entityID + " not found in panel: " + getClass());
  }

  /**
   * Returns true if this panel contains a detail panel for the given {@code entityID}
   * @param entityID the entityID
   * @return true if a detail panel for the given entityID is found
   */
  public final boolean containsDetailPanel(final String entityID) {
    for (final EntityPanel detailPanel : detailEntityPanels) {
      if (detailPanel.entityModel.getEntityID().equals(entityID)) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + caption;
  }

  /**
   * @return the caption to use when presenting this entity panel
   */
  public final String getCaption() {
    return caption;
  }

  /** {@inheritDoc} */
  @Override
  public final void activatePanel() {
    if (getMasterPanel() != null) {
      getMasterPanel().setActiveDetailPanel(this);
    }
    initializePanel();
    prepareUI(true, false);
  }

  /** {@inheritDoc} */
  @Override
  public final MasterDetailPanel getMasterPanel() {
    MasterDetailPanel parentPanel = masterPanel;
    if (parentPanel == null) {
      parentPanel = UiUtil.getParentOfType(this, MasterDetailPanel.class);
    }

    return parentPanel;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityPanel getActiveDetailPanel() {
    final Collection<EntityPanel> linkedDetailPanels = getLinkedDetailPanels();
    if (!linkedDetailPanels.isEmpty()) {
      return linkedDetailPanels.iterator().next();
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final void setActiveDetailPanel(final MasterDetailPanel detailPanel) {
    if (detailPanelTabbedPane != null) {
      detailPanelTabbedPane.setSelectedComponent((JComponent) detailPanel);
      for (final SwingEntityModel linkedModel : new ArrayList<>(entityModel.getLinkedDetailModels())) {
        entityModel.removeLinkedDetailModel(linkedModel);
      }
      entityModel.addLinkedDetailModel(getTabbedDetailPanel().getModel());
    }
  }

  /** {@inheritDoc} */
  @Override
  public final MasterDetailPanel getPreviousPanel() {
    if (getMasterPanel() == null) {//no parent, no siblings
      return null;
    }
    final List<? extends MasterDetailPanel> masterDetailPanels = getMasterPanel().getDetailPanels();
    if (masterDetailPanels.contains(this)) {
      final int index = masterDetailPanels.indexOf(this);
      if (index == 0) {
        return masterDetailPanels.get(masterDetailPanels.size() - 1);
      }
      else {
        return masterDetailPanels.get(index - 1);
      }
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final MasterDetailPanel getNextPanel() {
    if (getMasterPanel() == null) {//no parent, no siblings
      return null;
    }
    final List<? extends MasterDetailPanel> masterDetailPanels = getMasterPanel().getDetailPanels();
    if (masterDetailPanels.contains(this)) {
      final int index = masterDetailPanels.indexOf(this);
      if (index == masterDetailPanels.size() - 1) {
        return masterDetailPanels.get(0);
      }
      else {
        return masterDetailPanels.get(index + 1);
      }
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final List<MasterDetailPanel> getDetailPanels() {
    return Collections.unmodifiableList(detailEntityPanels);
  }

  /**
   * @return a control for toggling the edit panel
   */
  public final Control getToggleEditPanelControl() {
    final Control toggle = Controls.commandControl(this::toggleEditPanelState, Images.loadImage("Form16.gif"));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_EDIT_TIP));

    return toggle;
  }

  /**
   * @return a control for toggling the detail panel
   */
  public final Control getToggleDetailPanelControl() {
    final Control toggle = Controls.commandControl(this::toggleDetailPanelState, Images.loadImage(Images.IMG_HISTORY_16));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_DETAIL_TIP));

    return toggle;
  }

  /**
   * By default this delegates to the edit panel
   * @param exception the exception to handle
   */
  public final void handleException(final Exception exception) {
    if (editPanel != null) {
      editPanel.handleException(exception);
    }
    else {
      DefaultExceptionHandler.getInstance().handleException(exception, this);
    }
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
   * @return true if the control panel should be included
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
   * @see Configuration#SHOW_TOGGLE_EDIT_PANEL_CONTROL
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
   * @see Configuration#SHOW_DETAIL_PANEL_CONTROLS
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
   * @see Configuration#DISPOSE_EDIT_DIALOG_ON_ESCAPE
   */
  public final boolean isDisposeEditDialogOnEscape() {
    return disposeEditDialogOnEscape;
  }

  /**
   * @param disposeEditDialogOnEscape if true then the edit dialog is disposed of on ESC
   * @see Configuration#DISPOSE_EDIT_DIALOG_ON_ESCAPE
   */
  public final void setDisposeEditDialogOnEscape(final boolean disposeEditDialogOnEscape) {
    this.disposeEditDialogOnEscape = disposeEditDialogOnEscape;
  }

  /**
   * Toggles the detail panel state between DIALOG, HIDDEN and EMBEDDED
   */
  public final void toggleDetailPanelState() {
    if (detailPanelState == PanelState.DIALOG) {
      setDetailPanelState(PanelState.HIDDEN);
    }
    else if (detailPanelState == PanelState.EMBEDDED) {
      setDetailPanelState(PanelState.DIALOG);
    }
    else {
      setDetailPanelState(PanelState.EMBEDDED);
    }
  }

  /**
   * Toggles the edit panel state between DIALOG, HIDDEN and EMBEDDED
   */
  public final void toggleEditPanelState() {
    if (editPanelState == PanelState.DIALOG) {
      setEditPanelState(PanelState.HIDDEN);
    }
    else if (editPanelState == PanelState.EMBEDDED) {
      setEditPanelState(PanelState.DIALOG);
    }
    else {
      setEditPanelState(PanelState.EMBEDDED);
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

    if (state != PanelState.HIDDEN) {
      getTabbedDetailPanel().initializePanel();
    }

    if (detailPanelState == PanelState.DIALOG) {//if we are leaving the DIALOG state, hide all child detail dialogs
      for (final EntityPanel detailPanel : detailEntityPanels) {
        if (detailPanel.detailPanelState == PanelState.DIALOG) {
          detailPanel.setDetailPanelState(PanelState.HIDDEN);
        }
      }
    }

    if (state == PanelState.HIDDEN) {
      entityModel.removeLinkedDetailModel(getTabbedDetailPanel().entityModel);
    }
    else {
      entityModel.addLinkedDetailModel(getTabbedDetailPanel().entityModel);
    }

    detailPanelState = state;
    if (state != PanelState.DIALOG) {
      disposeDetailDialog();
    }

    if (state == PanelState.EMBEDDED) {
      horizontalSplitPane.setRightComponent(detailPanelTabbedPane);
    }
    else if (state == PanelState.HIDDEN) {
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
   * @param value true if the active panels should be shown, false if they should be hidden
   */
  public final void setFilterPanelsVisible(final boolean value) {
    if (!panelInitialized) {
      return;
    }

    if (containsTablePanel()) {
      tablePanel.setFilterPanelsVisible(value);
    }
    for (final EntityPanel detailEntityPanel : detailEntityPanels) {
      detailEntityPanel.setFilterPanelsVisible(value);
    }
  }

  /**
   * Resizes this panel in the given direction
   * @param direction the resize direction
   * @param pixelAmount the resize amount
   */
  public final void resizePanel(final Direction direction, final int pixelAmount) {
    switch(direction) {
      case UP:
        setEditPanelState(PanelState.HIDDEN);
        break;
      case DOWN:
        if (editPanelState == PanelState.EMBEDDED) {
          setEditPanelState(PanelState.DIALOG);
        }
        else {
          setEditPanelState(PanelState.EMBEDDED);
        }
        break;
      case RIGHT:
        if (horizontalSplitPane != null) {
          final int rightPos = horizontalSplitPane.getDividerLocation() + pixelAmount;
          if (rightPos <= horizontalSplitPane.getMaximumDividerLocation()) {
            horizontalSplitPane.setDividerLocation(rightPos);
          }
          else {
            horizontalSplitPane.setDividerLocation(horizontalSplitPane.getMaximumDividerLocation());
          }
        }
        break;
      case LEFT:
        if (horizontalSplitPane != null) {
          final int leftPos = horizontalSplitPane.getDividerLocation() - pixelAmount;
          if (leftPos >= 0) {
            horizontalSplitPane.setDividerLocation(leftPos);
          }
          else {
            horizontalSplitPane.setDividerLocation(0);
          }
        }
        break;
      default:
        throw new IllegalArgumentException("Undefined resize direction: " + direction);
    }
  }

  /**
   * Prepares the UI, by clearing the input fields and setting the initial focus,
   * if both parameters are set to false then there is no effect
   * @param setInitialFocus if true the component defined as the initialFocusComponent
   * gets the input focus, if none is defined the first child component of this EntityPanel is used,
   * if no edit panel is available the table receives the focus
   * @param clearUI if true the the input components are cleared
   * @see EntityEditPanel#setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void prepareUI(final boolean setInitialFocus, final boolean clearUI) {
    LOG.debug("{} prepareUI({}, {})", new Object[] {getEditModel().getEntityID(), setInitialFocus, clearUI});
    if (editPanel != null && editPanelState != PanelState.HIDDEN) {
      editPanel.prepareUI(setInitialFocus, clearUI);
    }
    else if (setInitialFocus) {
      if (tablePanel != null) {
        tablePanel.getJTable().requestFocus();
      }
      else if (getComponentCount() > 0) {
        getComponents()[0].requestFocus();
      }
    }
  }

  /**
   * Saves any user preferences for all entity panels and associated elements
   */
  public final void savePreferences() {
    for (final EntityPanel detailPanel : detailEntityPanels) {
      detailPanel.savePreferences();
    }
    getModel().savePreferences();
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
    setLayout(UiUtil.createBorderLayout());
    if (detailPanelTabbedPane != null || tablePanel != null) {
      initializeDetailAndTablePanels();
    }
    setDetailPanelState(detailPanelState);
    if (containsEditPanel()) {
      updateEditPanelState();
    }
    setupKeyboardActions();
    if (Configuration.getBooleanValue(Configuration.USE_KEYBOARD_NAVIGATION)) {
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
   * @see Configuration#TOOLBAR_BUTTONS
   */
  protected JComponent createEditControlPanel() {
    final int alignment = controlPanelConstraints.equals(BorderLayout.SOUTH) ||
            controlPanelConstraints.equals(BorderLayout.NORTH) ? FlowLayout.CENTER : FlowLayout.LEADING;
    return Configuration.getBooleanValue(Configuration.TOOLBAR_BUTTONS) ?
              editPanel.createControlToolBar(JToolBar.VERTICAL) : editPanel.createControlPanel(alignment == FlowLayout.CENTER);
  }

  /**
   * @param masterPanel the panel serving as master panel for this entity panel
   * @throws IllegalStateException in case a master panel has already been set
   */
  protected final void setMasterPanel(final EntityPanel masterPanel) {
    Objects.requireNonNull(masterPanel, "masterPanel");
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
  private void setupKeyboardActions() {
    final Action selectEditPanelAction = new AbstractAction("EntityPanel.selectEditPanel") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (getEditPanelState() == PanelState.HIDDEN) {
          setEditPanelState(PanelState.EMBEDDED);
        }
        getEditPanel().prepareUI(true, false);
      }
    };
    final Action selectInputComponentAction = new AbstractAction("EntityPanel.selectInputComponent") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (getEditPanelState() == PanelState.HIDDEN) {
          setEditPanelState(PanelState.EMBEDDED);
        }
        getEditPanel().selectInputComponent();
      }
    };
    final Action selectTablePanelAction = new AbstractAction("EntityPanel.selectTablePanel") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getTablePanel().getJTable().requestFocus();
      }
    };
    final Action selectSearchFieldAction = new AbstractAction("EntityPanel.selectSearchField") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getTablePanel().getSearchField().requestFocus();
      }
    };
    final Action toggleConditionPanelAction = new AbstractAction("EntityPanel.toggleConditionPanel") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (!getTablePanel().isConditionPanelVisible()) {
          getTablePanel().setConditionPanelVisible(true);
        }
        getTablePanel().getConditionPanel().requestFocus();
      }
    };
    if (containsTablePanel()) {
      UiUtil.addKeyEvent(this, KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              selectTablePanelAction);
      UiUtil.addKeyEvent(this, KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              selectSearchFieldAction);
      if (tablePanel.getConditionPanel() != null) {
        UiUtil.addKeyEvent(this, KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                toggleConditionPanelAction);
      }
      if (containsEditPanel()) {
        UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                selectTablePanelAction);
        UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                selectSearchFieldAction);
        if (tablePanel.getConditionPanel() != null) {
          UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                  toggleConditionPanelAction);
        }
      }
    }
    if (containsEditPanel()) {
      UiUtil.addKeyEvent(this, KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              selectEditPanelAction);
      UiUtil.addKeyEvent(this, KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              selectInputComponentAction);
      UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              selectInputComponentAction);
    }
  }

  private void initializeEditControlPanel() {
    editPanel.initializePanel();
    editControlPanel.setMinimumSize(new Dimension(0, 0));
    final int alignment = controlPanelConstraints.equals(BorderLayout.SOUTH) ||
            controlPanelConstraints.equals(BorderLayout.NORTH) ? FlowLayout.CENTER : FlowLayout.LEADING;
    final JPanel propertyBase = new JPanel(UiUtil.createFlowLayout(alignment));
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
      tablePanel.addToolbarControls(toolbarControls);
    }
    if (showDetailPanelControls) {
      final ControlSet detailPanelControlSet = getDetailPanelControlSet();
      if (detailPanelControlSet != null) {
        tablePanel.addPopupControls(detailPanelControlSet);
      }
    }
    if (tablePanel.getTableDoubleClickAction() == null) {
      tablePanel.setTableDoubleClickAction(initializeTableDoubleClickAction());
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
        compactBase = new JPanel(UiUtil.createBorderLayout());
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
    splitPane.setDividerSize(SPLIT_PANE_DIVIDER_SIZE);

    return splitPane;
  }

  /**
   * Initializes the JTabbedPane containing the detail panels, used in case of multiple detail panels
   * @return the JTabbedPane for holding detail panels
   */
  private JTabbedPane initializeDetailTabPane() {
    final JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setFocusable(false);
    tabbedPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final EntityPanel detailPanel : detailEntityPanels) {
      tabbedPane.addTab(detailPanel.caption, detailPanel);
    }
    tabbedPane.addChangeListener(e -> getTabbedDetailPanel().activatePanel());
    if (showDetailPanelControls) {
      tabbedPane.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseReleased(final MouseEvent e) {
          if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            setDetailPanelState(getDetailPanelState() == PanelState.DIALOG ? PanelState.EMBEDDED : PanelState.DIALOG);
          }
          else if (e.getButton() == MouseEvent.BUTTON2) {
            setDetailPanelState(getDetailPanelState() == PanelState.EMBEDDED ? PanelState.HIDDEN : PanelState.EMBEDDED);
          }
        }
      });
    }

    return tabbedPane;
  }

  private void initializeResizing() {
    UiUtil.addKeyEvent(this, KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeVerticallyAction(this, RESIZE_UP, Direction.UP));
    UiUtil.addKeyEvent(this, KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeVerticallyAction(this, RESIZE_DOWN, Direction.DOWN));
    UiUtil.addKeyEvent(this, KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeHorizontallyAction(this, RESIZE_RIGHT, Direction.RIGHT));
    UiUtil.addKeyEvent(this, KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeHorizontallyAction(this, RESIZE_LEFT, Direction.LEFT));
    if (containsEditPanel()) {
      UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK,
              JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeVerticallyAction(this, RESIZE_UP, Direction.UP));
      UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK,
              JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeVerticallyAction(this, RESIZE_DOWN, Direction.DOWN));
      UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK,
              JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeHorizontallyAction(this, RESIZE_RIGHT, Direction.RIGHT));
      UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK,
              JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new ResizeHorizontallyAction(this, RESIZE_LEFT, Direction.LEFT));
    }
  }

  private void initializeNavigation() {
    UiUtil.addKeyEvent(this, KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK + KeyEvent.CTRL_DOWN_MASK,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, NAVIGATE_UP, Direction.UP));
    UiUtil.addKeyEvent(this, KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK + KeyEvent.CTRL_DOWN_MASK,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, NAVIGATE_DOWN, Direction.DOWN));
    UiUtil.addKeyEvent(this, KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK + KeyEvent.CTRL_DOWN_MASK,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, NAVIGATE_RIGHT, Direction.RIGHT));
    UiUtil.addKeyEvent(this, KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK + KeyEvent.CTRL_DOWN_MASK,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, NAVIGATE_LEFT, Direction.LEFT));
    if (containsEditPanel()) {
      UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK + KeyEvent.CTRL_DOWN_MASK,
              JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, NAVIGATE_UP, Direction.UP));
      UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK + KeyEvent.CTRL_DOWN_MASK,
              JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, NAVIGATE_DOWN, Direction.DOWN));
      UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK + KeyEvent.CTRL_DOWN_MASK,
              JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, NAVIGATE_RIGHT, Direction.RIGHT));
      UiUtil.addKeyEvent(editControlPanel, KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK + KeyEvent.CTRL_DOWN_MASK,
              JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new NavigateAction(this, NAVIGATE_LEFT, Direction.LEFT));
    }
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
      controlSet.add(getDetailPanelControls(PanelState.EMBEDDED));

      return controlSet;
    }

    return null;
  }

  /**
   * Initialize the Action to perform when a double click is performed on the table, if a table is present.
   * The default implementation shows the edit panel in a dialog if one is available and hidden, if that is
   * not the case and the detail panels are hidden those are shown in a dialog.
   * @return the Action to perform when the a double click is performed on the table
   */
  private Action initializeTableDoubleClickAction() {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (containsEditPanel() || (!detailEntityPanels.isEmpty() && includeDetailPanelTabPane)) {
          if (containsEditPanel() && getEditPanelState() == PanelState.HIDDEN) {
            setEditPanelState(PanelState.DIALOG);
          }
          else if (getDetailPanelState() == PanelState.HIDDEN) {
            setDetailPanelState(PanelState.DIALOG);
          }
        }
      }
    };
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

    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.DETAIL_TABLES));
    for (final EntityPanel detailPanel : detailEntityPanels) {
      controlSet.add(new Control(detailPanel.getCaption()) {
        @Override
        public void actionPerformed(final ActionEvent e) {
          setDetailPanelState(status);
          detailPanel.activatePanel();
        }
      });
    }

    return controlSet;
  }

  //#############################################################################################
  // End - initialization methods
  //#############################################################################################

  private void updateEditPanelState() {
    if (editPanelState != PanelState.DIALOG) {
      disposeEditDialog();
    }

    if (editPanelState == PanelState.EMBEDDED) {
      if (compactBase != null) {
        compactBase.add(editControlPanel, BorderLayout.NORTH);
      }
      else {
        add(editControlPanel, BorderLayout.NORTH);
      }
    }
    else if (editPanelState == PanelState.HIDDEN) {
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
    prepareUI(true, false);

    revalidate();
  }

  /**
   * Shows the detail panels in a non-modal dialog
   */
  private void showDetailDialog() {
    final Window parent = UiUtil.getParentWindow(this);
    final Dimension parentSize = parent.getSize();
    final Dimension size = getDetailDialogSize(parentSize);
    final Point parentLocation = parent.getLocation();
    final Point location = new Point(parentLocation.x + (parentSize.width - size.width),
            parentLocation.y + (parentSize.height - size.height) - DETAIL_DIALOG_OFFSET);
    detailPanelDialog = UiUtil.displayInDialog(EntityPanel.this, detailPanelTabbedPane,
            caption + " - " + FrameworkMessages.get(FrameworkMessages.DETAIL_TABLES), false,
            new AbstractAction() {
              @Override
              public void actionPerformed(final ActionEvent e) {
                setDetailPanelState(PanelState.HIDDEN);
              }
            });
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
    if (Configuration.getBooleanValue(Configuration.CENTER_APPLICATION_DIALOGS)) {
      dialogOwner = UiUtil.getParentWindow(this);
    }
    editPanelDialog = UiUtil.displayInDialog(dialogOwner, editControlPanel, caption, false, disposeEditDialogOnEscape,
            new AbstractAction() {
              @Override
              public void actionPerformed(final ActionEvent e) {
                setEditPanelState(PanelState.HIDDEN);
              }
            });
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
    entityModel.addBeforeRefreshListener(() -> UiUtil.setWaitCursor(true, EntityPanel.this));
    entityModel.addAfterRefreshListener(() -> UiUtil.setWaitCursor(false, EntityPanel.this));
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

    private NavigateAction(final EntityPanel entityPanel, final String name, final Direction direction) {
      super(name);
      this.entityPanel = entityPanel;
      this.direction = direction;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      final MasterDetailPanel panel;
      switch(direction) {
        case LEFT:
          panel = entityPanel.getPreviousPanel();
          break;
        case RIGHT:
          panel = entityPanel.getNextPanel();
          break;
        case UP:
          panel = entityPanel.getMasterPanel();
          break;
        case DOWN:
          if (entityPanel.getDetailPanelState() == PanelState.HIDDEN) {
            entityPanel.setDetailPanelState(PanelState.EMBEDDED);
          }
          panel = entityPanel.getActiveDetailPanel();
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

    private ResizeHorizontallyAction(final EntityPanel panel, final String action, final Direction direction) {
      super(action);
      this.panel = panel;
      this.direction = direction;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      final MasterDetailPanel activePanelParent = panel.masterPanel;
      if (activePanelParent != null) {
        ((EntityPanel) activePanelParent).resizePanel(direction, RESIZE_AMOUNT);
      }
    }
  }

  private static final class ResizeVerticallyAction extends AbstractAction {

    private final EntityPanel panel;
    private final Direction direction;

    private ResizeVerticallyAction(final EntityPanel panel, final String action, final Direction direction) {
      super(action);
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
    public void propertyChange(final PropertyChangeEvent evt) {
      final EntityEditPanel editPanelParent = UiUtil.getParentOfType((Component) evt.getNewValue(), EntityEditPanel.class);
      if (editPanelParent != null) {
        editPanelParent.setActive(true);
      }
      else {
        final EntityPanel parent = UiUtil.getParentOfType((Component) evt.getNewValue(), EntityPanel.class);
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
