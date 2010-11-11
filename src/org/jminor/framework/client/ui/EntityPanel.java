/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Util;
import org.jminor.common.ui.MasterDetailPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A panel representing a Entity via a EntityModel, which facilitates browsing and editing of records.
 * To lay out the panel components and initialize the panel you must call the method <code>initializePanel()</code>.
 */
public class EntityPanel extends JPanel implements MasterDetailPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityPanel.class);

  public static final int DIALOG = 1;
  public static final int EMBEDDED = 2;
  public static final int HIDDEN = 3;

  private static final int UP = 0;
  private static final int DOWN = 1;
  private static final int RIGHT = 2;
  private static final int LEFT = 3;

  private static final String NAVIGATE_UP = "navigateUp";
  private static final String NAVIGATE_DOWN = "navigateDown";
  private static final String NAVIGATE_RIGHT = "navigateRight";
  private static final String NAVIGATE_LEFT = "navigateLeft";

  private static final int DIVIDER_JUMP = 30;

  private static final String DIV_LEFT = "divLeft";
  private static final String DIV_RIGHT = "divRight";
  private static final String DIV_UP = "divUp";
  private static final String DIV_DOWN = "divDown";

  private static final double DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT = 0.5;
  private static final int SPLIT_PANE_DIVIDER_SIZE = 18;
  private static final int DETAIL_DIALOG_OFFSET = 29;
  private static final double DETAIL_DIALOG_SIZE_RATIO = 1.5;
  private static final int DETAIL_DIALOG_HEIGHT_OFFSET = 54;
  private static final int EDIT_DIALOG_LOCATION_OFFSET = 98;

  /**
   * The EntityModel instance used by this EntityPanel
   */
  private final EntityModel entityModel;

  /**
   * The caption to use when presenting this entity panel
   */
  private final String caption;

  /**
   * A List containing the detail panels, if any
   */
  private final List<EntityPanel> detailEntityPanels = new ArrayList<EntityPanel>();

  /**
   * The EntityEditPanel instance
   */
  private final EntityEditPanel editPanel;

  /**
   * The EntityTablePanel instance used by this EntityPanel
   */
  private final EntityTablePanel tablePanel;

  /**
   * The edit panel which contains the controls required for editing a entity
   */
  private JPanel editControlPanel;

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
  private int editPanelState = EMBEDDED;

  /**
   * Holds the current state of the detail panels (HIDDEN, EMBEDDED or DIALOG)
   */
  private int detailPanelState = EMBEDDED;

  /**
   * if true then the edit control panel should be included
   */
  private boolean includeControlPanel = true;

  /**
   * if true and detail panels are available then the detail panel tab pane should be included
   */
  private boolean includeDetailPanelTabPane = true;

  /**
   * True after <code>initializePanel()</code> has been called
   */
  private boolean panelInitialized = false;

  private double detailSplitPanelResizeWeight = DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT;

  static {
    if (Configuration.getBooleanValue(Configuration.USE_FOCUS_ACTIVATION)) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", new FocusListener());
    }
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * The default caption of the underlying entity is used.
   * @param entityModel the EntityModel
   */
  public EntityPanel(final EntityModel entityModel) {
    this(entityModel, Entities.getCaption(entityModel.getEntityID()));
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param entityModel the EntityModel
   * @param caption the caption to use when presenting this entity panel
   */
  public EntityPanel(final EntityModel entityModel, final String caption) {
    this(entityModel, caption, (EntityEditPanel) null);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   */
  public EntityPanel(final EntityModel entityModel, final EntityEditPanel editPanel) {
    this(entityModel, Entities.getCaption(entityModel.getEntityID()), editPanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param entityModel the EntityModel
   * @param tablePanel the table panel
   */
  public EntityPanel(final EntityModel entityModel, final EntityTablePanel tablePanel) {
    this(entityModel, Entities.getCaption(entityModel.getEntityID()), tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param entityModel the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param editPanel the edit panel
   */
  public EntityPanel(final EntityModel entityModel, final String caption, final EntityEditPanel editPanel) {
    this(entityModel, caption, editPanel, null);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param entityModel the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param tablePanel the table panel
   */
  public EntityPanel(final EntityModel entityModel, final String caption, final EntityTablePanel tablePanel) {
    this(entityModel, caption, null, tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param entityModel the EntityModel
   * @param editPanel the edit panel
   * @param tablePanel the table panel
   */
  public EntityPanel(final EntityModel entityModel, final EntityEditPanel editPanel, final EntityTablePanel tablePanel) {
    this(entityModel, Entities.getCaption(entityModel.getEntityID()), editPanel, tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param entityModel the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param editPanel the edit panel
   * @param tablePanel the table panel
   */
  public EntityPanel(final EntityModel entityModel, final String caption, final EntityEditPanel editPanel,
                     final EntityTablePanel tablePanel) {
    Util.rejectNullValue(entityModel, "model");
    this.entityModel = entityModel;
    this.caption = caption == null ? entityModel.getEntityID() : caption;
    this.editPanel = editPanel;
    if (tablePanel == null && entityModel.containsTableModel()) {
      this.tablePanel = new EntityTablePanel(entityModel.getTableModel());
    }
    else {
      this.tablePanel = tablePanel;
    }
  }

  /**
   * @return the EntityModel
   */
  public final EntityModel getModel() {
    return entityModel;
  }

  /**
   * @return the EntityEditModel
   */
  public final EntityEditModel getEditModel() {
    return entityModel.getEditModel();
  }

  /**
   * @return the EntityTableModel, null if none is available
   */
  public final EntityTableModel getTableModel() {
    return entityModel.getTableModel();
  }

  /**
   * @return the control panel layout constraints (BorderLayout constraints)
   */
  public final String getControlPanelConstraints() {
    return controlPanelConstraints;
  }

  /**
   * @param controlPanelConstraints the control panel layout constraints (BorderLayout constraints)
   * @return this entity panel
   */
  public final EntityPanel setControlPanelConstraints(final String controlPanelConstraints) {
    if (panelInitialized) {
      throw new IllegalStateException("Panel has already been initialized");
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
   * @param compactDetailLayout true if this panel and it's detail panels should be laid out in a compact state
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
    Util.rejectNullValue(detailPanels, "detailPanels");
    for (final EntityPanel detailPanel : detailPanels) {
      addDetailPanel(detailPanel);
    }

    return this;
  }

  /**
   * Adds the given detail panel, and adds the detail model to the underlying
   * model if it does not contain it already, and then sets <code>includeDetailPanelTabPane</code>
   * to true
   * @param detailPanel the detail panel to add
   * @return this entity panel
   */
  public final EntityPanel addDetailPanel(final EntityPanel detailPanel) {
    if (panelInitialized) {
      throw new IllegalStateException("Can not add detail panel after initialization");
    }
    if (!entityModel.containsDetailModel(detailPanel.entityModel)) {
      entityModel.addDetailModel(detailPanel.entityModel);
    }
    detailPanel.setMasterPanel(this);
    detailEntityPanels.add(detailPanel);

    return this;
  }

  /**
   * Initializes this EntityPanel, in case of some specific initialization code, to show the search panel for example,
   * you can override the <code>initialize()</code> method and add your code there.
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
        bindModelEvents();
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
    final Collection<EntityModel> linkedDetailModels = entityModel.getLinkedDetailModels();
    final Collection<EntityPanel> linkedDetailPanels = new ArrayList<EntityPanel>(linkedDetailModels.size());
    for (final EntityPanel detailPanel : detailEntityPanels) {
      if (linkedDetailModels.contains(detailPanel.entityModel)) {
        linkedDetailPanels.add(detailPanel);
      }
    }

    return linkedDetailPanels;
  }

  /**
   * @return the detail panel selected in the detail tab pane.
   * If no detail panels are defined a RuntimeException is thrown.
   */
  public final EntityPanel getSelectedDetailPanel() {
    if (detailPanelTabbedPane == null) {
      throw new IllegalStateException("No detail panels available");
    }

    return (EntityPanel) detailPanelTabbedPane.getSelectedComponent();
  }

  /**
   * Returns the detail panel for the given <code>entityID</code>, if one is available
   * @param entityID the entiy ID of the detail panel to retrieve
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
  public final void activatePanel() {
    initializePanel();
    if (getMasterPanel() != null) {
      getMasterPanel().showDetailPanel(this);
    }
    //try to grab the focus unless the edit panel already has focus
//    final boolean editPanelHasFocus = isEditPanelParent(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
//    final boolean panelHasFocus = isPanelParent(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());

    prepareUI(true, false);
  }

  /** {@inheritDoc} */
  public final MasterDetailPanel getMasterPanel() {
    MasterDetailPanel parentPanel = masterPanel;
    if (parentPanel == null) {
      parentPanel = UiUtil.getParentOfType(this, EntityApplicationPanel.class);
    }

    return parentPanel;
  }

  /** {@inheritDoc} */
  public final MasterDetailPanel getCurrentDetailPanel() {
    final Collection<EntityPanel> linkedDetailPanels = getLinkedDetailPanels();
    if (!linkedDetailPanels.isEmpty()) {
      return linkedDetailPanels.iterator().next();
    }

    return null;
  }

  /** {@inheritDoc} */
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
  public final List<EntityPanel> getDetailPanels() {
    return Collections.unmodifiableList(detailEntityPanels);
  }

  /** {@inheritDoc} */
  public final void showDetailPanel(final MasterDetailPanel detailPanel) {
    if (detailPanelTabbedPane != null) {
      if (getDetailPanelState() == EntityPanel.HIDDEN) {
        setDetailPanelState(EntityPanel.EMBEDDED);
      }
      detailPanelTabbedPane.setSelectedComponent((JComponent) detailPanel);
    }
  }

  /**
   * @return a control for toggling the edit panel
   */
  public final Control getToggleEditPanelControl() {
    final Control toggle = Controls.methodControl(this, "toggleEditPanelState",
            Images.loadImage("Form16.gif"));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_EDIT_TIP));

    return toggle;
  }

  /**
   * @return a control for toggling the detail panel
   */
  public final Control getToggleDetailPanelControl() {
    final Control toggle = Controls.methodControl(this, "toggleDetailPanelState",
            Images.loadImage(Images.IMG_HISTORY_16));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_DETAIL_TIP));

    return toggle;
  }

  /**
   * By default this delegates to the edit panel
   * @param exception the exception to handle
   */
  public final void handleException(final Exception exception) {
    editPanel.handleException(exception);
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
   */
  public final EntityPanel setDetailSplitPanelResizeWeight(final double detailSplitPanelResizeWeight) {
    if (panelInitialized) {
      throw new IllegalStateException("Can not set detailSplitPanelResizeWeight after initialization");
    }
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
   */
  public final EntityPanel setIncludeDetailPanelTabPane(final boolean includeDetailPanelTabPane) {
    if (panelInitialized) {
      throw new IllegalStateException("Can not set includeDetailPanelTabPane after initialization");
    }
    this.includeDetailPanelTabPane = includeDetailPanelTabPane;
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
   * @return this entity panel
   */
  public final EntityPanel setIncludeControlPanel(final boolean includeControlPanel) {
    if (panelInitialized) {
      throw new IllegalStateException("Can not set includeControlPanel after initialization");
    }
    this.includeControlPanel = includeControlPanel;
    return this;
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
  public final int getDetailPanelState() {
    return detailPanelState;
  }

  /**
   * @return the edit panel state, either HIDDEN, EMBEDDED or DIALOG
   */
  public final int getEditPanelState() {
    return editPanelState;
  }

  /**
   * @param state the detail panel state (HIDDEN or EMBEDDED, DIALOG)
   */
  public final void setDetailPanelState(final int state) {
    if (detailPanelTabbedPane == null) {
      this.detailPanelState = state;
      return;
    }

    if (state != HIDDEN) {
      getSelectedDetailPanel().initializePanel();
    }

    if (detailPanelState == DIALOG) {//if we are leaving the DIALOG state, hide all child detail dialogs
      for (final EntityPanel detailPanel : detailEntityPanels) {
        if (detailPanel.detailPanelState == DIALOG) {
          detailPanel.setDetailPanelState(HIDDEN);
        }
      }
    }

    entityModel.setLinkedDetailModels(state == HIDDEN ? null : getSelectedDetailPanel().entityModel);

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
  public final void setEditPanelState(final int state) {
    if (!containsEditPanel()) {
      return;
    }
    if (editControlPanel == null) {
      editControlPanel = initializeEditControlPanel();
    }

    editPanelState = state;
    if (state != DIALOG) {
      disposeEditDialog();
    }

    if (state == EMBEDDED) {
      if (compactBase != null) {
        compactBase.add(editControlPanel, BorderLayout.NORTH);
      }
      else {
        add(editControlPanel, BorderLayout.NORTH);
      }
      prepareUI(true, false);
    }
    else if (state == HIDDEN) {
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

    revalidate();
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
  public final void resizePanel(final int direction, final int pixelAmount) {
    switch(direction) {
      case UP:
        setEditPanelState(HIDDEN);
        break;
      case DOWN:
        setEditPanelState(EMBEDDED);
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
    LOG.debug(getEditModel().getEntityID() + " prepareUI(" + setInitialFocus + ", " + clearUI + ")");
    if (editPanel != null) {
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
   * @param propertyID the ID of the property
   * @return true if the given property should be included when selecting a input component in the edit panel,
   * returns true by default.
   */
  protected boolean includeComponentSelectionProperty(final String propertyID) {
    return true;
  }

  //#############################################################################################
  // Begin - initialization methods
  //#############################################################################################

  /**
   * Initializes this EntityPanel's UI.
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
    if (!includeDetailPanelTabPane || detailEntityPanels.isEmpty()) {
      horizontalSplitPane = null;
      detailPanelTabbedPane = null;
    }
    else {
      horizontalSplitPane = initializeHorizontalSplitPane();
      detailPanelTabbedPane = initializeDetailTabPane();
    }
    if (editPanel != null) {
      editPanel.initializePanel();
    }
    if (tablePanel != null) {
      final ControlSet toolbarControls = new ControlSet("");
      if (editPanel != null) {
        toolbarControls.add(getToggleEditPanelControl());
      }
      if (!detailEntityPanels.isEmpty()) {
        toolbarControls.add(getToggleDetailPanelControl());
      }
      tablePanel.addToolbarControls(toolbarControls);
      final ControlSet tablePopupControls = getDetailPanelControlSet();
      if (tablePopupControls != null) {
        tablePanel.addPopupControls(tablePopupControls);
      }
      if (tablePanel.getTableDoubleClickAction() == null) {
        tablePanel.setTableDoubleClickAction(initializeTableDoubleClickAction());
      }
      tablePanel.initializePanel();
      tablePanel.setMinimumSize(new Dimension(0,0));
    }

    setLayout(new BorderLayout(5,5));
    if (detailPanelTabbedPane == null) { //no left right split pane
      add(tablePanel, BorderLayout.CENTER);
    }
    else {
      if (compactDetailLayout) {
        compactBase = new JPanel(new BorderLayout(5,5));
        compactBase.add(tablePanel, BorderLayout.CENTER);
        horizontalSplitPane.setLeftComponent(compactBase);
      }
      else {
        horizontalSplitPane.setLeftComponent(tablePanel);
      }
      horizontalSplitPane.setRightComponent(detailPanelTabbedPane);
      add(horizontalSplitPane, BorderLayout.CENTER);
    }
    setupKeyboardActions();
    setDetailPanelState(detailPanelState);
    setEditPanelState(editPanelState);
    if (Configuration.getBooleanValue(Configuration.USE_KEYBOARD_NAVIGATION)) {
      initializeNavigation();
    }
    initializeResizing();
  }

  /**
   * Initializes the keyboard navigation actions.
   * By default CTRL-T transfers focus to the table in case one is available,
   * CTR-E transfers focus to the edit panel in case one is available,
   * CTR-S transfers focus to the search panel, CTR-C opens a select control dialog
   * and CTR-F selects the table search field
   *///todo fix this so that dialogged panels also behave accordingly
  private void setupKeyboardActions() {
    if (containsTablePanel()) {
      UiUtil.addKeyEvent(this, KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              true, new AbstractAction("selectTablePanel") {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent e) {
                  getTablePanel().getJTable().requestFocusInWindow();
                }
              });
      UiUtil.addKeyEvent(this, KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              true, new AbstractAction("selectSearchField") {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent e) {
                  getTablePanel().getSearchField().requestFocusInWindow();
                }
              });
      if (tablePanel.getSearchPanel() != null) {
        UiUtil.addKeyEvent(this, KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                true, new AbstractAction("toggleSearchPanel") {
                  /** {@inheritDoc} */
                  public void actionPerformed(final ActionEvent e) {
                    if (!getTablePanel().isSearchPanelVisible()) {
                      getTablePanel().setSearchPanelVisible(true);
                    }
                    getTablePanel().getSearchPanel().requestFocusInWindow();
                  }
                });
      }
    }
    if (containsEditPanel()) {
      UiUtil.addKeyEvent(this, KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              true, new AbstractAction("selectEditPanel") {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent e) {
                  if (getEditPanelState() == HIDDEN) {
                    setEditPanelState(EMBEDDED);
                  }
                  getEditPanel().prepareUI(true, false);
                }
              });
      UiUtil.addKeyEvent(this, KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              true, new AbstractAction("selectComponent") {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent e) {
                  if (getEditPanelState() == HIDDEN) {
                    setEditPanelState(EMBEDDED);
                  }
                  final Property property = (Property) UiUtil.selectValue(getEditPanel(), getSelectComponentProperties(),
                          Messages.get(Messages.SELECT_INPUT_FIELD));
                  if (property != null) {
                    getEditPanel().selectComponent(property.getPropertyID());
                  }
                }
              });
    }
  }

  /**
   * Called during construction, before controls have been initialized
   */
  protected void initializeAssociatedPanels() {}

  /**
   * Called during construction, after controls have been initialized,
   * use this method to initialize any application panels that rely on controls having been initialized
   */
  protected void initializeControlPanels() {}

  /**
   * Override to add code that should be called during the initialization routine after the UI has been initialized
   */
  protected void initialize() {}

  /**
   * @param masterPanel the panel serving as master panel for this entity panel
   */
  protected final void setMasterPanel(final EntityPanel masterPanel) {
    this.masterPanel = masterPanel;
  }

  /**
   * Initializes the edit control panel.
   *<pre>
   * The default layout is as follows:
   * __________________________________
   * |   edit panel           |control|
   * |  (EntityEditPanel)     | panel | } edit control panel
   * |________________________|_______|
   *
   * or, if the <code>horizontalButtons</code> constructor parameter was true:
   * __________________________
   * |         edit           |
   * |        panel           |
   * |________________________| } edit control panel
   * |     control panel      |
   * |________________________|
   *</pre>
   * @return a panel used for editing entities, if <code>initializeEditPanel()</code>
   * returns null then by default this method returns null as well
   */
  private JPanel initializeEditControlPanel() {
    if (!containsEditPanel()) {
      return null;
    }

    final JPanel panel = new JPanel(new BorderLayout(5,5));
    panel.setMinimumSize(new Dimension(0,0));
    final int alignment = controlPanelConstraints.equals(BorderLayout.SOUTH) || controlPanelConstraints.equals(BorderLayout.NORTH) ? FlowLayout.CENTER : FlowLayout.LEADING;
    final JPanel propertyBase = new JPanel(new FlowLayout(alignment, 5, 5));
    panel.addMouseListener(new ActivationAdapter(editPanel));
    propertyBase.add(editPanel);
    panel.add(propertyBase, BorderLayout.CENTER);
    if (includeControlPanel) {
      final JComponent controlPanel = Configuration.getBooleanValue(Configuration.TOOLBAR_BUTTONS) ?
              editPanel.getControlToolBar(JToolBar.VERTICAL) : editPanel.createControlPanel(alignment == FlowLayout.CENTER);
      if (controlPanel != null) {
        panel.add(controlPanel, controlPanelConstraints);
      }
    }

    return panel;
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

    tabbedPane.addChangeListener(new ChangeListener() {
      /** {@inheritDoc} */
      public void stateChanged(final ChangeEvent e) {
        getModel().setLinkedDetailModels(getDetailPanelState() != HIDDEN ? getSelectedDetailPanel().getModel() : null);
        getSelectedDetailPanel().activatePanel();
      }
    });
    tabbedPane.addMouseListener(new MouseAdapter() {
      /** {@inheritDoc} */
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

    return tabbedPane;
  }

  private void initializeResizing() {
    UiUtil.addKeyEvent(this, KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            new ResizeVerticallyAction(this, DIV_UP, UP));
    UiUtil.addKeyEvent(this, KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            new ResizeVerticallyAction(this, DIV_DOWN, DOWN));
    UiUtil.addKeyEvent(this, KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            new ResizeHorizontallyAction(this, DIV_RIGHT, RIGHT));
    UiUtil.addKeyEvent(this, KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            new ResizeHorizontallyAction(this, DIV_LEFT, LEFT));
  }

  /**
   * Returns a ControlSet containing the detail panel controls, if no detail
   * panels exist null is returned.
   * @return a ControlSet for activating individual detail panels
   * @see #getDetailPanelControls(int)
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
   * Initialize the Action to perform when a double click is performed on the table, if a table is present.
   * The default implementation shows the edit panel in a dialog if one is available and hidden, if that is
   * not the case and the detail panels are hidden those are shown in a dialog.
   * @return the Action to perform when the a double click is performed on the table
   */
  private Action initializeTableDoubleClickAction() {
    return new AbstractAction() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        if (editControlPanel != null || (!detailEntityPanels.isEmpty() && includeDetailPanelTabPane)) {
          if (editControlPanel != null && getEditPanelState() == HIDDEN) {
            setEditPanelState(DIALOG);
          }
          else if (getDetailPanelState() == HIDDEN) {
            setDetailPanelState(DIALOG);
          }
        }
      }
    };
  }

  /**
   * Initializes a ControlSet containing a control for setting the state to <code>status</code> on each detail panel.
   * @param status the status
   * @return a ControlSet for controlling the state of the detail panels
   */
  private ControlSet getDetailPanelControls(final int status) {
    if (detailEntityPanels.isEmpty()) {
      return null;
    }

    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.DETAIL_TABLES));
    for (final EntityPanel detailPanel : detailEntityPanels) {
      controlSet.add(new Control(detailPanel.getCaption()) {
        /** {@inheritDoc} */
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
    SwingUtilities.invokeLater(new Runnable() {
      /** {@inheritDoc} */
      public void run() {
        detailPanelDialog = UiUtil.showInDialog(UiUtil.getParentWindow(EntityPanel.this), detailPanelTabbedPane, false,
                caption + " - " + FrameworkMessages.get(FrameworkMessages.DETAIL_TABLES), false, true,
                null, size, location, new AbstractAction() {
                  public void actionPerformed(final ActionEvent e) {
                    setDetailPanelState(HIDDEN);
                  }
                });
      }
    });
  }

  /**
   * @param parentSize the size of the parent window
   * @return the size to use when showing the detail dialog
   */
  private Dimension getDetailDialogSize(final Dimension parentSize) {
    return new Dimension((int) (parentSize.width / DETAIL_DIALOG_SIZE_RATIO), (editControlPanel != null) ?
            (int) (parentSize.height / DETAIL_DIALOG_SIZE_RATIO) : parentSize.height - DETAIL_DIALOG_HEIGHT_OFFSET);
  }

  /**
   * Shows the edit panel in a non-modal dialog
   */
  private void showEditDialog() {
    final Point location = getLocationOnScreen();
    location.setLocation(location.x + 1, location.y + getSize().height - editControlPanel.getSize().height - EDIT_DIALOG_LOCATION_OFFSET);
    editPanelDialog = UiUtil.showInDialog(UiUtil.getParentWindow(this), editControlPanel, false,
            caption, false, true, null, null, location, new AbstractAction() {
              /** {@inheritDoc} */
              public void actionPerformed(final ActionEvent e) {
                setEditPanelState(HIDDEN);
              }
            });
    editPanel.prepareUI(true, false);
  }

  /**
   * @return a list of properties to use when selecting a input component in the edit panel,
   * this returns all the properties that have mapped components in the edit panel
   * that are enabled, visible and focusable.
   * @see org.jminor.common.ui.valuemap.ValueChangeMapEditPanel#setComponent(Object, javax.swing.JComponent)
   */
  private List<Property> getSelectComponentProperties() {
    final Collection<String> propertyIDs = editPanel.getComponentKeys();
    final Collection<String> selectableComponentPropertyIDs = new ArrayList<String>(propertyIDs.size());
    for (final String propertyID : propertyIDs) {
      final JComponent component = editPanel.getComponent(propertyID);
      if (component != null && includeComponentSelectionProperty(propertyID) && component.isVisible() &&
              component.isFocusable() && component.isEnabled()) {
        selectableComponentPropertyIDs.add(propertyID);
      }
    }
    return EntityUtil.getSortedProperties(entityModel.getEntityID(), selectableComponentPropertyIDs);
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

  private void bindModelEvents() {
    entityModel.addBeforeRefreshListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        UiUtil.setWaitCursor(true, EntityPanel.this);
      }
    });
    entityModel.addAfterRefreshListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        UiUtil.setWaitCursor(false, EntityPanel.this);
      }
    });
  }

  private void bindEvents() {
    addComponentListener(new EntityPanelComponentAdapter());
  }

  private void initializeNavigation() {
    UiUtil.addKeyEvent(this, KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK,
            new NavigateAction(NAVIGATE_UP, UP));
    UiUtil.addKeyEvent(this, KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK,
            new NavigateAction(NAVIGATE_DOWN, DOWN));
    UiUtil.addKeyEvent(this, KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK,
            new NavigateAction(NAVIGATE_RIGHT, RIGHT));
    UiUtil.addKeyEvent(this, KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK,
            new NavigateAction(NAVIGATE_LEFT, LEFT));
  }

  private final class NavigateAction extends AbstractAction {

    private final int direction;

    private NavigateAction(final String name, final int direction) {
      super(name);
      this.direction = direction;
    }

    public void actionPerformed(final ActionEvent e) {
      MasterDetailPanel panel = null;
      switch(direction) {
        case LEFT:
          panel = getPreviousPanel();
          break;
        case RIGHT:
          panel = getNextPanel();
          break;
        case UP:
          panel = getMasterPanel();
          break;
        case DOWN:
          panel = getCurrentDetailPanel();
          break;
      }

      if (panel != null) {
        panel.activatePanel();
      }
    }
  }

  private static final class ResizeHorizontallyAction extends AbstractAction {

    private final EntityPanel panel;
    private final int direction;

    private ResizeHorizontallyAction(final EntityPanel panel, final String action, final int direction) {
      super(action);
      this.panel = panel;
      this.direction = direction;
    }

    public void actionPerformed(final ActionEvent e) {
      final MasterDetailPanel activePanelParent = panel.masterPanel;
      if (activePanelParent != null) {
        ((EntityPanel) activePanelParent).resizePanel(direction, DIVIDER_JUMP);
      }
    }
  }

  private static final class ResizeVerticallyAction extends AbstractAction {

    private final EntityPanel panel;
    private final int direction;

    private ResizeVerticallyAction(final EntityPanel panel, final String action, final int direction) {
      super(action);
      this.panel = panel;
      this.direction = direction;
    }

    public void actionPerformed(final ActionEvent e) {
      panel.resizePanel(direction, DIVIDER_JUMP);
    }
  }

  private static final class ActivationAdapter extends MouseAdapter {

    private final EntityEditPanel target;

    private ActivationAdapter(final EntityEditPanel target) {
      this.target = target;
    }

    /** {@inheritDoc} */
    @Override
    public void mouseReleased(final MouseEvent e) {
      target.requestFocusInWindow();//activates this EntityPanel
    }
  }

  private static class FocusListener implements PropertyChangeListener, Serializable {
    private static final long serialVersionUID = 1;
    /** {@inheritDoc} */
    public void propertyChange(final PropertyChangeEvent evt) {
      final EntityPanel parent = UiUtil.getParentOfType((JComponent) evt.getNewValue(), EntityPanel.class);
      if (parent != null && parent.getEditPanel() != null) {
        parent.getEditPanel().setActive(true);
      }
    }
  }

  private final class EntityPanelComponentAdapter extends ComponentAdapter {
    /** {@inheritDoc} */
    @Override
    public void componentHidden(final ComponentEvent e) {
      SwingUtilities.invokeLater(new Runnable() {
        /** {@inheritDoc} */
        public void run() {
          setFilterPanelsVisible(false);
        }
      });
    }
    /** {@inheritDoc} */
    @Override
    public void componentShown(final ComponentEvent e) {
      SwingUtilities.invokeLater(new Runnable() {
        /** {@inheritDoc} */
        public void run() {
          setFilterPanelsVisible(true);
        }
      });
    }
  }
}