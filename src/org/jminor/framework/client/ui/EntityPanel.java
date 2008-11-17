/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.DbException;
import org.jminor.common.db.ICriteria;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.AggregateState;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.ui.BorderlessTabbedPaneUI;
import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.IExceptionHandler;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.ToggleBeanPropertyLink;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.printing.JPrinter;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

/**
 * A panel representing a Entity via a EntityModel, which facilitates browsing and editing of records
 *
 * The default layout is as follows:
 * __________________________________
 * |     property           |action |
 * |      panel             | panel | } edit panel
 * |________________________|_______|
 * |                  |             |
 * |   table panel    |   detail    |
 * |(EntityTablePanel)|   panel     |
 * |                  |             |
 * |__________________|_____________|
 */
public abstract class EntityPanel extends EntityBindingFactory implements IExceptionHandler {

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

  public static final int DIVIDER_JUMP = 30;

  public static final int UP = 0;
  public static final int DOWN = 1;
  public static final int RIGHT = 2;
  public static final int LEFT = 3;

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

  private static final String NAV_UP = "navigateUp";
  private static final String NAV_DOWN = "navigateDown";
  private static final String NAV_RIGHT = "navigateRight";
  private static final String NAV_LEFT = "navigateLeft";
  private static final String DIV_LEFT = "divLeft";
  private static final String DIV_RIGHT = "divRight";
  private static final String DIV_UP = "divUp";
  private static final String DIV_DOWN = "divDown";

  private final HashMap<String, Control> controlMap = new HashMap<String, Control>();

  /**
   * true if this EntityPanel allows its underlying query to be configured
   */
  private final boolean queryConfigurationAllowed;

  /**
   * true if this panel should be compact
   */
  private final boolean compactPanel;

  /**
   * true if the rows in the table (if any) should be colored according to the underlying entity
   */
  private final boolean specialRendering;

  /**
   * true if the data should be refreshed during initialization
   */
  private final boolean refreshOnInit;

  /**
   * indicates where the edit panel buttons should be placed, either BorderLayout.SOUTH or BorderLayout.EAST
   */
  private final String buttonPlacement;

  /**
   * The EntityModel instance used by this EntityPanel
   */
  private EntityModel model;

  /**
   * The EntityTablePanel instance used by this EntityPanel
   */
  private EntityTablePanel entityTablePanel;

  /**
   * The edit panel which contains the controls required for editing a entity
   */
  private JPanel editPanel;

  /**
   * The horizontal split pane, which is used in case this entity panel has detail panels.
   * It splits the lower section of this EntityPanel into the EntityTablePanel on the left
   * and the detail panels on the right
   */
  private JSplitPane horizontalSplitPane;

  /**
   * A map containing the detail panels, if any.
   * Initialized during <code>initialize()</code>
   */
  private Map<EntityPanelInfo, EntityPanel> detailEntityPanels;

  /**
   * A tab pane for the detail panels, if any
   */
  private JTabbedPane detailTabPane;

  /**
   * A base panel used in case this EntityPanel is configured to be compact
   */
  private JPanel compactBase;

  /**
   * The dialog used when detail panels are undocked
   */
  private JDialog detailDialog;

  /**
   * The dialog used when the edit panel is undocked
   */
  private JDialog editDialog;

  /**
   * The component that should receive focus when the UI is prepared for a new record
   */
  private JComponent defaultFocusComponent;

  /**
   * Holds the current state of the edit panel (HIDDEN, EMBEDDED or DIALOG)
   */
  private int editPanelState = HIDDEN;

  /**
   * Holds the current state of the detail panels (HIDDEN, EMBEDDED or DIALOG)
   */
  private int detailPanelState = HIDDEN;

  /**
   * True after <code>initialize()</code> has been called
   */
  private boolean initialized = false;

  /**
   * Initializes a new EntityPanel instance.
   */
  public EntityPanel() {
    this(true);
  }

  /**
   * Initializes a new EntityPanel instance.
   * @param refreshOnInit if true then the underlying data model is refreshed during initialization
   */
  public EntityPanel(final boolean refreshOnInit) {
    this(refreshOnInit, true);
  }

  /**
   * Initializes a new EntityPanel instance.
   * @param refreshOnInit if true then the underlying data model is refreshed during initialization
   * @param specialRendering if true then each row in the table model (if any)
   * is colored according to the underlying entity
   */
  public EntityPanel(final boolean refreshOnInit, final boolean specialRendering) {
    this(refreshOnInit, specialRendering, false);
  }

  /**
   * Initializes a new EntityPanel instance.
   * @param refreshOnInit if true then the underlying data model is refreshed during initialization
   * @param specialRendering if true then each row in the table model (if any)
   * is colored according to the underlying entity
   * @param horizontalButtons if true the action panel buttons are laid out horizontally below the property panel,
   * otherwise vertically on its right side
   */
  public EntityPanel(final boolean refreshOnInit, final boolean specialRendering, final boolean horizontalButtons) {
    this(refreshOnInit, specialRendering, horizontalButtons, EMBEDDED);//embedded perhaps not default?
  }

  /**
   * Initializes a new EntityPanel instance.
   * @param refreshOnInit if true then the underlying data model is refreshed during initialization
   * @param specialRendering if true then each row in the table model (if any)
   * is colored according to the underlying entity
   * @param horizontalButtons if true the action panel buttons are laid out horizontally below the property panel,
   * otherwise vertically on its right side
   * @param detailPanelState the initial detail panel state (HIDDEN or EMBEDDED, DIALOG is not available upon initialization)
   */
  public EntityPanel(final boolean refreshOnInit, final boolean specialRendering, final boolean horizontalButtons,
                     final int detailPanelState) {
    this(refreshOnInit, specialRendering, horizontalButtons, detailPanelState, true);
  }

  /**
   * Initializes a new EntityPanel instance.
   * @param refreshOnInit if true then the underlying data model is refreshed during initialization
   * @param specialRendering if true then each row in the table model (if any)
   * is colored according to the underlying entity
   * @param horizontalButtons if true the action panel buttons are laid out horizontally below the property panel,
   * otherwise vertically on its right side
   * @param detailPanelState the initial detail panel state (HIDDEN or EMBEDDED, DIALOG is not available upon initialization)
   * @param queryConfigurationAllowed true if this panel should allow it's underlying query to be configured
   */
  public EntityPanel(final boolean refreshOnInit, final boolean specialRendering, final boolean horizontalButtons,
                     final int detailPanelState, final boolean queryConfigurationAllowed) {
    this(refreshOnInit, specialRendering, horizontalButtons, detailPanelState,
            queryConfigurationAllowed,  false);
  }

  /**
   * Initializes a new EntityPanel instance.
   * @param refreshOnInit if true then the underlying data model is refreshed during initialization
   * @param specialRendering if true then each row in the table model (if any)
   * is colored according to the underlying entity
   * @param horizontalButtons if true the action panel buttons are laid out horizontally below the property panel,
   * otherwise vertically on its right side
   * @param detailPanelState the initial detail panel state (HIDDEN or EMBEDDED, DIALOG is not available upon initialization)
   * @param queryConfigurationAllowed true if this panel should allow it's underlying query to be configured
   * @param compactPanel true if this panel should be laid out in a compact state
   */
  public EntityPanel(final boolean refreshOnInit, final boolean specialRendering, final boolean horizontalButtons,
                     final int detailPanelState, final boolean queryConfigurationAllowed, final boolean compactPanel) {
    if (detailPanelState == DIALOG)
      throw new IllegalArgumentException("EntityPanel constructor only accepts HIDDEN or EMBEDDED as default detail states");

    this.refreshOnInit = refreshOnInit;
    this.queryConfigurationAllowed = queryConfigurationAllowed;
    this.specialRendering = specialRendering;
    this.buttonPlacement = horizontalButtons ? BorderLayout.SOUTH : BorderLayout.EAST;
    this.detailPanelState = detailPanelState;
    this.compactPanel = compactPanel;
  }

  /**
   * Sets the EntityModel, this method can and should only be called once
   * @param model the EntityModel instance to use with this EntityPanel
   * @return this EntityPanel instance
   */
  public EntityPanel setModel(final EntityModel model) {
    if (this.model != null)
      throw new RuntimeException("EntityPanel already has a model: " + this.model);

    this.model = model;
    bindModelEvents();

    return this;
  }

  /** {@inheritDoc} */
  public EntityModel getModel() {
    return this.model;
  }

  /**
   * Initializes this EntityPanel, override to add any specific initialization
   * functionality, to show the search panel for example.
   * <code>setModel()</code> must be called prior to calling this method.
   * Remember to return right away if isInitialized() returns true and to call super.initialize()
   * After this method has finished isInitialized() returns true
   * @see #isInitialized()
   * @see #setModel(org.jminor.framework.client.model.EntityModel)
   */
  public void initialize() {
    if (isInitialized())
      return;

    if (model == null)
      throw new RuntimeException("Cannot initialize a EntityPanel without a model, call setModel() first");
    try {
      UiUtil.setWaitCursor(true, this);

      bindEvents();
      addComponentListener(new ComponentAdapter() {
        public void componentHidden(ComponentEvent e) {
          setFilterPanelsVisible(false);
        }
        public void componentShown(ComponentEvent e) {
          setFilterPanelsVisible(true);
        }
      });
      initializeResizing();
      if ((Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.USE_KEYBOARD_NAVIGATION))
        initializeNavigation();
      if ((Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.USE_FOCUS_ACTIVATION)) {//todo mind that darn memory leak!! only use for persistent panels?
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(
                "focusOwner", new java.beans.PropertyChangeListener() {
          public void propertyChange(final PropertyChangeEvent evt) {
            final Component focusOwner = (Component) evt.getNewValue();
            if (focusOwner != null && isParentPanel(focusOwner))
              model.stActive.setActive(true);
          }
        });
      }
      initializeAssociatedPanels();
      setupControls();
      initializeControlPanels();

      final List<EntityPanelInfo> detailPanelInfos = getDetailPanelInfo();
      this.detailEntityPanels = new LinkedHashMap<EntityPanelInfo, EntityPanel>(detailPanelInfos.size());
      for (final EntityPanelInfo detailPanelInfo : detailPanelInfos) {
        final EntityModel detailModel = model.getDetailModel(detailPanelInfo.getEntityModelClass());
        if (detailModel == null)
          throw new RuntimeException("Detail model of type " + detailPanelInfo.getEntityModelClass()
                  + " not found in model of type " + model.getClass());
        this.detailEntityPanels.put(detailPanelInfo, detailPanelInfo.getInstance(detailModel));
      }
      this.editPanel = initializeEditPanel();
      this.entityTablePanel = model.getTableModel() != null ? initializeEntityTablePanel(specialRendering) : null;
      if (entityTablePanel != null) {
        entityTablePanel.addSouthPanelButtons(getSouthPanelButtons(entityTablePanel));
        if (editPanel != null || detailEntityPanels.size() > 0) {
          entityTablePanel.setDoubleClickAction(new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
              if (editPanel != null && getEditPanelState() == HIDDEN)
                setEditPanelState(DIALOG);
              else if (getDetailPanelState() == HIDDEN)
                setDetailPanelState(DIALOG);
            }
          });
        }
        entityTablePanel.setMinimumSize(new Dimension(0,0));
      }
      this.horizontalSplitPane = this.detailEntityPanels.size() > 0 ? initializeLeftRightSplitPane() : null;
      this.detailTabPane = this.detailEntityPanels.size() > 0 ? initializeDetailTabPane() : null;

      bindTableModelEvents();
      bindTablePanelEvents();

      initializeUI();

      setDetailPanelState(detailPanelState);
      setEditPanelState(EMBEDDED);

      if (refreshOnInit)
        model.refresh();//refreshes combo models
      else
        model.refreshComboBoxModels();
    }
    catch (UserException e) {
      throw e.getRuntimeException();
    }
    finally {
      this.initialized = true;
      UiUtil.setWaitCursor(false, this);
    }
  }

  /**
   * @return true if the method initialize() has been called on this EntityPanel instance
   */
  public boolean isInitialized() {
    return initialized;
  }

  /**
   * @return true if this EntityPanel allows the underlying EntityTableModel query to be configured
   */
  public boolean isQueryConfigurationAllowed() {
    return queryConfigurationAllowed;
  }

  /**
   * Sets the component that should recieve the focus when the ui is initialised after
   * a new record has been inserted or the panel is activated
   * @param defaultFocusComponent the component
   * @return the component
   */
  public JComponent setDefaultFocusComponent(final JComponent defaultFocusComponent) {
    return this.defaultFocusComponent = defaultFocusComponent;
  }

  /**
   * @return the EntityTablePanel used by this EntityPanel
   */
  public EntityTablePanel getTablePanel() {
    return this.entityTablePanel;
  }

  /**
   * @return the edit panel
   */
  public JPanel getEditPanel() {
    return this.editPanel;
  }

  /**
   * @return the detail panel selected in the detail tab pane.
   * If no detail panels are defined a RuntimeException is thrown.
   */
  public EntityPanel getSelectedDetailPanel() {
    if (this.detailTabPane == null)
      throw new RuntimeException("No detail panels available");

    return (EntityPanel) this.detailTabPane.getSelectedComponent();
  }

  /**
   * Returns the detail panel of the type <code>detailPanelClass</code>, if one is available, otherwise
   * a RuntimeException is thrown
   * @param detailPanelClass the class of the detail panel to retrieve
   * @return the detail panel of the given type
   */
  public EntityPanel getDetailPanel(final Class<? extends EntityPanel> detailPanelClass) {
    for (final EntityPanel detailPanel : detailEntityPanels.values()) {
      if (detailPanel.getClass().equals(detailPanelClass))
        return detailPanel;
    }

    throw new RuntimeException("Detail panel of type: " + detailPanelClass + " not found in panel: " + getClass());
  }

  /** {@inheritDoc} */
  public String toString() {
    return this.model.getCaption();
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
    if (detailTabPane == null)
      return;

    if (state != HIDDEN)
      getSelectedDetailPanel().initialize();

    if (detailPanelState == DIALOG)//if we are leaving the DIALOG state, hide all child detail dialogs
      for (final EntityPanel detailPanel : detailEntityPanels.values())
        if (detailPanel.getDetailPanelState() == DIALOG)
          detailPanel.setDetailPanelState(HIDDEN);

    model.setLinkedDetailModel(state == HIDDEN ? null : getSelectedDetailPanel().getModel());

    detailPanelState = state;
    if (state != DIALOG)
      disposeDetailDialog();

    if (state == EMBEDDED)
      horizontalSplitPane.setRightComponent(detailTabPane);
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
    if (editPanel == null)
      return;

    editPanelState = state;
    if (state != DIALOG)
      disposeEditDialog();

    if (state == EMBEDDED) {
      if (compactPanel)
        compactBase.add(editPanel, BorderLayout.NORTH);
      else
        add(editPanel, BorderLayout.NORTH);
      prepareUI(true, false);
    }
    else if (state == HIDDEN) {
      if (compactPanel)
        compactBase.remove(editPanel);
      else
        remove(editPanel);
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
    if (!isInitialized())
      return;

    if (model.getTableModel() != null)
      entityTablePanel.setFilterPanelsVisible(value);
    for (final EntityPanel detailEntityPanel : detailEntityPanels.values())
      detailEntityPanel.setFilterPanelsVisible(value);
  }

  public void resizePanel(final int direction) {
    switch(direction) {
      case UP :
        setEditPanelState(HIDDEN);
        break;
      case DOWN :
        setEditPanelState(EMBEDDED);
        break;
      case RIGHT :
        int newPos = horizontalSplitPane.getDividerLocation() + DIVIDER_JUMP;
        if (newPos <= horizontalSplitPane.getMaximumDividerLocation())
          horizontalSplitPane.setDividerLocation(newPos);
        else
          horizontalSplitPane.setDividerLocation(horizontalSplitPane.getMaximumDividerLocation());
        break;
      case LEFT :
        newPos = horizontalSplitPane.getDividerLocation() - DIVIDER_JUMP;
        if (newPos >= 0)
          horizontalSplitPane.setDividerLocation(newPos);
        else
          horizontalSplitPane.setDividerLocation(0);
        break;
    }
  }

  /**
   * @param component the component
   * @return true if <code>component</code> is a child component of this EntityPanel
   */
  public boolean isParentPanel(final Component component) {
    final EntityPanel parent = (EntityPanel) SwingUtilities.getAncestorOfClass(EntityPanel.class, component);
    if (parent == this)
      return true;

    //is editDialog parent?
    return editDialog != null && SwingUtilities.getWindowAncestor(component) == editDialog;
  }

  /** {@inheritDoc} */
  public void handleException(final Throwable throwable) {
    handleException(throwable, this);
  }

  /**
   * Handles the given exception
   * @param throwable the exception to handle
   * @param dialogParent the component to use as exception dialog parent
   */
  public void handleException(final Throwable throwable, final JComponent dialogParent) {
    log.error(this, throwable);
    FrameworkUiUtil.handleException(throwable, model.getEntityID(), dialogParent);
  }

  //#############################################################################################
  // Begin - control methods, see setupControls
  //#############################################################################################

  /**
   * Saves the active entity, that is, if no entity is selected it performs a insert otherwise the user
   * is asked whether to update the selected record or insert a new one
   */
  public final void handleSave() {
    if ((getModel().getTableModel() != null && getModel().getTableModel().getSelectionModel().isSelectionEmpty())
            || !getModel().isActiveEntityModified() || !model.isUpdateAllowed()) {
      //no entity selected, selected entity is unmodified or update is not allowed, can only insert
      handleInsert();
    }
    else {//possibly update
      final int choiceIdx = JOptionPane.showOptionDialog(this, FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT),
              FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT_TITLE), -1, JOptionPane.QUESTION_MESSAGE, null,
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_RECORD),
                      FrameworkMessages.get(FrameworkMessages.INSERT_NEW), Messages.get(Messages.CANCEL)},
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE)});
      if (choiceIdx == 0) //update
        handleUpdate();
      else if (choiceIdx == 1) //insert
        handleInsert();
    }
  }

  /**
   * Performs a insert on the active entity
   * @return true in case of successful insert, false otherwise
   */
  public final boolean handleInsert() {
    try {
      validateData();
      try {
        UiUtil.setWaitCursor(true, EntityPanel.this);
        model.insert();
      }
      finally {
        UiUtil.setWaitCursor(false, EntityPanel.this);
      }
      prepareUI(true, true);
      postInsert();
      return true;
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
  public final boolean handleDelete() {
    try {
      if (confirmDelete()) {
        try {
          UiUtil.setWaitCursor(true, EntityPanel.this);
          model.delete();
        }
        finally {
          UiUtil.setWaitCursor(false, EntityPanel.this);
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
  public final boolean handleUpdate() {
    try {
      validateData();
      if (confirmUpdate()) {
        try {
          UiUtil.setWaitCursor(true, EntityPanel.this);
          model.update();
        }
        finally {
          UiUtil.setWaitCursor(false, EntityPanel.this);
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
   * @see #updateSelectedEntities(org.jminor.framework.model.Property)
   * @see #getInputManager(org.jminor.framework.model.Property, java.util.List)
   */
  public void updateSelectedEntities() {
    try {
      updateSelectedEntities(getPropertyToUpdate());
    }
    catch (UserCancelException e) {/**/}
  }

  /**
   * Retrieves a new property value via input dialog and performs an update on the selected entities
   * @param propertyToUpdate the property to update
   * @see #getInputManager(org.jminor.framework.model.Property, java.util.List)
   */
  public void updateSelectedEntities(final Property propertyToUpdate) {
    try {
      if (model.getTableModel() == null || model.getTableModel().stSelectionEmpty.isActive())
        return;

      final List<Entity> selectedEntities = model.getTableModel().getSelectedEntities();
      final EntityPropertyEditor editPanel = new EntityPropertyEditor(
              selectedEntities.get(0).getValue(propertyToUpdate.propertyID), propertyToUpdate,
              model, getInputManager(propertyToUpdate, selectedEntities), selectedEntities.size() > 1);
      UiUtil.showInDialog(this, editPanel, true, FrameworkMessages.get(FrameworkMessages.SET_PROPERTY_VALUE),
              null, editPanel.getOkButton(), editPanel.evtButtonClicked);
      if (editPanel.getButtonValue() == JOptionPane.OK_OPTION) {
        final Object[] oldValues = EntityUtil.setPropertyValue(
                propertyToUpdate.propertyID, editPanel.getValue(), selectedEntities);
        try {
          UiUtil.setWaitCursor(true, this);
          model.update(selectedEntities);
        }
        catch (Exception e) {
          EntityUtil.setPropertyValue(propertyToUpdate.propertyID, oldValues, selectedEntities);
          throw e;
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }
      }
    }
    catch (Exception e) {
      handleException(e);
    }
  }

  /**
   * Shows a dialog containing lists of entities which depend on the selected entities
   */
  public void viewSelectionDependencies() {
    new SwingWorker() {//just testing this thingy
      protected Object doInBackground() throws Exception {
        try {
          final Map<String, List<Entity>> dependencies;
          try {
            UiUtil.setWaitCursor(true, EntityPanel.this);
            dependencies = model.getTableModel().getSelectionDependencies();
          }
          finally {
            UiUtil.setWaitCursor(false, EntityPanel.this);
          }
          if (EntityUtil.activeDependencies(dependencies)) {
            showDependenciesDialog(dependencies, model.getDbConnectionProvider(), EntityPanel.this);
          }
          else {
            JOptionPane.showMessageDialog(EntityPanel.this, FrameworkMessages.get(FrameworkMessages.NONE_FOUND),
                    FrameworkMessages.get(FrameworkMessages.NO_DEPENDENT_RECORDS), JOptionPane.INFORMATION_MESSAGE);
          }

          return true;
        }
        catch (Exception e) {
          handleException(e);
        }

        return false;
      }
    }.execute();
  }

  /**
   * Prints the table if one is available
   * @throws UserException in case of a printer exception
   */
  public void printTable() throws UserException {
    try {
      if (model.getTableModel() != null)
        JPrinter.print(entityTablePanel.getJTable());

      prepareUI(true, false);
    }
    catch (PrinterException pr) {
      throw new UserException(pr);
    }
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
    if (getTablePanel() == null)
      return;

    final JPanel togglePanel = new JPanel(new GridLayout(getTablePanel().getJTable().getColumnCount(), 1));
    final Enumeration<TableColumn> columns = getTablePanel().getJTable().getColumnModel().getColumns();
    final List<JCheckBox> buttonList = new ArrayList<JCheckBox>();
    while (columns.hasMoreElements()) {
      final TableColumn column = columns.nextElement();
      final JCheckBox chkColumn = new JCheckBox(column.getHeaderValue().toString(), column.getPreferredWidth() > 0);
      buttonList.add(chkColumn);
      togglePanel.add(chkColumn);
    }
    final JScrollPane scroller = new JScrollPane(togglePanel);
    scroller.setPreferredSize(new Dimension(200, 400));
    final int result = JOptionPane.showOptionDialog(this, scroller,
            FrameworkMessages.get(FrameworkMessages.SELECT_COLUMNS), JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, null, null);
    if (result == JOptionPane.OK_OPTION) {
      final TableColumnModel columnModel = getTablePanel().getJTable().getColumnModel();
      for (final JCheckBox chkButton : buttonList) {
        final TableColumn column = columnModel.getColumn(buttonList.indexOf(chkButton));
        getTablePanel().setPropertyColumnVisible((Property) column.getIdentifier(), chkButton.isSelected());
      }
    }
  }

  //#############################################################################################
  // End - control methods
  //#############################################################################################

  /**
   * Prepares the UI, by clearing the input fields and setting the default focus,
   * if both parameters are set to false then there is no effect
   * @param requestDefaultFocus if true the component defined as the defaultFocusComponent
   * gets the input focus, if none is defined the first child component of this EntityPanel is used,
   * if no edit panel is available the table recieves the focus
   * @param clearUI if true the the input components are cleared
   * @see #setDefaultFocusComponent(javax.swing.JComponent)
   */
  public final void prepareUI(boolean requestDefaultFocus, final boolean clearUI) {
    if (clearUI)
      model.clear();
    if (requestDefaultFocus) {
      if (getEditPanelState() != EMBEDDED && entityTablePanel != null)
        entityTablePanel.requestFocusInWindow();
      else if (defaultFocusComponent != null)
        defaultFocusComponent.requestFocusInWindow();
      else if (getComponentCount() > 0)
        getComponents()[0].requestFocusInWindow();
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
            model.getTableModel().stSelectionEmpty.getReversedState(),
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
   * @return a control for deleting the selected entities
   */
  public Control getDeleteSelectedControl() {
    return ControlFactory.methodControl(this, "handleDelete", FrameworkMessages.get(FrameworkMessages.DELETE),
            new AggregateState(AggregateState.AND,
                    model.getDeleteAllowedState(),
                    model.getTableModel().stSelectionEmpty.getReversedState()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP), 0, null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for refreshing the model data
   */
  public Control getRefreshControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.REFRESH_MNEMONIC);
    return ControlFactory.methodControl(model, "refresh", FrameworkMessages.get(FrameworkMessages.REFRESH),
            model.stActive, FrameworkMessages.get(FrameworkMessages.REFRESH_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_REFRESH_16));
  }

  /**
   * @return a control for updating a property in the selected entities
   */
  public Control getUpdateSelectedControl() {
    return ControlFactory.methodControl(this, "updateSelectedEntities",
            FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED),
            new AggregateState(AggregateState.AND,
                    model.getUpdateAllowedState(),
                    model.getTableModel().stSelectionEmpty.getReversedState()),
            FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP), 0,
            null, Images.loadImage(Images.IMG_SAVE_16));
  }

  /**
   * @return a control set containing a set of controls, one for each updatable property in the
   * underlying entity, for performing an update on the selected entities
   */
  public ControlSet getUpdateSelectedControlSet() {
    final State enabled = new AggregateState(AggregateState.AND,
            model.getUpdateAllowedState(),
            model.getTableModel().stSelectionEmpty.getReversedState());
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED),
            (char) 0, Images.loadImage("Modify16.gif"), enabled);
    ret.setDescription(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP));
    for (final Property property : getUpdateProperties()) {
      final String caption = property.getCaption() == null ? property.propertyID : property.getCaption();
      ret.add(UiUtil.linkToEnabledState(enabled, new AbstractAction(caption) {
        public void actionPerformed(final ActionEvent e) {
          updateSelectedEntities(property);
        }
      }));
    }

    return ret;
  }

  /**
   * @return a control for deleting the active entity (or the selected entities if a table model is available)
   */
  public Control getDeleteControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.DELETE_MNEMONIC);
    return ControlFactory.methodControl(this, "handleDelete", FrameworkMessages.get(FrameworkMessages.DELETE),
            new AggregateState(AggregateState.AND,
                    model.stActive,
                    model.getDeleteAllowedState(),
                    model.stEntityActive),//changed from stSelectionEmpty.getReversedState()
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP) + " (ALT-" + mnemonic + ")", mnemonic.charAt(0), null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for clearing the UI controls
   */
  public Control getClearControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.CLEAR_MNEMONIC);
    return ControlFactory.methodControl(model, "clear", FrameworkMessages.get(FrameworkMessages.CLEAR),
            model.stActive, FrameworkMessages.get(FrameworkMessages.CLEAR_ALL_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_NEW_16));
  }

  /**
   * @return a control for performing an update on the active entity
   */
  public Control getUpdateControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.UPDATE_MNEMONIC);
    return ControlFactory.methodControl(this, "handleUpdate", FrameworkMessages.get(FrameworkMessages.UPDATE),
            new AggregateState(AggregateState.AND,
                    model.stActive,
                    model.getUpdateAllowedState(),
                    model.stEntityActive,
                    model.getActiveEntityModifiedState()),
            FrameworkMessages.get(FrameworkMessages.UPDATE_TIP) + " (ALT-" + mnemonic + ")", mnemonic.charAt(0),
            null, Images.loadImage(Images.IMG_SAVE_16));
  }

  /**
   * @return a control for performing an insert on the active entity
   */
  public Control getInsertControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.INSERT_MNEMONIC);
    return ControlFactory.methodControl(this, "handleSave", FrameworkMessages.get(FrameworkMessages.INSERT),
            new AggregateState(AggregateState.AND, model.stActive, model.getInsertAllowedState()),
            FrameworkMessages.get(FrameworkMessages.INSERT_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage("Add16.gif"));
  }

  /**
   * @return a control for performing a save on the active entity
   */
  public Control getSaveControl() {
    final String insertCaption = FrameworkMessages.get(FrameworkMessages.INSERT_UPDATE);
    final State stInsertUpdate = new AggregateState(AggregateState.OR, model.getInsertAllowedState(),
            new AggregateState(AggregateState.AND, model.getUpdateAllowedState(), model.getActiveEntityModifiedState()));
    return ControlFactory.methodControl(this, "handleSave", insertCaption,
            new AggregateState(AggregateState.AND, model.stActive, stInsertUpdate),
            FrameworkMessages.get(FrameworkMessages.INSERT_UPDATE_TIP),
            insertCaption.charAt(0), null, Images.loadImage(Images.IMG_PROPERTIES_16));
  }

  /**
   * Associates <code>control</code> with <code>controlCode</code>
   * @param controlCode the control code
   * @param control the control to associate with <code>controlCode</code>
   */
  public final void setControl(final String controlCode, final Control control) {
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
   * @return a static EntityPanel showing the given entities
   * @throws UserException in case of an exception
   */
  public static EntityPanel createStaticEntityPanel(final List<Entity> entities) throws UserException {
    if (entities == null || entities.size() == 0)
      throw new UserException("Cannot create an EntityPanel without the entities");

    return createStaticEntityPanel(entities, null);
  }

  /**
   * Creates a static entity panel showing the given entities
   * @param entities the entities to show in the panel
   * @param dbProvider the IEntityDbProvider, in case the returned panel should require one
   * @return a static EntityPanel showing the given entities
   * @throws UserException in case of an exception
   */
  public static EntityPanel createStaticEntityPanel(final List<Entity> entities,
                                                    final IEntityDbProvider dbProvider) throws UserException {
    if (entities == null || entities.size() == 0)
      throw new UserException("Cannot create an EntityPanel without the entities");

    return createStaticEntityPanel(entities, dbProvider, entities.get(0).getEntityID(), true);
  }

  /**
   * Creates a static entity panel showing the given entities
   * @param entities the entities to show in the panel
   * @param dbProvider the IEntityDbProvider, in case the returned panel should require one
   * @param entityID the entityID
   * @return a static EntityPanel showing the given entities
   * @throws UserException in case of an exception
   */
  public static EntityPanel createStaticEntityPanel(final List<Entity> entities,
                                                    final IEntityDbProvider dbProvider,
                                                    final String entityID) throws UserException {
    return createStaticEntityPanel(entities, dbProvider, entityID, true);
  }

  /**
   * Creates a static entity panel showing the given entities
   * @param entities the entities to show in the panel
   * @param dbProvider the IEntityDbProvider, in case the returned panel should require one
   * @param entityID the entityID
   * @param includePopupMenu if true then the default popup menu is included in the table panel, otherwise it's hidden
   * @return a static EntityPanel showing the given entities
   * @throws UserException in case of an exception
   */
  public static EntityPanel createStaticEntityPanel(final List<Entity> entities, final IEntityDbProvider dbProvider,
                                                    final String entityID, final boolean includePopupMenu) throws UserException {
    final EntityModel model = new EntityModel(entityID, dbProvider, entityID) {
      protected EntityTableModel initializeTableModel() {
        return new EntityTableModel(dbProvider, entityID) {
          protected List<Entity> getAllEntitiesFromDb(final ICriteria criteria) throws DbException, UserException {
            return entities;
          }
          public boolean isQueryRangeEnabled() {
            return false;
          }
        };
      }
    };

    final EntityPanel ret = new EntityPanel(true, false, false, EMBEDDED, false) {
      protected EntityTablePanel initializeEntityTablePanel(final boolean specialRendering) {
        return new EntityTablePanel(model.getTableModel(), getTablePopupControlSet(), false, false) {
          protected JPanel initializeSearchPanel() {
            return null;
          }
          protected JToolBar getRefreshToolbar() {
            return null;
          }
        };
      }
      protected ControlSet getTablePopupControlSet() {
        return includePopupMenu ? super.getTablePopupControlSet() : null;
      }
      protected JPanel initializePropertyPanel() {
        return null;
      }
    };
    ret.setModel(model);
    ret.initialize();

    return ret;
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
   * Initializes this EntityPanel's UI
   */
  protected void initializeUI() {
    setLayout(new BorderLayout(5,5));
    if (detailTabPane == null) { //no left right split pane
      add(entityTablePanel, BorderLayout.CENTER);
    }
    else {
      if (compactPanel) {
        compactBase = new JPanel(new BorderLayout(5,5));
        compactBase.add(entityTablePanel, BorderLayout.CENTER);
        horizontalSplitPane.setLeftComponent(compactBase);
      }
      else {
        horizontalSplitPane.setLeftComponent(entityTablePanel);
      }
      horizontalSplitPane.setRightComponent(detailTabPane);
      add(horizontalSplitPane, BorderLayout.CENTER);
    }
  }

  /**
   * Initializes the edit panel.
   *
   * The default layout is as follows:
   * __________________________________
   * |     property           |action |
   * |      panel             | panel | } edit panel
   * |________________________|_______|
   *
   * @return a panel used for editing entities
   */
  protected JPanel initializeEditPanel() {
    final JPanel propertyPanel = initializePropertyPanel();
    if (propertyPanel == null)
      return null;

    final JPanel editPanel = new JPanel(new BorderLayout(5,5));
    editPanel.setMinimumSize(new Dimension(0,0));
    editPanel.setBorder(BorderFactory.createEtchedBorder());
    final JPanel propertyBase =
            new JPanel(new FlowLayout(buttonPlacement.equals(BorderLayout.SOUTH) ? FlowLayout.CENTER : FlowLayout.LEADING,5,5));
    editPanel.addMouseListener(new ActivationFocusAdapter(propertyBase));
    propertyBase.add(propertyPanel);
    editPanel.add(propertyBase, BorderLayout.CENTER);
    final JComponent actionPanel = (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.TOOLBAR_BUTTONS) ?
            initializeActionToolBar() : initializeActionPanel();
    if (actionPanel != null)
      editPanel.add(actionPanel, (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.TOOLBAR_BUTTONS) ?
              (buttonPlacement.equals(BorderLayout.SOUTH) ? BorderLayout.NORTH : BorderLayout.WEST) :
              (buttonPlacement.equals(BorderLayout.SOUTH) ? BorderLayout.SOUTH : BorderLayout.EAST));

    return editPanel;
  }

  /**
   * Initializes the horizontal split pane, used in the case of detail panel(s)
   * @return the horizontal split pane
   */
  protected JSplitPane initializeLeftRightSplitPane() {
    final JSplitPane leftRightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
    leftRightSplitPane.setBorder(BorderFactory.createEmptyBorder());
    leftRightSplitPane.setOneTouchExpandable(true);
    leftRightSplitPane.setResizeWeight(getDetailSplitPaneResizeWeight());
    leftRightSplitPane.setDividerSize(18);

    return leftRightSplitPane;
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
   * Initializes the action panel, that is, the panel containing action buttons for editing entities (Insert, Update...)
   * @return the action panel
   */
  protected JPanel initializeActionPanel() {
    JPanel ret;
    if (buttonPlacement.equals(BorderLayout.SOUTH)) {
      ret = new JPanel(new FlowLayout(FlowLayout.CENTER,5,5));
      ret.add(ControlProvider.createHorizontalButtonPanel(getActionPanelControlSet()));
    }
    else {
      ret = new JPanel(new BorderLayout(5,5));
      ret.add(ControlProvider.createVerticalButtonPanel(getActionPanelControlSet()), BorderLayout.NORTH);
    }

    return ret;
  }

  /**
   * Initializes the action toolbar, that is, the toolbar containing action buttons for editing entities (Insert, Update...)
   * @return the action toolbar
   */
  protected JToolBar initializeActionToolBar() {
    return ControlProvider.createToolbar(getActionPanelControlSet(), JToolBar.VERTICAL);
  }

  /**
   * Initializes the JTabbedPane containing the detail panels, used in case of multiple detail panels
   * @return the JTabbedPane for holding detail panels
   */
  protected JTabbedPane initializeDetailTabPane() {
    final JTabbedPane ret = new JTabbedPane();
    ret.setFocusable(false);
    ret.setUI(new BasicTabbedPaneUI() {
      protected Insets getContentBorderInsets(final int tabPlacement) {
        return new Insets(1,0,0,0);
      }
    });
    for (final EntityPanel detailPanel : detailEntityPanels.values())
      ret.addTab(detailPanel.model.getCaption(), detailPanel);

    ret.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent e) {
        model.setLinkedDetailModel(getDetailPanelState() != HIDDEN ? getSelectedDetailPanel().getModel() : null);
        getSelectedDetailPanel().initialize();
      }
    });
    ret.addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
          setDetailPanelState(getDetailPanelState() == DIALOG ? EMBEDDED : DIALOG);
        else if (e.getButton() == MouseEvent.BUTTON2)
          setDetailPanelState(getDetailPanelState() == EMBEDDED ? HIDDEN : EMBEDDED);
      }
    });

    return ret;
  }

  /**
   * Initializes the property panel, that is, the panel containing the UI controls for editing the active entity
   * @return the property panel
   */
  protected abstract JPanel initializePropertyPanel();

  /**
   * @return a list of EntityPanelInfo objects, specifying which detail panels this panel should contain
   */
  protected List<EntityPanelInfo> getDetailPanelInfo() {
    return new ArrayList<EntityPanelInfo>(0);
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
   * Initializes the table panel
   * @param specialRendering true if the a table row should be colored according to the underlying entity
   * @return the table panel
   */
  protected EntityTablePanel initializeEntityTablePanel(final boolean specialRendering) {
    return new EntityTablePanel(getModel().getTableModel(), getTablePopupControlSet(),
            specialRendering, isQueryConfigurationAllowed());
  }

  /**
   * @param tablePanel the EntityTablePanel
   * @return an array containing the buttons to include on the south panel toolbar, a null item indicates a seperator
   */
  protected AbstractButton[] getSouthPanelButtons(final EntityTablePanel tablePanel) {
    final List<AbstractButton> ret = new ArrayList<AbstractButton>();

    AbstractButton tmp = getToggleSummaryPanelButton(tablePanel);
    if (tmp != null)
      ret.add(tmp);
    tmp = getSearchButton(tablePanel);
    if (tmp != null)
      ret.add(tmp);
    tmp = getPrintButton();
    if (tmp != null)
      ret.add(tmp);
    ret.add(null);
    tmp = getUpdateButton();
    if (tmp != null)
      ret.add(tmp);
    tmp = getDeleteButton();
    if (tmp != null)
      ret.add(tmp);
    tmp = getClearSelectionButton();
    if (tmp != null)
      ret.add(tmp);
    ret.add(null);
    tmp = getSelectionDownButton();
    if (tmp != null)
      ret.add(tmp);
    tmp = getSelectionUpButton();
    if (tmp != null)
      ret.add(tmp);
    ret.add(null);
    tmp = getToggleEditPanelButton();
    if (tmp != null)
      ret.add(tmp);
    tmp = getToggleDetaiPanelButton();
    if (tmp != null)
      ret.add(tmp);

    return ret.toArray(new AbstractButton[ret.size()]);
  }

  /**
   * Sets up the controls used by this EntityPanel
   * @see org.jminor.common.ui.control.Control
   * @see #setControl(String, org.jminor.common.ui.control.Control)
   * @see #getControl(String)
   */
  protected void setupControls() {
    if (!model.isReadOnly()) {
      if (model.isInsertAllowed())
        setControl(INSERT, getInsertControl());
      if (model.isUpdateAllowed())
        setControl(UPDATE, getUpdateControl());
      if (model.isDeleteAllowed())
        setControl(DELETE, getDeleteControl());
    }
    setControl(CLEAR, getClearControl());
    if (model.getTableModel() != null) {
      if (!model.isReadOnly() && model.isUpdateAllowed() && model.isMultipleUpdateAllowed())
        setControl(UPDATE_SELECTED, getUpdateSelectedControlSet());
      setControl(REFRESH, getRefreshControl());
      if (!model.isReadOnly() && model.isDeleteAllowed())
        setControl(MENU_DELETE, getDeleteSelectedControl());
      setControl(PRINT, getPrintControl());
      setControl(VIEW_DEPENDENCIES, getViewDependenciesControl());
      if (isQueryConfigurationAllowed())
        setControl(CONFIGURE_QUERY, getConfigureQueryControl());
      setControl(SELECT_COLUMNS, getSelectColumnsControl());
    }
  }

  /**
   * @return the ControlSet on which the table popup menu is based
   */
  protected ControlSet getTablePopupControlSet() {
    boolean seperatorRequired = false;
    final ControlSet ret = new ControlSet("");
    if (detailEntityPanels.size() > 0) {
      ret.add(getDetailPanelControls(EMBEDDED));
      seperatorRequired = true;
    }
    if (seperatorRequired) {
      ret.addSeparator();
      seperatorRequired = false;
    }
    if (controlMap.containsKey(UPDATE_SELECTED)) {
      ret.add(controlMap.get(UPDATE_SELECTED));
      seperatorRequired = true;
    }
    if (controlMap.containsKey(MENU_DELETE)) {
      ret.add(controlMap.get(MENU_DELETE));
      seperatorRequired = true;
    }
    if (seperatorRequired) {
      ret.addSeparator();
      seperatorRequired = false;
    }
    if (controlMap.containsKey(VIEW_DEPENDENCIES)) {
      ret.add(controlMap.get(VIEW_DEPENDENCIES));
      seperatorRequired = true;
    }
    if (seperatorRequired) {
      ret.addSeparator();
      seperatorRequired = false;
    }
    final ControlSet printControls = getPrintControls();
    if (printControls != null) {
      ret.add(getPrintControls());
      seperatorRequired = true;
    }
    if (controlMap.containsKey(CONFIGURE_QUERY)) {
      if (seperatorRequired) {
        ret.addSeparator();
        seperatorRequired = false;
      }
      ret.add(controlMap.get(CONFIGURE_QUERY));
    }
    if (controlMap.containsKey(SELECT_COLUMNS)) {
      if (seperatorRequired)
        ret.addSeparator();
      ret.add(controlMap.get(SELECT_COLUMNS));
    }

    return ret;
  }

  /**
   * Initializes a ControlSet containing a control for setting the state to <code>status</code> on each detail panel.
   * @param status the status
   * @return a ControlSet for controlling the state of the detail panels
   */
  protected ControlSet getDetailPanelControls(final int status) {
    if (detailEntityPanels.size() == 0)
      return null;

    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.DETAIL_TABLES));
    for (final EntityPanel detailPanel : detailEntityPanels.values()) {
      final EntityModel model = detailPanel.getModel();
      if (model == null)
        throw new RuntimeException("EntityPanel does not have a EntityModel associated with it");
      ret.add(new Control(model.getCaption()) {
        public void actionPerformed(ActionEvent e) {
          detailTabPane.setSelectedComponent(detailPanel);
          setDetailPanelState(status);
        }
      });
    }

    return ret;
  }

  /**
   * Initializes the print control set, override to provide specific printing funtionality, i.e. report printing
   * @return the print control set
   */
  protected ControlSet getPrintControls() {
    return controlMap.containsKey(PRINT) ? new ControlSet(Messages.get(Messages.PRINT), (char) 0, null,
            Images.loadImage("Print16.gif"), controlMap.get(PRINT)) : null;
  }

  /**
   * Override to keep event bindings in one place,
   * this method is called during initialization
   */
  protected void bindEvents() {}

  /**
   * Override to keep table model event bindings in one place,
   * this method is called during initialization
   */
  protected void bindTableModelEvents() {}

  /**
   * Binds events associated to the EntityTablePanel
   */
  protected void bindTablePanelEvents() {
    if (entityTablePanel == null)
      return;

    if (!getModel().isReadOnly() && getModel().isDeleteAllowed()) {
      entityTablePanel.getJTable().addKeyListener(new KeyAdapter() {
        public void keyTyped(KeyEvent e) {
          if (e.getKeyChar() == KeyEvent.VK_DELETE && !getModel().getTableModel().stSelectionEmpty.isActive())
            handleDelete();
        }
      });
    }
    getModel().evtEntitiesChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        entityTablePanel.getJTable().repaint();
      }
    });
  }

  /**
   * @return the ControlSet on which the action panel is based
   */
  protected ControlSet getActionPanelControlSet() {
    final ControlSet ret = new ControlSet("Actions");
    if (controlMap.containsKey(INSERT))
      ret.add(controlMap.get(INSERT));
    if (controlMap.containsKey(UPDATE))
      ret.add(controlMap.get(UPDATE));
    if (controlMap.containsKey(DELETE))
      ret.add(controlMap.get(DELETE));
    if (controlMap.containsKey(CLEAR))
      ret.add(controlMap.get(CLEAR));
    if (controlMap.containsKey(REFRESH))
      ret.add(controlMap.get(REFRESH));

    return ret;
  }

  //#############################################################################################
  // End - initialization methods
  //#############################################################################################

  /**
   * for overriding, called after a successful insert, after the UI has been initialized for a new entry
   * @throws UserException in case of an exception
   */
  protected void postInsert() throws UserException {}

  /**
   * for overriding, called before insert/update
   * @throws UserException in case of an exception
   * @throws UserCancelException in case the user cancels the action during validation
   */
  protected void validateData() throws UserException, UserCancelException {}

  /**
   * for overriding, to provide specific input components for multi-entity update
   * @param property the property for which to get the InputManager
   * @param toUpdate the entities that are about to be updated
   * @return the InputManager handling input for <code>property</code>
   * @see #updateSelectedEntities
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected EntityPropertyEditor.InputManager getInputManager(final Property property, final List<Entity> toUpdate) {
    return null;
  }

  /**
   * Called before a delete is performed, if true is returned the delete action is performed otherwise it is cancelled
   * @return true if the delete action should be performed
   */
  protected boolean confirmDelete() {
    final String[] msgs = getConfirmationMessages(CONFIRM_TYPE_DELETE);
    final int res = JOptionPane.showConfirmDialog(EntityPanel.this,
            msgs[0], msgs[1], JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  /**
   * Called before an update is performed, if true is returned the update action is performed otherwise it is cancelled
   * @return true if the update action should be performed
   */
  protected boolean confirmUpdate() {
    final String[] msgs = getConfirmationMessages(CONFIRM_TYPE_UPDATE);
    final int res = JOptionPane.showConfirmDialog(EntityPanel.this,
            msgs[0], msgs[1], JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  /**
   * @param type the confirmation message type
   * @return a string array containing two elements, the element at index 0 is used
   * as the message displayed in the dialog and the element at index 1 is used as the dialog title,
   * i.e. ["Are you sure you want to delete the selected records?", "About to delete selected records"]
   */
  protected String[] getConfirmationMessages(final int type) {
    switch (type) {
      case CONFIRM_TYPE_DELETE :
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_SELECTED),
                FrameworkMessages.get(FrameworkMessages.DELETE)};
      case CONFIRM_TYPE_INSERT :
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_INSERT),
                FrameworkMessages.get(FrameworkMessages.INSERT)};
      case CONFIRM_TYPE_UPDATE :
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
    detailDialog = UiUtil.showInDialog(UiUtil.getParentWindow(EntityPanel.this), detailTabPane, false,
            getModel().getCaption() + " - " + FrameworkMessages.get(FrameworkMessages.DETAIL_TABLES), false, true,
            null, size, location, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
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
            (editPanel != null) ? (int) (parentSize.height/1.5) : parentSize.height-54);
  }

  /**
   * Shows the edit panel in a non-modal dialog
   */
  protected void showEditDialog() {
    final Point location = getLocationOnScreen();
    location.setLocation(location.x+1, location.y + getSize().height-editPanel.getSize().height-98);
    editDialog = UiUtil.showInDialog(UiUtil.getParentWindow(EntityPanel.this), editPanel, false,
            getModel().getCaption(), false, true,
            null, null, location, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setEditPanelState(HIDDEN);
      }
    });
    prepareUI(true, false);
  }

  /**
   * Prints a report based on the selected entities
   * @param reportPath the path to the report object
   * @param reportParams a map containing the parameters required for the report
   * @throws UserException in case of an exception
   */
  protected void printReportForSelected(final String reportPath, final HashMap reportParams) throws UserException {
    try {
      final JFrame frame = new JFrame(FrameworkMessages.get(FrameworkMessages.REPORT_PRINTER));
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.getContentPane().add(new JRViewer(initJasperPrintObject(reportPath, reportParams)));
      UiUtil.resizeWindow(frame, 0.8, new Dimension(800, 600));
      UiUtil.centerWindow(frame);
      frame.setVisible(true);
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  /**
   * Initializes the button used to toggle the search panel state (hidden, visible and advanced)
   * @param tablePanel the table panel
   * @return a search panel toggle button
   */
  private JButton getSearchButton(final EntityTablePanel tablePanel) {
    if (!isQueryConfigurationAllowed())
      return null;

    final JButton ret = new JButton(new Control() {
      public void actionPerformed(ActionEvent e) {
        tablePanel.toggleSearchPanel();
      }
    });
    ret.setIcon(Images.loadImage("Filter16.gif"));
    ret.setToolTipText(FrameworkMessages.get(FrameworkMessages.SEARCH));
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  /**
   * Initializes a button for updating multiple entities
   * @return the multiple entity update button
   */
  private JButton getUpdateButton() {
    if (model.isReadOnly() || !model.isMultipleUpdateAllowed() || !model.isUpdateAllowed())
      return null;

    final ControlSet updateSet = getUpdateSelectedControlSet();
    final JPopupMenu menu = ControlProvider.createPopupMenu(updateSet);
    final JButton ret = new JButton(Images.loadImage("Modify16.gif"));
    ret.setToolTipText(updateSet.getDescription());
    UiUtil.linkToEnabledState(updateSet.getEnabledState(), ret);
    ret.addMouseListener(new MouseAdapter() {
      public void mouseReleased(final MouseEvent e) {
        menu.show(ret, e.getX(), e.getY()-menu.getPreferredSize().height);
      }
    });
    ret.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        menu.show(ret, (int) ret.getLocation().getX()+ret.getWidth()/2,
                (int) ret.getLocation().getY()+ret.getHeight()/2-menu.getPreferredSize().height);
      }
    });
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getDeleteButton() {
    if (model.isReadOnly() || !model.isDeleteAllowed())
      return null;

    final Control delete = getDeleteSelectedControl();
    delete.setName(null);
    delete.setIcon(Images.loadImage("Delete16.gif"));
    final JButton ret = ControlProvider.createButton(delete);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getClearSelectionButton() {
    final Control clearSelection = ControlFactory.methodControl(getModel().getTableModel(), "clearSelection", null,
            getModel().getTableModel().stSelectionEmpty.getReversedState(), null, -1, null,
            Images.loadImage("ClearSelection16.gif"));
    clearSelection.setDescription(FrameworkMessages.get(FrameworkMessages.CLEAR_SELECTION_TIP));
    final JButton ret = ControlProvider.createButton(clearSelection);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getSelectionDownButton() {
    final Control selectionDown = ControlFactory.methodControl(getModel().getTableModel(), "selectionDown",
            Images.loadImage("Down16.gif"));
    selectionDown.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_DOWN_TIP));
    final JButton ret = ControlProvider.createButton(selectionDown);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getSelectionUpButton() {
    final Control selectionUp = ControlFactory.methodControl(getModel().getTableModel(), "selectionUp",
            Images.loadImage("Up16.gif"));
    selectionUp.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_UP_TIP));
    final JButton ret = ControlProvider.createButton(selectionUp);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getPrintButton() {
    final Control print = getPrintControl();
    print.setName("");
    print.setIcon(Images.loadImage("Print16.gif"));
    final JButton ret = ControlProvider.createButton(print);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private Dimension getSouthButtonSize() {
    return new Dimension(20,20);
  }

  private JButton getToggleEditPanelButton() {
    if (editPanel == null)
      return null;

    final Control toggle = ControlFactory.methodControl(this, "toggleEditPanelState",
            Images.loadImage("Form16.gif"));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_EDIT_TIP));
    final JButton ret = ControlProvider.createButton(toggle);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JButton getToggleDetaiPanelButton() {
    if (detailEntityPanels.size() == 0)
      return null;

    final Control toggle = ControlFactory.methodControl(this, "toggleDetailPanelState",
            Images.loadImage("History16.gif"));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_DETAIL_TIP));
    final JButton ret = ControlProvider.createButton(toggle);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private JToggleButton getToggleSummaryPanelButton(final EntityTablePanel tablePanel) {
    final ToggleBeanPropertyLink toggle = ControlFactory.toggleControl(tablePanel, "summaryPanelVisible", null,
            tablePanel.evtTableSummaryPanelVisibleChanged);
    toggle.setIcon(Images.loadImage("Sum16.gif"));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_SUMMARY_TIP));
    final JToggleButton ret = ControlProvider.createToggleButton(toggle);
    ret.setPreferredSize(getSouthButtonSize());

    return ret;
  }

  private void disposeEditDialog() {
    if (editDialog != null) {
      editDialog.setVisible(false);
      editDialog.dispose();
      editDialog = null;
    }
  }

  private void disposeDetailDialog() {
    if (detailDialog != null) {
      detailDialog.setVisible(false);
      detailDialog.dispose();
      detailDialog = null;
    }
  }

  private JasperPrint initJasperPrintObject(final String reportPath, final HashMap reportParams)
          throws JRException, IOException {
    InputStream stream = null;
    try {
      if (reportPath.toUpperCase().startsWith("HTTP"))
        return JasperFillManager.fillReport(stream = new URL(reportPath).openStream(),
                reportParams, getModel().getTableModel().getJRDataSource());
      else
        return JasperFillManager.fillReport(reportPath, reportParams, getModel().getTableModel().getJRDataSource());
    }
    finally {
      if (stream != null)
        stream.close();
    }
  }

  private Property getPropertyToUpdate() throws UserCancelException {
    final JComboBox box = new JComboBox(new Vector<Property>(getUpdateProperties()));
    final int ret = JOptionPane.showOptionDialog(this, box,
            FrameworkMessages.get(FrameworkMessages.SELECT_PROPERTY_FOR_UPDATE),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);

    if (ret == JOptionPane.OK_OPTION)
      return (Property) box.getSelectedItem();
    else
      throw new UserCancelException();
  }

  private List<Property> getUpdateProperties() {
    final List<Property> ret = EntityRepository.get().getDatabaseProperties(getModel().getEntityID(), false, false, false);
    final ListIterator<Property> iter = ret.listIterator();
    while(iter.hasNext()) {
      final Property property = iter.next();
      if (property.hasParentProperty() || property instanceof Property.DenormalizedProperty)
        iter.remove();
    }
    Collections.sort(ret, new Comparator<Property>() {
      public int compare(final Property propertyOne, final Property propertyTwo) {
        return propertyOne.toString().toLowerCase().compareTo(propertyTwo.toString().toLowerCase());
      }
    });

    return ret;
  }

  private void bindModelEvents() {
    model.stActive.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (model.stActive.isActive()) {
          initialize();
          showPanelTab();
          prepareUI(!isParentPanel(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()), false);
        }
      }
    });
    model.evtRefreshStarted.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        UiUtil.setWaitCursor(true, EntityPanel.this);
      }
    });
    model.evtRefreshDone.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        UiUtil.setWaitCursor(false, EntityPanel.this);
      }
    });
  }

  private void initializeResizing() {
    final InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    final ActionMap actionMap = getActionMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
            KeyEvent.SHIFT_MASK + KeyEvent.CTRL_MASK, true), DIV_LEFT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
            KeyEvent.SHIFT_MASK + KeyEvent.CTRL_MASK, true), DIV_RIGHT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,
            KeyEvent.SHIFT_MASK + KeyEvent.CTRL_MASK, true), DIV_UP);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,
            KeyEvent.SHIFT_MASK + KeyEvent.CTRL_MASK, true), DIV_DOWN);

    actionMap.put(DIV_RIGHT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        final EntityPanel parent = (EntityPanel) SwingUtilities.getAncestorOfClass(EntityPanel.class, EntityPanel.this);
        if (parent != null)
          parent.resizePanel(RIGHT);
      }
    });
    actionMap.put(DIV_LEFT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        final EntityPanel parent = (EntityPanel) SwingUtilities.getAncestorOfClass(EntityPanel.class, EntityPanel.this);
        if (parent != null)
          parent.resizePanel(LEFT);
      }
    });
    actionMap.put(DIV_DOWN, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        resizePanel(DOWN);
      }
    });
    actionMap.put(DIV_UP, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        resizePanel(UP);
      }
    });
  }

  private void initializeNavigation() {
    final InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final ActionMap actionMap = getActionMap();
    final DefaultTreeModel applicationTreeModel = EntityApplicationModel.getApplicationModel().getApplicationTreeModel();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK, true), NAV_UP);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK, true), NAV_DOWN);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK, true), NAV_RIGHT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK, true), NAV_LEFT);

    actionMap.put(NAV_UP, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(UP, applicationTreeModel);
      }
    });
    actionMap.put(NAV_DOWN, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(DOWN, applicationTreeModel);
      }
    });
    actionMap.put(NAV_RIGHT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(RIGHT, applicationTreeModel);
      }
    });
    actionMap.put(NAV_LEFT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(LEFT, applicationTreeModel);
      }
    });
  }

  private void navigate(final int direction, final DefaultTreeModel applicationTreeModel) {
    final EntityModel active = getActiveModel();
    if (active == null) //fallback on default if no active panel found
      activateModel(EntityApplicationModel.getApplicationModel().getMainApplicationModels().iterator().next());
    else {
      switch(direction) {
        case UP:
          activateModel(getParent(active));
          break;
        case DOWN:
          if (active.getDetailModels().size() > 0 && active.getLinkedDetailModels().size() > 0)
            activateModel(active.getLinkedDetailModel());
          else
            activateModel(EntityApplicationModel.getApplicationModel().getMainApplicationModels().iterator().next());
          break;
        case LEFT:
          if (!activateModel(getLeftSibling(active, applicationTreeModel))) //wrap around
            activateModel(getRightmostSibling(active, applicationTreeModel));
          break;
        case RIGHT:
          if (!activateModel(getRightSibling(active, applicationTreeModel))) //wrap around
            activateModel(getLeftmostSibling(active, applicationTreeModel));
          break;
      }
    }
  }

  private static boolean activateModel(final EntityModel model) {
    if (model != null)
      model.stActive.setActive(true);

    return model != null;
  }

  private EntityModel getActiveModel() {
    final Enumeration enu = ((DefaultMutableTreeNode) EntityApplicationModel.getApplicationModel().getApplicationTreeModel().getRoot()).breadthFirstEnumeration();
    while (enu.hasMoreElements()) {
      final EntityModel model = (EntityModel) ((DefaultMutableTreeNode) enu.nextElement()).getUserObject();
      if (model != null && model.stActive.isActive())
        return model;
    }

    return null;
  }

  private EntityModel getParent(final EntityModel entityModel) {
    final TreePath path = findObject((DefaultMutableTreeNode) EntityApplicationModel.getApplicationModel().getApplicationTreeModel().getRoot(), entityModel);
    if (path != null) {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
      if (node.getParent() != null) {
        final EntityModel parent = (EntityModel) ((DefaultMutableTreeNode) node.getParent()).getUserObject();
        if (parent != null)
          return parent;
      }
    }

    return null;
  }

  private static EntityModel getRightSibling(final EntityModel model, final DefaultTreeModel applicationTreeModel) {
    final TreePath path = findObject((DefaultMutableTreeNode) applicationTreeModel.getRoot(), model);
    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
    if (node.getNextSibling() != null)
      return (EntityModel) node.getNextSibling().getUserObject();

    return null;
  }

  private static EntityModel getRightmostSibling(final EntityModel model, final DefaultTreeModel applicationTreeModel) {
    final TreePath path = findObject((DefaultMutableTreeNode) applicationTreeModel.getRoot(), model);
    final DefaultMutableTreeNode node = (DefaultMutableTreeNode)
            ((DefaultMutableTreeNode) path.getLastPathComponent()).getParent();

    return (EntityModel) ((DefaultMutableTreeNode) node.getLastChild()).getUserObject();
  }

  private static EntityModel getLeftSibling(final EntityModel model, final DefaultTreeModel applicationTreeModel) {
    final TreePath path = findObject((DefaultMutableTreeNode) applicationTreeModel.getRoot(), model);
    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
    if (node.getPreviousSibling() != null)
      return (EntityModel) node.getPreviousSibling().getUserObject();

    return null;
  }

  private static EntityModel getLeftmostSibling(final EntityModel model, final DefaultTreeModel applicationTreeModel) {
    final TreePath path = findObject((DefaultMutableTreeNode) applicationTreeModel.getRoot(), model);
    final DefaultMutableTreeNode node = (DefaultMutableTreeNode)
            ((DefaultMutableTreeNode) path.getLastPathComponent()).getParent();

    return (EntityModel) ((DefaultMutableTreeNode) node.getFirstChild()).getUserObject();
  }

  private static TreePath findObject(final DefaultMutableTreeNode root, final Object object) {
    if (object == null)
      return null;

    final Enumeration nodes = root.preorderEnumeration();
    while (nodes.hasMoreElements()) {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
      if (node.getUserObject() == object)
        return new TreePath(node.getPath());
    }

    return null;
  }

  private static Container createDependenciesPanel(final Map<String, List<Entity>> dependencies,
                                                   final IEntityDbProvider dbProvider) throws UserException {
    try {
      final JPanel ret = new JPanel(new BorderLayout());
      final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
      tabPane.setUI(new BorderlessTabbedPaneUI());
      for (final Map.Entry<String, List<Entity>> entry : dependencies.entrySet()) {
        final List<Entity> dependantEntities = entry.getValue();
        if (dependantEntities.size() > 0)
          tabPane.addTab(entry.getKey(), createStaticEntityPanel(dependantEntities, dbProvider));
      }
      ret.add(tabPane, BorderLayout.CENTER);

      return ret;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  private static void showDependenciesDialog(final Map<String, List<Entity>> dependencies,
                                             final IEntityDbProvider model,
                                             final JComponent dialogParent) throws UserException {
    JDialog dialog;
    try {
      UiUtil.setWaitCursor(true, dialogParent);

      final JOptionPane optionPane = new JOptionPane(createDependenciesPanel(dependencies, model),
              JOptionPane.PLAIN_MESSAGE, JOptionPane.NO_OPTION, null,
              new String[] {Messages.get(Messages.CLOSE)});
      dialog = optionPane.createDialog(dialogParent,
              FrameworkMessages.get(FrameworkMessages.DEPENDENT_RECORDS_FOUND));
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      UiUtil.resizeWindow(dialog, 0.4, new Dimension(800, 400));
      dialog.setLocationRelativeTo(dialogParent);
      dialog.setResizable(true);
    }
    finally {
      UiUtil.setWaitCursor(false, dialogParent);
    }

    dialog.setVisible(true);
  }

  private static class ActivationFocusAdapter extends MouseAdapter {

    private final JComponent target;

    public ActivationFocusAdapter(final JComponent target) {
      this.target = target;
    }

    public void mouseReleased(MouseEvent e) {
      target.requestFocusInWindow();//activates this EntityPanel
    }
  }

  public static class EntityPanelInfo  implements Comparable {

    private final String caption;
    private final Class<? extends EntityPanel> entityPanelClass;
    private final Class<? extends EntityModel> entityModelClass;

    public EntityPanelInfo(final Class<? extends EntityModel> entityModelClass,
                           final Class<? extends EntityPanel> entityPanelClass) {
      this(null, entityModelClass, entityPanelClass);
    }

    public EntityPanelInfo(final String caption,
                           final Class<? extends EntityModel> entityModelClass,
                           final Class<? extends EntityPanel> entityPanelClass) {
      this.caption = caption == null ? "" : caption;
      this.entityModelClass = entityModelClass;
      this.entityPanelClass = entityPanelClass;
    }

    /**
     * @return Value for property 'caption'.
     */
    public String getCaption() {
      return caption;
    }

    /**
     * @return Value for property 'entityModelClass'.
     */
    public Class<? extends EntityModel> getEntityModelClass() {
      return entityModelClass;
    }

    /**
     * @return Value for property 'entityPanelClass'.
     */
    public Class<? extends EntityPanel> getEntityPanelClass() {
      return entityPanelClass;
    }

    public EntityPanel getInstance(final EntityModel model) throws UserException {
      if (model == null)
        throw new RuntimeException("Can not create a EntityPanel without an EntityModel");
      try {
        return getEntityPanelClass().getConstructor().newInstance().setModel(model);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException ite) {
        if (ite.getCause() instanceof UserException)
          throw (UserException) ite.getCause();

        throw new UserException(ite.getCause());
      }
      catch (Exception e) {
        throw new UserException(e);
      }
    }

    public EntityPanel getInstance(final IEntityDbProvider provider) throws UserException {
      try {
        return getInstance(getEntityModelClass().getConstructor(IEntityDbProvider.class).newInstance(provider));
      }
      catch (UserException e) {
        throw e;
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (InvocationTargetException ite) {
        if (ite.getCause() instanceof UserException)
          throw (UserException) ite.getCause();

        throw new UserException(ite.getCause());
      }
      catch (Exception e) {
        throw new UserException(e);
      }
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
      if(this == obj)
        return true;
      if((obj == null) || (obj.getClass() != this.getClass()))
        return false;

      final EntityPanelInfo panelInfo = (EntityPanelInfo) obj;

      return getCaption().equals(panelInfo.getCaption())
              && getEntityModelClass().equals(panelInfo.getEntityModelClass())
              && getEntityPanelClass().equals(panelInfo.getEntityPanelClass());
    }

    /** {@inheritDoc} */
    public int hashCode() {
      int hash = 7;
      hash = 31 * hash + getCaption().hashCode();
      hash = 31 * hash + getEntityModelClass().hashCode();
      hash = 31 * hash + getEntityPanelClass().hashCode();

      return hash;
    }

    /** {@inheritDoc} */
    public int compareTo(Object o) {
      final String thisCompare = getCaption() == null ? entityPanelClass.getSimpleName() : getCaption();
      final String thatCompare = ((EntityPanelInfo)o).getCaption() == null
              ? ((EntityPanelInfo)o).entityPanelClass.getSimpleName() : ((EntityPanelInfo)o).getCaption();

      return thisCompare.compareTo(thatCompare);
    }
  }
}