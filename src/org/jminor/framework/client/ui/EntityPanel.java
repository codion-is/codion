/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.AggregateState;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.WeakPropertyChangeListener;
import org.jminor.common.ui.DefaultExceptionHandler;
import org.jminor.common.ui.ExceptionHandler;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.input.BooleanInputProvider;
import org.jminor.common.ui.input.DateInputProvider;
import org.jminor.common.ui.input.DoubleInputProvider;
import org.jminor.common.ui.input.InputProvider;
import org.jminor.common.ui.input.InputProviderPanel;
import org.jminor.common.ui.input.IntInputProvider;
import org.jminor.common.ui.input.TextInputProvider;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertyValueListProvider;
import org.jminor.framework.client.model.exception.ValidationException;
import org.jminor.framework.client.ui.reporting.EntityReportUiUtil;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;
import org.json.JSONException;

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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * A panel representing a Entity via a EntityModel, which facilitates browsing and editing of records.
 */
public abstract class EntityPanel extends JPanel implements ExceptionHandler {

  private static final Logger log = Util.getLogger(EntityPanel.class);

  public static final int CONFIRM_TYPE_DELETE = 0;
  public static final int CONFIRM_TYPE_UPDATE = 1;
  public static final int CONFIRM_TYPE_INSERT = 2;

  public static final int ACTION_NONE = -1;
  public static final int ACTION_INSERT = 0;
  public static final int ACTION_UPDATE = 1;
  public static final int ACTION_DELETE = 2;

  public static final int DIALOG = 1;
  public static final int EMBEDDED = 2;
  public static final int HIDDEN = 3;

  //Control codes
  public static final String INSERT = "insert";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";
  public static final String REFRESH = "refresh";
  public static final String CLEAR = "clear";
  public static final String PRINT = "print";
  public static final String MENU_DELETE = "menuDelete";
  public static final String VIEW_DEPENDENCIES = "viewDependencies";
  public static final String UPDATE_SELECTED = "updateSelected";
  public static final String CONFIGURE_QUERY = "configureQuery";
  public static final String SELECT_COLUMNS = "selectTableColumns";
  public static final String EXPORT_JSON = "exportJSON";

  public static final int UP = 0;
  public static final int DOWN = 1;
  public static final int RIGHT = 2;
  public static final int LEFT = 3;

  /**
   * Indicates whether the panel is active and ready to receive input
   */
  protected final State stActive = new State(Configuration.getBooleanValue(Configuration.ALL_PANELS_ACTIVE));

  private final Map<String, Control> controlMap = new HashMap<String, Control>();

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
   * The EntityModel instance used by this EntityPanel
   */
  private final EntityModel model;

  /**
   * The EntityTablePanel instance used by this EntityPanel
   */
  private final EntityTablePanel entityTablePanel;

  /**
   * A List containing the detail panels, if any
   */
  private final List<EntityPanel> detailEntityPanels;

  /**
   * The edit panel which contains the controls required for editing a entity
   */
  private JPanel editControlPanel;

  /**
   * The EntityEditPanel instance
   */
  private EntityEditPanel editPanel;

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
   * The mechanism for restricting a single active EntityPanel at a time
   */
  private static final State.StateGroup activeStateGroup = new State.StateGroup();

  /**
   * Hold a reference to this PropertyChangeListener so that it will be garbage collected along with this EntityPanel instance
   */
  private final PropertyChangeListener focusPropertyListener = new PropertyChangeListener() {
    public void propertyChange(final PropertyChangeEvent event) {
      final Component focusOwner = (Component) event.getNewValue();
      if (focusOwner != null && isParentPanel(focusOwner) && !isActive())
        setActive(true);
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
    this(model, caption, refreshOnInit, true);
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param refreshOnInit if true then the underlying data model should be refreshed during initialization
   * @param rowColoring if true then each row in the table model (if any)
   */
  public EntityPanel(final EntityModel model, final String caption, final boolean refreshOnInit, final boolean rowColoring) {
    this(model, caption, refreshOnInit, rowColoring, false);
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param refreshOnInit if true then the underlying data model should be refreshed during initialization
   * @param rowColoring if true then each row in the table model (if any)
   * is colored according to the underlying entity
   * @param horizontalButtons if true the control panel buttons are laid out horizontally below the edit panel,
   * otherwise vertically on its right side
   */
  public EntityPanel(final EntityModel model, final String caption, final boolean refreshOnInit,
                     final boolean rowColoring, final boolean horizontalButtons) {
    this(model, caption, refreshOnInit, rowColoring, horizontalButtons, EMBEDDED);//embedded perhaps not default?
  }

  /**
   * Initializes a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param refreshOnInit if true then the underlying data model should be refreshed during initialization
   * @param rowColoring if true then each row in the table model (if any) is colored according to the underlying entity
   * @param horizontalButtons if true the control panel buttons are laid out horizontally below the edit panel,
   * otherwise vertically on its right side
   * @param detailPanelState the initial detail panel state (HIDDEN or EMBEDDED, DIALOG is not available upon initialization)
   */
  public EntityPanel(final EntityModel model, final String caption, final boolean refreshOnInit,
                     final boolean rowColoring, final boolean horizontalButtons, final int detailPanelState) {
    this(model, caption, refreshOnInit, rowColoring, horizontalButtons, detailPanelState,
            Configuration.getBooleanValue(Configuration.COMPACT_ENTITY_PANEL_LAYOUT));
  }

  /**
   * Instantiates a new EntityPanel instance. The Panel is not laid out and initialized until initialize() is called.
   * @param model the EntityModel
   * @param caption the caption to use when presenting this entity panel
   * @param refreshOnInit if true then the underlying data model should be refreshed during initialization
   * @param rowColoring if true then each row in the table model (if any) is colored according to the underlying entity
   * @param horizontalButtons if true the control panel buttons are laid out horizontally below the edit panel,
   * otherwise vertically on its right side
   * @param detailPanelState the initial detail panel state (HIDDEN or EMBEDDED, DIALOG is not available upon initialization)
   * @param compactDetailLayout true if this panel should be laid out in a compact state
   */
  public EntityPanel(final EntityModel model, final String caption, final boolean refreshOnInit,
                     final boolean rowColoring, final boolean horizontalButtons, final int detailPanelState,
                     final boolean compactDetailLayout) {
    if (model == null)
      throw new IllegalArgumentException("Can not construct a EntityPanel without a EntityModel instance");
    if (!Configuration.getBooleanValue(Configuration.ALL_PANELS_ACTIVE))
      activeStateGroup.addState(stActive);
    this.model = model;
    this.caption = caption;
    this.refreshOnInit = refreshOnInit;
    this.buttonPlacement = horizontalButtons ? BorderLayout.SOUTH : BorderLayout.EAST;
    this.detailPanelState = detailPanelState;
    this.detailEntityPanels = new ArrayList<EntityPanel>(initializeDetailPanels());
    this.compactDetailLayout = compactDetailLayout && this.detailEntityPanels.size() > 0;
    setupControls();
    this.entityTablePanel = model.containsTableModel() ? initializeTablePanel(model.getTableModel(),
            getTablePopupControlSet(), rowColoring) : null;
    this.stActive.eventStateChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        if (isActive()) {
          initializePanel();
          showPanelTab();
          //do not try to grab the default focus when a child component already has the focus, for example the table
          prepareUI(!isParentPanel(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()), false);
        }
      }
    });
  }

  /**
   * @return the EntityModel
   */
  public EntityModel getModel() {
    return model;
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

        if (refreshOnInit && getModel().containsTableModel())
          getModel().getTableModel().refresh();
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

  /**
   * @return the EntityTablePanel used by this EntityPanel
   * @see #initializeTablePanel(org.jminor.framework.client.model.EntityTableModel, org.jminor.common.ui.control.ControlSet, boolean)
   */
  public EntityTablePanel getTablePanel() {
    return entityTablePanel;
  }

  /**
   * @return the edit control panel
   * @see #initializeEditControlPanel()
   */
  public JPanel getEditControlPanel() {
    return editControlPanel;
  }

  /**
   * @return the edit panel
   * @see #initializeEditPanel(org.jminor.framework.client.model.EntityEditModel)
   */
  public EntityEditPanel getEditPanel() {
    return editPanel;
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
    if (detailPanelTabbedPane == null)
      throw new RuntimeException("No detail panels available");

    return (EntityPanel) detailPanelTabbedPane.getSelectedComponent();
  }

  /**
   * Returns the detail panel of the type <code>detailPanelClass</code>, if one is available, otherwise
   * a RuntimeException is thrown
   * @param detailPanelClass the class of the detail panel to retrieve
   * @return the detail panel of the given type
   */
  public EntityPanel getDetailPanel(final Class<? extends EntityPanel> detailPanelClass) {
    for (final EntityPanel detailPanel : detailEntityPanels)
      if (detailPanel.getClass().equals(detailPanelClass))
        return detailPanel;

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
   * @return true if this EntityPanel is active and ready to receive input
   */
  public boolean isActive() {
    return stActive.isActive();
  }

  /**
   * @param active true if this EntityPanel should be activated for receiving input
   */
  public void setActive(final boolean active) {
    stActive.setActive(active);
  }

  /**
   * Toggles the detail panel state between DIALOG, HIDDEN and EMBEDDED
   */
  public void toggleDetailPanelState() {
    final int state = getDetailPanelState();
    if (state == DIALOG)
      setDetailPanelState(HIDDEN);
    else if (state == EMBEDDED)
      setDetailPanelState(DIALOG);
    else
      setDetailPanelState(EMBEDDED);
  }

  /**
   * Toggles the edit panel state between DIALOG, HIDDEN and EMBEDDED
   */
  public void toggleEditPanelState() {
    final int state = getEditPanelState();
    if (state == DIALOG)
      setEditPanelState(HIDDEN);
    else if (state == EMBEDDED)
      setEditPanelState(DIALOG);
    else
      setEditPanelState(EMBEDDED);
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
    if (detailPanelTabbedPane == null)
      return;

    if (state != HIDDEN)
      getSelectedDetailPanel().initializePanel();

    if (detailPanelState == DIALOG)//if we are leaving the DIALOG state, hide all child detail dialogs
      for (final EntityPanel detailPanel : detailEntityPanels)
        if (detailPanel.getDetailPanelState() == DIALOG)
          detailPanel.setDetailPanelState(HIDDEN);

    getModel().setLinkedDetailModel(state == HIDDEN ? null : getSelectedDetailPanel().getModel());

    detailPanelState = state;
    if (state != DIALOG)
      disposeDetailDialog();

    if (state == EMBEDDED)
      horizontalSplitPane.setRightComponent(detailPanelTabbedPane);
    else if (state == HIDDEN)
      horizontalSplitPane.setRightComponent(null);
    else
      showDetailDialog();

    revalidate();
  }

  /**
   * @param state the edit panel state, either HIDDEN, EMBEDDED or DIALOG
   */
  public void setEditPanelState(final int state) {
    if (editControlPanel == null)
      return;

    editPanelState = state;
    if (state != DIALOG)
      disposeEditDialog();

    if (state == EMBEDDED) {
      if (compactDetailLayout)
        compactBase.add(editControlPanel, BorderLayout.NORTH);
      else
        add(editControlPanel, BorderLayout.NORTH);
      prepareUI(true, false);
    }
    else if (state == HIDDEN) {
      if (compactDetailLayout)
        compactBase.remove(editControlPanel);
      else
        remove(editControlPanel);
    }
    else
      showEditDialog();

    revalidate();
  }

  /**
   * Hides or shows the active filter panels for this panel and all its child panels
   * (detail panels and their detail panels etc.)
   * @param value true if the active panels should be shown, false if they should be hidden
   */
  public void setFilterPanelsVisible(final boolean value) {
    if (!isPanelInitialized())
      return;

    if (entityTablePanel != null)
      entityTablePanel.setFilterPanelsVisible(value);
    for (final EntityPanel detailEntityPanel : detailEntityPanels)
      detailEntityPanel.setFilterPanelsVisible(value);
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
        if (newPos <= horizontalSplitPane.getMaximumDividerLocation())
          horizontalSplitPane.setDividerLocation(newPos);
        else
          horizontalSplitPane.setDividerLocation(horizontalSplitPane.getMaximumDividerLocation());
        break;
      case LEFT:
        newPos = horizontalSplitPane.getDividerLocation() - pixelAmount;
        if (newPos >= 0)
          horizontalSplitPane.setDividerLocation(newPos);
        else
          horizontalSplitPane.setDividerLocation(0);
        break;
    }
  }

  public void handleException(final Throwable throwable) {
    if (throwable instanceof ValidationException) {
      final Property property = ((ValidationException) throwable).getProperty();
      JOptionPane.showMessageDialog(this, throwable.getMessage(), Messages.get(Messages.EXCEPTION),
              JOptionPane.ERROR_MESSAGE);
      getEditPanel().selectControl(property);
    }
    else {
      handleException(throwable, this);
    }
  }

  /**
   * Handles the given exception
   * @param throwable the exception to handle
   * @param dialogParent the component to use as exception dialog parent
   */
  public void handleException(final Throwable throwable, final JComponent dialogParent) {
    log.error(this, throwable);
    DefaultExceptionHandler.get().handleException(throwable, dialogParent);
  }

  //#############################################################################################
  // Begin - control methods, see setupControls
  //#############################################################################################

  /**
   * Saves the active entity, that is, if no entity is selected it performs a insert otherwise the user
   * is asked whether to update the selected entity or insert a new one
   */
  public final void save() {
    if ((getModel().containsTableModel() && getModel().getTableModel().getSelectionModel().isSelectionEmpty())
            || !getEditModel().isEntityModified() || !getEditModel().isUpdateAllowed()) {
      //no entity selected, selected entity is unmodified or update is not allowed, can only insert
      insert();
    }
    else {//possibly update
      final int choiceIdx = JOptionPane.showOptionDialog(this, FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT),
              FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT_TITLE), -1, JOptionPane.QUESTION_MESSAGE, null,
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_RECORD),
                      FrameworkMessages.get(FrameworkMessages.INSERT_NEW), Messages.get(Messages.CANCEL)},
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE)});
      if (choiceIdx == 0) //update
        update();
      else if (choiceIdx == 1) //insert
        insert();
    }
  }

  /**
   * Performs a insert on the active entity
   * @return true in case of successful insert, false otherwise
   */
  public final boolean insert() {
    try {
      if (confirmInsert()) {
        validateData();
        try {
          UiUtil.setWaitCursor(true, this);
          getModel().getEditModel().insert();
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }
        prepareUI(true, true);
        return true;
      }
    }
    catch (Exception ex) {
      handleException(ex);
    }

    return false;
  }

  /**
   * Performs a delete on the active entity or if a table model is available, the selected entities
   * @return true if the delete operation was successful
   */
  public final boolean delete() {
    try {
      if (confirmDelete()) {
        try {
          UiUtil.setWaitCursor(true, this);
          if (getModel().containsTableModel())
            getModel().getEditModel().delete(getModel().getTableModel().getSelectedItems());
          else
            getModel().getEditModel().delete();
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }

        return true;
      }
    }
    catch (Exception e) {
      handleException(e);
    }

    return false;
  }

  /**
   * Performs an update on the active entity
   * @return true if the update operation was successful
   */
  public final boolean update() {
    try {
      if (confirmUpdate()) {
        validateData();
        try {
          UiUtil.setWaitCursor(true, this);
          getModel().getEditModel().update();
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }
        prepareUI(true, false);

        return true;
      }
    }
    catch (Exception e) {
      handleException(e);
    }

    return false;
  }

  /**
   * Queries the user on which property to update, after which it calls the
   * <code>updateSelectedEntities(property)</code> with that property
   * @see #updateSelectedEntities(org.jminor.framework.domain.Property)
   * @see #getInputProvider(org.jminor.framework.domain.Property, java.util.List)
   */
  public void updateSelectedEntities() {
    try {
      updateSelectedEntities(getPropertyToUpdate());
    }
    catch (CancelException e) {/**/}
  }

  /**
   * Retrieves a new property value via input dialog and performs an update on the selected entities
   * @param propertyToUpdate the property to update
   * @see #getInputProvider(org.jminor.framework.domain.Property, java.util.List)
   */
  public void updateSelectedEntities(final Property propertyToUpdate) {
    if (!getModel().containsTableModel() || getModel().getTableModel().stateSelectionEmpty().isActive())
      return;

    final List<Entity> selectedEntities = EntityUtil.copyEntities(getModel().getTableModel().getSelectedItems());
    final InputProviderPanel inputPanel = new InputProviderPanel(propertyToUpdate.getCaption(),
            getInputProvider(propertyToUpdate, selectedEntities));
    UiUtil.showInDialog(this, inputPanel, true, FrameworkMessages.get(FrameworkMessages.SET_PROPERTY_VALUE),
            null, inputPanel.getOkButton(), inputPanel.eventButtonClicked());
    if (inputPanel.isEditAccepted()) {
      EntityUtil.setPropertyValue(propertyToUpdate.getPropertyID(), inputPanel.getValue(), selectedEntities);
      try {
        UiUtil.setWaitCursor(true, this);
        getModel().getEditModel().update(selectedEntities);
      }
      catch (RuntimeException re) {
        throw re;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
      finally {
        UiUtil.setWaitCursor(false, this);
      }
    }
  }

  /**
   * Shows a dialog containing lists of entities depending on the selected entities via foreign key
   */
  public void viewSelectionDependencies() {
    try {
      final Map<String, List<Entity>> dependencies;
      try {
        UiUtil.setWaitCursor(true, this);
        dependencies = getModel().getTableModel().getSelectionDependencies();
      }
      finally {
        UiUtil.setWaitCursor(false, this);
      }
      if (dependencies.size() > 0) {
        showDependenciesDialog(dependencies, getModel().getDbProvider(), this);
      }
      else {
        JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.NONE_FOUND),
                FrameworkMessages.get(FrameworkMessages.NO_DEPENDENT_RECORDS), JOptionPane.INFORMATION_MESSAGE);
      }
    }
    catch (Exception e) {
      handleException(e);
    }
  }

  /**
   * Exports the selected records as a JSON file
   * @throws CancelException in case the action is cancelled
   * @throws JSONException in case of a JSON exception
   */
  public void exportSelected() throws CancelException, JSONException {
    final List<Entity> selected = getModel().getTableModel().getSelectedItems();
    Util.writeFile(EntityUtil.getJSONString(selected, 2), UiUtil.chooseFileToSave(this, null, null));
    JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED_DONE));
  }

  /**
   * Prints the table if one is available
   */
  public void printTable() {
    if (entityTablePanel != null)
      entityTablePanel.printTable();
  }

  /**
   * Shows the query configuration dialog if a table panel is available
   */
  public void configureQuery() {
    if (getTablePanel() != null)
      getTablePanel().configureQuery();
  }

  /**
   * Shows a dialog for selecting which columns to show/hide if a table panel is available
   */
  public void selectTableColumns() {
    if (getTablePanel() != null)
      getTablePanel().selectTableColumns();
  }

  //#############################################################################################
  // End - control methods
  //#############################################################################################

  /**
   * Prepares the UI, by clearing the input fields and setting the default focus,
   * if both parameters are set to false then there is no effect
   * @param requestDefaultFocus if true the component defined as the defaultFocusComponent
   * gets the input focus, if none is defined the first child component of this EntityPanel is used,
   * if no edit panel is available the table receives the focus
   * @param clearUI if true the the input components are cleared
   * @see EntityEditPanel#setDefaultFocusComponent(javax.swing.JComponent)
   */
  public final void prepareUI(final boolean requestDefaultFocus, final boolean clearUI) {
    final EntityEditPanel editPanel = getEditPanel();
    if (editPanel != null) {
      editPanel.prepareUI(requestDefaultFocus, clearUI);
    }
    else if (requestDefaultFocus) {
      if (getTablePanel() != null)
        getTablePanel().getJTable().requestFocus();
      else if (getComponentCount() > 0)
        getComponents()[0].requestFocus();
    }
  }

  /**
   * @return a control for showing the column selection dialog
   */
  public Control getSelectColumnsControl() {
    return ControlFactory.methodControl(this, "selectTableColumns",
            FrameworkMessages.get(FrameworkMessages.SELECT_COLUMNS) + "...", null,
            FrameworkMessages.get(FrameworkMessages.SELECT_COLUMNS));
  }

  /**
   * @return a control for showing the query configuration dialog
   */
  public Control getConfigureQueryControl() {
    return ControlFactory.methodControl(this, "configureQuery",
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY) + "...", null,
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY));
  }

  /**
   * @return a control for showing the dependencies dialog
   */
  public Control getViewDependenciesControl() {
    return ControlFactory.methodControl(this, "viewSelectionDependencies",
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES) + "...",
            getModel().getTableModel().stateSelectionEmpty().getReversedState(),
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES_TIP), 'W');
  }

  /**
   * @return a control for printing the table
   */
  public Control getPrintControl() {
    final String printCaption = FrameworkMessages.get(FrameworkMessages.PRINT_TABLE);
    return ControlFactory.methodControl(this, "printTable", printCaption, null,
            printCaption, printCaption.charAt(0));
  }

  /**
   * @return a control for exporting the selected records to file
   */
  public Control getExportControl() {
    return ControlFactory.methodControl(this, "exportSelected",
            FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED) + "...",
            getModel().getTableModel().stateSelectionEmpty().getReversedState(),
            FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED_TIP), 0, null,
            Images.loadImage(Images.IMG_SAVE_16));
  }

  /**
   * @return a control for deleting the selected entities
   */
  public Control getDeleteSelectedControl() {
    return ControlFactory.methodControl(this, "delete", FrameworkMessages.get(FrameworkMessages.DELETE),
            new AggregateState(AggregateState.Type.AND,
                    getModel().getEditModel().stateAllowDelete(),
                    getModel().getTableModel().stateSelectionEmpty().getReversedState()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP), 0, null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for refreshing the model data
   */
  public Control getRefreshControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.REFRESH_MNEMONIC);
    return ControlFactory.methodControl(getModel(), "refresh", FrameworkMessages.get(FrameworkMessages.REFRESH),
            stActive, FrameworkMessages.get(FrameworkMessages.REFRESH_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_REFRESH_16));
  }

  /**
   * @return a control for updating a property in the selected entities
   */
  public Control getUpdateSelectedControl() {
    return ControlFactory.methodControl(this, "updateSelectedEntities",
            FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED),
            new AggregateState(AggregateState.Type.AND,
                    getModel().getEditModel().stateAllowUpdate(),
                    getModel().getTableModel().stateSelectionEmpty().getReversedState()),
            FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP), 0,
            null, Images.loadImage(Images.IMG_SAVE_16));
  }

  /**
   * @return a control set containing a set of controls, one for each updatable property in the
   * underlying entity, for performing an update on the selected entities
   */
  public ControlSet getUpdateSelectedControlSet() {
    final State enabled = new AggregateState(AggregateState.Type.AND,
            getModel().getEditModel().stateAllowUpdate(),
            getModel().getTableModel().stateSelectionEmpty().getReversedState());
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED),
            (char) 0, Images.loadImage("Modify16.gif"), enabled);
    controlSet.setDescription(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP));
    for (final Property property : getUpdateProperties()) {
      final String caption = property.getCaption() == null ? property.getPropertyID() : property.getCaption();
      controlSet.add(UiUtil.linkToEnabledState(enabled, new AbstractAction(caption) {
        public void actionPerformed(final ActionEvent event) {
          updateSelectedEntities(property);
        }
      }));
    }

    return controlSet;
  }

  /**
   * @return a control for deleting the active entity (or the selected entities if a table model is available)
   */
  public Control getDeleteControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.DELETE_MNEMONIC);
    return ControlFactory.methodControl(this, "delete", FrameworkMessages.get(FrameworkMessages.DELETE),
            new AggregateState(AggregateState.Type.AND,
                    stActive,
                    getModel().getEditModel().stateAllowDelete(),
                    getEditModel().getEntityNullState().getReversedState()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP) + " (ALT-" + mnemonic + ")", mnemonic.charAt(0), null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for clearing the UI controls
   */
  public Control getClearControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.CLEAR_MNEMONIC);
    return ControlFactory.methodControl(getModel().getEditModel(), "clear", FrameworkMessages.get(FrameworkMessages.CLEAR),
            stActive, FrameworkMessages.get(FrameworkMessages.CLEAR_ALL_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_NEW_16));
  }

  /**
   * @return a control for performing an update on the active entity
   */
  public Control getUpdateControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.UPDATE_MNEMONIC);
    return ControlFactory.methodControl(this, "update", FrameworkMessages.get(FrameworkMessages.UPDATE),
            new AggregateState(AggregateState.Type.AND,
                    stActive,
                    getModel().getEditModel().stateAllowUpdate(),
                    getEditModel().getEntityNullState().getReversedState(),
                    getEditModel().stateModified()),
            FrameworkMessages.get(FrameworkMessages.UPDATE_TIP) + " (ALT-" + mnemonic + ")", mnemonic.charAt(0),
            null, Images.loadImage(Images.IMG_SAVE_16));
  }

  /**
   * @return a control for performing an insert on the active entity
   */
  public Control getInsertControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.INSERT_MNEMONIC);
    return ControlFactory.methodControl(this, "save", FrameworkMessages.get(FrameworkMessages.INSERT),
            new AggregateState(AggregateState.Type.AND, stActive, getModel().getEditModel().stateAllowInsert()),
            FrameworkMessages.get(FrameworkMessages.INSERT_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage("Add16.gif"));
  }

  /**
   * @return a control for performing a save on the active entity
   */
  public Control getSaveControl() {
    final String insertCaption = FrameworkMessages.get(FrameworkMessages.INSERT_UPDATE);
    final State stInsertUpdate = new AggregateState(AggregateState.Type.OR, getModel().getEditModel().stateAllowInsert(),
            new AggregateState(AggregateState.Type.AND, getModel().getEditModel().stateAllowUpdate(),
                    getEditModel().stateModified()));
    return ControlFactory.methodControl(this, "save", insertCaption,
            new AggregateState(AggregateState.Type.AND, stActive, stInsertUpdate),
            FrameworkMessages.get(FrameworkMessages.INSERT_UPDATE_TIP),
            insertCaption.charAt(0), null, Images.loadImage(Images.IMG_PROPERTIES_16));
  }

  /**
   * Associates <code>control</code> with <code>controlCode</code>
   * @param controlCode the control code
   * @param control the control to associate with <code>controlCode</code>
   */
  public final void setControl(final String controlCode, final Control control) {
    if (control == null)
      controlMap.remove(controlCode);
    else
      controlMap.put(controlCode, control);
  }

  /**
   * @param controlCode the control code
   * @return the control associated with <code>controlCode</code>
   * @throws RuntimeException in case no control is associated with the given control code
   */
  public final Control getControl(final String controlCode) {
    if (!controlMap.containsKey(controlCode))
      throw new RuntimeException(controlCode + " control not available in panel: " + this);

    return controlMap.get(controlCode);
  }

  /**
   * Creates a static entity panel showing the given entities
   * @param entities the entities to show in the panel
   * @param dbProvider the EntityDbProvider, in case the returned panel should require one
   * @return a static EntityPanel showing the given entities
   */
  public static EntityPanel createStaticEntityPanel(final Collection<Entity> entities, final EntityDbProvider dbProvider) {
    if (entities == null || entities.size() == 0)
      throw new RuntimeException("Cannot create an EntityPanel without the entities");

    return createStaticEntityPanel(entities, dbProvider, entities.iterator().next().getEntityID());
  }

  /**
   * Creates a static entity panel showing the given entities
   * @param entities the entities to show in the panel
   * @param dbProvider the EntityDbProvider, in case the returned panel should require one
   * @param entityID the entityID
   * @return a static EntityPanel showing the given entities
   */
  public static EntityPanel createStaticEntityPanel(final Collection<Entity> entities, final EntityDbProvider dbProvider,
                                                    final String entityID) {
    return new EntityPanel(new EntityModel(entityID, dbProvider) {
      @Override
      protected EntityTableModel initializeTableModel() {
        return new EntityTableModel(entityID, dbProvider, false) {
          @Override
          protected List<Entity> performQuery(final Criteria criteria) {
            return new ArrayList<Entity>(entities);
          }
        };
      }
    }, entityID, true, false, false, EMBEDDED) {
      @Override
      protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
        return null;
      }
    }.initializePanel();
  }

  public static EntityPanel createInstance(final EntityPanelProvider panelProvider, final EntityModel model) {
    if (model == null)
      throw new RuntimeException("Can not create a EntityPanel without an EntityModel");
    try {
      return (EntityPanel) panelProvider.getEntityPanelClass().getConstructor(EntityModel.class).newInstance(model);
    }
    catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof RuntimeException)
        throw (RuntimeException) ite.getCause();

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
      return createInstance(panelProvider, (EntityModel) panelProvider.getEntityModelClass().getConstructor(
              EntityDbProvider.class).newInstance(dbProvider));
    }
    catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof RuntimeException)
        throw (RuntimeException) ite.getCause();

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
    if (tp != null)
      tp.setSelectedComponent(this);
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
    if (entityTablePanel != null) {
      entityTablePanel.initializeSouthPanelToolBar(getTablePanelControlSet(entityTablePanel));
      entityTablePanel.setTableDoubleClickAction(initializeTableDoubleClickAction());
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
    if (Configuration.getBooleanValue(Configuration.USE_FOCUS_ACTIVATION))
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner",
              new WeakPropertyChangeListener(focusPropertyListener));
  }

  /**
   * Initializes the keyboard navigation actions.
   * By default ALT-CTRL-T transfers focus to the table in case one is available,
   * ALT-CTR-E transfers focus to the edit panel in case one is available
   * and ALT-CTR-S transfers focus to the search panel.
   */
  protected void setupKeyboardActions() {
    if (getTablePanel() != null) {
      getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_T,
              KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, true), "selectTablePanel");
      getActionMap().put("selectTablePanel", new AbstractAction() {
        public void actionPerformed(ActionEvent event) {
          getTablePanel().getJTable().requestFocusInWindow();
        }
      });
    }
    if (getEditControlPanel() != null) {
      getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_E,
              KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, true), "selectEditPanel");
      getActionMap().put("selectEditPanel", new AbstractAction() {
        public void actionPerformed(ActionEvent event) {
          if (getEditPanelState() == HIDDEN)
            setEditPanelState(EMBEDDED);
          getEditPanel().prepareUI(true, false);
        }
      });
    }
    if (getTablePanel().getSearchPanel() != null) {
      getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_S,
              KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, true), "selectSearchPanel");
      getActionMap().put("selectSearchPanel", new AbstractAction() {
        public void actionPerformed(ActionEvent event) {
          getTablePanel().setSearchPanelVisible(true);
          getTablePanel().getSearchPanel().requestFocusInWindow();
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
    editPanel = initializeEditPanel(getEditModel());
    if (editPanel == null)
      return null;

    final JPanel panel = new JPanel(new BorderLayout(5,5));
    panel.setMinimumSize(new Dimension(0,0));
    panel.setBorder(BorderFactory.createEtchedBorder());
    final JPanel propertyBase =
            new JPanel(new FlowLayout(buttonPlacement.equals(BorderLayout.SOUTH) ? FlowLayout.CENTER : FlowLayout.LEADING,5,5));
    panel.addMouseListener(new ActivationFocusAdapter(propertyBase));
    propertyBase.add(editPanel);
    panel.add(propertyBase, BorderLayout.CENTER);
    final JComponent controlPanel = Configuration.getBooleanValue(Configuration.TOOLBAR_BUTTONS) ?
            initializeControlToolBar() : initializeControlPanel();
    if (controlPanel != null)
      panel.add(controlPanel, Configuration.getBooleanValue(Configuration.TOOLBAR_BUTTONS) ?
              (buttonPlacement.equals(BorderLayout.SOUTH) ? BorderLayout.NORTH : BorderLayout.WEST) :
              (buttonPlacement.equals(BorderLayout.SOUTH) ? BorderLayout.SOUTH : BorderLayout.EAST));

    return panel;
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
   * Initializes the control panel, that is, the panel containing buttons for editing entities (Insert, Update...)
   * @return the control panel
   */
  protected JPanel initializeControlPanel() {
    JPanel panel;
    if (buttonPlacement.equals(BorderLayout.SOUTH)) {
      panel = new JPanel(new FlowLayout(FlowLayout.CENTER,5,5));
      panel.add(ControlProvider.createHorizontalButtonPanel(getControlPanelControlSet()));
    }
    else {
      panel = new JPanel(new BorderLayout(5,5));
      panel.add(ControlProvider.createVerticalButtonPanel(getControlPanelControlSet()), BorderLayout.NORTH);
    }

    return panel;
  }

  /**
   * Initializes the control toolbar, that is, the toolbar containing buttons for editing entities (Insert, Update...)
   * @return the control toolbar
   */
  protected JToolBar initializeControlToolBar() {
    return ControlProvider.createToolbar(getControlPanelControlSet(), JToolBar.VERTICAL);
  }

  /**
   * Initializes the JTabbedPane containing the detail panels, used in case of multiple detail panels
   * @return the JTabbedPane for holding detail panels
   */
  protected JTabbedPane initializeDetailTabPane() {
    final JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setFocusable(false);
    tabbedPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final EntityPanel detailPanel : detailEntityPanels)
      tabbedPane.addTab(detailPanel.getCaption(), detailPanel);

    tabbedPane.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent event) {
        getModel().setLinkedDetailModel(getDetailPanelState() != HIDDEN ? getSelectedDetailPanel().getModel() : null);
        getSelectedDetailPanel().initializePanel();
      }
    });
    tabbedPane.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(MouseEvent event) {
        if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1)
          setDetailPanelState(getDetailPanelState() == DIALOG ? EMBEDDED : DIALOG);
        else if (event.getButton() == MouseEvent.BUTTON2)
          setDetailPanelState(getDetailPanelState() == EMBEDDED ? HIDDEN : EMBEDDED);
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
   * @see #setupControls()
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
      final EntityModel detailModel = getModel().getDetailModel(detailPanelProvider.getEntityModelClass());
      if (detailModel == null)
        throw new RuntimeException("Detail model of type " + detailPanelProvider.getEntityModelClass()
                + " not found in model of type " + getModel().getClass());
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
   * Initializes the EntityEditPanel, that is, the panel containing the UI controls for editing the active entity,
   * this method should return null if editing is not required
   * @param editModel the EntityEditModel
   * @return the EntityEditPanel panel
   */
  protected abstract EntityEditPanel initializeEditPanel(final EntityEditModel editModel);

  /**
   * Initializes the EntityTablePanel instance using the EntityTableModel instance
   * provided by the getTableModel() method in the underlying EntityModel
   * @param tableModel the EntityTableModel
   * @param popupMenuControlSet the ControlSet to use when creating the popup menu for the EntityTablePanel
   * @param rowColoring true if the a table row should be colored according to the underlying entity
   * @return the EntityTablePanel
   */
  protected EntityTablePanel initializeTablePanel(final EntityTableModel tableModel, final ControlSet popupMenuControlSet,
                                                  final boolean rowColoring) {
    return new EntityTablePanel(tableModel, popupMenuControlSet, rowColoring);
  }

  /**
   * @param tablePanel the EntityTablePanel
   * @return a ControlSet containing the controls to include on the table panel, usually in a toolbar
   */
  protected ControlSet getTablePanelControlSet(final EntityTablePanel tablePanel) {
    final ControlSet controls = new ControlSet();

    Control control = tablePanel.getToggleSummaryPanelControl();
    if (control != null)
      controls.add(control);
    control = tablePanel.getToggleSearchPanelControl();
    if (control != null)
      controls.add(control);
    control = tablePanel.getPrintControl();
    if (control != null) {
      control.setName("");
      controls.add(control);
    }
    controls.addSeparator();
    if (!getModel().getEditModel().isReadOnly() && getModel().getEditModel().isDeleteAllowed()) {
      control = getDeleteSelectedControl();
      if (control != null) {
        control.setName(null);
        controls.add(control);
      }
    }
    control = tablePanel.getClearSelectionControl();
    if (control != null)
      controls.add(control);
    controls.addSeparator();
    control = tablePanel.getMoveSelectionDownControl();
    if (control != null)
      controls.add(control);
    control = tablePanel.getMoveSelectionUpControl();
    if (control != null)
      controls.add(control);
    controls.addSeparator();
    control = getToggleEditPanelControl();
    if (control != null)
      controls.add(control);
    control = getToggleDetailPanelControl();
    if (control != null)
      controls.add(control);

    return controls;
  }

  /**
   * Initializes the controls available to this EntityPanel by mapping them to their respective
   * control codes (EntityPanel.INSERT, UPDATE etc) via the <code>setControl(String, Control) method,
   * these can then be retrieved via the <code>getControl(String)</code> method.
   * @see org.jminor.common.ui.control.Control
   * @see #setControl(String, org.jminor.common.ui.control.Control)
   * @see #getControl(String)
   */
  protected void setupControls() {
    if (!getModel().getEditModel().isReadOnly()) {
      if (getModel().getEditModel().isInsertAllowed())
        setControl(INSERT, getInsertControl());
      if (getModel().getEditModel().isUpdateAllowed())
        setControl(UPDATE, getUpdateControl());
      if (getModel().getEditModel().isDeleteAllowed())
        setControl(DELETE, getDeleteControl());
    }
    setControl(CLEAR, getClearControl());
    if (getModel().containsTableModel()) {
      if (!getModel().getEditModel().isReadOnly() && getModel().getEditModel().isUpdateAllowed()
              && getModel().getEditModel().isMultipleUpdateAllowed())
        setControl(UPDATE_SELECTED, getUpdateSelectedControlSet());
      setControl(REFRESH, getRefreshControl());
      if (!getModel().getEditModel().isReadOnly() && getModel().getEditModel().isDeleteAllowed())
        setControl(MENU_DELETE, getDeleteSelectedControl());
      setControl(PRINT, getPrintControl());
      setControl(EXPORT_JSON, getExportControl());
      setControl(VIEW_DEPENDENCIES, getViewDependenciesControl());
      if (getModel().getTableModel().isQueryConfigurationAllowed())
        setControl(CONFIGURE_QUERY, getConfigureQueryControl());
      setControl(SELECT_COLUMNS, getSelectColumnsControl());
    }
  }

  /**
   * @return the ControlSet on which the table popup menu is based
   */
  protected ControlSet getTablePopupControlSet() {
    boolean separatorRequired = false;
    final ControlSet controlSet = new ControlSet("");
    if (detailEntityPanels.size() > 0) {
      controlSet.add(getDetailPanelControls(EMBEDDED));
      separatorRequired = true;
    }
    if (separatorRequired) {
      controlSet.addSeparator();
      separatorRequired = false;
    }
    if (controlMap.containsKey(UPDATE_SELECTED)) {
      controlSet.add(controlMap.get(UPDATE_SELECTED));
      separatorRequired = true;
    }
    if (controlMap.containsKey(MENU_DELETE)) {
      controlSet.add(controlMap.get(MENU_DELETE));
      separatorRequired = true;
    }
    if (controlMap.containsKey(EXPORT_JSON)) {
      controlSet.add(controlMap.get(EXPORT_JSON));
      separatorRequired = true;
    }
    if (separatorRequired) {
      controlSet.addSeparator();
      separatorRequired = false;
    }
    if (controlMap.containsKey(VIEW_DEPENDENCIES)) {
      controlSet.add(controlMap.get(VIEW_DEPENDENCIES));
      separatorRequired = true;
    }
    if (separatorRequired) {
      controlSet.addSeparator();
      separatorRequired = false;
    }
    final ControlSet printControls = getPrintControls();
    if (printControls != null) {
      controlSet.add(getPrintControls());
      separatorRequired = true;
    }
    if (controlMap.containsKey(CONFIGURE_QUERY)) {
      if (separatorRequired) {
        controlSet.addSeparator();
        separatorRequired = false;
      }
      controlSet.add(controlMap.get(CONFIGURE_QUERY));
    }
    if (controlMap.containsKey(SELECT_COLUMNS)) {
      if (separatorRequired)
        controlSet.addSeparator();
      controlSet.add(controlMap.get(SELECT_COLUMNS));
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
          if (editControlPanel != null && getEditPanelState() == HIDDEN)
            setEditPanelState(DIALOG);
          else if (getDetailPanelState() == HIDDEN)
            setDetailPanelState(DIALOG);
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
    if (detailEntityPanels.size() == 0)
      return null;

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
   * Initializes the print control set, override to provide specific printing functionality, i.e. report printing
   * @return the print control set
   */
  protected ControlSet getPrintControls() {
    return controlMap.containsKey(PRINT) ? new ControlSet(Messages.get(Messages.PRINT), (char) 0, null,
            Images.loadImage("Print16.gif"), controlMap.get(PRINT)) : null;
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
    if (entityTablePanel == null)
      return;

    if (!getModel().getEditModel().isReadOnly() && getModel().getEditModel().isDeleteAllowed()) {
      entityTablePanel.getJTable().addKeyListener(new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent event) {
          if (event.getKeyChar() == KeyEvent.VK_DELETE && !getModel().getTableModel().stateSelectionEmpty().isActive())
            delete();
        }
      });
    }
    getModel().eventEntitiesChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        entityTablePanel.getJTable().repaint();
      }
    });
  }

  /**
   * @return the ControlSet on which the control panel is based
   */
  protected ControlSet getControlPanelControlSet() {
    final ControlSet controlSet = new ControlSet("Actions");
    if (controlMap.containsKey(INSERT))
      controlSet.add(controlMap.get(INSERT));
    if (controlMap.containsKey(UPDATE))
      controlSet.add(controlMap.get(UPDATE));
    if (controlMap.containsKey(DELETE))
      controlSet.add(controlMap.get(DELETE));
    if (controlMap.containsKey(CLEAR))
      controlSet.add(controlMap.get(CLEAR));
    if (controlMap.containsKey(REFRESH))
      controlSet.add(controlMap.get(REFRESH));

    return controlSet;
  }

  //#############################################################################################
  // End - initialization methods
  //#############################################################################################

  /**
   * for overriding, called before insert/update
   * @throws ValidationException in case of a validation failure
   * @throws CancelException in case the user cancels the action during validation
   */
  protected void validateData() throws ValidationException, CancelException {}

  /**
   * Provides value input components for multiple entity update, override to supply
   * specific InputValueProvider implementations for properties.
   * Remember to return with a call to super.getInputProviderInputProvider().
   * @param property the property for which to get the InputProvider
   * @param toUpdate the entities that are about to be updated
   * @return the InputProvider handling input for <code>property</code>
   * @see #updateSelectedEntities
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected InputProvider getInputProvider(final Property property, final List<Entity> toUpdate) {
    final Collection<Object> values = EntityUtil.getDistinctPropertyValues(toUpdate, property.getPropertyID());
    final Object currentValue = values.size() == 1 ? values.iterator().next() : null;
    switch (property.getPropertyType()) {
      case TIMESTAMP:
        return new DateInputProvider((Date) currentValue, Configuration.getDefaultTimestampFormat());
      case DATE:
        return new DateInputProvider((Date) currentValue, Configuration.getDefaultDateFormat());
      case DOUBLE:
        return new DoubleInputProvider((Double) currentValue);
      case INT:
        return new IntInputProvider((Integer) currentValue);
      case BOOLEAN:
        return new BooleanInputProvider((Boolean) currentValue);
      case STRING:
        return new TextInputProvider(property.getCaption(), new PropertyValueListProvider(getModel().getDbProvider(),
                getModel().getEntityID(), property.getPropertyID()), (String) currentValue);
      case ENTITY:
        return createEntityInputProvider((Property.ForeignKeyProperty) property, (Entity) currentValue);
    }

    throw new IllegalArgumentException("Unsupported property type: " + property.getPropertyType());
  }

  /**
   * Creates a InputProvider for the given foreign key property
   * @param foreignKeyProperty the property
   * @param currentValue the current value to initialize the InputProvider with
   * @return a Entity InputProvider
   */
  protected InputProvider createEntityInputProvider(final Property.ForeignKeyProperty foreignKeyProperty, final Entity currentValue) {
    if (!EntityRepository.isLargeDataset(foreignKeyProperty.getReferencedEntityID())) {
      return new EntityComboProvider(getEditModel().createEntityComboBoxModel(foreignKeyProperty), currentValue);
    }
    else {
      List<Property> searchProperties = EntityRepository.getSearchProperties(foreignKeyProperty.getReferencedEntityID());
      if (searchProperties.size() == 0)
        throw new RuntimeException("No searchable properties found for entity: " + foreignKeyProperty.getReferencedEntityID());

      return new EntityLookupProvider(getEditModel().createEntityLookupModel(foreignKeyProperty.getReferencedEntityID(), null, searchProperties), currentValue);
    }
  }

  /**
   * Called before a insert is performed, the default implementation simply returns true
   * @return true if a insert should be performed, false if it should be vetoed
   */
  protected boolean confirmInsert() {
    return true;
  }

  /**
   * Called before a delete is performed, if true is returned the delete action is performed otherwise it is canceled
   * @return true if the delete action should be performed
   */
  protected boolean confirmDelete() {
    final String[] messages = getConfirmationMessages(CONFIRM_TYPE_DELETE);
    final int res = JOptionPane.showConfirmDialog(this, messages[0], messages[1], JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  /**
   * Called before an update is performed, if true is returned the update action is performed otherwise it is cancelled
   * @return true if the update action should be performed
   */
  protected boolean confirmUpdate() {
    final String[] messages = getConfirmationMessages(CONFIRM_TYPE_UPDATE);
    final int res = JOptionPane.showConfirmDialog(this, messages[0], messages[1], JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  /**
   * @param type the confirmation message type, one of the following:
   * EntityPanel.CONFIRM_TYPE_INSERT, EntityPanel.CONFIRM_TYPE_DELETE or EntityPanel.CONFIRM_TYPE_UPDATE
   * @return a string array containing two elements, the element at index 0 is used
   * as the message displayed in the dialog and the element at index 1 is used as the dialog title,
   * i.e. ["Are you sure you want to delete the selected records?", "About to delete selected records"]
   */
  protected String[] getConfirmationMessages(final int type) {
    switch (type) {
      case CONFIRM_TYPE_DELETE:
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_SELECTED),
                FrameworkMessages.get(FrameworkMessages.DELETE)};
      case CONFIRM_TYPE_INSERT:
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_INSERT),
                FrameworkMessages.get(FrameworkMessages.INSERT)};
      case CONFIRM_TYPE_UPDATE:
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_UPDATE),
                FrameworkMessages.get(FrameworkMessages.UPDATE)};
    }

    throw new IllegalArgumentException("Unknown confirmation type constant: " + type);
  }

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
   * @param reportPath the path to the report object
   * @param reportParameters a map containing the parameters required for the report
   * @param frameTitle the title to display on the frame
   */
  protected void viewJdbcReport(final String reportPath, final Map<String, Object> reportParameters,
                                final String frameTitle) {
    try {
      UiUtil.setWaitCursor(true, this);
      EntityReportUiUtil.viewReport(getModel().fillJdbcReport(reportPath, reportParameters), frameTitle);
    }
    catch (JRException e) {
      throw new RuntimeException(e);
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
  }

  /**
   * Shows a JRViewer for printing a report using the datasource returned
   * by <code>getModel().getTableModel().getJRDataSource()</code> method
   * @param reportPath the path to the report object
   * @param reportParameters a map containing the parameters required for the report
   * @param frameTitle the title to display on the frame
   * @see EntityTableModel#getJRDataSource()
   */
  protected void viewReport(final String reportPath, final Map<String, Object> reportParameters,
                            final String frameTitle) {
    viewReport(reportPath, reportParameters, getModel().getTableModel().getJRDataSource(), frameTitle);
  }

  /**
   * Shows a JRViewer for report printing
   * @param reportPath the path to the report object
   * @param reportParameters a map containing the parameters required for the report
   * @param dataSource the JRDataSource used to provide the report data
   * @param frameTitle the title to display on the frame
   */
  protected void viewReport(final String reportPath, final Map<String, Object> reportParameters,
                            final JRDataSource dataSource, final String frameTitle) {
    try {
      UiUtil.setWaitCursor(true, this);
      EntityReportUiUtil.viewReport(getModel().fillReport(reportPath, reportParameters, dataSource), frameTitle);
    }
    catch (JRException e) {
      throw new RuntimeException(e);
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
  }

  private Control getToggleEditPanelControl() {
    if (editControlPanel == null)
      return null;

    final Control toggle = ControlFactory.methodControl(this, "toggleEditPanelState",
            Images.loadImage("Form16.gif"));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_EDIT_TIP));

    return toggle;
  }

  private Control getToggleDetailPanelControl() {
    if (detailEntityPanels.size() == 0)
      return null;

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

  private Property getPropertyToUpdate() throws CancelException {
    final JComboBox box = new JComboBox(new Vector<Property>(getUpdateProperties()));
    final int ret = JOptionPane.showOptionDialog(this, box,
            FrameworkMessages.get(FrameworkMessages.SELECT_PROPERTY_FOR_UPDATE),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);

    if (ret == JOptionPane.OK_OPTION)
      return (Property) box.getSelectedItem();
    else
      throw new CancelException();
  }

  private List<Property> getUpdateProperties() {
    final List<Property> properties = EntityRepository.getDatabaseProperties(getModel().getEntityID(), true, false, false);
    final ListIterator<Property> iterator = properties.listIterator();
    while(iterator.hasNext()) {
      final Property property = iterator.next();
      if (property.hasParentProperty() || property.isDenormalized()||
              (property instanceof Property.PrimaryKeyProperty && EntityRepository.getIdSource(getModel().getEntityID()).isAutoGenerated()))
        iterator.remove();
    }
    Collections.sort(properties, new Comparator<Property>() {
      public int compare(final Property propertyOne, final Property propertyTwo) {
        return propertyOne.toString().toLowerCase().compareTo(propertyTwo.toString().toLowerCase());
      }
    });

    return properties;
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
    if (parent == this)
      return true;

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

  private static void showDependenciesDialog(final Map<String, List<Entity>> dependencies, final EntityDbProvider dbProvider,
                                             final JComponent dialogParent) {
    JPanel dependenciesPanel;
    try {
      UiUtil.setWaitCursor(true, dialogParent);
      dependenciesPanel = createDependenciesPanel(dependencies, dbProvider);
    }
    finally {
      UiUtil.setWaitCursor(false, dialogParent);
    }
    UiUtil.showInDialog(UiUtil.getParentWindow(dialogParent), dependenciesPanel,
            true, FrameworkMessages.get(FrameworkMessages.DEPENDENT_RECORDS_FOUND), true, true, null);
  }

  private static JPanel createDependenciesPanel(final Map<String, List<Entity>> dependencies,
                                                final EntityDbProvider dbProvider) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    tabPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final Map.Entry<String, List<Entity>> entry : dependencies.entrySet()) {
      final List<Entity> dependantEntities = entry.getValue();
      if (dependantEntities.size() > 0)
        tabPane.addTab(entry.getKey(), createStaticEntityPanel(dependantEntities, dbProvider));
    }
    panel.add(tabPane, BorderLayout.CENTER);

    return panel;
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