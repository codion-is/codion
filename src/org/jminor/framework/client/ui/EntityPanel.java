/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.WeakPropertyChangeListener;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.common.model.valuemap.ValueChangeMap;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.AbstractFilteredTablePanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.reports.ReportUIWrapper;
import org.jminor.common.ui.valuemap.ValueChangeMapEditPanel;
import org.jminor.common.ui.valuemap.ValueChangeMapPanel;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.ui.reporting.EntityReportUiUtil;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A panel representing a Entity via a EntityModel, which facilitates browsing and editing of records.
 * To lay out the panel components and initialize the panel you must call the method <code>initializePanel()</code>.
 */
public abstract class EntityPanel extends ValueChangeMapPanel<String, Object> {

  public static final int DIALOG = 1;
  public static final int EMBEDDED = 2;
  public static final int HIDDEN = 3;

  public static final int UP = 0;
  public static final int DOWN = 1;
  public static final int RIGHT = 2;
  public static final int LEFT = 3;

  /**
   * true if this panel should be compact
   */
  private final boolean compactDetailLayout;

  /**
   * true if the data should be refreshed (fetched from the database) during initialization
   */
  private final boolean refreshOnInit;

  /**
   * indicates where the edit panel buttons should be placed, either BorderLayout.SOUTH or BorderLayout.EAST
   */
  private final String buttonPlacement;

  /**
   * The caption to use when presenting this entity panel
   */
  private final String caption;

  /**
   * A List containing the detail panels, if any
   */
  private final List<EntityPanel> detailEntityPanels;

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
   * Holds the current state of the edit panel (HIDDEN, EMBEDDED or DIALOG)
   */
  private int editPanelState = EMBEDDED;

  /**
   * Holds the current state of the detail panels (HIDDEN, EMBEDDED or DIALOG)
   */
  private int detailPanelState = HIDDEN;

  /**
   * True after <code>initializePanel()</code> has been called
   */
  private boolean panelInitialized = false;

  /**
   * Hold a reference to this PropertyChangeListener so that it will be garbage collected along with this EntityPanel instance
   */
  private final PropertyChangeListener focusPropertyListener = new PropertyChangeListener() {
    public void propertyChange(final PropertyChangeEvent event) {
      final Component focusOwner = (Component) event.getNewValue();
      if (focusOwner != null && isParentPanel(focusOwner) && !isActive()) {
        getEditModel().setActive(true);
      }
    }
  };

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   */
  public EntityPanel(final EntityModel model, final String caption) {
    this(model, caption, true);
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param refreshOnInit if true then the underlying data model should be refreshed during initialization
   */
  public EntityPanel(final EntityModel model, final String caption, final boolean refreshOnInit) {
    this(model, caption, refreshOnInit, false);
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param refreshOnInit if true then the underlying data model should be refreshed during initialization
   * @param horizontalButtons if true the control panel buttons are laid out horizontally below the edit panel,
   * otherwise vertically on its right side
   */
  public EntityPanel(final EntityModel model, final String caption, final boolean refreshOnInit,
                     final boolean horizontalButtons) {
    this(model, caption, refreshOnInit, horizontalButtons, EMBEDDED);//embedded perhaps not default?
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param refreshOnInit if true then the underlying data model should be refreshed during initialization
   * @param horizontalButtons if true the control panel buttons are laid out horizontally below the edit panel,
   * otherwise vertically on its right side
   * @param detailPanelState the initial detail panel state (HIDDEN or EMBEDDED, DIALOG is not available upon initialization)
   */
  public EntityPanel(final EntityModel model, final String caption, final boolean refreshOnInit,
                     final boolean horizontalButtons, final int detailPanelState) {
    this(model, caption, refreshOnInit, horizontalButtons, detailPanelState,
            Configuration.getBooleanValue(Configuration.COMPACT_ENTITY_PANEL_LAYOUT));
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param refreshOnInit if true then the underlying data model should be refreshed during initialization
   * @param horizontalButtons if true the control panel buttons are laid out horizontally below the edit panel,
   * otherwise vertically on its right side
   * @param detailPanelState the initial detail panel state (HIDDEN or EMBEDDED, DIALOG is not available upon initialization)
   * @param compactDetailLayout true if this panel should be laid out in a compact state
   */
  public EntityPanel(final EntityModel model, final String caption, final boolean refreshOnInit,
                     final boolean horizontalButtons, final int detailPanelState,
                     final boolean compactDetailLayout) {
    super(model);
    this.caption = caption;
    this.refreshOnInit = refreshOnInit;
    this.buttonPlacement = horizontalButtons ? BorderLayout.SOUTH : BorderLayout.EAST;
    this.detailPanelState = detailPanelState;
    this.detailEntityPanels = new ArrayList<EntityPanel>(initializeDetailPanels());
    this.compactDetailLayout = compactDetailLayout && this.detailEntityPanels.size() > 0;
    getEditModel().stateActive().eventStateChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        if (isActive()) {
          initializePanel();
          showPanelTab();
          //do not try to grab the initial focus when a child component already has the focus, for example the table
          prepareUI(!isParentPanel(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()), false);
        }
      }
    });
  }

  /**
   * @return the EntityModel
   */
  @Override
  public EntityModel getModel() {
    return (EntityModel) super.getModel();
  }

  /**
   * @return the EntityEditModel
   */
  public EntityEditModel getEditModel() {
    return getModel().getEditModel();
  }

  /**
   * @return the master panel, if any
   */
  public EntityPanel getMasterPanel() {
    return masterPanel;
  }

  /**
   * Initializes this EntityPanel, in case of some specific initialization code, to show the search panel for example,
   * you can override the <code>initialize()</code> method and add your code there.
   * This method marks this panel as initialized which prevents it from running again, whether or not an exception occurs.
   * @return this EntityPanel instance
   * @see #initialize()
   * @see #isPanelInitialized()
   */
  public EntityPanel initializePanel() {
    if (!isPanelInitialized()) {
      try {
        UiUtil.setWaitCursor(true, this);
        initializeAssociatedPanels();
        initializeControlPanels();
        bindEventsInternal();
        bindEvents();
        bindModelEvents();
        bindTableModelEvents();
        initializeUI();
        bindTablePanelEvents();
        initialize();

        if (refreshOnInit && getModel().containsTableModel()) {
          getModel().getTableModel().refresh();
        }
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
  public boolean isPanelInitialized() {
    return panelInitialized;
  }

  @Override
  public EntityEditPanel getEditPanel() {
    return (EntityEditPanel) super.getEditPanel();
  }

  /**
   * @return the EntityTablePanel used by this EntityPanel
   * @see org.jminor.common.ui.valuemap.ValueChangeMapPanel#initializeTablePanel(org.jminor.common.model.AbstractFilteredTableModel)
   */
  @Override
  public EntityTablePanel getTablePanel() {
    return (EntityTablePanel) super.getTablePanel();
  }

  /**
   * @return the edit control panel
   * @see #initializeEditControlPanel()
   */
  public JPanel getEditControlPanel() {
    return editControlPanel;
  }

  /**
   * @return a List containing the detail EntityPanels, if any
   * @see #initializeDetailPanels()
   */
  public List<EntityPanel> getDetailPanels() {
    return new ArrayList<EntityPanel>(detailEntityPanels);
  }

  /**
   * @return the currently visible/linked detail EntityPanel, if any
   */
  public EntityPanel getLinkedDetailPanel() {
    return detailPanelTabbedPane != null ? (EntityPanel) detailPanelTabbedPane.getSelectedComponent() : null;
  }

  /**
   * @return the detail panel selected in the detail tab pane.
   * If no detail panels are defined a RuntimeException is thrown.
   */
  public EntityPanel getSelectedDetailPanel() {
    if (detailPanelTabbedPane == null) {
      throw new RuntimeException("No detail panels available");
    }

    return (EntityPanel) detailPanelTabbedPane.getSelectedComponent();
  }

  /**
   * Returns the detail panel of the type <code>detailPanelClass</code>, if one is available, otherwise
   * a RuntimeException is thrown
   * @param detailPanelClass the class of the detail panel to retrieve
   * @return the detail panel of the given type
   */
  public EntityPanel getDetailPanel(final Class<? extends EntityPanel> detailPanelClass) {
    for (final EntityPanel detailPanel : detailEntityPanels) {
      if (detailPanel.getClass().equals(detailPanelClass)) {
        return detailPanel;
      }
    }

    throw new RuntimeException("Detail panel of type: " + detailPanelClass + " not found in panel: " + getClass());
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getCaption();
  }

  /**
   * @return the caption to use when presenting this entity panel
   */
  public String getCaption() {
    return caption;
  }

  /**
   * By default this delegates to the edit panel
   * @param exception the exception to handle
   */
  public void handleException(final Exception exception) {
    getEditPanel().handleException(exception);
  }

  /**
   * @return true if this EntityPanel is active and ready to receive input
   */
  public boolean isActive() {
    return getEditModel().stateActive().isActive();
  }

  /**
   * Toggles the detail panel state between DIALOG, HIDDEN and EMBEDDED
   */
  public void toggleDetailPanelState() {
    final int state = getDetailPanelState();
    if (state == DIALOG) {
      setDetailPanelState(HIDDEN);
    }
    else if (state == EMBEDDED) {
      setDetailPanelState(DIALOG);
    }
    else {
      setDetailPanelState(EMBEDDED);
    }
  }

  /**
   * Toggles the edit panel state between DIALOG, HIDDEN and EMBEDDED
   */
  public void toggleEditPanelState() {
    final int state = getEditPanelState();
    if (state == DIALOG) {
      setEditPanelState(HIDDEN);
    }
    else if (state == EMBEDDED) {
      setEditPanelState(DIALOG);
    }
    else {
      setEditPanelState(EMBEDDED);
    }
  }

  /**
   * @return the detail panel state, either HIDDEN, EMBEDDED or DIALOG
   */
  public int getDetailPanelState() {
    return detailPanelState;
  }

  /**
   * @return the edit panel state, either HIDDEN, EMBEDDED or DIALOG
   */
  public int getEditPanelState() {
    return editPanelState;
  }

  /**
   * @param state the detail panel state, either HIDDEN, EMBEDDED or DIALOG
   */
  public void setDetailPanelState(final int state) {
    if (detailPanelTabbedPane == null) {
      return;
    }

    if (state != HIDDEN) {
      getSelectedDetailPanel().initializePanel();
    }

    if (detailPanelState == DIALOG) {//if we are leaving the DIALOG state, hide all child detail dialogs
      for (final EntityPanel detailPanel : detailEntityPanels) {
        if (detailPanel.getDetailPanelState() == DIALOG) {
          detailPanel.setDetailPanelState(HIDDEN);
        }
      }
    }

    getModel().setLinkedDetailModel(state == HIDDEN ? null : getSelectedDetailPanel().getModel());

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
  public void setEditPanelState(final int state) {
    if (editControlPanel == null) {
      return;
    }

    editPanelState = state;
    if (state != DIALOG) {
      disposeEditDialog();
    }

    if (state == EMBEDDED) {
      if (compactDetailLayout) {
        compactBase.add(editControlPanel, BorderLayout.NORTH);
      }
      else {
        add(editControlPanel, BorderLayout.NORTH);
      }
      prepareUI(true, false);
    }
    else if (state == HIDDEN) {
      if (compactDetailLayout) {
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
  public void setFilterPanelsVisible(final boolean value) {
    if (!isPanelInitialized()) {
      return;
    }

    if (getTablePanel() != null) {
      getTablePanel().setFilterPanelsVisible(value);
    }
    for (final EntityPanel detailEntityPanel : detailEntityPanels) {
      detailEntityPanel.setFilterPanelsVisible(value);
    }
  }

  public void resizePanel(final int direction, final int pixelAmount) {
    switch(direction) {
      case UP:
        setEditPanelState(HIDDEN);
        break;
      case DOWN:
        setEditPanelState(EMBEDDED);
        break;
      case RIGHT:
        int newPos = horizontalSplitPane.getDividerLocation() + pixelAmount;
        if (newPos <= horizontalSplitPane.getMaximumDividerLocation()) {
          horizontalSplitPane.setDividerLocation(newPos);
        }
        else {
          horizontalSplitPane.setDividerLocation(horizontalSplitPane.getMaximumDividerLocation());
        }
        break;
      case LEFT:
        newPos = horizontalSplitPane.getDividerLocation() - pixelAmount;
        if (newPos >= 0) {
          horizontalSplitPane.setDividerLocation(newPos);
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
    final EntityEditPanel editPanel = getEditPanel();
    if (editPanel != null) {
      editPanel.prepareUI(setInitialFocus, clearUI);
    }
    else if (setInitialFocus) {
      if (getTablePanel() != null) {
        getTablePanel().getJTable().requestFocus();
      }
      else if (getComponentCount() > 0) {
        getComponents()[0].requestFocus();
      }
    }
  }

  /**
   * @return a list of properties to use when selecting a input component in the edit panel,
   * by default this returns all the properties that have mapped enabled components in the edit panel.
   * @see ValueChangeMapEditPanel#setComponent(Object, javax.swing.JComponent)
   */
  protected List<Property> getSelectComponentProperties() {
    final EntityEditPanel editPanel = getEditPanel();
    final Collection<String> componentKeys = editPanel.getComponentKeys();
    final Collection<String> focusableComponentKeys = new ArrayList<String>(componentKeys.size());
    for (final String key : componentKeys) {
      if (editPanel.getComponent(key).isEnabled()) {
        focusableComponentKeys.add(key);
      }
    }
    return EntityUtil.getSortedProperties(getModel().getEntityID(), focusableComponentKeys);
  }

  public static EntityPanel createInstance(final EntityPanelProvider panelProvider, final EntityModel model) {
    if (model == null) {
      throw new RuntimeException("Can not create a EntityPanel without an EntityModel");
    }
    try {
      return panelProvider.getPanelClass().getConstructor(EntityModel.class).newInstance(model);
    }
    catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof RuntimeException) {
        throw (RuntimeException) ite.getCause();
      }

      throw new RuntimeException(ite.getCause());
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static EntityPanel createInstance(final EntityPanelProvider panelProvider, final EntityDbProvider dbProvider) {
    try {
      return createInstance(panelProvider, panelProvider.getModelClass().getConstructor(
              EntityDbProvider.class).newInstance(dbProvider));
    }
    catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof RuntimeException) {
        throw (RuntimeException) ite.getCause();
      }

      throw new RuntimeException(ite.getCause());
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Finds the next JTabbedPane ancestor and sets the selected component to be this EntityPanel instance
   */
  protected void showPanelTab() {
    final JTabbedPane tp = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, this);
    if (tp != null) {
      tp.setSelectedComponent(this);
    }
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
    editControlPanel = initializeEditControlPanel();
    final EntityTablePanel entityTablePanel = getTablePanel();
    if (entityTablePanel != null) {
      if (entityTablePanel.getTableDoubleClickAction() == null) {
        entityTablePanel.setTableDoubleClickAction(initializeTableDoubleClickAction());
      }
      entityTablePanel.setMinimumSize(new Dimension(0,0));
    }
    horizontalSplitPane = detailEntityPanels.size() > 0 ? initializeHorizontalSplitPane() : null;
    detailPanelTabbedPane = detailEntityPanels.size() > 0 ? initializeDetailTabPane() : null;

    setLayout(new BorderLayout(5,5));
    if (detailPanelTabbedPane == null) { //no left right split pane
      add(entityTablePanel, BorderLayout.CENTER);
    }
    else {
      if (compactDetailLayout) {
        compactBase = new JPanel(new BorderLayout(5,5));
        compactBase.add(entityTablePanel, BorderLayout.CENTER);
        horizontalSplitPane.setLeftComponent(compactBase);
      }
      else {
        horizontalSplitPane.setLeftComponent(entityTablePanel);
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
  protected void setupKeyboardActions() {
    if (containsTablePanel()) {
      UiUtil.addKeyEvent(this, KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              true, new AbstractAction("selectTablePanel") {
                public void actionPerformed(ActionEvent event) {
                  getTablePanel().getJTable().requestFocusInWindow();
                }
              });
      UiUtil.addKeyEvent(this, KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                true, new AbstractAction("selectSearchField") {
                  public void actionPerformed(ActionEvent event) {
                    getTablePanel().getSearchField().requestFocusInWindow();
                  }
                });
      if (getTablePanel().getSearchPanel() != null) {
        UiUtil.addKeyEvent(this, KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                true, new AbstractAction("toggleSearchPanel") {
                  public void actionPerformed(ActionEvent event) {
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
                public void actionPerformed(ActionEvent event) {
                  if (getEditPanelState() == HIDDEN) {
                    setEditPanelState(EMBEDDED);
                  }
                  getEditPanel().prepareUI(true, false);
                }
              });
      UiUtil.addKeyEvent(this, KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
              true, new AbstractAction("selectComponent") {
                public void actionPerformed(ActionEvent event) {
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
  protected JPanel initializeEditControlPanel() {
    if (getEditPanel() == null) {
      return null;
    }

    final JPanel panel = new JPanel(new BorderLayout(5,5));
    panel.setMinimumSize(new Dimension(0,0));
    panel.setBorder(BorderFactory.createEtchedBorder());
    final JPanel propertyBase =
            new JPanel(new FlowLayout(buttonPlacement.equals(BorderLayout.SOUTH) ? FlowLayout.CENTER : FlowLayout.LEADING,5,5));
    panel.addMouseListener(new ActivationFocusAdapter(propertyBase));
    final EntityEditPanel editPanel = getEditPanel();
    propertyBase.add(editPanel);
    panel.add(propertyBase, BorderLayout.CENTER);
    final JComponent controlPanel = Configuration.getBooleanValue(Configuration.TOOLBAR_BUTTONS) ?
            editPanel.getControlToolBar() : editPanel.getControlPanel(buttonPlacement.equals(BorderLayout.SOUTH));
    if (controlPanel != null) {
      panel.add(controlPanel, Configuration.getBooleanValue(Configuration.TOOLBAR_BUTTONS) ?
              (buttonPlacement.equals(BorderLayout.SOUTH) ? BorderLayout.NORTH : BorderLayout.WEST) :
              (buttonPlacement.equals(BorderLayout.SOUTH) ? BorderLayout.SOUTH : BorderLayout.EAST));
    }

    return panel;
  }

  @Override
  protected final ValueChangeMapEditPanel<String, Object> initializeEditPanel(final ValueChangeMapEditModel<String, Object> editModel) {
    return initializeEditPanel((EntityEditModel) editModel);
  }

  protected abstract EntityEditPanel initializeEditPanel(final EntityEditModel editModel);

  @Override
  protected final AbstractFilteredTablePanel<? extends ValueChangeMap<String, Object>> initializeTablePanel(
          final AbstractFilteredTableModel<? extends ValueChangeMap<String, Object>> tableModel) {
    return initializeTablePanel((EntityTableModel) tableModel);
  }

  protected EntityTablePanel initializeTablePanel(final EntityTableModel tableModel) {
    return new EntityTablePanel(tableModel, getTablePopupControlSet(), getToolbarControlSet(), getPrintControls());
  }

  /**
   * Override to provide additional print controls to display in the table popup menu.
   * @return a ControlSet containing the print controls
   */
  protected ControlSet getPrintControls() {
    return null;
  }

  /**
   * Initializes the horizontal split pane, used in the case of detail panel(s)
   * @return the horizontal split pane
   */
  protected JSplitPane initializeHorizontalSplitPane() {
    final JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
    horizontalSplitPane.setBorder(BorderFactory.createEmptyBorder());
    horizontalSplitPane.setOneTouchExpandable(true);
    horizontalSplitPane.setResizeWeight(getDetailSplitPaneResizeWeight());
    horizontalSplitPane.setDividerSize(18);

    return horizontalSplitPane;
  }

  /**
   * @return the resize weight value to use when initializing the left/right split pane, which
   * controls the initial divider placement (0 - 1).
   * Override to control the initial divider placement
   */
  protected double getDetailSplitPaneResizeWeight() {
    return 0.5;
  }

  /**
   * Initializes the JTabbedPane containing the detail panels, used in case of multiple detail panels
   * @return the JTabbedPane for holding detail panels
   */
  protected JTabbedPane initializeDetailTabPane() {
    final JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setFocusable(false);
    tabbedPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final EntityPanel detailPanel : detailEntityPanels) {
      tabbedPane.addTab(detailPanel.getCaption(), detailPanel);
    }

    tabbedPane.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent event) {
        getModel().setLinkedDetailModel(getDetailPanelState() != HIDDEN ? getSelectedDetailPanel().getModel() : null);
        getSelectedDetailPanel().initializePanel();
      }
    });
    tabbedPane.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(MouseEvent event) {
        if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
          setDetailPanelState(getDetailPanelState() == DIALOG ? EMBEDDED : DIALOG);
        }
        else if (event.getButton() == MouseEvent.BUTTON2) {
          setDetailPanelState(getDetailPanelState() == EMBEDDED ? HIDDEN : EMBEDDED);
        }
      }
    });

    return tabbedPane;
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
   * Instantiates the detail panels according to the result of <code>getDetailPanelProviders</code> method.
   * This method should return an empty List instead of null if overridden.
   * This method is responsible for setting the master panel value of the returned detail panels
   * @return a List containing the detail EntityPanels
   * @see #setMasterPanel(EntityPanel)
   */
  protected List<? extends EntityPanel> initializeDetailPanels() {
    final List<EntityPanel> detailEntityPanels = new ArrayList<EntityPanel>();
    for (final EntityPanelProvider detailPanelProvider : getDetailPanelProviders()) {
      final EntityModel detailModel = getModel().getDetailModel(detailPanelProvider.getModelClass());
      if (detailModel == null) {
        throw new RuntimeException("Detail model of type " + detailPanelProvider.getModelClass()
                + " not found in model of type " + getModel().getClass());
      }
      final EntityPanel detailPanel = createInstance(detailPanelProvider, detailModel);
      detailPanel.setMasterPanel(this);
      detailEntityPanels.add(detailPanel);
    }

    return detailEntityPanels;
  }

  /**
   * This method should return an empty List instead of null if overridden.
   * @return a list of EntityPanelProvider objects, specifying the detail panels this panel should contain
   */
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return new ArrayList<EntityPanelProvider>(0);
  }

  /**
   * Returns a ControlSet containing the detail panel controls, if no detail
   * panels exist the resulting ControlSet will be empty.
   * @return the ControlSet on which the table popup menu is based
   * @see #getDetailPanelControls(int)
   */
  protected ControlSet getTablePopupControlSet() {
    final ControlSet controlSet = new ControlSet("");
    if (detailEntityPanels.size() > 0) {
      controlSet.add(getDetailPanelControls(EMBEDDED));
    }

    return controlSet;
  }

  /**
   * @return the ControlSet on which the table popup menu is based
   */
  protected ControlSet getToolbarControlSet() {
    final ControlSet controlSet = new ControlSet("");
    if (getEditPanel() != null) {
      controlSet.add(getToggleEditPanelControl());
    }
    if (detailEntityPanels.size() > 0) {
      controlSet.add(getToggleDetailPanelControl());
    }

    return controlSet;
  }

  /**
   * Initialize the Action to perform when a double click is performed on the table, if a table is present.
   * The default implementation shows the edit panel in a dialog if one is available and hidden, if that is
   * not the case and the detail panels are hidden those are shown in a dialog.
   * @return the Action to perform when the a double click is performed on the table
   */
  protected Action initializeTableDoubleClickAction() {
    return new AbstractAction() {
      public void actionPerformed(final ActionEvent event) {
        if (editControlPanel != null || detailEntityPanels.size() > 0) {
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
  protected ControlSet getDetailPanelControls(final int status) {
    if (detailEntityPanels.size() == 0) {
      return null;
    }

    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.DETAIL_TABLES));
    for (final EntityPanel detailPanel : detailEntityPanels) {
      controlSet.add(new Control(detailPanel.getCaption()) {
        @Override
        public void actionPerformed(ActionEvent event) {
          detailPanelTabbedPane.setSelectedComponent(detailPanel);
          setDetailPanelState(status);
        }
      });
    }

    return controlSet;
  }

  /**
   * Override to keep event bindings in one place,
   * this method is called during initialization before the UI is initialized
   */
  protected void bindEvents() {}

  /**
   * Override to keep table model event bindings in one place,
   * this method is called during initialization before the UI is initialized
   */
  protected void bindTableModelEvents() {}

  /**
   * Binds events associated with the EntityTablePanel
   * this method is called during initialization after the UI is initialized
   */
  protected void bindTablePanelEvents() {
    if (getTablePanel() == null) {
      return;
    }

    getModel().eventEntitiesChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        getTablePanel().getJTable().repaint();
      }
    });
  }

  //#############################################################################################
  // End - initialization methods
  //#############################################################################################

  /**
   * Shows the detail panels in a non-modal dialog
   */
  protected void showDetailDialog() {
    final Window parent = UiUtil.getParentWindow(this);
    final Dimension parentSize = parent.getSize();
    final Dimension size = getDetailDialogSize(parentSize);
    final Point parentLocation = parent.getLocation();
    final Point location = new Point(parentLocation.x+(parentSize.width-size.width),
            parentLocation.y+(parentSize.height-size.height)-29);
    detailPanelDialog = UiUtil.showInDialog(UiUtil.getParentWindow(this), detailPanelTabbedPane, false,
            getCaption() + " - " + FrameworkMessages.get(FrameworkMessages.DETAIL_TABLES), false, true,
            null, size, location, new AbstractAction() {
              public void actionPerformed(ActionEvent event) {
                setDetailPanelState(HIDDEN);
              }
            });
  }

  /**
   * @param parentSize the size of the parent window
   * @return the size to use when showing the detail dialog
   */
  protected Dimension getDetailDialogSize(final Dimension parentSize) {
    return new Dimension((int) (parentSize.width/1.5),
            (editControlPanel != null) ? (int) (parentSize.height/1.5) : parentSize.height-54);
  }

  /**
   * Shows the edit panel in a non-modal dialog
   */
  protected void showEditDialog() {
    final Point location = getLocationOnScreen();
    location.setLocation(location.x+1, location.y + getSize().height- editControlPanel.getSize().height-98);
    editPanelDialog = UiUtil.showInDialog(UiUtil.getParentWindow(this), editControlPanel, false,
            getCaption(), false, true, null, null, location, new AbstractAction() {
              public void actionPerformed(ActionEvent event) {
                setEditPanelState(HIDDEN);
              }
            });
    getEditPanel().prepareUI(true, false);
  }

  /**
   * Shows a JRViewer for report printing
   * @param reportWrapper the report wrapper
   * @param uiWrapper the ui wrapper
   * @param reportParameters a map containing the parameters required for the report
   * @param frameTitle the title to display on the frame
   */
  protected void viewJdbcReport(final ReportWrapper reportWrapper, final ReportUIWrapper uiWrapper,
                                final Map<String, Object> reportParameters, final String frameTitle) {
    try {
      UiUtil.setWaitCursor(true, this);
      EntityReportUiUtil.viewReport(getModel().fillReport(reportWrapper, reportParameters), uiWrapper, frameTitle);
    }
    catch (ReportException e) {
      throw new RuntimeException(e);
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
  }

  /**
   * Shows a JRViewer for report printing
   * @param reportParameters a map containing the parameters required for the report
   * @param reportWrapper the report wrapper
   * @param uiWrapper the ui wrapper
   * @param dataSource the JRDataSource used to provide the report data
   * @param frameTitle the title to display on the frame
   */
  protected void viewReport(final ReportWrapper reportWrapper, final ReportUIWrapper uiWrapper,
                            final ReportDataWrapper dataSource, final Map<String, Object> reportParameters,
                            final String frameTitle) {
    try {
      UiUtil.setWaitCursor(true, this);
      EntityReportUiUtil.viewReport(getModel().fillReport(reportWrapper, dataSource, reportParameters), uiWrapper, frameTitle);
    }
    catch (ReportException e) {
      throw new RuntimeException(e);
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
  }

  private Control getToggleEditPanelControl() {
    if (editControlPanel == null) {
      return null;
    }

    final Control toggle = ControlFactory.methodControl(this, "toggleEditPanelState",
            Images.loadImage("Form16.gif"));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_EDIT_TIP));

    return toggle;
  }

  private Control getToggleDetailPanelControl() {
    if (detailEntityPanels.size() == 0) {
      return null;
    }

    final Control toggle = ControlFactory.methodControl(this, "toggleDetailPanelState",
            Images.loadImage(Images.IMG_HISTORY_16));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_DETAIL_TIP));

    return toggle;
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
    getModel().eventRefreshStarted().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        UiUtil.setWaitCursor(true, EntityPanel.this);
      }
    });
    getModel().eventRefreshDone().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        UiUtil.setWaitCursor(false, EntityPanel.this);
      }
    });
  }

  private void setMasterPanel(final EntityPanel masterPanel) {
    this.masterPanel = masterPanel;
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

  private void bindEventsInternal() {
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentHidden(ComponentEvent event) {
        setFilterPanelsVisible(false);
      }
      @Override
      public void componentShown(ComponentEvent event) {
        setFilterPanelsVisible(true);
      }
    });
  }

  private static class ActivationFocusAdapter extends MouseAdapter {

    private final JComponent target;

    public ActivationFocusAdapter(final JComponent target) {
      this.target = target;
    }

    @Override
    public void mouseReleased(MouseEvent event) {
      target.requestFocusInWindow();//activates this EntityPanel
    }
  }
}