/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Util;
import org.jminor.common.model.WeakPropertyChangeListener;
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
import java.awt.Component;
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
public class EntityPanel extends JPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityPanel.class);

  public static final int DIALOG = 1;
  public static final int EMBEDDED = 2;
  public static final int HIDDEN = 3;

  public static final int UP = 0;
  public static final int DOWN = 1;
  public static final int RIGHT = 2;
  public static final int LEFT = 3;

  private static final double DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT = 0.5;
  private static final int SPLIT_PANE_DIVIDER_SIZE = 18;
  private static final int DETAIL_DIALOG_OFFSET = 29;
  private static final double DETAIL_DIALOG_SIZE_RATIO = 1.5;
  private static final int DETAIL_DIALOG_HEIGHT_OFFSET = 54;
  private static final int EDIT_DIALOG_LOCATION_OFFSET = 98;

  /**
   * The EntityModel instance used by this EntityPanel
   */
  private final EntityModel model;

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
  private boolean includeControlPanel = true;

  /**
   * True after <code>initializePanel()</code> has been called
   */
  private boolean panelInitialized = false;

  private double detailSplitPanelResizeWeight = DEFAULT_SPLIT_PANEL_RESIZE_WEIGHT;

  /**
   * Hold a reference to this PropertyChangeListener so that it will be garbage collected along with this EntityPanel instance
   */
  private final PropertyChangeListener focusPropertyListener = new FocusListener();

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * The default caption of the underlying entity is used.
   * @param model the EntityModel
   */
  public EntityPanel(final EntityModel model) {
    this(model, Entities.getCaption(model.getEntityID()));
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   */
  public EntityPanel(final EntityModel model, final String caption) {
    this(model, caption, (EntityEditPanel) null);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param editPanel the edit panel
   */
  public EntityPanel(final EntityModel model, final EntityEditPanel editPanel) {
    this(model, Entities.getCaption(model.getEntityID()), editPanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param tablePanel the table panel
   */
  public EntityPanel(final EntityModel model, final EntityTablePanel tablePanel) {
    this(model, Entities.getCaption(model.getEntityID()), tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param editPanel the edit panel
   */
  public EntityPanel(final EntityModel model, final String caption, final EntityEditPanel editPanel) {
    this(model, caption, editPanel, null);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param tablePanel the table panel
   */
  public EntityPanel(final EntityModel model, final String caption, final EntityTablePanel tablePanel) {
    this(model, caption, null, tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param editPanel the edit panel
   * @param tablePanel the table panel
   */
  public EntityPanel(final EntityModel model, final EntityEditPanel editPanel, final EntityTablePanel tablePanel) {
    this(model, Entities.getCaption(model.getEntityID()), editPanel, tablePanel);
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param editPanel the edit panel
   * @param tablePanel the table panel
   */
  public EntityPanel(final EntityModel model, final String caption, final EntityEditPanel editPanel,
                     final EntityTablePanel tablePanel) {
    Util.rejectNullValue(model, "model");
    this.model = model;
    this.caption = caption == null ? model.getEntityID() : caption;
    this.editPanel = editPanel;
    if (editPanel != null) {
      this.editPanel.getActiveState().addListener(new ActivationListener());
    }
    if (tablePanel == null && model.containsTableModel()) {
      this.tablePanel = new EntityTablePanel(model.getTableModel());
    }
    else {
      this.tablePanel = tablePanel;
    }
  }

  /**
   * @return the EntityModel
   */
  public final EntityModel getModel() {
    return model;
  }

  /**
   * @return the EntityEditModel
   */
  public final EntityEditModel getEditModel() {
    return model.getEditModel();
  }

  /**
   * @return the EntityTableModel, null if none is available
   */
  public final EntityTableModel getTableModel() {
    return model.getTableModel();
  }

  /**
   * @return the master panel, if any
   */
  public final EntityPanel getMasterPanel() {
    return masterPanel;
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
   * Adds the given detail panel
   * @param detailPanel the detail panel to add
   * @return this entity panel
   */
  public final EntityPanel addDetailPanel(final EntityPanel detailPanel) {
    if (panelInitialized) {
      throw new IllegalStateException("Can not add detail panel after initialization");
    }
    detailPanel.masterPanel = this;
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

    //do not try to grab the initial focus when a child component already has the focus, for example the table
    final boolean grabInitialFocus = !isParentPanel(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
    prepareUI(grabInitialFocus, false);

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
   * @return an unmodifiable list containing the detail EntityPanels, if any
   */
  public final List<EntityPanel> getDetailPanels() {
    return Collections.unmodifiableList(detailEntityPanels);
  }

  /**
   * @return the currently visible/linked detail EntityPanel, if any
   */
  public final EntityPanel getLinkedDetailPanel() {
    return detailPanelTabbedPane != null ? (EntityPanel) detailPanelTabbedPane.getSelectedComponent() : null;
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
   * Returns the detail panel of the type <code>detailPanelClass</code>, if one is available, otherwise
   * a RuntimeException is thrown
   * @param detailPanelClass the class of the detail panel to retrieve
   * @return the detail panel of the given type
   */
  public final EntityPanel getDetailPanel(final Class<? extends EntityPanel> detailPanelClass) {
    for (final EntityPanel detailPanel : detailEntityPanels) {
      if (detailPanel.getClass().equals(detailPanelClass)) {
        return detailPanel;
      }
    }

    throw new IllegalArgumentException("Detail panel of type: " + detailPanelClass + " not found in panel: " + getClass());
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

    model.setLinkedDetailModels(state == HIDDEN ? null : getSelectedDetailPanel().model);

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
      if (compactDetailLayout && !detailEntityPanels.isEmpty()) {
        compactBase.add(editControlPanel, BorderLayout.NORTH);
      }
      else {
        add(editControlPanel, BorderLayout.NORTH);
      }
      prepareUI(true, false);
    }
    else if (state == HIDDEN) {
      if (compactDetailLayout && !detailEntityPanels.isEmpty()) {
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
        final int rightPos = horizontalSplitPane.getDividerLocation() + pixelAmount;
        if (rightPos <= horizontalSplitPane.getMaximumDividerLocation()) {
          horizontalSplitPane.setDividerLocation(rightPos);
        }
        else {
          horizontalSplitPane.setDividerLocation(horizontalSplitPane.getMaximumDividerLocation());
        }
        break;
      case LEFT:
        final int leftPos = horizontalSplitPane.getDividerLocation() - pixelAmount;
        if (leftPos >= 0) {
          horizontalSplitPane.setDividerLocation(leftPos);
        }
        else {
          horizontalSplitPane.setDividerLocation(0);
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
    if (editPanel != null) {
      editPanel.initializePanel();
    }
    if (tablePanel != null) {
      final ControlSet toolbarControls = new ControlSet("");
      if (editPanel != null) {
        toolbarControls.add(getToggleEditPanelControl());
      }
      if (this.model.getDetailModels().size() > 0) {
        toolbarControls.add(getToggleDetailPanelControl());
      }
      tablePanel.setAdditionalToolbarControls(toolbarControls);
      tablePanel.setAdditionalPopupControls(getTablePopupControlSet());
      if (tablePanel.getTableDoubleClickAction() == null) {
        tablePanel.setTableDoubleClickAction(initializeTableDoubleClickAction());
      }
      tablePanel.initializePanel();
      tablePanel.setMinimumSize(new Dimension(0,0));
    }
    horizontalSplitPane = !detailEntityPanels.isEmpty() ? initializeHorizontalSplitPane() : null;
    detailPanelTabbedPane = !detailEntityPanels.isEmpty() ? initializeDetailTabPane() : null;

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
    setDetailPanelState(detailPanelState);
    setEditPanelState(editPanelState);
    setupKeyboardActions();
    if (Configuration.getBooleanValue(Configuration.USE_FOCUS_ACTIVATION)) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner",
              new WeakPropertyChangeListener(focusPropertyListener));
    }
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
                    getTablePanel().toggleSearchPanel();
                    if (getTablePanel().isSearchPanelVisible()) {
                      getTablePanel().getSearchPanel().requestFocusInWindow();
                    }
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
    panel.addMouseListener(new ActivationFocusAdapter(propertyBase));
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
      if (detailPanel.editPanel != null) {
        detailPanel.editPanel.getActiveState().addListener(new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            if (detailPanel.editPanel.isActive()) {
              LOG.debug(getEditModel().getEntityID() + " selectDetailPanelTab: " + detailPanel.getEditModel().getEntityID());
              tabbedPane.setSelectedComponent(detailPanel);
            }
          }
        });
      }
    }

    tabbedPane.addChangeListener(new ChangeListener() {
      /** {@inheritDoc} */
      public void stateChanged(final ChangeEvent e) {
        getModel().setLinkedDetailModels(getDetailPanelState() != HIDDEN ? getSelectedDetailPanel().getModel() : null);
        getSelectedDetailPanel().initializePanel();
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

  /**
   * Returns a ControlSet containing the detail panel controls, if no detail
   * panels exist the resulting ControlSet will be empty.
   * @return the ControlSet on which the table popup menu is based
   * @see #getDetailPanelControls(int)
   */
  private ControlSet getTablePopupControlSet() {
    final ControlSet controlSet = new ControlSet("");
    if (!detailEntityPanels.isEmpty()) {
      controlSet.add(getDetailPanelControls(EMBEDDED));
    }

    return controlSet;
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
        if (editControlPanel != null || !detailEntityPanels.isEmpty()) {
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
          detailPanelTabbedPane.setSelectedComponent(detailPanel);
          setDetailPanelState(status);
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
      if (includeComponentSelectionProperty(propertyID) && component.isVisible() &&
              component.isFocusable() && component.isEnabled()) {
        selectableComponentPropertyIDs.add(propertyID);
      }
    }
    return EntityUtil.getSortedProperties(model.getEntityID(), selectableComponentPropertyIDs);
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
    model.addBeforeRefreshListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        UiUtil.setWaitCursor(true, EntityPanel.this);
      }
    });
    model.addAfterRefreshListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        UiUtil.setWaitCursor(false, EntityPanel.this);
      }
    });
  }

  /**
   * @param component the component
   * @return true if <code>component</code> is a child component of this EntityPanel
   */
  private boolean isParentPanel(final Component component) {
    final EntityPanel parent = (EntityPanel) SwingUtilities.getAncestorOfClass(EntityPanel.class, component);
    if (parent == this) {
      return true;
    }

    //is editPanelDialog parent?
    return editPanelDialog != null && SwingUtilities.getWindowAncestor(component) == editPanelDialog;
  }

  private void bindEvents() {
    addComponentListener(new EntityPanelComponentAdapter());
  }

  private final class ActivationListener implements ActionListener {
    /** {@inheritDoc} */
    public void actionPerformed(final ActionEvent e) {
      if (editPanel.isActive()) {
        initializePanel();
      }
    }
  }

  private class FocusListener implements PropertyChangeListener, Serializable {
    private static final long serialVersionUID = 1;
    /** {@inheritDoc} */
    public void propertyChange(final PropertyChangeEvent evt) {
      final Component focusOwner = (Component) evt.getNewValue();
      if (focusOwner != null && isParentPanel(focusOwner) && !editPanel.isActive()) {
        LOG.debug(editPanel.getEntityEditModel().getEntityID() + " focusActivation");
        getEditPanel().setActive(true);
      }
    }
  }

  private static final class ActivationFocusAdapter extends MouseAdapter {

    private final JComponent target;

    private ActivationFocusAdapter(final JComponent target) {
      this.target = target;
    }

    /** {@inheritDoc} */
    @Override
    public void mouseReleased(final MouseEvent e) {
      target.requestFocusInWindow();//activates this EntityPanel
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